package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.Config;
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
import net.minecraft.world.phys.Vec3;
import com.example.examplemod.AbilityEvents; // [추가]

import java.util.List;

public class SwapAbility implements IAbility {

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "swap");
    }

    @Override
    public Item getTriggerItem() {
        // 트리거 아이템: 엔더의 눈
        return Items.ENDER_EYE;
    }
    @Override
    public Component getDescription() {
        return Component.literal("40블록 내의 가장 가까운 대상과 위치를 바꿉니다. ('pol' 태그 제외)");
    }

    @Override
    public int getCooldownSeconds() {
        return Config.swap_cooldown; // 20초 쿨타임
    }

    @Override
    @SuppressWarnings("resource")
    public void execute(ServerPlayer caster) {
        ServerLevel level = (ServerLevel) caster.level();
        double maxDistanceSqr = Config.swap_range * Config.swap_range; // 40블록 (제곱 거리)

        ServerPlayer closestTarget = null;
        double closestDistSqr = Double.MAX_VALUE;

        // --- 1. 대상 탐색 ---
        // 서버의 모든 플레이어를 순회하며 조건을 검사합니다.
        for (ServerPlayer target : level.getServer().getPlayerList().getPlayers()) {
            // 1. 자기 자신은 제외
            if (target == caster) {
                continue;
            }

            // 2. "pol" 태그가 있는지 검사 (태그가 있으면 제외). 태영이가 만든 pol시스템과 호환됨
            if (target.getTags().contains("pol")) {
                continue;
            }

            // 3. 거리 검사
            double distSqr = caster.distanceToSqr(target);
            if (distSqr < maxDistanceSqr && distSqr < closestDistSqr) {
                // 더 가까운 유효한 대상을 찾음
                closestDistSqr = distSqr;
                closestTarget = target;
            }
        }

        // --- 2. 능력 실행 ---
        if (closestTarget != null) {
            // 유효한 대상("closestTarget")을 찾은 경우

            // 2-1. 위치 및 시야각 정보 저장
            Vec3 casterPos = caster.position();
            float casterYRot = caster.getYRot();
            float casterXRot = caster.getXRot();

            Vec3 targetPos = closestTarget.position();
            float targetYRot = closestTarget.getYRot();
            float targetXRot = closestTarget.getXRot();

            // 2-2. 텔레포트 실행 (위치 + 시야각)
            caster.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            caster.setYRot(targetYRot);
            caster.setXRot(targetXRot);

            closestTarget.teleportTo(casterPos.x, casterPos.y, casterPos.z);
            closestTarget.setYRot(casterYRot);
            closestTarget.setXRot(casterXRot);

            // 2-3. 시각 및 청각 효과 (양쪽 위치에)
            level.playSound(null, casterPos.x, casterPos.y, casterPos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

            level.sendParticles(ParticleTypes.PORTAL, casterPos.x, casterPos.y + 1.0, casterPos.z, 50, 0.5, 0.5, 0.5, 0.1);
            level.sendParticles(ParticleTypes.PORTAL, targetPos.x, targetPos.y + 1.0, targetPos.z, 50, 0.5, 0.5, 0.5, 0.1);

            // 2-4. 피드백 메시지
            caster.displayClientMessage(Component.literal(closestTarget.getName().getString() + "님과 위치를 바꿨습니다!"), true);
            closestTarget.displayClientMessage(Component.literal(caster.getName().getString() + "님과 위치가 바뀌었습니다!"), true);

        } else {
            // 유효한 대상을 찾지 못한 경우
            caster.displayClientMessage(Component.literal("능력 범위 내에 대상이 없습니다."), true);
            AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(caster.getUUID(), 0L); // [추가]
        }
    }
}