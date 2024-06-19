package fr.aeldit.cyansh.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.aeldit.cyansh.commands.arguments.ArgumentSuggestion;
import fr.aeldit.cyansh.warps.Warps;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import static fr.aeldit.cyansh.CyanSHCore.WarpsObj;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WarpCommands {

    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(literal("setwarp")
            .requires(ServerCommandSource::isExecutedByPlayer)
            .requires(Permissions.require("cyansh.setwarp", 4))
            .then(argument("warp", StringArgumentType.word())
                .executes(WarpCommands::setWarp)
            )
        );

        dispatcher.register(literal("delwarp")
            .requires(Permissions.require("cyansh.delwarp", 4))
            .then(argument("warp", StringArgumentType.word()).suggests(((context, builder) -> ArgumentSuggestion.getWarps(builder)))
                .executes(WarpCommands::delWarp)
            )
        );

        dispatcher.register(literal("warp")
            .requires(ServerCommandSource::isExecutedByPlayer)
            .then(argument("warp", StringArgumentType.word()).suggests(((context, builder) -> ArgumentSuggestion.getWarps(builder)))
                .executes(WarpCommands::goToWarp)
            )
        );

        dispatcher.register(literal("warps")
            .executes(WarpCommands::listWarps)
        );

    }

    private static int setWarp(@NotNull CommandContext<ServerCommandSource> context) {
        String warpName = StringArgumentType.getString(context, "warp");
        ServerPlayerEntity player = context.getSource().getPlayer();

        if(WarpsObj.addWarp(
            new Warps.Warp(
                warpName,
                player.getWorld().getDimensionEntry().getIdAsString().replace("minecraft:", "").replace("the_", ""),
                player.getX(), player.getY(), player.getZ(),
                player.getPitch(), player.getYaw()
            )
        )) {
            player.sendMessage(Text.literal(String.format("§6Warp §c%s §6set.", warpName)));
        } else {
            player.sendMessage(Text.literal("§4A warp with that name already exists."));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int delWarp(@NotNull CommandContext<ServerCommandSource> context) {
        String warpName = StringArgumentType.getString(context, "warp");

        if(WarpsObj.deleteWarp(warpName)) {
            context.getSource().sendMessage(Text.literal(String.format("§6Warp §c%s §6has been removed.", warpName)));
        } else {
            context.getSource().sendMessage(Text.literal("§4That warp does not exist."));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int goToWarp(@NotNull CommandContext<ServerCommandSource> context) {
        String warpName = StringArgumentType.getString(context, "warp");
        ServerPlayerEntity player = context.getSource().getPlayer();

        if(!WarpsObj.warpExists(warpName)) {
            player.sendMessage(Text.literal("§4That warp does not exist."));
            return Command.SINGLE_SUCCESS;
        }

        Warps.Warp warp = WarpsObj.getWarp(warpName);
        MinecraftServer server = player.getServer();

        if(warp == null || server == null) return 0;

        RegistryKey<World> world = switch (warp.dimension()) {
            default -> World.OVERWORLD;
            case "nether" -> World.NETHER;
            case "the_end" -> World.END;
        };

        player.teleport(server.getWorld(world), warp.x(), warp.y(), warp.z(), warp.yaw(), warp.pitch());
        player.sendMessage(Text.literal(String.format("§6Warping to §c%s§6.", warp.name())));

        return Command.SINGLE_SUCCESS;
    }

    private static int listWarps(@NotNull CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if(WarpsObj.getWarps().isEmpty()) {
            source.sendMessage(Text.literal("§6No warps defined."));
            return Command.SINGLE_SUCCESS;
        }

        List<Warps.Warp> warps = WarpsObj.getWarps();

        if(warps == null) return 0;

        source.sendMessage(Text.literal(String.format("§6Warps:§r %s", warps.stream().map(Warps.Warp::name).collect(Collectors.joining()))));

        return Command.SINGLE_SUCCESS;
    }

}
