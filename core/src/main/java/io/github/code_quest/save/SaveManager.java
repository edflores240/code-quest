package io.github.code_quest.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.Locale;

/**
 * Handles serialization and deserialization of {@link SaveData} to JSON files.
 */
public final class SaveManager {

    private static final String SAVE_FOLDER = "saves";
    private static final String DEFAULT_SAVE_FILE = "profile.json";
    private static final String SAVE_EXTENSION = "json";

    private static final Json JSON = new Json();
    private static String activeSaveFile = DEFAULT_SAVE_FILE;

    static {
        JSON.setOutputType(JsonWriter.OutputType.json);
        JSON.setUsePrototypes(false);
    }

    private SaveManager() {
        // utility
    }

    public static void save(SaveData data) {
        save(data, activeSaveFile);
    }

    public static void save(SaveData data, String fileName) {
        if (data == null) {
            throw new IllegalArgumentException("Save data cannot be null");
        }
        activeSaveFile = sanitizeFileName(fileName);

        FileHandle saveFile = resolveFile(activeSaveFile);
        String json = JSON.toJson(data);
        saveFile.writeString(json, false, "UTF-8");
    }

    public static SaveData load() {
        return load(activeSaveFile);
    }

    public static SaveData load(String fileName) {
        activeSaveFile = sanitizeFileName(fileName);
        FileHandle saveFile = resolveFile(activeSaveFile);
        if (!saveFile.exists()) {
            return null;
        }

        JsonValue value = new JsonReader().parse(saveFile);
        return JSON.readValue(SaveData.class, value);
    }

    public static SaveData loadRaw(FileHandle handle) {
        if (handle == null || !handle.exists()) {
            return null;
        }
        JsonValue value = new JsonReader().parse(handle);
        return JSON.readValue(SaveData.class, value);
    }

    public static FileHandle[] listSaves() {
        FileHandle dir = Gdx.files.local(SAVE_FOLDER);
        if (!dir.exists()) {
            return new FileHandle[0];
        }
        FileHandle[] files = dir.list();
        Array<FileHandle> filtered = new Array<>();
        for (FileHandle handle : files) {
            if (handle.name().toLowerCase(Locale.ROOT).endsWith("." + SAVE_EXTENSION)) {
                filtered.add(handle);
            }
        }
        return filtered.toArray(FileHandle.class);
    }

    public static boolean delete(String fileName) {
        FileHandle saveFile = resolveFile(fileName);
        if (!saveFile.exists()) {
            return false;
        }
        return saveFile.delete();
    }

    public static boolean exists() {
        return exists(activeSaveFile);
    }

    public static boolean exists(String fileName) {
        return resolveFile(fileName).exists();
    }

    public static void setActiveFile(String fileName) {
        activeSaveFile = sanitizeFileName(fileName);
    }

    public static String getActiveFile() {
        return activeSaveFile;
    }

    public static String getDefaultFileName() {
        return DEFAULT_SAVE_FILE;
    }

    public static String createNewSlot() {
        String newName = "slot-" + System.currentTimeMillis() + "." + SAVE_EXTENSION;
        setActiveFile(newName);
        return newName;
    }

    private static FileHandle resolveFile(String fileName) {
        FileHandle dir = Gdx.files.local(SAVE_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.child(sanitizeFileName(fileName));
    }

    private static String sanitizeFileName(String fileName) {
        String value = (fileName == null || fileName.trim().isEmpty())
                ? DEFAULT_SAVE_FILE
                : fileName.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (!lower.endsWith("." + SAVE_EXTENSION)) {
            value += "." + SAVE_EXTENSION;
        }
        return value;
    }
}
