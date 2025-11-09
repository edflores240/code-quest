package io.github.code_quest.battle;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;

/**
 * Core turn-based battle logic shared by encounter screens.
 * Tracks combatants, health pools, questions, and outcome resolution
 * so that screens can focus on presentation.
 */
public class BattleMechanics {

    public enum Outcome {
        HERO_ATTACK,
        ENEMY_ATTACK,
        STALEMATE
    }

    public static final class Result {
        private final Outcome outcome;
        private final boolean correct;
        private final String message;
        private final boolean battleFinished;
        private final boolean heroVictorious;
        private final int heroHealth;
        private final int heroMaxHealth;
        private final int enemyHealth;
        private final int enemyMaxHealth;

        private Result(Outcome outcome,
                       boolean correct,
                       String message,
                       boolean battleFinished,
                       boolean heroVictorious,
                       int heroHealth,
                       int heroMaxHealth,
                       int enemyHealth,
                       int enemyMaxHealth) {
            this.outcome = outcome;
            this.correct = correct;
            this.message = message;
            this.battleFinished = battleFinished;
            this.heroVictorious = heroVictorious;
            this.heroHealth = heroHealth;
            this.heroMaxHealth = heroMaxHealth;
            this.enemyHealth = enemyHealth;
            this.enemyMaxHealth = enemyMaxHealth;
        }

        public Outcome getOutcome() {
            return outcome;
        }

        public boolean isCorrect() {
            return correct;
        }

        public String getMessage() {
            return message;
        }

        public boolean isBattleFinished() {
            return battleFinished;
        }

        public boolean isHeroVictorious() {
            return heroVictorious;
        }

        public int getHeroHealth() {
            return heroHealth;
        }

        public int getHeroMaxHealth() {
            return heroMaxHealth;
        }

        public int getEnemyHealth() {
            return enemyHealth;
        }

        public int getEnemyMaxHealth() {
            return enemyMaxHealth;
        }
    }

    public static final class BattleQuestion {
        private final String prompt;
        private final Array<String> options;
        private final int correctIndex;

        public BattleQuestion(String prompt, Array<String> options, int correctIndex) {
            if (prompt == null || options == null || options.isEmpty()) {
                throw new IllegalArgumentException("Prompt and options are required");
            }
            if (correctIndex < 0 || correctIndex >= options.size) {
                throw new IllegalArgumentException("Correct index out of range");
            }
            this.prompt = prompt;
            this.options = new Array<>(options);
            this.correctIndex = correctIndex;
        }

        public String getPrompt() {
            return prompt;
        }

        public Array<String> getOptions() {
            return new Array<>(options);
        }

        public boolean isCorrect(int index) {
            return index == correctIndex;
        }

        public int getCorrectIndex() {
            return correctIndex;
        }
    }

    public static final class Builder {
        private String heroName = "Hero";
        private int heroMaxHealth = 100;
        private String enemyName = "Enemy";
        private int enemyMaxHealth = 100;
        private int damageToEnemyPerCorrect = 30;
        private int damageToHeroPerIncorrect = 25;
        private final Array<BattleQuestion> questions = new Array<>();

        public Builder heroName(String value) {
            if (value != null) {
                this.heroName = value;
            }
            return this;
        }

        public Builder heroMaxHealth(int value) {
            this.heroMaxHealth = Math.max(1, value);
            return this;
        }

        public Builder enemyName(String value) {
            if (value != null) {
                this.enemyName = value;
            }
            return this;
        }

        public Builder enemyMaxHealth(int value) {
            this.enemyMaxHealth = Math.max(1, value);
            return this;
        }

        public Builder damageToEnemyPerCorrect(int value) {
            this.damageToEnemyPerCorrect = Math.max(1, value);
            return this;
        }

        public Builder damageToHeroPerIncorrect(int value) {
            this.damageToHeroPerIncorrect = Math.max(1, value);
            return this;
        }

        public Builder addQuestion(String prompt, Array<String> options, int correctIndex) {
            questions.add(new BattleQuestion(prompt, options, correctIndex));
            return this;
        }

        public Builder addQuestion(String prompt, String[] options, int correctIndex) {
            Array<String> optionArray = new Array<>(options.length);
            for (String option : options) {
                optionArray.add(option);
            }
            return addQuestion(prompt, optionArray, correctIndex);
        }

        public BattleMechanics build() {
            if (questions.isEmpty()) {
                throw new IllegalStateException("At least one question is required for a battle");
            }
            return new BattleMechanics(heroName,
                    heroMaxHealth,
                    enemyName,
                    enemyMaxHealth,
                    damageToEnemyPerCorrect,
                    damageToHeroPerIncorrect,
                    questions);
        }
    }

    private final String heroName;
    private final String enemyName;
    private final int heroMaxHealth;
    private final int enemyMaxHealth;
    private final int damageToEnemyPerCorrect;
    private final int damageToHeroPerIncorrect;
    private final Array<BattleQuestion> questions;

    private int heroHealth;
    private int enemyHealth;
    private int currentQuestionIndex;
    private boolean battleFinished;
    private boolean heroVictorious;

    private BattleMechanics(String heroName,
                            int heroMaxHealth,
                            String enemyName,
                            int enemyMaxHealth,
                            int damageToEnemyPerCorrect,
                            int damageToHeroPerIncorrect,
                            Array<BattleQuestion> questions) {
        this.heroName = heroName;
        this.heroMaxHealth = heroMaxHealth;
        this.enemyName = enemyName;
        this.enemyMaxHealth = enemyMaxHealth;
        this.damageToEnemyPerCorrect = damageToEnemyPerCorrect;
        this.damageToHeroPerIncorrect = damageToHeroPerIncorrect;
        this.questions = new Array<>(questions);

        this.heroHealth = heroMaxHealth;
        this.enemyHealth = enemyMaxHealth;
        this.currentQuestionIndex = 0;
        this.battleFinished = false;
        this.heroVictorious = false;
    }

    public String getHeroName() {
        return heroName;
    }

    public String getEnemyName() {
        return enemyName;
    }

    public int getHeroHealth() {
        return heroHealth;
    }

    public int getHeroMaxHealth() {
        return heroMaxHealth;
    }

    public int getEnemyHealth() {
        return enemyHealth;
    }

    public int getEnemyMaxHealth() {
        return enemyMaxHealth;
    }

    public float getHeroHealthRatio() {
        return heroHealth / (float) heroMaxHealth;
    }

    public float getEnemyHealthRatio() {
        return enemyHealth / (float) enemyMaxHealth;
    }

    public boolean isBattleFinished() {
        return battleFinished;
    }

    public boolean isHeroVictorious() {
        return heroVictorious;
    }

    public BattleQuestion getCurrentQuestion() {
        if (questions.isEmpty()) {
            return null;
        }
        int index = Math.min(currentQuestionIndex, questions.size - 1);
        return questions.get(index);
    }

    public Result submitAnswer(int optionIndex) {
        if (battleFinished) {
            return new Result(Outcome.STALEMATE,
                    false,
                    heroName + " already resolved the encounter.",
                    true,
                    heroVictorious,
                    heroHealth,
                    heroMaxHealth,
                    enemyHealth,
                    enemyMaxHealth);
        }

        BattleQuestion question = getCurrentQuestion();
        if (question == null) {
            battleFinished = true;
            heroVictorious = true;
            return new Result(Outcome.STALEMATE,
                    true,
                    heroName + " completes the encounter effortlessly.",
                    true,
                    true,
                    heroHealth,
                    heroMaxHealth,
                    enemyHealth,
                    enemyMaxHealth);
        }

        boolean correct = question.isCorrect(optionIndex);
        Outcome outcome;
        StringBuilder messageBuilder = new StringBuilder();

        if (correct) {
            enemyHealth = Math.max(0, enemyHealth - damageToEnemyPerCorrect);
            outcome = Outcome.HERO_ATTACK;
            messageBuilder.append(heroName)
                    .append(" executes a flawless command! ")
                    .append(enemyName)
                    .append(" loses ")
                    .append(damageToEnemyPerCorrect)
                    .append(" HP.");

            if (enemyHealth <= 0) {
                battleFinished = true;
                heroVictorious = true;
                messageBuilder.append('\n')
                        .append(enemyName)
                        .append(" is debugged! ")
                        .append(heroName)
                        .append(" wins the encounter.");
            } else {
                advanceQuestion();
            }
        } else {
            heroHealth = Math.max(0, heroHealth - damageToHeroPerIncorrect);
            outcome = Outcome.ENEMY_ATTACK;
            messageBuilder.append(enemyName)
                    .append(" exploits the mistake! ")
                    .append(heroName)
                    .append(" loses ")
                    .append(damageToHeroPerIncorrect)
                    .append(" HP.");

            if (heroHealth <= 0) {
                battleFinished = true;
                heroVictorious = false;
                messageBuilder.append('\n')
                        .append(heroName)
                        .append(" can no longer continue.");
            }
        }

        return new Result(outcome,
                correct,
                messageBuilder.toString(),
                battleFinished,
                heroVictorious,
                heroHealth,
                heroMaxHealth,
                enemyHealth,
                enemyMaxHealth);
    }

    private void advanceQuestion() {
        currentQuestionIndex = (currentQuestionIndex + 1) % questions.size;
    }
}
