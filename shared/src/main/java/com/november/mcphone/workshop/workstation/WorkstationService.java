// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.workstation;

import com.november.mcphone.workshop.crafting.PortableCraftingMenu;
import com.november.mcphone.workshop.menu.PortableAnvilMenu;
import com.november.mcphone.workshop.menu.PortableSmithingMenu;
import com.november.mcphone.workshop.menu.PortableStonecutterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class WorkstationService {
    private WorkstationService() {}
    /** Called on the server thread; the client supplies only a bounded station selector. */
    public static void open(ServerPlayer player, int networkId) {
        Workstation workstation = Workstation.fromNetworkId(networkId);
        if (workstation == null) return;
        if (!WorkstationAccess.canUse(player, workstation)) {
            player.sendSystemMessage(Component.translatable("message.mcphone_workshop.purchase_required",
                    Component.translatable(workstation.titleKey())));
            return;
        }
        if (isCurrentMenu(player.containerMenu, workstation)) return;
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> createMenu(id, inventory, workstation),
                Component.translatable(workstation.titleKey())));
    }
    private static AbstractContainerMenu createMenu(int id, Inventory inventory, Workstation workstation) {
        return switch (workstation) {
            case CRAFTING -> new PortableCraftingMenu(id, inventory);
            case SMITHING -> new PortableSmithingMenu(id, inventory);
            case ANVIL -> new PortableAnvilMenu(id, inventory);
            case STONECUTTER -> new PortableStonecutterMenu(id, inventory);
        };
    }
    private static boolean isCurrentMenu(AbstractContainerMenu menu, Workstation workstation) {
        return switch (workstation) {
            case CRAFTING -> menu instanceof PortableCraftingMenu;
            case SMITHING -> menu instanceof PortableSmithingMenu;
            case ANVIL -> menu instanceof PortableAnvilMenu;
            case STONECUTTER -> menu instanceof PortableStonecutterMenu;
        };
    }
}
