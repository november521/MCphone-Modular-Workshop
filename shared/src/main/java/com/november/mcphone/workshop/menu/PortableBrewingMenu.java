// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.menu;

import com.november.mcphone.workshop.processing.BrewingState;
import com.november.mcphone.workshop.processing.ProcessingStorage;
import com.november.mcphone.workshop.workstation.Workstation;
import com.november.mcphone.workshop.workstation.WorkstationAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import java.util.UUID;

public final class PortableBrewingMenu extends BrewingStandMenu {
    private final UUID owner;
    public PortableBrewingMenu(int id, Inventory inventory, BrewingState state) {
        super(id, inventory, state, state.data());
        owner = inventory.player.getUUID();
    }
    @Override public boolean stillValid(Player player) { return owner.equals(player.getUUID()) && WorkstationAccess.canUse(player, Workstation.BREWING); }
    @Override public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) ProcessingStorage.save(serverPlayer);
    }
}
