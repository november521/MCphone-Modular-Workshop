// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.workshop.workstation.WorkstationService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.server.level.ServerPlayer;

public final class WorkshopNetwork {
    private WorkshopNetwork() {}
    public static void register(IEventBus bus) { bus.addListener(WorkshopNetwork::payloads); }
    private static void payloads(RegisterPayloadHandlersEvent event) {
        if (com.november.mcphone.workshop.ender.EnderChestAccess.active()) {
            event.registrar("ender-handoff-1").playToServer(OpenEnderChestPayload.TYPE, OpenEnderChestPayload.CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) com.november.mcphone.workshop.ender.EnderChestService.open(player);
                }));
        }

        event.registrar("2").playToServer(OpenWorkstationPayload.TYPE, OpenWorkstationPayload.CODEC,
            (packet, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) WorkstationService.open(player, packet.workstation());
            }));
    }
}
