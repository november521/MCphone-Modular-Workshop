// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.ender;
import com.november.mcphone.api.cost.ICost;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

public final class EnderChestCost implements ICost {
    private final ICost item = ICost.of(Items.ENDER_CHEST, 1);
    public Component describe() { return item.describe(); }
    public boolean canAfford(Player player) {
        if (!EnderChestAccess.active()) return false;
        try { return player instanceof ServerPlayer server && EnderChestAccess.purchased(server) || item.canAfford(player); }
        catch (RuntimeException e) { return false; }
    }
    public boolean consume(Player player) {
        if (!(player instanceof ServerPlayer server) || !EnderChestAccess.active()) return false;
        try {
            if (!EnderChestAccess.device(server, ItemStack.EMPTY)) return false;
            // The MCphone purchase transaction writes its existing world-scoped receipt after success.
            // A lookup failure rejects the purchase; it must never be treated as 'not yet purchased'.
            return EnderChestAccess.purchased(server) || item.consume(server);
        } catch (RuntimeException e) { return false; }
    }
}
