package io.github.apace100.apoli.power;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class ModelColorPower extends Power {

    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    private final boolean isTranslucent;

    public ModelColorPower(PowerType<?> type, LivingEntity entity, float red, float green, float blue, float alpha) {
        super(type, entity);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.isTranslucent = alpha < 1.0F;
    }

    public float getRed() {
        return red;
    }

    public float getGreen() {
        return green;
    }

    public float getBlue() {
        return blue;
    }

    public float getAlpha() {
        return alpha;
    }

    public boolean isTranslucent() {
        return isTranslucent;
    }
}
