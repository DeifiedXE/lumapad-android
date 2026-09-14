# LumaPad

LumaPad is an Android game-streaming client focused on touch controls for PC games that do not
support controllers. It uses Moonlight's GameStream client core and connects to an existing
Sunshine host; no LumaPad-specific Windows service is required.

## MVP controls

- Left virtual stick: four independently bindable keyboard directions (WASD by default).
- Right-side aim area: direct relative-mouse swiping with adjustable sensitivity, optional axis
  inversion, and a configurable input combination held for as long as the area is touched.
- Uncovered video area: Moonlight's normal relative mouse trackpad.
- Round buttons: addable, removable, and bindable to one input or a multi-key combination of
  mouse buttons, alphanumeric and function keys, navigation keys, numpad keys, and distinct
  left/right Ctrl, Shift, Alt, and Windows modifier keys.
- Binding editor: a full-screen PC keyboard for visual key selection, with the original list
  selector retained as an option under On-screen Controls settings.
- Monitor switch button: toggles between two configurable Sunshine capture outputs (monitors 1
  and 2 by default) using Sunshine's built-in Ctrl+Alt+Shift+F1-F12 shortcuts.
- Profiles: saved separately for each streamed application and input mode.
- Physical gamepads: continue to pass through using Moonlight's existing controller path.

Enable **Settings > On-screen Controls > Show on-screen controls**, then select
**Keyboard + mouse (mixed sticks)**. During streaming, tap the gear repeatedly to cycle through
move, resize, binding, and active modes. In binding mode, tap a round button to select one or more
inputs, or delete it. Tap the keyboard stick or aim area to edit or delete it. The **+** menu can
create a virtual button and restore a deleted keyboard stick or aim area. Set the aim area's
touch-held input to **RMB** for games that aim while the right mouse button is held.

The binding interface defaults to **Full-screen PC keyboard**. Change **Button mapping interface**
to **Original list selector** in the On-screen Controls settings if a compact scrolling list is
preferred. Both interfaces expose the complete key set and keep left/right modifiers separate.
The monitor switch is included in new/default layouts. For an existing saved layout, enter binding
mode and choose **+ > Switch streaming monitor**; tap the button while in binding mode to change
the two monitor numbers or delete it.

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
