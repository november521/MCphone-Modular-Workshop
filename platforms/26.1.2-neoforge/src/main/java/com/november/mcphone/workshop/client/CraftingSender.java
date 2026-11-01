// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.client;

public final class CraftingSender {
    private CraftingSender() {}
    public static void open() {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(com.november.mcphone.workshop.platform.OpenCraftingPayload.INSTANCE);
    }
}
