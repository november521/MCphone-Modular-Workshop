// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.client;
import com.november.mcphone.api.client.app.IPhoneApp;
import com.november.mcphone.workshop.ender.EnderChestAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
public final class EnderChestApp implements IPhoneApp {
    public ResourceLocation getId() { return EnderChestAccess.ID; }
    public Component getDisplayName() { return Component.translatable("mcphone.app.ender_chest"); }
    public ResourceLocation getIconTexture() { return ResourceLocation.parse("mcphone:textures/app/ender_chest.png"); }
    public boolean isAvailable() { return EnderChestAccess.active(); }
    public boolean isPreinstalled() { return false; }
    public boolean opensInsidePhone() { return false; }
    public String getAuthor() { return "november521"; }
    public String getDescription() { return Component.translatable("mcphone.app.ender_chest.desc").getString(); }
    public void onPress() { if (EnderChestAccess.active()) EnderChestSender.open(); }
}
