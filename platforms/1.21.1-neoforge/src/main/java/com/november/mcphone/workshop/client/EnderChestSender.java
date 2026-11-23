// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.client;
public final class EnderChestSender {
    private EnderChestSender() {}
    public static void open() { net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.november.mcphone.workshop.platform.OpenEnderChestPayload()); }
}
