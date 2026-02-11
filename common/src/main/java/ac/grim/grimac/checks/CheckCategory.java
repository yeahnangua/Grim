package ac.grim.grimac.checks;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CheckCategory {
    MOVEMENT("movement", "Movement", "LIME_STAINED_GLASS_PANE"),
    ELYTRA("elytra", "Elytra", "ELYTRA"),
    COMBAT("combat", "Combat", "DIAMOND_SWORD"),
    VEHICLE("vehicle", "Vehicle", "MINECART"),
    TIMER("timer", "Timer", "CLOCK"),
    BADPACKETS("badpackets", "BadPackets", "BARRIER"),
    BREAKING("breaking", "Breaking", "IRON_PICKAXE"),
    PLACING("placing", "Placing", "BRICKS"),
    PACKET_ORDER("packet-order", "PacketOrder", "HOPPER"),
    MULTI_ACTIONS("multi-actions", "MultiActions", "REPEATER"),
    CRASH("crash", "Crash", "TNT"),
    SPRINT("sprint", "Sprint", "LEATHER_BOOTS"),
    CHAT("chat", "Chat", "WRITABLE_BOOK");

    private final String configKey;    // key in config.yml under "checks:"
    private final String displayName;  // shown in GUI
    private final String iconMaterial; // Bukkit Material name for GUI

    public static CheckCategory fromConfigKey(String key) {
        for (CheckCategory cat : values()) {
            if (cat.configKey.equalsIgnoreCase(key)) return cat;
        }
        return null;
    }
}
