package com.example.examplemod.abilities.abilitySet;

// 1. 필요한 Import 문 정리
import com.example.examplemod.ExampleMod;
import com.example.examplemod.abilityDataObject.PlayerStateSnapshot;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TracerAbility implements IAbility {
    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "tracer");
    }

    @Override
    public Item getTriggerItem() {
        return Items.CLOCK; // 트리거 아이템: 시계
    }

    @Override
    public int getCooldownSeconds() {
        return 12;
    }

    @Override
    public void execute(ServerPlayer player) {
        PlayerStateSnapshot targetState = RecallHandler.getOldestSnapshot(player);

        if (targetState == null) {
            player.displayClientMessage(Component.literal("되돌아갈 기록이 없습니다!"), true);
            return;
        }

        RecallHandler.startRewind(player);
    }

    /**
     * '역행' 능력의 배경 로직(데이터, 틱 처리)을 담는 중첩 클래스
     */
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class RecallHandler {

        private static final Map<UUID, Deque<PlayerStateSnapshot>> PLAYER_HISTORY = new HashMap<>();
        private static final Set<UUID> REWINDING_PLAYERS = new HashSet<>();
        private static final int HISTORY_DURATION_TICKS = 3 * 20;

        @SuppressWarnings("resource")
        @SubscribeEvent
        public static void onTick(TickEvent.PlayerTickEvent.Pre event) {
            if (event.player.level().isClientSide || !(event.player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            UUID uuid = serverPlayer.getUUID();

            // [수정] 불필요한 history 변수 선언 제거

            if (REWINDING_PLAYERS.contains(uuid)) {
                Deque<PlayerStateSnapshot> history = PLAYER_HISTORY.get(uuid); // 여기서 한 번만 가져옴
                PlayerStateSnapshot targetFrame = null;

                // 3배속 재생 루프
                for (int i = 0; i < 3; i++) {
                    if (history == null || history.isEmpty()) {
                        break;
                    }
                    targetFrame = history.removeLast();

                    // [정리] 주석 처리된 파티클 코드 삭제
                }

                if (targetFrame != null) {
                    serverPlayer.teleportTo(
                            targetFrame.position().x,
                            targetFrame.position().y,
                            targetFrame.position().z
                    );
                    serverPlayer.setYRot(targetFrame.yRot());
                    serverPlayer.setXRot(targetFrame.xRot());

                    if (history.isEmpty()) {
                        serverPlayer.setHealth(targetFrame.health());
                        serverPlayer.level().playSound(null, targetFrame.position().x, targetFrame.position().y, targetFrame.position().z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
                        REWINDING_PLAYERS.remove(uuid);

                        // [수정] Holder 타입을 사용하도록 .get() 추가
                        serverPlayer.removeEffect(MobEffects.INVISIBILITY);
                    } else {
                        // [수정] 역행 중간 소리가 너무 시끄러울 수 있어 주석 처리 (필요하면 해제)
                        // serverPlayer.level().playSound(null, targetFrame.position().x, targetFrame.position().y, targetFrame.position().z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.3F, 2.0F);
                    }
                } else {
                    REWINDING_PLAYERS.remove(uuid);
                    // [수정] Holder 타입을 사용하도록 .get() 추가
                    serverPlayer.removeEffect(MobEffects.INVISIBILITY);
                }
            } else {
                // 역행 중이 아닐 때 (과거 기록 저장)
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

        @SuppressWarnings("resource")
        public static void startRewind(ServerPlayer player) {
            // [수정] 추가하기 전에 '먼저' 확인해야 함
            if (REWINDING_PLAYERS.contains(player.getUUID()) || !PLAYER_HISTORY.containsKey(player.getUUID()) || PLAYER_HISTORY.get(player.getUUID()).isEmpty()) {
                return; // 이미 역행 중이거나 기록이 없으면 중단
            }

            // [수정] 로직 순서 변경: 확인 후 상태 변경
            REWINDING_PLAYERS.add(player.getUUID());

            Level level = player.level();
            Vec3 currentPos = player.position();

            // [수정] Holder 타입을 사용하도록 .get() 추가
            Holder<MobEffect> nausea = MobEffects.NAUSEA;
            Holder<MobEffect> blindness = MobEffects.BLINDNESS;
            Holder<MobEffect> invisibility = MobEffects.INVISIBILITY;

            player.addEffect(new MobEffectInstance(nausea, 40, 0, false, false));
            player.addEffect(new MobEffectInstance(blindness, 10, 0, false, false));
            player.addEffect(new MobEffectInstance(invisibility, 60, 0, false, false));

            level.playSound(null, currentPos.x, currentPos.y, currentPos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

            // 첫 프레임 파티클 (정상)
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        30, 0.4, 0.5, 0.4, 0.1
                );
            }
        }

        public static PlayerStateSnapshot getOldestSnapshot(Player player) {
            Deque<PlayerStateSnapshot> history = PLAYER_HISTORY.get(player.getUUID());
            if (history == null || history.isEmpty()) {
                return null;
            }
            return history.getFirst();
        }

        public static void clearHistory(Player player) {
            REWINDING_PLAYERS.remove(player.getUUID());
            PLAYER_HISTORY.remove(player.getUUID());
        }
    }
}