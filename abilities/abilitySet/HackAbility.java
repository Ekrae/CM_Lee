package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.AbilityEvents;
import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HackAbility implements IAbility {

    // --- 설정 ---
    private static final String HACK_TAG = "pol"; // 대상 태그
    private static final Item BLIND_CATALYST = Items.INK_SAC; // 실명 촉매

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "hack");
    }

    @Override
    public Item getTriggerItem() {
        return Items.REDSTONE_TORCH;
    }

    @Override
    public int getCooldownSeconds() {
        return Config.hack_cooldown; // 기본 쿨타임 (태그 제거 기준)
    }

    @Override
    public Component getDescription() {
        return Component.literal("주무기: 태그 제거 / 왼손(먹물): 대상 시야 차단(실명)");
    }

    @Override
    public void execute(ServerPlayer player) {
        ItemStack offHandStack = player.getOffhandItem();
        long currentTime = player.level().getGameTime();

        // 1. [신규] 어둠 능력 (실명) - 촉매: 먹물 주머니
        if (offHandStack.is(BLIND_CATALYST)) {
            boolean success = BlindSpell.cast(player);

            if (success) {
                // 성공 시 실명 쿨타임 적용
                long newCooldown = currentTime + (Config.hack_blind_cooldown * 20L);
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldown);
            } else {
                // 실패 시 쿨타임 0
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
            }

        }
        // 2. [기존] 태그 제거 능력 - 촉매: 없음 (기본)
        else {
            boolean success = TagRemovalSpell.cast(player);

            if (success) {
                // 성공 시 기본 쿨타임 적용 (Config.hack_cooldown)
                // AbilityEvents가 기본적으로 적용하지만 확실하게 설정
                long newCooldown = currentTime + (Config.hack_cooldown * 20L);
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldown);
            } else {
                // 실패 시 쿨타임 0
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
            }
        }
    }

    // --- [신규] 실명 마법 (타겟팅) ---
    private static class BlindSpell {
        static boolean cast(ServerPlayer player) {
            double range = Config.hack_blind_range;
            int duration = Config.hack_blind_duration * 20;

            // 시선 처리를 통한 타겟팅 로직
            Vec3 eyePos = player.getEyePosition();
            Vec3 viewVec = player.getViewVector(1.0F);
            Vec3 targetVec = eyePos.add(viewVec.scale(range));
            AABB searchBox = player.getBoundingBox().expandTowards(viewVec.scale(range)).inflate(1.0D);

            // 범위 내 엔티티 중 시선에 닿는 가장 가까운 플레이어 찾기
            List<ServerPlayer> candidates = player.level().getEntitiesOfClass(ServerPlayer.class, searchBox, p -> p != player);

            ServerPlayer target = null;
            double closestDist = range * range;

            for (ServerPlayer candidate : candidates) {
                AABB box = candidate.getBoundingBox().inflate(0.5); // 판정 범위 약간 여유
                Optional<Vec3> hit = box.clip(eyePos, targetVec);

                if (hit.isPresent()) {
                    double dist = eyePos.distanceToSqr(hit.get());
                    if (dist < closestDist) {
                        closestDist = dist;
                        target = candidate;
                    }
                }
            }

            if (target != null) {
                // 효과 적용 (실명 + 어둠)
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, 0, false, false));

                // 피드백
                player.displayClientMessage(Component.literal(target.getName().getString() + "의 시야를 차단했습니다!"), true);
                target.displayClientMessage(Component.literal("시스템이 해킹당해 시야가 차단되었습니다!"), true);

                // 사운드
                player.level().playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.0F, 1.0F);
                target.level().playSound(null, target.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.5F);

                return true;
            } else {
                player.displayClientMessage(Component.literal("사거리 내에 대상이 없습니다."), true);
                return false;
            }
        }
    }

    // --- [기존] 태그 제거 마법 (로직 캡슐화) ---
    private static class TagRemovalSpell {
        static boolean cast(ServerPlayer player) {
            // 범위 설정: 플레이어 중심 5x3x5
            AABB searchArea = new AABB(player.blockPosition()).inflate(2.0, 1.0, 2.0);

            List<ServerPlayer> targets = player.level().getEntitiesOfClass(
                    ServerPlayer.class,
                    searchArea,
                    targetPlayer -> targetPlayer != player && targetPlayer.getTags().contains(HACK_TAG)
            );

            if (targets.isEmpty()) {
                player.displayClientMessage(Component.literal("주변에 '" + HACK_TAG + "' 태그를 가진 플레이어가 없습니다."), true);
                return false;
            }

            int duration = Config.hack_duration * 20;

            for (ServerPlayer target : targets) {
                target.removeTag(HACK_TAG);
                HackHandler.scheduleTagReapply(target.getUUID(), duration);

                target.displayClientMessage(Component.literal("술래가 일시적으로 해킹당했습니다!"), true);
                target.level().playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.PLAYERS, 1.0F, 0.8F);
            }

            player.displayClientMessage(Component.literal(targets.size() + "명의 '" + HACK_TAG + "' 태그를 비활성화했습니다."), true);
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7F, 1.5F);

            return true;
        }
    }

    // --- 핸들러 (기존 유지) ---
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class HackHandler {
        private static final Map<UUID, Integer> hackedPlayers = new ConcurrentHashMap<>();

        public static void scheduleTagReapply(UUID playerUuid, int durationTicks) {
            hackedPlayers.put(playerUuid, durationTicks);
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (hackedPlayers.isEmpty()) return;

            MinecraftServer server = event.getServer();
            for (UUID uuid : new java.util.HashSet<>(hackedPlayers.keySet())) {
                Integer ticksRemaining = hackedPlayers.get(uuid);

                if (ticksRemaining <= 0) {
                    ServerPlayer targetPlayer = server.getPlayerList().getPlayer(uuid);
                    if (targetPlayer != null) {
                        targetPlayer.addTag(HACK_TAG);
                        targetPlayer.displayClientMessage(Component.literal("보안 시스템이 복구되었습니다."), true);
                    }
                    hackedPlayers.remove(uuid);
                } else {
                    hackedPlayers.put(uuid, ticksRemaining - 1);
                }
            }
        }
    }
}