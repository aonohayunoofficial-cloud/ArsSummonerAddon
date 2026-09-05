package com.example.an_addon.entity;

import com.example.an_addon.config.AddonConfig;
import com.example.an_addon.item.ContractCardItem;
import com.example.an_addon.registry.ModRegistry;
import com.example.an_addon.summon.SummonData;
import com.example.an_addon.summon.ability.AbilityId;
import com.example.an_addon.summon.ability.AbilityTable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** ステータス算出・表示名・経験値・契約札への同期を担当する */
public class SummonProgression {

    private final SummonEntity summon;

    SummonProgression(SummonEntity summon) {
        this.summon = summon;
    }

    /** レベルとティアから属性値を再計算する。ティアは倍率（T1 1.00 〜 T5 2.00） */
    public void applyStats() {
        SummonData data = summon.getSummonData();
        int lv = data.level();
        double tierMul = 1.0D + (data.tier() - 1) * 0.25D;
        double hp = (20.0D + (lv - 1) * 1.5D) * tierMul;
        double atk = (3.0D + (lv - 1) * 0.35D) * tierMul;
        double jump = summon.hasAbility(AbilityId.HIGH_JUMP) ? 1.05D : 0.7D;

        if (summon.getAttribute(Attributes.MAX_HEALTH) != null) {
            summon.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        }
        if (summon.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            summon.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(atk);
        }
        if (summon.getAttribute(Attributes.JUMP_STRENGTH) != null) {
            summon.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(jump);
        }
        if (summon.getHealth() > hp) {
            summon.setHealth((float) hp);
        }
    }

    public void updateDisplayName() {
        SummonData data = summon.getSummonData();
        summon.setCustomName(Component.literal(
                data.base().getDisplayName()
                        + "（" + data.element().getDisplayName() + "）"
                        + " T" + data.tier()
                        + " Lv" + data.level()));
        summon.setCustomNameVisible(!summon.isPossessing());
    }

    public void grantExp(int amount) {
        if (summon.level().isClientSide || amount <= 0) return;
        int beforeLv = summon.getSummonData().level();
        summon.setSummonData(summon.getSummonData().grantExp(amount));
        syncToCard();
        if (!(summon.getOwnerEntity() instanceof Player player)) return;

        SummonData data = summon.getSummonData();
        int afterLv = data.level();

        if (afterLv > beforeLv) {
            if (AddonConfig.SHOW_LEVEL_UP.get()) {
                player.displayClientMessage(Component.literal(
                        "★ " + data.base().getDisplayName() + " が Lv" + afterLv + " になった"), false);
            }
            if (AddonConfig.SHOW_ABILITY_UNLOCK.get()) {
                for (AbilityTable.Entry e : AbilityTable.entriesFor(data.base())) {
                    if (e.level() > beforeLv && e.level() <= afterLv) {
                        player.displayClientMessage(Component.literal(
                                "【解放】" + e.ability().getDisplayName()), false);
                    }
                }
            }
        } else if (AddonConfig.SHOW_EXP_GAIN.get()) {
            player.displayClientMessage(Component.literal(
                    "+" + amount + " EXP (" + data.exp() + "/" + data.expToNext() + ")"), true);
        }
    }

    public boolean syncToCard() {
        if (!(summon.getOwnerEntity() instanceof Player player)) return false;
        ItemStack card = findCard(player);
        if (card.isEmpty()) return false;
        card.set(ModRegistry.SUMMON_DATA.get(), summon.getSummonData());
        return true;
    }

    /** 死亡時に札へ関係値 -10 を書き戻し、召喚状態を解除する */
    public void onDeath() {
        if (summon.level().isClientSide) return;
        if (!(summon.getOwnerEntity() instanceof Player player)) return;

        SummonData data = summon.getSummonData();
        ItemStack card = findCard(player);
        if (!card.isEmpty()) {
            card.set(ModRegistry.SUMMON_DATA.get(),
                    data.withRelationship(data.relationship() - 10)
                            .withPossessing(false)
                            .withEntity(Optional.empty()));
        }
        if (AddonConfig.SHOW_STATE_MESSAGE.get()) {
            player.displayClientMessage(Component.literal(
                    data.base().getDisplayName() + " が倒れた（関係値 -10）"), true);
        }
    }

    /** この召喚獣に紐付いた契約札を所有者のインベントリから探す */
    public ItemStack findCard(Player player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!(stack.getItem() instanceof ContractCardItem)) continue;
            SummonData cardData = stack.get(ModRegistry.SUMMON_DATA.get());
            if (cardData != null && cardData.entityId().isPresent()
                    && cardData.entityId().get().equals(summon.getUUID())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
