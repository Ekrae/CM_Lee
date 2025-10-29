package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.abilities.IAbility;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import com.example.examplemod.AbilityEvents;
import com.example.examplemod.ExampleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items; // 예시: 시계
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.example.examplemod.abilitySet.PlayerStateSnapshot;

public class TracerAbility implements IAbility {
    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "tracer");
    }

    @Override
    public Item getTriggerItem() {
        return Items.CLOCK; // 트리거 아이템: 시계
    }

    @Override
    public int getCooldownSeconds() {
        return 12;
    }

    @Override
    public void execute(ServerPlayer player) {
        // 기록이 있는지 간단히 확인
        PlayerStateSnapshot targetState = AbilityEvents.getOldestSnapshot(player);

        if (targetState == null) {
            player.displayClientMessage(Component.literal("되돌아갈 기록이 없습니다!"), true);
            return;
        }

        // [수정!] 실제 로직 대신 AbilityEvents의 메서드를 호출합니다.
        AbilityEvents.startRewind(player);
    }
}
