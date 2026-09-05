package com.example.an_addon.command;

import com.example.an_addon.entity.SummonEntity;
import com.example.an_addon.summon.SummonBase;
import com.example.an_addon.summon.SummonElement;
import com.example.an_addon.config.AddonConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.example.an_addon.ExampleANAddon.MODID;

@EventBusSubscriber(modid = MODID)
public class SummonCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("ansummon")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("exp")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> forEachSummon(ctx.getSource(),
                                                s -> s.grantExp(IntegerArgumentType.getInteger(ctx, "amount"))))))
                        .then(Commands.literal("level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 20))
                                        .executes(ctx -> {
                                            int lv = IntegerArgumentType.getInteger(ctx, "level");
                                            return forEachSummon(ctx.getSource(), s -> {
                                                s.setSummonData(s.getSummonData().withLevelForced(lv));
                                                s.syncToCard();
                                            });
                                        })))
                        .then(Commands.literal("base")
                                .then(Commands.argument("base", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(SummonBase.values()).map(SummonBase::getId), b))
                                        .executes(ctx -> {
                                            SummonBase base = SummonBase.byId(StringArgumentType.getString(ctx, "base"));
                                            return forEachSummon(ctx.getSource(), s -> {
                                                s.setSummonData(s.getSummonData()
                                                        .withBaseElement(base, s.getSummonData().element()));
                                                s.syncToCard();
                                            });
                                        })))
                        .then(Commands.literal("element")
                                .then(Commands.argument("element", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(SummonElement.values()).map(SummonElement::getId), b))
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "element");
                                            SummonElement el = Arrays.stream(SummonElement.values())
                                                    .filter(e -> e.getId().equalsIgnoreCase(id))
                                                    .findFirst().orElse(SummonElement.NONE);
                                            return forEachSummon(ctx.getSource(), s -> {
                                                s.setSummonData(s.getSummonData()
                                                        .withBaseElement(s.getSummonData().base(), el));
                                                s.syncToCard();
                                            });
                                        })))
                        .then(Commands.literal("info")
                                .executes(ctx -> forEachSummon(ctx.getSource(), s -> {
                                    var d = s.getSummonData();
                                    var abilities = com.example.an_addon.summon.ability.AbilityTable
                                            .unlocked(d.base(), d.level());
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            d.base().getDisplayName() + "/" + d.element().getDisplayName()
                                                    + " T" + d.tier() + " Lv" + d.level()
                                                    + " EXP " + d.exp() + "/" + d.expToNext()
                                                    + " 関係値 " + d.relationship()
                                                    + " 憑依中=" + d.possessing()
                                                    + " 能力=" + abilities
                                                    + " 札同期=" + s.syncToCard()), false);
                                })))
                        .then(Commands.literal("config")
                                .then(Commands.literal("expmsg")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    boolean v = BoolArgumentType.getBool(ctx, "enabled");
                                                    AddonConfig.SHOW_EXP_GAIN.set(v);
                                                    AddonConfig.SHOW_EXP_GAIN.save();
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "経験値表示: " + (v ? "ON" : "OFF")), false);
                                                    return 1;
                                                })))
                                .then(Commands.literal("ridespeed")
                                        .then(Commands.argument("multiplier", DoubleArgumentType.doubleArg(0.1D, 3.0D))
                                                .executes(ctx -> {
                                                    double v = DoubleArgumentType.getDouble(ctx, "multiplier");
                                                    AddonConfig.RIDE_SPEED_MULTIPLIER.set(v);
                                                    AddonConfig.RIDE_SPEED_MULTIPLIER.save();
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "騎乗速度倍率: " + v), false);
                                                    return 1;
                                                }))))
        );
    }

    private static int forEachSummon(CommandSourceStack source, Consumer<SummonEntity> action) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("プレイヤーから実行してください"));
            return 0;
        }
        List<SummonEntity> list = player.level().getEntitiesOfClass(
                SummonEntity.class, player.getBoundingBox().inflate(32.0D),
                s -> player.getUUID().equals(s.getOwnerUUID()));
        if (list.isEmpty()) {
            source.sendFailure(Component.literal("近くに自分の召喚獣がいません"));
            return 0;
        }
        list.forEach(action);
        return list.size();
    }
}
