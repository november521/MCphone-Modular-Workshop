// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.menu;

import com.november.mcphone.workshop.workstation.Workstation;
import com.november.mcphone.workshop.workstation.WorkstationAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CartographyTableMenu;

public final class PortableCartographyMenu extends CartographyTableMenu {
    public PortableCartographyMenu(int id, Inventory inventory) {
        super(id, inventory, ContainerLevelAccess.create(inventory.player.level(), inventory.player.blockPosition()));
    }
    @Override public boolean stillValid(Player player) {
        return WorkstationAccess.canUse(player, Workstation.CARTOGRAPHY);
    }
}
