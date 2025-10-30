package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class StealAbility implements IAbility {

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "steal");
    }

    @Override
    public Item getTriggerItem() {
        // 이 능력의 트리거 아이템은 '사슬'입니다.
        return Items.CHAIN;
    }

    @Override
    public int getCooldownSeconds() {
        return 60; // 훔치기 자체의 쿨타임
    }

    @Override
    public void execute(ServerPlayer player) {
        // 사슬을 그냥 우클릭(사용)했을 때.
        // 실제 훔치기는 onPlayerHit 이벤트에서 처리됩니다.
        player.displayClientMessage(Component.literal("사슬을 들고 다른 플레이어를 때려서 능력을 훔치세요!"), true);
    }
}