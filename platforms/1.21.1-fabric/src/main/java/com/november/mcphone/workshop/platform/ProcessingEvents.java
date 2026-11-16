// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;
import com.november.mcphone.workshop.processing.ProcessingStorage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
public final class ProcessingEvents {
    private ProcessingEvents() {}
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ProcessingStorage::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ProcessingStorage.logout(handler.player));
        ServerLifecycleEvents.SERVER_STOPPING.register(ProcessingStorage::stop);
        ServerLifecycleEvents.SERVER_STOPPED.register(ProcessingStorage::stopped);
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> {
            for (var player : server.getPlayerList().getPlayers()) ProcessingStorage.save(player);
        });
    }
}
