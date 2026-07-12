package com.Murphy.cosmicwill.network;

import com.Murphy.cosmicwill.client.ClientErasureHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;


public record ClientErasurePacket(
        int entityId,
        UUID entityUuid,
        String rootClassName,
        boolean clearOniMikoResidue
) {

    public static void encode(ClientErasurePacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId());
        buffer.writeUUID(message.entityUuid());
        buffer.writeUtf(message.rootClassName(), 512);
        buffer.writeBoolean(message.clearOniMikoResidue());
    }

    public static ClientErasurePacket decode(FriendlyByteBuf buffer) {
        return new ClientErasurePacket(
                buffer.readVarInt(),
                buffer.readUUID(),
                buffer.readUtf(512),
                buffer.readBoolean()
        );
    }

    public static void handle(
            ClientErasurePacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientErasureHandler.handle(message)
        ));
        context.setPacketHandled(true);
    }
}
