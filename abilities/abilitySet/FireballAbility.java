package com.example.examplemod.abilities.abilitySet;

import com.example.examplemod.ExampleMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items; // 예시로 '막대기' 사용
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.LargeFireball;

public class FireballAbility implements IAbility {

    @Override
    public ResourceLocation getId() {
        // "examplemod:fireball"
        return ResourceLocation.fromNamespaceAndPath(ExampleMod.MODID, "fireball");
    }

    @Override
    public Item getTriggerItem() {
        // 이 능력은 '막대기(Stick)'로 발동됩니다.
        return Items.STICK;
    }

    @Override
    public Component getDescription() {
        return Component.literal("바라보는 방향으로 강력한 화염구를 발사합니다. (맵 파괴)");
    }
    @Override
    public int getCooldownSeconds() {
        return 5;
    }

    @Override
    public void execute(ServerPlayer player) {
        Level level = player.level();
        Vec3 look = player.getLookAngle();

        // 플레이어 앞 1칸에서 화염구 생성
        double x = player.getX() + look.x;
        double y = player.getEyeY() + look.y - 0.2; // 눈높이에서 살짝 아래
        double z = player.getZ() + look.z;

        // 화염구의 속도 설정
        LargeFireball fireball = new LargeFireball(level, player, look, 1);
        fireball.setPos(x, y, z);
        level.addFreshEntity(fireball);

        player.sendSystemMessage(Component.literal("화염구 발사!"));
    }
}