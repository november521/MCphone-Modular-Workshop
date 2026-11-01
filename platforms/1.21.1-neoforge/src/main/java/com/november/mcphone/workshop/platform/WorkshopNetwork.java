// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.workshop.crafting.CraftingService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.server.level.ServerPlayer;

public final class WorkshopNetwork {
    private WorkshopNetwork() {}
    public static void register(IEventBus bus) { bus.addListener(WorkshopNetwork::payloads); }
    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(OpenCraftingPayload.TYPE, OpenCraftingPayload.CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) CraftingService.open(player);
            }));
    }
}
