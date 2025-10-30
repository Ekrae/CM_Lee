package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.AbilityEvents;
import com.example.examplemod.ExampleMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class HackAbility implements IAbility {

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "hack");
    }

    @Override
    public Item getTriggerItem() {
        // 트리거 아이템: 덫 갈고리 (Tripwire Hook)
        return Items.TRIPWIRE_HOOK;
    }

    @Override
    public int getCooldownSeconds() {
        return 30; // 30초 쿨타임
    }

    @Override
    @SuppressWarnings("resource")
    public void execute(ServerPlayer caster) {
        ServerLevel level = (ServerLevel) caster.level();
        double range = 10.0; // 10블록 범위
        long durationTicks = 5 * 20; // 5초

        AABB searchArea = caster.getBoundingBox().inflate(range);

        // 범위 내 자신을 제외한 모든 플레이어 탐색
        List<ServerPlayer> targets = level.getEntitiesOfClass(
                ServerPlayer.class,
                searchArea,
                player -> player != caster
        );

        int successCount = 0;
        long hackEndTime = level.getGameTime() + durationTicks;

        for (ServerPlayer target : targets) {
            // "pol" 태그를 가지고 있고, "hacked_pol" 태그가 없는 대상만
            if (target.getTags().contains("pol") && !target.getTags().contains("hacked_pol")) {

                // 1. 태그 변경
                target.removeTag("pol");
                target.addTag("hacked_pol");

                // 2. 5초 뒤 복구되도록 AbilityEvents에 등록
                AbilityEvents.scheduleHackRevert(target.getUUID(), hackEndTime);

                // 3. 효과
                level.playSound(null, target.blockPosition(), SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.PLAYERS, 1.0f, 0.5f);
                level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.5, 0.3, 0.1);

                target.displayClientMessage(Component.literal("시스템이 해킹당했습니다! (5초)"), true);
                successCount++;
            }
        }

        // 4. 시전자 피드백
        if (successCount > 0) {
            caster.displayClientMessage(Component.literal(successCount + "명의 대상을 해킹했습니다!"), true);
        } else {
            caster.displayClientMessage(Component.literal("범위 내에 'pol' 태그를 가진 대상이 없습니다."), true);
        }
    }
}