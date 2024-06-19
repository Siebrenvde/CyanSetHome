package fr.aeldit.cyansh.warps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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

import static fr.aeldit.cyansh.CyanSHCore.MOD_PATH;
import static fr.aeldit.cyansh.CyanSHCore.checkOrCreateHomesDir;

public class Warps {

    public record Warp(String name, String dimension, double x, double y, double z, float pitch, float yaw) {}

    private List<Warp> warps = new ArrayList<>();
    private final TypeToken<List<Warp>> warpsTypeToken = new TypeToken<>(){};
    public static Path WARPS_PATH = Path.of("%s/warps.json".formatted(MOD_PATH));

    public boolean addWarp(@NotNull Warp warp) {
        if(warpExists(warp.name())) { return false; }
        warps.add(warp);
        writeWarps();
        return true;
    }

    public boolean deleteWarp(@NotNull String warpName) {
        Warp warp = getWarp(warpName);
        if(warp == null) { return false; }
        warps.remove(warp);
        writeWarps();
        return true;
    }

    public @Nullable Warp getWarp(@NotNull String name) {
        for(Warp warp : warps) {
            if(warp.name().equals(name)) { return warp; }
        }
        return null;
    }

    public List<Warp> getWarps() {
        return Collections.unmodifiableList(warps);
    }

    public boolean warpExists(@NotNull String name) {
        return getWarp(name) != null;
    }

    /**
     * Read all warps into memory
     */
    public void readServer() {
        File file = new File(WARPS_PATH.toUri());

        if (file.isFile())
        {
            try
            {
                Gson gsonReader = new Gson();
                Reader reader = Files.newBufferedReader(file.toPath());
                warps = Collections.synchronizedList(new ArrayList<>(gsonReader.fromJson(reader, warpsTypeToken)));
                reader.close();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Write warps to file
     */
    private void writeWarps() {
        checkOrCreateHomesDir();

        try {
            Gson gsonWriter = new GsonBuilder().setPrettyPrinting().create();
            Writer writer = Files.newBufferedWriter(WARPS_PATH);
            gsonWriter.toJson(warps, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
