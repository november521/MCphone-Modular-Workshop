// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.processing;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.november.mcphone.workshop.platform.ProcessingPlatform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

/** Server-thread owned; scoped to world + player, never to a transient GUI or player entity. */
public final class ProcessingStorage {
    public static final class Machines {
        public final FurnaceState furnace = new FurnaceState();
        public final BrewingState brewing = new BrewingState();
    }
    private static final Logger LOGGER = LoggerFactory.getLogger("mcphone_workshop");
    private static final Map<MinecraftServer, ProcessingStorage> SERVERS = new IdentityHashMap<>();
    private final Map<UUID, Machines> players = new HashMap<>();
    private final Set<UUID> checked = new HashSet<>(), failed = new HashSet<>();
    private int ticks;
    private static ProcessingStorage of(MinecraftServer server) { return SERVERS.computeIfAbsent(server, unused -> new ProcessingStorage()); }
    private static Path path(MinecraftServer server, UUID id) {
        return server.getWorldPath(LevelResource.ROOT).resolve("mcphone_workshop").resolve(id + ".json");
    }
    public static Machines get(ServerPlayer player) {
        MinecraftServer server = ProcessingPlatform.server(player);
        return of(server).load(server, player.getUUID(), true);
    }
    private Machines load(MinecraftServer server, UUID id, boolean create) {
        if (failed.contains(id)) throw new IllegalStateException("Workshop storage could not be loaded");
        if (!checked.contains(id)) {
            try {
                String contents = Files.readString(path(server, id));
                JsonObject json = JsonParser.parseString(contents).getAsJsonObject();
                if (MachineState.number(json, "version", 1) != 1) throw new IllegalArgumentException("Unsupported workshop save version");
                Machines machines = new Machines();
                var ops = ProcessingPlatform.ops(server);
                machines.furnace.read(json.getAsJsonObject("furnace"), ops);
                machines.brewing.read(json.getAsJsonObject("brewing"), ops);
                players.put(id, machines);
            } catch (NoSuchFileException absent) {
                // A player who has never opened a machine does not need a save.
            } catch (IOException | RuntimeException error) {
                failed.add(id);
                LOGGER.error("Cannot load Workshop inventory {}; preserving the existing file", path(server, id), error);
                throw new IllegalStateException("Workshop storage could not be loaded", error);
            }
            checked.add(id);
        }
        if (create && !players.containsKey(id)) {
            Machines machines = new Machines();
            machines.furnace.dirty = true;
            players.put(id, machines);
        }
        return players.get(id);
    }
    public static void tick(MinecraftServer server) {
        ProcessingStorage storage = of(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Do not deliver ingredient remainders into an inventory being replaced by respawn.
            if (!player.isAlive()) continue;
            if (storage.failed.contains(player.getUUID())) continue;
            Machines machines;
            try { machines = storage.load(server, player.getUUID(), false); }
            catch (IllegalStateException failure) { continue; }
            if (machines != null) {
                machines.furnace.tick(player);
                machines.brewing.tick(player);
            }
        }
        if (++storage.ticks % 200 == 0) storage.saveAll(server);
    }
    public static void save(ServerPlayer player) {
        MinecraftServer server = ProcessingPlatform.server(player);
        of(server).save(server, player.getUUID());
    }
    private boolean save(MinecraftServer server, UUID id) {
        Machines machines = players.get(id);
        if (machines == null || (!machines.furnace.dirty && !machines.brewing.dirty)) return true;
        try {
            JsonObject json = new JsonObject(); json.addProperty("version", 1);
            var ops = ProcessingPlatform.ops(server);
            json.add("furnace", machines.furnace.write(ops)); json.add("brewing", machines.brewing.write(ops));
            AtomicJsonFile.write(path(server, id), new GsonBuilder().setPrettyPrinting().create().toJson(json));
            machines.furnace.dirty = machines.brewing.dirty = false;
            return true;
        } catch (IOException | RuntimeException error) {
            LOGGER.error("Cannot save Workshop inventory {}; retaining it in memory for retry", path(server, id), error);
            return false;
        }
    }
    private void saveAll(MinecraftServer server) { for (UUID id : players.keySet()) save(server, id); }
    public static void logout(ServerPlayer player) {
        MinecraftServer server = ProcessingPlatform.server(player);
        ProcessingStorage storage = of(server);
        // Keep the live object until server shutdown: closing menus during logout can still update it.
        storage.save(server, player.getUUID());
    }
    public static void stop(MinecraftServer server) {
        ProcessingStorage storage = SERVERS.get(server);
        if (storage != null) storage.saveAll(server);
    }
    public static void stopped(MinecraftServer server) {
        stop(server);
        SERVERS.remove(server);
    }
}
