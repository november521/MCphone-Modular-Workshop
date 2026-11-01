// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.november.mcphone.api.cost.IAppPriceProvider;
import com.november.mcphone.api.cost.ICost;
import com.november.mcphone.workshop.crafting.CraftingPurchaseCost;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

public final class WorkshopPrices implements IAppPriceProvider {
    @Override public Map<ResourceLocation, ICost> prices() {
        return Map.of(ResourceLocation.parse("mcphone_workshop:crafting"), new CraftingPurchaseCost());
    }
}
