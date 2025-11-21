package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.Config;
import com.example.examplemod.ExampleMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class DashAbility implements IAbility {

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "dash");
    }

    @Override
    public Component getDescription() {
        return Component.literal("바라보는 수평 방향으로 빠르게 돌진합니다.");
    }
    @Override
    public Item getTriggerItem() {
        // 요청하신 대로 '철창'을 트리거 아이템으로 설정합니다.
        return Items.IRON_BARS;
    }

    @Override
    public int getCooldownSeconds() {
        return Config.dash_cooldown; // 돌진 쿨타임 (8초)
    }

    @Override
    public void execute(ServerPlayer player) {

        // 1. 플레이어가 바라보는 방향 벡터(Vec3)를 가져옵니다.
        Vec3 look = player.getLookAngle();
        Vec3 currentMotion = player.getDeltaMovement();
        // 2. 돌진 속도를 설정합니다. (급류 3단계와 유사한 값)
        // 2. 돌진 속도를 설정합니다.
        double speed = Config.dash_speed;

        // 3. 바라보는 방향에서 수평(X, Z) 방향 벡터만 추출하고 정규화(normalize)합니다.
        //    (normalize를 해야 위나 아래를 보고 돌진해도 수평 속도가 일정하게 유지됩니다)
        Vec3 horizontalDir = new Vec3(look.x, 0.0, look.z).normalize();
        // 4. 새로운 속도를 계산합니다.
        //    X와 Z는 수평 돌진 속도로 덮어쓰고, Y는 현재 Y속도(중력 등)를 그대로 유지합니다.
        Vec3 newMotion = new Vec3(
                horizontalDir.x * speed,
                currentMotion.y(), // <-- 수직 속도는 그대로 유지
                horizontalDir.z * speed
        );
        player.setDeltaMovement(newMotion);

        // 4. [매우 중요] 서버에서 변경한 속도를 클라이언트에 즉시 동기화합니다.
        //    이 값을 true로 설정해야 날아가는 움직임이 렉 없이 부드럽게 적용됩니다.
        player.hurtMarked = true;

        // 5. [중요] 돌진 직후 낙하 데미지를 받지 않도록 낙하 거리를 초기화합니다.
        player.fallDistance = 0.0F;

        // 6. 효과 (소리 및 파티클)
        ServerLevel level = (ServerLevel) player.level();
        // 급류(Riptide) 효과음을 사용합니다.
        // [수정] TracerAbility와 동일하게 X, Y, Z 좌표를 직접 전달합니다.
        level.playSound(null,
                player.getX(), player.getY(), player.getZ(), // player.blockPosition() 대신 사용
                SoundEvents.TRIDENT_RIPTIDE_1,
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        // 물보라 파티클을 플레이어 위치에 생성합니다.
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.5, 0.5, 0.5, 0.1);

        // 7. 피드백
        player.displayClientMessage(Component.literal("돌진!"), true);
    }
}