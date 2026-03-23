package com.eddy1.tidesourcer.init;

import com.eddy1.tidesourcer.TideSourcerMod;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, TideSourcerMod.MOD_ID);

    public static final Supplier<EntityType<TideSourcerEntity>> ABYSSAL_CORRUPTER =
            ENTITY_TYPES.register("abyssal_corrupter",
                    () -> EntityType.Builder.of(TideSourcerEntity::new, MobCategory.MONSTER)
                            .sized(0.9375f, 2.46875f)
                            .build("abyssal_corrupter"));
}
