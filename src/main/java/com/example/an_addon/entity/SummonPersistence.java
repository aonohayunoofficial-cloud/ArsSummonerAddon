package com.example.an_addon.entity;

import com.example.an_addon.summon.SummonData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.UUID;

/** 所有者 UUID と SummonData の NBT 保存・読み込みを担当する */
public class SummonPersistence {

    private final SummonEntity summon;

    SummonPersistence(SummonEntity summon) {
        this.summon = summon;
    }

    public void save(CompoundTag tag) {
        UUID owner = summon.getOwnerUUID();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        SummonData.CODEC.encodeStart(NbtOps.INSTANCE, summon.getSummonData())
                .resultOrPartial(err -> System.err.println("SummonData save failed: " + err))
                .ifPresent(t -> tag.put("SummonData", t));
    }

    public void load(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            summon.setOwnerUUID(tag.getUUID("Owner"));
        }
        if (tag.contains("SummonData")) {
            Tag data = tag.get("SummonData");
            SummonData.CODEC.parse(NbtOps.INSTANCE, data)
                    .resultOrPartial(err -> System.err.println("SummonData load failed: " + err))
                    .ifPresent(summon::setSummonData);
        }
    }
}
