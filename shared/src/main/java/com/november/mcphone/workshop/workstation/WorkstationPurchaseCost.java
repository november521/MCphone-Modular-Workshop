// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.workstation;

import com.november.mcphone.api.cost.ICost;
import com.november.mcphone.workshop.platform.PurchaseAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** The host purchases the App; the receipt separately authorizes our server endpoint. */
public final class WorkstationPurchaseCost implements ICost {
    private final Workstation workstation;
    private final ICost itemCost;
    public WorkstationPurchaseCost(Workstation workstation) {
        this.workstation = workstation;
        this.itemCost = ICost.of(workstation.purchaseItem(), 1);
    }
    @Override public Component describe() { return itemCost.describe(); }
    @Override public boolean canAfford(Player player) {
        return (player instanceof ServerPlayer serverPlayer && PurchaseAccess.has(serverPlayer, workstation))
                || itemCost.canAfford(player);
    }
    @Override public boolean consume(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !WorkstationAccess.hasDevice(player)) return false;
        if (PurchaseAccess.has(serverPlayer, workstation)) return true;
        // Validate the independent receipt BEFORE deducting the corresponding block.
        if (!PurchaseAccess.ready(serverPlayer, workstation) || !itemCost.consume(player)) return false;
        PurchaseAccess.grant(serverPlayer, workstation);
        return true;
    }
}
