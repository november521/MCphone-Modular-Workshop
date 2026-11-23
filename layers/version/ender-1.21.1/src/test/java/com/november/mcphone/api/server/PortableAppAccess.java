// Test-only proposed contract fixture. NEVER included in a mod JAR.
package com.november.mcphone.api.server;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
public final class PortableAppAccess {
    public static boolean owner, paid, device, failure;
    public static boolean isProvidedBy(ResourceLocation id, String mod) { return owner && id.toString().equals("mcphone:ender_chest") && mod.equals("mcphone_workshop"); }
    public static boolean hasPurchased(ServerPlayer player, ResourceLocation id) { if(failure) throw new IllegalStateException("test"); return paid; }
    public static boolean isDeviceCarried(Player player, ItemStack cursor) { if(failure) throw new IllegalStateException("test"); return device; }
}
