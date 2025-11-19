package com.example.examplemod;

import com.example.examplemod.abilities.AbilityRegistry;
import com.example.examplemod.abilities.abilitySet.IAbility;
import com.example.examplemod.abilities.abilitySet.StealAbility;
import com.example.examplemod.abilities.abilitySet.TracerAbility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AbilityEvents {

    private static final Map<UUID, ResourceLocation> PLAYER_ABILITIES = new HashMap<>();
    public static final Map<UUID, Long> PLAYER_COOLDOWNS_END_TICK = new HashMap<>();
    private static final Map<UUID, Long> HACKED_PLAYERS_END_TICK = new HashMap<>();

    public static void scheduleHackRevert(UUID playerUUID, long endTime) {
        HACKED_PLAYERS_END_TICK.put(playerUUID, endTime);
    }

    public static IAbility getPlayerAbility(Player player) {
        ResourceLocation id = PLAYER_ABILITIES.get(player.getUUID());
        return (id != null) ? AbilityRegistry.get(id) : null;
    }

    public static void setPlayerAbility(Player player, IAbility ability) {
        if (ability == null) {
            PLAYER_ABILITIES.remove(player.getUUID());
            if (player.getServer() != null) {
                player.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(player.getName().getString() + "의 능력이 제거되었습니다"), false);
            }
        } else {
            PLAYER_ABILITIES.put(player.getUUID(), ability.getId());
            if (player.getServer() != null) {
                player.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(player.getName().getString() + "가 " + ability.getId().getPath() + " 능력을 장착했습니다."),
                        false
                );
            }
        }
        PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), 0L);
        HACKED_PLAYERS_END_TICK.remove(player.getUUID());
        StealAbility.StealHandler.clearStolenAbility(player);
        TracerAbility.RecallHandler.clearHistory(player);
    }

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Pre event) {
        if (event.player.level().isClientSide || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        UUID uuid = serverPlayer.getUUID();
        Level level = serverPlayer.level();
        long currentTime = level.getGameTime();

        Long hackEndTime = HACKED_PLAYERS_END_TICK.get(uuid);
        if (hackEndTime != null && currentTime >= hackEndTime) {
            serverPlayer.removeTag("hacked_pol");
            serverPlayer.addTag("pol");
            HACKED_PLAYERS_END_TICK.remove(uuid);
            serverPlayer.displayClientMessage(Component.literal("시스템이 복구되었습니다."), true);
        }
    }

    @SuppressWarnings("resource")
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();

        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 1. 훔친 능력 사용
        IAbility stolenAbility = StealAbility.StealHandler.getStolenAbility(serverPlayer);
        if (stolenAbility != null) {
            if (stolenAbility.getTriggerItem() != null && stack.getItem().equals(stolenAbility.getTriggerItem())) {
                stolenAbility.execute(serverPlayer);
                StealAbility.StealHandler.clearStolenAbility(serverPlayer);
                stack.shrink(1);
                serverPlayer.displayClientMessage(Component.literal("훔친 능력을 사용했습니다!"), true);
                return;
            }
        }

        // 2. 기본 능력 사용
        IAbility ability = getPlayerAbility(player);
        if (ability == null) {
            return;
        }

        if (ability.getTriggerItem() != null && stack.getItem() == ability.getTriggerItem()) {

            // 3. STEAL 능력은 쿨타임 로직 무시
            if (ability.getId().equals(AbilityRegistry.STEAL.getId())) {
                ability.execute(serverPlayer);
                return;
            }

            // 4. 쿨타임 확인
            long currentTime = level.getGameTime();
            long cooldownEndTick = PLAYER_COOLDOWNS_END_TICK.getOrDefault(player.getUUID(), 0L);

            if (currentTime < cooldownEndTick) {
                long remainingTicks = cooldownEndTick - currentTime;
                double remainingSeconds = remainingTicks / 20.0;
                player.displayClientMessage(Component.literal(
                        "쿨타임: " + String.format("%.1f", remainingSeconds) + "초 남음"
                ), true);
                return;
            }

            // --- [핵심 수정: 선 쿨타임 적용, 후 실행] ---

            // 5. 기본 쿨타임을 *먼저* 적용합니다.
            int cooldownInSeconds = ability.getCooldownSeconds();
            long defaultNewCooldown = currentTime + (cooldownInSeconds * 20L);
            PLAYER_COOLDOWNS_END_TICK.put(player.getUUID(), defaultNewCooldown);

            // 6. 그 다음 능력을 실행합니다.
            // (능력 내부에서 실패하면 쿨타임을 0으로, 물 마법 등은 20초로 덮어쓰게 됩니다.)
            ability.execute(serverPlayer);
        }
    }
}