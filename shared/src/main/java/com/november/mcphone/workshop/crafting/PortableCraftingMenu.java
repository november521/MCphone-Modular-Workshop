// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.crafting;

import com.november.mcphone.workshop.workstation.Workstation;
import com.november.mcphone.workshop.workstation.WorkstationAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

/** Uses the vanilla menu type so the client opens its unmodified crafting screen. */
public final class PortableCraftingMenu extends CraftingMenu {
    public PortableCraftingMenu(int id, Inventory inventory) {
        // A real server context is required: NULL skips crafting/cleanup in older versions.
        super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
    }
    @Override public boolean stillValid(Player player) {
        return WorkstationAccess.canUse(player, Workstation.CRAFTING);
    }
}
