// SPDX-License-Identifier: MIT
package com.november.mcphone.workshop.platform;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenWorkstationPayload(int workstation) implements CustomPacketPayload {
    public static final Type<OpenWorkstationPayload> TYPE = new Type<>(Identifier.parse("mcphone_workshop:open_workstation"));
    public static final StreamCodec<FriendlyByteBuf, OpenWorkstationPayload> CODEC = StreamCodec.of(
            (buffer, packet) -> buffer.writeVarInt(packet.workstation()),
            buffer -> new OpenWorkstationPayload(buffer.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
