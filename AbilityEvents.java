package com.example.examplemod;

import com.example.examplemod.abilities.AbilityRegistry;
import com.example.examplemod.abilities.IAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.example.examplemod.abilitySet.PlayerStateSnapshot; // 새로 만든 클래스 import
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 능력 발동 및 쿨타임 관리를 위한 이벤트 핸들러
 * [중요] 현재는 간단한 Map을 사용하지만, 최종본은 Capability로 교체해야 합니다.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AbilityEvents {

    // --- 임시 데이터 저장소 (나중에 Capability로 대체) ---
    // <플레이어 UUID, 현재 능력 ID>
    private static final Map<UUID, ResourceLocation> PLAYER_ABILITIES = new HashMap<>();
    // [수정!] <플레이어 UUID, 쿨타임이 "끝나는" 월드 틱 시간 (long)>
    private static final Map<UUID, Long> PLAYER_COOLDOWNS_END_TICK = new HashMap<>();
    // --------------------------------------------------

    // --- (기존 맵들...) ---

    // --- [새로운 데이터 저장소 추가] ---
    // <플레이어 UUID, 과거 상태 기록 Deque>
    private static final Map<UUID, Deque<PlayerStateSnapshot>> PLAYER_HISTORY = new HashMap<>();
    // 역행 시간 (초) * 20틱/초 = 저장할 틱 수
    private static final int HISTORY_DURATION_TICKS = 3 * 20;
    // ------------------------------------
    // --- 명령어 등에서 호출할 공용 메서드 ---

    public static PlayerStateSnapshot getOldestSnapshot(Player player) {
        Deque<PlayerStateSnapshot> history = PLAYER_HISTORY.get(player.getUUID());
        if (history == null || history.isEmpty()) {
            return null;
        }
        return history.getFirst(); // Deque의 맨 앞(가장 오래된) 기록 반환
    }
    public static void clearHistory(Player player) {
        Deque<PlayerStateSnapshot> history = PLAYER_HISTORY.get(player.getUUID());
        if (history != null) {
            history.clear();
        }
    }

    public static void setPlayerAbility(Player player, IAbility ability) {
        if (ability == null) {
            PLAYER_ABILITIES.remove(player.getUUID());
            if (player.getServer() != null) {
                player.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(player.getName().getString() + "의 능력이 제거되었습니다"), false);
            }
        } else {
            PLAYER_ABILITIES.put(player.getUUID(), ability.getId());
            if (player.getServer() != null) { //player 클래스에서 서버를 받아와서, null이 아니면 이걸 통해 broadcast를 날림
                player.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(player.getName().getString() + "가 " + ability.getId().getPath() + " 능력을 장착했습니다."),
                        false
                );
            }
            // 쿨타임 초기화
            PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
        }
    }


    public static IAbility getPlayerAbility(Player player) {
        ResourceLocation id = PLAYER_ABILITIES.get(player.getUUID());
        return (id != null) ? AbilityRegistry.get(id) : null;
    }


    // --- 이벤트 리스너 ---

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Pre event) {
        // 서버 측에서만 실행
        if (!event.player.level().isClientSide) {
            // --- [1. 기존 쿨타임 로직] ---
            // (이 부분은 그대로 둡니다)

            // --- [2. 새로운 과거 기록 로직] ---
            UUID uuid = event.player.getUUID();

            // 해당 플레이어의 기록 리스트를 가져오거나 새로 만듭니다.
            Deque<PlayerStateSnapshot> history = PLAYER_HISTORY.computeIfAbsent(uuid, k -> new ArrayDeque<>());

            // 현재 상태를 '사진' 찍습니다.
            PlayerStateSnapshot snapshot = new PlayerStateSnapshot(
                    event.player.position(),
                    event.player.getHealth(),
                    event.player.getYRot(),
                    event.player.getXRot()
            );

            // '사진'을 기록의 맨 뒤에 추가합니다.
            history.addLast(snapshot);

            // 기록이 너무 길어지면 맨 앞(가장 오래된) 기록을 삭제합니다.
            if (history.size() > HISTORY_DURATION_TICKS) {
                history.removeFirst();
            }
        }
    }

    /**
     * 아이템 우클릭 시 능력을 발동시킵니다.
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel(); // 월드 객체 가져오기

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        IAbility ability = getPlayerAbility(player);
        if (ability == null) {
            return;
        }

        if (stack.getItem() == ability.getTriggerItem()) {

            // --- [쿨타임 로직 전체 수정] ---

            // 1. 현재 월드 시간(틱)을 가져옵니다.
            long currentTime = level.getGameTime();

            // 2. 이 플레이어의 쿨타임이 끝나는 시간을 가져옵니다. (기본값 0)
            long cooldownEndTick = PLAYER_COOLDOWNS_END_TICK.getOrDefault(player.getUUID(), 0L);

            // 3. 쿨타임 확인: 현재 시간이 < 쿨타임 끝나는 시간이면, 아직 쿨타임 중
            if (currentTime < cooldownEndTick) {
                // 남은 틱 계산
                long remainingTicks = cooldownEndTick - currentTime;
                // 초 단위로 변환 (정확한 계산을 위해 20.0으로 나눔)
                double remainingSeconds = remainingTicks / 20.0;

                player.displayClientMessage(Component.literal(
                        "쿨타임: " + String.format("%.1f", remainingSeconds) + "초 남음"
                ), true);
                return;
            }

            // [발동 성공]
            // 4. 능력 실행
            ability.execute(serverPlayer);

            // 5. 쿨타임 설정: "현재 시간 + (능력 쿨타임(초) * 20)"
            int cooldownInSeconds = ability.getCooldownSeconds();
            long newCooldownEndTick = currentTime + (cooldownInSeconds * 20L); // 20L (long 타입)

            PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldownEndTick);

            // --- [여기까지 수정] ---
        }
    }
}