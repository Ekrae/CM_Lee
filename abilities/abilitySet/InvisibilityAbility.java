package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class InvisibilityAbility implements IAbility {

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "invisibility");
    }

    @Override
    public Item getTriggerItem() {
        // 트리거: 하얀색 염료
        return Items.WHITE_DYE;
    }
    @Override
    public Component getDescription() {
        return Component.literal("5초간 파티클이 없는 투명화 상태가 됩니다.");
    }

    @Override
    public int getCooldownSeconds() {
        // 쿨타임 (적절하게 20초로 설정했습니다)
        return 20;
    }

    @Override
    public void execute(ServerPlayer player) {
        // 1. 설정: 5초
        int durationInTicks = 5 * 20;
        int amplifier = 0; // 1단계

        // 2. 효과 "상자(Holder)" 가져오기
        Holder<MobEffect> invisibilityHolder = MobEffects.INVISIBILITY;

        // 3. 효과 인스턴스 생성
        MobEffectInstance effectInstance = new MobEffectInstance(
                invisibilityHolder,
                durationInTicks,
                amplifier,
                false, // (Ambient) 주변 효과 여부
                false  // (Show Particles) [중요!] 투명화는 파티클을 숨겨야 함
        );

        // 4. 플레이어에게 효과 적용
        player.addEffect(effectInstance);

        // 5. 피드백
        player.displayClientMessage(Component.literal("투명화되었습니다."), true);
    }
}