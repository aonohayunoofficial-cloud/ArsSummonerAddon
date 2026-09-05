package com.example.an_addon.item;

import com.example.an_addon.entity.SummonEntity;
import com.example.an_addon.registry.ModRegistry;
import com.example.an_addon.summon.SummonData;
import com.example.an_addon.summon.ability.AbilityId;
import com.example.an_addon.summon.ability.AbilityTable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class ContractCardItem extends Item {

    public ContractCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        ServerLevel serverLevel = (ServerLevel) level;
        SummonData data = stack.getOrDefault(ModRegistry.SUMMON_DATA.get(), SummonData.DEFAULT);

        Entity existing = data.entityId().map(serverLevel::getEntity).orElse(null);
        SummonEntity summon = existing instanceof SummonEntity se ? se : null;

        if (player.isShiftKeyDown()) {
            if (summon == null) {
                player.displayClientMessage(Component.literal("先に召喚してください"), true);
                return InteractionResultHolder.success(stack);
            }
            summon.togglePossession(player);
            stack.set(ModRegistry.SUMMON_DATA.get(), summon.getSummonData());
            return InteractionResultHolder.success(stack);
        }

        if (summon != null) {
            SummonData latest = summon.getSummonData().withPossessing(false).withEntity(Optional.empty());
            stack.set(ModRegistry.SUMMON_DATA.get(), latest);
            summon.ejectPassengers();
            summon.discard();
            player.displayClientMessage(Component.literal(
                    latest.base().getDisplayName() + " を送還した"), true);
            return InteractionResultHolder.success(stack);
        }

        if (data.entityId().isPresent()) {
            data = data.withEntity(Optional.empty()).withPossessing(false);
        }

        Vec3 spawnPos = player.position().add(player.getLookAngle().scale(2.0D));
        SummonEntity created = new SummonEntity(ModRegistry.SUMMON_ENTITY.get(), serverLevel);
        created.moveTo(spawnPos.x, player.getY(), spawnPos.z, player.getYRot(), 0.0F);
        created.setOwnerUUID(player.getUUID());
        created.setSummonData(data.withEntity(Optional.of(created.getUUID())));
        created.setHealth(created.getMaxHealth());
        serverLevel.addFreshEntity(created);

        stack.set(ModRegistry.SUMMON_DATA.get(), created.getSummonData());
        player.displayClientMessage(Component.literal(
                data.base().getDisplayName() + " を召喚した"), true);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SummonData data = stack.get(ModRegistry.SUMMON_DATA.get());
        if (data == null) {
            tooltip.add(Component.literal("未契約の空札").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.literal(
                data.base().getDisplayName() + "（ベース）／"
                        + data.element().getDisplayName() + "（属性）").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Tier " + data.tier() + "  Lv " + data.level())
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(data.level() < SummonData.LEVEL_CAP
                ? "EXP " + data.exp() + " / " + data.expToNext()
                : "EXP -- (上限)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("関係値 " + data.relationship() + " / 100")
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        List<AbilityId> unlocked = AbilityTable.unlocked(data.base(), data.level());
        if (!unlocked.isEmpty()) {
            StringBuilder sb = new StringBuilder("能力: ");
            for (int i = 0; i < unlocked.size(); i++) {
                if (i > 0) sb.append("・");
                sb.append(unlocked.get(i).getDisplayName());
            }
            tooltip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.GREEN));
        }
        AbilityTable.Entry next = AbilityTable.next(data.base(), data.level());
        if (next != null) {
            tooltip.add(Component.literal(
                            "次の解放: Lv" + next.level() + " " + next.ability().getDisplayName())
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        tooltip.add(Component.literal(
                        data.possessing() ? "状態: 憑依中"
                                : data.entityId().isPresent() ? "状態: 召喚中" : "状態: 待機")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        SummonData data = stack.get(ModRegistry.SUMMON_DATA.get());
        return data != null && data.entityId().isPresent();
    }
}
