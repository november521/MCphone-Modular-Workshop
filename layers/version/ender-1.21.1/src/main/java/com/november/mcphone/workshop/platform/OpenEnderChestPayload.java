// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record OpenEnderChestPayload() implements CustomPacketPayload {
    public static final Type<OpenEnderChestPayload> TYPE = new Type<>(ResourceLocation.parse("mcphone_workshop:open_ender_chest"));
    public static final StreamCodec<FriendlyByteBuf, OpenEnderChestPayload> CODEC = StreamCodec.of((buffer, packet) -> {}, buffer -> new OpenEnderChestPayload());
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
