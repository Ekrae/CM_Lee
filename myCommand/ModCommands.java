package com.example.examplemod.myCommand;

import com.example.examplemod.AbilityEvents;
import com.example.examplemod.ExampleMod;
import com.example.examplemod.abilities.AbilityRegistry;
import com.example.examplemod.abilities.abilitySet.IAbility;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    public static final String EXCLUDE_TAG = "exclude_ability";
    private static final Random random = new Random();

    private static final SuggestionProvider<CommandSourceStack> ABILITY_SUGGESTIONS = (context, builder) -> {
        AbilityRegistry.getAbilityIds().stream()
                .map(ResourceLocation::toString)
                .forEach(builder::suggest);
        return builder.buildFuture();
    };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        registerAbilityCommand(dispatcher);
        registerRandomizeCommand(dispatcher);
    }

    // =================================================================================
    //                      1. /ability 명령어
    // =================================================================================
    private static void registerAbilityCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ability")
                // set
                .then(Commands.literal("set")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("ability_id", ResourceLocationArgument.id())
                                        .suggests(ABILITY_SUGGESTIONS)
                                        .executes(ModCommands::executeAbilitySet))))
                // get
                .then(Commands.literal("get")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ModCommands::executeAbilityGet)))
                // clear
                .then(Commands.literal("clear")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ModCommands::executeAbilityClear)))
                // list
                .then(Commands.literal("list")
                        .executes(ModCommands::executeAbilityList))
                // help
                .then(Commands.literal("help")
                        .executes(ModCommands::executeAbilityHelpList)
                        .then(Commands.argument("ability_id", ResourceLocationArgument.id())
                                .suggests(ABILITY_SUGGESTIONS)
                                .executes(ModCommands::executeAbilityHelpDetail)))

                // [신규] giveitems: 능력 아이템 지급
                .then(Commands.literal("giveitems")
                        .requires(source -> source.hasPermission(2))
                        // 인자 없으면: exclude 태그 없는 모든 플레이어 대상
                        .executes(ctx -> executeGiveItems(ctx, null))
                        // 인자 있으면: 지정된 플레이어 대상
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> executeGiveItems(ctx, EntityArgument.getPlayers(ctx, "targets")))
                        )
                )
        );
    }

    // --- (set, get, clear, list, help 메서드들은 기존과 동일) ---
    private static int executeAbilitySet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        ResourceLocation abilityId = ResourceLocationArgument.getId(context, "ability_id");
        IAbility ability = AbilityRegistry.get(abilityId);
        if (ability == null) {
            context.getSource().sendFailure(Component.literal("존재하지 않는 능력 ID입니다: " + abilityId));
            return 0;
        }
        AbilityEvents.setPlayerAbility(target, ability);
        context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + "의 능력을 " + abilityId + "(으)로 설정했습니다."), true);
        return 1;
    }

    private static int executeAbilityGet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        IAbility ability = AbilityEvents.getPlayerAbility(target);
        if (ability != null) {
            context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + "의 현재 능력: " + ability.getId().toString()), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + "은(는) 현재 능력이 없습니다."), false);
        }
        return 1;
    }

    private static int executeAbilityClear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        AbilityEvents.setPlayerAbility(target, null);
        context.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + "의 능력을 초기화했습니다."), true);
        return 1;
    }

    private static int executeAbilityList(CommandContext<CommandSourceStack> context) {
        Collection<ResourceLocation> abilities = AbilityRegistry.getAbilityIds();
        if (abilities.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("등록된 능력이 없습니다."), false);
        } else {
            context.getSource().sendSuccess(() -> Component.literal("=== 등록된 능력 목록 ==="), false);
            for (ResourceLocation id : abilities) {
                context.getSource().sendSuccess(() -> Component.literal("- " + id.toString()), false);
            }
        }
        return abilities.size();
    }

    private static int executeAbilityHelpList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        net.minecraft.network.chat.MutableComponent fullMessage = Component.literal("--- 사용 가능한 능력 목록 ---\n");
        for (ResourceLocation id : AbilityRegistry.getAbilityIds()) {
            fullMessage.append(Component.literal(" - ").append(Component.literal(id.getPath()).withStyle(net.minecraft.ChatFormatting.GREEN)).append("\n"));
        }
        fullMessage.append(Component.literal("상세 정보: /ability help <능력ID>").withStyle(net.minecraft.ChatFormatting.YELLOW));
        source.sendSuccess(() -> fullMessage, false);
        return 1;
    }

    private static int executeAbilityHelpDetail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ResourceLocation abilityId = ResourceLocationArgument.getId(context, "ability_id");
        IAbility ability = AbilityRegistry.get(abilityId);
        if (ability == null) {
            source.sendFailure(Component.literal("능력을 찾을 수 없습니다: " + abilityId));
            return 0;
        }
        ItemStack triggerStack = new ItemStack(ability.getTriggerItem());
        Component itemName = triggerStack.getHoverName();
        String itemId = ForgeRegistries.ITEMS.getKey(ability.getTriggerItem()).toString();
        net.minecraft.network.chat.MutableComponent info = Component.literal("")
                .append(Component.literal("--- 능력: ").withStyle(net.minecraft.ChatFormatting.WHITE))
                .append(Component.literal(ability.getId().getPath()).withStyle(net.minecraft.ChatFormatting.GOLD))
                .append(Component.literal(" ---\n"))
                .append(Component.literal("  트리거: ").withStyle(net.minecraft.ChatFormatting.GRAY))
                .append(itemName.copy().withStyle(net.minecraft.ChatFormatting.AQUA))
                .append(Component.literal(" (" + itemId + ")").withStyle(net.minecraft.ChatFormatting.DARK_GRAY))
                .append(Component.literal("\n"))
                .append(Component.literal("  쿨타임: ").withStyle(net.minecraft.ChatFormatting.GRAY))
                .append(Component.literal(ability.getCooldownSeconds() + "초").withStyle(net.minecraft.ChatFormatting.WHITE))
                .append(Component.literal("\n"))
                .append(Component.literal("  효  과: ").withStyle(net.minecraft.ChatFormatting.GRAY))
                .append(ability.getDescription().copy().withStyle(net.minecraft.ChatFormatting.WHITE));
        source.sendSuccess(() -> info, false);
        return 1;
    }

    // --- [신규 구현] executeGiveItems ---
    private static int executeGiveItems(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> explicitTargets) {
        CommandSourceStack source = context.getSource();
        List<ServerPlayer> targets;

        // 대상 선정: 인자 없으면 exclude 태그 없는 전원, 있으면 지정된 인원
        if (explicitTargets == null) {
            targets = source.getServer().getPlayerList().getPlayers().stream()
                    .filter(player -> !player.getTags().contains(EXCLUDE_TAG))
                    .collect(Collectors.toList());
        } else {
            targets = new ArrayList<>(explicitTargets);
        }

        if (targets.isEmpty()) {
            source.sendFailure(Component.literal("아이템을 지급할 대상이 없습니다."));
            return 0;
        }

        int count = 0;
        for (ServerPlayer player : targets) {
            IAbility ability = AbilityEvents.getPlayerAbility(player);
            if (ability != null) {
                // 1. 기본 트리거 아이템 지급
                giveItem(player, ability.getTriggerItem());

                // 2. 마법사(Magician) 추가 아이템: 촉매 4종
                if (ability.getId().equals(AbilityRegistry.MAGICIAN.getId())) {
                    giveItem(player, Items.RED_CANDLE);     // 불
                    giveItem(player, Items.FEATHER);        // 바람
                    giveItem(player, Items.DIRT);           // 땅
                    giveItem(player, Items.COPPER_INGOT);   // 번개
                }

                // 3. 해커(Hack) 추가 아이템: 먹물 주머니
                if (ability.getId().equals(AbilityRegistry.HACK.getId())) {
                    giveItem(player, Items.INK_SAC); // 실명 촉매
                }

                count++;
            }
        }
        // [수정] 람다식 사용을 위해 '사실상 final'인 새 변수에 값을 복사합니다.
        int finalCount = count;

        source.sendSuccess(() -> Component.literal(finalCount + "명에게 능력 아이템을 지급했습니다."), true);
        return count;
    }

    // 아이템 지급 헬퍼 메서드
    private static void giveItem(ServerPlayer player, Item item) {
        ItemStack stack = new ItemStack(item);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // =================================================================================
    //                 2. /randomizeabilities 명령어
    // =================================================================================
    private static void registerRandomizeCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("randomizeabilities")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> executeRandomize(ctx, false, null, ""))
                .then(Commands.argument("allow_duplicates", BoolArgumentType.bool())
                        .executes(ctx -> executeRandomize(ctx, BoolArgumentType.getBool(ctx, "allow_duplicates"), null, ""))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> executeRandomize(ctx, BoolArgumentType.getBool(ctx, "allow_duplicates"), EntityArgument.getPlayers(ctx, "targets"), ""))
                                .then(Commands.argument("excluded_abilities", StringArgumentType.greedyString())
                                        .executes(ctx -> executeRandomize(ctx, BoolArgumentType.getBool(ctx, "allow_duplicates"), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "excluded_abilities")))
                                )
                        )
                )
        );

        dispatcher.register(Commands.literal("ra")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> executeRandomize(ctx, false, null, ""))
                .then(Commands.argument("allow_duplicates", BoolArgumentType.bool())
                        .executes(ctx -> executeRandomize(ctx, BoolArgumentType.getBool(ctx, "allow_duplicates"), null, ""))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> executeRandomize(ctx, BoolArgumentType.getBool(ctx, "allow_duplicates"), EntityArgument.getPlayers(ctx, "targets"), ""))
                                .then(Commands.argument("excluded_abilities", StringArgumentType.greedyString())
                                        .executes(ctx -> executeRandomize(ctx, BoolArgumentType.getBool(ctx, "allow_duplicates"), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "excluded_abilities")))
                                )
                        )
                )
        );
    }

    private static int executeRandomize(CommandContext<CommandSourceStack> context, boolean allowDuplicates, Collection<ServerPlayer> targets, String excludedAbilitiesStr) {
        CommandSourceStack source = context.getSource();
        List<IAbility> availableAbilities = AbilityRegistry.getAbilityIds().stream()
                .map(AbilityRegistry::get)
                .collect(Collectors.toList());

        if (!excludedAbilitiesStr.isEmpty()) {
            String[] excludedIds = excludedAbilitiesStr.split(",");
            for (String exId : excludedIds) {
                ResourceLocation id = ResourceLocation.tryParse(exId.trim());
                if (id != null) availableAbilities.removeIf(ab -> ab.getId().equals(id));
            }
        }

        if (availableAbilities.isEmpty()) {
            source.sendFailure(Component.literal("사용 가능한 능력이 없습니다!"));
            return 0;
        }

        List<ServerPlayer> targetPlayers;
        if (targets == null) {
            targetPlayers = source.getServer().getPlayerList().getPlayers().stream()
                    .filter(player -> !player.getTags().contains(EXCLUDE_TAG))
                    .collect(Collectors.toList());
        } else {
            targetPlayers = new ArrayList<>(targets);
        }

        if (targetPlayers.isEmpty()) {
            source.sendFailure(Component.literal("능력을 받을 플레이어가 없습니다."));
            return 0;
        }

        if (!allowDuplicates && availableAbilities.size() < targetPlayers.size()) {
            source.sendFailure(Component.literal("오류: 인원 수보다 능력 수가 적습니다."));
            return 0;
        }

        if (!allowDuplicates) Collections.shuffle(availableAbilities);

        int assignedCount = 0;
        for (int i = 0; i < targetPlayers.size(); i++) {
            ServerPlayer player = targetPlayers.get(i);
            IAbility selectedAbility = allowDuplicates ?
                    availableAbilities.get(random.nextInt(availableAbilities.size())) :
                    availableAbilities.get(i);

            AbilityEvents.setPlayerAbility(player, selectedAbility);
            player.displayClientMessage(Component.literal("당신의 능력은 [" + selectedAbility.getId().getPath() + "] 입니다!"), true);
            assignedCount++;
        }

        source.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§6[시스템] §f" + assignedCount + "명의 능력 설정 완료!"), false);
        return assignedCount;
    }
}