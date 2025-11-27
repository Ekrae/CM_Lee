package com.example.examplemod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // =========================================
    //         Config Spec Definitions
    // =========================================

    // --- Bind Ability ---
    private static final ForgeConfigSpec.IntValue BIND_COOLDOWN = BUILDER.comment("Bind Ability Cooldown (seconds)").defineInRange("abilities.bind.cooldown", 15, 0, 3600);
    private static final ForgeConfigSpec.DoubleValue BIND_RADIUS = BUILDER.comment("Bind Ability Radius (blocks)").defineInRange("abilities.bind.radius", 4.0, 1.0, 64.0);
    private static final ForgeConfigSpec.IntValue BIND_DURATION = BUILDER.comment("Bind Ability Duration (seconds)").defineInRange("abilities.bind.duration", 3, 1, 60);

    // --- Dash Ability ---
    private static final ForgeConfigSpec.IntValue DASH_COOLDOWN = BUILDER.comment("Dash Ability Cooldown (seconds)").defineInRange("abilities.dash.cooldown", 8, 0, 3600);
    private static final ForgeConfigSpec.DoubleValue DASH_SPEED = BUILDER.comment("Dash Ability Speed Multiplier").defineInRange("abilities.dash.speed", 2.5, 0.1, 10.0);

    // --- Fireball Ability ---
    private static final ForgeConfigSpec.IntValue FIREBALL_COOLDOWN = BUILDER.comment("Fireball Ability Cooldown (seconds)").defineInRange("abilities.fireball.cooldown", 5, 0, 3600);

    // --- Gambler Ability ---
    private static final ForgeConfigSpec.IntValue GAMBLER_COOLDOWN = BUILDER.comment("Gambler Ability Cooldown (seconds)").defineInRange("abilities.gambler.cooldown", 5, 0, 3600);
    private static final ForgeConfigSpec.IntValue GAMBLER_DURATION = BUILDER.comment("Gambler Effect Duration (seconds)").defineInRange("abilities.gambler.duration", 10, 1, 600);
    // [추가] 실패 시 지속 시간 설정
    private static final ForgeConfigSpec.IntValue GAMBLER_FAIL_DURATION = BUILDER.comment("Gambler Failure Duration (seconds)").defineInRange("abilities.gambler.fail_duration", 5, 1, 600);
    // --- Hack Ability (기존 + 신규 실명 설정) ---
    private static final ForgeConfigSpec.IntValue HACK_COOLDOWN = BUILDER.comment("Hack Tag Removal Cooldown (seconds)").defineInRange("abilities.hack.cooldown", 20, 0, 3600);
    private static final ForgeConfigSpec.IntValue HACK_DURATION = BUILDER.comment("Hack Tag Removal Duration (seconds)").defineInRange("abilities.hack.duration", 5, 1, 600);
    // [신규] 실명 능력 설정
    private static final ForgeConfigSpec.IntValue HACK_BLIND_COOLDOWN = BUILDER.comment("Hack Blind Cooldown (seconds)").defineInRange("abilities.hack.blind_cooldown", 15, 0, 3600);
    private static final ForgeConfigSpec.IntValue HACK_BLIND_DURATION = BUILDER.comment("Hack Blind Duration (seconds)").defineInRange("abilities.hack.blind_duration", 5, 1, 60);
    private static final ForgeConfigSpec.DoubleValue HACK_BLIND_RANGE = BUILDER.comment("Hack Blind Range (blocks)").defineInRange("abilities.hack.blind_range", 10.0, 1.0, 100.0);

    // --- Invisibility Ability ---
    private static final ForgeConfigSpec.IntValue INVISIBILITY_COOLDOWN = BUILDER.comment("Invisibility Ability Cooldown (seconds)").defineInRange("abilities.invisibility.cooldown", 20, 0, 3600);
    private static final ForgeConfigSpec.IntValue INVISIBILITY_DURATION = BUILDER.comment("Invisibility Duration (seconds)").defineInRange("abilities.invisibility.duration", 5, 1, 600);

    // --- Kingslayer Ability ---
    private static final ForgeConfigSpec.IntValue KINGSLAYER_COOLDOWN = BUILDER.comment("Kingslayer Ability Cooldown (seconds)").defineInRange("abilities.kingslayer.cooldown", 15, 0, 3600);
    private static final ForgeConfigSpec.DoubleValue KINGSLAYER_RANGE = BUILDER.comment("Kingslayer Cast Range").defineInRange("abilities.kingslayer.range", 5.0, 1.0, 100.0);
    private static final ForgeConfigSpec.DoubleValue KINGSLAYER_POL_HEAL = BUILDER.comment("Heal amount when targeting POL (Health Points)").defineInRange("abilities.kingslayer.pol.heal", 12.0, 0.0, 100.0);
    private static final ForgeConfigSpec.DoubleValue KINGSLAYER_POL_DAMAGE = BUILDER.comment("Damage amount when targeting POL").defineInRange("abilities.kingslayer.pol.damage", 4.0, 0.0, 100.0);
    private static final ForgeConfigSpec.DoubleValue KINGSLAYER_RUNNER_HEAL = BUILDER.comment("Heal amount when targeting Runner (Health Points)").defineInRange("abilities.kingslayer.runner.heal", 3.0, 0.0, 100.0);
    private static final ForgeConfigSpec.DoubleValue KINGSLAYER_RUNNER_DAMAGE = BUILDER.comment("Damage amount when targeting Runner").defineInRange("abilities.kingslayer.runner.damage", 6.0, 0.0, 100.0);

    // --- Magician Ability ---
    private static final ForgeConfigSpec.IntValue MAGICIAN_FIRE_COOLDOWN = BUILDER.comment("Magician Fire Cooldown").defineInRange("abilities.magician.fire.cooldown", 8, 0, 3600);
    private static final ForgeConfigSpec.IntValue MAGICIAN_WIND_COOLDOWN = BUILDER.comment("Magician Wind Cooldown").defineInRange("abilities.magician.wind.cooldown", 10, 0, 3600);
    private static final ForgeConfigSpec.IntValue MAGICIAN_WIND_DURATION = BUILDER.comment("Magician Wind Duration").defineInRange("abilities.magician.wind.duration", 5, 1, 600);
    private static final ForgeConfigSpec.IntValue MAGICIAN_EARTH_COOLDOWN = BUILDER.comment("Magician Earth Cooldown").defineInRange("abilities.magician.earth.cooldown", 15, 0, 3600);
    private static final ForgeConfigSpec.IntValue MAGICIAN_EARTH_DURATION = BUILDER.comment("Magician Earth Wall Duration").defineInRange("abilities.magician.earth.duration", 3, 1, 600);
    // [수정] 물(Water) -> 번개(Lightning)
    private static final ForgeConfigSpec.IntValue MAGICIAN_LIGHTNING_COOLDOWN = BUILDER.comment("Magician Lightning Cooldown").defineInRange("abilities.magician.lightning.cooldown", 20, 0, 3600);
    private static final ForgeConfigSpec.IntValue MAGICIAN_LIGHTNING_STUN_DURATION = BUILDER.comment("Magician Lightning Stun Duration (ticks per hit)").defineInRange("abilities.magician.lightning.stun_duration", 15, 1, 100);

    // --- Push Ability ---
    private static final ForgeConfigSpec.IntValue PUSH_COOLDOWN = BUILDER.comment("Push Ability Cooldown (seconds)").defineInRange("abilities.push.cooldown", 10, 0, 3600);
    private static final ForgeConfigSpec.DoubleValue PUSH_STRENGTH = BUILDER.comment("Push Knockback Strength").defineInRange("abilities.push.strength", 20, 0.1, 30.0);

    // --- Steal Ability ---
    private static final ForgeConfigSpec.IntValue STEAL_COOLDOWN = BUILDER.comment("Steal Ability Cooldown (seconds)").defineInRange("abilities.steal.cooldown", 30, 0, 3600);

    // --- Swap Ability ---
    private static final ForgeConfigSpec.IntValue SWAP_COOLDOWN = BUILDER.comment("Swap Ability Cooldown (seconds)").defineInRange("abilities.swap.cooldown", 20, 0, 3600);
    private static final ForgeConfigSpec.DoubleValue SWAP_RANGE = BUILDER.comment("Swap Ability Range (blocks)").defineInRange("abilities.swap.range", 40.0, 1.0, 200.0);

    // --- Tracer Ability ---
    private static final ForgeConfigSpec.IntValue TRACER_COOLDOWN = BUILDER.comment("Tracer Ability Cooldown (seconds)").defineInRange("abilities.tracer.cooldown", 12, 0, 3600);
    // [수정] History 설정 삭제 (요청 사항 반영)

    // --- Misc ---
    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);
    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER.comment("A magic number").defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER.comment("What you want the introduction message to be for the magic number").define("magicNumberIntroduction", "The magic number is... ");
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER.comment("A list of items to log on common setup.").defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // =========================================
    //         Runtime Variables
    // =========================================
    public static int bind_cooldown, bind_duration;
    public static double bind_radius;

    public static int dash_cooldown;
    public static double dash_speed;

    public static int fireball_cooldown;

    public static int gambler_cooldown, gambler_duration,gambler_fail_duration;

    public static int hack_cooldown, hack_duration;
    // [신규] 해커 실명 변수
    public static int hack_blind_cooldown, hack_blind_duration;
    public static double hack_blind_range;

    public static int invisibility_cooldown, invisibility_duration;

    public static int kingslayer_cooldown;
    public static double kingslayer_range, kingslayer_pol_heal, kingslayer_pol_damage, kingslayer_runner_heal, kingslayer_runner_damage;

    public static int magician_fire_cd, magician_wind_cd, magician_wind_dur, magician_earth_cd, magician_earth_dur;
    public static int magician_lightning_cd, magician_lightning_stun_dur;

    public static int push_cooldown;
    public static double push_strength;

    public static int steal_cooldown;

    public static int swap_cooldown;
    public static double swap_range;

    public static int tracer_cooldown; // History 변수 제거

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        bind_cooldown = BIND_COOLDOWN.get();
        bind_radius = BIND_RADIUS.get();
        bind_duration = BIND_DURATION.get();

        dash_cooldown = DASH_COOLDOWN.get();
        dash_speed = DASH_SPEED.get();

        fireball_cooldown = FIREBALL_COOLDOWN.get();

        gambler_cooldown = GAMBLER_COOLDOWN.get();
        gambler_duration = GAMBLER_DURATION.get();
        gambler_fail_duration = GAMBLER_FAIL_DURATION.get();

        hack_cooldown = HACK_COOLDOWN.get();
        hack_duration = HACK_DURATION.get();
        // [신규] 해커 실명 설정 로딩
        hack_blind_cooldown = HACK_BLIND_COOLDOWN.get();
        hack_blind_duration = HACK_BLIND_DURATION.get();
        hack_blind_range = HACK_BLIND_RANGE.get();

        invisibility_cooldown = INVISIBILITY_COOLDOWN.get();
        invisibility_duration = INVISIBILITY_DURATION.get();

        kingslayer_cooldown = KINGSLAYER_COOLDOWN.get();
        kingslayer_range = KINGSLAYER_RANGE.get();
        kingslayer_pol_heal = KINGSLAYER_POL_HEAL.get();
        kingslayer_pol_damage = KINGSLAYER_POL_DAMAGE.get();
        kingslayer_runner_heal = KINGSLAYER_RUNNER_HEAL.get();
        kingslayer_runner_damage = KINGSLAYER_RUNNER_DAMAGE.get();

        magician_fire_cd = MAGICIAN_FIRE_COOLDOWN.get();
        magician_wind_cd = MAGICIAN_WIND_COOLDOWN.get();
        magician_wind_dur = MAGICIAN_WIND_DURATION.get();
        magician_earth_cd = MAGICIAN_EARTH_COOLDOWN.get();
        magician_earth_dur = MAGICIAN_EARTH_DURATION.get();
        magician_lightning_cd = MAGICIAN_LIGHTNING_COOLDOWN.get();
        magician_lightning_stun_dur = MAGICIAN_LIGHTNING_STUN_DURATION.get();

        push_cooldown = PUSH_COOLDOWN.get();
        push_strength = PUSH_STRENGTH.get();

        steal_cooldown = STEAL_COOLDOWN.get();

        swap_cooldown = SWAP_COOLDOWN.get();
        swap_range = SWAP_RANGE.get();

        tracer_cooldown = TRACER_COOLDOWN.get();
        // tracer_history 로딩 제거

        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemName)))
                .collect(Collectors.toSet());
    }
}