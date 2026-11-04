// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.workstation;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** Stable wire IDs and purchase identities. Never reorder or reuse these numeric IDs. */
public enum Workstation {
    CRAFTING(0, "crafting", "crafting_table_top"),
    SMITHING(1, "smithing", "smithing_table_top"),
    ANVIL(2, "anvil", "anvil_top"),
    STONECUTTER(3, "stonecutter", "stonecutter_top");

    private final int networkId;
    private final String path;
    private final String texture;
    Workstation(int networkId, String path, String texture) {
        this.networkId = networkId;
        this.path = path;
        this.texture = texture;
    }
    public int networkId() { return networkId; }
    public String path() { return path; }
    public String appId() { return "mcphone_workshop:" + path; }
    public String receiptId() { return appId() + "_purchase"; }
    public String titleKey() { return "app.mcphone_workshop." + path; }
    public String iconTexture() { return this == CRAFTING ? "minecraft:textures/block/" + texture + ".png"
                : "mcphone_workshop:textures/app/" + path + ".png"; }
    public ItemLike purchaseItem() {
        return switch (this) {
            case CRAFTING -> Items.CRAFTING_TABLE;
            case SMITHING -> Items.SMITHING_TABLE;
            case ANVIL -> Items.ANVIL;
            case STONECUTTER -> Items.STONECUTTER;
        };
    }
    public static Workstation fromNetworkId(int id) {
        return switch (id) {
            case 0 -> CRAFTING;
            case 1 -> SMITHING;
            case 2 -> ANVIL;
            case 3 -> STONECUTTER;
            default -> null;
        };
    }
}
