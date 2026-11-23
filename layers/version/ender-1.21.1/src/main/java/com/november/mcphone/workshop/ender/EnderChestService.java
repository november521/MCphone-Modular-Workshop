// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.ender;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import java.util.UUID;

public final class EnderChestService {
    private EnderChestService() {}
    public static void open(ServerPlayer player) {
        // Do not trust installed icons, client inventory snapshots or arbitrary player IDs.
        if (!EnderChestAccess.canUse(player, ItemStack.EMPTY)) return;
        if (player.containerMenu instanceof PortableMenu) return;
        player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new PortableMenu(id, inventory, player),
                Component.translatable("mcphone.container.ender_chest")));
    }
    public static final class PortableMenu extends ChestMenu {
        private final UUID owner;
        public PortableMenu(int id, Inventory inventory, ServerPlayer player) {
            super(MenuType.GENERIC_9x3, id, inventory, player.getEnderChestInventory(), 3);
            owner = player.getUUID();
        }
        @Override public boolean stillValid(Player player) {
            return player instanceof ServerPlayer server && owner.equals(player.getUUID())
                    && EnderChestAccess.canUse(server, getCarried());
        }
    }
}
