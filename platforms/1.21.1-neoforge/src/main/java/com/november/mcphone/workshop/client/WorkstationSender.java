// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.client;

import com.november.mcphone.workshop.workstation.Workstation;

public final class WorkstationSender {
    private WorkstationSender() {}
    public static void open(Workstation workstation) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.november.mcphone.workshop.platform.OpenWorkstationPayload(workstation.networkId()));
    }
}
