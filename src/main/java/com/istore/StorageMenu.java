package com.istore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * A chest GUI that exists only on the client. It is never registered with the server: the server keeps
 * its normal inventory menu, and our fake container id makes it ignore the click packets this screen sends.
 * Clicks are turned into silent data pack commands instead.
 */
public class StorageMenu extends ChestMenu {
    private static final int CONTAINER_ID = 101; // servers only hand out 1..100
    private static final int SIZE = 54;
    private static final int PER_PAGE = 45;

    private static final String[] HINTS = {
        "Click: take a stack", "Right-click: take half a stack", "Shift-click: take 1"
    };

    private record Row(String id, Item item, String name, int count) {}

    private final Container display;
    private SortMode sort = SortMode.COUNT_DESC;
    private int page = 0;

    private StorageMenu(Inventory inventory) {
        super(MenuType.GENERIC_9x6, CONTAINER_ID, inventory, new SimpleContainer(SIZE), 6);
        this.display = getContainer();
        refresh();
    }

    public static void open(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || player.containerMenu != player.inventoryMenu) return;
        StorageMenu menu = new StorageMenu(player.getInventory());
        player.containerMenu = menu;
        mc.setScreen(new ContainerScreen(menu, player.getInventory(), Component.literal("Infinite Storage")));
        StorageData.sync();
    }

    // Only clicks on the 54 display slots do anything. Everything else (your own inventory slots,
    // hotkeys, dragging, dropping) is ignored so this fake menu can never desync your real inventory.
    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < SIZE && (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE)) {
            handle(slotId, button, type);
            refresh();
        }
    }

    private void handle(int slot, int button, ClickType type) {
        if (slot >= PER_PAGE) {
            switch (slot) {
                case 45 -> page--;
                case 46 -> page++;
                case 48 -> {
                    sort = sort.next();
                    page = 0;
                }
                case 49 -> StorageData.depositAll();
                case 50 -> StorageData.sync();
                default -> {}
            }
            return;
        }

        List<Row> rows = rows();
        int idx = page * PER_PAGE + slot;
        if (idx >= rows.size()) return;
        Row row = rows.get(idx);

        int max = Math.max(1, row.item().getDefaultMaxStackSize());
        int amount;
        if (type == ClickType.QUICK_MOVE) amount = 1;
        else if (button == 1) amount = Math.max(1, Math.min(max, row.count()) / 2);
        else amount = Math.min(max, row.count());
        StorageData.withdraw(row.id(), amount);
    }

    // ------------------------------------------------------------------ rendering

    private List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> e : StorageData.counts().entrySet()) {
            Identifier id = Identifier.tryParse(e.getKey());
            if (id == null || e.getValue() <= 0) continue;
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == Items.AIR) continue;
            rows.add(new Row(e.getKey(), item, new ItemStack(item).getHoverName().getString(), e.getValue()));
        }
        Comparator<Row> byName = Comparator.comparing(Row::name, String.CASE_INSENSITIVE_ORDER);
        switch (sort) {
            case COUNT_DESC -> rows.sort(Comparator.comparingInt(Row::count).reversed().thenComparing(byName));
            case COUNT_ASC -> rows.sort(Comparator.comparingInt(Row::count).thenComparing(byName));
            case NAME -> rows.sort(byName);
        }
        return rows;
    }

    public void refresh() {
        List<Row> rows = rows();
        int pages = Math.max(1, (rows.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));

        for (int i = 0; i < SIZE; i++) display.setItem(i, named(Items.GRAY_STAINED_GLASS_PANE, " "));

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= rows.size()) break;
            Row row = rows.get(idx);
            ItemStack s = new ItemStack(row.item());
            List<Component> lines = new ArrayList<>();
            lines.add(line("Stored: " + fmt(row.count()), ChatFormatting.GOLD));
            for (String h : HINTS) lines.add(line(h, ChatFormatting.GRAY));
            s.set(DataComponents.LORE, new ItemLore(lines));
            display.setItem(i, s);
        }

        long total = 0;
        for (Row r : rows) total += r.count();

        display.setItem(45, named(Items.ARROW, "Previous page"));
        display.setItem(46, named(Items.ARROW, "Next page"));
        display.setItem(47, named(Items.PAPER, "Page " + (page + 1) + " / " + pages));
        display.setItem(48, named(Items.COMPARATOR, "Sort: " + sort.label, "Click to change"));
        display.setItem(49, named(Items.HOPPER, "Deposit inventory",
                "Stores your main inventory (not hotbar)", "Enchanted, named and damaged items are skipped"));
        display.setItem(50, named(Items.CLOCK, "Refresh"));
        display.setItem(53, named(Items.CHEST, "Stored: " + fmt(total) + " items", rows.size() + " different types"));
    }

    private static ItemStack named(Item item, String name, String... lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name).withStyle(st -> st.withItalic(false)));
        if (lore.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String l : lore) lines.add(line(l, ChatFormatting.GRAY));
            s.set(DataComponents.LORE, new ItemLore(lines));
        }
        return s;
    }

    private static Component line(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color).withStyle(st -> st.withItalic(false));
    }

    private static String fmt(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }
}
