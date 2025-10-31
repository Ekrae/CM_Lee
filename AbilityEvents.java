package com.example.examplemod;

// 1. Import 정리: TracerAbility/StealAbility의 핸들러를 호출하기 위해 import
import com.example.examplemod.abilities.AbilityRegistry;
import com.example.examplemod.abilities.abilitySet.IAbility;
import com.example.examplemod.abilities.abilitySet.StealAbility; // <-- StealHandler 호출용
import com.example.examplemod.abilities.abilitySet.TracerAbility; // <-- Tracer 핸들러 호출용
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
// import net.minecraftforge.event.entity.player.AttackEntityEvent; // <-- [삭제]
import net.minecraftforge.eventbus.api.listener.SubscribeEvent; // <-- 'listener' 포함
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 전역 능력 이벤트 핸들러 (우클릭 발동, 해킹)
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AbilityEvents {

    /** --- 전역 능력 관리 맵 ---*/
    private static final Map<UUID, ResourceLocation> PLAYER_ABILITIES = new HashMap<>();

    // [수정] public static으로 변경하여 다른 핸들러에서 접근 가능하도록 함
    public static final Map<UUID, Long> PLAYER_COOLDOWNS_END_TICK = new HashMap<>();

    /**
     * --- Hack (해킹) 능력 전용 맵 ---
     */
    private static final Map<UUID, Long> HACKED_PLAYERS_END_TICK = new HashMap<>();


    // --- [삭제] Steal 헬퍼 메서드 (StealHandler로 이동) ---

    // --- Hack 헬퍼 메서드 ---
    public static void scheduleHackRevert(UUID playerUUID, long endTime) {
        HACKED_PLAYERS_END_TICK.put(playerUUID, endTime);
    }

    // --- 전역 헬퍼 메서드 ---
    public static IAbility getPlayerAbility(Player player) {
        ResourceLocation id = PLAYER_ABILITIES.get(player.getUUID());
        return (id != null) ? AbilityRegistry.get(id) : null;
    }

    /**
     * [수정됨] 플레이어의 능력을 설정하고, 모든 상태를 초기화합니다.
     */
    public static void setPlayerAbility(Player player, IAbility ability) {
        if (ability == null) {
            PLAYER_ABILITIES.remove(player.getUUID());
            if (player.getServer() != null) {
                player.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(player.getName().getString() + "의 능력이 제거되었습니다"), false);
            }
        } else {
            PLAYER_ABILITIES.put(player.getUUID(), ability.getId());
            if (player.getServer() != null) {
                player.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(player.getName().getString() + "가 " + ability.getId().getPath() + " 능력을 장착했습니다."),
                        false
                );
            }
        }

        PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
        HACKED_PLAYERS_END_TICK.remove(player.getUUID());

        // [수정] StealAbility의 중첩 클래스(StealHandler)를 호출하여 초기화
        StealAbility.StealHandler.clearStolenAbility(player);

        // [수정] TracerAbility의 중첩 클래스(RecallHandler)를 호출하여 초기화
        TracerAbility.RecallHandler.clearHistory(player);
    }
    /**
     * [수정됨] Hack (해킹) 능력의 복구 로직만 담당합니다.
     */
    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Pre event) {
        if (event.player.level().isClientSide || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        UUID uuid = serverPlayer.getUUID();
        Level level = serverPlayer.level();
        long currentTime = level.getGameTime();

        // --- [Hack 로직만 남김] ---
        Long hackEndTime = HACKED_PLAYERS_END_TICK.get(uuid);
        if (hackEndTime != null && currentTime >= hackEndTime) {
            serverPlayer.removeTag("hacked_pol");
            serverPlayer.addTag("pol");
            HACKED_PLAYERS_END_TICK.remove(uuid); // 맵에서 제거
            serverPlayer.displayClientMessage(Component.literal("시스템이 복구되었습니다."), true);
        }
        // --- [Tracer 로직은 모두 삭제됨] ---
    }

    /**
     * [수정됨] 모든 능력의 공통 발동 이벤트
     */
    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 1. [수정] 훔친 능력 사용 (StealHandler 호출)
        IAbility stolenAbility = StealAbility.StealHandler.getStolenAbility(serverPlayer);
        if (stolenAbility != null) {
            // [방어 코드 추가] 훔친 능력의 트리거 아이템이 null인지 확인
            if (stolenAbility.getTriggerItem() != null && stack.getItem() == stolenAbility.getTriggerItem()) {
                stolenAbility.execute(serverPlayer);
                StealAbility.StealHandler.clearStolenAbility(serverPlayer);
                stack.shrink(1);
                serverPlayer.displayClientMessage(Component.literal("훔친 능력을 사용했습니다!"), true);
                return;
            }
        }

        // 2. 기본 능력 사용
        IAbility ability = getPlayerAbility(player);
        if (ability == null) {
            return;
        }

        // [방어 코드 추가] 기본 능력의 트리거 아이템이 null인지 확인
        if (ability.getTriggerItem() != null && stack.getItem() == ability.getTriggerItem()) {

            // --- [핵심 수정: 사슬 우클릭 쿨타임 문제 해결] ---
            if (ability.getId() == AbilityRegistry.STEAL.getId()) {
                ability.execute(serverPlayer); // 메시지만 표시
                return; // 쿨타임 로직을 실행하지 않고 즉시 종료
            }
            // --- [여기까지 수정] ---

            // (이하는 STEAL 능력이 아닐 때만 실행됨)
            long currentTime = level.getGameTime();
            long cooldownEndTick = PLAYER_COOLDOWNS_END_TICK.getOrDefault(player.getUUID(), 0L);

            if (currentTime < cooldownEndTick) {
                long remainingTicks = cooldownEndTick - currentTime;
                double remainingSeconds = remainingTicks / 20.0;
                player.displayClientMessage(Component.literal(
                        "쿨타임: " + String.format("%.1f", remainingSeconds) + "초 남음"
                ), true);
                return;
            }

            ability.execute(serverPlayer);

            int cooldownInSeconds = ability.getCooldownSeconds();
            long newCooldownEndTick = currentTime + (cooldownInSeconds * 20L);
            PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldownEndTick);
        }
    }
}

