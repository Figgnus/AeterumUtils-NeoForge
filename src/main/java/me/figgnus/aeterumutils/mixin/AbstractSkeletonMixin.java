package me.figgnus.aeterumutils.mixin;

import me.figgnus.aeterumutils.utils.SkeletonConfig;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin {
    @ModifyArg(
        method = "performRangedAttack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;shoot(DDDFF)V"
        ),
        index = 3,
        require = 0
    )
    private float aeterumutils$replaceVelocity(float originalVelocity) {
        return SkeletonConfig.arrowVelocity();
    }

    @ModifyConstant(
        method = "performRangedAttack",
        constant = @Constant(ordinal = 0),
        require = 0
    )
    private float aeterumutils$scaleYAimWithVelocity(float modifier) {
        float velocity = SkeletonConfig.arrowVelocity();
        if (velocity <= 0.0F) {
            return modifier;
        }
        return (float) (modifier * (1.0D / (velocity / 1.6D)));
    }

    @ModifyArg(
        method = "performRangedAttack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;shoot(DDDFF)V"
        ),
        index = 4,
        require = 0
    )
    private float aeterumutils$modifyInaccuracy(float originalInaccuracy) {
        return originalInaccuracy * SkeletonConfig.inaccuracyMultiplier();
    }
}
