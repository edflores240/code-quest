package io.github.code_quest.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Loads and caches {@link BugEncounterDefinition} instances from a JSON data file.
 */
public final class BugEncounterRepository {

    private static final String DATA_PATH = "assets/data/bug_encounters.json";
    private static final ObjectMap<String, BugEncounterDefinition> DEFINITIONS = new ObjectMap<>();
    private static boolean loaded;

    private BugEncounterRepository() {
        // utility
    }

    public static synchronized BugEncounterDefinition get(String id) {
        ensureLoaded();
        BugEncounterDefinition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown bug encounter id: " + id);
        }
        return definition;
    }

    public static synchronized boolean has(String id) {
        ensureLoaded();
        return DEFINITIONS.containsKey(id);
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        FileHandle handle = Gdx.files.internal(DATA_PATH);
        if (!handle.exists()) {
            Gdx.app.error("BugEncounterRepository", "Missing encounter data file: " + DATA_PATH);
            loaded = true;
            return;
        }

        JsonReader reader = new JsonReader();
        JsonValue root = reader.parse(handle);
        for (JsonValue entry = root.child; entry != null; entry = entry.next) {
            String id = entry.name;
            if (id == null || id.trim().isEmpty()) {
                continue;
            }
            BugEncounterDefinition definition = BugEncounterDefinition.fromJson(id, entry);
            DEFINITIONS.put(id, definition);
        }
        loaded = true;
    }
}
