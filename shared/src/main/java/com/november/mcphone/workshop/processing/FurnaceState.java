// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.processing;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.november.mcphone.workshop.platform.ProcessingPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FurnaceState extends MachineState {
    public record Recipe(String key, ItemStack result, int ticks, float experience) {}
    private int burn, burnDuration, progress, duration = 200;
    private ItemStack cookingInput = ItemStack.EMPTY;
    private String recipeKey = "";
    private final Map<String, Double> experience = new LinkedHashMap<>();
    public FurnaceState() { super(3); }
    @Override public void tick(ServerPlayer player) { tick(ProcessingContext.of(player)); }
    public void tick(ProcessingContext context) {
        boolean wasActive = burn > 0 || progress > 0;
        if (burn > 0) burn--;
        Recipe recipe = context.smelting(getItem(0));
        String key = recipe == null ? "" : recipe.key();
        if (!ProcessingPlatform.same(cookingInput, getItem(0)) || !recipeKey.equals(key)) {
            progress = 0;
            cookingInput = getItem(0).copy();
            recipeKey = key;
        }
        duration = recipe == null ? 200 : Math.max(1, recipe.ticks());
        ItemStack output = getItem(2);
        boolean canCook = recipe != null && !recipe.result().isEmpty()
                && (output.isEmpty() || ProcessingPlatform.same(output, recipe.result()))
                && ProcessingTimer.accepts(output.getCount(), recipe.result().getCount(),
                        Math.min(getMaxStackSize(), recipe.result().getMaxStackSize()));
        if (burn == 0 && canCook) {
            int fuel = context.burnTime(getItem(1));
            if (fuel > 0) {
                burn = burnDuration = fuel;
                consume(1, context);
            }
        }
        progress = ProcessingTimer.cooking(progress, burn > 0, canCook);
        if (progress >= duration && canCook) {
            if (output.isEmpty()) setItem(2, recipe.result().copy());
            else output.grow(recipe.result().getCount());
            if (getItem(0).is(Items.WET_SPONGE) && getItem(1).is(Items.BUCKET)) setItem(1, new ItemStack(Items.WATER_BUCKET));
            getItem(0).shrink(1);
            experience.merge(recipe.key(), (double)recipe.experience(), Double::sum);
            progress = 0;
            setChanged();
        }
        if (wasActive || burn > 0 || progress > 0) setChanged();
    }
    public void claimExperience(ServerPlayer player) {
        Map<String, Double> earned = new LinkedHashMap<>(experience);
        experience.clear();
        setChanged();
        for (var entry : earned.entrySet()) {
            double value = entry.getValue();
            int points = (int)Math.floor(value);
            if (player.getRandom().nextDouble() < value - points) points++;
            net.minecraft.world.entity.ExperienceOrb.award((net.minecraft.server.level.ServerLevel)player.level(), player.position(), points);
            ProcessingPlatform.awardRecipe(player, entry.getKey(), java.util.List.of(getItem(0), getItem(1), getItem(2)));
        }
    }
    @Override public ContainerData data() {
        return new ContainerData() {
            public int get(int index) { return switch (index) { case 0 -> burn; case 1 -> burnDuration; case 2 -> progress; case 3 -> duration; default -> 0; }; }
            public void set(int index, int value) {} // Authoritative state never accepts client progress.
            public int getCount() { return 4; }
        };
    }
    @Override protected void writeProgress(JsonObject json, DynamicOps<JsonElement> ops) {
        json.addProperty("burn", burn); json.addProperty("burnDuration", burnDuration);
        json.addProperty("progress", progress); json.addProperty("duration", duration);
        json.addProperty("recipe", recipeKey); json.add("input", encode(cookingInput, ops));
        JsonObject xp = new JsonObject(); experience.forEach(xp::addProperty); json.add("experience", xp);
    }
    @Override protected void readProgress(JsonObject json, DynamicOps<JsonElement> ops) {
        burn = number(json, "burn", Integer.MAX_VALUE); burnDuration = number(json, "burnDuration", Integer.MAX_VALUE);
        duration = number(json, "duration", Integer.MAX_VALUE);
        if (duration == 0 || burn > burnDuration) throw new IllegalArgumentException("Invalid furnace duration");
        progress = number(json, "progress", duration - 1);
        recipeKey = json.get("recipe").getAsString(); cookingInput = decode(json.get("input"), ops);
        experience.clear();
        for (var entry : json.getAsJsonObject("experience").entrySet()) {
            double value = entry.getValue().getAsDouble();
            if (!Double.isFinite(value) || value < 0 || value > Integer.MAX_VALUE - 1) throw new IllegalArgumentException("Invalid furnace experience");
            experience.put(entry.getKey(), value);
        }
    }
}
