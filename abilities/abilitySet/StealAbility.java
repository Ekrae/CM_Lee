package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.AbilityEvents; // AbilityEvents의 공용 메서드를 호출하기 위해 필요
import com.example.examplemod.abilities.AbilityRegistry;
import com.example.examplemod.ExampleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent; // 'listener' 포함
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class StealAbility implements IAbility {

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "steal");
    }

    @Override
    public Item getTriggerItem() {
        return Items.CHAIN;
    }

    @Override
    public int getCooldownSeconds() {
        return 30; // 훔치기 자체의 쿨타임
    }

    @Override
    public void execute(ServerPlayer player) {
        // 사슬을 그냥 우클릭(사용)했을 때 (쿨타임 X, onRightClickItem에서 처리)
        player.displayClientMessage(Component.literal("사슬을 들고 다른 플레이어를 때려서 능력을 훔치세요!"), true);
    }

    /**
     * [추가] '훔치기' 능력의 배경 로직(데이터, 훔치기 감지)을 모두 담는 중첩 클래스
     */
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class StealHandler {

        // --- 1. Steal (훔치기) 능력 전용 맵 ---
        private static final Map<UUID, IAbility> STOLEN_ABILITY = new HashMap<>();

        // --- 2. Steal 헬퍼 메서드(get,set,초기화) ---
        public static IAbility getStolenAbility(Player player) {
            return STOLEN_ABILITY.get(player.getUUID());
        }
        public static void setStolenAbility(Player player, IAbility ability) {
            STOLEN_ABILITY.put(player.getUUID(), ability);
        }
        public static void clearStolenAbility(Player player) {
            STOLEN_ABILITY.remove(player.getUUID());
        }

        // --- 3. 훔치기 이벤트 리스너 (AbilityEvents에서 이동) ---
        @SuppressWarnings("resource")
        @SubscribeEvent
        public static void onPlayerHit(AttackEntityEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer attacker) || !(event.getTarget() instanceof ServerPlayer target)) {
                return;

            } //선언 하면서 변수도 선언하나봄

            Level level = attacker.level();
            if (level.isClientSide){

                return;
            }

            // [수정] AbilityEvents의 공용 헬퍼 메서드를 호출
            IAbility attackerAbility = AbilityEvents.getPlayerAbility(attacker);

            // [중요] 훔치기 능력이 없으면 당연히 리턴
            if (attackerAbility == null || !attackerAbility.getId().equals(AbilityRegistry.STEAL.getId())) {
                return;
            }
            // [중요] 훔치기 능력의 트리거(사슬)를 안 들고 있으면 리턴
            if (!attacker.getMainHandItem().is(Items.CHAIN)) {
                return;
            }

            // [수정] AbilityEvents의 공용 쿨타임 맵을 참조
            long currentTime = level.getGameTime();
            long cooldownEndTick = AbilityEvents.PLAYER_COOLDOWNS_END_TICK.getOrDefault(attacker.getUUID(), 0L);

            if (currentTime < cooldownEndTick) {
                double remainingSeconds = (cooldownEndTick - currentTime) / 20.0;
                attacker.displayClientMessage(Component.literal(
                        "훔치기 쿨타임: " + String.format("%.1f", remainingSeconds) + "초 남음"
                ), true);
                //attacker.displayClientMessage(Component.literal("5번문제"),true);
                return;
            }
            attacker.displayClientMessage(Component.literal("여기 실행이 되나?"), true);

            // [수정] AbilityEvents의 공용 헬퍼 메서드를 호출
            IAbility targetAbility = AbilityEvents.getPlayerAbility(target);

            // Null 방지 코드 (유지)
            if (targetAbility == null) {
                attacker.displayClientMessage(Component.literal(target.getName().getString() + "님은 능력이 없습니다."), true);
                return;
            }
            if (targetAbility.getTriggerItem() == null) {
                // "Item id not set" 크래시를 방지
                attacker.displayClientMessage(Component.literal(target.getName().getString() + "님의 능력은 훔칠 수 없는 아이템입니다."), true);
                return;
            }

            // [수정] 이 클래스(StealHandler)의 헬퍼 메서드를 호출
            if (getStolenAbility(attacker) != null) {
                attacker.displayClientMessage(Component.literal("이미 훔친 능력을 가지고 있습니다! 먼저 사용하세요."), true);
                return;
            }

            // [훔치기 성공]
            // [수정] 이 클래스(StealHandler)의 헬퍼 메서드를 호출
            setStolenAbility(attacker, targetAbility);

            ItemStack triggerItem = new ItemStack(targetAbility.getTriggerItem(), 1);
            if (!attacker.getInventory().add(triggerItem)) {
                attacker.drop(triggerItem, false);
            }

            // [수정] AbilityEvents의 공용 쿨타임 맵에 쿨타임 적용
            long newCooldownEndTick = currentTime + (attackerAbility.getCooldownSeconds() * 20L);
            AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(attacker.getUUID(), newCooldownEndTick);

            attacker.displayClientMessage(Component.literal(targetAbility.getId().getPath() + " 능력을 훔쳤습니다! (1회용)"), true);
            target.displayClientMessage(Component.literal(attacker.getName().getString() + "에게 능력을 복사당했습니다!"), true);
            level.playSound(null, attacker.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}

