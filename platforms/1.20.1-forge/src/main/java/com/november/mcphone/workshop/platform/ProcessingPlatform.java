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
    public static DynamicOps<JsonElement> ops(MinecraftServer server) { return JsonOps.INSTANCE; }
    // SNBT preserves Forge capabilities and numeric/array tag types on the legacy target.
    public static JsonElement encode(ItemStack stack, DynamicOps<JsonElement> ops) {
        return new com.google.gson.JsonPrimitive(stack.save(new net.minecraft.nbt.CompoundTag()).toString());
    }
    public static ItemStack decode(JsonElement json, DynamicOps<JsonElement> ops) {
        try { return ItemStack.of(net.minecraft.nbt.TagParser.parseTag(json.getAsString())); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new IllegalArgumentException("Invalid saved item NBT", error); }
    }
    public static boolean same(ItemStack a, ItemStack b) { return a.isEmpty() && b.isEmpty() || !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItemSameTags(a, b); }
    public static ItemStack remainder(ItemStack stack) { return stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY; }
    public static int burnTime(ServerPlayer player, ItemStack stack) { return stack.isEmpty() ? 0 : net.minecraftforge.common.ForgeHooks.getBurnTime(stack, RecipeType.SMELTING); }
    public static boolean brewingFuel(ItemStack stack) { return stack.is(Items.BLAZE_POWDER); }
    public static FurnaceState.Recipe smelting(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return null;
        ServerLevel level = (ServerLevel)player.level();
        var input = new net.minecraft.world.SimpleContainer(stack);
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level).orElse(null);
        return recipe == null ? null : new FurnaceState.Recipe(recipe.getId().toString(), recipe.assemble(input, level.registryAccess()), recipe.getCookingTime(), recipe.getExperience());
    }
    public static void awardRecipe(ServerPlayer player, String key, java.util.List<ItemStack> inputs) {
        ServerLevel level = (ServerLevel)player.level();
        level.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation(key)).ifPresent(recipe -> { player.awardRecipes(java.util.List.of(recipe)); player.triggerRecipeCrafted(recipe, inputs); });
    }
    public static boolean canBrew(ServerPlayer player, ItemStack bottle, ItemStack ingredient) {
        return !bottle.isEmpty() && !ingredient.isEmpty() && net.minecraftforge.common.brewing.BrewingRecipeRegistry.hasOutput(bottle, ingredient);
    }
    public static ItemStack mix(ServerPlayer player, ItemStack bottle, ItemStack ingredient) {
        ItemStack output = net.minecraftforge.common.brewing.BrewingRecipeRegistry.getOutput(bottle, ingredient);
        return output.isEmpty() ? bottle.copy() : output.copy();
    }
    public static boolean cancelBrew(NonNullList<ItemStack> items) { return net.minecraftforge.event.ForgeEventFactory.onPotionAttemptBrew(items); }
    public static void brewed(NonNullList<ItemStack> items) { net.minecraftforge.event.ForgeEventFactory.onPotionBrewed(items); }
}
