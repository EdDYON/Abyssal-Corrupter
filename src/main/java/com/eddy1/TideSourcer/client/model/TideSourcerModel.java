package com.eddy1.tidesourcer.client.model;

import com.eddy1.tidesourcer.TideSourcerMod;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TideSourcerModel extends GeoModel<TideSourcerEntity> {
    @Override
    public ResourceLocation getModelResource(TideSourcerEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TideSourcerMod.MOD_ID, "geo/abyssal_corrupter.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TideSourcerEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TideSourcerMod.MOD_ID, "textures/entity/abyssal_corrupter.png");
    }

    @Override
    public ResourceLocation getAnimationResource(TideSourcerEntity object) {
        return ResourceLocation.fromNamespaceAndPath(TideSourcerMod.MOD_ID, "animations/abyssal_corrupter.animation.json");
    }
}
