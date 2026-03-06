package me.figgnus.aeterumutils.utils;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SkeletonConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue ARROW_VELOCITY;
    private static final ModConfigSpec.DoubleValue INACCURACY_MULTIPLIER;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("skeleton");
        ARROW_VELOCITY = builder
            .comment("Skeleton arrow velocity. Vanilla default is 1.6.")
            .defineInRange("arrowVelocity", 1.6D, 0.1D, 10.0D);
        INACCURACY_MULTIPLIER = builder
            .comment("Multiplier applied to vanilla inaccuracy. 1.0 = vanilla, 0.0 = perfect aim, >1.0 = less accurate.")
            .defineInRange("inaccuracyMultiplier", 1.0D, 0.0D, 10.0D);
        builder.pop();

        SPEC = builder.build();
    }

    private SkeletonConfig() {
    }

    public static float arrowVelocity() {
        return ARROW_VELOCITY.get().floatValue();
    }

    public static float inaccuracyMultiplier() {
        return INACCURACY_MULTIPLIER.get().floatValue();
    }
}
