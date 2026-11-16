// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.workstation;

import com.november.mcphone.workshop.crafting.PortableCraftingMenu;
import com.november.mcphone.workshop.menu.PortableAnvilMenu;
import com.november.mcphone.workshop.menu.PortableSmithingMenu;
import com.november.mcphone.workshop.menu.PortableStonecutterMenu;
import com.november.mcphone.workshop.platform.PurchaseAccess;
import com.november.mcphone.workshop.menu.PortableLoomMenu;
import com.november.mcphone.workshop.menu.PortableCartographyMenu;
import com.november.mcphone.workshop.menu.PortableGrindstoneMenu;
import com.november.mcphone.workshop.menu.PortableFurnaceMenu;
import com.november.mcphone.workshop.menu.PortableBrewingMenu;
import com.november.mcphone.workshop.processing.ProcessingStorage;
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
        if (!WorkstationAccess.hasDevice(player)) {
            player.sendSystemMessage(Component.translatable("message.mcphone_workshop.device_required"));
            return;
        }
        PurchaseAccess.recover(player, workstation);
        if (!WorkstationAccess.canUse(player, workstation)) {
            player.sendSystemMessage(Component.translatable("message.mcphone_workshop.purchase_required",
                    Component.translatable(workstation.titleKey())));
            return;
        }
        if (isCurrentMenu(player.containerMenu, workstation)) return;
        if (workstation == Workstation.FURNACE || workstation == Workstation.BREWING) {
            try { ProcessingStorage.get(player); }
            catch (IllegalStateException error) {
                player.sendSystemMessage(Component.translatable("message.mcphone_workshop.storage_failed"));
                return;
            }
        }
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> createMenu(id, inventory, workstation),
                Component.translatable(workstation.titleKey())));
    }
    private static AbstractContainerMenu createMenu(int id, Inventory inventory, Workstation workstation) {
        return switch (workstation) {
            case CRAFTING -> new PortableCraftingMenu(id, inventory);
            case SMITHING -> new PortableSmithingMenu(id, inventory);
            case ANVIL -> new PortableAnvilMenu(id, inventory);
            case STONECUTTER -> new PortableStonecutterMenu(id, inventory);
            case LOOM -> new PortableLoomMenu(id, inventory);
            case CARTOGRAPHY -> new PortableCartographyMenu(id, inventory);
            case GRINDSTONE -> new PortableGrindstoneMenu(id, inventory);
            case FURNACE -> new PortableFurnaceMenu(id, inventory, ProcessingStorage.get((ServerPlayer)inventory.player).furnace);
            case BREWING -> new PortableBrewingMenu(id, inventory, ProcessingStorage.get((ServerPlayer)inventory.player).brewing);
        };
    }
    private static boolean isCurrentMenu(AbstractContainerMenu menu, Workstation workstation) {
        return switch (workstation) {
            case CRAFTING -> menu instanceof PortableCraftingMenu;
            case SMITHING -> menu instanceof PortableSmithingMenu;
            case ANVIL -> menu instanceof PortableAnvilMenu;
            case STONECUTTER -> menu instanceof PortableStonecutterMenu;
            case LOOM -> menu instanceof PortableLoomMenu;
            case CARTOGRAPHY -> menu instanceof PortableCartographyMenu;
            case GRINDSTONE -> menu instanceof PortableGrindstoneMenu;
            case FURNACE -> menu instanceof PortableFurnaceMenu;
            case BREWING -> menu instanceof PortableBrewingMenu;
        };
    }
}
