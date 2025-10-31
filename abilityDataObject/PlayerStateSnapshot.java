package com.example.examplemod.abilityDataObject;

import net.minecraft.world.phys.Vec3;

/**
 * 특정 틱(순간)의 플레이어 상태를 저장하는 데이터 클래스.
 * @param position 위치 (x, y, z)
 * @param health 체력
 * @param yRot 좌우 회전 (Yaw)
 * @param xRot 상하 회전 (Pitch)
 */
public record PlayerStateSnapshot(
        Vec3 position,
        float health,
        float yRot,
        float xRot
) {}