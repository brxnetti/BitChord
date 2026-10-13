# Third-party notices — BitChord (Automix Pro branch)

BitChord is GPLv3 (see LICENSE). This file lists third-party components
shipped in or built into the app, so a redistributor can see at a glance
what each piece allows. Nothing here changes any license; the original
texts govern.

## App license

- **BitChord** — GNU General Public License v3.0
  <https://github.com/kushagrasinghx/BitChord>

## Transition planner / analysis (derived from Orchard)

The files under `shared/.../playback/smart/` derived from Orchard carry
their own headers:

- **Orchard transition planner & analysis** — GNU Affero General Public
  License v3.0
  <https://github.com/SFG5453/Orchard>
  Note: the headers say AGPLv3. The app as a whole is distributed under
  GPLv3; anyone redistributing should confirm the combined work's
  obligations with the upstream authors.

## Neural models (shipped in `app/src/main/assets/`)

- **Beat This!** (`beat_this_int8.onnx`) — MIT License.
  Beat-tracking model; code and weights published under MIT.
  <https://github.com/CPJKU/beat_this>

- **open-unmix umxhq, vocals target** (`vocals_umxhq_int8.onnx`) — MIT License.
  Vocal-activity mask model. The umxhq checkpoint's weights are MIT
  (confirmed on the weights' Zenodo deposit, record 3370489, "License: MIT
  License") — unlike the `umxl` checkpoint, whose weights are CC BY-NC-SA
  4.0 (non-commercial) and are NOT used here. Only the vocals target ships.
  <https://github.com/sigsep/open-unmix-pytorch>

## Native audio

- **FFmpeg via librempeg** (`native/ffmpeg/build.sh`) — built with
  `--enable-gpl --enable-version3` and without `--enable-agpl`, so the
  resulting `libffmpegJNI.so` is GPLv3, like the app. (In librempeg only
  avfilter's frame threading and the ffmpeg CLI are AGPL, and neither is
  built.)
  <https://github.com/librempeg/librempeg>

- **DST (Dynamic Spectral Transform)** — LGPL-2.1 notice, see
  `native/` for the attribution file.

## What is NOT shipped

- No Rubber Band, no SoundTouch. Time-stretching is done by Media3 /
  AudioTrack's own resampling; the planner only picks rates.
- No Essentia models, no Meta htdemucs weights (both non-commercial and
  deliberately excluded).
