package io.github.apace100.apoli.power;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.function.Predicate;

public class ClimbingPower extends Power {

    private final boolean allowHolding;
    private final Predicate<LivingEntity> holdingCondition;

    public ClimbingPower(PowerType<?> type, LivingEntity entity, boolean allowHolding, Predicate<LivingEntity> holdingCondition) {
        super(type, entity);
        this.allowHolding = allowHolding;
        this.holdingCondition = holdingCondition;
    }

    public boolean canHold() {
        return allowHolding && (holdingCondition == null ? isActive() : holdingCondition.test(entity));
    }
}
