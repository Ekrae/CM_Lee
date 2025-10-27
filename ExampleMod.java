package com.example.examplemod;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge; // 중요!
import net.minecraftforge.event.RegisterCommandsEvent; // 중요!
import net.minecraft.commands.arguments.ResourceLocationArgument;
import com.example.examplemod.AbilityEvents;
import com.example.examplemod.abilities.AbilityRegistry;
import com.example.examplemod.abilities.IAbility;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.SharedSuggestionProvider;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ExampleMod.MODID)
public final class ExampleMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "examplemod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "examplemod" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "examplemod:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("example_block"))
            .mapColor(MapColor.STONE)
        )
    );
    // Creates a new BlockItem with the id "examplemod:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block",
        () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties().setId(ITEMS.key("example_block")))
    );

    // Creates a new food item with the id "examplemod:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item",
        () -> new Item(new Item.Properties()
            .setId(ITEMS.key("example_item"))
            .food(new FoodProperties.Builder()
                .alwaysEdible()
                .nutrition(1)
                .saturationModifier(2f)
                .build()
            )
        )
    );

    // Creates a creative tab with the id "examplemod:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    public ExampleMod(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        // Register the commonSetup method for modloading
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modBusGroup);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modBusGroup);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modBusGroup);

        // Register the item to a creative tab
        BuildCreativeModeTabContentsEvent.getBus(modBusGroup).addListener(ExampleMod::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        //명령어는 EVENT_BUS에 등록, 나머지는 modBusGroup에 등록.
        MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent
    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    @SubscribeEvent
    private static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }
    // --- [ 여기에 새 코드 추가 ] ---

    /**
     * 명령어 등록 이벤트를 수신합니다. (Forge 이벤트 버스에서 호출됨)
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // 기존 /bee 명령어는 삭제하거나 주석 처리
        /*
        dispatcher.register(
            Commands.literal("bee").executes(this::executeBeeCommand)
        );
        */

        // --- 새로운 /ability 명령어 등록 ---
        dispatcher.register(
                Commands.literal("ability")
                        // 관리자(op레벨 2)만 사용 가능하도록 설정
                        .requires(cs -> cs.hasPermission(2))

                        // /ability set <player> <ability_id>
                        .then(Commands.literal("set")
                                .then(Commands.argument("player", EntityArgument.player())// String ArgumentType.string() 대신 ResourceLocationArgument.id() 사용
                                        .then(Commands.argument("ability_id", ResourceLocationArgument.id()) // <ability_id>
                                                // 명령어 자동완성 (Tab 키) 기능 추가
                                                .suggests((context, builder) -> {
                                                    AbilityRegistry.getAbilityIds().stream()
                                                            .map(ResourceLocation::toString)
                                                            .forEach(builder::suggest);
                                                    return builder.buildFuture();
                                                })
                                                .executes(this::executeAbilitySet) // 실행할 메서드
                                        )
                                )
                        )
                        // /ability clear <player>
                        .then(Commands.literal("clear")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(this::executeAbilityClear) // 실행할 메서드
                                )
                        )
        );
    }

    /**
     * /ability set <player> <ability_id> 명령어의 실제 로직
     */
    private int executeAbilitySet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ResourceLocation abilityId = ResourceLocationArgument.getId(context,"ability_id"); //context가 입력받는 커맨드. 그 안에 ability_id에 대한 정보

                IAbility ability = AbilityRegistry.get(abilityId);
        if (ability == null) {
            context.getSource().sendFailure(Component.literal(abilityId + " 능력을 찾을 수 없습니다.")); //여기도 문자열로 객체 출력시 .toString이 자동으로 붙나봄
            return 0;
        }

        // AbilityEvents에 정의한 메서드 호출
        AbilityEvents.setPlayerAbility(player, ability);
        context.getSource().sendSuccess(() -> Component.literal(
                player.getName().getString() + "에게 " + ability.getId().getPath() + " 능력을 부여했습니다."
        ), true);

        return 1; // 성공
    }

    /**
     * /ability clear <player> 명령어의 실제 로직
     */
    private int executeAbilityClear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");

        // ability를 null로 설정하여 제거
        AbilityEvents.setPlayerAbility(player, null);
        context.getSource().sendSuccess(() -> Component.literal(
                player.getName().getString() + "의 능력을 제거했습니다."
        ), true);

        return 1; // 성공
    }

    /**
     * /bee 명령어의 실제 실행 로직
     */
    private int executeBeeCommand(CommandContext<CommandSourceStack> context) {
        // 명령어를 실행한 주체(소스)를 가져옵니다.
        CommandSourceStack source = context.getSource();

        // 명령어를 실행한 것이 플레이어인지 확인합니다.
        if (source.getEntity() instanceof ServerPlayer player) {
            // 플레이어의 현재 위치를 가져옵니다.
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();

            // Y축(위쪽)으로 1.0만큼 텔레포트시킵니다.
            player.teleportTo(x, y + 1.0, z);

            // 성공 메시지를 채팅창에 보냅니다. (false는 다른 관리자에게 알리지 않음)
            source.sendSuccess(() -> Component.literal("Bzzzz! (위로 1칸 이동!)"), false);

            return 1; // 1은 명령어 성공을 의미합니다.
        } else {
            // 플레이어가 아닌 경우 (예: 커맨드 블록)
            source.sendFailure(Component.literal("이 명령어는 플레이어만 사용할 수 있습니다!"));
            return 0; // 0은 명령어 실패를 의미합니다.
        }
    }
    // --- [ 여기까지 ] ---

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
