package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HackAbility implements IAbility {

    // --- 설정 ---
    private static final String HACK_TAG = "pol"; // 대상 태그
    private static final int HACK_DURATION_TICKS = 100; // 5초 (1초 = 20틱)
    // ---------------

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "hack");
    }

    @Override
    public Item getTriggerItem() {
        // 능력 아이템: 레드스톤 토치 (테마에 맞게)
        return Items.REDSTONE_TORCH;
    }

    @Override
    public int getCooldownSeconds() {
        return 20; // 쿨타임 20초
    }

    @Override
    public void execute(ServerPlayer player) {
        // 1. 범위 설정: 플레이어 중심 5x3x5 (가로x세로x깊이)
        // AABB(player.blockPosition())는 플레이어 발이 있는 1x1x1 블록
        // .inflate(2.0, 1.0, 2.0)는 XZ축으로 2블록씩, Y축으로 1블록씩 확장
        // -> (1+2+2) x (1+1+1) x (1+2+2) = 5x3x5 영역
        AABB searchArea = new AABB(player.blockPosition()).inflate(2.0, 1.0, 2.0);

        // 2. 범위 내에서 'pol' 태그를 가진 다른 플레이어 찾기
        List<ServerPlayer> targets = player.level().getEntitiesOfClass(
                ServerPlayer.class,
                searchArea,
                targetPlayer -> targetPlayer != player && targetPlayer.getTags().contains(HACK_TAG)
        );

        if (targets.isEmpty()) {
            player.displayClientMessage(Component.literal("주변에 '" + HACK_TAG + "' 태그를 가진 플레이어가 없습니다."), true);
            return;
        }

        // 3. 대상들의 태그 제거 및 복구 예약
        for (ServerPlayer target : targets) {
            target.removeTag(HACK_TAG); // 태그 즉시 제거
            HackHandler.scheduleTagReapply(target.getUUID(), HACK_DURATION_TICKS); // 복구 예약

            // 대상에게 피드백
            target.displayClientMessage(Component.literal("보안 시스템이 일시적으로 해킹당했습니다!"), true);
            target.level().playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 0.8F);
        }

        // 4. 시전자에게 피드백
        player.displayClientMessage(Component.literal(targets.size() + "명의 '" + HACK_TAG + "' 태그를 비활성화했습니다."), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, 1.5F);
    }


    /**
     * 해킹된 플레이어의 태그 복구를 처리하는 static 내부 핸들러 클래스.
     * @Mod.EventBusSubscriber 어노테이션이 Forge 이벤트 버스에 자동으로 등록해줍니다.
     */
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class HackHandler {

        // <플레이어 UUID, 남은 틱>
        // ConcurrentHashMap: 여러 스레드(게임 로직, 틱 이벤트)에서 동시에 접근해도 안전함
        private static final Map<UUID, Integer> hackedPlayers = new ConcurrentHashMap<>();

        /**
         * 플레이어의 태그 복구를 예약합니다.
         */
        public static void scheduleTagReapply(UUID playerUuid, int durationTicks) {
            hackedPlayers.put(playerUuid, durationTicks);
        }

        /**
         * 매 서버 틱마다 호출되어 타이머를 감소시키고 태그를 복구합니다.
         */
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            // [수정] 1.21.8+ 에서는 phase 구분이 없습니다.
            // 처리할 플레이어가 없으면 즉시 종료
            if (hackedPlayers.isEmpty()) {
                return;
            }

            MinecraftServer server = event.getServer();

            // 맵의 키(UUID)를 순회하며 처리
            for (UUID uuid : new java.util.HashSet<>(hackedPlayers.keySet())) {

                Integer ticksRemaining = hackedPlayers.get(uuid);

                // 틱이 0 이하로 떨어지면 태그 복구
                if (ticksRemaining <= 0) {
                    ServerPlayer targetPlayer = server.getPlayerList().getPlayer(uuid);
                    if (targetPlayer != null) {
                        // 플레이어가 온라인 상태면 태그 복구
                        targetPlayer.addTag(HACK_TAG);
                        targetPlayer.displayClientMessage(Component.literal("보안 시스템이 복구되었습니다."), true);
                    }
                    // 맵에서 제거 (오프라인 상태여도 제거)
                    hackedPlayers.remove(uuid);
                } else {
                    // 틱 1 감소
                    hackedPlayers.put(uuid, ticksRemaining - 1);
                }
            }
        }
    }
}