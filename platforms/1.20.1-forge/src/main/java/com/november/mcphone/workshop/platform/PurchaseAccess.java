// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.workshop.workstation.Workstation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Independent native advancement receipts retain the existing crafting purchase ID. */
public final class PurchaseAccess {
    private PurchaseAccess() {}
    public static boolean ready(ServerPlayer player, Workstation workstation) {
        var advancement = player.level().getServer().getAdvancements().getAdvancement(new ResourceLocation(workstation.receiptId()));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).getCriterion("purchased") != null;
    }
    public static boolean has(ServerPlayer player, Workstation workstation) {
        var advancement = player.level().getServer().getAdvancements().getAdvancement(new ResourceLocation(workstation.receiptId()));
        if (advancement == null) return false;
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion("purchased");
        return criterion != null && criterion.isDone();
    }
    public static void recover(ServerPlayer player, Workstation workstation) {
        com.november.mcphone.workshop.compat.PurchaseRecovery.recover(
                () -> has(player, workstation), () -> ready(player, workstation),
                () -> com.november.mcphone.workshop.compat.LegacyPurchaseReader.hasPurchased(player, new ResourceLocation(workstation.appId())),
                () -> grant(player, workstation));
    }
    public static void grant(ServerPlayer player, Workstation workstation) {
        var advancement = player.level().getServer().getAdvancements().getAdvancement(new ResourceLocation(workstation.receiptId()));
        if (advancement == null) throw new IllegalStateException("Missing purchase receipt: " + workstation.receiptId());
        player.getAdvancements().award(advancement, "purchased");
    }
}
