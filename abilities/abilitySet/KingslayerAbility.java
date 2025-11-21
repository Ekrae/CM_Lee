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
import net.minecraft.world.phys.Vec3; // 텔레포트를 위해 Vec3 Import
import com.example.examplemod.AbilityEvents; // <-- [1. 이것을 추가하세요]

public class KingslayerAbility implements IAbility {

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "kingslayer");
    }

    @Override
    public Item getTriggerItem() {
        return Items.GOLDEN_SWORD;
    }

    @Override
    public int getCooldownSeconds() {
        return 15;
    }
    @Override
    public Component getDescription() {
        return Component.literal("가까운 적에게 돌진하여 피해를 주고 체력을 흡수합니다.");
    }

    @Override
    @SuppressWarnings("resource")
    public void execute(ServerPlayer caster) {
        ServerLevel level = (ServerLevel) caster.level();

        // 1. 대상 탐색 (범위 5)
        double maxDistanceSqr = Config.kslayerRange * Config.kslayerRange;
        ServerPlayer closestTarget = null;
        double closestDistSqr = Double.MAX_VALUE;

        for (ServerPlayer target : level.getServer().getPlayerList().getPlayers()) {
            if (target == caster) {
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

            // --- [핵심 추가] 대상에게 돌진 (텔레포트) ---
            // 2-1. 대상의 위치를 가져옵니다.
            Vec3 targetPos = closestTarget.position();
            // 2-2. 시전자를 대상의 위치로 텔레포트시킵니다.
            caster.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            // 2-3. (중요) 텔레포트 직후 낙하 데미지를 받지 않도록 낙하 거리를 초기화합니다.
            caster.fallDistance = 0.0F;
            // ------------------------------------------

            // 2-4. (기존 로직) "pol" 태그 여부 확인
            boolean isTargetPol = closestTarget.getTags().contains("pol");

            float healAmount;
            String feedbackMessage;
            String targetFeedbackMessage;

            if (isTargetPol) {
                // [A] 대상이 'pol' (술래)
                healAmount = 12.0F; // 6하트
                float damage = 4.0F; // 2하트 데미지

                closestTarget.hurt(caster.damageSources().playerAttack(caster), damage);
                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.5F);
                level.playSound(null, closestTarget.getX(), closestTarget.getY(), closestTarget.getZ(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1.0F, 1.0F);
                level.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY(0.7), caster.getZ(), 10, 0.3, 0.3, 0.3, 0.1);

                feedbackMessage = "술래(" + closestTarget.getName().getString() + ")에게 돌진하여 체력을 3칸 흡수합니다!";
                targetFeedbackMessage = caster.getName().getString() + "이(가) 당신에게 돌진하여 힘을 흡수합니다!";

            } else {
                // [B] 대상이 'pol'이 아닌 (도망자)
                healAmount = 3.0F; // 1.5하트
                float damage = (float) Config.kslayerDamageRunner; // 3하트 데미지

                closestTarget.hurt(caster.damageSources().playerAttack(caster), damage);

                level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, closestTarget.getX(), closestTarget.getY(), closestTarget.getZ(), SoundEvents.GENERIC_HURT, SoundSource.PLAYERS, 1.0F, 1.0F);
                level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, closestTarget.getX(), closestTarget.getY(0.5), closestTarget.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                level.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY(0.7), caster.getZ(), 5, 0.3, 0.3, 0.3, 0.1);

                feedbackMessage = "술래시해자! " + closestTarget.getName().getString() + "에게 돌진하여 피해를 주고 1.5칸 회복합니다!";
                targetFeedbackMessage = caster.getName().getString() + "에게 공격당했습니다!";
            }

            // 2-5. 공통: 시전자 회복 (최신 API)
            float currentHealth = caster.getHealth();
            float maxHealth = caster.getMaxHealth();
            caster.setHealth(Math.min(currentHealth + healAmount, maxHealth));

            // 2-6. 공통: 피드백 메시지 (최신 API)
            caster.sendSystemMessage(Component.literal(feedbackMessage));
            closestTarget.sendSystemMessage(Component.literal(targetFeedbackMessage));

        } else {
            // 대상 없음 (최신 API)
            caster.sendSystemMessage(Component.literal("능력 범위 내에 대상이 없습니다."));
            // 쿨타임을 즉시 초기화 (0L = 0틱)
            AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(caster.getUUID(), 0L);
        }
    }
}