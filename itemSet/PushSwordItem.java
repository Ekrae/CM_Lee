package com.example.examplemod.itemSet; // (사용자님의 패키지 경로)

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


public class PushSwordItem extends Item {

    public PushSwordItem(Properties pProperties) {
        super(pProperties);
    }

    // [삭제!] getDefaultAttributeModifiers 메서드 전체를 삭제합니다.

    /**
     * [수정] 이 아이템으로 적을 때렸을 때 발동됩니다.
     * (내용은 같지만, 올바른 최신 시그니처로 다시 작성)
     */
    @Override
    public void hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        if (!pAttacker.level().isClientSide()) {
            float knockbackStrength = 2.5F;

            pTarget.knockback(
                    knockbackStrength,
                    pAttacker.getX() - pTarget.getX(),
                    pAttacker.getZ() - pTarget.getZ()
            );

            pAttacker.level().playSound(null, pAttacker.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 1.0F);

            pStack.shrink(1);
        }


    }

}