package com.example.an_addon.event;

import com.example.an_addon.entity.SummonEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static com.example.an_addon.ExampleANAddon.MODID;

@EventBusSubscriber(modid = MODID)
public class SummonEvents {

    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) return;
        if (dead instanceof SummonEntity) return;

        Entity killer = event.getSource().getEntity();
        int baseExp = 5 + (int) dead.getMaxHealth();

        LOGGER.info("[an_addon] death={} killer={}", dead.getType(), killer == null ? "null" : killer.getType());

        if (killer instanceof SummonEntity summon) {
            summon.grantExp(baseExp);
            return;
        }

        if (killer instanceof Player player) {
            List<SummonEntity> nearby = dead.level().getEntitiesOfClass(
                    SummonEntity.class, dead.getBoundingBox().inflate(16.0D),
                    s -> player.getUUID().equals(s.getOwnerUUID()));
            LOGGER.info("[an_addon] nearby owned summons = {}", nearby.size());
            for (SummonEntity summon : nearby) {
                summon.grantExp(Math.max(1, baseExp * 3 / 5));
            }
        }
    }
}
