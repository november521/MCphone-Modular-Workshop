// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.processing;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.nio.file.Files;

/** Real ItemStacks/containers/codecs, deterministic native-operation substitutes; no world or server. */
public final class ProcessingStateTest {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static final class Context implements ProcessingContext {
        int returned;
        boolean cancel;
        boolean customBrew;
        public FurnaceState.Recipe smelting(ItemStack input) {
            if (input.is(Items.RAW_IRON)) return new FurnaceState.Recipe("minecraft:iron_ingot", new ItemStack(Items.IRON_INGOT), 200, .7f);
            if (input.is(Items.WET_SPONGE)) return new FurnaceState.Recipe("minecraft:sponge", new ItemStack(Items.SPONGE), 200, .15f);
            return null;
        }
        public int burnTime(ItemStack fuel) { return fuel.is(Items.COAL) ? 1600 : fuel.is(Items.LAVA_BUCKET) ? 20000 : 0; }
        public boolean brewingFuel(ItemStack fuel) { return fuel.is(Items.BLAZE_POWDER); }
        public boolean canBrew(ItemStack bottle, ItemStack ingredient) { return bottle.is(Items.GLASS_BOTTLE) && (ingredient.is(Items.NETHER_WART) || ingredient.is(Items.DRAGON_BREATH)); }
        public ItemStack mix(ItemStack bottle, ItemStack ingredient) { return canBrew(bottle, ingredient) ? new ItemStack(Items.POTION) : bottle.copy(); }
        public ItemStack remainder(ItemStack consumed) { return consumed.is(Items.LAVA_BUCKET) ? new ItemStack(Items.BUCKET) : consumed.is(Items.DRAGON_BREATH) ? new ItemStack(Items.GLASS_BOTTLE) : ItemStack.EMPTY; }
        public void returnRemainder(ItemStack item) { returned += item.getCount(); }
        public boolean cancelBrew(NonNullList<ItemStack> items) {
            if (cancel && customBrew) items.set(0, new ItemStack(Items.POTION));
            return cancel;
        }
        public void brewed(NonNullList<ItemStack> items) {}
    }
    private static void ticks(MachineState state, Context context, int ticks) {
        for (int i = 0; i < ticks; i++) {
            if (state instanceof FurnaceState furnace) furnace.tick(context);
            else ((BrewingState)state).tick(context);
        }
    }
    private static BrewingState brewing() {
        BrewingState state = new BrewingState();
        for (int i = 0; i < 3; i++) state.setItem(i, new ItemStack(Items.GLASS_BOTTLE));
        state.setItem(3, new ItemStack(Items.NETHER_WART, 2));
        state.setItem(4, new ItemStack(Items.BLAZE_POWDER));
        return state;
    }
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Context context = new Context();
        FurnaceState furnace = new FurnaceState();
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 2));
        furnace.setItem(1, new ItemStack(Items.COAL, 2));
        ticks(furnace, context, 100);
        check(furnace.data().get(2) == 100 && furnace.getItem(1).getCount() == 1, "One fuel, half cooked");
        String snapshot = furnace.write(JsonOps.INSTANCE).toString();
        FurnaceState restored = new FurnaceState();
        restored.read(JsonParser.parseString(snapshot).getAsJsonObject(), JsonOps.INSTANCE);
        check(snapshot.equals(restored.write(JsonOps.INSTANCE).toString()), "Loading must not advance offline time or lose items/experience");
        ticks(restored, context, 100);
        check(restored.getItem(2).is(Items.IRON_INGOT) && restored.getItem(2).getCount() == 1 && restored.getItem(0).getCount() == 1, "Resume remaining half exactly once");
        check(restored.write(JsonOps.INSTANCE).getAsJsonObject("experience").get("minecraft:iron_ingot").getAsDouble() > .69, "Experience retained until pickup");
        ticks(restored, context, 200);
        check(restored.getItem(2).getCount() == 2 && restored.getItem(0).isEmpty(), "Batch cooking retains progress across input count changes");

        FurnaceState full = new FurnaceState();
        full.setItem(0, new ItemStack(Items.RAW_IRON)); full.setItem(1, new ItemStack(Items.COAL)); full.setItem(2, new ItemStack(Items.IRON_INGOT, 64));
        ticks(full, context, 300);
        check(full.getItem(1).getCount() == 1 && full.getItem(0).getCount() == 1 && full.data().get(0) == 0, "Full output never consumes fresh fuel or input");
        full.setItem(2, new ItemStack(Items.GOLD_INGOT)); ticks(full, context, 1);
        check(full.data().get(0) == 0, "Incompatible output blocks cooking");

        furnace.setItem(0, new ItemStack(Items.WET_SPONGE)); ticks(furnace, context, 1);
        check(furnace.data().get(2) == 1, "Replacing input resets accumulated cooking time");
        FurnaceState sponge = new FurnaceState(); sponge.setItem(0, new ItemStack(Items.WET_SPONGE)); sponge.setItem(1, new ItemStack(Items.LAVA_BUCKET));
        ticks(sponge, context, 200);
        check(sponge.getItem(2).is(Items.SPONGE) && sponge.getItem(1).is(Items.WATER_BUCKET), "Lava remainder and wet sponge bucket interaction");
        check(ProcessingTimer.cooking(1, false, true) == 0 && ProcessingTimer.cooking(100, false, true) == 98, "Unlit furnace cools down without going negative");

        BrewingState brewing = brewing(); ticks(brewing, context, 201);
        check(brewing.data().get(0) == 200 && brewing.data().get(1) == 19, "One brewing charge starts one 400 tick batch");
        BrewingState reloaded = new BrewingState(); reloaded.read(brewing.write(JsonOps.INSTANCE), JsonOps.INSTANCE);
        ticks(reloaded, context, 200);
        for (int i = 0; i < 3; i++) check(reloaded.getItem(i).is(Items.POTION), "All three bottles brewed");
        check(reloaded.getItem(3).getCount() == 1 && reloaded.data().get(0) == 0 && reloaded.data().get(1) == 19, "One ingredient per completed batch");
        ticks(reloaded, context, 500); check(reloaded.getItem(3).getCount() == 1, "Non-brewable output cannot consume ingredients");
        BrewingState changed = brewing(); ticks(changed, context, 201); changed.setItem(3, new ItemStack(Items.DRAGON_BREATH)); ticks(changed, context, 1);
        check(changed.data().get(0) == 0 && changed.getItem(0).is(Items.GLASS_BOTTLE), "Changing ingredient cancels the old batch");
        BrewingState remainder = brewing(); remainder.setItem(3, new ItemStack(Items.DRAGON_BREATH, 2)); ticks(remainder, context, 401);
        check(remainder.getItem(3).getCount() == 1 && context.returned == 1, "Stacked ingredient returns exactly one remainder");
        BrewingState canceled = brewing(); context.cancel = true; ticks(canceled, context, 401);
        check(canceled.getItem(3).getCount() == 2 && canceled.getItem(0).is(Items.GLASS_BOTTLE), "Loader cancellation cannot consume ingredients or produce potions");
        BrewingState custom = brewing(); context.customBrew = true; ticks(custom, context, 401);
        check(custom.getItem(0).is(Items.POTION) && custom.getItem(3).getCount() == 2, "Preserve a loader's custom canceled-event results without vanilla consumption");

        var invalid = brewing.write(JsonOps.INSTANCE); invalid.addProperty("fuel", -1);
        try { new BrewingState().read(invalid, JsonOps.INSTANCE); throw new AssertionError("Negative fuel accepted"); }
        catch (IllegalArgumentException expected) {}
        var directory = Files.createTempDirectory("workshop-save-test-"); var file = directory.resolve("player.json");
        try {
            AtomicJsonFile.write(file, snapshot); AtomicJsonFile.write(file, reloaded.write(JsonOps.INSTANCE).toString());
            check(Files.readString(file).equals(reloaded.write(JsonOps.INSTANCE).toString()), "Atomic replacement retains the complete snapshot");
            try (var files = Files.list(directory)) { check(files.count() == 1, "No temporary saves remain after replacement"); }
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory); }
        System.out.println("Processing checks passed: save/resume, output limits, input changes, fuel remainders, batch brewing and cancellation.");
    }
}
