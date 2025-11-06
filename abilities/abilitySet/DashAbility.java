package com.example.examplemod.abilities.abilitySet;

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
    public Item getTriggerItem() {
        // 요청하신 대로 '철창'을 트리거 아이템으로 설정합니다.
        return Items.IRON_BARS;
    }

    @Override
    public int getCooldownSeconds() {
        return 8; // 돌진 쿨타임 (8초)
    }

    @Override
    public void execute(ServerPlayer player) {

        // 1. 플레이어가 바라보는 방향 벡터(Vec3)를 가져옵니다.
        Vec3 look = player.getLookAngle();

        // 2. 돌진 속도를 설정합니다. (급류 3단계와 유사한 값)
        double speed = 2.5;

        // 3. 바라보는 방향으로 속도(Motion)를 설정합니다.
        //    Y축(수직) 속도를 살짝 보정하여(look.y * 0.8) 너무 높이/낮게 날아가지 않게 조절합니다.
        Vec3 motion = new Vec3(look.x * speed, look.y * speed * 0.8 + 0.2, look.z * speed);
        player.setDeltaMovement(motion);

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