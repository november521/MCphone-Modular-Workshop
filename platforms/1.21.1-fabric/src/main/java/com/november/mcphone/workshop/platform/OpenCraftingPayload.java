// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenCraftingPayload() implements CustomPacketPayload {
    public static final OpenCraftingPayload INSTANCE = new OpenCraftingPayload();
    public static final Type<OpenCraftingPayload> TYPE = new Type<>(ResourceLocation.parse("mcphone_workshop:open_crafting"));
    public static final StreamCodec<FriendlyByteBuf, OpenCraftingPayload> CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
