package com.example.examplemod.abilities.abilitySet;

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

public class KingslayerAbility implements IAbility {

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "kingslayer");
    }

    @Override
    public Item getTriggerItem() {
        // '금 검'을 트리거로 유지
        return Items.GOLDEN_SWORD;
    }

    @Override
    public int getCooldownSeconds() {
        return 15; // 15초 쿨타임
    }

    @Override
    @SuppressWarnings("resource")
    public void execute(ServerPlayer caster) {
        ServerLevel level = (ServerLevel) caster.level();

        // 1. 대상 탐색 (기존과 동일)
        double maxDistanceSqr = 5.0 * 5.0; // 사거리 5블록
        ServerPlayer closestTarget = null;
        double closestDistSqr = Double.MAX_VALUE;

        // [수정] SwapAbility와 달리 'pol' 태그가 있어도 대상으로 감지합니다.
        for (ServerPlayer target : level.getServer().getPlayerList().getPlayers()) {
            if (target == caster) { // 자기 자신만 제외
                continue;
            }

            double distSqr = caster.distanceToSqr(target);
            if (distSqr < maxDistanceSqr && distSqr < closestDistSqr) {
                closestDistSqr = distSqr;
                closestTarget = target;
            }
        }

        // 2. 능력 실행
        if (closestTarget != null) {

            // --- [핵심 수정] 대상의 "pol" 태그 여부 확인 ---
            boolean isTargetPol = closestTarget.getTags().contains("pol");

            float healAmount;
            String feedbackMessage;
            String targetFeedbackMessage;

            if (isTargetPol) {
                // [A] 대상이 'pol' (술래)인 경우: 데미지 X, 시전자 3하트 회복
                healAmount = 6.0F; // 3하트

                // (데미지를 주지 않음)

                // 효과 (소리 및 파티클) - 회복 강조
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.5F);
                level.playSound(null, closestTarget.getX(), closestTarget.getY(), closestTarget.getZ(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.0F, 1.0F);

                // 회복 파티클 (시전자 위치)
                level.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY(0.7), caster.getZ(), 10, 0.3, 0.3, 0.3, 0.1);

                // 피드백 메시지
                feedbackMessage = "술래(" + closestTarget.getName().getString() + ")에게서 체력을 3칸 흡수합니다!";
                targetFeedbackMessage = caster.getName().getString() + "이(가) 당신에게서 힘을 흡수합니다!";

            } else {
                // [B] 대상이 'pol'이 아닌 (도망자) 경우: 데미지 O, 시전자 1.5하트 회복
                healAmount = 3.0F; // 1.5하트
                float damage = 6.0F; // 3하트 데미지

                // 대상에게 피해
                closestTarget.hurt(caster.damageSources().playerAttack(caster), damage);

                // 효과 (소리 및 파티클) - 공격/회복
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, closestTarget.getX(), closestTarget.getY(), closestTarget.getZ(), SoundEvents.GENERIC_HURT, SoundSource.PLAYERS, 1.0F, 1.0F);

                // 피해 파티클 (대상 위치)
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, closestTarget.getX(), closestTarget.getY(0.5), closestTarget.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                // 회복 파티클 (시전자 위치)
                level.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY(0.7), caster.getZ(), 5, 0.3, 0.3, 0.3, 0.1);

                // 피드백 메시지
                feedbackMessage = "국왕시해자! " + closestTarget.getName().getString() + "에게 피해를 주고 1.5칸 회복합니다!";
                targetFeedbackMessage = caster.getName().getString() + "에게 공격당했습니다!";
            }

            // 2-3. 공통: 시전자 회복
            caster.heal(healAmount);

            // 2-4. 공통: 피드백 메시지 전송
            caster.displayClientMessage(Component.literal(feedbackMessage), true);
            closestTarget.displayClientMessage(Component.literal(targetFeedbackMessage), true);

        } else {
            // 유효한 대상을 찾지 못한 경우
            caster.displayClientMessage(Component.literal("능력 범위 내에 대상이 없습니다."), true);
        }
    }
}