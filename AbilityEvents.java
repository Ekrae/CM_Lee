package com.example.examplemod;

import com.example.examplemod.abilities.AbilityRegistry;
import com.example.examplemod.abilities.abilitySet.IAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.example.examplemod.abilitySet.PlayerStateSnapshot; // 새로 만든 클래스 import
import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

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
    // --- [새로운 데이터 저장소: 훔친 능력] ---
    // <플레이어 UUID, 1회용 훔친 능력>
    private static final Map<UUID, IAbility> STOLEN_ABILITY = new HashMap<>();
    // --- (기존 맵들...) ---

    // --- [새로운 데이터 저장소 추가] ---
    // <플레이어 UUID, 과거 상태 기록 Deque>
    private static final Map<UUID, Deque<PlayerStateSnapshot>> PLAYER_HISTORY = new HashMap<>();
    // 역행 시간 (초) * 20틱/초 = 저장할 틱 수
    private static final int HISTORY_DURATION_TICKS = 3 * 20;
    // ------------------------------------
    private static final Set<UUID> REWINDING_PLAYERS = new HashSet<>();


    // --- [새로운 헬퍼 메서드 3개] ---
    public static IAbility getStolenAbility(Player player) {
        return STOLEN_ABILITY.get(player.getUUID());
    }

    public static void setStolenAbility(Player player, IAbility ability) {
        STOLEN_ABILITY.put(player.getUUID(), ability);
        // (훔친 능력을 시각적으로 표시하는 로직을 나중에 추가할 수 있습니다)
    }

    public static void clearStolenAbility(Player player) {
        STOLEN_ABILITY.remove(player.getUUID());
    }

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

    // --- 명령어 등에서 호출할 공용 메서드 ---
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
            // 능력이 변경되면, 진행 중이던 모든 상태를 강제로 초기화합니다.
            REWINDING_PLAYERS.remove(player.getUUID());
            clearHistory(player);
        }
        // --- [추가] 능력이 변경되면, 훔쳤던 능력도 강제로 제거합니다.
        clearStolenAbility(player);
    }

    // --- [새로운 이벤트: 플레이어 공격 시 (훔치기 로직)] ---
    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onPlayerHit(AttackEntityEvent event) {
        // 공격자가 플레이어이고, 대상도 플레이어일 때만
        if (!(event.getEntity() instanceof ServerPlayer attacker) || !(event.getTarget() instanceof ServerPlayer target)) {
            return;
        }

        Level level = attacker.level();
        if (level.isClientSide) return;

        // 1. 공격자의 현재 능력이 "STEAL"인지 확인
        IAbility attackerAbility = getPlayerAbility(attacker);
        if (attackerAbility == null || attackerAbility.getId() != AbilityRegistry.STEAL.getId()) {
            return;
        }

        // 2. 공격자가 "사슬(STEAL 능력의 트리거)"을 들고 있는지 확인
        if (!attacker.getMainHandItem().is(Items.CHAIN)) {
            return;
        }

        // 3. 훔치기 능력 자체의 쿨타임 확인
        long currentTime = level.getGameTime();
        long cooldownEndTick = PLAYER_COOLDOWNS_END_TICK.getOrDefault(attacker.getUUID(), 0L);

        if (currentTime < cooldownEndTick) {
            double remainingSeconds = (cooldownEndTick - currentTime) / 20.0;
            attacker.displayClientMessage(Component.literal(
                    "훔치기 쿨타임: " + String.format("%.1f", remainingSeconds) + "초 남음"
            ), true);
            return;
        }

        // 4. 대상이 능력이 있는지 확인
        IAbility targetAbility = getPlayerAbility(target);
        if (targetAbility == null) {
            attacker.displayClientMessage(Component.literal(target.getName().getString() + "님은 능력이 없습니다."), true);
            return;
        }

        // 5. 이미 훔친 능력을 갖고 있는지 확인
        if (getStolenAbility(attacker) != null) {
            attacker.displayClientMessage(Component.literal("이미 훔친 능력을 가지고 있습니다! 먼저 사용하세요."), true);
            return;
        }

        // 6. [훔치기 성공]
        setStolenAbility(attacker, targetAbility);
        // --- [새 코드 추가: 훔친 능력의 트리거 아이템 1개 지급] ---
        ItemStack triggerItem = new ItemStack(targetAbility.getTriggerItem(), 1);
        // 인벤토리에 추가, 만약 꽉 찼다면 바닥에 드롭
        if (!attacker.getInventory().add(triggerItem)) {
            attacker.drop(triggerItem, false);
        }
        // --- [여기까지 추가] ---
        // 7. 훔치기 능력(STEAL)에 쿨타임 적용
        long newCooldownEndTick = currentTime + (attackerAbility.getCooldownSeconds() * 20L);
        PLAYER_COOLDOWNS_END_TICK.put(attacker.getUUID(), newCooldownEndTick);

        // 8. 피드백
        attacker.displayClientMessage(Component.literal(targetAbility.getId().getPath() + " 능력을 훔쳤습니다! (1회용)"), true);
        target.displayClientMessage(Component.literal(attacker.getName().getString() + "에게 능력을 복사당했습니다!"), true);
        level.playSound(null, attacker.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static IAbility getPlayerAbility(Player player) {
        ResourceLocation id = PLAYER_ABILITIES.get(player.getUUID());
        return (id != null) ? AbilityRegistry.get(id) : null;
    }
    // --- [새로운 '역행 시작' 메서드 추가] ---
    public static void startRewind(ServerPlayer player) {
        if (REWINDING_PLAYERS.contains(player.getUUID()) || !PLAYER_HISTORY.containsKey(player.getUUID())) {
            return;
        }

        Level level = player.level();
        Vec3 currentPos = player.position();

        // 1. 역행 시작 신호
        REWINDING_PLAYERS.add(player.getUUID());

        // 2. 시작 시 화면 및 사운드 효과
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 40, 0, false, false)); // 2초 멀미
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 10, 0, false, false)); // 0.5초 실명
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
        // --- [새로운 기능 1: 투명화 적용] ---
        // 역행이 끝날 때까지 (넉넉하게 3초) 투명화 적용 (파티클 없음)
        // ------------------------------------
        level.playSound(null, currentPos.x, currentPos.y, currentPos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.END_ROD,        // 파티클 종류 (엔드 막대)
                    player.getX(),                // X 좌표
                    player.getY() + 1.0,          // Y 좌표 (몸 중앙)
                    player.getZ(),                // Z 좌표
                    30,                           // 파티클 개수 (조금 많게)
                    0.4,                          // X축 분산 범위
                    0.5,                          // Y축 분산 범위
                    0.4,                          // Z축 분산 범위
                    0.1                           // 파티클 속도
            );
        }
    }
    // -------------------------------------------

    // --- 이벤트 리스너 ---
    /**
     * [수정!] onPlayerTick 이벤트가 이제 '역행 애니메이션'도 처리합니다.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Pre event) {
        // [수정] ServerPlayer로 바로 형변환하여 사용
        if (event.player.level().isClientSide || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID uuid = serverPlayer.getUUID();

        // --- [수정!] 역행 중일때
        if (REWINDING_PLAYERS.contains(uuid)) {
            // --- 1. 역행 애니메이션 로직 (역행 중일 때) ---
            Deque<PlayerStateSnapshot> history = PLAYER_HISTORY.get(uuid);
            PlayerStateSnapshot targetFrame = null; // 이번 틱에 텔레포트할 최종 프레임

            for (int i = 0; i < 3; i++) {
                if (history == null || history.isEmpty()) {
                    break; // 처리할 프레임이 없으면 중단
                }

                // 가장 '최근' 기록부터 하나씩 꺼냅니다.
                targetFrame = history.removeLast();

                // --- [새로운 기능 3: 파티클 남기기] ---
                // 되감기가 지나가는 각 프레임 위치에 엔드 막대 파티클을 1개씩 남깁니다.
//                serverPlayer.level().sendParticles(
//                        ParticleTypes.END_ROD, // 파티클 종류
//                        targetFrame.position().x,
//                        targetFrame.position().y + 1.0, // 플레이어 몸 중앙
//                        targetFrame.position().z,
//                        1, 0, 0, 0, 0 // 개수, 퍼짐(x,y,z), 속도
//                );
            }
            // ------------------------------------------

            // 3프레임 처리 후, 마지막으로 꺼낸 프레임(targetFrame)이 있다면 그 위치로 이동
            if (targetFrame != null) {
                serverPlayer.teleportTo(
                        targetFrame.position().x,
                        targetFrame.position().y,
                        targetFrame.position().z
                );
                serverPlayer.setYRot(targetFrame.yRot());
                serverPlayer.setXRot(targetFrame.xRot());

                // 만약 history가 '방금' 비었다면 (역행 종료)
                if (history.isEmpty()) {
                    serverPlayer.setHealth(targetFrame.health()); // 체력 복원

                    serverPlayer.level().playSound(null, targetFrame.position().x, targetFrame.position().y, targetFrame.position().z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);

                    REWINDING_PLAYERS.remove(uuid); // 역행 상태 해제
                    // --- [새로운 기능 1: 투명화 해제] ---
                    serverPlayer.removeEffect(MobEffects.INVISIBILITY);
                    // ------------------------------------
                } else {
                    // 역행이 진행 중인 소리
                    serverPlayer.level().playSound(null, targetFrame.position().x, targetFrame.position().y, targetFrame.position().z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.3F, 2.0F);
                }
            } else {
                // (오류 방지) 처리할 프레임이 없으면 즉시 종료
                REWINDING_PLAYERS.remove(uuid);
                serverPlayer.removeEffect(MobEffects.INVISIBILITY);
            }
        } else {
            // --- 2. 과거 기록 로직 (역행 중이 아닐 때) ---
            // (이 로직은 이제 역행 중이 아닐 때만 실행됩니다)
            Deque<PlayerStateSnapshot> history = PLAYER_HISTORY.computeIfAbsent(uuid, k -> new ArrayDeque<>());

            PlayerStateSnapshot snapshot = new PlayerStateSnapshot(
                    serverPlayer.position(),
                    serverPlayer.getHealth(),
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot()
            );

            history.addLast(snapshot);

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

        IAbility stolenAbility = getStolenAbility(serverPlayer); //stolen이 null이면 기본 능력들임.
        if (stolenAbility != null) {
            // 1-1. 훔친 능력의 트리거 아이템이 맞는지 확인
            if (stack.getItem() == stolenAbility.getTriggerItem()) {

                // 1-2. [실행] 쿨타임 없이 즉시 실행
                stolenAbility.execute(serverPlayer);

                // 1-3. [제거] 1회용이므로 사용 후 즉시 제거
                clearStolenAbility(serverPlayer);
                stack.shrink(1);
                serverPlayer.displayClientMessage(Component.literal("훔친 능력을 사용했습니다!"), true);

                // 1-4. [중요] 뒤의 '기본 능력' 로직을 실행하지 않고 여기서 종료
                return;
            }
        }

        // --- [2. 기본 능력 사용 (훔친 능력이 없을 때)] ---
        IAbility ability = getPlayerAbility(player);
        if (ability == null) {
            return;
        }

        // 2-1. 기본 능력의 트리거 아이템이 맞는지 확인
        if (stack.getItem() == ability.getTriggerItem()) {

            // 2-2. 기본 능력 쿨타임 확인 (기존 로직)
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

            // 2-3. [실행]
            ability.execute(serverPlayer);

            // 2-4. [쿨타임 적용]
            int cooldownInSeconds = ability.getCooldownSeconds();
            long newCooldownEndTick = currentTime + (cooldownInSeconds * 20L);
            PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldownEndTick);
        }
    }
}