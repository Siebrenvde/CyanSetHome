package fr.aeldit.cyansh.homes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static fr.aeldit.cyansh.CyanSHCore.*;
import static fr.aeldit.cyansh.config.Config.MAX_HOMES;

public class Homes
{
    public static class Home
    {
        private String name;
        private final String dimension, date;
        private final double x, y, z;
        private final float yaw;
        private final float pitch;

        @Contract(pure = true)
        public Home(String name, String dimension, double x, double y, double z, float yaw, float pitch, String date)
        {
            this.name = name;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.date = date;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public String getDimension()
        {
            return dimension;
        }

        public double getX()
        {
            return x;
        }

        public double getY()
        {
            return y;
        }

        public double getZ()
        {
            return z;
        }

        public float getYaw()
        {
            return yaw;
        }

        public float getPitch()
        {
            return pitch;
        }

        public String getDate()
        {
            return date;
        }
    }

    // HashMap<playerName, List<Home>>
    private final ConcurrentHashMap<String, List<Home>> homes = new ConcurrentHashMap<>();
    private final TypeToken<List<Home>> homesType = new TypeToken<>()
    {
    };
    private final List<String> editingFiles = Collections.synchronizedList(new ArrayList<>());
    public static Path HOMES_PATH = Path.of("%s/homes".formatted(MOD_PATH));

    /**
     * Adds an entry to the map {@code homes} with all the player's homes
     */
    public void addPlayerHomes(String playerKey, List<Home> playerHomes)
    {
        homes.put(playerKey, playerHomes);
        writeHomes(playerKey);
    }

    /**
     * @return {@code true} on success | {@code false} on failure
     */
    public boolean addHome(String playerKey, @NotNull Home home)
    {
        if (!homeExists(playerKey, home.getName()))
        {
            if (!homes.containsKey(playerKey))
            {
                homes.put(playerKey, Collections.synchronizedList(new ArrayList<>(Collections.singleton(home))));
            }
            else
            {
                homes.get(playerKey).add(home);
            }
            writeHomes(playerKey);

            return true;
        }
        return false;
    }

    /**
     * @return {@code true} on success | {@code false} on failure
     */
    public boolean removeHome(@NotNull String playerKey, String homeName)
    {
        Home home = getHome(playerKey, homeName);
        if (home != null)
        {
            homes.get(playerKey).remove(home);

            if (homes.get(playerKey).isEmpty())
            {
                homes.remove(playerKey);
            }
            writeHomes(playerKey);

            return true;
        }
        return false;
    }

    /**
     * Can be called if and only if the result of {@link Homes#isEmpty} is false
     *
     * @return An ArrayList containing all the homes of the player {@code playerName}
     */
    public @Nullable List<Home> getPlayerHomes(String playerName)
    {
        return homes.get(playerName);
    }

    /**
     * @return An ArrayList containing all the homes names of the player {@code playerName}
     */
    public @Nullable List<String> getHomesNames(String playerKey)
    {
        if (homes.containsKey(playerKey))
        {
            List<String> names = new ArrayList<>(homes.get(playerKey).size());
            for (Home home : homes.get(playerKey))
            {
                names.add(home.name);
            }
            return names;
        }
        return null;
    }

    public boolean isEmpty(String playerKey)
    {
        return !homes.containsKey(playerKey) || homes.get(playerKey).isEmpty();
    }

    /**
     * Returns whether the given player has reached its maximum amount of homes
     * <p>
     * If the player is not found in the list (has no homes), this returns {@code false}
     *
     * @param playerKey The player key (in the form {@code "playerUUID playerName"})
     */
    public boolean maxHomesNotReached(String playerKey)
    {
        return !homes.containsKey(playerKey) || homes.get(playerKey).size() < MAX_HOMES;
    }

    /**
     * Returns whether the home with the name {@code homeName} exists in the list of the player's homes
     * <p>
     * If the player is not found in the list, this returns {@code false}
     *
     * @param playerKey The player key (in the form {@code "playerUUID playerName"})
     * @param homeName  The name of the home
     */
    public boolean homeExists(String playerKey, String homeName)
    {
        return getHome(playerKey, homeName) != null;
    }

    /**
     * Returns the index of the home named {@code homeName} belonging
     * to the player whose key is {@code playerKey}
     *
     * @return The index of the given home if it exists | {@code -1} otherwise
     */
    public @Nullable Home getHome(String playerKey, String homeName)
    {
        if (homes.containsKey(playerKey))
        {
            for (Home home : homes.get(playerKey))
            {
                if (home.name.equals(homeName))
                {
                    return home;
                }
            }
        }
        return null;
    }

    /**
     * Renames the username for each UUID if the username currently associated with the UUID is not the same in the file
     *
     * @param playerUUID The player's UUID
     * @param playerName The player's username
     */
    public void renameChangedUsernames(String playerUUID, String playerName)
    {
        if (Files.exists(HOMES_PATH))
        {
            File[] listOfFiles = new File(HOMES_PATH.toUri()).listFiles();

            if (listOfFiles != null)
            {
                String playerKey = "%s %s".formatted(playerUUID, playerName);
                for (File file : listOfFiles)
                {
                    if (file.isFile())
                    {
                        String[] splitFileName = file.getName().split(" ");
                        String[] splitFileNameOld = file.getName().split("_");

                        if (splitFileName[0].equals(playerUUID)
                                && !splitFileName[1].equals("%s.json".formatted(playerName))
                                || (splitFileNameOld.length == 2 && splitFileNameOld[0].equals(
                                playerUUID) && !splitFileNameOld[1].equals("%s.json".formatted(playerName)))
                        )
                        {
                            try
                            {
                                Files.move(
                                        file.toPath(), Path.of("%s/%s.json".formatted(HOMES_PATH, playerKey))
                                                .resolveSibling("%s.json".formatted(playerKey)));
                                CYANSH_LOGGER.info(
                                        "[CyanSetHome] Rename the file '{}' to '{}' because the player changed its " +
                                                "pseudo",
                                        file.getName(), playerKey + ".json"
                                );
                            }
                            catch (IOException e)
                            {
                                throw new RuntimeException(e);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Directory : minecraft/config/cyansh/homes
     */
    public void readServer()
    {
        File[] listOfFiles = new File(HOMES_PATH.toUri()).listFiles();

        if (listOfFiles != null)
        {
            for (File file : listOfFiles)
            {
                if (file.isFile())
                {
                    try
                    {
                        Gson gsonReader = new Gson();
                        Reader reader = Files.newBufferedReader(file.toPath());
                        // TODO -> Don't use \\.
                        addPlayerHomes(
                                file.getName().split("\\.")[0],
                                Collections.synchronizedList(new ArrayList<>(gsonReader.fromJson(reader, homesType)))
                        );
                        reader.close();
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private void writeHomes(String playerKey)
    {
        checkOrCreateHomesDir();

        try
        {
            Path path = Path.of("%s/%s.json".formatted(HOMES_PATH, playerKey));

            if (!homes.containsKey(playerKey) || (homes.containsKey(playerKey) && homes.get(playerKey).isEmpty()))
            {
                if (Files.exists(path))
                {
                    Files.delete(path);
                    removeEmptyModDir();
                }
            }
            else
            {
                if (!editingFiles.contains(path.getFileName().toString()))
                {
                    editingFiles.add(path.getFileName().toString());

                    Gson gsonWriter = new GsonBuilder().setPrettyPrinting().create();
                    Writer writer = Files.newBufferedWriter(path);
                    gsonWriter.toJson(homes.get(playerKey), writer);
                    writer.close();

                    editingFiles.remove(path.getFileName().toString());
                }
                else
                {
                    long end = System.currentTimeMillis() + 1000; // 1 s
                    boolean couldWrite = false;

                    while (System.currentTimeMillis() < end)
                    {
                        if (!editingFiles.contains(path.getFileName().toString()))
                        {
                            editingFiles.add(path.getFileName().toString());

                            Gson gsonWriter = new GsonBuilder().setPrettyPrinting().create();
                            Writer writer = Files.newBufferedWriter(path);
                            gsonWriter.toJson(homes.get(playerKey), writer);
                            writer.close();

                            couldWrite = true;
                            editingFiles.remove(path.getFileName().toString());
                            break;
                        }
                    }

                    if (!couldWrite)
                    {
                        CYANSH_LOGGER.info(
                                ("[CyanSetHome] Could not write the file %s because it is already being written (for " +
                                        "more than 1 sec)").formatted(
                                        path.getFileName().toString()));
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
