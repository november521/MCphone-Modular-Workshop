// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Native advancement persistence survives reconnects, death and dimension changes. No client trust. */
public final class PurchaseAccess {
    private static final ResourceLocation ID = new ResourceLocation("mcphone_workshop", "crafting_purchase");
    private PurchaseAccess() {}
    public static boolean ready(ServerPlayer player) {
        var advancement = player.level().getServer().getAdvancements().getAdvancement(ID);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).getCriterion("purchased") != null;
    }
    public static boolean has(ServerPlayer player) {
        var advancement = player.level().getServer().getAdvancements().getAdvancement(ID);
        if (advancement == null) return false;
        var criterion = player.getAdvancements().getOrStartProgress(advancement).getCriterion("purchased");
        return criterion != null && criterion.isDone();
    }
    public static void grant(ServerPlayer player) {
        var advancement = player.level().getServer().getAdvancements().getAdvancement(ID);
        if (advancement == null) throw new IllegalStateException("Crafting purchase advancement is missing");
        player.getAdvancements().award(advancement, "purchased");
    }
}
