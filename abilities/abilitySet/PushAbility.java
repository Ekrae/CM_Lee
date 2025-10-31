package com.example.examplemod.abilities.abilitySet; // (능력 패키지 경로)

import com.example.examplemod.ExampleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PushAbility implements IAbility {

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "push");
    }

    @Override
    public Item getTriggerItem() {
        // 트리거 아이템: 피스톤
        return Items.PISTON;
    }

    @Override
    public int getCooldownSeconds() {
        return 10; // 아이템을 하나 '생성'하는 쿨타임
    }

    @Override
    @SuppressWarnings("resource")
    public void execute(ServerPlayer player) {

        // 1. 우리가 등록한 커스텀 아이템의 인스턴스를 만듭니다.
        //ItemStack pushSwordStack = new ItemStack(ExampleMod.PUSH_SWORD.get());

        // (선택) 아이템에 특별한 이름을 부여할 수도 있습니다.
        // pushSwordStack.set(DataComponents.CUSTOM_NAME, Component.literal("밀쳐내기 칼날"));

        // 2. 플레이어의 인벤토리에 아이템을 추가합니다.
        // .add()는 빈 슬롯을 찾아 넣어주고, 꽉 찼으면 바닥에 드롭합니다.
//        if (!player.getInventory().add(pushSwordStack)) {
//            player.drop(pushSwordStack, false);
//        }
//
//        // 3. 피드백
//        player.level().playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.7F, 1.0F);
//        player.displayClientMessage(Component.literal("밀쳐내기 칼날이 생성되었습니다!"), true);
    }
}