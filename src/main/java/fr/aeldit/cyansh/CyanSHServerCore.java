package fr.aeldit.cyansh;

import fr.aeldit.cyansh.commands.HomeCommands;
import fr.aeldit.cyansh.commands.WarpCommands;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import static fr.aeldit.cyansh.CyanSHCore.*;
import static fr.aeldit.cyansh.util.EventUtils.renameFileIfUsernameChanged;

public class CyanSHServerCore implements DedicatedServerModInitializer
{
    @Override
    public void onInitializeServer()
    {
        HomesObj.readServer();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> renameFileIfUsernameChanged(handler));

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            HomeCommands.register(dispatcher);
            WarpCommands.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> removeEmptyModDir());

        CYANSH_LOGGER.info("[CyanSetHome] Successfully initialized");
    }
}
