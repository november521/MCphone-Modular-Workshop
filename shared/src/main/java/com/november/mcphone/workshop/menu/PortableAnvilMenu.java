// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.menu;

import com.november.mcphone.workshop.workstation.Workstation;
import com.november.mcphone.workshop.workstation.WorkstationAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

public final class PortableAnvilMenu extends AnvilMenu {
    private final AnvilLevelAccess portableAccess;
    public PortableAnvilMenu(int id, Inventory inventory) {
        this(id, inventory, new AnvilLevelAccess(ContainerLevelAccess.create(
                inventory.player.level(), inventory.player.blockPosition())));
    }
    private PortableAnvilMenu(int id, Inventory inventory, AnvilLevelAccess access) {
        super(id, inventory, access);
        this.portableAccess = access;
    }
    @Override public boolean stillValid(Player player) {
        return WorkstationAccess.canUse(player, Workstation.ANVIL);
    }
    @Override protected void onTake(Player player, ItemStack stack) {
        // Keep native XP, input consumption and loader hooks. Only skip the world-block callback.
        // In particular, never damage an actual anvil at the player's opening position.
        if (portableAccess.withoutWorldEffects(() -> super.onTake(player, stack))) {
            player.level().levelEvent(1030, player.blockPosition(), 0);
        }
    }
}
