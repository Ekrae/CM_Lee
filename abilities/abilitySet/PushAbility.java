package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

public class PushAbility implements IAbility {

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "push");
    }

    @Override
    public Item getTriggerItem() {
        return Items.PISTON;
    }

    @Override
    public Component getDescription() {
        return Component.literal("데미지 없이 적을 멀리 밀쳐내는 '밀쳐내기 막대'를 1개 생성합니다.");
    }
    @Override
    public int getCooldownSeconds() {
        return 10;
    }

    @Override
    public void execute(ServerPlayer player) {
        // 1. 기반 아이템을 '목검'으로 변경
        ItemStack customSword = new ItemStack(Items.WOODEN_SWORD);
        double knockback = Config.push_strength;
        // 2. 아이템에 부여할 속성 정의
        ItemAttributeModifiers attributeModifiers = ItemAttributeModifiers.builder()

                // [속성 A] 강력한 밀치기 속성 (기존과 동일)
                .add(
                        Attributes.ATTACK_KNOCKBACK, // 공격 밀치기
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "extra_knockback"),
                                knockback, // 추가할 밀치기 수치 (매우 강력함)
                                AttributeModifier.Operation.ADD_VALUE // 값 더하기
                        ),
                        EquipmentSlotGroup.MAINHAND
                )

                // [속성 B] 공격 데미지 제거 속성 (신규)
                .add(
                        Attributes.ATTACK_DAMAGE, // 공격 데미지
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "remove_damage"),
                                -0.99, // -100%
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL // 최종값에 곱하기
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();

        // 3. 아이템에 속성과 커스텀 이름 부여
        customSword.set(DataComponents.ATTRIBUTE_MODIFIERS, attributeModifiers);
        customSword.set(DataComponents.CUSTOM_NAME, Component.literal("밀쳐내기 막대"));
        customSword.set(DataComponents.LORE, new ItemLore(java.util.List.of(
                Component.literal("§7데미지 없이 적을 멀리 날려버립니다."))));

        // 4. 완성된 아이템을 플레이어에게 지급
        if (!player.getInventory().add(customSword)) {
            player.drop(customSword, false);
        }

        // 5. 피드백
        player.level().playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.7F, 1.0F);
        player.displayClientMessage(Component.literal("밀쳐내기 막대가 생성되었습니다!"), true);
    }
}