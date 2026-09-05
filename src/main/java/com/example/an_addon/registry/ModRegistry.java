package com.example.an_addon.registry;

import com.example.an_addon.entity.SummonEntity;
import com.example.an_addon.item.ContractCardItem;
import com.example.an_addon.item.ExampleCosmetic;
import com.example.an_addon.summon.SummonData;
import com.hollingsworth.arsnouveau.api.sound.SpellSound;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import static com.example.an_addon.ExampleANAddon.MODID;
import static com.example.an_addon.ExampleANAddon.prefix;
import static net.minecraft.core.registries.Registries.SOUND_EVENT;

public class ModRegistry {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(SOUND_EVENT, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    public static void registerRegistries(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        SOUNDS.register(bus);
        ENTITIES.register(bus);
        DATA_COMPONENTS.register(bus);
        bus.addListener(ModRegistry::registerAttributes);
    }

    // ---------------- データコンポーネント ----------------

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SummonData>> SUMMON_DATA =
            DATA_COMPONENTS.register("summon_data", () -> DataComponentType.<SummonData>builder()
                    .persistent(SummonData.CODEC)
                    .networkSynchronized(SummonData.STREAM_CODEC)
                    .build());

    // ---------------- エンティティ ----------------

    public static final DeferredHolder<EntityType<?>, EntityType<SummonEntity>> SUMMON_ENTITY =
            ENTITIES.register("summon", () -> EntityType.Builder.<SummonEntity>of(SummonEntity::new, MobCategory.CREATURE)
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("summon"));

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SUMMON_ENTITY.get(), SummonEntity.createAttributes().build());
    }

    // ---------------- アイテム ----------------

    public static final DeferredHolder<Item, ? extends Item> EXAMPLE;
    public static final DeferredHolder<Item, ContractCardItem> CONTRACT_CARD;

    //this is an example of how to register a sound. You also need to add the sound to the sound.json file, referencing your ogg files, and a texture for the button under textures/sounds.
    public static DeferredHolder<SoundEvent, SoundEvent> EXAMPLE_FAMILY =
            SOUNDS.register("example_sound", () -> makeSound("example_sound"));
    public static SpellSound EXAMPLE_SPELL_SOUND =
            new SpellSound(ModRegistry.EXAMPLE_FAMILY, Component.literal("Example"), prefix("example_random_sound"));

    static {
        EXAMPLE = ITEMS.register("star_hat", () -> new ExampleCosmetic(new Item.Properties()));
        CONTRACT_CARD = ITEMS.register("contract_card", () -> new ContractCardItem(new Item.Properties()));
    }

    static SoundEvent makeSound(@NotNull String name) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, name));
    }
}
