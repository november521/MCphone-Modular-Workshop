// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.workshop.workstation.WorkstationService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class WorkshopNetwork {
    private WorkshopNetwork() {}
    public static void register() {
        PayloadTypeRegistry.playC2S().register(OpenWorkstationPayload.TYPE, OpenWorkstationPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenWorkstationPayload.TYPE,
            (packet, context) -> context.server().execute(() -> WorkstationService.open(context.player(), packet.workstation())));
    }
}
