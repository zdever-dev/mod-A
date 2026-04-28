package com.example.structuregen.internal.world;

import com.example.structuregen.internal.world.StructureWorldState;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Test helper pro přístup k private metodám {@link StructureWorldState}.
 * Používán výhradně v unit testech.
 */
final class StructureWorldStateTestAccess {

    static StructureWorldState createEmpty() {
        try {
            Constructor<StructureWorldState> ctor =
                StructureWorldState.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create StructureWorldState", e);
        }
    }

    static StructureWorldState loadFromTag(CompoundTag tag) {
        try {
            Method load = StructureWorldState.class
                .getDeclaredMethod("load", CompoundTag.class);
            load.setAccessible(true);
            return (StructureWorldState) load.invoke(null, tag);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load StructureWorldState from tag", e);
        }
    }

    private StructureWorldStateTestAccess() {}
}