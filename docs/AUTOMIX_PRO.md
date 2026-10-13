# Automix Pro — auditoría, arquitectura y parámetros

Rama: `automix-pro` (sobre `main` @ `fe95bb6`). Fecha: 2026-10-13.

## 1. Fase 0: auditoría (cómo funciona hoy)

### Crossfade
- Ajuste `crossfade_seconds` (0–12 s, SharedPreferences `bitchord_settings`).
- Lo ejecuta `CrossfadeController` (3035 líneas): rampa de ganancia **equal-power
  sin²/cos²** sobre dos reproductores, con tick de ~250 ms por corrutina.
- 0 s ≈ gapless implícito (no hay modo Gapless explícito).

### Gapless
- Dos capas: gapless nativo de Media3 dentro de un mismo reproductor, y
  `TransitionStyle.GAPLESS` del planificador para pistas consecutivas del mismo
  álbum (handoff casi instantáneo, 0.12 s, sin mezcla).

### Automix [Beta] (hoy)
- Toggle `smart_fade_enabled` (off por defecto). No es un crossfade con otro
  nombre: es un **dual-deck real** — `PlaybackService` construye DOS ExoPlayer
  (`player` + `spare`); el segundo se carga con la pista entrante en el punto
  de cue (sin seek, sin discontinuidad), se le aplica el time-stretch antes de
  `prepare()`, arranca en silencio y a mitad de la mezcla `adoptPlayer()`
  intercambia sesión, audio focus y MediaSession al reproductor entrante.
- Análisis previo por pista en `playback/smart/TrackAnalyzer.kt`: decodifica
  inicio+final de la pista actual y la siguiente, corre el modelo **Beat This!**
  (ONNX int8, `assets/beat_this_int8.onnx`, 3.4 MB, sin DBN) para beats/downbeats
  y un modelo de máscara vocal (`vocals_umxhq_int8.onnx`, 8.6 MB), más DSP nativo
  de respaldo. Resultado: `TrackAnalysis` (bpm, beatInterval, beatConfidence
  0–1, downbeats, key+confianza, loudness, curvas de energía por bandas,
  máscara vocal, estructura con candidatos mix-in/mix-out, contentEnd).
- Caché versionada en disco: `AnalysisStore` → `<filesDir>/smart_analysis/`,
  un JSON por pista con `SCHEMA_VERSION` (hoy = 1; entradas viejas se descartan
  y se re-analizan, no se migran).
- Política por niveles (`TransitionPolicy.assessTransitionTier`):
  - **BEATMATCHED** (Nivel 1): confianza ≥ 0.55 en ambas, BPM 40–220 tras
    alineación de octava, estiramiento ≤ 4 % (modo avanzado: solo acelerar,
    hasta 10 %).
  - **DJ_ASSISTED** (Nivel 2): confianza ≥ 0.2 en al menos una; anclas
    cuantizadas a beat/compás + handoff de EQ, sin time-stretch.
  - **PLAIN_CROSSFADE** (Nivel 3): fade equal-power en anclas analizadas
    (contentEnd / audibleStart).
- Planificador (`TransitionPlanner.planTransition`, 1537 líneas, portado de
  Orchard bajo AGPLv3): elige `TransitionStyle`
  (EQUAL_POWER / GAPLESS / DJ_BLEND / DJ_FILTER), anclas mix-in/out,
  `incomingPlaybackRate`, punto de **bass swap** (curva low-band), variante de
  filtro determinista por par (SWEEP / BASS_HANDOFF / ECHO_RIDE), eco
  sincronizado al beat. Ya evita solapar voces (`vocalOverlap`) y limita la
  música descartada a 12 s.
- DSP en vivo por reproductor (`playback/audio/DspChain.kt`, float32):
  `SpatialAudioProcessor → EqualizerProcessor → TransitionFilterProcessor →
  LoudnessProcessor` (cada uno se auto-bypassea cuando está inactivo).
  `LoudnessProcessor` es por reproductor con peak limiter y `setBlendTrim`
  (`1/sqrt(in+out)`, ≤ 1.5 dB en el punto medio) para que la suma no sature.
- Velocidad 0.5×–2× (`AppSettings.playbackSpeed`) se multiplica con el stretch
  de beatmatch y se deshace al terminar. Skip silence: `SilenceSkippingAudioProcessor`
  de Media3 en la rama PCM (en la rama float lo suple el propio sink).
- EQ: `EqualizerProcessor` paramétrico de 10 slots en la app + EQ del sistema
  vía la sesión de audio compartida por ambos reproductores.

### native/analyzer (qué calcula, cómo se compila)
- `native/analyzer/`: `tempo_analysis.cpp` (onsets por flujo espectral +
  autocorrelación + **phase-locked loop** sobre la rejilla; downbeats por
  banda grave, asume 4/4), `audio_analysis.cpp` (envolvente, `FindMixOutTime`,
  **chroma + plantillas Krumhansl** para tonalidad, heurística logística de
  actividad vocal, loudness = RMS dBFS − 0.691 — **no es BS.1770**,
  true-peak, rango dinámico, curvas de energía, estructura por frases de
  32 beats), `mel_spectrogram.cpp` (log-mel 22050 Hz / 128 mels / hop 441,
  formato exacto que Beat This! espera), `vocal_spectrogram.cpp` (STFT para
  open-unmix), `resampler.cpp` (sinc ventaneado).
- JNI en `app/src/main/cpp/jni/` (`analysis_jni.cpp`, `mel_jni.cpp`,
  `vocal_jni.cpp`); wrappers Kotlin en
  `shared/src/jvmSharedMain/.../playback/smart/` (`BeatTracker.kt`,
  `VocalTracker.kt`, `TrackFeatures.kt`, `NativeAnalysisLibrary.kt`).
- Compila con CMake 3.22.1 → `libbitchord_analysis.so` (C++17, `-O3`, sin
  `-ffast-math`, segmentos alineados a 16 KB). Sin `ndkVersion` fijado en
  Gradle (usa el NDK que AGP encuentre/instale).
- `native/ffmpeg/build.sh`: `libffmpegJNI.so` para el renderer FFmpeg de
  Media3, enlazado estático contra **librempeg** (elegido por su decodificador
  AC-4). Códecs: ac4, alac, ac3, eac3, truehd, mlp, dca, flac, mp3, aac,
  vorbis, opus. ABIs: armeabi-v7a, arm64-v8a, x86_64. Licencia declarada:
  `--enable-gpl --enable-version3`, sin AGPL.

### Stack de reproducción
- Media3 1.11.0 (exoplayer, session, hls, dash, datasource-okhttp).
- Fuente de datos: `AudioCache` (SimpleCache + `DynamicLruCacheEvictor`,
  512 MB–10 GB) → `SmbDataSource` → `DefaultDataSource` con resolución
  `bitchord://` → URL real (Innertube + deofuscación JS del parámetro `n`).
- Decodificación: MediaCodec primero; renderer FFmpeg al final (ALAC en todos
  los teléfonos, AC-4/Dolby donde no hay decodificador con licencia); DSD vía
  `playback/dsd/` (decimador nativo `bitchord_dsd` + descompresor DST
  `bitchord_dst`, LGPL-2.1). Negociación de salida float vs PCM16 en
  `playback/audio/` (`OutputNegotiator`, `PcmEncoding`, `LosslessOutput`).
- Hi-Res FLAC/ALAC: vía módulos de fuente (YouTube, SMB, archivos locales,
  descargas offline — `DownloadSession`/`Downloads`).

### Dónde vive cada cosa
- Ajustes: `AppSettings.kt` (objeto singleton, SharedPreferences
  `bitchord_settings`; **no hay DataStore ni Room**).
- Strings: `app/src/main/res/values*/strings.xml` (18 locales con "Automix").
- Sin base de datos: no hay Room/SQLite; el análisis vive en archivos JSON.
- MediaSession: `PlaybackService : MediaLibraryService`, sesión
  `MediaLibrarySession`; el cambio de pista se publica a mitad de la mezcla
  (`onTrackBecameCurrent` re-invocado en el handoff).
- Scrobbling: `ScrobbleManager` (Last.fm/ListenBrainz, al min(50 %, 180 s)).
- Discord: `DiscordRPC` (KizzyRPC, al inicio de pista).
- Indicador "Mixing": `AppSettings.smartMixInProgress` (solo se enciende en
  mezclas reales, no en fades de respaldo) + `smartMixBlend` para el scrubber.
- "Stats for nerds": `SmartAnalysis` (estado de análisis actual/siguiente) +
  `smartTransitionWindow`.

### Cómo se construye y firma
- Gradle 8.11.1, AGP 8.10.1 (con override de R8 a 8.13.23 por un bug de
  dexing en debug), Kotlin 2.3.20, JDK 17 (CI) / 21, compileSdk 37
  (plataforma `android-37.0`; el AAR de InnerTubeX exige compilar contra 37),
  minSdk 26, targetSdk 36, ABIs armeabi-v7a + arm64-v8a + x86_64 (APK
  universal), R8 con `-dontobfuscate` y keeps para JNI/modelos/reflexión en
  `proguard-rules.pro`.
- `applicationId = "com.music.bitchord"` (flavor `prod`; `dev` usa
  `com.dev.bitchord`), `versionCode = 27`, `versionName = "1.8-automixpro"`
  (vía el mecanismo `betaSuffix` del proyecto).
- Firma: `keystore.properties` (gitignored) → `signingConfigs.release`;
  CI (`.github/workflows/android.yml`) decodifica `secrets.KEYSTORE_BASE64`
  y corre `./gradlew assembleProdRelease`.
- Estado de compilación en este entorno (2026-10-13, verificado): el build
  completo de Gradle **no corre aquí** — el daemon de Gradle muere al
  arrancar ("Could not receive a message from the daemon"; diagnosticado:
  el entorno solo deja ~0.5 GB libres y el daemon no sobrevive). En su
  lugar, el módulo `shared` (donde vive toda la lógica nueva de Automix
  Pro) **sí compiló limpio** con kotlinc 2.3.20 directo y sus pruebas
  corrieron: 22 nuevas (`AutomixProTest`: migración, topes por estilo,
  anti-repetición, Camelot, **selección armónica de técnica, solape matcheado
  por energía, reglas de cue profesionales (Zehren)**, veredicto) + 11
  existentes
  (`AdvancedAutomixPlannerTest`), todas en verde. El APK se compila en
  GitHub Actions con `.github/workflows/automix-pro.yml` (corre
  `:shared:jvmTest`, `assembleProdRelease`, verifica con `aapt2`/`zipalign`/
  `apksigner` y publica el APK sin firmar + SHA-256). Sin keystore, el
  release sale sin firmar en vez de fallar.

## 2. Decisión: evolucionar el motor, no sustituirlo

1. El dual-deck ya existe y funciona: dos ExoPlayers, handoff a mitad de
   mezcla, rampas equal-power y planificador por niveles de confianza.
2. Reescribirlo tiraría ~6000 líneas probadas (CrossfadeController,
   TransitionPlanner, filtros DJ) y violaría la prioridad 3 (no romper lo
   que funciona).
3. Lo que falta es la capa Pro encima: selector único de modo, estilos de
   mezcla, anti-repetición, veredicto explicable, versionado del análisis.
4. Un pre-render nativo con Rubber Band sería ideal en teoría, pero suma
   MBs al APK y semanas de JNI; el dual-deck en vivo con tick de 250 ms ya
   logra el objetivo audible sin él.
5. Evolucionar permite compilar y medir en cada hito sin regresiones;
   sustituir sería un big-bang imposible de verificar aquí.

## 3. Arquitectura Automix Pro

```
Análisis (ahead-of-time)          Runtime (decide, nunca toca PCM crudo)
─────────────────────────        ──────────────────────────────────────
TrackAnalyzer ──► TrackAnalysis ──► assessTransitionTier ──► Nivel 1/2/3
     │                │                     │
     │        AnalysisStore (v1,           TransitionPlanner.planTransition
     │         sin cambios: el             │  (+ estilos, anti-repetición)
     │         formato guardado                                     CrossfadeController (dual-deck)
     │         no cambió)                        │  tick 250 ms, handoff 50 %
                                           DspChain float32 por deck
```

- **Modo de transición** (ajuste único): `OFF` / `CROSSFADE` / `GAPLESS` /
  `AUTOMIX_PRO`. Migra `smart_fade_enabled`+`crossfade_seconds`:
  smart_fade → AUTOMIX_PRO; si no, crossfade>0 → CROSSFADE; si no → GAPLESS.
- **Estilos** (solo Automix Pro): Sutil (solape 4–6 s), Equilibrado (8–12 s),
  DJ (16–30 s, solo electrónica de pulso estable). Tope aplicado en el
  planificador como `maxFadeSeconds`.
- **Niveles por confianza** (sin cambios en umbrales, ya calibrados):
  Nivel 1 = BEATMATCHED, Nivel 2 = DJ_ASSISTED, Nivel 3 = PLAIN_CROSSFADE.
- **Anti-repetición**: el planificador elige la variante de filtro de forma
  determinista por par (hash FNV-1a), pero `CrossfadeController` le pasa las
  variantes de las últimas 4 transiciones (`ANTI_REPEAT_WINDOW = 4`); si la
  elección determinista repetiría una reciente y hay alternativa válida, rota
  a la siguiente. Sin historial, el comportamiento es idéntico al anterior.
- **Explicable**: cada transición publica `TransitionVerdict` (nivel, técnica,
  solape en segundos y compases, estiramiento %, relación de tonalidad en
  Camelot, motivo) a "Stats for nerds" (`AppSettings.lastTransitionVerdict`);
  el indicador "Mixing" ya existe (`smartMixInProgress`).
- **Selección armónica de técnica** (`HarmonicRelation` en `AutomixPro.kt`):
  un clash de tonalidad (p. ej. C mayor → F♯ mayor) nunca se mezcla abierto,
  por más que los tempos coincidan: esos pares usan el ride de filtro con su
  low-pass de cierre enmascarando la disonancia. Tonalidades compatibles
  conservan el `DJ_BLEND` abierto; con tonalidad desconocida/poco confiable,
  el comportamiento es el histórico. El clash se aplica vía el *estilo*, no
  vía la variante de filtro, para no matar la variedad de gestos.
- **Solape matcheado por energía**: si la cabeza del tema entrante es mucho
  más silenciosa que la cola del saliente (energía media < 0.5×), el solape
  se topa a 6 s en vez de arrastrar un outro fuerte sobre un intro
  silencioso.
- **Reglas de cue profesionales** (Zehren et al., "Automatic Detection of
  Cue Points for DJ Mixing", ISMIR 2020, validadas contra datos anotados por
  expertos): `rankMixInCandidates` puntúa los puntos de entrada con novedad
  (el tema "llega" ahí: la energía sube), downbeat que abre período de frase
  y saliencia (los 4 compases siguientes se sostienen solos, energía media >
  0.4× el pico). Dónde entra la pista importa tanto como cómo se mezcla.

## 4. Parámetros (un solo lugar)

En `TransitionPolicy.kt` (compartido):
- `MIN_BEATMATCH_CONFIDENCE = 0.55`, `MIN_DJ_CONFIDENCE = 0.2`
- `MIN_BPM = 40`, `MAX_BPM = 220` (tras alineación de octava)
- `MAX_STRETCH_DEVIATION = 0.04` (avanzado: solo-acelerar hasta 0.10)
- `MAX_DISCARDED_MUSIC_SECONDS = 12.0`
- `VOCAL_ACTIVE_THRESHOLD = 0.6`

En `TransitionPlanner.kt`:
- `AutomixStyle`: SUBTLE (4–6 s), BALANCED (8–12 s), DJ (16–30 s). El tope se
  aplica en `mixPlan` (`maximumOverlap`) y en el despacho de `phraseSwitch`
  (si el phrase-switch necesitaría más solape del que el estilo permite, se
  usa el blend más corto de `mixPlan` en vez de un phrase-switch desalineado).
- `ANTI_REPEAT_WINDOW = 4` (rotación de variante, no penalización en coste).

Cómo afinarlos: el estilo es solo un tope — la técnica y el nivel los sigue
decidiendo el análisis. Si una transición suena forzada, bajar primero el
tope del estilo antes de tocar los umbrales de confianza (ya calibrados).

## 5. Pruebas

- Unitarias (JVM): `AutomixProTest` (22 pruebas: migración del modo legado,
  topes por estilo, anti-repetición, Camelot, selección armónica de técnica,
  solape matcheado por energía, reglas de cue profesionales, formato del
  veredicto) y
  `AdvancedAutomixPlannerTest` (11 pruebas existentes, sin regresiones).
  Compiladas y corridas con kotlinc 2.3.20 directo el 2026-10-13: 33/33 en
  verde. (El runner de Gradle no funciona en este entorno; en CI corren vía
  `./gradlew :shared:jvmTest`.)
- Compilación: `./gradlew assembleProdRelease` en cada hito — en GitHub
  Actions (`.github/workflows/automix-pro.yml`), no local.
- En dispositivo/emulador (no disponible en este entorno — ver informe):
  log de tiempos planificados vs. reales (< 20 ms) y cero underruns.
- Regresión: con Automix Pro apagado (modo OFF/CROSSFADE/GAPLESS), la
  reproducción es idéntica a la anterior (el mapeo de modos preserva el
  comportamiento previo por construcción; verificado en pruebas para la
  migración, pendiente en dispositivo para el audio).

## 6. Notas del entorno de compilación

- SDK: `~/workspace/android-sdk` (build-tools 36.1.0, platform-tools 37.0.1,
  plataformas 34/36/37.0 — la 37.0 se instaló manual desde
  `dl.google.com/android/repository/platform-37.0_r01.zip` porque el
  `repository2-1.xml` público aún no la lista; symlink `android-37 →
  android-37.0` para AGP 8.10).
- NDK r28c (`ndk/28.2.13676358`, descarga directa; el `sdkmanager` de este
  entorno falla de forma intermitente tras el proxy con
  `NoSuchElementException` en `doTunneling`).
- JDK 21 para Gradle; `local.properties` no se necesita (se usa
  `ANDROID_HOME`/`ANDROID_SDK_ROOT`).

## 7. Licencias de terceros (ver THIRD_PARTY_NOTICES.md)

- Beat This! (código y pesos): MIT — verificado en el README oficial de
  CPJKU (ISMIR 2024). Sin DBN de madmom (licencia no comercial) en el
  pipeline.
- open-unmix `umxhq`, objetivo vocals (`vocals_umxhq_int8.onnx`): **MIT** —
  verificado el 2026-10-13 en la página del depósito Zenodo de los pesos
  (record 3370489, "License: MIT License"). Ojo: el checkpoint `umxl` SÍ es
  CC BY-NC-SA 4.0 (no comercial) y no se usa aquí.
- ONNX Runtime Android: MIT. librempeg: según su build (GPLv3 declarado).
  DST: LGPL-2.1. TransitionPlanner/analyzer nativo: port de Orchard,
  AGPLv3 (sección 13: sigue gobernado por AGPLv3 dentro de la app GPLv3).
