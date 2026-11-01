// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.client;

public final class CraftingSender {
    private CraftingSender() {}
    public static void open() {
        com.november.mcphone.workshop.platform.WorkshopNetwork.CHANNEL.sendToServer(new com.november.mcphone.workshop.platform.WorkshopNetwork.OpenCrafting());
    }
}
