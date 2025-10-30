package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import java.util.List;

public class BindAbility implements IAbility {
    // 이 ID는 이 능력 효과를 식별하는 데 사용됩니다.
    private static final ResourceLocation JUMP_LOCK_ID =        // <-- 이름 변경 (ID)
            ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "bind_jump_lock");
    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "bind");
    }

    @Override
    public Item getTriggerItem() {
        return Items.VINE; // 트리거 아이템: 덩굴
    }

    @Override
    public int getCooldownSeconds() {
        return 15; // 15초 쿨타임
    }

    @Override
    @SuppressWarnings("resource")
    public void execute(ServerPlayer caster) {
        double radius = 4.0;
        int durationInTicks = 3 * 20; // 3초

        ServerLevel level = (ServerLevel) caster.level();
        AABB searchArea = caster.getBoundingBox().inflate(radius);

        List<ServerPlayer> targets = level.getEntitiesOfClass(
                ServerPlayer.class,
                searchArea,
                player -> player != caster
        );

        if (targets.isEmpty()) {
            caster.displayClientMessage(Component.literal("능력 범위 내에 다른 플레이어가 없습니다."), true);
            return;
        }

        // --- [핵심 수정] ---
        // 1. ExampleMod에 등록된 커스텀 효과의 "상자(Holder)"를 가져옵니다.
        Holder<MobEffect> bindEffectHolder = ExampleMod.BIND_EFFECT.getHolder().orElseThrow();

        for (ServerPlayer target : targets) {
            // 2. 이 "상자"로 효과 인스턴스를 만듭니다. (AttributeModifier 로직 완전 삭제)
            MobEffectInstance bindInstance = new MobEffectInstance(
                    bindEffectHolder,
                    durationInTicks,
                    0, // 증폭 레벨은 0 (필요 없음)
                    false,
                    false
            );

            // 3. 대상에게 우리의 커스텀 효과를 적용합니다.
            target.addEffect(bindInstance);

            // (시각/청각 효과는 동일)
            level.playSound(null, target.blockPosition(), SoundEvents.VINE_PLACE, SoundSource.PLAYERS, 1.0f, 1.0f);
            BlockParticleOption vineParticle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.VINE.defaultBlockState());
            level.sendParticles(vineParticle, target.getX(), target.getY() + 1.0, target.getZ(), 30, 0.3, 0.5, 0.3, 0.1);
        }

        caster.displayClientMessage(Component.literal(targets.size() + "명의 플레이어를 구속했습니다!"), true);
    }
}