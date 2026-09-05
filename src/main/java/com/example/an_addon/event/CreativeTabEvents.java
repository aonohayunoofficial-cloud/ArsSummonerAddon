package com.example.an_addon.event;

import com.example.an_addon.registry.ModRegistry;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import static com.example.an_addon.ExampleANAddon.MODID;

@EventBusSubscriber(modid = MODID)
public class CreativeTabEvents {

    @SubscribeEvent
    public static void addItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModRegistry.CONTRACT_CARD.get());
            event.accept(ModRegistry.BLANK_CONTRACT_CARD.get());
            event.accept(ModRegistry.GUILD_ALTAR_ITEM.get());
        }
    }
}
