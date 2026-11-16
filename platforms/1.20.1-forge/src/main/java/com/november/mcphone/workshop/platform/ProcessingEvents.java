// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;
import com.november.mcphone.workshop.processing.ProcessingStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
public final class ProcessingEvents {
    private ProcessingEvents() {}
    public static void register() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) ProcessingStorage.tick(event.getServer());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.SaveToFile event) -> {
            if (event.getEntity() instanceof ServerPlayer player) ProcessingStorage.save(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) ProcessingStorage.logout(player);
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> ProcessingStorage.stop(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> ProcessingStorage.stopped(event.getServer()));
    }
}
