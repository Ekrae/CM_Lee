package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.AbilityEvents;
import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MagicianAbility implements IAbility {

    // --- 각 마법의 촉매 아이템 정의 ---
    private static final Item FIRE_CATALYST = Items.RED_CANDLE;
    private static final Item WIND_CATALYST = Items.FEATHER;
    private static final Item EARTH_CATALYST = Items.DIRT;
    private static final Item LIGHTNING_CATALYST = Items.COPPER_INGOT;

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "magician");
    }

    @Override
    public Item getTriggerItem() {
        return Items.STICK;
    }

    @Override
    public int getCooldownSeconds() {
        return Config.magician_fire_cd; // 기본 쿨타임
    }

    @Override
    public Component getDescription() {
        return Component.literal("왼손의 촉매(양초, 깃털, 흙, 구리)에 따라 4원소 마법을 사용합니다.");
    }

    @Override
    public void execute(ServerPlayer player) {
        ItemStack offHandStack = player.getOffhandItem();
        long currentTime = player.level().getGameTime();

        // 1. 불 마법
        if (offHandStack.is(FIRE_CATALYST)) {
            FireSpell.cast(player);
            // 기본 쿨타임 적용 (AbilityEvents가 처리)
        }
        // 2. 바람 마법
        else if (offHandStack.is(WIND_CATALYST)) {
            WindSpell.cast(player);
            long newCooldown = currentTime + (Config.magician_wind_cd * 20L);
            AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldown);
        }
        // 3. 땅 마법
        else if (offHandStack.is(EARTH_CATALYST)) {
            boolean success = EarthSpell.cast(player);
            if (success) {
                long newCooldown = currentTime + (Config.magician_earth_cd * 20L);
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldown);
            } else {
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
            }
        }
        // 4. 번개 마법 (구리 주괴)
        else if (offHandStack.is(LIGHTNING_CATALYST)) {
            boolean success = LightningSpell.cast(player);
            if (success) {
                // [수정] Config의 번개 쿨타임 변수 사용
                long newCooldown = currentTime + (Config.magician_lightning_cd * 20L);
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldown);
            } else {
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
            }
        }
        // 5. 촉매 없음
        else {
            player.sendSystemMessage(Component.literal("왼손에 속성 촉매(양초, 깃털, 흙, 구리 주괴)를 들어주세요."));
            AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
        }
    }

    // --- [1] 불 마법 ---
    private static class FireSpell {
        static void cast(ServerPlayer player) {
            Level level = player.level();
            Vec3 look = player.getLookAngle();
            LargeFireball fireball = new LargeFireball(level, player, look, 0);
            fireball.setPos(player.getX() + look.x, player.getEyeY() + look.y - 0.2, player.getZ() + look.z);
            level.addFreshEntity(fireball);

            // [수정] TracerAbility 방식 (X,Y,Z)
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.literal("화염구 발사!"));
        }
    }

    // --- [2] 바람 마법 ---
    private static class WindSpell {
        static void cast(ServerPlayer player) {
            int duration = Config.magician_wind_dur * 20;
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, duration, 0, false, true));
            // [수정] TracerAbility 방식 (X,Y,Z)
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.5F);
            player.sendSystemMessage(Component.literal("바람처럼 신속해집니다!"));
        }
    }

    // --- [3] 땅 마법 ---
    private static class EarthSpell {
        static boolean cast(ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            Direction facing = player.getDirection();
            BlockPos startPos = player.blockPosition().relative(facing, 2);
            BlockState wallBlock = Blocks.COARSE_DIRT.defaultBlockState();
            int wallCount = 0;

            // 5x2 벽 생성
            for (int y = 0; y < 2; y++) {
                for (int i = 0; i < 5; i++) {
                    BlockPos wallPos = startPos.relative(facing.getClockWise(), i - 2).above(y);
                    if (level.getBlockState(wallPos).canBeReplaced()) {
                        level.setBlock(wallPos, wallBlock, 3);
                        int duration = Config.magician_earth_dur * 20;
                        WallTickHandler.scheduleWallRemoval(level, wallPos, duration);
                        wallCount++;
                    }
                }
            }

            if (wallCount > 0) {
                // [수정] TracerAbility 방식 (X,Y,Z - BlockPos 중심 좌표 계산)
                level.playSound(null, startPos.getX() + 0.5, startPos.getY() + 0.5, startPos.getZ() + 0.5, SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.sendSystemMessage(Component.literal("땅의 벽을 생성합니다!"));
                return true;
            } else {
                player.sendSystemMessage(Component.literal("벽을 생성할 공간이 없습니다."));
                return false;
            }
        }
    }

    // --- [4] 번개 마법 ---
    private static class LightningSpell {
        static boolean cast(ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            double range = 15.0;
            AABB searchArea = player.getBoundingBox().inflate(range);

            List<ServerPlayer> targets = level.getEntitiesOfClass(
                    ServerPlayer.class, searchArea,
                    p -> p != player && p.getTags().contains("pol")
            );

            if (targets.isEmpty()) {
                targets = level.getEntitiesOfClass(
                        ServerPlayer.class, searchArea, p -> p != player
                );
            }

            if (targets.isEmpty()) {
                player.sendSystemMessage(Component.literal("범위 내에 대상이 없습니다."));
                return false;
            }

            ServerPlayer target = targets.get(0);
            double closestDist = player.distanceToSqr(target);
            for (ServerPlayer p : targets) {
                double d = player.distanceToSqr(p);
                if (d < closestDist) {
                    closestDist = d;
                    target = p;
                }
            }

            // 1차 타격
            applyLightningStrike(level, target);
            player.sendSystemMessage(Component.literal(target.getName().getString() + "에게 번개를 떨어뜨립니다!"));

            // 2차 타격 예약
            LightningTickHandler.scheduleSecondStrike(target.getUUID(), 10);

            return true;
        }

        static void applyLightningStrike(ServerLevel level, ServerPlayer target) {
            LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
            lightning.setPos(target.getX(), target.getY(), target.getZ());
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);

            // 물리적 속도/가속도 제거 (순간 정지)
            target.setDeltaMovement(0, target.getDeltaMovement().y, 0);
            target.hurtMarked = true;
            target.hasImpulse = true;

            // 약한 스턴(이동속도 감소)
            int stunDuration = Config.magician_lightning_stun_dur; // [수정] Config 사용
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stunDuration, 2, false, false));

            // 대미지
            target.hurt(level.damageSources().lightningBolt(), 2.0f);

            // [핵심 수정] playSound 오류 해결 (X,Y,Z 좌표 사용)
            level.playSound(null,
                    target.getX(), target.getY(), target.getZ(), // blockPosition() 대신 사용
                    SoundEvents.TRIDENT_THUNDER,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F);
        }
    }

    // --- 핸들러 1: 땅 마법 ---
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class WallTickHandler {
        private static final Map<BlockPos, Long> wallBlocks = new ConcurrentHashMap<>();
        private static ServerLevel worldInstance = null;

        public static void scheduleWallRemoval(ServerLevel level, BlockPos pos, long durationTicks) {
            if (worldInstance == null) worldInstance = level;
            wallBlocks.put(pos, level.getGameTime() + durationTicks);
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (wallBlocks.isEmpty() || worldInstance == null) return;
            long currentTime = worldInstance.getGameTime();
            BlockState air = Blocks.AIR.defaultBlockState();

            wallBlocks.entrySet().removeIf(entry -> {
                if (currentTime >= entry.getValue()) {
                    if (worldInstance.getBlockState(entry.getKey()).is(Blocks.COARSE_DIRT)) {
                        worldInstance.setBlock(entry.getKey(), air, 3);
                    }
                    return true;
                }
                return false;
            });
        }
    }

    // --- 핸들러 2: 번개 2차 타격 ---
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class LightningTickHandler {
        private static final Map<UUID, Long> pendingStrikes = new ConcurrentHashMap<>();
        private static ServerLevel worldInstance = null;

        public static void scheduleSecondStrike(UUID targetUuid, int delayTicks) {
            if (worldInstance != null) {
                pendingStrikes.put(targetUuid, worldInstance.getGameTime() + delayTicks);
            }
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.getServer().overworld() != null) {
                worldInstance = event.getServer().overworld();
            }

            if (pendingStrikes.isEmpty() || worldInstance == null) return;

            long currentTime = worldInstance.getGameTime();
            MinecraftServer server = event.getServer();

            pendingStrikes.entrySet().removeIf(entry -> {
                if (currentTime >= entry.getValue()) {
                    ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
                    if (target != null && target.isAlive()) {
                        // 2차 타격 실행
                        LightningSpell.applyLightningStrike(worldInstance, target);
                        target.sendSystemMessage(Component.literal("2차 충격!"));
                    }
                    return true;
                }
                return false;
            });
        }
    }
}