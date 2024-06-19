package fr.aeldit.cyansh.commands.arguments;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static fr.aeldit.cyansh.CyanSHCore.HomesObj;

public final class ArgumentSuggestion
{
    /**
     * Called for the command {@code /get-homes} or the suggestions of the {@code home} commands
     *
     * @return A suggestion with all the player's homes
     */
    public static CompletableFuture<Suggestions> getHomes(
            @NotNull SuggestionsBuilder builder,
            @NotNull ServerPlayerEntity player
    )
    {
        List<String> names = HomesObj.getHomesNames("%s %s".formatted(player.getUuidAsString(),
                player.getName().getString()
        ));
        if (names != null)
        {
            return CommandSource.suggestMatching(names, builder);
        }
        return new CompletableFuture<>();
    }

}
