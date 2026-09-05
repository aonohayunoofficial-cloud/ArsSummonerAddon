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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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

        // 契約済みの札は再契約しない。上限到達なら進化・限界突破の分岐へ入る
        if (card) {
            SummonData data = stack.get(ModRegistry.SUMMON_DATA.get());
            if (data == null) {
                player.displayClientMessage(Component.literal("この札は壊れている"), true);
                return ItemInteractionResult.SUCCESS;
            }
            if (data.isAtCap()) {
                return handleCapBranch((ServerLevel) level, pos, player, stack, data);
            }
            player.displayClientMessage(Component.literal(
                    "契約済み: " + data.base().getDisplayName()
                            + " Lv" + data.level() + "/" + data.levelCap()
                            + "（上限で進化・限界突破が選べる）"), true);
            return ItemInteractionResult.SUCCESS;
        }

        // 白紙の札からの新規契約。ティアは常に T1
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

    /** Lv上限到達札。進化石があれば進化、無ければ限界突破の証を消費する */
    private static ItemInteractionResult handleCapBranch(ServerLevel level, BlockPos pos,
                                                         Player player, ItemStack stack, SummonData data) {
        if (data.entityId().isPresent()) {
            player.displayClientMessage(Component.literal("召喚獣を札に戻してから行う"), true);
            return ItemInteractionResult.SUCCESS;
        }

        if (data.canEvolve() && consume(player, ModRegistry.EVOLUTION_STONE.get())) {
            SummonData next = data.evolve();
            stack.set(ModRegistry.SUMMON_DATA.get(), next);
            playEffects(level, pos, next.tier());
            player.displayClientMessage(Component.literal(
                    "進化: T" + data.tier() + " → T" + next.tier() + "（Lv1 から再出発）"), false);
            return ItemInteractionResult.SUCCESS;
        }

        if (data.canBreakthrough() && consume(player, ModRegistry.BREAKTHROUGH_TOKEN.get())) {
            SummonData next = data.breakthrough();
            stack.set(ModRegistry.SUMMON_DATA.get(), next);
            playEffects(level, pos, next.tier());
            player.displayClientMessage(Component.literal(
                    "限界突破: レベル上限 " + data.levelCap() + " → " + next.levelCap()), false);
            return ItemInteractionResult.SUCCESS;
        }

        String need;
        if (data.canEvolve() && data.canBreakthrough()) {
            need = "進化石 または 限界突破の証 が要る";
        } else if (data.canEvolve()) {
            need = "進化石 が要る";
        } else if (data.canBreakthrough()) {
            need = "限界突破の証 が要る";
        } else {
            need = "この札はこれ以上伸ばせない";
        }
        player.displayClientMessage(Component.literal(need), true);
        return ItemInteractionResult.SUCCESS;
    }

    /** インベントリ全スロットから1個消費する。クリエイティブでも消費する */
    private static boolean consume(Player player, Item item) {
        int idx = findIndex(player.getInventory(), item);
        if (idx < 0) return false;
        player.getInventory().removeItem(idx, 1);
        return true;
    }

    private static int findIndex(Inventory inv, Item item) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) return i;
        }
        return -1;
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
