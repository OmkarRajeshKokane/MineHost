package com.minehost.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/** A local Android control surface for preparing a Java Edition server profile. */
public class MainActivity extends Activity {
    private static final int PICK_MOD = 42;
    private static final int GREEN = Color.rgb(116, 217, 155);
    private static final int INK = Color.rgb(226, 244, 234);
    private static final int MUTED = Color.rgb(159, 190, 171);
    private LinearLayout page;
    private SharedPreferences prefs;
    private EditText serverName, maxPlayers, memory, port, publicAddress;
    private Switch onlineMode, pvp, commandBlocks;
    private final ArrayList<String> modEntries = new ArrayList<>();
    private Spinner serverType, version;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("minehost", MODE_PRIVATE);
        modEntries.addAll(prefs.getStringSet("mods", new HashSet<String>()));
        buildShell();
        showHost();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(11, 22, 17));
        root.setPadding(dp(18), dp(14), dp(18), dp(10));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(this);
        mark.setImageResource(getResources().getIdentifier("hero_blockworld", "drawable", getPackageName()));
        header.addView(mark, new LinearLayout.LayoutParams(dp(52), dp(42)));
        LinearLayout titleStack = new LinearLayout(this);
        titleStack.setOrientation(LinearLayout.VERTICAL);
        titleStack.addView(label("MINEHOST", 24, INK, true));
        titleStack.addView(label("Java server control room", 12, MUTED, false));
        header.addView(titleStack, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button help = smallButton("?");
        help.setOnClickListener(v -> showHelp());
        header.addView(help, new LinearLayout.LayoutParams(dp(44), dp(40)));
        root.addView(header);

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        String[] labels = {"Host", "Version", "Settings"};
        for (String text : labels) {
            Button tab = new Button(this);
            tab.setText(text);
            tab.setTextSize(12);
            tab.setTextColor(MUTED);
            tab.setAllCaps(false);
            tab.setBackgroundColor(Color.TRANSPARENT);
            if (text.equals("Host")) tab.setOnClickListener(v -> showHost());
            if (text.equals("Version")) tab.setOnClickListener(v -> showVersions());
            if (text.equals("Settings")) tab.setOnClickListener(v -> showSettings());
            nav.addView(tab, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        root.addView(nav);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, dp(4), 0, dp(16));
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
    }

    private void showDashboard() {
        clearPage();
        TextView hero = label("Your server, ready for the world.", 25, INK, true);
        page.addView(hero);
        page.addView(label("Create a profile here, then deploy it to a computer or VPS that has Java. Keep the host online for friends to join.", 14, MUTED, false), margin(0, 7, 0, 16));
        page.addView(illustration());

        LinearLayout status = panel();
        status.addView(label("PUBLIC PLAY CHECKLIST", 12, GREEN, true));
        status.addView(check("Choose a server version and accept the Minecraft EULA."));
        status.addView(check("Run the generated configuration on a Java-capable PC or VPS."));
        status.addView(check("Expose TCP port 25565 using port forwarding or a TCP tunnel."));
        page.addView(status, margin(0, 14, 0, 14));

        LinearLayout connect = panel();
        connect.addView(label("Server address", 16, INK, true));
        publicAddress = field("play.example.com:25565", InputType.TYPE_CLASS_TEXT);
        publicAddress.setText(prefs.getString("address", ""));
        connect.addView(publicAddress, margin(0, 8, 0, 8));
        Button copy = primary("Copy address");
        copy.setOnClickListener(v -> {
            String address = publicAddress.getText().toString().trim();
            if (address.isEmpty()) { toast("Add a public address first"); return; }
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Minecraft address", address));
            prefs.edit().putString("address", address).apply(); toast("Address copied");
        });
        connect.addView(copy);
        page.addView(connect);

        Button plan = primary("Open deployment guide");
        plan.setOnClickListener(v -> showDeploymentGuide());
        page.addView(plan, margin(0, 16, 0, 0));
    }

    private void showVersions() {
        clearPage();
        page.addView(label("Choose your Java server", 25, INK, true));
        page.addView(label("Phone host mode uses the official Vanilla server for a reliable, simple setup. Mods and plugins need a separately managed Fabric, Forge, or Paper host.", 14, MUTED, false), margin(0, 6, 0, 16));
        LinearLayout card = panel();
        card.addView(label("SERVER SOFTWARE", 12, GREEN, true));
        serverType = spinner(new String[]{"Official Vanilla (phone-host compatible)"});
        setSpinner(serverType, "Official Vanilla (phone-host compatible)");
        card.addView(serverType, margin(0, 7, 0, 12));
        card.addView(label("MINECRAFT VERSION", 12, GREEN, true));
        version = spinner(new String[]{"1.21.8 (latest profile)", "1.21.7", "1.21.6", "1.20.6", "1.20.4", "1.19.4"});
        setSpinner(version, prefs.getString("version", "1.21.8 (latest profile)"));
        card.addView(version, margin(0, 7, 0, 12));
        card.addView(label("The host downloads the selected server version directly from Minecraft after you accept Mojang's EULA.", 13, MUTED, false));
        page.addView(card);
        Button save = primary("Save server profile");
        save.setOnClickListener(v -> {
            prefs.edit().putString("type", serverType.getSelectedItem().toString()).putString("version", version.getSelectedItem().toString()).apply();
            toast("Version profile saved");
        });
        page.addView(save, margin(0, 16, 0, 0));
    }

    private void showMods() {
        clearPage();
        page.addView(label("Mods & plugins", 25, INK, true));
        page.addView(label("Add .jar files to your deployment queue. Use plugins with Paper; use mods only with their matching Fabric or Forge loader.", 14, MUTED, false), margin(0, 6, 0, 16));
        LinearLayout add = panel();
        add.addView(label("ADD A DOWNLOAD LINK", 12, GREEN, true));
        EditText url = field("https://example.com/mod.jar", InputType.TYPE_TEXT_VARIATION_URI);
        add.addView(url, margin(0, 7, 0, 8));
        Button addUrl = primary("Add link to queue");
        addUrl.setOnClickListener(v -> {
            String entry = url.getText().toString().trim();
            if (!entry.startsWith("https://")) { toast("Use a secure https:// download link"); return; }
            modEntries.add(entry); persistMods(); url.setText(""); showMods();
        });
        add.addView(addUrl);
        Button browse = outline("Choose a local .jar file");
        browse.setOnClickListener(v -> pickMod());
        add.addView(browse, margin(0, 9, 0, 0));
        page.addView(add);
        LinearLayout list = panel();
        list.addView(label("DEPLOYMENT QUEUE · " + modEntries.size(), 12, GREEN, true));
        if (modEntries.isEmpty()) list.addView(label("No additions yet. You can use a link or select a file from this device.", 14, MUTED, false), margin(0, 8, 0, 0));
        for (String entry : modEntries) {
            TextView row = label("• " + shortName(entry), 14, INK, false);
            row.setPadding(0, dp(8), 0, dp(8));
            row.setOnLongClickListener(v -> { modEntries.remove(entry); persistMods(); showMods(); toast("Removed from queue"); return true; });
            list.addView(row);
        }
        page.addView(list, margin(0, 14, 0, 0));
        page.addView(label("Tip: long-press a queued item to remove it. Only add files you trust; a server .jar runs code on your host.", 12, MUTED, false), margin(4, 12, 4, 0));
    }

    private void showSettings() {
        clearPage();
        page.addView(label("Server settings", 25, INK, true));
        page.addView(label("These values become a server.properties-style configuration for your host.", 14, MUTED, false), margin(0, 6, 0, 16));
        LinearLayout card = panel();
        card.addView(label("GENERAL", 12, GREEN, true));
        serverName = field("Server name", InputType.TYPE_CLASS_TEXT); serverName.setText(prefs.getString("name", "My Minecraft Server"));
        maxPlayers = field("Max players", InputType.TYPE_CLASS_NUMBER); maxPlayers.setText(prefs.getString("players", "20"));
        memory = field("Memory in GB", InputType.TYPE_CLASS_NUMBER); memory.setText(prefs.getString("memory", "4"));
        port = field("Server port", InputType.TYPE_CLASS_NUMBER); port.setText(prefs.getString("port", "25565"));
        card.addView(serverName, margin(0, 8, 0, 8)); card.addView(maxPlayers, margin(0, 0, 0, 8)); card.addView(memory, margin(0, 0, 0, 8)); card.addView(port);
        page.addView(card);
        LinearLayout access = panel();
        access.addView(label("ACCESS & GAMEPLAY", 12, GREEN, true));
        onlineMode = toggle("Require official Microsoft/Minecraft authentication", prefs.getBoolean("online", true));
        onlineMode.setOnCheckedChangeListener((b, checked) -> { if (!checked) showOfflineWarning(); });
        pvp = toggle("Enable player-versus-player combat", prefs.getBoolean("pvp", true));
        commandBlocks = toggle("Enable command blocks", prefs.getBoolean("commands", false));
        access.addView(onlineMode); access.addView(pvp); access.addView(commandBlocks);
        page.addView(access, margin(0, 14, 0, 0));
        Button save = primary("Save settings & copy config");
        save.setOnClickListener(v -> saveSettings());
        page.addView(save, margin(0, 16, 0, 0));
    }

    private void showHost() {
        clearPage();
        page.addView(label("Host on this phone", 25, INK, true));
        page.addView(label("MineHost uses Termux to run an official Vanilla Java server directly on this device. Keep the phone plugged in, on Wi-Fi, and exempt Termux from battery restrictions.", 14, MUTED, false), margin(0, 6, 0, 16));

        LinearLayout versionCard = panel();
        versionCard.addView(label("ACTIVE SERVER VERSION", 12, GREEN, true));
        versionCard.addView(label("Minecraft: Java Edition " + selectedVersionId(), 19, INK, true), margin(0, 7, 0, 2));
        versionCard.addView(label("Phone host runtime: Official Vanilla server", 13, MUTED, false));
        page.addView(versionCard, margin(0, 0, 0, 14));

        LinearLayout readiness = panel();
        readiness.addView(label("PHONE HOST READINESS", 12, GREEN, true));
        boolean termux = TermuxBridge.installed(this);
        boolean permission = TermuxBridge.authorized(this);
        readiness.addView(check("Termux " + (termux ? "is installed" : "needs to be installed")));
        readiness.addView(check("MineHost command permission: " + (permission ? "granted" : "not granted")));
        readiness.addView(check("In Termux, enable external apps once: mkdir -p ~/.termux && echo allow-external-apps=true >> ~/.termux/termux.properties"));
        readiness.addView(check("Disable battery optimization for Termux before starting a long-running world."));
        page.addView(readiness);

        LinearLayout commandCard = panel();
        commandCard.addView(label("ONE-TIME TERMUX COMMAND", 12, GREEN, true));
        commandCard.addView(label("mkdir -p ~/.termux && printf 'allow-external-apps=true\\n' > ~/.termux/termux.properties", 13, INK, false), margin(0, 8, 0, 8));
        Button copyCommand = primary("Copy Termux command");
        copyCommand.setOnClickListener(v -> {
            String command = "mkdir -p ~/.termux && printf 'allow-external-apps=true\\n' > ~/.termux/termux.properties";
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("MineHost Termux command", command));
            toast("Termux command copied — paste it in Termux and press Enter");
        });
        commandCard.addView(copyCommand);
        page.addView(commandCard, margin(0, 14, 0, 0));

        LinearLayout connectionCard = panel();
        connectionCard.addView(label("CONNECT FROM YOUR PC", 12, GREEN, true));
        TextView localAddress = label(connectionAddressText(), 17, INK, true);
        localAddress.setPadding(0, dp(8), 0, dp(2));
        connectionCard.addView(localAddress);
        connectionCard.addView(label("Use this in Minecraft Java Edition → Multiplayer → Add Server while the PC and phone are on the same Wi-Fi.", 13, MUTED, false), margin(0, 2, 0, 5));
        Button copyAddress = primary("Copy PC connection address");
        copyAddress.setOnClickListener(v -> {
            String address = localConnectionAddress();
            if (address == null) { toast("No local Wi-Fi address found — connect the phone to Wi-Fi and refresh"); return; }
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Minecraft LAN address", address));
            toast("PC connection address copied");
        });
        connectionCard.addView(copyAddress);
        Button refreshAddress = outline("Refresh phone IP");
        refreshAddress.setOnClickListener(v -> { localAddress.setText(connectionAddressText()); toast("Phone IP refreshed"); });
        connectionCard.addView(refreshAddress);
        page.addView(connectionCard, margin(0, 14, 0, 0));

        LinearLayout controls = panel();
        controls.addView(label("TERMUX HOST CONTROLS", 12, GREEN, true));
        Button getTermux = outline(termux ? "Open Termux" : "Get Termux from F-Droid");
        getTermux.setOnClickListener(v -> { if (TermuxBridge.installed(this)) openTermux(); else openUrl("https://f-droid.org/packages/com.termux/"); });
        controls.addView(getTermux, margin(0, 4, 0, 2));
        Button grant = outline("Open MineHost permission settings");
        grant.setOnClickListener(v -> openMineHostSettings());
        controls.addView(grant);
        Button setup = primary("Set up Java server on this phone");
        setup.setOnClickListener(v -> confirmPhoneSetup());
        controls.addView(setup, margin(0, 5, 0, 5));
        Button sync = outline("Sync saved settings to phone host");
        sync.setOnClickListener(v -> runTermux("Sync server settings", TermuxBridge.syncScript(serverProperties())));
        controls.addView(sync);
        Button start = primary("Start phone host");
        start.setOnClickListener(v -> runTermux("Start MineHost server", TermuxBridge.startScript()));
        controls.addView(start, margin(0, 5, 0, 5));
        Button stop = outline("Stop phone host");
        stop.setOnClickListener(v -> runTermux("Stop MineHost server", TermuxBridge.stopScript()));
        controls.addView(stop);
        Button health = outline("Check host session");
        health.setOnClickListener(v -> runTermux("Check MineHost host", TermuxBridge.healthScript()));
        controls.addView(health);
        page.addView(controls, margin(0, 14, 0, 0));

        LinearLayout network = panel();
        network.addView(label("PUBLIC INTERNET ACCESS", 12, GREEN, true));
        network.addView(label("The world runs on your phone, but other players still need a route to it. On Wi-Fi, forward TCP port " + prefs.getString("port", "25565") + " to this phone's local address. On carrier data or CGNAT, use a TCP tunnel service. The app cannot bypass a carrier's network firewall.", 14, MUTED, false), margin(0, 8, 0, 0));
        page.addView(network, margin(0, 14, 0, 0));
        page.addView(label("Phone host mode intentionally downloads the Vanilla server from Minecraft after you confirm setup. It does not bundle or redistribute Minecraft server software. Fabric/Forge mods need their own compatible runtime; queued add-ons are not loaded by Vanilla.", 12, MUTED, false), margin(4, 12, 4, 0));
    }

    private void saveSettings() {
        if (serverName.getText().toString().trim().isEmpty()) { toast("Server name is required"); return; }
        SharedPreferences.Editor e = prefs.edit();
        e.putString("name", serverName.getText().toString().trim()).putString("players", maxPlayers.getText().toString().trim())
         .putString("memory", memory.getText().toString().trim()).putString("port", port.getText().toString().trim())
         .putBoolean("online", onlineMode.isChecked()).putBoolean("pvp", pvp.isChecked()).putBoolean("commands", commandBlocks.isChecked()).apply();
        String config = "motd=" + prefs.getString("name", "My Minecraft Server") + "\nmax-players=" + prefs.getString("players", "20") + "\nserver-port=" + prefs.getString("port", "25565") + "\nonline-mode=" + onlineMode.isChecked() + "\npvp=" + pvp.isChecked() + "\nenable-command-block=" + commandBlocks.isChecked();
        ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("server.properties", config));
        toast("Saved. Configuration copied to clipboard");
    }

    private void showOfflineWarning() {
        new AlertDialog.Builder(this).setTitle("Offline authentication enabled")
            .setMessage("This is the setting commonly used for TLauncher/offline accounts. It disables Microsoft account verification, so anyone can join using another player's name. Use a whitelist and an authentication plugin, and never expose an admin account.")
            .setPositiveButton("I understand", null).setNegativeButton("Keep verification on", (d, w) -> onlineMode.setChecked(true)).show();
    }

    private void showDeploymentGuide() {
        String guide = "1. On a PC or VPS, install the Java version required by your chosen server.\n\n2. Download the matching Paper/Vanilla/Fabric/Forge server from its official source, accept the Minecraft EULA, and run it.\n\n3. Copy the settings from this app into server.properties. Put queued files in plugins (Paper) or mods (Fabric/Forge).\n\n4. For public joining, forward TCP " + prefs.getString("port", "25565") + " from your router to the host, or use a reputable TCP tunnel. Share the host's public address.\n\nThis app stores your setup profile; the always-online computer/VPS is what actually hosts the world.";
        new AlertDialog.Builder(this).setTitle("Deploy for public play").setMessage(guide).setPositiveButton("Got it", null).show();
    }

    private void confirmPhoneSetup() {
        String versionId = selectedVersionId();
        new AlertDialog.Builder(this).setTitle("Set up phone-hosted server")
            .setMessage("This downloads OpenJDK, Termux tools, and the official Vanilla Minecraft " + versionId + " server to this phone. It uses about 1–2 GB before world data, can warm the phone, and requires you to accept Minecraft's EULA. Continue?")
            .setNegativeButton("Cancel", null).setPositiveButton("Accept & set up", (d, w) -> runTermux("Set up MineHost phone server", TermuxBridge.installScript(versionId, serverProperties(), memoryGb()))).show();
    }

    private void runTermux(String label, String script) {
        try { TermuxBridge.run(this, label, script); toast("Command sent to Termux. Open Termux to view progress."); }
        catch (IllegalStateException e) { toast(e.getMessage()); }
        catch (Exception e) { toast("Could not start Termux command: " + e.getMessage()); }
    }

    private void openTermux() {
        Intent i = getPackageManager().getLaunchIntentForPackage(TermuxBridge.PACKAGE);
        if (i == null) { openUrl("https://f-droid.org/packages/com.termux/"); return; }
        startActivity(i);
    }

    private void openMineHostSettings() {
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private void openUrl(String value) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(value))); }
        catch (Exception e) { toast("No browser is available"); }
    }

    private String selectedVersionId() {
        String profile = prefs.getString("version", "1.21.8 (latest profile)");
        int stop = profile.indexOf(' ');
        return stop > 0 ? profile.substring(0, stop) : profile;
    }

    private int memoryGb() {
        try { return Math.max(1, Math.min(8, Integer.parseInt(prefs.getString("memory", "4")))); }
        catch (NumberFormatException e) { return 4; }
    }

    private String serverProperties() {
        String name = prefs.getString("name", "My Minecraft Server").replace('\n', ' ').replace('\r', ' ');
        String players = prefs.getString("players", "20").replaceAll("[^0-9]", "");
        String serverPort = prefs.getString("port", "25565").replaceAll("[^0-9]", "");
        if (players.isEmpty()) players = "20";
        if (serverPort.isEmpty()) serverPort = "25565";
        return "motd=" + name + "\nmax-players=" + players + "\nserver-port=" + serverPort + "\nonline-mode=" + prefs.getBoolean("online", true) + "\npvp=" + prefs.getBoolean("pvp", true) + "\nenable-command-block=" + prefs.getBoolean("commands", false) + "\n";
    }

    private String localConnectionAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                        return address.getHostAddress() + ":" + prefs.getString("port", "25565");
                    }
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    private String connectionAddressText() {
        String address = localConnectionAddress();
        return address == null ? "No local Wi-Fi IP found" : address;
    }

    private void showHelp() {
        new AlertDialog.Builder(this).setTitle("About MineHost")
            .setMessage("MineHost is a mobile control room for Java Edition server profiles. It does not include Minecraft server software and does not impersonate Mojang. Keep official authentication enabled unless you understand the security impact of offline mode.")
            .setPositiveButton("OK", null).show();
    }

    private void pickMod() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/java-archive"); i.addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(i, PICK_MOD); } catch (Exception e) { i.setType("*/*"); startActivityForResult(i, PICK_MOD); }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_MOD && result == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData(); getContentResolver().takePersistableUriPermission(uri, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
            modEntries.add(uri.toString()); persistMods(); showMods(); toast("Local file queued");
        }
    }

    private void persistMods() { prefs.edit().putStringSet("mods", new HashSet<>(modEntries)).apply(); }
    private void clearPage() { page.removeAllViews(); }
    private LinearLayout panel() { LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(16), dp(15), dp(16), dp(15)); box.setBackgroundResource(getResources().getIdentifier("bg_panel", "drawable", getPackageName())); return box; }
    private TextView label(String s, int sp, int color, boolean bold) { TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setLineSpacing(2, 1.08f); if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return v; }
    private TextView check(String s) { TextView v = label("✓  " + s, 14, INK, false); v.setPadding(0, dp(8), 0, 0); return v; }
    private EditText field(String hint, int type) { EditText e = new EditText(this); e.setHint(hint); e.setTextColor(INK); e.setHintTextColor(MUTED); e.setTextSize(15); e.setSingleLine(true); e.setInputType(type); e.setBackgroundResource(getResources().getIdentifier("bg_field", "drawable", getPackageName())); e.setPadding(dp(14), 0, dp(14), 0); e.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))); return e; }
    private Switch toggle(String s, boolean checked) { Switch w = new Switch(this); w.setText(s); w.setTextColor(INK); w.setTextSize(14); w.setChecked(checked); w.setPadding(0, dp(9), 0, dp(9)); return w; }
    private Spinner spinner(String[] items) { Spinner s = new Spinner(this); ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items) { @Override public View getView(int p, View c, ViewGroup parent) { TextView v = (TextView)super.getView(p,c,parent); v.setTextColor(INK); v.setTextSize(15); v.setPadding(dp(12),0,dp(12),0); return v; } }; s.setAdapter(a); s.setBackgroundResource(getResources().getIdentifier("bg_field", "drawable", getPackageName())); s.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))); return s; }
    private void setSpinner(Spinner s, String value) { for (int i=0;i<s.getCount();i++) if (s.getItemAtPosition(i).toString().equals(value)) { s.setSelection(i); return; } }
    private Button primary(String s) { Button b = new Button(this); b.setText(s); b.setTextColor(Color.rgb(10, 30, 18)); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setAllCaps(false); b.setBackgroundResource(getResources().getIdentifier("bg_primary", "drawable", getPackageName())); b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50))); return b; }
    private Button outline(String s) { Button b = new Button(this); b.setText(s); b.setTextColor(GREEN); b.setTextSize(14); b.setAllCaps(false); b.setBackgroundColor(Color.TRANSPARENT); b.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46))); return b; }
    private Button smallButton(String s) { Button b = new Button(this); b.setText(s); b.setTextColor(GREEN); b.setTextSize(20); b.setAllCaps(false); b.setBackgroundColor(Color.TRANSPARENT); return b; }
    private ImageView illustration() { ImageView v = new ImageView(this); v.setImageResource(getResources().getIdentifier("hero_blockworld", "drawable", getPackageName())); v.setScaleType(ImageView.ScaleType.CENTER_CROP); v.setBackgroundResource(getResources().getIdentifier("bg_panel", "drawable", getPackageName())); v.setPadding(dp(8),dp(8),dp(8),dp(8)); v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(136))); return v; }
    private LinearLayout.LayoutParams margin(int l,int t,int r,int b) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); p.setMargins(dp(l),dp(t),dp(r),dp(b)); return p; }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    private String shortName(String item) { int slash = Math.max(item.lastIndexOf('/'), item.lastIndexOf(':')); return slash >= 0 ? item.substring(slash + 1) : item; }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
