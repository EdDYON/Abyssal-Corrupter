package com.eddy1.tidesourcer.client.renderer;

import com.eddy1.tidesourcer.client.model.TideSourcerModel;
import com.eddy1.tidesourcer.entity.custom.TideSourcerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TideSourcerRenderer extends GeoEntityRenderer<TideSourcerEntity> {
    public TideSourcerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TideSourcerModel());
        this.shadowRadius = 0.8f;
    }
}