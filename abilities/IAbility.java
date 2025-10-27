package com.example.examplemod.abilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/**
 * 모든 '능력'이 구현해야 하는 인터페이스 (설계도)
 */
public interface IAbility {

    int tickToSecond=20;
    /**
     * 능력의 고유 ID (예: "examplemod:fireball")
     * 이 ID는 능력 등록 및 명령어에 사용됩니다.
     */
    ResourceLocation getId();

    /**
     * 능력을 발동시키는 '트리거' 아이템을 반환합니다.
     */
    Item getTriggerItem();

    /**
     * 이 능력의 쿨타임 (단위: 틱, 20틱 = 1초)
     */
    int getCooldownSeconds();

    /**
     * 능력의 실제 효과를 실행합니다.
     * @param player 능력을 사용한 플레이어
     */
    void execute(ServerPlayer player);

    // (선택적) 능력에 대한 설명을 반환하는 메서드 등을 추가할 수 있습니다.
    //String getDescription();
}