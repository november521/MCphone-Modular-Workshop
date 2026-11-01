// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.api.cost.IAppPriceProvider;
import com.november.mcphone.api.cost.ICost;
import com.november.mcphone.workshop.workstation.Workstation;
import com.november.mcphone.workshop.workstation.WorkstationPurchaseCost;
import net.minecraft.resources.Identifier;
import java.util.Map;
import java.util.LinkedHashMap;

public final class WorkshopPrices implements IAppPriceProvider {
    @Override public Map<Identifier, ICost> prices() {
        Map<Identifier, ICost> prices = new LinkedHashMap<>();
        for (Workstation workstation : Workstation.values()) {
            prices.put(Identifier.parse(workstation.appId()), new WorkstationPurchaseCost(workstation));
        }
        return prices;
    }
}
