// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.ender;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Proposed public host contract. No private host API fallback, no locally copied purchase record. */
public final class EnderChestAccess {
    public static final ResourceLocation ID = ResourceLocation.parse("mcphone:ender_chest");
    public static final String OWNER = "mcphone_workshop";
    private record Api(Method owner, Method purchased, Method device) {}
    private static final Api API = discover();
    private EnderChestAccess() {}
    private static Method method(Class<?> type, String name, Class<?>... args) throws ReflectiveOperationException {
        Method m = type.getMethod(name, args);
        if (!Modifier.isStatic(m.getModifiers()) || m.getReturnType() != boolean.class) throw new NoSuchMethodException(name);
        return m;
    }
    private static Api discover() {
        try {
            Class<?> type = Class.forName("com.november.mcphone.api.server.PortableAppAccess", false, EnderChestAccess.class.getClassLoader());
            return new Api(method(type, "isProvidedBy", ResourceLocation.class, String.class),
                    method(type, "hasPurchased", ServerPlayer.class, ResourceLocation.class),
                    method(type, "isDeviceCarried", Player.class, ItemStack.class));
        } catch (ReflectiveOperationException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger(OWNER).info("Portable ender chest takeover disabled: host public handoff API is unavailable.");
            return null;
        }
    }
    private static boolean invoke(Method method, Object... args) {
        try { return Boolean.TRUE.equals(method.invoke(null, args)); }
        catch (ReflectiveOperationException | LinkageError e) { throw new IllegalStateException("MCphone handoff API failed", e); }
    }
    public static boolean active() {
        try { return API != null && invoke(API.owner(), ID, OWNER); }
        catch (RuntimeException e) { return false; }
    }
    public static boolean purchased(ServerPlayer player) {
        if (!active()) throw new IllegalStateException("Host has not transferred ownership");
        return invoke(API.purchased(), player, ID);
    }
    public static boolean device(Player player, ItemStack carried) {
        if (!active()) throw new IllegalStateException("Host has not transferred ownership");
        return invoke(API.device(), player, carried);
    }
    public static boolean canUse(ServerPlayer player, ItemStack carried) {
        try { return player.isAlive() && !player.isSpectator() && purchased(player) && device(player, carried); }
        catch (RuntimeException e) { return false; }
    }
}
