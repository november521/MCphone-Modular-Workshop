// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.processing;

import com.november.mcphone.workshop.platform.ProcessingPlatform;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Native operations are isolated so inventory transitions can also be checked without a live world. */
public interface ProcessingContext {
    FurnaceState.Recipe smelting(ItemStack input);
    int burnTime(ItemStack fuel);
    boolean brewingFuel(ItemStack fuel);
    boolean canBrew(ItemStack bottle, ItemStack ingredient);
    ItemStack mix(ItemStack bottle, ItemStack ingredient);
    ItemStack remainder(ItemStack consumed);
    void returnRemainder(ItemStack remainder);
    boolean cancelBrew(NonNullList<ItemStack> items);
    void brewed(NonNullList<ItemStack> items);
    static ProcessingContext of(ServerPlayer player) {
        return new ProcessingContext() {
            public FurnaceState.Recipe smelting(ItemStack input) { return ProcessingPlatform.smelting(player, input); }
            public int burnTime(ItemStack fuel) { return ProcessingPlatform.burnTime(player, fuel); }
            public boolean brewingFuel(ItemStack fuel) { return ProcessingPlatform.brewingFuel(fuel); }
            public boolean canBrew(ItemStack bottle, ItemStack ingredient) { return ProcessingPlatform.canBrew(player, bottle, ingredient); }
            public ItemStack mix(ItemStack bottle, ItemStack ingredient) { return ProcessingPlatform.mix(player, bottle, ingredient); }
            public ItemStack remainder(ItemStack consumed) { return ProcessingPlatform.remainder(consumed); }
            public void returnRemainder(ItemStack remainder) { if (!player.getInventory().add(remainder)) player.drop(remainder, false); }
            public boolean cancelBrew(NonNullList<ItemStack> items) { return ProcessingPlatform.cancelBrew(items); }
            public void brewed(NonNullList<ItemStack> items) { ProcessingPlatform.brewed(items); }
        };
    }
}
