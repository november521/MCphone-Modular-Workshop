// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;
import com.november.mcphone.workshop.processing.ProcessingStorage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
public final class ProcessingEvents {
    private ProcessingEvents() {}
    public static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
            ProcessingStorage.tick(event.getServer());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.SaveToFile event) -> {
            if (event.getEntity() instanceof ServerPlayer player) ProcessingStorage.save(player);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) ProcessingStorage.logout(player);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> ProcessingStorage.stop(event.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> ProcessingStorage.stopped(event.getServer()));
    }
}
