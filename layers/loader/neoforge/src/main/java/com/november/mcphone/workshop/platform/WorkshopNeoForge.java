// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.workshop.ModularWorkshop;
import net.neoforged.fml.common.Mod;

@Mod(ModularWorkshop.MOD_ID)
public final class WorkshopNeoForge {
    public WorkshopNeoForge(net.neoforged.bus.api.IEventBus bus) {
        ModularWorkshop.initialize();
        WorkshopNetwork.register(bus);
    }
}
