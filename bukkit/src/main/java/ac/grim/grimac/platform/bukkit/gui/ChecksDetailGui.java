package ac.grim.grimac.platform.bukkit.gui;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.checks.CheckCategory;
import ac.grim.grimac.manager.CheckManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChecksDetailGui implements InventoryHolder {
    private final Inventory inventory;
    private final Player player;
    private final CheckCategory category;
    private final List<Class<? extends AbstractCheck>> checkClasses;

    public ChecksDetailGui(Player player, CheckCategory category) {
        this.player = player;
        this.category = category;
        this.checkClasses = new ArrayList<>();

        // Collect all check classes in this category
        for (Map.Entry<Class<? extends AbstractCheck>, CheckCategory> entry :
                CheckManager.getCategoryMap().entrySet()) {
            if (entry.getValue() == category) {
                checkClasses.add(entry.getKey());
            }
        }
        // Sort by name
        checkClasses.sort(Comparator.comparing(ChecksCategoryGui::getCheckName));

        int size = Math.min(54, ((checkClasses.size() / 9) + 2) * 9); // Round up to nearest 9, min 18
        size = Math.max(size, 18);
        String title = ChatColor.DARK_GRAY + "Grim " + ChatColor.WHITE + category.getDisplayName();
        this.inventory = Bukkit.createInventory(this, size, title);
        populate();
    }

    private void populate() {
        inventory.clear();
        Set<String> runtimeDisabled = ChecksCategoryGui.getRuntimeDisabledChecks();

        for (int i = 0; i < checkClasses.size() && i < inventory.getSize() - 9; i++) {
            Class<? extends AbstractCheck> clazz = checkClasses.get(i);
            String name = ChecksCategoryGui.getCheckName(clazz);
            boolean disabled = runtimeDisabled.contains(name.toLowerCase(Locale.ROOT));

            ItemStack item = new ItemStack(disabled ? Material.RED_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((disabled ? ChatColor.RED + "\u2717 " : ChatColor.GREEN + "\u2713 ") + name);

            List<String> lore = new ArrayList<>();
            lore.add(disabled
                    ? ChatColor.RED + "DISABLED (temporary)"
                    : ChatColor.GREEN + "ENABLED");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "Click to toggle");

            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }

        // Back button in last row
        int backSlot = inventory.getSize() - 5;
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(ChatColor.GRAY + "Back");
        back.setItemMeta(backMeta);
        inventory.setItem(backSlot, back);
    }

    public void handleClick(int slot) {
        int backSlot = inventory.getSize() - 5;
        if (slot == backSlot) {
            new ChecksCategoryGui(player).open();
            return;
        }
        if (slot < 0 || slot >= checkClasses.size()) return;

        Class<? extends AbstractCheck> clazz = checkClasses.get(slot);
        String name = ChecksCategoryGui.getCheckName(clazz);
        Set<String> runtimeDisabled = ChecksCategoryGui.getRuntimeDisabledChecks();

        String key = name.toLowerCase(Locale.ROOT);
        if (runtimeDisabled.contains(key)) {
            runtimeDisabled.remove(key);
        } else {
            runtimeDisabled.add(key);
        }

        ChecksCategoryGui.applyRuntimeOverrides();
        populate();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
