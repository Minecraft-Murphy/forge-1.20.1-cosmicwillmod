package com.Murphy.cosmicwill.data;

import com.Murphy.cosmicwill.CustomWill;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Objects;

public final class CWTags {

    private CWTags() {
    }

    public static final class Items {

        public static final TagKey<Item> DECONSTRUCTION_PROTECTED =
                create("deconstruction_protected");

        public static final TagKey<Item>
                DECONSTRUCTION_ABSOLUTE_BLACKLIST =
                create("deconstruction_absolute_blacklist");

        private Items() {
        }

        private static TagKey<Item> create(String path) {
            ResourceLocation id = Objects.requireNonNull(
                    ResourceLocation.tryBuild(
                            CustomWill.MODID,
                            path
                    )
            );
            return TagKey.create(Registries.ITEM, id);
        }
    }
}
