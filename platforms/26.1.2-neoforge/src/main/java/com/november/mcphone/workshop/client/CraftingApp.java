// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.client;

import com.november.mcphone.api.client.app.IPhoneApp;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Loaded by MCphone's client-only SPI discovery, never by a common entrypoint. */
public final class CraftingApp implements IPhoneApp {
    @Override public Identifier getId() { return Identifier.parse("mcphone_workshop:crafting"); }
    @Override public Component getDisplayName() { return Component.translatable("app.mcphone_workshop.crafting"); }
    @Override public Identifier getIconTexture() { return Identifier.parse("minecraft:textures/block/crafting_table_top.png"); }
    @Override public boolean isPreinstalled() { return false; }
    @Override public boolean opensInsidePhone() { return false; }
    @Override public String getAuthor() { return "november521"; }
    @Override public String getDescription() { return Component.translatable("app.mcphone_workshop.crafting.description").getString(); }
    @Override public void onPress() { CraftingSender.open(); }
}
