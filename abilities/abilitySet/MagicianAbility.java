package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.AbilityEvents;
import com.example.examplemod.ExampleMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
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
// [수정 1] SmallFireball 대신 LargeFireball import
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
import java.util.concurrent.ConcurrentHashMap;

// [참고] WallTickHandler가 이제 이 파일 내부에 중첩 클래스로 포함됩니다.

public class MagicianAbility implements IAbility {

    // --- 각 마법의 촉매 아이템 정의 ---
    // [수정 2] 사용자님의 파일(MagicianAbility.java)을 존중하여 RED_CANDLE 사용
    private static final Item FIRE_CATALYST = Items.RED_CANDLE;
    private static final Item WIND_CATALYST = Items.FEATHER;
    private static final Item EARTH_CATALYST = Items.DIRT;
    private static final Item WATER_CATALYST = Items.NAUTILUS_SHELL;

    // --- 각 마법의 쿨타임 (초) ---
    private static final int FIRE_COOLDOWN_SEC = 8;
    private static final int WIND_COOLDOWN_SEC = 10;
    private static final int EARTH_COOLDOWN_SEC = 15;
    private static final int WATER_COOLDOWN_SEC = 20;


    @Override
    public Component getDescription() {
        return Component.literal("왼손의 촉매에 따라 4원소(불,바람,땅,물) 마법을 사용합니다.");
    }

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
        // 기본 쿨타임 8초
        return FIRE_COOLDOWN_SEC;
    }

    @Override
    public void execute(ServerPlayer player) {
        ItemStack offHandStack = player.getOffhandItem();
        Level level = player.level();
        long currentTime = level.getGameTime();

        // (이제 AbilityEvents.java가 수정되었으므로 이 로직은 의도대로 작동합니다)

        // 1. 불 마법
        if (offHandStack.is(FIRE_CATALYST)) {
            FireSpell.cast(player);
            // 8초 쿨타임 (기본 쿨타임이므로 AbilityEvents가 자동 적용)

            // 2. 바람 마법
        } else if (offHandStack.is(WIND_CATALYST)) {
            WindSpell.cast(player);
            // 10초 쿨타임 덮어쓰기
            long newCooldownEndTick = currentTime + (WIND_COOLDOWN_SEC * 20L);
            AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldownEndTick);

            // 3. 땅 마법
        } else if (offHandStack.is(EARTH_CATALYST)) {
            boolean success = EarthSpell.cast(player);
            if (success) {
                // 15초 쿨타임 덮어쓰기
                long newCooldownEndTick = currentTime + (EARTH_COOLDOWN_SEC * 20L);
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldownEndTick);
            } else {
                // 실패 시 쿨타임 0으로 초기화
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
            }

            // 4. 물 마법
        } else if (offHandStack.is(WATER_CATALYST)) {
            WaterSpell.cast(player);
            // 20초 쿨타임 덮어쓰기
            long newCooldownEndTick = currentTime + (WATER_COOLDOWN_SEC * 20L);
            AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldownEndTick);

            // 5. 촉매 없음
        } else {
            // [수정 3] 힌트 메시지 수정 (RED_CANDLE)
            player.sendSystemMessage(Component.literal("왼손에 속성 촉매(빨간 양초, 깃털, 흙, 앵무조개 껍데기)를 들어주세요."));
            // 쿨타임 0으로 초기화
            AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
        }
    }

    // --- 중첩 클래스 (1): 불 마법 ---
    private static class FireSpell {
        static void cast(ServerPlayer player) {
            Level level = player.level();
            Vec3 look = player.getLookAngle();
            double x = player.getX() + look.x;
            double y = player.getEyeY() + look.y - 0.2;
            double z = player.getZ() + look.z;

            // [수정 4] 맵을 파괴하지 않는 LargeFireball (폭발 위력 0) 사용
            LargeFireball fireball = new LargeFireball(level, player, look, 0);
            fireball.setPos(x, y, z);
            level.addFreshEntity(fireball);

            // [수정 5] 소리 변경 (Ghast)
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.literal("화염구 발사!"));
        }
    }

    // --- 중첩 클래스 (2): 바람 마법 ---
    private static class WindSpell {
        static void cast(ServerPlayer player) {
            MobEffectInstance effectInstance = new MobEffectInstance(
                    MobEffects.SPEED, 100, 0, false, true); // 5초
            player.addEffect(effectInstance);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.5F);
            player.sendSystemMessage(Component.literal("바람처럼 신속해집니다! (5초)"));
        }
    }

    // --- 중첩 클래스 (4): 물 마법 --- (순서 변경)
    private static class WaterSpell {
        @SuppressWarnings("resource")
        static void cast(ServerPlayer caster) {
            ServerLevel level = (ServerLevel) caster.level();
            AABB searchArea = caster.getBoundingBox().inflate(4.0);
            List<ServerPlayer> targets = level.getEntitiesOfClass(ServerPlayer.class, searchArea);

            Holder<MobEffect> regenerationHolder = MobEffects.REGENERATION;
            int durationInTicks = 60; // 3초
            int amplifier = 0; // 재생 I

            for (ServerPlayer target : targets) {
                // (1.21.8 방식)
                MobEffectInstance newHealInstance = new MobEffectInstance(
                        regenerationHolder,
                        durationInTicks,
                        amplifier,
                        false,
                        true
                );
                target.addEffect(newHealInstance);
            }

            // (X,Y,Z 방식)
            level.playSound(null,
                    caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.GENERIC_DRINK,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f);
            level.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            caster.sendSystemMessage(Component.literal("주변 " + targets.size() + "명에게 3초간 재생 효과를 부여합니다!"));
        }
    }

    // --- 중첩 클래스 (3): 땅 마법 ---
    private static class EarthSpell {
        static boolean cast(ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            Direction facing = player.getDirection();
            BlockPos startPos = player.blockPosition().relative(facing, 2);
            BlockState wallBlock = Blocks.COARSE_DIRT.defaultBlockState();
            int wallCount = 0;

            // [수정 6] 3x2 크기
            for (int y = 0; y < 2; y++) {
                for (int i = 0; i < 5; i++) { // 5
                    // [수정 7] 5블록 중앙 정렬 (i-2 -> i-2)
                    BlockPos wallPos = startPos.relative(facing.getClockWise(), i - 2).above(y);
                    if (level.getBlockState(wallPos).canBeReplaced()) {
                        level.setBlock(wallPos, wallBlock, 3);

                        // [수정 8] try-catch 제거, 내부 WallTickHandler 직접 호출
                        WallTickHandler.scheduleWallRemoval(level, wallPos, 3 * 20);
                        wallCount++;
                    }
                }
            }

            if (wallCount > 0) {
                // [수정 9] playSound (X,Y,Z) 방식
                level.playSound(null, startPos.getX() + 0.5, startPos.getY() + 0.5, startPos.getZ() + 0.5, SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
                BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, wallBlock);
                level.sendParticles(particle, startPos.getX() + 0.5, startPos.getY() + 1.0, startPos.getZ() + 0.5, 50, 1.5, 1.0, 1.5, 0.1);
                player.sendSystemMessage(Component.literal("땅의 벽을 3초간 생성합니다!"));
                return true;
            } else {
                player.sendSystemMessage(Component.literal("벽을 생성할 공간이 없습니다."));
                return false;
            }
        }
    }

    // --- 중첩 클래스 (5): 땅 마법 타이머 ---
    @Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class WallTickHandler {

        private static final Map<BlockPos, Long> wallBlocks = new ConcurrentHashMap<>();
        private static ServerLevel worldInstance = null;

        public static void scheduleWallRemoval(ServerLevel level, BlockPos pos, long durationTicks) {
            if (worldInstance == null) {
                worldInstance = level;
            }
            long endTime = level.getGameTime() + durationTicks;
            wallBlocks.put(pos, endTime);
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (wallBlocks.isEmpty() || worldInstance == null) {
                return;
            }

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
}