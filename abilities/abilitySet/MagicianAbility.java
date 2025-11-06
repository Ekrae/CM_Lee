package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.AbilityEvents; // [중요] 쿨타임 직접 제어를 위해 Import
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
import net.minecraft.world.entity.projectile.SmallFireball;
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

// [참고] 땅 마법의 벽 제거 로직(WallTickHandler)은 파일이 너무 커지는 것을 방지하기 위해
// MagicianEarthAbility.java 파일에 그대로 두었다고 가정합니다.
// 만약 해당 파일들을 삭제하셨다면, 그 파일의 'WallTickHandler' 중첩 클래스를
// 이 파일의 최하단(MagicianAbility 클래스 밖)이나 별도 파일로 옮겨야 합니다.
// (여기서는 MagicianEarthAbility.WallTickHandler를 호출하는 것으로 구현했습니다.)

public class MagicianAbility implements IAbility {

    // --- 각 마법의 촉매 아이템 정의 ---
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
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "magician");
    }

    @Override
    public Item getTriggerItem() {
        return Items.STICK; // 주무기: 지팡이(막대기)
    }

    @Override
    public int getCooldownSeconds() {
        // [중요] AbilityEvents가 참조할 기본(최소) 쿨타임
        return FIRE_COOLDOWN_SEC; // 가장 짧은 8초
    }

    @Override
    public void execute(ServerPlayer player) {
        ItemStack offHandStack = player.getOffhandItem(); // 왼손(보조무기) 아이템 확인
        Level level = player.level();
        long currentTime = level.getGameTime();

        // 1. 불 마법
        if (offHandStack.is(FIRE_CATALYST)) {
            FireSpell.cast(player);
            // 쿨타임 8초 (getCooldownSeconds()와 동일하므로 AbilityEvents가 자동으로 처리)

            // 2. 바람 마법
        } else if (offHandStack.is(WIND_CATALYST)) {
            WindSpell.cast(player);
            // [쿨타임 덮어쓰기] 10초
            long newCooldownEndTick = currentTime + (WIND_COOLDOWN_SEC * 20L);
            AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldownEndTick);

            // 3. 땅 마법
        } else if (offHandStack.is(EARTH_CATALYST)) {
            boolean success = EarthSpell.cast(player);
            if (success) {
                // [쿨타임 덮어쓰기] 15초
                long newCooldownEndTick = currentTime + (EARTH_COOLDOWN_SEC * 20L);
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldownEndTick);
            } else {
                // 실패 시 쿨타임 초기화
                AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
            }

            // 4. 물 마법
        } else if (offHandStack.is(WATER_CATALYST)) {
            WaterSpell.cast(player);
            // [쿨타임 덮어쓰기] 20초
            long newCooldownEndTick = currentTime + (WATER_COOLDOWN_SEC * 20L);
            AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), newCooldownEndTick);

            // 5. 촉매 없음
        } else {
            player.sendSystemMessage(Component.literal("왼손에 속성 촉매(화염구, 깃털, 흙, 앵무조개 껍데기)를 들어주세요."));
            // [중요] 쿨타임 초기화
            AbilityEvents.PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
        }
    }

    // --- 사용자님이 제안하신 '중첩 클래스'를 활용한 코드 정리 ---

    /** 🔥 불 마법 */
    private static class FireSpell {
        static void cast(ServerPlayer player) {
            Level level = player.level();
            Vec3 look = player.getLookAngle();
            double x = player.getX() + look.x;
            double y = player.getEyeY() + look.y - 0.2;
            double z = player.getZ() + look.z;

            SmallFireball fireball = new SmallFireball(level, player, look);
            fireball.setPos(x, y, z);
            level.addFreshEntity(fireball);

            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.literal("화염구 발사!"));
        }
    }

    /** 💨 바람 마법 */
    private static class WindSpell {
        static void cast(ServerPlayer player) {
            MobEffectInstance effectInstance = new MobEffectInstance(
                    MobEffects.SPEED, 100, 0, false, true); // 5초
            player.addEffect(effectInstance);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.5F);
            player.sendSystemMessage(Component.literal("바람처럼 신속해집니다! (5초)"));
        }
    }

    /** 💧 물 마법 */
    private static class WaterSpell {
        @SuppressWarnings("resource")
        static void cast(ServerPlayer caster) {
            ServerLevel level = (ServerLevel) caster.level();
            AABB searchArea = caster.getBoundingBox().inflate(4.0);
            List<ServerPlayer> targets = level.getEntitiesOfClass(ServerPlayer.class, searchArea);

            // 1. 효과의 "설계도(Holder)"와 설정을 밖에서 정의
            Holder<MobEffect> regenerationHolder = MobEffects.REGENERATION;
            int durationInTicks = 60; // 3초
            int amplifier = 0; // 재생 I

            for (ServerPlayer target : targets) {
                // 2. [핵심 수정] 루프 안에서 매번 '새로운' 효과 인스턴스를 생성하여 적용
                MobEffectInstance newHealInstance = new MobEffectInstance(
                        regenerationHolder,
                        durationInTicks,
                        amplifier,
                        false, // Ambient
                        true  // Show particles
                );
                target.addEffect(newHealInstance);
            }

            // [수정] 1.20.x 호환을 위해 .get()을 사용했으나, 1.21.8 환경이 확실하므로 .get() 제거
            level.playSound(null,
                    caster.getX(), caster.getY(), caster.getZ(), // caster.blockPosition() 대신 사용
                    SoundEvents.GENERIC_DRINK,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f);
            level.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY() + 1.0, caster.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
            caster.sendSystemMessage(Component.literal("주변 " + targets.size() + "명에게 3초간 재생 효과를 부여합니다!"));
        }
    }

    /** ⛰️ 땅 마법 */
    private static class EarthSpell {
        static boolean cast(ServerPlayer player) {
            ServerLevel level = (ServerLevel) player.level();
            Direction facing = player.getDirection();
            BlockPos startPos = player.blockPosition().relative(facing, 2);
            BlockState wallBlock = Blocks.COARSE_DIRT.defaultBlockState();
            int wallCount = 0;

            for (int y = 0; y < 2; y++) {
                for (int i = 0; i < 3; i++) {
                    BlockPos wallPos = startPos.relative(facing.getClockWise(), i - 1).above(y);
                    if (level.getBlockState(wallPos).canBeReplaced()) {
                        level.setBlock(wallPos, wallBlock, 3);

                        // [중요] MagicianEarthAbility의 WallTickHandler를 호출합니다.
                        // (이 클래스가 별도 파일로 존재하거나, 이 파일 하단에 복사되어야 함)
                        try {
                            WallTickHandler.scheduleWallRemoval(level, wallPos, 3 * 20);
                            wallCount++;
                        } catch (NoClassDefFoundError e) {
                            player.sendSystemMessage(Component.literal("오류: WallTickHandler 클래스를 찾을 수 없습니다."));
                            return false;
                        }
                    }
                }
            }

            if (wallCount > 0) {
                level.playSound(null, startPos, SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
                BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, wallBlock);
                level.sendParticles(particle, startPos.getX() + 0.5, startPos.getY() + 1.0, startPos.getZ() + 0.5, 50, 2.0, 1.0, 2.0, 0.1);
                player.sendSystemMessage(Component.literal("땅의 벽을 3초간 생성합니다!"));
                return true;
            } else {
                player.sendSystemMessage(Component.literal("벽을 생성할 공간이 없습니다."));
                return false;
            }
        }
    }

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
            // [수정] 1.21.8+ 에서는 event.phase 구분이 없음 (HackHandler 참고)
            if (wallBlocks.isEmpty() || worldInstance == null) {
                return;
            }

            long currentTime = worldInstance.getGameTime();
            BlockState air = Blocks.AIR.defaultBlockState();

            wallBlocks.entrySet().removeIf(entry -> {
                if (currentTime >= entry.getValue()) {
                    // [수정] '거친 흙'일 때만 제거하도록 변경
                    if (worldInstance.getBlockState(entry.getKey()).is(Blocks.COARSE_DIRT)) {
                        worldInstance.setBlock(entry.getKey(), air, 3);
                    }
                    return true; // 맵에서 제거
                }
                return false; // 유지
            });
        }
    }
}
