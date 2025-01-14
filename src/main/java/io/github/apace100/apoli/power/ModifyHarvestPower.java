package io.github.apace100.apoli.power;

import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class ModifyHarvestPower extends Power {

    private final Predicate<CachedBlockPosition> predicate;
    private boolean allow;

    public ModifyHarvestPower(PowerType<?> type, LivingEntity entity, Predicate<CachedBlockPosition> predicate, boolean allow) {
        super(type, entity);
        this.predicate = predicate;
        this.allow = allow;
    }

    public boolean doesApply(BlockPos pos) {
        CachedBlockPosition cbp = new CachedBlockPosition(entity.world, pos, true);
        return predicate.test(cbp);
    }

    public boolean doesApply(CachedBlockPosition pos) {
        return predicate.test(pos);
    }

    public boolean isHarvestAllowed() {
        return allow;
    }
}
