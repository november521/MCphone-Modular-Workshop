// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.workshop.crafting.CraftingService;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WorkshopNetwork {
    private WorkshopNetwork() {}
    public record OpenCrafting() {}
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("mcphone_workshop", "main"), () -> "1", "1"::equals, "1"::equals);
    public static void register() {
        CHANNEL.messageBuilder(OpenCrafting.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder((packet, buffer) -> {})
            .decoder(buffer -> new OpenCrafting())
            .consumerMainThread((packet, context) -> {
                var player = context.get().getSender();
                if (player != null) CraftingService.open(player);
                context.get().setPacketHandled(true);
            }).add();
    }
}
