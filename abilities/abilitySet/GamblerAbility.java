package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class GamblerAbility implements IAbility {

    // [삭제] 클래스 로딩 시점이 아닌 실행 시점에 Config 값을 가져와야 하므로 삭제
    // final int durationInTicks = Config.gambler_duration * 20;

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "gambler");
    }

    @Override
    public Item getTriggerItem() {
        return Items.COMPASS;
    }

    @Override
    public int getCooldownSeconds() {
        return Config.gambler_cooldown;
    }

    @Override
    public Component getDescription() {
        return Component.literal("무작위 효과를 얻습니다. (성공: 투명/신속/재생, 실패: 발광/구속)");
    }

    @Override
    public void execute(ServerPlayer player) {
        // 1. 랜덤 굴리기 (0~4)
        RandomSource random = player.level().getRandom();
        int roll = random.nextInt(5);

        Holder<MobEffect> effectType;
        String effectName;
        boolean isSuccess; // 성공 여부 체크

        // 2. 결과 판정
        switch (roll) {
            case 0:
                effectType = MobEffects.INVISIBILITY;
                effectName = "투명";
                isSuccess = true;
                break;
            case 1:
                effectType = MobEffects.SPEED;
                effectName = "신속";
                isSuccess = true;
                break;
            case 2:
                effectType = MobEffects.GLOWING;
                effectName = "발광";
                isSuccess = false; // 꽝
                break;
            case 3:
                effectType = MobEffects.SLOWNESS;
                effectName = "구속";
                isSuccess = false; // 꽝
                break;
            default: // case 4
                effectType = MobEffects.REGENERATION;
                effectName = "재생";
                isSuccess = true;
                break;
        }

        // 3. 지속 시간 설정 (성공/실패에 따라 다르게)
        int durationInSeconds = isSuccess ? Config.gambler_duration : Config.gambler_fail_duration;
        int durationInTicks = durationInSeconds * 20;
        int amplifier = 5; // 1단계 효과

        // 4. 효과 적용
        MobEffectInstance effectInstance = new MobEffectInstance(
                effectType,
                durationInTicks,
                amplifier,
                false, // Ambient
                true   // Show Particles
        );
        player.addEffect(effectInstance);

        // 5. 피드백 메시지 (성공은 초록색, 실패는 빨간색)
        if (isSuccess) {
            player.displayClientMessage(Component.literal("§a도박 성공! §f'" + effectName + "' 효과 발동! (" + durationInSeconds + "초)"), true);
        } else {
            player.displayClientMessage(Component.literal("§c도박 실패... §f'" + effectName + "' 효과 발동... (" + durationInSeconds + "초)"), true);
        }
    }
}