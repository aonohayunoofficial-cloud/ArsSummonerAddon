package com.example.an_addon.client;

import com.example.an_addon.registry.ModRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import static com.example.an_addon.ExampleANAddon.MODID;

@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.SUMMON_ENTITY.get(), SummonRenderer::new);
    }
}
