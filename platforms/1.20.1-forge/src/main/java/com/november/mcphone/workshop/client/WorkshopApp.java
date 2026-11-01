// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.client;

import com.november.mcphone.api.client.app.IPhoneApp;
import com.november.mcphone.workshop.workstation.Workstation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** MCphone discovers the concrete Apps only on the client. */
public abstract class WorkshopApp implements IPhoneApp {
    private final Workstation workstation;
    protected WorkshopApp(Workstation workstation) { this.workstation = workstation; }
    @Override public ResourceLocation getId() { return new ResourceLocation(workstation.appId()); }
    @Override public Component getDisplayName() { return Component.translatable(workstation.titleKey()); }
    @Override public ResourceLocation getIconTexture() { return new ResourceLocation(workstation.iconTexture()); }
    @Override public boolean isPreinstalled() { return false; }
    @Override public boolean opensInsidePhone() { return false; }
    @Override public String getAuthor() { return "november521"; }
    @Override public String getDescription() { return Component.translatable(workstation.titleKey() + ".description").getString(); }
    @Override public void onPress() { WorkstationSender.open(workstation); }
}
