package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.Config;
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
import net.minecraftforge.event.entity.living.LivingDeathEvent; // [추가] 사망 이벤트
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
        return Items.CLOCK;
    }

    @Override
    public int getCooldownSeconds() {
        return Config.tracer_cooldown;
    }

    @Override
    public Component getDescription() {
        return Component.literal("3초 전의 위치, 체력, 시야각으로 되돌아갑니다.");
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

        // --- [신규] 플레이어 사망 시 기록 초기화 ---
        @SubscribeEvent
        public static void onPlayerDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                // 죽으면 즉시 기록 삭제 (부활 후 죽은 위치로 역행하는 것 방지)
                clearHistory(player);
            }
        }
        // ---------------------------------------

        @SuppressWarnings("resource")
        @SubscribeEvent
        public static void onTick(TickEvent.PlayerTickEvent.Pre event) {
            if (event.player.level().isClientSide || !(event.player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            // [추가 안전장치] 죽어있는 상태라면 기록하지 않음
            if (serverPlayer.isDeadOrDying()) {
                return;
            }

            UUID uuid = serverPlayer.getUUID();

            if (REWINDING_PLAYERS.contains(uuid)) {
                Deque<PlayerStateSnapshot> history = PLAYER_HISTORY.get(uuid);
                PlayerStateSnapshot targetFrame = null;

                // 3배속 재생 루프
                for (int i = 0; i < 3; i++) {
                    if (history == null || history.isEmpty()) {
                        break;
                    }
                    targetFrame = history.removeLast();
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
                        serverPlayer.removeEffect(MobEffects.INVISIBILITY);
                    }
                } else {
                    REWINDING_PLAYERS.remove(uuid);
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
            if (REWINDING_PLAYERS.contains(player.getUUID()) || !PLAYER_HISTORY.containsKey(player.getUUID()) || PLAYER_HISTORY.get(player.getUUID()).isEmpty()) {
                return;
            }

            REWINDING_PLAYERS.add(player.getUUID());

            Level level = player.level();
            Vec3 currentPos = player.position();

            Holder<MobEffect> nausea = MobEffects.NAUSEA;
            Holder<MobEffect> blindness = MobEffects.BLINDNESS;
            Holder<MobEffect> invisibility = MobEffects.INVISIBILITY;

            player.addEffect(new MobEffectInstance(nausea, 40, 0, false, false));
            player.addEffect(new MobEffectInstance(blindness, 10, 0, false, false));
            player.addEffect(new MobEffectInstance(invisibility, 60, 0, false, false));

            level.playSound(null, currentPos.x, currentPos.y, currentPos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

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