# LumaPad

LumaPad is an Android game-streaming client focused on touch controls for PC games that do not
support controllers. It uses Moonlight's GameStream client core and connects to an existing
Sunshine host; no LumaPad-specific Windows service is required.

## MVP controls

- Left virtual stick: WASD, switchable to arrow keys.
- Right virtual stick: velocity-based relative mouse movement.
- Uncovered video area: Moonlight's normal relative mouse trackpad.
- Round buttons: bindable to mouse buttons, 0-9, A-Z, F1-F12, arrows, and common modifier keys.
- Profiles: saved separately for each streamed application and input mode.
- Physical gamepads: continue to pass through using Moonlight's existing controller path.

Enable **Settings > On-screen Controls > Show on-screen controls**, then select
**Keyboard + mouse (mixed sticks)**. During streaming, tap the gear repeatedly to cycle through
move, resize, binding, and active modes. In binding mode, tap a round button or the left stick.

The default mapping is aimed at action RPGs such as Path of Exile: mouse buttons, Q/W/E/R, and
1-5 are immediately available, while the left stick provides WASD movement for games that support
keyboard movement.

## Build

The project currently follows Moonlight Android's SDK requirements (Android SDK 37.0, NDK
29.0.14206865, and JDK 17 or newer):

```powershell
.\gradlew.bat assembleNonRootDebug
```

On Windows, Android's native build tools may require the repository to be located at an ASCII-only
path. A temporary `subst` drive can be used when the workspace path contains non-ASCII characters.

## License and attribution

LumaPad is a modified work based on Moonlight Android and moonlight-common-c. The project remains
licensed under GNU GPL v3; see `LICENSE.txt`. LumaPad is not an official Moonlight project.
