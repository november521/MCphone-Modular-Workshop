// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.workshop.ModularWorkshop;
import net.minecraftforge.fml.common.Mod;

@Mod(ModularWorkshop.MOD_ID)
public final class WorkshopForge {
    public WorkshopForge() {
        ModularWorkshop.initialize();
    }
}
