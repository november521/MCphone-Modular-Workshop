// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.processing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.november.mcphone.workshop.platform.ProcessingPlatform;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

public final class BrewingState extends MachineState {
    private int remaining, fuel;
    private ItemStack ingredient = ItemStack.EMPTY;
    public BrewingState() { super(5); }
    @Override public void tick(ServerPlayer player) { tick(ProcessingContext.of(player)); }
    public void tick(ProcessingContext context) {
        if (fuel == 0 && context.brewingFuel(getItem(4))) {
            fuel = 20; getItem(4).shrink(1); setChanged();
        }
        boolean brewable = false;
        for (int slot = 0; slot < 3; slot++) brewable |= context.canBrew(getItem(slot), getItem(3));
        if (remaining > 0) {
            remaining--;
            if (!brewable || !ProcessingPlatform.same(ingredient, getItem(3))) remaining = 0;
            else if (remaining == 0) {
                NonNullList<ItemStack> stacks = NonNullList.withSize(5, ItemStack.EMPTY);
                for (int i = 0; i < 5; i++) stacks.set(i, getItem(i).copy());
                boolean canceled = context.cancelBrew(stacks);
                if (!canceled) {
                    for (int i = 0; i < 3; i++) stacks.set(i, context.mix(stacks.get(i), stacks.get(3)));
                    context.brewed(stacks);
                }
                // Loader cancellation can replace stacks itself; preserve those explicit changes
                // without also performing vanilla ingredient consumption or a second Post event.
                for (int i = 0; i < 5; i++) setItem(i, stacks.get(i));
                if (!canceled) consume(3, context);
            }
            setChanged();
        } else if (brewable && fuel > 0) {
            fuel--; remaining = 400; ingredient = getItem(3).copy(); setChanged();
        }
    }
    @Override public ContainerData data() {
        return new ContainerData() {
            public int get(int index) { return index == 0 ? remaining : index == 1 ? fuel : 0; }
            public void set(int index, int value) {}
            public int getCount() { return 2; }
        };
    }
    @Override protected void writeProgress(JsonObject json, DynamicOps<JsonElement> ops) {
        json.addProperty("remaining", remaining); json.addProperty("fuel", fuel); json.add("ingredient", encode(ingredient, ops));
    }
    @Override protected void readProgress(JsonObject json, DynamicOps<JsonElement> ops) {
        remaining = number(json, "remaining", 400); fuel = number(json, "fuel", 20);
        ingredient = decode(json.get("ingredient"), ops);
        for (int i = 0; i < 3; i++) if (getItem(i).getCount() > 1) throw new IllegalArgumentException("Stacked brewing bottles");
    }
}
