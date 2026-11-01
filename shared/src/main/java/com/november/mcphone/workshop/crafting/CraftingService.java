// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.crafting;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public final class CraftingService {
    private CraftingService() {}
    /** Called only on the server thread. No client inventory or recipe data is accepted. */
    public static void open(ServerPlayer player) {
        if (!CraftingAccess.canUse(player) || player.containerMenu instanceof PortableCraftingMenu) return;
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new PortableCraftingMenu(id, inventory),
                Component.translatable("container.crafting")));
    }
}
