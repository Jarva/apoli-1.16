package io.github.apace100.apoli.mixin;

import com.mojang.authlib.GameProfile;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.ParticlePower;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(OtherClientPlayerEntity.class)
public abstract class OtherClientPlayerEntityMixin extends AbstractClientPlayerEntity {

    public OtherClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void tick(CallbackInfo info) {
        if(!this.isInvisibleTo(MinecraftClient.getInstance().player)) {
            PowerHolderComponent component = PowerHolderComponent.KEY.get(this);
            List<ParticlePower> particlePowers = component.getPowers(ParticlePower.class);
            for (ParticlePower particlePower : particlePowers) {
                if(this.age % particlePower.getFrequency() == 0) {
                    world.addParticle(particlePower.getParticle(), this.getParticleX(0.5), this.getRandomBodyY(), this.getParticleZ(0.5), 0, 0, 0);
                }
            }
        }
    }
}
