// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.workstation;

import com.november.mcphone.workshop.platform.PurchaseAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WorkstationAccess {
    private WorkstationAccess() {}
    private static boolean isDevice(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return id.equals("mcphone:phone") || id.equals("mcphone:tablet");
    }
    public static boolean canUse(Player player, Workstation workstation) {
        return player instanceof ServerPlayer serverPlayer && hasDevice(player)
                && PurchaseAccess.has(serverPlayer, workstation);
    }
    public static boolean hasDevice(Player player) {
        if (!player.isAlive() || player.isSpectator()) return false;
        if (isDevice(player.getOffhandItem())) return true;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isDevice(player.getInventory().getItem(slot))) return true;
        }
        return false;
    }
}
