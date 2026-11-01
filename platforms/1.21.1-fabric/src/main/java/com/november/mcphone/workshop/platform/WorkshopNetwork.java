// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.workshop.crafting.CraftingService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class WorkshopNetwork {
    private WorkshopNetwork() {}
    public static void register() {
        PayloadTypeRegistry.playC2S().register(OpenCraftingPayload.TYPE, OpenCraftingPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenCraftingPayload.TYPE,
            (packet, context) -> context.server().execute(() -> CraftingService.open(context.player())));
    }
}
