// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop;

import com.november.mcphone.api.MCphoneApi;
import org.slf4j.LoggerFactory;

/** Common bootstrap; must remain safe to load on a dedicated server. */
public final class ModularWorkshop {
    public static final String MOD_ID = "mcphone_workshop";

    private ModularWorkshop() {}

    public static void initialize() {
        LoggerFactory.getLogger(MOD_ID).info("MCphone: Modular Workshop initialized (MCphone API {})",
                MCphoneApi.VERSION);
    }
}
