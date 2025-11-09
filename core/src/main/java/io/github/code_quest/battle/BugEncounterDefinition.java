package io.github.code_quest.battle;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Describes a configurable bug encounter, including visuals and battle parameters,
 * loaded from {@code assets/data/bug_encounters.json}.
 */
public final class BugEncounterDefinition {

    public static final class Visual {
        private String backgroundPath;
        private String questionBoxPath;
        private String answersBoxPath;
        private String correctSfxPath;
        private String incorrectSfxPath;
        private float heroTargetHeight = 280f;
        private float heroFrameDuration = 0.32f;
        private float enemyTargetHeight = 180f;
        private float enemyFrameDuration = 0.28f;
        private final Array<String> heroFramePaths = new Array<>();
        private final Array<String> enemyFramePaths = new Array<>();

        public String getBackgroundPath() {
            return backgroundPath;
        }

        public String getQuestionBoxPath() {
            return questionBoxPath;
        }

        public String getAnswersBoxPath() {
            return answersBoxPath;
        }

        public String getCorrectSfxPath() {
            return correctSfxPath;
        }

        public String getIncorrectSfxPath() {
            return incorrectSfxPath;
        }

        public float getHeroTargetHeight() {
            return heroTargetHeight;
        }

        public float getHeroFrameDuration() {
            return heroFrameDuration;
        }

        public float getEnemyTargetHeight() {
            return enemyTargetHeight;
        }

        public float getEnemyFrameDuration() {
            return enemyFrameDuration;
        }

        public Array<String> getHeroFramePaths() {
            return heroFramePaths;
        }

        public Array<String> getEnemyFramePaths() {
            return enemyFramePaths;
        }
    }

    public static final class Battle {
        private String enemyName;
        private int heroMaxHealth = 100;
        private int enemyMaxHealth = 100;
        private int damageToEnemyPerCorrect = 30;
        private int damageToHeroPerIncorrect = 20;
        private final Array<Question> questions = new Array<>();

        public String getEnemyName() {
            return enemyName;
        }

        public int getHeroMaxHealth() {
            return heroMaxHealth;
        }

        public int getEnemyMaxHealth() {
            return enemyMaxHealth;
        }

        public int getDamageToEnemyPerCorrect() {
            return damageToEnemyPerCorrect;
        }

        public int getDamageToHeroPerIncorrect() {
            return damageToHeroPerIncorrect;
        }

        public Array<Question> getQuestions() {
            return questions;
        }
    }

    public static final class Question {
        private String prompt;
        private final Array<String> options = new Array<>();
        private int correctIndex;

        public String getPrompt() {
            return prompt;
        }

        public Array<String> getOptions() {
            return options;
        }

        public int getCorrectIndex() {
            return correctIndex;
        }
    }

    private final String id;
    private final String bugName;
    private final String bugType;
    private final String locationKey;
    private final Visual visual;
    private final Battle battle;

    private BugEncounterDefinition(String id,
                                   String bugName,
                                   String bugType,
                                   String locationKey,
                                   Visual visual,
                                   Battle battle) {
        this.id = id;
        this.bugName = bugName;
        this.bugType = bugType;
        this.locationKey = locationKey;
        this.visual = visual;
        this.battle = battle;
    }

    public String getId() {
        return id;
    }

    public String getBugName() {
        return bugName;
    }

    public String getBugType() {
        return bugType;
    }

    public String getLocationKey() {
        return locationKey;
    }

    public Visual getVisual() {
        return visual;
    }

    public Battle getBattle() {
        return battle;
    }

    public static BugEncounterDefinition fromJson(String id, JsonValue json) {
        String bugName = json.getString("bugName", id);
        String bugType = json.getString("bugType", "");
        String locationKey = json.getString("locationKey", "");

        Visual visual = new Visual();
        JsonValue visualValue = json.get("visual");
        if (visualValue != null) {
            visual.backgroundPath = visualValue.getString("background", visual.backgroundPath);
            visual.questionBoxPath = visualValue.getString("questionBox", visual.questionBoxPath);
            visual.answersBoxPath = visualValue.getString("answersBox", visual.answersBoxPath);
            visual.correctSfxPath = visualValue.getString("correctSfx", visual.correctSfxPath);
            visual.incorrectSfxPath = visualValue.getString("incorrectSfx", visual.incorrectSfxPath);
            visual.heroTargetHeight = visualValue.getFloat("heroTargetHeight", visual.heroTargetHeight);
            visual.heroFrameDuration = visualValue.getFloat("heroFrameDuration", visual.heroFrameDuration);
            visual.enemyTargetHeight = visualValue.getFloat("enemyTargetHeight", visual.enemyTargetHeight);
            visual.enemyFrameDuration = visualValue.getFloat("enemyFrameDuration", visual.enemyFrameDuration);

            JsonValue heroFrames = visualValue.get("heroFrames");
            if (heroFrames != null) {
                for (JsonValue frame : heroFrames) {
                    if (frame != null && frame.isValue()) {
                        visual.heroFramePaths.add(frame.asString());
                    }
                }
            }

            JsonValue enemyFrames = visualValue.get("enemyFrames");
            if (enemyFrames != null) {
                for (JsonValue frame : enemyFrames) {
                    if (frame != null && frame.isValue()) {
                        visual.enemyFramePaths.add(frame.asString());
                    }
                }
            }
        }

        Battle battle = new Battle();
        JsonValue battleValue = json.get("battle");
        if (battleValue != null) {
            battle.enemyName = battleValue.getString("enemyName", bugName);
            battle.heroMaxHealth = battleValue.getInt("heroMaxHealth", battle.heroMaxHealth);
            battle.enemyMaxHealth = battleValue.getInt("enemyMaxHealth", battle.enemyMaxHealth);
            battle.damageToEnemyPerCorrect = battleValue.getInt("damageToEnemyPerCorrect", battle.damageToEnemyPerCorrect);
            battle.damageToHeroPerIncorrect = battleValue.getInt("damageToHeroPerIncorrect", battle.damageToHeroPerIncorrect);

            JsonValue questions = battleValue.get("questions");
            if (questions != null) {
                for (JsonValue questionValue : questions) {
                    if (questionValue == null) {
                        continue;
                    }
                    Question question = new Question();
                    question.prompt = questionValue.getString("prompt", "");
                    question.correctIndex = questionValue.getInt("correctIndex", 0);

                    JsonValue options = questionValue.get("options");
                    if (options != null) {
                        for (JsonValue optionValue : options) {
                            if (optionValue != null && optionValue.isValue()) {
                                question.options.add(optionValue.asString());
                            }
                        }
                    }

                    battle.questions.add(question);
                }
            }
        } else {
            battle.enemyName = bugName;
        }

        return new BugEncounterDefinition(id, bugName, bugType, locationKey, visual, battle);
    }
}
