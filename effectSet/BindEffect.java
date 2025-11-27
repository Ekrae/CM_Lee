package com.example.examplemod.effectSet;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.example.examplemod.ExampleMod;

public class BindEffect extends MobEffect {

    // 각 속성 변경자를 식별하기 위한 고유 ID
    private static final ResourceLocation SPEED_LOCK_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "bind_speed_lock");
    private static final ResourceLocation JUMP_LOCK_ID = ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "bind_jump_lock");

    public BindEffect(MobEffectCategory category, int color) {
        super(category, color);

        // [핵심] 이 "설계도(BindEffect)"가 만들어질 때, 속성 변경자를 미리 추가합니다.

        // 1. 이동 속도(MOVEMENT_SPEED)를 100% 감소시켜 0으로 만듭니다.
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED, // <-- GENERIC_ 없음
                SPEED_LOCK_ID,
                -0.99,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );

        // 2. 점프력(JUMP_STRENGTH)을 100% 감소시켜 0으로 만듭니다.
        addAttributeModifier(
                Attributes.JUMP_STRENGTH, // <-- GENERIC_ 없음
                JUMP_LOCK_ID,
                -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}