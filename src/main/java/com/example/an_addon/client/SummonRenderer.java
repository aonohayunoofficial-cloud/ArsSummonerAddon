package com.example.an_addon.client;

import com.example.an_addon.entity.SummonEntity;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SummonRenderer extends MobRenderer<SummonEntity, SlimeModel<SummonEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/slime/slime.png");

    public SummonRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(SummonEntity entity) {
        return TEXTURE;
    }
}
