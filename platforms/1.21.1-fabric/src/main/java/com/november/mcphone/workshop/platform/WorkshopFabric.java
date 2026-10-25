// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.workshop.ModularWorkshop;
import net.fabricmc.api.ModInitializer;

public final class WorkshopFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ModularWorkshop.initialize();
    }
}
