// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.client;

public final class CraftingSender {
    private CraftingSender() {}
    public static void open() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(com.november.mcphone.workshop.platform.OpenCraftingPayload.INSTANCE);
    }
}
