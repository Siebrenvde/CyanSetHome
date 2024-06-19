package fr.aeldit.cyansh.commands.arguments;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.aeldit.cyansh.warps.Warps;
import net.minecraft.command.CommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static fr.aeldit.cyansh.CyanSHCore.HomesObj;
import static fr.aeldit.cyansh.CyanSHCore.WarpsObj;

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

    public static CompletableFuture<Suggestions> getWarps(@NotNull SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(WarpsObj.getWarps().stream().map(Warps.Warp::name), builder);
    }

}
