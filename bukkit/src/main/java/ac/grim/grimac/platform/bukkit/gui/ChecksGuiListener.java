package ac.grim.grimac.platform.bukkit.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ChecksGuiListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        if (event.getInventory().getHolder() instanceof ChecksCategoryGui gui) {
            event.setCancelled(true);
            gui.handleClick(event.getRawSlot(), event.isRightClick());
        } else if (event.getInventory().getHolder() instanceof ChecksDetailGui gui) {
            event.setCancelled(true);
            gui.handleClick(event.getRawSlot());
        }
    }
}
