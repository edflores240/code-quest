package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;
import io.github.code_quest.battle.BattleMechanics;
import io.github.code_quest.battle.BugEncounterDefinition;
import io.github.code_quest.battle.BugEncounterRepository;
import java.util.function.Supplier;

/**
 * Reusable battle presentation screen that drives encounters with the BotBug NPC (and future foes).
 * Assets are pulled from images/fightingscreen and the combat flow is orchestrated by {@link BattleMechanics}.
 */
public class FightingBugScreen implements Screen {

    private static final float VIEWPORT_WIDTH = 1024f;
    private static final float VIEWPORT_HEIGHT = 576f;
    private static final float OPTION_SPACING = 46f;
    private static final float KEY_COOLDOWN = 0.18f;
    private static final float MESSAGE_DURATION = 3.2f;
    private static final float POST_BATTLE_DELAY = 2.5f;
    private static final float ENEMY_LOOP_PAUSE = 0.6f;
    private static final float HERO_ATTACK_DISPLAY = 0.55f;
    private static final float HERO_HURT_DISPLAY = 0.55f;

    private final Main game;
    private final String characterKey;
    private final BattleMechanics mechanics;
    private final VisualConfig visuals;
    private final Supplier<Screen> returnSupplier;

    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final SpriteBatch batch;

    private final BitmapFont promptFont;
    private final BitmapFont optionFont;
    private final BitmapFont infoFont;
    private final GlyphLayout glyphLayout;

    private final Array<Texture> disposableTextures;
    private Texture questionBoxTexture;
    private Texture optionsBoxTexture;
    private Texture backgroundTexture;
    private Texture whitePixel;

    private Animation<TextureRegion> heroIntroAnimation;
    private Animation<TextureRegion> enemyAnimation;
    private float heroAnimTime;
    private float enemyAnimTime;
    private TextureRegion heroIdleFrame;
    private TextureRegion heroAttackFrame;
    private TextureRegion heroHurtFrame;
    private float heroAttackTimer;
    private float heroHurtTimer;
    private boolean heroIntroFinished;
    private TextureRegion enemyIdleFrame;

    private Sound correctSound;
    private Sound incorrectSound;

    private float keyTimer;
    private int selectedOption;

    private BattleMechanics.Result lastResult;
    private float messageTimer;
    private float postBattleTimer;

    public FightingBugScreen(Main game, String characterKey) {
        this(game,
                characterKey,
                createDefaultMechanics(characterKey),
                VisualConfig.botBugDefaults(),
                () -> new GreenValleyScreen(game, characterKey));
    }

    public FightingBugScreen(Main game, String characterKey, String encounterId) {
        this(game,
                characterKey,
                createMechanicsFromEncounter(characterKey, encounterId),
                createVisualsFromEncounter(encounterId),
                () -> new GreenValleyScreen(game, characterKey));
    }

    public FightingBugScreen(Main game,
                             String characterKey,
                             BattleMechanics mechanics,
                             VisualConfig visuals,
                             Supplier<Screen> returnSupplier) {
        this.game = game;
        this.characterKey = characterKey;
        this.mechanics = mechanics;
        this.visuals = visuals != null ? visuals : VisualConfig.botBugDefaults();
        this.returnSupplier = returnSupplier;

        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        camera.setToOrtho(false, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        camera.position.set(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT / 2f, 0f);
        camera.update();

        this.batch = new SpriteBatch();
        this.promptFont = new BitmapFont();
        this.optionFont = new BitmapFont();
        this.infoFont = new BitmapFont();
        this.glyphLayout = new GlyphLayout();

        this.disposableTextures = new Array<>();

        configureFonts();
        loadVisualAssets();
        loadAudio();

        this.selectedOption = 0;
        this.heroAnimTime = 0f;
        this.enemyAnimTime = 0f;
        this.keyTimer = 0f;
        this.messageTimer = 0f;
        this.postBattleTimer = -1f;
        this.heroHurtTimer = 0f;
    }

    private void configureFonts() {
        promptFont.getData().setScale(1.35f);
        promptFont.setColor(Color.valueOf("F3E4A6"));

        optionFont.getData().setScale(1.15f);
        optionFont.setColor(Color.valueOf("D9D8FF"));

        infoFont.getData().setScale(0.95f);
        infoFont.setColor(Color.valueOf("EFE6C7"));
    }

    private void loadVisualAssets() {
        this.whitePixel = createSolidTexture(Color.WHITE);

        backgroundTexture = loadOptionalTexture(visuals.backgroundPath, false);
        questionBoxTexture = loadOptionalTexture(visuals.questionBoxPath, true);
        optionsBoxTexture = loadOptionalTexture(visuals.answersBoxPath, true);

        heroIntroAnimation = null;
        heroIdleFrame = null;
        heroAttackFrame = null;
        heroHurtFrame = null;
        Array<TextureRegion> heroFrames = loadFrameRegions(visuals.heroFramePaths);
        if (!heroFrames.isEmpty()) {
            heroAttackFrame = heroFrames.peek();
            heroIdleFrame = heroFrames.size >= 2 ? heroFrames.get(heroFrames.size - 2) : heroAttackFrame;
            Array<TextureRegion> introFrames = new Array<>(heroFrames);
            introFrames.pop();
            if (!introFrames.isEmpty()) {
                heroIntroAnimation = buildAnimationFromRegions(introFrames, visuals.heroFrameDuration, Animation.PlayMode.NORMAL);
            }
        }

        heroHurtFrame = loadFrameRegion("assets/images/fightingscreen/fightingframe/wronganswer.png");

        enemyIdleFrame = null;
        enemyAnimation = buildAnimation(visuals.enemyFramePaths, visuals.enemyFrameDuration);
        if (enemyAnimation != null) {
            enemyAnimation.setPlayMode(Animation.PlayMode.NORMAL);
            enemyIdleFrame = enemyAnimation.getKeyFrame(0f);
        } else if (!heroFrames.isEmpty()) {
            // fallback to first enemy frame if animation couldn't be created
            enemyIdleFrame = loadFrameRegion(visuals.enemyFramePaths.size > 0 ? visuals.enemyFramePaths.first() : null);
        }
    }

    private void loadAudio() {
        if (visuals.correctSfxPath != null && Gdx.files.internal(visuals.correctSfxPath).exists()) {
            correctSound = Gdx.audio.newSound(Gdx.files.internal(visuals.correctSfxPath));
        }
        if (visuals.incorrectSfxPath != null && Gdx.files.internal(visuals.incorrectSfxPath).exists()) {
            incorrectSound = Gdx.audio.newSound(Gdx.files.internal(visuals.incorrectSfxPath));
        }
    }

    public static BattleMechanics createDefaultMechanics(String heroName) {
        String resolvedHeroName = heroName == null || heroName.trim().isEmpty() ? "Hero" : heroName;
        return new BattleMechanics.Builder()
                .heroName(resolvedHeroName)
                .enemyName("BotBug")
                .heroMaxHealth(100)
                .enemyMaxHealth(90)
                .damageToEnemyPerCorrect(35)
                .damageToHeroPerIncorrect(25)
                .addQuestion(
                        "How to print CodeQuest?",
                        new String[]{
                                "System.out.println(\"CodeQuest\");",
                                "printf(\"CodeQuest\");",
                                "System.out.println(\"CodeQuest\");",
                                "system.out.println(\"CodeQuest\");"
                        },
                        0)
                .addQuestion(
                        "Which keyword defines a class in Java?",
                        new String[]{"def", "class", "struct", "function"},
                        1)
                .addQuestion(
                        "Choose the correct file extension for Java source files",
                        new String[]{".class", ".java", ".jar", ".jav"},
                        1)
                .build();
    }

    private static BattleMechanics createMechanicsFromEncounter(String heroName, String encounterId) {
        if (encounterId != null && BugEncounterRepository.has(encounterId)) {
            BugEncounterDefinition definition = BugEncounterRepository.get(encounterId);
            BugEncounterDefinition.Battle battle = definition.getBattle();
            BattleMechanics.Builder builder = new BattleMechanics.Builder()
                    .heroName(heroName)
                    .enemyName(battle.getEnemyName())
                    .heroMaxHealth(battle.getHeroMaxHealth())
                    .enemyMaxHealth(battle.getEnemyMaxHealth())
                    .damageToEnemyPerCorrect(battle.getDamageToEnemyPerCorrect())
                    .damageToHeroPerIncorrect(battle.getDamageToHeroPerIncorrect());
            for (BugEncounterDefinition.Question question : battle.getQuestions()) {
                builder.addQuestion(question.getPrompt(), question.getOptions(), question.getCorrectIndex());
            }
            return builder.build();
        }
        return createDefaultMechanics(heroName);
    }

    private static VisualConfig createVisualsFromEncounter(String encounterId) {
        if (encounterId != null && BugEncounterRepository.has(encounterId)) {
            BugEncounterDefinition definition = BugEncounterRepository.get(encounterId);
            return VisualConfig.fromDefinition(definition.getVisual());
        }
        return VisualConfig.botBugDefaults();
    }

    @Override
    public void show() {
        heroAnimTime = 0f;
        enemyAnimTime = 0f;
        selectedOption = 0;
        messageTimer = 0f;
        lastResult = null;
        postBattleTimer = -1f;
        heroAttackTimer = 0f;
        heroHurtTimer = 0f;
        heroIntroFinished = heroIntroAnimation == null;
    }

    @Override
    public void render(float delta) {
        updateTimers(delta);
        handleInput();

        Gdx.gl.glClearColor(0.04f, 0.07f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        drawBackground();
        drawHealthBars();
        drawCharacters();
        drawQuestionPanel();
        drawOptionsPanel();
        drawStatusText();

        batch.end();

        attemptAutoReturn(delta);
    }

    private void updateTimers(float delta) {
        if (!heroIntroFinished) {
            heroAnimTime += delta;
            if (heroIntroAnimation != null && heroIntroAnimation.isAnimationFinished(heroAnimTime)) {
                heroIntroFinished = true;
                heroAnimTime = heroIntroAnimation.getAnimationDuration();
            }
        }
        enemyAnimTime += delta;
        keyTimer = Math.max(0f, keyTimer - delta);

        if (messageTimer > 0f) {
            messageTimer -= delta;
        }

        if (postBattleTimer >= 0f) {
            postBattleTimer -= delta;
        }

        if (heroAttackTimer > 0f) {
            heroAttackTimer = Math.max(0f, heroAttackTimer - delta);
        }

        if (heroHurtTimer > 0f) {
            heroHurtTimer = Math.max(0f, heroHurtTimer - delta);
        }
    }

    private void handleInput() {
        if (keyTimer > 0f) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            moveSelection(-1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            moveSelection(1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            submitOrExit();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    private void moveSelection(int delta) {
        BattleMechanics.BattleQuestion question = mechanics.getCurrentQuestion();
        if (question == null) {
            return;
        }
        Array<String> options = question.getOptions();
        if (options.isEmpty()) {
            return;
        }
        int newIndex = (selectedOption + delta + options.size) % options.size;
        if (newIndex != selectedOption) {
            selectedOption = newIndex;
            keyTimer = KEY_COOLDOWN;
        }
    }

    private void submitOrExit() {
        if (mechanics.isBattleFinished()) {
            returnToWorld();
            return;
        }

        lastResult = mechanics.submitAnswer(selectedOption);
        messageTimer = MESSAGE_DURATION;
        keyTimer = KEY_COOLDOWN;

        if (lastResult.isCorrect()) {
            if (correctSound != null) {
                correctSound.play(0.9f);
            }
            heroAttackTimer = HERO_ATTACK_DISPLAY;
            heroHurtTimer = 0f;
        } else {
            if (incorrectSound != null) {
                incorrectSound.play(0.85f);
            }
            heroAttackTimer = 0f;
            heroHurtTimer = HERO_HURT_DISPLAY;
        }

        if (mechanics.isBattleFinished()) {
            postBattleTimer = POST_BATTLE_DELAY;
        }
    }

    private void drawBackground() {
        if (backgroundTexture != null) {
            batch.draw(backgroundTexture, 0f, 0f, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        } else {
            batch.setColor(0.05f, 0.12f, 0.08f, 1f);
            batch.draw(whitePixel, 0f, 0f, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
            batch.setColor(Color.WHITE);
        }
    }

    private void drawHealthBars() {
        float barWidth = 260f;
        float barHeight = 20f;
        float heroX = 70f;
        float heroY = VIEWPORT_HEIGHT - 60f;
        float enemyX = VIEWPORT_WIDTH - barWidth - 70f;
        float enemyY = heroY;

        drawHealthBar(heroX, heroY, barWidth, barHeight,
                mechanics.getHeroHealth(), mechanics.getHeroMaxHealth(),
                Color.valueOf("00D47F"), mechanics.getHeroName());

        drawHealthBar(enemyX, enemyY, barWidth, barHeight,
                mechanics.getEnemyHealth(), mechanics.getEnemyMaxHealth(),
                Color.valueOf("C266FF"), mechanics.getEnemyName());
    }

    private void drawHealthBar(float x, float y, float width, float height, int current, int max, Color color, String label) {
        if (whitePixel == null) {
            return;
        }

        float ratio = max <= 0 ? 0f : Math.max(0f, Math.min(1f, current / (float) max));
        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(whitePixel, x - 4f, y - 4f, width + 8f, height + 8f);

        batch.setColor(Color.DARK_GRAY);
        batch.draw(whitePixel, x, y, width, height);

        batch.setColor(color);
        batch.draw(whitePixel, x, y, width * ratio, height);

        batch.setColor(Color.WHITE);
        infoFont.draw(batch, label + " " + current + "/" + max, x, y - 6f);
    }

    private void drawCharacters() {
        TextureRegion heroFrame;
        if (heroAttackTimer > 0f && heroAttackFrame != null) {
            heroFrame = heroAttackFrame;
        } else if (heroHurtTimer > 0f && heroHurtFrame != null) {
            heroFrame = heroHurtFrame;
        } else if (!heroIntroFinished && heroIntroAnimation != null) {
            heroFrame = heroIntroAnimation.getKeyFrame(heroAnimTime);
            if (heroIntroAnimation.isAnimationFinished(heroAnimTime) && heroIdleFrame != null) {
                heroFrame = heroIdleFrame;
            }
        } else if (heroIdleFrame != null) {
            heroFrame = heroIdleFrame;
        } else {
            heroFrame = null;
        }

        TextureRegion enemyFrame = enemyIdleFrame;
        if (enemyAnimation != null) {
            float animDuration = Math.max(0f, enemyAnimation.getAnimationDuration());
            if (animDuration > 0f) {
                float cycleDuration = animDuration + ENEMY_LOOP_PAUSE;
                float modTime = enemyAnimTime % cycleDuration;
                float frameTime = Math.min(modTime, animDuration);
                enemyFrame = enemyAnimation.getKeyFrame(frameTime);
            }
        }

        if (heroFrame != null) {
            float targetHeight = visuals.heroTargetHeight * 1.12f;
            float scale = targetHeight / heroFrame.getRegionHeight();
            float width = heroFrame.getRegionWidth() * scale;
            float height = targetHeight;
            float x = 20f;
            float y = 128f + (float) Math.sin(heroAnimTime * 1.4f) * 4f;
            batch.draw(heroFrame, x, y, width, height);
        }

        if (enemyFrame != null) {
            float targetHeight = visuals.enemyTargetHeight;
            float scale = targetHeight / enemyFrame.getRegionHeight();
            float width = enemyFrame.getRegionWidth() * scale;
            float height = targetHeight;
            float x = VIEWPORT_WIDTH - width - 145f;
            float y = 175f + (float) Math.sin(enemyAnimTime * 1.6f) * 6f;
            batch.draw(enemyFrame, x, y, width, height);
        }
    }

    private void drawQuestionPanel() {
        float panelWidth = VIEWPORT_WIDTH * 0.74f;
        float panelHeight = 138f;
        float panelX = (VIEWPORT_WIDTH - panelWidth) / 2f;
        float panelY = VIEWPORT_HEIGHT - panelHeight - 76f;

        if (questionBoxTexture != null) {
            batch.draw(questionBoxTexture, panelX, panelY, panelWidth, panelHeight);
        } else if (whitePixel != null) {
            batch.setColor(0f, 0f, 0f, 0.55f);
            batch.draw(whitePixel, panelX, panelY, panelWidth, panelHeight);
            batch.setColor(Color.WHITE);
        }

        String textToShow = null;
        if (messageTimer > 0f && lastResult != null) {
            textToShow = lastResult.getMessage();
        } else {
            BattleMechanics.BattleQuestion question = mechanics.getCurrentQuestion();
            if (question != null) {
                textToShow = question.getPrompt();
            }
        }

        if (textToShow != null) {
            float textPadding = 40f;
            float wrapWidth = panelWidth - textPadding * 2f;
            glyphLayout.setText(promptFont, textToShow, Color.WHITE, wrapWidth, Align.left, true);
            promptFont.draw(batch, glyphLayout, panelX + textPadding, panelY + panelHeight - 30f);
        }
    }

    private void drawOptionsPanel() {
        float panelWidth = VIEWPORT_WIDTH * 0.78f;
        float panelHeight = 190f;
        float panelX = (VIEWPORT_WIDTH - panelWidth) / 2f;
        float panelY = 24f;

        if (optionsBoxTexture != null) {
            batch.draw(optionsBoxTexture, panelX, panelY, panelWidth, panelHeight);
        } else if (whitePixel != null) {
            batch.setColor(0f, 0f, 0f, 0.6f);
            batch.draw(whitePixel, panelX, panelY, panelWidth, panelHeight);
            batch.setColor(Color.WHITE);
        }

        BattleMechanics.BattleQuestion question = mechanics.getCurrentQuestion();
        if (question == null) {
            optionFont.draw(batch, "No more questions. Press ENTER to continue...", panelX + 42f, panelY + panelHeight - 32f);
            return;
        }

        Array<String> options = question.getOptions();
        float startY = panelY + panelHeight - 48f;

        for (int i = 0; i < options.size; i++) {
            String prefix = ((char) ('A' + i)) + ". ";
            String text = prefix + options.get(i);
            float y = startY - i * OPTION_SPACING;

            if (mechanics.isBattleFinished()) {
                if (question.isCorrect(i)) {
                    optionFont.setColor(Color.valueOf("9EE493"));
                } else {
                    optionFont.setColor(Color.valueOf("FF9EA7"));
                }
            } else if (i == selectedOption) {
                optionFont.setColor(Color.valueOf("FEEA8B"));
            } else {
                optionFont.setColor(Color.WHITE);
            }

            glyphLayout.setText(optionFont, text);
            optionFont.draw(batch, glyphLayout, panelX + 42f, y);
        }

        optionFont.setColor(Color.WHITE);
    }

    private void drawStatusText() {
        String bottomHint;
        if (mechanics.isBattleFinished()) {
            if (mechanics.isHeroVictorious()) {
                bottomHint = "Victory! Press ENTER to return.";
            } else {
                bottomHint = "Defeat... Press ENTER to regroup.";
            }
        } else {
            bottomHint = "Use UP/DOWN to choose an answer. Press ENTER to submit.";
        }
        infoFont.draw(batch, bottomHint, 46f, 18f);
    }

    private void attemptAutoReturn(float delta) {
        if (mechanics.isBattleFinished() && postBattleTimer <= 0f && postBattleTimer != -1f) {
            returnToWorld();
        }
    }

    private void returnToWorld() {
        if (returnSupplier != null) {
            game.setScreen(returnSupplier.get());
        } else {
            game.setScreen(new GreenValleyScreen(game, characterKey));
        }
    }

    private Texture createSolidTexture(Color color) {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        disposableTextures.add(texture);
        return texture;
    }

    private Array<TextureRegion> loadFrameRegions(Array<String> framePaths) {
        Array<TextureRegion> frames = new Array<>();
        if (framePaths == null) {
            return frames;
        }
        for (String path : framePaths) {
            TextureRegion region = loadFrameRegion(path);
            if (region != null) {
                frames.add(region);
            }
        }
        return frames;
    }

    private TextureRegion loadFrameRegion(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        if (!Gdx.files.internal(path).exists()) {
            Gdx.app.debug("FightingBugScreen", "Missing hero frame: " + path);
            return null;
        }
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        disposableTextures.add(texture);
        return new TextureRegion(texture);
    }

    private Texture loadOptionalTexture(String path, boolean logMissing) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        if (!Gdx.files.internal(path).exists()) {
            if (logMissing) {
                Gdx.app.debug("FightingBugScreen", "Optional texture missing: " + path);
            }
            return null;
        }
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        disposableTextures.add(texture);
        return texture;
    }

    private Animation<TextureRegion> buildAnimation(Array<String> framePaths, float frameDuration) {
        if (framePaths == null || framePaths.isEmpty()) {
            return null;
        }
        Array<TextureRegion> frames = new Array<>();
        for (String path : framePaths) {
            if (!Gdx.files.internal(path).exists()) {
                Gdx.app.debug("FightingBugScreen", "Missing animation frame: " + path);
                continue;
            }
            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            disposableTextures.add(texture);
            frames.add(new TextureRegion(texture));
        }
        if (frames.isEmpty()) {
            return null;
        }
        Animation<TextureRegion> animation = new Animation<>(frameDuration <= 0f ? 0.28f : frameDuration, frames);
        animation.setPlayMode(Animation.PlayMode.LOOP);
        return animation;
    }

    private Animation<TextureRegion> buildAnimationFromRegions(Array<TextureRegion> frames,
                                                              float frameDuration,
                                                              Animation.PlayMode playMode) {
        if (frames == null || frames.isEmpty()) {
            return null;
        }
        Animation<TextureRegion> animation = new Animation<>(frameDuration <= 0f ? 0.28f : frameDuration, frames);
        animation.setPlayMode(playMode != null ? playMode : Animation.PlayMode.LOOP);
        return animation;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        promptFont.dispose();
        optionFont.dispose();
        infoFont.dispose();

        for (Texture texture : disposableTextures) {
            if (texture != null) {
                texture.dispose();
            }
        }

        if (correctSound != null) {
            correctSound.dispose();
        }
        if (incorrectSound != null) {
            incorrectSound.dispose();
        }
    }

    /**
     * Visual configuration holder for the battle screen.
     */
    public static final class VisualConfig {
        public String backgroundPath;
        public String questionBoxPath;
        public String answersBoxPath;
        public String correctSfxPath;
        public String incorrectSfxPath;
        public float heroTargetHeight = 280f;
        public float enemyTargetHeight = 180f;
        public float heroFrameDuration = 0.32f;
        public float enemyFrameDuration = 0.28f;
        public final Array<String> heroFramePaths = new Array<>();
        public final Array<String> enemyFramePaths = new Array<>();

        public VisualConfig addHeroFrame(String path) {
            if (path != null) {
                heroFramePaths.add(path);
            }
            return this;
        }

        public VisualConfig addEnemyFrame(String path) {
            if (path != null) {
                enemyFramePaths.add(path);
            }
            return this;
        }

        public static VisualConfig botBugDefaults() {
            VisualConfig config = new VisualConfig();
            config.backgroundPath = "assets/images/fightingscreen/backgroundforfighting.png";
            config.questionBoxPath = "assets/images/fightingscreen/fightingtextbox/questiontextbox.png";
            config.answersBoxPath = "assets/images/fightingscreen/fightingtextbox/textboxfighting.png";
            config.correctSfxPath = "assets/sounds/afterselectingcharacter (1).mp3";
            config.incorrectSfxPath = "assets/sounds/soud/typingletters.mp3";
            config.heroTargetHeight = 340f;
            config.enemyTargetHeight = 300f;
            config.heroFrameDuration = 0.35f;
            config.enemyFrameDuration = 0.32f;

            config.addHeroFrame("assets/images/fightingscreen/fightingframe/1fightingframenmale.png")
                    .addHeroFrame("assets/images/fightingscreen/fightingframe/2fightingframenmale.png")
                    .addHeroFrame("assets/images/fightingscreen/fightingframe/3fightingframemale.png")
                    .addHeroFrame("assets/images/fightingscreen/fightingframe/4fightingframemale.png");

            config.addEnemyFrame("assets/images/fightingscreen/fightingbug/1framebug.png")
                    .addEnemyFrame("assets/images/fightingscreen/fightingbug/2framebug.png");

            return config;
        }

        public static VisualConfig fromDefinition(BugEncounterDefinition.Visual visual) {
            VisualConfig config = new VisualConfig();
            if (visual == null) {
                return config;
            }
            config.backgroundPath = visual.getBackgroundPath();
            config.questionBoxPath = visual.getQuestionBoxPath();
            config.answersBoxPath = visual.getAnswersBoxPath();
            config.correctSfxPath = visual.getCorrectSfxPath();
            config.incorrectSfxPath = visual.getIncorrectSfxPath();
            config.heroTargetHeight = visual.getHeroTargetHeight();
            config.heroFrameDuration = visual.getHeroFrameDuration();
            config.enemyTargetHeight = visual.getEnemyTargetHeight();
            config.enemyFrameDuration = visual.getEnemyFrameDuration();

            if (visual.getHeroFramePaths() != null) {
                for (String path : visual.getHeroFramePaths()) {
                    config.addHeroFrame(path);
                }
            }
            if (visual.getEnemyFramePaths() != null) {
                for (String path : visual.getEnemyFramePaths()) {
                    config.addEnemyFrame(path);
                }
            }
            return config;
        }
    }
}
