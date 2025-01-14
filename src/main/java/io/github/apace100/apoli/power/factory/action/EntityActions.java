package io.github.apace100.apoli.power.factory.action;

import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.apoli.power.CooldownPower;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.VariableIntPower;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import io.github.apace100.apoli.util.Scheduler;
import io.github.apace100.apoli.util.Space;
import io.github.apace100.calio.ClassUtil;
import io.github.apace100.calio.FilterableWeightedList;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.PotionUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class EntityActions {

    @SuppressWarnings("unchecked")
    public static void register() {
        register(new ActionFactory<>(Apoli.identifier("and"), new SerializableData()
            .add("actions", ApoliDataTypes.ENTITY_ACTIONS),
            (data, entity) -> ((List<ActionFactory<Entity>.Instance>)data.get("actions")).forEach((e) -> e.accept(entity))));
        register(new ActionFactory<>(Apoli.identifier("chance"), new SerializableData()
            .add("action", ApoliDataTypes.ENTITY_ACTION)
            .add("chance", SerializableDataTypes.FLOAT),
            (data, entity) -> {
                if(new Random().nextFloat() < data.getFloat("chance")) {
                    ((ActionFactory<Entity>.Instance)data.get("action")).accept(entity);
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("if_else"), new SerializableData()
            .add("condition", ApoliDataTypes.ENTITY_CONDITION)
            .add("if_action", ApoliDataTypes.ENTITY_ACTION)
            .add("else_action", ApoliDataTypes.ENTITY_ACTION, null),
            (data, entity) -> {
                if(entity instanceof LivingEntity) {
                    if(((ConditionFactory<LivingEntity>.Instance)data.get("condition")).test((LivingEntity)entity)) {
                        ((ActionFactory<Entity>.Instance)data.get("if_action")).accept(entity);
                    } else {
                        if(data.isPresent("else_action")) {
                            ((ActionFactory<Entity>.Instance)data.get("else_action")).accept(entity);
                        }
                    }
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("choice"), new SerializableData()
            .add("actions", SerializableDataType.weightedList(ApoliDataTypes.ENTITY_ACTION)),
            (data, entity) -> {
                FilterableWeightedList<ActionFactory<Entity>.Instance> actionList = (FilterableWeightedList<ActionFactory<Entity>.Instance>)data.get("actions");
                ActionFactory<Entity>.Instance action = actionList.pickRandom(new Random());
                action.accept(entity);
            }));
        register(new ActionFactory<>(Apoli.identifier("if_else_list"), new SerializableData()
            .add("actions", SerializableDataType.list(SerializableDataType.compound(ClassUtil.castClass(Pair.class), new SerializableData()
                .add("action", ApoliDataTypes.ENTITY_ACTION)
                .add("condition", ApoliDataTypes.ENTITY_CONDITION),
                inst -> new Pair<>((ConditionFactory<LivingEntity>.Instance)inst.get("condition"), (ActionFactory<Entity>.Instance)inst.get("action")),
                (data, pair) -> {
                    SerializableData.Instance inst = data.new Instance();
                    inst.set("condition", pair.getLeft());
                    inst.set("action", pair.getRight());
                    return inst;
                }))),
            (data, entity) -> {
                if(entity instanceof LivingEntity) {
                    List<Pair<ConditionFactory<Entity>.Instance, ActionFactory<Entity>.Instance>> actions =
                        (List<Pair<ConditionFactory<Entity>.Instance, ActionFactory<Entity>.Instance>>)data.get("actions");
                    for (Pair<ConditionFactory<Entity>.Instance, ActionFactory<Entity>.Instance> action: actions) {
                        if(action.getLeft().test(entity)) {
                            action.getRight().accept(entity);
                            break;
                        }
                    }
                }
            }));
        Scheduler scheduler = new Scheduler();
        register(new ActionFactory<>(Apoli.identifier("delay"), new SerializableData()
            .add("ticks", SerializableDataTypes.INT)
            .add("action", ApoliDataTypes.ENTITY_ACTION),
            (data, entity) -> {
                ActionFactory<Entity>.Instance action = (ActionFactory<Entity>.Instance)data.get("action");
                scheduler.queue(s -> action.accept(entity), data.getInt("ticks"));
            }));


        register(new ActionFactory<>(Apoli.identifier("damage"), new SerializableData()
            .add("amount", SerializableDataTypes.FLOAT)
            .add("source", SerializableDataTypes.DAMAGE_SOURCE),
            (data, entity) -> entity.damage((DamageSource)data.get("source"), data.getFloat("amount"))));
        register(new ActionFactory<>(Apoli.identifier("heal"), new SerializableData()
            .add("amount", SerializableDataTypes.FLOAT),
            (data, entity) -> {
                if(entity instanceof LivingEntity) {
                    ((LivingEntity)entity).heal(data.getFloat("amount"));
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("play_sound"), new SerializableData()
                .add("sound", SerializableDataTypes.SOUND_EVENT)
                .add("volume", SerializableDataTypes.FLOAT, 1F)
                .add("pitch", SerializableDataTypes.FLOAT, 1F),
                (data, entity) -> {
                    if(entity instanceof PlayerEntity) {
                        entity.world.playSound((PlayerEntity) null, (entity).getX(), (entity).getY(), (entity).getZ(), (SoundEvent)data.get("sound"),
                        SoundCategory.PLAYERS, data.getFloat("volume"), data.getFloat("pitch"));
                    }
                }));
        register(new ActionFactory<>(Apoli.identifier("exhaust"), new SerializableData()
            .add("amount", SerializableDataTypes.FLOAT),
            (data, entity) -> {
                if(entity instanceof PlayerEntity)
                    ((PlayerEntity)entity).getHungerManager().addExhaustion(data.getFloat("amount"));
            }));
        register(new ActionFactory<>(Apoli.identifier("apply_effect"), new SerializableData()
            .add("effect", SerializableDataTypes.STATUS_EFFECT_INSTANCE, null)
            .add("effects", SerializableDataTypes.STATUS_EFFECT_INSTANCES, null),
            (data, entity) -> {
                if(entity instanceof LivingEntity && !entity.world.isClient) {
                    LivingEntity le = (LivingEntity) entity;
                    if(data.isPresent("effect")) {
                        StatusEffectInstance effect = (StatusEffectInstance)data.get("effect");
                        le.addStatusEffect(new StatusEffectInstance(effect));
                    }
                    if(data.isPresent("effects")) {
                        ((List<StatusEffectInstance>)data.get("effects")).forEach(e -> le.addStatusEffect(new StatusEffectInstance(e)));
                    }
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("clear_effect"), new SerializableData()
            .add("effect", SerializableDataTypes.STATUS_EFFECT, null),
            (data, entity) -> {
                if(entity instanceof LivingEntity) {
                    LivingEntity le = (LivingEntity) entity;
                    if(data.isPresent("effect")) {
                        le.removeStatusEffect((StatusEffect)data.get("effect"));
                    } else {
                        le.clearStatusEffects();
                    }
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("set_on_fire"), new SerializableData()
            .add("duration", SerializableDataTypes.INT),
            (data, entity) -> {
                entity.setOnFireFor(data.getInt("duration"));
            }));
        register(new ActionFactory<>(Apoli.identifier("add_velocity"), new SerializableData()
            .add("x", SerializableDataTypes.FLOAT, 0F)
            .add("y", SerializableDataTypes.FLOAT, 0F)
            .add("z", SerializableDataTypes.FLOAT, 0F)
            .add("space", ApoliDataTypes.SPACE, Space.WORLD)
            .add("set", SerializableDataTypes.BOOLEAN, false),
            (data, entity) -> {
                Space space = (Space)data.get("space");
                Vec3f vec = new Vec3f(data.getFloat("x"), data.getFloat("y"), data.getFloat("z"));
                Vec3d vel;
                Vec3d velH;
                TriConsumer<Float, Float, Float> method = entity::addVelocity;
                if(data.getBoolean("set")) {
                    method = entity::setVelocity;
                }
                switch(space) {
                    case WORLD:
                        method.accept(data.getFloat("x"), data.getFloat("y"), data.getFloat("z"));
                        break;
                    case LOCAL:
                        Space.rotateVectorToBase(entity.getRotationVector(), vec);
                        method.accept(vec.getX(), vec.getY(), vec.getZ());
                        break;
                    case LOCAL_HORIZONTAL:
                        vel = entity.getRotationVector();
                        velH = new Vec3d(vel.x, 0, vel.z);
                        if(velH.lengthSquared() > 0.00005) {
                            velH = velH.normalize();
                            Space.rotateVectorToBase(velH, vec);
                            method.accept(vec.getX(), vec.getY(), vec.getZ());
                        }
                        break;
                    case VELOCITY:
                        Space.rotateVectorToBase(entity.getVelocity(), vec);
                        method.accept(vec.getX(), vec.getY(), vec.getZ());
                        break;
                    case VELOCITY_NORMALIZED:
                        Space.rotateVectorToBase(entity.getVelocity().normalize(), vec);
                        method.accept(vec.getX(), vec.getY(), vec.getZ());
                        break;
                    case VELOCITY_HORIZONTAL:
                        vel = entity.getVelocity();
                        velH = new Vec3d(vel.x, 0, vel.z);
                        Space.rotateVectorToBase(velH, vec);
                        method.accept(vec.getX(), vec.getY(), vec.getZ());
                        break;
                    case VELOCITY_HORIZONTAL_NORMALIZED:
                        vel = entity.getVelocity();
                        velH = new Vec3d(vel.x, 0, vel.z);
                        if(velH.lengthSquared() > 0.00005) {
                            velH = velH.normalize();
                            Space.rotateVectorToBase(velH, vec);
                            method.accept(vec.getX(), vec.getY(), vec.getZ());
                        }
                        break;
                }
                entity.velocityModified = true;
            }));
        register(new ActionFactory<>(Apoli.identifier("spawn_entity"), new SerializableData()
            .add("entity_type", SerializableDataTypes.ENTITY_TYPE)
            .add("tag", SerializableDataTypes.NBT, null)
            .add("entity_action", ApoliDataTypes.ENTITY_ACTION, null),
            (data, entity) -> {
                Entity e = ((EntityType<?>)data.get("entity_type")).create(entity.world);
                if(e != null) {
                    e.refreshPositionAndAngles(entity.getPos().x, entity.getPos().y, entity.getPos().z, entity.getYaw(), entity.getPitch());
                    if(data.isPresent("tag")) {
                        NbtCompound mergedTag = e.writeNbt(new NbtCompound());
                        mergedTag.copyFrom((NbtCompound)data.get("tag"));
                        e.readNbt(mergedTag);
                    }

                    entity.world.spawnEntity(e);
                    if(data.isPresent("entity_action")) {
                        ((ActionFactory<Entity>.Instance)data.get("entity_action")).accept(e);
                    }
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("gain_air"), new SerializableData()
            .add("value", SerializableDataTypes.INT),
            (data, entity) -> {
                if(entity instanceof LivingEntity) {
                    LivingEntity le = (LivingEntity) entity;
                    le.setAir(Math.min(le.getAir() + data.getInt("value"), le.getMaxAir()));
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("block_action_at"), new SerializableData()
            .add("block_action", ApoliDataTypes.BLOCK_ACTION),
            (data, entity) -> {
                    ((ActionFactory<Triple<World, BlockPos, Direction>>.Instance)data.get("block_action")).accept(
                        Triple.of(entity.world, entity.getBlockPos(), Direction.UP));
            }));
        register(new ActionFactory<>(Apoli.identifier("spawn_effect_cloud"), new SerializableData()
            .add("radius", SerializableDataTypes.FLOAT, 3.0F)
            .add("radius_on_use", SerializableDataTypes.FLOAT, -0.5F)
            .add("wait_time", SerializableDataTypes.INT, 10)
            .add("effect", SerializableDataTypes.STATUS_EFFECT_INSTANCE, null)
            .add("effects", SerializableDataTypes.STATUS_EFFECT_INSTANCES, null),
            (data, entity) -> {
                AreaEffectCloudEntity areaEffectCloudEntity = new AreaEffectCloudEntity(entity.world, entity.getX(), entity.getY(), entity.getZ());
                if (entity instanceof LivingEntity) {
                    areaEffectCloudEntity.setOwner((LivingEntity)entity);
                }
                areaEffectCloudEntity.setRadius(data.getFloat("radius"));
                areaEffectCloudEntity.setRadiusOnUse(data.getFloat("radius_on_use"));
                areaEffectCloudEntity.setWaitTime(data.getInt("wait_time"));
                areaEffectCloudEntity.setRadiusGrowth(-areaEffectCloudEntity.getRadius() / (float)areaEffectCloudEntity.getDuration());
                List<StatusEffectInstance> effects = new LinkedList<>();
                if(data.isPresent("effect")) {
                    effects.add((StatusEffectInstance)data.get("effect"));
                }
                if(data.isPresent("effects")) {
                    effects.addAll((List<StatusEffectInstance>)data.get("effects"));
                }
                areaEffectCloudEntity.setColor(PotionUtil.getColor(effects));
                effects.forEach(areaEffectCloudEntity::addEffect);

                entity.world.spawnEntity(areaEffectCloudEntity);
            }));
        register(new ActionFactory<>(Apoli.identifier("extinguish"), new SerializableData(),
            (data, entity) -> entity.extinguish()));
        register(new ActionFactory<>(Apoli.identifier("execute_command"), new SerializableData()
            .add("command", SerializableDataTypes.STRING)
            .add("permission_level", SerializableDataTypes.INT, 4),
            (data, entity) -> {
                MinecraftServer server = entity.world.getServer();
                if(server != null) {
                    ServerCommandSource source = new ServerCommandSource(
                        CommandOutput.DUMMY,
                        entity.getPos(),
                        entity.getRotationClient(),
                        entity.world instanceof ServerWorld ? (ServerWorld)entity.world : null,
                        data.getInt("permission_level"),
                        entity.getName().getString(),
                        entity.getDisplayName(),
                        entity.world.getServer(),
                        entity);
                    server.getCommandManager().execute(source, data.getString("command"));
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("change_resource"), new SerializableData()
            .add("resource", ApoliDataTypes.POWER_TYPE)
            .add("change", SerializableDataTypes.INT),
            (data, entity) -> {
                if(entity instanceof PlayerEntity) {
                    PowerHolderComponent component = PowerHolderComponent.KEY.get(entity);
                    Power p = component.getPower((PowerType<?>)data.get("resource"));
                    if(p instanceof VariableIntPower) {
                        VariableIntPower vip = (VariableIntPower)p;
                        int newValue = vip.getValue() + data.getInt("change");
                        vip.setValue(newValue);
                        PowerHolderComponent.sync((PlayerEntity)entity);
                    } else if(p instanceof CooldownPower) {
                        CooldownPower cp = (CooldownPower)p;
                        cp.modify(data.getInt("change"));
                        PowerHolderComponent.sync((PlayerEntity)entity);
                    }
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("feed"), new SerializableData()
            .add("food", SerializableDataTypes.INT)
            .add("saturation", SerializableDataTypes.FLOAT),
            (data, entity) -> {
                if(entity instanceof PlayerEntity) {
                    ((PlayerEntity)entity).getHungerManager().add(data.getInt("food"), data.getFloat("saturation"));
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("add_xp"), new SerializableData()
            .add("points", SerializableDataTypes.INT, 0)
            .add("levels", SerializableDataTypes.INT, 0),
            (data, entity) -> {
                if(entity instanceof PlayerEntity) {
                    int points = data.getInt("points");
                    int levels = data.getInt("levels");
                    if(points > 0) {
                        ((PlayerEntity)entity).addExperience(points);
                    }
                    ((PlayerEntity)entity).addExperienceLevels(levels);
                }
            }));

        register(new ActionFactory<>(Apoli.identifier("set_fall_distance"), new SerializableData()
            .add("fall_distance", SerializableDataTypes.FLOAT),
            (data, entity) -> {
                entity.fallDistance = data.getFloat("fall_distance");
            }));
        register(new ActionFactory<>(Apoli.identifier("give"), new SerializableData()
            .add("stack", SerializableDataTypes.ITEM_STACK),
            (data, entity) -> {
                if(!entity.world.isClient()) {
                    ItemStack stack = (ItemStack)data.get("stack");
                    stack = stack.copy();
                    if(entity instanceof PlayerEntity) {
                        ((PlayerEntity)entity).getInventory().offerOrDrop(stack);
                    } else {
                        entity.world.spawnEntity(new ItemEntity(entity.world, entity.getX(), entity.getY(), entity.getZ(), stack));
                    }
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("equipped_item_action"), new SerializableData()
            .add("equipment_slot", SerializableDataTypes.EQUIPMENT_SLOT)
            .add("action", ApoliDataTypes.ITEM_ACTION),
            (data, entity) -> {
                if(entity instanceof LivingEntity) {
                    ItemStack stack = ((LivingEntity)entity).getEquippedStack((EquipmentSlot)data.get("equipment_slot"));
                    ActionFactory<ItemStack>.Instance action = (ActionFactory<ItemStack>.Instance)data.get("action");
                    action.accept(stack);
                }
            }));
        register(new ActionFactory<>(Apoli.identifier("trigger_cooldown"), new SerializableData()
            .add("power", ApoliDataTypes.POWER_TYPE),
            (data, entity) -> {
                if(entity instanceof PlayerEntity) {
                    PowerHolderComponent component = PowerHolderComponent.KEY.get(entity);
                    Power p = component.getPower((PowerType<?>)data.get("power"));
                    if(p instanceof CooldownPower) {
                        CooldownPower cp = (CooldownPower)p;
                        cp.use();
                    }
                }
            }));
    }

    private static void register(ActionFactory<Entity> actionFactory) {
        Registry.register(ApoliRegistries.ENTITY_ACTION, actionFactory.getSerializerId(), actionFactory);
    }
}
