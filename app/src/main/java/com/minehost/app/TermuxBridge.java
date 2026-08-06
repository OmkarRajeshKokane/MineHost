package com.minehost.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Base64;

/** Sends only MineHost-owned scripts to the Termux RUN_COMMAND integration. */
public final class TermuxBridge {
    public static final String PACKAGE = "com.termux";
    private static final String RUN_PERMISSION = "com.termux.permission.RUN_COMMAND";

    private TermuxBridge() { }

    public static boolean installed(Context context) {
        try { context.getPackageManager().getPackageInfo(PACKAGE, 0); return true; }
        catch (PackageManager.NameNotFoundException e) { return false; }
    }

    public static boolean authorized(Context context) {
        return context.checkSelfPermission(RUN_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    public static void run(Context context, String label, String script) {
        if (!installed(context)) throw new IllegalStateException("Termux is not installed");
        if (!authorized(context)) throw new IllegalStateException("Allow 'Run commands in Termux environment' in MineHost permissions first");
        Intent command = new Intent("com.termux.RUN_COMMAND");
        command.setClassName(PACKAGE, "com.termux.app.RunCommandService");
        command.putExtra("com.termux.RUN_COMMAND_PATH", "$PREFIX/bin/bash");
        command.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-s"});
        command.putExtra("com.termux.RUN_COMMAND_STDIN", script);
        // Omit the working directory: Termux defaults it to its own home directory.
        command.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        command.putExtra("com.termux.RUN_COMMAND_LABEL", label);
        command.putExtra("com.termux.RUN_COMMAND_DESCRIPTION", "MineHost mobile Java server operation");
        context.startService(command);
    }

    public static String installScript(String version, String properties, int memoryGb) {
        String encoded = Base64.encodeToString(properties.getBytes(), Base64.NO_WRAP);
        return "set -eu\n"
                + "pkg update -y\n"
                + "pkg install -y openjdk-21 curl jq tmux coreutils\n"
                + "ROOT=\"$HOME/minehost\"\n"
                + "mkdir -p \"$ROOT/server\" \"$ROOT/logs\"\n"
                + "cd \"$ROOT/server\"\n"
                + "VERSION='" + shellToken(version) + "'\n"
                + "META=\"$(curl -fsSL https://piston-meta.mojang.com/mc/game/version_manifest_v2.json)\"\n"
                + "VERSION_META=\"$(printf '%s' \"$META\" | jq -r --arg v \"$VERSION\" '.versions[] | select(.id == $v) | .url' | head -n 1)\"\n"
                + "test -n \"$VERSION_META\" && test \"$VERSION_META\" != null\n"
                + "SERVER_URL=\"$(curl -fsSL \"$VERSION_META\" | jq -r '.downloads.server.url')\"\n"
                + "curl -fL --retry 3 \"$SERVER_URL\" -o server.jar\n"
                + "printf '%s' '" + encoded + "' | base64 -d > server.properties\n"
                + "printf 'eula=true\\n' > eula.txt\n"
                + "cat > start.sh <<'MINEHOST_START'\n"
                + "#!/data/data/com.termux/files/usr/bin/bash\n"
                + "cd \"$(dirname \"$0\")\"\n"
                + "exec java -Xms" + memoryGb + "G -Xmx" + memoryGb + "G -jar server.jar nogui\n"
                + "MINEHOST_START\n"
                + "chmod 700 start.sh\n"
                + "echo 'MineHost setup complete. Use Start phone host in MineHost.'\n";
    }

    public static String syncScript(String properties) {
        String encoded = Base64.encodeToString(properties.getBytes(), Base64.NO_WRAP);
        return "set -eu\nmkdir -p \"$HOME/minehost/server\"\nprintf '%s' '" + encoded + "' | base64 -d > \"$HOME/minehost/server/server.properties\"\necho 'Server settings synchronized.'\n";
    }

    public static String startScript() {
        return "set -eu\ncd \"$HOME/minehost/server\"\ntest -f server.jar || { echo 'Run setup first.'; exit 1; }\nif tmux has-session -t minehost 2>/dev/null; then echo 'Server is already running.'; else tmux new-session -d -s minehost 'exec bash \"$HOME/minehost/server/start.sh\"'; echo 'Server starting in tmux session minehost.'; fi\n";
    }

    public static String stopScript() {
        return "if tmux has-session -t minehost 2>/dev/null; then tmux send-keys -t minehost 'stop' C-m; sleep 5; tmux kill-session -t minehost 2>/dev/null || true; echo 'Stop command sent.'; else echo 'No MineHost tmux session is running.'; fi\n";
    }

    public static String healthScript() {
        return "if tmux has-session -t minehost 2>/dev/null; then echo 'RUNNING: MineHost server session active'; else echo 'STOPPED: no MineHost server session'; fi\n";
    }

    private static String shellToken(String value) { return value.replace("'", "").replace("\n", "").replace("\r", ""); }
}
