// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.processing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.november.mcphone.workshop.platform.ProcessingPlatform;
import com.mojang.serialization.DynamicOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

public abstract class MachineState extends SimpleContainer {
    public boolean dirty;
    protected MachineState(int slots) { super(slots); }
    @Override public void setChanged() { super.setChanged(); dirty = true; }
    public abstract void tick(ServerPlayer player);
    public abstract ContainerData data();
    protected abstract void writeProgress(JsonObject json, DynamicOps<JsonElement> ops);
    protected abstract void readProgress(JsonObject json, DynamicOps<JsonElement> ops);
    public final JsonObject write(DynamicOps<JsonElement> ops) {
        JsonObject json = new JsonObject();
        JsonArray slots = new JsonArray();
        for (int i = 0; i < getContainerSize(); i++) slots.add(encode(getItem(i), ops));
        json.add("slots", slots);
        writeProgress(json, ops);
        return json;
    }
    public final void read(JsonObject json, DynamicOps<JsonElement> ops) {
        JsonArray slots = json.getAsJsonArray("slots");
        if (slots.size() != getContainerSize()) throw new IllegalArgumentException("Wrong machine inventory size");
        for (int i = 0; i < slots.size(); i++) setItem(i, decode(slots.get(i), ops));
        readProgress(json, ops);
        dirty = false;
    }
    protected static JsonElement encode(ItemStack stack, DynamicOps<JsonElement> ops) {
        return stack.isEmpty() ? com.google.gson.JsonNull.INSTANCE : ProcessingPlatform.encode(stack, ops);
    }
    protected static ItemStack decode(JsonElement json, DynamicOps<JsonElement> ops) {
        if (json.isJsonNull()) return ItemStack.EMPTY;
        ItemStack stack = ProcessingPlatform.decode(json, ops);
        if (stack.getCount() <= 0 || stack.getCount() > stack.getMaxStackSize()) throw new IllegalArgumentException("Invalid machine stack size");
        return stack;
    }
    protected static int number(JsonObject json, String key, int max) {
        int value = json.get(key).getAsBigDecimal().intValueExact();
        if (value < 0 || value > max) throw new IllegalArgumentException("Invalid machine field: " + key);
        return value;
    }
    protected final void consume(int slot, ProcessingContext context) {
        ItemStack ingredient = getItem(slot);
        ItemStack remainder = context.remainder(ingredient).copy();
        ingredient.shrink(1);
        if (!remainder.isEmpty()) {
            if (ingredient.isEmpty()) setItem(slot, remainder);
            else context.returnRemainder(remainder);
        }
        setChanged();
    }
}
