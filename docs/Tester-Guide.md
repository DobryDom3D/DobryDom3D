# Testing DobryDom3D

DobryDom3D is a fork of Sweet Home 3D 7.5. The builds you are being asked to try
are **early test builds**: they have not been widely used yet, and they are
**not code-signed**, so Windows and macOS will show security warnings the first
time you start them (see [Installing](#installing)). Thank you for helping.

## Before you start — please read

### 1. Back up every `.sh3d` file you open

**Make a copy of any home file before you open it in DobryDom3D**, and keep the
copy somewhere DobryDom3D will not touch. Work on the copy, or at least do not
overwrite your only original.

Future test builds may change the `.sh3d` format slightly, and it is not yet
clear whether a home saved by DobryDom3D can still be opened by Sweet Home 3D.
Treat files saved by DobryDom3D as DobryDom3D-only.

### 2. If Sweet Home 3D is installed on the same computer

For now DobryDom3D uses **the same settings and auto-recovery folders as Sweet
Home 3D**, so the two programs can see and overwrite each other's data. Either
test on a computer or user account that does not use Sweet Home 3D, or back up
these folders first:

| System | Folder |
|---|---|
| Windows | `C:\Users\<you>\AppData\Roaming\eTeks\Sweet Home 3D` |
| macOS | `~/Library/Application Support/eTeks/Sweet Home 3D` |
| Linux | `~/.eteks/sweethome3d` |

Some settings are also kept in the Java preferences store (Windows registry key
`HKEY_CURRENT_USER\Software\JavaSoft\Prefs\com\eteks\sweethome3d`, macOS
`~/Library/Preferences/com.apple.java.util.prefs.plist`, Linux
`~/.java/.userPrefs/com/eteks/sweethome3d`).

### 3. File associations

The Windows and macOS installers register DobryDom3D for `.sh3d` files (and the
other Sweet Home 3D file types), so double-clicking a home may now open
DobryDom3D instead of Sweet Home 3D. You can change this in your system's
"Open with" settings.

## Downloading

Go to <https://github.com/DobryDom3D/DobryDom3D/releases> and open the
pre-release you were pointed to. Download the file for your system:

| System | File |
|---|---|
| Windows (64-bit) | `DobryDom3D-<version>-windows-x64.exe` |
| macOS, Apple Silicon (M1 and later) | `DobryDom3D-<version>-macosx-arm64.dmg` |
| macOS, Intel | `DobryDom3D-<version>-macosx-x64.dmg` |
| Linux (64-bit) | `DobryDom3D-<version>-linux-x64.tgz` |

To check which Mac you have, choose Apple menu > About This Mac: the chip is
named "Apple M…" or "Intel". `SHA256SUMS` in the same release lets you verify a
download if you wish.

## Installing

### Windows

1. Run the `.exe`.
2. Windows may show **"Windows protected your PC"** because the installer is not
   signed. Click **More info**, then **Run anyway**.
3. Follow the installer, then start DobryDom3D from the Start menu.

### macOS

1. Open the `.dmg` and drag **DobryDom3D** into **Applications**.
2. The first start will be blocked because the app is not signed. Open
   **System Settings > Privacy & Security**, scroll down to the message about
   DobryDom3D, and click **Open Anyway**. (On older macOS versions, right-click
   the app and choose Open.)
3. If macOS says the app is **"damaged"**, open Terminal and run:

   ```sh
   xattr -dr com.apple.quarantine /Applications/DobryDom3D.app
   ```

   then start it again.

### Linux

Extract the archive anywhere and run `DobryDom3D-<version>/bin/DobryDom3D`.

## What to try

You do not need to test everything; whatever you normally do with Sweet Home 3D
is useful. In particular:

- Start the program, create a small home, save it, close the program and open
  the home again.
- Open one of your own homes (a copy!) and check that it looks right in the plan
  and in the 3D view.
- Create a photo with each renderer available in the photo dialog (Sunflow and
  YafaRay), including a photo with a transparent sky and with different
  exposure and sun power settings if you can.
- Print or export (PDF, SVG, images) something.
- On Windows and macOS: uninstall, and install a newer test build over an older
  one.

## Reporting problems

Open an issue at <https://github.com/DobryDom3D/DobryDom3D/issues> and include:

- your operating system and its version,
- your computer's processor and graphics card,
- the build you used (the release name, or the version in the Help menu's About
  dialog),
- what you did, what you expected, and what happened — an exact error message or
  a screenshot helps a lot,
- if it concerns a particular home, the `.sh3d` file, if you are able to share it.

## Uninstalling

- **Windows:** Settings > Apps > DobryDom3D > Uninstall.
- **macOS:** drag DobryDom3D from Applications to the Trash.
- **Linux:** delete the extracted folder.

Your settings folder (see above) is left in place.
