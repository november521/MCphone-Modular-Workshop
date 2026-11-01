// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.crafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class CraftingAccess {
    private CraftingAccess() {}
    private static boolean isDevice(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return id.equals("mcphone:phone") || id.equals("mcphone:tablet");
    }
    public static boolean canUse(Player player) {
        if (!player.isAlive() || player.isSpectator()) return false;
        if (isDevice(player.getOffhandItem())) return true;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (isDevice(player.getInventory().getItem(slot))) return true;
        }
        return false;
    }
}
