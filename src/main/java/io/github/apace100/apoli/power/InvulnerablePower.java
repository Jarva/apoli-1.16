package io.github.apace100.apoli.power;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

import java.util.function.Predicate;

public class InvulnerablePower extends Power {

    private final Predicate<DamageSource> damageSources;

    public InvulnerablePower(PowerType<?> type, LivingEntity entity, Predicate<DamageSource> damageSourcePredicate) {
        super(type, entity);
        this.damageSources = damageSourcePredicate;
    }

    public boolean doesApply(DamageSource source) {
        return damageSources.test(source);
    }
}
