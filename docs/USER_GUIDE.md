# LumaPad Installation and User Guide

[Download latest release](https://github.com/DeifiedXE/lumapad-android/releases/latest) ·
[繁體中文教學](USER_GUIDE.zh-TW.md) · [Back to README](../README.md)

LumaPad is an unofficial client based on Moonlight Android. It is designed to make PC games
without native controller support practical on a touchscreen. It uses the normal Moonlight
streaming protocol and connects directly to Sunshine, so no separate LumaPad Windows server is
required.

## 1. Requirements

You need:

- A computer capable of running the game and Sunshine.
- An Android phone or tablet running Android 5.0 or later.
- For initial setup, connecting both devices to the same local network is recommended. A wired
  connection for the host PC usually provides the best results.

If the original Moonlight app already connects to your PC, skip to **Install LumaPad on Android**.

## 2. Install Sunshine on the host PC

1. Open the official [Sunshine Releases](https://github.com/LizardByte/Sunshine/releases) and
   download the current stable package for your operating system. On a typical Windows PC, use
   the AMD64/x64 `.msi` installer.
2. Install and start Sunshine. The Windows installer normally runs Sunshine as a background
   service.
3. On the host PC, open `https://localhost:47990` in a browser.
4. Create the Sunshine web-interface username and password on first launch. The browser may warn
   about a self-signed certificate; verify that the address is the local `localhost:47990` page
   before continuing.
5. Add games under **Applications** if desired. Streaming **Desktop** also provides access to every
   LumaPad manual-profile feature.

See the official
[Sunshine Getting Started guide](https://docs.lizardbyte.dev/projects/sunshine/latest/md_docs_2getting__started.html)
for the latest host-specific instructions.

## 3. Install LumaPad on Android

1. Open [LumaPad Releases](https://github.com/DeifiedXE/lumapad-android/releases/latest).
2. Under the newest release's **Assets**, download the file whose name starts with `LumaPad-` and
   ends in `.apk`.
3. Open the downloaded APK. If Android blocks it, follow the prompt to allow the current browser or
   file manager to **Install unknown apps**. Menu names vary by device manufacturer.
4. Return to the installer and select **Install**. If Google Play Protect offers to scan the APK,
   you can scan it before continuing.

LumaPad uses its own application ID and can be installed alongside the Google Play version of
Moonlight.

To update, install the newer APK directly over the installed version so your profiles remain. If
you uninstall LumaPad first, Android also deletes its local settings and control profiles.

## 4. Connect and pair with Sunshine

1. Confirm that Sunshine is running, then open LumaPad.
2. LumaPad normally discovers PCs on the same network. If yours is missing, use the add-PC action
   and enter the host's local IP address.
3. Select the PC. LumaPad displays a pairing PIN.
4. Open `https://localhost:47990` on the host, go to **PIN**, and enter the PIN and a device name.
5. After pairing succeeds, select **Desktop** or a configured Sunshine application to start the
   stream.

## 5. Enable mixed keyboard and mouse controls

In LumaPad, open:

**Settings > On-screen Controls Settings > Show on-screen controls**

Then set **Touch control type** to:

**Keyboard + mouse (mixed sticks)**

The default layout includes:

- Left stick: W, A, S, and D keyboard movement.
- Right transparent aim area: relative mouse movement while swiping.
- Round shortcuts: mouse buttons, Q/W/E/R, and number keys 1–5.
- Radial shortcut menu: less frequently used keyboard or mouse inputs.
- Stream-display switch: switches between displays 1 and 2 by default.
- Uncovered video area: uses a button to switch between separately configured mouse and shooting
  modes.

## 6. Move, resize, and configure controls

The gear button at the top-left of the stream cycles through four modes:

1. **Active mode**: normal gameplay.
2. **Move mode**: drag controls to reposition them.
3. **Resize mode**: drag controls to change their size.
4. **Binding mode**: tap a button, stick, aim area, or radial menu to edit it. A `+` button also
   appears at the top-left.

Press the gear once more to return to active mode and save the layout. Colored outlines during
configuration are normal.

### Add or remove a control

In **binding mode**, press `+` to add:

- A general virtual button
- A radial shortcut menu
- A mouse/shooting mode switch
- A keyboard movement stick
- A free-swipe aim area
- A stream-display switch button

Tap an existing control in binding mode to find its delete option. The keyboard stick and aim area
can also be deleted and restored later from the `+` menu.

## 7. Bind keys and combinations

1. Enter **binding mode**.
2. Tap a round virtual button.
3. Select one or more inputs, such as `L Ctrl + L Shift + Q`, then save.

The full-screen keyboard includes function keys, navigation keys, the numeric keypad, mouse
buttons, and separate left/right Ctrl, Shift, Alt, and Windows keys. To use the original list UI,
open:

**Settings > On-screen Controls Settings > Button mapping interface > Original list selector**

Both editors provide the complete input set and support key combinations.

## 8. Configure the aim area

Tap the right-side aim area in binding mode to configure:

- Horizontal and vertical mouse sensitivity
- X-axis and Y-axis inversion
- A key, mouse button, or combination held while the aim area is touched

For games that aim while the right mouse button is held, set the touch-held input to **RMB**. The
right button remains down while your finger is on the aim area, mouse movement follows your swipe,
and the button is released when your finger lifts.

### Switch uncovered areas between mouse and shooting modes

The **MOUSE/SHOOT** button on the stream shows the active mode. Tap it once to switch. In binding
mode, tap this button to configure:

- Uncovered-area sensitivity and left-click/long-press-drag behavior for **Mouse mode**.
- Uncovered-area sensitivity and left-click/long-press-drag behavior for **Shooting mode**.
- An optional key or combination sent whenever the mode changes.

For example, while shooting mode is active, bind `Tab` to the switch. One tap sends Tab to open the
inventory and enters mouse mode; another tap sends Tab to close it and returns to shooting mode.
Both sensitivity controls range from 25%–300%.

The left-click option applies only to uncovered stream areas. It does not change a separate virtual
left button, the aim area's sensitivity, or two-finger scrolling. **Use the touchscreen as a
trackpad** must be enabled. If the switch is deleted, restore it with
`+ > Mouse/shooting mode switch`.

## 9. Use a radial shortcut menu

The radial menu is useful for inventory, map, skill-panel, and function-key actions that do not
need permanent screen space:

1. Enter binding mode and select `+ > Radial shortcut menu`.
2. Tap the wheel and choose between 2 and 12 sectors.
3. Assign a key, mouse button, or combination to each sector. Sectors run clockwise from the top.
4. In active mode, hold the center, drag toward a sector, and release to trigger it.
5. Drag back into the center dead zone before releasing to cancel.

Each wheel has independent settings for:

- **Center dead zone**: controls the size of the cancellation area.
- **Sector switching hysteresis**: prevents flicker near sector boundaries. A higher value is more
  stable but requires a more deliberate direction change.
- **Selection haptics**: vibrates when a different sector becomes selected. The global on-screen
  control vibration preference must also be enabled.
- **Activation mode**: release to tap once, or hold the selected input until the finger is lifted.

## 10. Switch control profiles manually

LumaPad does not automatically switch layouts based on the Steam or Desktop name reported by
Sunshine. The button beside the top-left gear shows the current profile name. Tap it to:

- Switch profiles manually
- Create a profile
- Rename the current profile
- Delete the current profile

A new profile starts as a complete copy of the current layout, so you can quickly adapt it for
Path of Exile, a shooter, or another game. Your manual selection remains active for future streams
and is not replaced when you launch Steam or Desktop. Older app-specific Steam/Desktop layouts are
imported as named profiles when LumaPad first encounters them.

## 11. Switch between two displays

Tap the display-switch button to send Sunshine's `Ctrl + Alt + Shift + F1` through `F12` monitor
shortcut. It switches between displays 1 and 2 by default.

If the order is wrong, enter binding mode, tap the display-switch button, and change its two display
numbers. The actual numbering follows Sunshine's display order. If an older saved layout does not
contain the button, restore it with `+ > Switch streaming monitor`.

## 12. Troubleshooting

### The PC is not discovered

- Confirm Sunshine is running and both devices are on the same local network.
- Add the PC's local IP address manually in LumaPad.
- Check whether Windows Firewall or a third-party firewall is blocking Sunshine.

### The stream works but touch controls are missing

- Confirm **Show on-screen controls** is enabled.
- Confirm the selected mode is **Keyboard + mouse (mixed sticks)**.
- If controls were temporarily hidden, enable their display again.

### The radial menu does not vibrate

- Enable **Selection haptics** for that radial menu.
- Enable LumaPad's global on-screen-control vibration preference.
- Some devices, battery-saving modes, or system haptic settings may suppress vibration.

### Profiles appear missing after an update

Installing over the old version preserves profiles; uninstalling clears app data. LumaPad imports
older Steam/Desktop-specific layouts as named profiles when encountered. Tap the profile-name
button at the top-left to check the imported profiles.

## Security and project notice

Download APKs only from this project's
[GitHub Releases](https://github.com/DeifiedXE/lumapad-android/releases/latest). LumaPad is an
unofficial Moonlight-derived project and is not affiliated with the Moonlight or Sunshine teams.
