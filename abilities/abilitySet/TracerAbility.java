//package com.example.examplemod.abilities.abilitySet;
//
//import com.example.examplemod.abilities.IAbility;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.world.item.Item;
//import com.example.examplemod.AbilityEvents;
//import com.example.examplemod.ExampleMod;
//import net.minecraft.network.chat.Component;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.sounds.SoundEvents;
//import net.minecraft.sounds.SoundSource;
//import net.minecraft.world.effect.MobEffectInstance;
//import net.minecraft.world.effect.MobEffects;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.Items; // 예시: 시계
//import net.minecraft.world.level.Level;
//import net.minecraft.world.phys.Vec3;
//import com.example.examplemod.abilitySet.PlayerStateSnapshot;
//
//public class TracerAbility implements IAbility {
//    @Override
//    public ResourceLocation getId() {
//        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "tracer");
//    }
//
//    @Override
//    public Item getTriggerItem() {
//        return Items.CLOCK; // 트리거 아이템: 시계
//    }
//
//    @Override
//    public int getCooldownSeconds() {
//        return 12;
//    }
//
//    @Override
//    public void execute(ServerPlayer player) {
//        // 1. AbilityEvents에서 가장 오래된 기록을 가져옵니다.
//        PlayerStateSnapshot targetState = AbilityEvents.getOldestSnapshot(player);
//
//        if (targetState == null) {
//            player.displayClientMessage(Component.literal("되돌아갈 기록이 없습니다!"), true);
//            return;
//        }
//
//        Level level = player.level();
//        Vec3 currentPos = player.position();
//
//        // --- 2. 화면 및 사운드 효과 ---
//        // 멀미 효과로 화면을 왜곡시킵니다. (1.5초)
//        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION.get(), 30, 0, false, false));
//        // 실명 효과로 잠시 시야를 어둡게 합니다. (0.5초)
//        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS.get(), 10, 0, false, false));
//
//        // 출발 지점과 도착 지점에 엔더맨 텔레포트 소리를 재생합니다.
//        level.playSound(null, currentPos.x, currentPos.y, currentPos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
//        level.playSound(null, targetState.position().x, targetState.position().y, targetState.position().z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
//
//        // --- 3. 상태 복원 ---
//        // 위치와 바라보던 방향으로 텔레포트합니다.
//        player.teleportTo(
//                targetState.position().x,
//                targetState.position().y,
//                targetState.position().z
//        );
//        // 텔레포트 후 시점 변경
//        player.setYRot(targetState.yRot());
//        player.setXRot(targetState.xRot());
//
//        // 체력을 복원합니다.
//        player.setHealth(targetState.health());
//
//        // --- 4. 기록 초기화 ---
//        // 역행을 사용했으므로, 과거 기록을 모두 지워 연속 사용을 방지합니다.
//        AbilityEvents.clearHistory(player);
//
//        player.displayClientMessage(Component.literal("역행!"), true);
//    }
//}
