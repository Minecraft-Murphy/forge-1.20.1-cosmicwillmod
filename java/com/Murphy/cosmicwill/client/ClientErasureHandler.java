package com.Murphy.cosmicwill.client;

import com.Murphy.cosmicwill.compat.OniMikoCompat;
import com.Murphy.cosmicwill.network.ClientErasurePacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientErasureHandler {

    private ClientErasureHandler() {
    }

    public static void handle(ClientErasurePacket packet) {
        ClientRegistryEraser.erase(
                packet.entityId(),
                packet.entityUuid(),
                packet.rootClassName()
        );

        if (packet.clearOniMikoResidue()) {
            OniMikoCompat.resetClientResidue();
        }
    }
}
