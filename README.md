# MineHost

MineHost is an Android control-room app for running a Minecraft: Java Edition Vanilla server from an Android phone through Termux. It has a version picker, public-address helper, phone-host controls, and editable general `server.properties` settings.

## What it does

- Stores a phone-host-compatible official Vanilla profile locally on the Android device.
- Copies a small `server.properties` configuration to the clipboard.
- Detects and copies the local Minecraft connection address for a PC on the same Wi-Fi network.
- Includes **Host** mode: a permission-gated Termux integration that sets up OpenJDK, downloads the official Vanilla server after EULA confirmation, and can sync, start, stop, and check a server session running on the phone.

## Important limits

Phone-host mode requires the current Termux app from F-Droid/GitHub, the `Run commands in Termux environment` permission for MineHost, and `allow-external-apps=true` in Termux. The server runs in a Termux `tmux` session, so use Wi-Fi, disable battery optimization for Termux, and keep the phone cool and powered. For public access, expose the selected TCP port (default `25565`) through router port forwarding or a reputable TCP tunnel; carrier NAT cannot be bypassed by an app.

MineHost intentionally does not bundle Minecraft server software. It downloads the selected official Vanilla server directly on the phone only after the user confirms EULA acceptance.

The **Require official authentication** switch maps to `online-mode`. Turning it off enables offline/cracked-client compatibility, often requested for TLauncher, but it removes account identity verification. Use a whitelist and an authentication plugin; do not grant operator permissions to untrusted players.

## Build

Open in Android Studio or run:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is written to `app\build\outputs\apk\debug\app-debug.apk`.

## Server references

- [Minecraft Java server download and EULA](https://www.minecraft.net/en-us/download/server)
- [Paper server getting started](https://docs.papermc.io/paper/getting-started/)
- [Paper server properties reference](https://docs.papermc.io/paper/reference/server-properties/)
