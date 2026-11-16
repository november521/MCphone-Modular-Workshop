// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.menu;

import com.november.mcphone.workshop.processing.FurnaceState;
import com.november.mcphone.workshop.processing.ProcessingStorage;
import com.november.mcphone.workshop.workstation.Workstation;
import com.november.mcphone.workshop.workstation.WorkstationAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public final class PortableFurnaceMenu extends FurnaceMenu {
    private final UUID owner;
    public PortableFurnaceMenu(int id, Inventory inventory, FurnaceState state) {
        super(id, inventory, state, state.data());
        owner = inventory.player.getUUID();
        FurnaceResultSlot result = new FurnaceResultSlot(inventory.player, state, 2, 116, 35) {
            @Override public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                if (player instanceof ServerPlayer serverPlayer) state.claimExperience(serverPlayer);
            }
        };
        result.index = 2;
        slots.set(2, result);
    }
    @Override public boolean stillValid(Player player) { return owner.equals(player.getUUID()) && WorkstationAccess.canUse(player, Workstation.FURNACE); }
    @Override public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) ProcessingStorage.save(serverPlayer);
    }
}
