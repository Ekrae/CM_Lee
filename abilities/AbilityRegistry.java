package com.example.examplemod.abilities;
import com.example.examplemod.abilities.abilitySet.*; //능력 패키지들을 전부 가져오기.
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 모든 IAbility 구현체를 등록하고 관리하는 중앙 등록소
 */
public class AbilityRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();

    // <능력 ID, 능력 인스턴스>를 저장하는 맵
    private static final Map<ResourceLocation, IAbility> ABILITIES = new HashMap<>();

    // --- 우리가 만들 실제 능력들을 여기에 등록합니다 ---
    public static final IAbility
            FIREBALL, GAMBLER,TRACER ,BIND, PUSH,INVISIBILITY,SWAP,STEAL,DASH,KINGSLAYER;
    static {
        FIREBALL = register(new FireballAbility());
        GAMBLER = register(new GamblerAbility());
        TRACER = register(new TracerAbility());
        BIND = register(new BindAbility());
        PUSH = register(new PushAbility());
        INVISIBILITY = register(new InvisibilityAbility());
        SWAP = register(new SwapAbility());
        STEAL = register(new StealAbility());
        DASH = register(new DashAbility());
        KINGSLAYER = register(new KingslayerAbility());

    }
    //public static final IAbility HEAL = register(new HealAbility());
    // (새 능력을 만들 때마다 여기에 추가)


    /**
     * 능력을 맵에 등록하는 내부 헬퍼 메서드
     */
    private static IAbility register(IAbility ability) {
        if (ABILITIES.containsKey(ability.getId())) {
            LOGGER.warn("Ability ID {} is already registered! Overwriting.", ability.getId());
        }
        ABILITIES.put(ability.getId(), ability);
        LOGGER.info("Registered ability: {}", ability.getId());
        return ability;
    }

    /**
     * ID를 통해 등록된 능력을 가져옵니다.
     */
    public static IAbility get(ResourceLocation id) {
        return ABILITIES.get(id);
    }

    /**
     * 명령어 자동완성 등을 위해 모든 능력 ID 목록을 반환합니다.
     */
    public static Set<ResourceLocation> getAbilityIds() {
        return ABILITIES.keySet();
    }
}