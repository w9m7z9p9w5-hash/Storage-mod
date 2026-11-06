package com.istore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/** Client-side copy of what the data pack has stored, plus the (silent) commands that talk to it. */
public final class StorageData {
    private static final String PREFIX = "IS1|";
    private static final Pattern ENTRY =
            Pattern.compile("([\"'])([a-z0-9_.\\-]+:[a-z0-9_./\\-]+)\\1\\s*:\\s*(-?\\d+)[bBsSlL]?");

    private static Map<String, Integer> counts = new HashMap<>();
    private static long lastSent;

    private StorageData() {}

    public static Map<String, Integer> counts() {
        return counts;
    }

    /** Called for every incoming game message. Returns false to hide it from chat. */
    public static boolean allowIncoming(Component message) {
        String text = message.getString();
        if (text.startsWith(PREFIX)) {
            Map<String, Integer> parsed = new HashMap<>();
            Matcher m = ENTRY.matcher(text);
            while (m.find()) {
                try {
                    parsed.put(m.group(2), Integer.parseInt(m.group(3)));
                } catch (NumberFormatException ignored) {
                    // skip a malformed entry
                }
            }
            counts = parsed;
            refreshOpenScreen();
            return false;
        }
        if (message.getContents() instanceof TranslatableContents t
                && t.getKey().startsWith("commands.function.success")
                && System.currentTimeMillis() - lastSent < 3000) {
            return false;
        }
        return true;
    }

    private static void refreshOpenScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof StorageMenu menu) {
            menu.refresh();
        }
    }

    // ---------------- commands (sent silently, the player never types them) ----------------

    private static void run(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        lastSent = System.currentTimeMillis();
        mc.player.connection.sendCommand(command);
    }

    public static void sync() {
        run("function istore:sync");
    }

    public static void depositAll() {
        run("function istore:deposit_all");
        sync();
    }

    public static void withdraw(String id, int amount) {
        // show the new number right away; the sync reply corrects it if the server disagrees
        counts.computeIfPresent(id, (k, v) -> v - amount <= 0 ? null : v - amount);
        run("function istore:withdraw {id:\"" + id + "\",count:" + amount + "}");
        sync();
    }
}
