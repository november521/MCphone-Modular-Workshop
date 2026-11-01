// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.workshop.workstation.WorkstationService;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WorkshopNetwork {
    private WorkshopNetwork() {}
    public record OpenWorkstation(int workstation) {}
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("mcphone_workshop", "main"), () -> "2", "2"::equals, "2"::equals);
    public static void register() {
        CHANNEL.messageBuilder(OpenWorkstation.class, 0, NetworkDirection.PLAY_TO_SERVER)
            .encoder((packet, buffer) -> buffer.writeVarInt(packet.workstation()))
            .decoder(buffer -> new OpenWorkstation(buffer.readVarInt()))
            .consumerMainThread((packet, context) -> {
                var player = context.get().getSender();
                if (player != null) WorkstationService.open(player, packet.workstation());
                context.get().setPacketHandled(true);
            }).add();
    }
}
