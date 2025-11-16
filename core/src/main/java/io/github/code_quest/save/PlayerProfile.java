package io.github.code_quest.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Stores and retrieves player customization information outside the JSON save file.
 */
public final class PlayerProfile {

    private static final String PREFS_NAME = "player_profile";
    private static final String KEY_NAME = "player_name";
    private static final String KEY_AGE = "player_age";
    private static final String KEY_COURSE = "player_course";
    private static final String KEY_CHARACTER = "character_key";

    private PlayerProfile() {
        // utility class
    }

    private static Preferences prefs() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }

    public static void save(String name, String age, String course, String characterKey) {
        Preferences preferences = prefs();
        preferences.putString(KEY_NAME, name != null ? name : "");
        preferences.putString(KEY_AGE, age != null ? age : "");
        preferences.putString(KEY_COURSE, course != null ? course : "");
        preferences.putString(KEY_CHARACTER, characterKey != null ? characterKey : "male");
        preferences.flush();
    }

    public static ProfileSnapshot load() {
        Preferences preferences = prefs();
        String name = preferences.getString(KEY_NAME, "");
        String age = preferences.getString(KEY_AGE, "");
        String course = preferences.getString(KEY_COURSE, "");
        String characterKey = preferences.getString(KEY_CHARACTER, "");
        return new ProfileSnapshot(name, age, course, characterKey);
    }

    public static String getPlayerName() {
        String name = prefs().getString(KEY_NAME, "");
        return name != null ? name : "";
    }

    public static String getPlayerAge() {
        String age = prefs().getString(KEY_AGE, "");
        return age != null ? age : "";
    }

    public static String getPlayerCourse() {
        String course = prefs().getString(KEY_COURSE, "");
        return course != null ? course : "";
    }

    public static String getCharacterKey(String fallback) {
        String key = prefs().getString(KEY_CHARACTER, fallback != null ? fallback : "");
        if (key == null || key.trim().isEmpty()) {
            return fallback != null ? fallback : "male";
        }
        return key;
    }

    public static final class ProfileSnapshot {
        private final String playerName;
        private final String playerAge;
        private final String playerCourse;
        private final String characterKey;

        private ProfileSnapshot(String playerName, String playerAge, String playerCourse, String characterKey) {
            this.playerName = playerName != null ? playerName : "";
            this.playerAge = playerAge != null ? playerAge : "";
            this.playerCourse = playerCourse != null ? playerCourse : "";
            this.characterKey = characterKey != null ? characterKey : "";
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getPlayerAge() {
            return playerAge;
        }

        public String getPlayerCourse() {
            return playerCourse;
        }

        public String getCharacterKey() {
            return characterKey;
        }
    }
}
