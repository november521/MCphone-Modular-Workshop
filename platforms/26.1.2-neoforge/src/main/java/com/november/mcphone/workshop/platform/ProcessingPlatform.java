// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.november.mcphone.workshop.processing.FurnaceState;
import net.minecraft.core.NonNullList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

/** Version-specific native recipes, registry serialization and loader hooks. */
public final class ProcessingPlatform {
    private ProcessingPlatform() {}
    public static MinecraftServer server(ServerPlayer player) { return ((ServerLevel)player.level()).getServer(); }
    public static DynamicOps<JsonElement> ops(MinecraftServer server) { return net.minecraft.resources.RegistryOps.create(JsonOps.INSTANCE, server.registryAccess()); }
    public static JsonElement encode(ItemStack stack, DynamicOps<JsonElement> ops) {
        return ItemStack.CODEC.encodeStart(ops, stack).result().orElseThrow(() -> new IllegalStateException("Cannot encode machine item"));
    }
    public static ItemStack decode(JsonElement json, DynamicOps<JsonElement> ops) {
        return ItemStack.CODEC.parse(ops, json).result().orElseThrow(() -> new IllegalArgumentException("Cannot decode machine item"));
    }
    public static boolean same(ItemStack a, ItemStack b) { return a.isEmpty() && b.isEmpty() || !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameComponents(a, b); }
    public static ItemStack remainder(ItemStack stack) { return stack.getCraftingRemainder() == null ? ItemStack.EMPTY : stack.getCraftingRemainder().create(); }
    public static int burnTime(ServerPlayer player, ItemStack stack) { return stack.isEmpty() ? 0 : stack.getBurnTime(RecipeType.SMELTING, ((ServerLevel)player.level()).fuelValues()); }
    public static boolean brewingFuel(ItemStack stack) { return stack.is(net.minecraft.tags.ItemTags.BREWING_FUEL); }
    public static FurnaceState.Recipe smelting(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return null;
        ServerLevel level = (ServerLevel)player.level();
        var input = new net.minecraft.world.item.crafting.SingleRecipeInput(stack);
        var holder = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level).orElse(null);
        return holder == null ? null : new FurnaceState.Recipe(holder.id().identifier().toString(), holder.value().assemble(input), holder.value().cookingTime(), holder.value().experience());
    }
    public static void awardRecipe(ServerPlayer player, String key, java.util.List<ItemStack> inputs) {
        ServerLevel level = (ServerLevel)player.level();
        level.recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.parse(key))).ifPresent(recipe -> { player.awardRecipes(java.util.List.of(recipe)); player.triggerRecipeCrafted(recipe, inputs); });
    }
    public static boolean canBrew(ServerPlayer player, ItemStack bottle, ItemStack ingredient) {
        return !bottle.isEmpty() && !ingredient.isEmpty() && ((ServerLevel)player.level()).potionBrewing().hasMix(bottle, ingredient);
    }
    public static ItemStack mix(ServerPlayer player, ItemStack bottle, ItemStack ingredient) {
        return ((ServerLevel)player.level()).potionBrewing().mix(ingredient, bottle).copy();
    }
    public static boolean cancelBrew(NonNullList<ItemStack> items) { return net.neoforged.neoforge.event.EventHooks.onPotionAttemptBrew(items); }
    public static void brewed(NonNullList<ItemStack> items) { net.neoforged.neoforge.event.EventHooks.onPotionBrewed(items); }
}
