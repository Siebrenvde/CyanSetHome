package fr.aeldit.cyansh.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.aeldit.cyansh.commands.arguments.ArgumentSuggestion;
import fr.aeldit.cyansh.homes.Homes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static fr.aeldit.cyansh.CyanSHCore.*;
import static fr.aeldit.cyansh.config.Config.*;

public class HomeCommands {

    public static void register(@NotNull CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("sethome")
            .requires(ServerCommandSource::isExecutedByPlayer)
            .then(CommandManager.argument("home_name", StringArgumentType.string())
                .executes(HomeCommands::setHome)
            )
        );

        dispatcher.register(CommandManager.literal("delhome")
            .requires(ServerCommandSource::isExecutedByPlayer)
            .then(CommandManager.argument("home_name", StringArgumentType.string())
                .suggests((context, builder) -> ArgumentSuggestion.getHomes(
                    builder,
                    Objects.requireNonNull(context.getSource().getPlayer())
                ))
                .executes(HomeCommands::removeHome)
            )
        );

        dispatcher.register(CommandManager.literal("home")
            .requires(ServerCommandSource::isExecutedByPlayer)
            .then(CommandManager.argument("home_name", StringArgumentType.string())
                .suggests((context, builder) -> ArgumentSuggestion.getHomes(
                    builder,
                    Objects.requireNonNull(context.getSource().getPlayer())
                ))
                .executes(HomeCommands::goToHome)
            )
        );

        dispatcher.register(CommandManager.literal("homes")
            .requires(ServerCommandSource::isExecutedByPlayer)
            .executes(HomeCommands::getHomesList)
        );
    }

    /**
     * Called by the command {@code /sethome <home_name>}
     * <p>
     * Creates a home with the player current position (dimension, x, y, z, yaw, pitch, date)
     */
    public static int setHome(@NotNull CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String homeName = StringArgumentType.getString(context, "home_name");
        String playerKey = "%s %s".formatted(player.getUuidAsString(), player.getName().getString());

        if(!HomesObj.maxHomesNotReached(playerKey)) {
            player.sendMessage(Text.literal(String.format("§4You cannot set more than§c %s §4homes.", MAX_HOMES)));
            return Command.SINGLE_SUCCESS;
        }

        if(HomesObj.addHome(
            playerKey,
            new Homes.Home(homeName, player.getWorld().getDimensionEntry().getIdAsString()
                .replace("minecraft:", "").replace("the_", ""),
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(),
                new SimpleDateFormat("dd/MM/yyyy HH:mm").format(
                    Calendar.getInstance().getTime())
            )
        )) {
            player.sendMessage(Text.literal("§6Home set to current location."));
        } else {
            player.sendMessage(Text.literal(String.format("§6You already have a home named §c%s§6!", homeName)));
        }

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Called by the command {@code /delhome <home_name>}
     * <p>
     * Removes the given home
     */
    public static int removeHome(@NotNull CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String homeName = StringArgumentType.getString(context, "home_name");
        String playerKey = "%s %s".formatted(player.getUuidAsString(), player.getName().getString());

        if(HomesObj.removeHome(playerKey, homeName)) {
            player.sendMessage(Text.literal(String.format("§6Home§c %s §6has been removed.", homeName)));
        } else {
            player.sendMessage(Text.literal(String.format("§4Home§c %s §4doesn't exist!", homeName)));
        }

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Called by the command {@code /home <home_name>}
     * <p>
     * Teleports the player to the given home
     */
    public static int goToHome(@NotNull CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String homeName = StringArgumentType.getString(context, "home_name");
        String playerKey = "%s %s".formatted(player.getUuidAsString(), player.getName().getString());

        if(!HomesObj.homeExists(playerKey, homeName)) {
            player.sendMessage(Text.literal(String.format("§4Home§c %s §4doesn't exist!", homeName)));
            return Command.SINGLE_SUCCESS;
        }

        Homes.Home home = HomesObj.getHome(playerKey, homeName);
        MinecraftServer server = player.getServer();

        if (home == null || server == null) return 0;

        switch (home.getDimension())
        {
            case "overworld" -> player.teleport(server.getWorld(World.OVERWORLD), home.getX(),
                    home.getY(), home.getZ(), home.getYaw(), home.getPitch()
            );
            case "nether" -> player.teleport(server.getWorld(World.NETHER), home.getX(),
                    home.getY(), home.getZ(), home.getYaw(), home.getPitch()
            );
            case "end" -> player.teleport(server.getWorld(World.END), home.getX(),
                    home.getY(), home.getZ(), home.getYaw(), home.getPitch()
            );
        }

        player.sendMessage(Text.literal(String.format("§6Teleporting to §c%s§6.", homeName)));

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Called by the command {@code /homes}
     * <p>
     * Sends a message in the player's chat with all its homes
     */
    public static int getHomesList(@NotNull CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        String playerKey = "%s %s".formatted(player.getUuidAsString(), player.getName().getString());

        if(HomesObj.isEmpty(playerKey)) {
            player.sendMessage(Text.literal("§6You have not set any homes."));
            return Command.SINGLE_SUCCESS;
        }

        List<Homes.Home> homes = HomesObj.getPlayerHomes(playerKey);

        if (homes == null) return 0;

        player.sendMessage(Text.literal(String.format("§6Homes:§f %s", homes.stream().map(Homes.Home::getName).collect(Collectors.joining(", ")))));

        return Command.SINGLE_SUCCESS;
    }
}
