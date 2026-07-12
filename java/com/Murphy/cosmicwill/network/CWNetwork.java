package com.Murphy.cosmicwill.network;

import com.Murphy.cosmicwill.CustomWill;
import com.Murphy.cosmicwill.nullification.ErasureContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Objects;

public final class CWNetwork {

    private static final String PROTOCOL = "1";


    private static final ResourceLocation CHANNEL_ID = Objects.requireNonNull(
            ResourceLocation.tryBuild(CustomWill.MODID, "main"),
            "Invalid network channel id: " + CustomWill.MODID + ":main"
    );

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(CHANNEL_ID)
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static int packetId;
    private static boolean registered;

    private CWNetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        registered = true;

        CHANNEL.messageBuilder(
                        ClientErasurePacket.class,
                        packetId++,
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .encoder(ClientErasurePacket::encode)
                .decoder(ClientErasurePacket::decode)
                .consumerMainThread(ClientErasurePacket::handle)
                .add();
    }

    public static void sendErasure(
            ServerLevel level,
            Entity target,
            ErasureContext context
    ) {
        boolean clearOniResidue = context.rootClassName()
                .equals("oni_miko.entity.OnimikoEntity");

        ClientErasurePacket packet = new ClientErasurePacket(
                target.getId(),
                target.getUUID(),
                context.rootClassName(),
                clearOniResidue
        );

        for (ServerPlayer player : level.players()) {
            try {
                CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        packet
                );
            } catch (Throwable ignored) {
            }
        }
    }
}
