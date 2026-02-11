package ac.grim.grimac.platform.bukkit.gui;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckCategory;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.SetbackMode;
import ac.grim.grimac.manager.CheckManager;
import ac.grim.grimac.player.GrimPlayer;
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

public class ChecksCategoryGui implements InventoryHolder {
    public static final String TITLE = ChatColor.DARK_GRAY + "Grim " + ChatColor.WHITE + "Check Categories";
    private final Inventory inventory;
    private final Player player;

    // Runtime overrides: category -> mode (null = use config default)
    private static final Map<CheckCategory, SetbackMode> runtimeCategoryModes =
            Collections.synchronizedMap(new EnumMap<>(CheckCategory.class));
    // Runtime disabled individual checks
    private static final Set<String> runtimeDisabledChecks =
            Collections.synchronizedSet(new HashSet<>());

    public ChecksCategoryGui(Player player) {
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, TITLE);
        populate();
    }

    private void populate() {
        inventory.clear();

        // Global mode info item at slot 49
        SetbackMode globalMode = GrimAPI.INSTANCE.getConfigManager().getGlobalSetbackMode();
        ItemStack globalItem = new ItemStack(Material.COMPASS);
        ItemMeta globalMeta = globalItem.getItemMeta();
        ChatColor globalColor = switch (globalMode) {
            case SETBACK -> ChatColor.GREEN;
            case ALERT_ONLY -> ChatColor.YELLOW;
            case DISABLED -> ChatColor.RED;
        };
        globalMeta.setDisplayName(ChatColor.WHITE + "Global Setback Mode");
        globalMeta.setLore(List.of(
                "",
                ChatColor.GRAY + "Mode: " + globalColor + globalMode.name(),
                "",
                ChatColor.DARK_GRAY + "Categories set to 'inherit'",
                ChatColor.DARK_GRAY + "will use this mode."
        ));
        globalItem.setItemMeta(globalMeta);
        inventory.setItem(49, globalItem);

        CheckCategory[] categories = CheckCategory.values();
        for (int i = 0; i < categories.length; i++) {
            CheckCategory cat = categories[i];
            SetbackMode mode = getEffectiveMode(cat);
            inventory.setItem(i, createCategoryItem(cat, mode));
        }
        // Close button at slot 53
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Close");
        close.setItemMeta(closeMeta);
        inventory.setItem(53, close);
    }

    private ItemStack createCategoryItem(CheckCategory category, SetbackMode mode) {
        Material material;
        ChatColor color;
        switch (mode) {
            case SETBACK -> { material = Material.LIME_STAINED_GLASS_PANE; color = ChatColor.GREEN; }
            case ALERT_ONLY -> { material = Material.YELLOW_STAINED_GLASS_PANE; color = ChatColor.YELLOW; }
            case DISABLED -> { material = Material.RED_STAINED_GLASS_PANE; color = ChatColor.RED; }
            default -> { material = Material.LIME_STAINED_GLASS_PANE; color = ChatColor.GREEN; }
        }

        // Try to use the category's icon material, fall back to colored glass pane
        try {
            Material icon = Material.valueOf(category.getIconMaterial());
            material = icon;
        } catch (IllegalArgumentException ignored) {}

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color + category.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Current mode: " + color + mode.name());

        // Show the source of the current config mode
        boolean configEnabled = GrimAPI.INSTANCE.getConfigManager()
                .getCategoryEnabled().getOrDefault(category, true);
        boolean hasExplicitMode = GrimAPI.INSTANCE.getConfigManager()
                .getCategorySetbackMode().containsKey(category);
        SetbackMode globalMode = GrimAPI.INSTANCE.getConfigManager().getGlobalSetbackMode();

        if (!configEnabled) {
            lore.add(ChatColor.DARK_GRAY + "Config: DISABLED (category disabled)");
        } else if (hasExplicitMode) {
            SetbackMode explicitMode = GrimAPI.INSTANCE.getConfigManager()
                    .getCategorySetbackMode().get(category);
            lore.add(ChatColor.DARK_GRAY + "Config: " + explicitMode.name() + " (explicit)");
        } else {
            lore.add(ChatColor.DARK_GRAY + "Config: " + globalMode.name() + " (from global)");
        }

        if (runtimeCategoryModes.containsKey(category)) {
            lore.add(ChatColor.GOLD + "Runtime override active");
        }

        // Count checks in this category
        int total = 0, disabled = 0;
        for (Map.Entry<Class<? extends AbstractCheck>, CheckCategory> entry :
                CheckManager.getCategoryMap().entrySet()) {
            if (entry.getValue() == category) {
                total++;
                String name = getCheckName(entry.getKey());
                if (name != null && runtimeDisabledChecks.contains(name.toLowerCase(Locale.ROOT))) {
                    disabled++;
                }
            }
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Checks: " + ChatColor.WHITE + total +
                (disabled > 0 ? ChatColor.RED + " (" + disabled + " disabled)" : ""));
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Left-click: Cycle mode");
        lore.add(ChatColor.DARK_GRAY + "Right-click: View sub-checks");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void handleClick(int slot, boolean isRightClick) {
        CheckCategory[] categories = CheckCategory.values();
        if (slot == 53) {
            player.closeInventory();
            return;
        }
        if (slot < 0 || slot >= categories.length) return;

        CheckCategory category = categories[slot];

        if (isRightClick) {
            // Open sub-panel
            new ChecksDetailGui(player, category).open();
            return;
        }

        // Left-click: cycle mode
        SetbackMode current = getEffectiveMode(category);
        SetbackMode next = switch (current) {
            case SETBACK -> SetbackMode.ALERT_ONLY;
            case ALERT_ONLY -> SetbackMode.DISABLED;
            case DISABLED -> SetbackMode.SETBACK;
        };
        runtimeCategoryModes.put(category, next);
        applyRuntimeOverrides();
        populate();
    }

    public static SetbackMode getEffectiveMode(CheckCategory category) {
        if (runtimeCategoryModes.containsKey(category)) {
            return runtimeCategoryModes.get(category);
        }
        boolean enabled = GrimAPI.INSTANCE.getConfigManager()
                .getCategoryEnabled().getOrDefault(category, true);
        if (!enabled) return SetbackMode.DISABLED;
        // Inherit from global-setback-mode when category doesn't have explicit override
        SetbackMode globalMode = GrimAPI.INSTANCE.getConfigManager().getGlobalSetbackMode();
        return GrimAPI.INSTANCE.getConfigManager()
                .getCategorySetbackMode().getOrDefault(category, globalMode);
    }

    /**
     * Apply all runtime overrides to every online player's checks.
     * Called after any GUI change.
     */
    public static void applyRuntimeOverrides() {
        for (GrimPlayer grimPlayer : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            for (AbstractCheck check : grimPlayer.checkManager.allChecks.values()) {
                CheckCategory category = CheckManager.getCategory(check.getClass());
                if (category == null) continue;

                // Start from the punishment-manager-determined enabled state
                // We re-run the category logic on top
                if (runtimeCategoryModes.containsKey(category)) {
                    SetbackMode mode = runtimeCategoryModes.get(category);
                    if (mode == SetbackMode.DISABLED) {
                        check.setEnabled(false);
                        if (check instanceof Check c) c.setSetbackMode(SetbackMode.DISABLED);
                    } else {
                        if (check instanceof Check c) c.setSetbackMode(mode);
                    }
                }

                // Per-check runtime disable
                if (check.getCheckName() != null &&
                        runtimeDisabledChecks.contains(check.getCheckName().toLowerCase(Locale.ROOT))) {
                    check.setEnabled(false);
                    if (check instanceof Check c) c.setSetbackMode(SetbackMode.DISABLED);
                }
            }
        }
    }

    /**
     * Clear all runtime overrides (called on /grim reload).
     */
    public static void clearRuntimeOverrides() {
        runtimeCategoryModes.clear();
        runtimeDisabledChecks.clear();
    }

    public static Set<String> getRuntimeDisabledChecks() {
        return runtimeDisabledChecks;
    }

    static String getCheckName(Class<? extends AbstractCheck> clazz) {
        CheckData data = clazz.getAnnotation(CheckData.class);
        if (data != null && !data.name().equals("UNKNOWN")) return data.name();
        return clazz.getSimpleName();
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
