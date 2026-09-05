package com.example.an_addon.block;

import com.example.an_addon.registry.ModRegistry;
import com.example.an_addon.summon.SummonData;
import com.example.an_addon.summon.SummonLottery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class GuildAltarBlock extends Block {

    public GuildAltarBlock(Properties props) {
        super(props);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        boolean blank = stack.is(ModRegistry.BLANK_CONTRACT_CARD.get());
        boolean card = stack.is(ModRegistry.CONTRACT_CARD.get());
        if (!blank && !card) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (card) {
            SummonData old = stack.get(ModRegistry.SUMMON_DATA.get());
            if (old != null && old.level() > 1) {
                player.displayClientMessage(Component.literal("Lv2以上の契約は打ち直せない"), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        SummonData rolled = SummonLottery.roll(level.getRandom());
        ItemStack result = new ItemStack(ModRegistry.CONTRACT_CARD.get());
        result.set(ModRegistry.SUMMON_DATA.get(), rolled);

        stack.shrink(1);
        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }

        playEffects((ServerLevel) level, pos, rolled.tier());
        player.displayClientMessage(Component.literal(
                "契約成立: " + rolled.base().getDisplayName()
                        + " / " + rolled.element().getDisplayName()
                        + " (T" + rolled.tier() + ")"), false);
        return ItemInteractionResult.SUCCESS;
    }

    private static void playEffects(ServerLevel level, BlockPos pos, int tier) {
        level.sendParticles(ParticleTypes.ENCHANT,
                pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                20 * tier, 0.4D, 0.4D, 0.4D, 0.2D);
        if (tier >= 2) {
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 1.2D, pos.getZ() + 0.5D,
                    10 * tier, 0.3D, 0.3D, 0.3D, 0.05D);
        }
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.BLOCKS, 1.0F, 0.8F + 0.2F * tier);
    }
}
