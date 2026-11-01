// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.crafting;

import com.november.mcphone.api.cost.ICost;
import com.november.mcphone.workshop.platform.PurchaseAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/** MCphone handles the purchase UI and purchased-App cache; this also authorizes our server endpoint. */
public final class CraftingPurchaseCost implements ICost {
    private final ICost itemCost = ICost.of(Items.CRAFTING_TABLE, 1);
    @Override public Component describe() { return itemCost.describe(); }
    @Override public boolean canAfford(Player player) {
        return (player instanceof ServerPlayer serverPlayer && PurchaseAccess.has(serverPlayer))
                || itemCost.canAfford(player);
    }
    @Override public boolean consume(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !CraftingAccess.hasDevice(player)) return false;
        if (PurchaseAccess.has(serverPlayer)) return true;
        // Check the persistent marker is available BEFORE touching any items.
        if (!PurchaseAccess.ready(serverPlayer) || !itemCost.consume(player)) return false;
        PurchaseAccess.grant(serverPlayer);
        return true;
    }
}
