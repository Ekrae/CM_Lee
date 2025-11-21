package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource; // 월드의 랜덤 소스
import net.minecraft.core.Holder;

public class GamblerAbility implements IAbility {

    final int durationInTicks = Config.gambler_duration;
    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "gambler");
    }

    @Override
    public Item getTriggerItem() {
        return Items.COMPASS;
    }

    @Override
    public Component getDescription() {
        return Component.literal("무작위 효과(신속,투명,재생,구속,발광) 중 하나를 얻습니다.");
    }
    @Override
    public int getCooldownSeconds() {
        return Config.gambler_cooldown;
    }

    @Override
    public void execute(ServerPlayer player) {
        // --- 1. 효과 기본 설정 ---
        // 10초
        int amplifier = 0; // 1단계 효과 (0 = I, 1 = II)

        // --- 2. 랜덤 굴리기 (0~4) ---
        // player.level().getRandom()을 사용하는 것이 마인크래프트 표준 방식입니다.

        RandomSource random = player.level().getRandom();
        int roll = random.nextInt(5); // 0, 1, 2, 3, 4 중 하나가 나옴 (각 20% 확률)

        Holder<MobEffect> effectType; // 적용할 효과
        String effectName;      // 피드백 메시지에 쓸 효과 이름

        // --- 3. 결과에 따라 효과 결정 ---
        switch (roll) {
            case 0:
                effectType = MobEffects.INVISIBILITY;
                effectName = "투명";
                break;
            case 1:
                effectType = MobEffects.SPEED;
                effectName = "신속";
                break;
            case 2:
                effectType = MobEffects.GLOWING;
                effectName = "발광";
                break;
            case 3:
                effectType = MobEffects.SLOWNESS;
                effectName = "구속";
                break;
            default: // case 4
                effectType = MobEffects.REGENERATION;
                effectName = "재생";
                break;
        }

        // --- 4. 효과 인스턴스 생성 ---
        // (참고: 투명 효과는 파티클을 false로 하는게 좋지만, 일관성을 위해 true로 둡니다)
        MobEffectInstance effectInstance = new MobEffectInstance(
                effectType,
                durationInTicks,
                amplifier,
                false, // (Ambient) 주변 효과 여부
                true   // (Show Particles) 파티클 표시 여부
        );

        // --- 5. 플레이어에게 효과 적용 ---
        player.addEffect(effectInstance);

        // --- 6. 피드백 메시지 ---
        player.displayClientMessage(Component.literal("도박 성공! '" + effectName + "' 효과 발동!"), true);
    }


}
