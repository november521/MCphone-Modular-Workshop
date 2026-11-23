// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.client;
public final class EnderChestSender {
    private EnderChestSender() {}
    public static void open() { net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new com.november.mcphone.workshop.platform.OpenEnderChestPayload()); }
}
