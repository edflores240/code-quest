package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.github.code_quest.Main;
import io.github.code_quest.save.SaveData;
import io.github.code_quest.save.SaveManager;
import java.util.function.BiConsumer;

public class GreenValleyScreen implements Screen {
    private static final String MALE_SPRITE_BASE = "assets/images/walkingcharactermale/";
    private static final String MALE_FRONT_FRAME = MALE_SPRITE_BASE + "walkingfront (1).png";

    private static float MALE_REFERENCE_HEIGHT = -1f;
    private static float MALE_REFERENCE_WIDTH = -1f;

    private static final float VIEWPORT_WIDTH = 1100f;
    private static final float VIEWPORT_HEIGHT = 640f;
    private static final float MOVE_SPEED = 150f;
    private static final float SPRITE_SCALE = 0.13f;

    private static final String PORTAL_BASE_PATH = "assets/images/portal/portal";
    private static final int PORTAL_FRAME_COUNT = 5;
    private static final float PORTAL_SCALE = 0.3f;
    private static final float PORTAL_INTRO_FRAME_DURATION = 0.12f;
    private static final float PORTAL_LOOP_FRAME_DURATION = 0.18f;
    private static final float PORTAL_MIN_SPAWN_DELAY = 1f;
    private static final float PORTAL_MAX_SPAWN_DELAY = 5f;
    private static final float PORTAL_EXIT_RADIUS_RATIO = 0.35f;
    private static final float FADE_TRANSITION_DURATION = 1.0f;

    private static final String BOTBUG_BASE_PATH = "assets/images/bugbotwalking/";
    private static final String BOTBUG_ENCOUNTER_ID = "botbug_green_valley";
    private static final float BOTBUG_HEIGHT_RATIO = 0.5f;
    private static final float BOTBUG_SPEED = 45f;
    private static final float BOTBUG_PAUSE_DURATION = 1.2f;

    private static final String BUG2_BASE_PATH = "assets/images/bugbotwalking/"; // same for now
    private static final String BUG2_ENCOUNTER_ID = "bug2_green_valley";
    private static final float BUG2_HEIGHT_RATIO = 0.5f;
    private static final float BUG2_SPEED = 40f;
    private static final float BUG2_PAUSE_DURATION = 1.0f;

    private static final String BUG3_BASE_PATH = "assets/images/bugbotwalking/"; // same
    private static final String BUG3_ENCOUNTER_ID = "bug3_green_valley";
    private static final float BUG3_HEIGHT_RATIO = 0.5f;
    private static final float BUG3_SPEED = 50f;
    private static final float BUG3_PAUSE_DURATION = 1.5f;

    private static final float COLLISION_PADDING_RATIO = 0.4f;

    private final Main game;
    private final String characterKey;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final SpriteBatch batch;
    private final Array<Texture> loadedTextures;
    private final Vector2 position;
    private final Texture background;
    private final Music backgroundMusic;

    private float mapWidth;
    private float mapHeight;

    private Animation<TextureRegion> walkFrontAnimation;
    private Animation<TextureRegion> walkBackAnimation;
    private Animation<TextureRegion> walkLeftAnimation;
    private Animation<TextureRegion> walkRightAnimation;

    private TextureRegion facingFront;
    private TextureRegion facingBack;
    private TextureRegion facingLeft;
    private TextureRegion facingRight;

    private float baseFrameWidth;
    private float baseFrameHeight;
    private float renderWidth;
    private float renderHeight;

    private float stateTime;
    private Direction currentDirection;
    private boolean positionInitialized;

    private Animation<TextureRegion> portalIntroAnimation;
    private Animation<TextureRegion> portalLoopAnimation;
    private TextureRegion portalReferenceFrame;
    private final Vector2 portalPosition;
    private boolean portalActive;
    private boolean portalIntroFinished;
    private float portalStateTime;
    private float portalSpawnTimer;
    private float portalRenderWidth;
    private float portalRenderHeight;

    private Texture fadeTexture;
    private boolean fadeOutActive;
    private float fadeOutTime;
    private boolean fadeInActive;
    private float fadeInTime;
    private Runnable pendingTransition;
    private boolean transitionReady;

    private Animation<TextureRegion> botBugWalkFrontAnimation;
    private Animation<TextureRegion> botBugWalkBackAnimation;
    private Animation<TextureRegion> botBugWalkLeftAnimation;
    private Animation<TextureRegion> botBugWalkRightAnimation;

    private TextureRegion botBugFacingFront;
    private TextureRegion botBugFacingBack;
    private TextureRegion botBugFacingLeft;
    private TextureRegion botBugFacingRight;

    private final Vector2 botBugPosition;
    private final Rectangle botBugBounds;
    private final Rectangle playerBounds;
    private Vector2[] botBugWaypoints;
    private int botBugTargetIndex;
    private int botBugNextTargetIndex;
    private float botBugPauseTimer;
    private float botBugStateTime;
    private Direction botBugDirection;
    private float botBugRenderWidth;
    private float botBugRenderHeight;
    private float botBugBaseFrameWidth;
    private float botBugBaseFrameHeight;
    private boolean botBugPositionInitialized;
    private boolean botBugPathInitialized;
    private boolean botBugMoving;
    private boolean triggeredFightingScreen;

    // Bug2 variables
    private Animation<TextureRegion> bug2WalkFrontAnimation;
    private Animation<TextureRegion> bug2WalkBackAnimation;
    private Animation<TextureRegion> bug2WalkLeftAnimation;
    private Animation<TextureRegion> bug2WalkRightAnimation;

    private TextureRegion bug2FacingFront;
    private TextureRegion bug2FacingBack;
    private TextureRegion bug2FacingLeft;
    private TextureRegion bug2FacingRight;

    private final Vector2 bug2Position;
    private final Rectangle bug2Bounds;
    private Vector2[] bug2Waypoints;
    private int bug2TargetIndex;
    private int bug2NextTargetIndex;
    private float bug2PauseTimer;
    private float bug2StateTime;
    private Direction bug2Direction;
    private float bug2RenderWidth;
    private float bug2RenderHeight;
    private float bug2BaseFrameWidth;
    private float bug2BaseFrameHeight;
    private boolean bug2PositionInitialized;
    private boolean bug2PathInitialized;
    private boolean bug2Moving;
    private boolean triggeredBug2Screen;

    // Bug3 variables
    private Animation<TextureRegion> bug3WalkFrontAnimation;
    private Animation<TextureRegion> bug3WalkBackAnimation;
    private Animation<TextureRegion> bug3WalkLeftAnimation;
    private Animation<TextureRegion> bug3WalkRightAnimation;

    private TextureRegion bug3FacingFront;
    private TextureRegion bug3FacingBack;
    private TextureRegion bug3FacingLeft;
    private TextureRegion bug3FacingRight;

    private final Vector2 bug3Position;
    private final Rectangle bug3Bounds;
    private Vector2[] bug3Waypoints;
    private int bug3TargetIndex;
    private int bug3NextTargetIndex;
    private float bug3PauseTimer;
    private float bug3StateTime;
    private Direction bug3Direction;
    private float bug3RenderWidth;
    private float bug3RenderHeight;
    private float bug3BaseFrameWidth;
    private float bug3BaseFrameHeight;
    private boolean bug3PositionInitialized;
    private boolean bug3PathInitialized;
    private boolean bug3Moving;
    private boolean triggeredBug3Screen;

    private int currentLevel;
    private int coins;
    private Array<String> inventory;
    private boolean saveLoaded;
    private float autoSaveTimer;
    private ObjectMap<String, Boolean> defeatedEncounters;

    private enum Direction {
        FRONT,
        BACK,
        LEFT,
        RIGHT
    }

    private void saveGame() {
        Array<String> defeatedIds = new Array<>();
        for (ObjectMap.Entry<String, Boolean> entry : defeatedEncounters) {
            if (entry.value) {
                defeatedIds.add(entry.key);
            }
        }
        SaveData data = new SaveData(position.x, position.y, currentLevel, coins, inventory, characterKey);
        data.setDefeatedEncounterIds(defeatedIds);
        SaveManager.save(data);
    }

    private void loadGame() {
        SaveData data = SaveManager.load();
        if (data == null) {
            return;
        }
        position.set(data.getPositionX(), data.getPositionY());
        positionInitialized = true;
        currentLevel = data.getCurrentLevel();
        coins = data.getCoins();
        inventory = data.getInventory() != null ? new Array<>(data.getInventory()) : new Array<>();
        autoSaveTimer = 0f;

        defeatedEncounters.clear();
        if (data.getDefeatedEncounterIds() != null) {
            for (String id : data.getDefeatedEncounterIds()) {
                defeatedEncounters.put(id, true);
            }
        }

        portalActive = false;
        portalIntroFinished = false;
        portalStateTime = 0f;
        portalSpawnTimer = MathUtils.random(PORTAL_MIN_SPAWN_DELAY, PORTAL_MAX_SPAWN_DELAY);
        fadeOutActive = false;
        fadeOutTime = 0f;
        fadeInActive = true;
        fadeInTime = 0f;
        pendingTransition = null;
        transitionReady = false;
    }

    public GreenValleyScreen(Main game) {
        this(game, "male");
    }

    public GreenValleyScreen(Main game, String characterKey) {
        this.game = game;
        this.characterKey = (characterKey != null) ? characterKey : "male";
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        camera.setToOrtho(false, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        this.batch = new SpriteBatch();
        this.loadedTextures = new Array<>();
        this.position = new Vector2();
        this.playerBounds = new Rectangle();
        this.portalPosition = new Vector2();

        this.stateTime = 0f;
        this.currentDirection = Direction.FRONT;
        this.positionInitialized = false;
        this.portalActive = false;
        this.portalIntroFinished = false;
        this.portalStateTime = 0f;
        this.portalSpawnTimer = MathUtils.random(PORTAL_MIN_SPAWN_DELAY, PORTAL_MAX_SPAWN_DELAY);
        this.portalRenderWidth = 0f;
        this.portalRenderHeight = 0f;
        this.fadeOutActive = false;
        this.fadeOutTime = 0f;
        this.fadeInActive = true;
        this.fadeInTime = 0f;
        this.pendingTransition = null;
        this.transitionReady = false;

        this.botBugPosition = new Vector2();
        this.botBugBounds = new Rectangle();
        this.botBugWaypoints = createDefaultBotBugPath();
        this.botBugTargetIndex = 0;
        this.botBugNextTargetIndex = 1;
        this.botBugPauseTimer = 0f;
        this.botBugStateTime = 0f;
        this.botBugDirection = Direction.FRONT;
        this.botBugRenderWidth = 0f;
        this.botBugRenderHeight = 0f;
        this.botBugBaseFrameWidth = 0f;
        this.botBugBaseFrameHeight = 0f;
        this.triggeredFightingScreen = false;
        this.botBugPositionInitialized = false;
        this.botBugPathInitialized = false;
        this.botBugMoving = false;

        this.bug2Position = new Vector2();
        this.bug2Bounds = new Rectangle();
        this.bug2Waypoints = createDefaultBug2Path();
        this.bug2TargetIndex = 0;
        this.bug2NextTargetIndex = 1;
        this.bug2PauseTimer = 0f;
        this.bug2StateTime = 0f;
        this.bug2Direction = Direction.FRONT;
        this.bug2RenderWidth = 0f;
        this.bug2RenderHeight = 0f;
        this.bug2BaseFrameWidth = 0f;
        this.bug2BaseFrameHeight = 0f;
        this.triggeredBug2Screen = false;
        this.bug2PositionInitialized = false;
        this.bug2PathInitialized = false;
        this.bug2Moving = false;

        this.bug3Position = new Vector2();
        this.bug3Bounds = new Rectangle();
        this.bug3Waypoints = createDefaultBug3Path();
        this.bug3TargetIndex = 0;
        this.bug3NextTargetIndex = 1;
        this.bug3PauseTimer = 0f;
        this.bug3StateTime = 0f;
        this.bug3Direction = Direction.FRONT;
        this.bug3RenderWidth = 0f;
        this.bug3RenderHeight = 0f;
        this.bug3BaseFrameWidth = 0f;
        this.bug3BaseFrameHeight = 0f;
        this.triggeredBug3Screen = false;
        this.bug3PositionInitialized = false;
        this.bug3PathInitialized = false;
        this.bug3Moving = false;

        this.background = loadTexture("assets/images/corruptedgreenvalley.png");
        if (background != null) {
            mapWidth = background.getWidth();
            mapHeight = background.getHeight();
        } else {
            mapWidth = VIEWPORT_WIDTH;
            mapHeight = VIEWPORT_HEIGHT;
        }
        loadAnimations();
        loadPortalAssets();
        createFadeTexture();
        loadBotBugAssets();
        loadBug2Assets();
        loadBug3Assets();

        this.currentLevel = 1;
        this.coins = 0;
        this.inventory = new Array<>();
        this.saveLoaded = false;
        this.autoSaveTimer = 0f;
        this.defeatedEncounters = new ObjectMap<>();

        Music music = null;
        String musicPath = "assets/sounds/firsbiome.mp3";
        if (Gdx.files.internal(musicPath).exists()) {
            music = Gdx.audio.newMusic(Gdx.files.internal(musicPath));
            music.setLooping(true);
            music.setVolume(0.5f);
        } else {
            Gdx.app.error("GreenValleyScreen", "Missing music: " + musicPath);
        }
        this.backgroundMusic = music;
    }

    private void loadAnimations() {
        String key = characterKey.toLowerCase();

        if ("female".equals(key)) {
            // Attempt to load female walking set if available, otherwise fall back to male assets
            String femaleBase = "assets/images/walkingcharacterfemale/";
            facingFront = loadTextureRegion(femaleBase + "facingfront.png");
            facingBack = loadTextureRegion(femaleBase + "facingback.png");

            walkFrontAnimation = buildAnimation(
                    femaleBase + "walkingfront (1).png",
                    femaleBase + "walkingfront (2).png"
            );
            walkBackAnimation = buildAnimation(
                    femaleBase + "walkingback (1).png",
                    femaleBase + "walkingback (2).png"
            );
            walkLeftAnimation = buildAnimation(
                    femaleBase + "walkingleft (1).png",
                    femaleBase + "walkingleft (2).png",
                    femaleBase + "walkingleft (3).png"
            );
            walkRightAnimation = buildAnimation(
                    femaleBase + "walkingright (1).png",
                    femaleBase + "walkingright (2).png",
                    femaleBase + "walkingright (3).png"
            );
        }

        if (walkFrontAnimation == null && walkBackAnimation == null
                && walkLeftAnimation == null && walkRightAnimation == null) {
            // Fallback to male walking set
            facingFront = loadTextureRegion("assets/images/walkingcharactermale/facingfront.png");
            facingBack = loadTextureRegion("assets/images/walkingcharactermale/facingback.png");

            walkFrontAnimation = buildAnimation(
                    "assets/images/walkingcharactermale/walkingfront (1).png",
                    "assets/images/walkingcharactermale/walkingfront (2).png"
            );
            walkBackAnimation = buildAnimation(
                    "assets/images/walkingcharactermale/walkingback (1).png",
                    "assets/images/walkingcharactermale/walkingback (2).png"
            );
            walkLeftAnimation = buildAnimation(
                    "assets/images/walkingcharactermale/walkingleft (1).png",
                    "assets/images/walkingcharactermale/walkingleft (2).png"
            );
            walkRightAnimation = buildAnimation(
                    "assets/images/walkingcharactermale/walkingright (1).png",
                    "assets/images/walkingcharactermale/walkingright (2).png"
            );
        }

        if (walkFrontAnimation != null && facingFront == null) {
            facingFront = walkFrontAnimation.getKeyFrame(0f);
        }
        if (walkBackAnimation != null && facingBack == null) {
            facingBack = walkBackAnimation.getKeyFrame(0f);
        }
        facingLeft = walkLeftAnimation != null ? walkLeftAnimation.getKeyFrame(0f) : facingFront;
        facingRight = walkRightAnimation != null ? walkRightAnimation.getKeyFrame(0f) : facingFront;

        if (!"female".equals(key) && baseFrameHeight > 0f && baseFrameWidth > 0f) {
            MALE_REFERENCE_HEIGHT = baseFrameHeight;
            MALE_REFERENCE_WIDTH = baseFrameWidth;
        }
    }

    private void loadPortalAssets() {
        Array<TextureRegion> frames = new Array<>();
        for (int i = 1; i <= PORTAL_FRAME_COUNT; i++) {
            String path = PORTAL_BASE_PATH + i + ".png";
            if (!Gdx.files.internal(path).exists()) {
                Gdx.app.error("GreenValleyScreen", "Missing portal frame: " + path);
                continue;
            }
            Texture texture = loadTexture(path);
            frames.add(new TextureRegion(texture));
        }

        if (frames.isEmpty()) {
            portalIntroAnimation = null;
            portalLoopAnimation = null;
            portalReferenceFrame = null;
            return;
        }

        portalIntroAnimation = new Animation<>(PORTAL_INTRO_FRAME_DURATION, frames, Animation.PlayMode.NORMAL);

        Array<TextureRegion> loopFrames = new Array<>();
        for (int i = 2; i < frames.size; i++) {
            loopFrames.add(frames.get(i));
        }
        if (!loopFrames.isEmpty()) {
            portalLoopAnimation = new Animation<>(PORTAL_LOOP_FRAME_DURATION, loopFrames, Animation.PlayMode.LOOP);
        } else {
            portalLoopAnimation = null;
        }

        portalReferenceFrame = frames.first();

        if (portalReferenceFrame != null) {
            portalRenderWidth = portalReferenceFrame.getRegionWidth() * PORTAL_SCALE;
            portalRenderHeight = portalReferenceFrame.getRegionHeight() * PORTAL_SCALE;
        }
    }

    private void loadBotBugAssets() {
        botBugFacingFront = loadTextureRegion(BOTBUG_BASE_PATH + "bugfacingfront.png");
        botBugFacingBack = loadTextureRegion(BOTBUG_BASE_PATH + "bugfacingback.png");

        botBugWalkFrontAnimation = buildAnimation(
                BOTBUG_BASE_PATH + "1bugwalkingfront.png",
                BOTBUG_BASE_PATH + "2bugwalkingfront.png"
        );
        botBugWalkBackAnimation = buildAnimation(
                BOTBUG_BASE_PATH + "1bugwalkingback.png",
                BOTBUG_BASE_PATH + "2bugwalkingback.png"
        );
        botBugWalkLeftAnimation = buildAnimation(
                BOTBUG_BASE_PATH + "1bugwalkingleft.png",
                BOTBUG_BASE_PATH + "2bugwalkingleft.png"
        );
        botBugWalkRightAnimation = buildAnimation(
                BOTBUG_BASE_PATH + "1bugwalkingright.png",
                BOTBUG_BASE_PATH + "2bugwalkingright.png"
        );

        if (botBugFacingFront == null && botBugWalkFrontAnimation != null) {
            botBugFacingFront = botBugWalkFrontAnimation.getKeyFrame(0f);
        }
        if (botBugFacingBack == null && botBugWalkBackAnimation != null) {
            botBugFacingBack = botBugWalkBackAnimation.getKeyFrame(0f);
        }
        if (botBugFacingLeft == null && botBugWalkLeftAnimation != null) {
            botBugFacingLeft = botBugWalkLeftAnimation.getKeyFrame(0f);
        }
        if (botBugFacingRight == null && botBugWalkRightAnimation != null) {
            botBugFacingRight = botBugWalkRightAnimation.getKeyFrame(0f);
        }

        TextureRegion baselineRegion = botBugFacingFront != null
                ? botBugFacingFront
                : (botBugWalkFrontAnimation != null ? botBugWalkFrontAnimation.getKeyFrame(0f) : null);

        if (baselineRegion != null) {
            botBugBaseFrameWidth = baselineRegion.getRegionWidth();
            botBugBaseFrameHeight = baselineRegion.getRegionHeight();
        }
    }

    private void loadBug2Assets() {
        bug2FacingFront = loadTextureRegion(BUG2_BASE_PATH + "bugfacingfront.png");
        bug2FacingBack = loadTextureRegion(BUG2_BASE_PATH + "bugfacingback.png");

        bug2WalkFrontAnimation = buildAnimation(
                BUG2_BASE_PATH + "1bugwalkingfront.png",
                BUG2_BASE_PATH + "2bugwalkingfront.png"
        );
        bug2WalkBackAnimation = buildAnimation(
                BUG2_BASE_PATH + "1bugwalkingback.png",
                BUG2_BASE_PATH + "2bugwalkingback.png"
        );
        bug2WalkLeftAnimation = buildAnimation(
                BUG2_BASE_PATH + "1bugwalkingleft.png",
                BUG2_BASE_PATH + "2bugwalkingleft.png"
        );
        bug2WalkRightAnimation = buildAnimation(
                BUG2_BASE_PATH + "1bugwalkingright.png",
                BUG2_BASE_PATH + "2bugwalkingright.png"
        );

        if (bug2FacingFront == null && bug2WalkFrontAnimation != null) {
            bug2FacingFront = bug2WalkFrontAnimation.getKeyFrame(0f);
        }
        if (bug2FacingBack == null && bug2WalkBackAnimation != null) {
            bug2FacingBack = bug2WalkBackAnimation.getKeyFrame(0f);
        }
        if (bug2FacingLeft == null && bug2WalkLeftAnimation != null) {
            bug2FacingLeft = bug2WalkLeftAnimation.getKeyFrame(0f);
        }
        if (bug2FacingRight == null && bug2WalkRightAnimation != null) {
            bug2FacingRight = bug2WalkRightAnimation.getKeyFrame(0f);
        }

        TextureRegion baselineRegion = bug2FacingFront != null
                ? bug2FacingFront
                : (bug2WalkFrontAnimation != null ? bug2WalkFrontAnimation.getKeyFrame(0f) : null);

        if (baselineRegion != null) {
            bug2BaseFrameWidth = baselineRegion.getRegionWidth();
            bug2BaseFrameHeight = baselineRegion.getRegionHeight();
        }
    }

    private void loadBug3Assets() {
        bug3FacingFront = loadTextureRegion(BUG3_BASE_PATH + "bugfacingfront.png");
        bug3FacingBack = loadTextureRegion(BUG3_BASE_PATH + "bugfacingback.png");

        bug3WalkFrontAnimation = buildAnimation(
                BUG3_BASE_PATH + "1bugwalkingfront.png",
                BUG3_BASE_PATH + "2bugwalkingfront.png"
        );
        bug3WalkBackAnimation = buildAnimation(
                BUG3_BASE_PATH + "1bugwalkingback.png",
                BUG3_BASE_PATH + "2bugwalkingback.png"
        );
        bug3WalkLeftAnimation = buildAnimation(
                BUG3_BASE_PATH + "1bugwalkingleft.png",
                BUG3_BASE_PATH + "2bugwalkingleft.png"
        );
        bug3WalkRightAnimation = buildAnimation(
                BUG3_BASE_PATH + "1bugwalkingright.png",
                BUG3_BASE_PATH + "2bugwalkingright.png"
        );

        if (bug3FacingFront == null && bug3WalkFrontAnimation != null) {
            bug3FacingFront = bug3WalkFrontAnimation.getKeyFrame(0f);
        }
        if (bug3FacingBack == null && bug3WalkBackAnimation != null) {
            bug3FacingBack = bug3WalkBackAnimation.getKeyFrame(0f);
        }
        if (bug3FacingLeft == null && bug3WalkLeftAnimation != null) {
            bug3FacingLeft = bug3WalkLeftAnimation.getKeyFrame(0f);
        }
        if (bug3FacingRight == null && bug3WalkRightAnimation != null) {
            bug3FacingRight = bug3WalkRightAnimation.getKeyFrame(0f);
        }

        TextureRegion baselineRegion = bug3FacingFront != null
                ? bug3FacingFront
                : (bug3WalkFrontAnimation != null ? bug3WalkFrontAnimation.getKeyFrame(0f) : null);

        if (baselineRegion != null) {
            bug3BaseFrameWidth = baselineRegion.getRegionWidth();
            bug3BaseFrameHeight = baselineRegion.getRegionHeight();
        }
    }

    private boolean updateBotBug(float delta, float playerWidth, float playerHeight) {
        if (defeatedEncounters.get(BOTBUG_ENCOUNTER_ID, false)) {
            return false;
        }
        if (triggeredFightingScreen) {
            return true;
        }

        if (botBugBaseFrameWidth <= 0f || botBugBaseFrameHeight <= 0f) {
            return false;
        }

        float baselineHeightPx = resolveBaselineHeight();
        float targetHeightPx = baselineHeightPx > 0f
                ? baselineHeightPx * BOTBUG_HEIGHT_RATIO
                : botBugBaseFrameHeight;

        float frameHeight = Math.max(1f, botBugBaseFrameHeight);
        float frameWidth = Math.max(1f, botBugBaseFrameWidth);
        float heightScale = targetHeightPx / frameHeight;
        botBugRenderHeight = targetHeightPx * SPRITE_SCALE;
        botBugRenderWidth = frameWidth * heightScale * SPRITE_SCALE;

        if (!botBugPositionInitialized && botBugRenderWidth > 0f && botBugRenderHeight > 0f) {
            float maxX = Math.max(0f, mapWidth - botBugRenderWidth);
            float maxY = Math.max(0f, mapHeight - botBugRenderHeight);
            float startX = MathUtils.clamp(mapWidth * 0.62f, 0f, maxX);
            float startY = MathUtils.clamp(mapHeight * 0.48f, 0f, maxY);
            botBugPosition.set(startX, startY);
            botBugPositionInitialized = true;
        }

        if (!botBugPathInitialized && botBugPositionInitialized) {
            initializeBotBugPath(botBugRenderWidth, botBugRenderHeight);
        }

        if (botBugWaypoints == null || botBugWaypoints.length < 2) {
            return false;
        }

        if (botBugPauseTimer > 0f) {
            botBugPauseTimer -= delta;
            if (botBugPauseTimer <= 0f) {
                botBugPauseTimer = 0f;
            }
            botBugMoving = false;
        } else {
            Vector2 target = botBugWaypoints[botBugTargetIndex];
            Vector2 temp = new Vector2(target).sub(botBugPosition);
            float distance = temp.len();

            if (distance <= 1.5f) {
                botBugPosition.set(target);
                botBugMoving = false;
                botBugPauseTimer = BOTBUG_PAUSE_DURATION;
                advanceBotBugTarget();
            } else {
                float moveAmount = BOTBUG_SPEED * delta;
                if (moveAmount >= distance) {
                    botBugPosition.set(target);
                    botBugMoving = false;
                    botBugPauseTimer = BOTBUG_PAUSE_DURATION;
                    advanceBotBugTarget();
                } else {
                    float moveX = temp.x;
                    float moveY = temp.y;
                    temp.nor().scl(moveAmount);
                    botBugPosition.add(temp);
                    botBugMoving = true;

                    if (Math.abs(moveX) > Math.abs(moveY)) {
                        botBugDirection = moveX > 0f ? Direction.RIGHT : Direction.LEFT;
                    } else {
                        botBugDirection = moveY > 0f ? Direction.BACK : Direction.FRONT;
                    }
                }
            }
        }

        if (botBugMoving) {
            botBugStateTime += delta;
        } else {
            botBugStateTime = 0f;
        }

        float playerPaddingX = playerWidth * COLLISION_PADDING_RATIO;
        float playerPaddingY = playerHeight * COLLISION_PADDING_RATIO;
        float botPaddingX = botBugRenderWidth * COLLISION_PADDING_RATIO;
        float botPaddingY = botBugRenderHeight * COLLISION_PADDING_RATIO;

        float playerCollisionWidth = Math.max(0f, playerWidth - playerPaddingX);
        float playerCollisionHeight = Math.max(0f, playerHeight - playerPaddingY);
        float botCollisionWidth = Math.max(0f, botBugRenderWidth - botPaddingX);
        float botCollisionHeight = Math.max(0f, botBugRenderHeight - botPaddingY);

        playerBounds.set(
                position.x + (playerWidth - playerCollisionWidth) / 2f,
                position.y + (playerHeight - playerCollisionHeight) / 2f,
                playerCollisionWidth,
                playerCollisionHeight
        );

        botBugBounds.set(
                botBugPosition.x + (botBugRenderWidth - botCollisionWidth) / 2f,
                botBugPosition.y + (botBugRenderHeight - botCollisionHeight) / 2f,
                botCollisionWidth,
                botCollisionHeight
        );

        if (!triggeredFightingScreen && playerBounds.overlaps(botBugBounds)) {
            triggeredFightingScreen = true;
            if (backgroundMusic != null && backgroundMusic.isPlaying()) {
                backgroundMusic.stop();
            }
            final GreenValleyScreen self = this;
            Gdx.app.postRunnable(() -> game.setScreen(new FightingBugScreen(game,
                    characterKey,
                    BOTBUG_ENCOUNTER_ID,
                    () -> self,
                    (id, won) -> {
                        triggeredFightingScreen = false;
                        if (won) {
                            defeatedEncounters.put(id, true);
                        }
                    })));
            return true;
        }

        return false;
    }

    private boolean updateBug2(float delta, float playerWidth, float playerHeight) {
        if (defeatedEncounters.get(BUG2_ENCOUNTER_ID, false)) {
            return false;
        }
        if (triggeredBug2Screen) {
            return true;
        }

        if (bug2BaseFrameWidth <= 0f || bug2BaseFrameHeight <= 0f) {
            return false;
        }

        float baselineHeightPx = resolveBaselineHeight();
        float targetHeightPx = baselineHeightPx > 0f
                ? baselineHeightPx * BUG2_HEIGHT_RATIO
                : bug2BaseFrameHeight;

        float frameHeight = Math.max(1f, bug2BaseFrameHeight);
        float frameWidth = Math.max(1f, bug2BaseFrameWidth);
        float heightScale = targetHeightPx / frameHeight;
        bug2RenderHeight = targetHeightPx * SPRITE_SCALE;
        bug2RenderWidth = frameWidth * heightScale * SPRITE_SCALE;

        if (!bug2PositionInitialized && bug2RenderWidth > 0f && bug2RenderHeight > 0f) {
            float maxX = Math.max(0f, mapWidth - bug2RenderWidth);
            float maxY = Math.max(0f, mapHeight - bug2RenderHeight);
            float startX = MathUtils.clamp(mapWidth * 0.75f, 0f, maxX);
            float startY = MathUtils.clamp(mapHeight * 0.35f, 0f, maxY);
            bug2Position.set(startX, startY);
            bug2PositionInitialized = true;
        }

        if (!bug2PathInitialized && bug2PositionInitialized) {
            initializeBug2Path(bug2RenderWidth, bug2RenderHeight);
        }

        if (bug2Waypoints == null || bug2Waypoints.length < 2) {
            return false;
        }

        if (bug2PauseTimer > 0f) {
            bug2PauseTimer -= delta;
            if (bug2PauseTimer <= 0f) {
                bug2PauseTimer = 0f;
            }
            bug2Moving = false;
        } else {
            Vector2 target = bug2Waypoints[bug2TargetIndex];
            Vector2 temp = new Vector2(target).sub(bug2Position);
            float distance = temp.len();

            if (distance <= 1.5f) {
                bug2Position.set(target);
                bug2Moving = false;
                bug2PauseTimer = BUG2_PAUSE_DURATION;
                advanceBug2Target();
            } else {
                float moveAmount = BUG2_SPEED * delta;
                if (moveAmount >= distance) {
                    bug2Position.set(target);
                    bug2Moving = false;
                    bug2PauseTimer = BUG2_PAUSE_DURATION;
                    advanceBug2Target();
                } else {
                    float moveX = temp.x;
                    float moveY = temp.y;
                    temp.nor().scl(moveAmount);
                    bug2Position.add(temp);
                    bug2Moving = true;

                    if (Math.abs(moveX) > Math.abs(moveY)) {
                        bug2Direction = moveX > 0f ? Direction.RIGHT : Direction.LEFT;
                    } else {
                        bug2Direction = moveY > 0f ? Direction.BACK : Direction.FRONT;
                    }
                }
            }
        }

        if (bug2Moving) {
            bug2StateTime += delta;
        } else {
            bug2StateTime = 0f;
        }

        float playerPaddingX = playerWidth * COLLISION_PADDING_RATIO;
        float playerPaddingY = playerHeight * COLLISION_PADDING_RATIO;
        float botPaddingX = bug2RenderWidth * COLLISION_PADDING_RATIO;
        float botPaddingY = bug2RenderHeight * COLLISION_PADDING_RATIO;

        float playerCollisionWidth = Math.max(0f, playerWidth - playerPaddingX);
        float playerCollisionHeight = Math.max(0f, playerHeight - playerPaddingY);
        float botCollisionWidth = Math.max(0f, bug2RenderWidth - botPaddingX);
        float botCollisionHeight = Math.max(0f, bug2RenderHeight - botPaddingY);

        playerBounds.set(
                position.x + (playerWidth - playerCollisionWidth) / 2f,
                position.y + (playerHeight - playerCollisionHeight) / 2f,
                playerCollisionWidth,
                playerCollisionHeight
        );

        bug2Bounds.set(
                bug2Position.x + (bug2RenderWidth - botCollisionWidth) / 2f,
                bug2Position.y + (bug2RenderHeight - botCollisionHeight) / 2f,
                botCollisionWidth,
                botCollisionHeight
        );

        if (!triggeredBug2Screen && playerBounds.overlaps(bug2Bounds)) {
            triggeredBug2Screen = true;
            if (backgroundMusic != null && backgroundMusic.isPlaying()) {
                backgroundMusic.stop();
            }
            final GreenValleyScreen self = this;
            Gdx.app.postRunnable(() -> game.setScreen(new FightingBugScreen(game,
                    characterKey,
                    BUG2_ENCOUNTER_ID,
                    () -> self,
                    (id, won) -> {
                        triggeredBug2Screen = false;
                        if (won) {
                            defeatedEncounters.put(id, true);
                        }
                    })));
            return true;
        }

        return false;
    }

    private boolean updateBug3(float delta, float playerWidth, float playerHeight) {
        if (defeatedEncounters.get(BUG3_ENCOUNTER_ID, false)) {
            return false;
        }
        if (triggeredBug3Screen) {
            return true;
        }

        if (bug3BaseFrameWidth <= 0f || bug3BaseFrameHeight <= 0f) {
            return false;
        }

        float baselineHeightPx = resolveBaselineHeight();
        float targetHeightPx = baselineHeightPx > 0f
                ? baselineHeightPx * BUG3_HEIGHT_RATIO
                : bug3BaseFrameHeight;

        float frameHeight = Math.max(1f, bug3BaseFrameHeight);
        float frameWidth = Math.max(1f, bug3BaseFrameWidth);
        float heightScale = targetHeightPx / frameHeight;
        bug3RenderHeight = targetHeightPx * SPRITE_SCALE;
        bug3RenderWidth = frameWidth * heightScale * SPRITE_SCALE;

        if (!bug3PositionInitialized && bug3RenderWidth > 0f && bug3RenderHeight > 0f) {
            float maxX = Math.max(0f, mapWidth - bug3RenderWidth);
            float maxY = Math.max(0f, mapHeight - bug3RenderHeight);
            float startX = MathUtils.clamp(mapWidth * 0.45f, 0f, maxX);
            float startY = MathUtils.clamp(mapHeight * 0.65f, 0f, maxY);
            bug3Position.set(startX, startY);
            bug3PositionInitialized = true;
        }

        if (!bug3PathInitialized && bug3PositionInitialized) {
            initializeBug3Path(bug3RenderWidth, bug3RenderHeight);
        }

        if (bug3Waypoints == null || bug3Waypoints.length < 2) {
            return false;
        }

        if (bug3PauseTimer > 0f) {
            bug3PauseTimer -= delta;
            if (bug3PauseTimer <= 0f) {
                bug3PauseTimer = 0f;
            }
            bug3Moving = false;
        } else {
            Vector2 target = bug3Waypoints[bug3TargetIndex];
            Vector2 temp = new Vector2(target).sub(bug3Position);
            float distance = temp.len();

            if (distance <= 1.5f) {
                bug3Position.set(target);
                bug3Moving = false;
                bug3PauseTimer = BUG3_PAUSE_DURATION;
                advanceBug3Target();
            } else {
                float moveAmount = BUG3_SPEED * delta;
                if (moveAmount >= distance) {
                    bug3Position.set(target);
                    bug3Moving = false;
                    bug3PauseTimer = BUG3_PAUSE_DURATION;
                    advanceBug3Target();
                } else {
                    float moveX = temp.x;
                    float moveY = temp.y;
                    temp.nor().scl(moveAmount);
                    bug3Position.add(temp);
                    bug3Moving = true;

                    if (Math.abs(moveX) > Math.abs(moveY)) {
                        bug3Direction = moveX > 0f ? Direction.RIGHT : Direction.LEFT;
                    } else {
                        bug3Direction = moveY > 0f ? Direction.BACK : Direction.FRONT;
                    }
                }
            }
        }

        if (bug3Moving) {
            bug3StateTime += delta;
        } else {
            bug3StateTime = 0f;
        }

        float playerPaddingX = playerWidth * COLLISION_PADDING_RATIO;
        float playerPaddingY = playerHeight * COLLISION_PADDING_RATIO;
        float botPaddingX = bug3RenderWidth * COLLISION_PADDING_RATIO;
        float botPaddingY = bug3RenderHeight * COLLISION_PADDING_RATIO;

        float playerCollisionWidth = Math.max(0f, playerWidth - playerPaddingX);
        float playerCollisionHeight = Math.max(0f, playerHeight - playerPaddingY);
        float botCollisionWidth = Math.max(0f, bug3RenderWidth - botPaddingX);
        float botCollisionHeight = Math.max(0f, bug3RenderHeight - botPaddingY);

        playerBounds.set(
                position.x + (playerWidth - playerCollisionWidth) / 2f,
                position.y + (playerHeight - playerCollisionHeight) / 2f,
                playerCollisionWidth,
                playerCollisionHeight
        );

        bug3Bounds.set(
                bug3Position.x + (bug3RenderWidth - botCollisionWidth) / 2f,
                bug3Position.y + (bug3RenderHeight - botCollisionHeight) / 2f,
                botCollisionWidth,
                botCollisionHeight
        );

        if (!triggeredBug3Screen && playerBounds.overlaps(bug3Bounds)) {
            triggeredBug3Screen = true;
            if (backgroundMusic != null && backgroundMusic.isPlaying()) {
                backgroundMusic.stop();
            }
            final GreenValleyScreen self = this;
            Gdx.app.postRunnable(() -> game.setScreen(new FightingBugScreen(game,
                    characterKey,
                    BUG3_ENCOUNTER_ID,
                    () -> self,
                    (id, won) -> {
                        triggeredBug3Screen = false;
                        if (won) {
                            defeatedEncounters.put(id, true);
                        }
                    })));
            return true;
        }

        return false;
    }

    private void advanceBotBugTarget() {
        botBugTargetIndex = botBugNextTargetIndex;
        botBugNextTargetIndex = (botBugNextTargetIndex + 1) % botBugWaypoints.length;
    }

    private void advanceBug2Target() {
        bug2TargetIndex = bug2NextTargetIndex;
        bug2NextTargetIndex = (bug2NextTargetIndex + 1) % bug2Waypoints.length;
    }

    private void advanceBug3Target() {
        bug3TargetIndex = bug3NextTargetIndex;
        bug3NextTargetIndex = (bug3NextTargetIndex + 1) % bug3Waypoints.length;
    }

    private void initializeBotBugPath(float npcWidth, float npcHeight) {
        if (botBugWaypoints == null || botBugWaypoints.length < 4) {
            botBugWaypoints = createDefaultBotBugPath();
        }

        float maxX = Math.max(0f, mapWidth - npcWidth);
        float maxY = Math.max(0f, mapHeight - npcHeight);
        float baseX = MathUtils.clamp(botBugPosition.x, 0f, maxX);
        float baseY = MathUtils.clamp(botBugPosition.y, 0f, maxY);

        float stepX = Math.min(220f, Math.max(80f, mapWidth * 0.18f));
        float stepY = Math.min(180f, Math.max(70f, mapHeight * 0.16f));

        float x1 = baseX;
        float y1 = baseY;
        float x2 = MathUtils.clamp(baseX + stepX, 0f, maxX);
        float y2 = y1;
        float x3 = x2;
        float y3 = MathUtils.clamp(baseY - stepY, 0f, maxY);
        float x4 = x1;
        float y4 = y3;

        botBugWaypoints[0].set(x1, y1);
        botBugWaypoints[1].set(x2, y2);
        botBugWaypoints[2].set(x3, y3);
        botBugWaypoints[3].set(x4, y4);

        botBugPosition.set(botBugWaypoints[0]);
        botBugTargetIndex = 1;
        botBugNextTargetIndex = 2;
        botBugPathInitialized = true;
    }

    private void initializeBug2Path(float npcWidth, float npcHeight) {
        if (bug2Waypoints == null || bug2Waypoints.length < 4) {
            bug2Waypoints = createDefaultBug2Path();
        }

        float maxX = Math.max(0f, mapWidth - npcWidth);
        float maxY = Math.max(0f, mapHeight - npcHeight);
        float baseX = MathUtils.clamp(bug2Position.x, 0f, maxX);
        float baseY = MathUtils.clamp(bug2Position.y, 0f, maxY);

        float stepX = Math.min(220f, Math.max(80f, mapWidth * 0.18f));
        float stepY = Math.min(180f, Math.max(70f, mapHeight * 0.16f));

        float x1 = baseX;
        float y1 = baseY;
        float x2 = MathUtils.clamp(baseX + stepX, 0f, maxX);
        float y2 = y1;
        float x3 = x2;
        float y3 = MathUtils.clamp(baseY - stepY, 0f, maxY);
        float x4 = x1;
        float y4 = y3;

        bug2Waypoints[0].set(x1, y1);
        bug2Waypoints[1].set(x2, y2);
        bug2Waypoints[2].set(x3, y3);
        bug2Waypoints[3].set(x4, y4);

        bug2Position.set(bug2Waypoints[0]);
        bug2TargetIndex = 1;
        bug2NextTargetIndex = 2;
        bug2PathInitialized = true;
    }

    private void initializeBug3Path(float npcWidth, float npcHeight) {
        if (bug3Waypoints == null || bug3Waypoints.length < 4) {
            bug3Waypoints = createDefaultBug3Path();
        }

        float maxX = Math.max(0f, mapWidth - npcWidth);
        float maxY = Math.max(0f, mapHeight - npcHeight);
        float baseX = MathUtils.clamp(bug3Position.x, 0f, maxX);
        float baseY = MathUtils.clamp(bug3Position.y, 0f, maxY);

        float stepX = Math.min(220f, Math.max(80f, mapWidth * 0.18f));
        float stepY = Math.min(180f, Math.max(70f, mapHeight * 0.16f));

        float x1 = baseX;
        float y1 = baseY;
        float x2 = MathUtils.clamp(baseX + stepX, 0f, maxX);
        float y2 = y1;
        float x3 = x2;
        float y3 = MathUtils.clamp(baseY - stepY, 0f, maxY);
        float x4 = x1;
        float y4 = y3;

        bug3Waypoints[0].set(x1, y1);
        bug3Waypoints[1].set(x2, y2);
        bug3Waypoints[2].set(x3, y3);
        bug3Waypoints[3].set(x4, y4);

        bug3Position.set(bug3Waypoints[0]);
        bug3TargetIndex = 1;
        bug3NextTargetIndex = 2;
        bug3PathInitialized = true;
    }

    private TextureRegion getBotBugFrame() {
        switch (botBugDirection) {
            case BACK:
                if (botBugMoving && botBugWalkBackAnimation != null) {
                    return botBugWalkBackAnimation.getKeyFrame(botBugStateTime, true);
                }
                return botBugFacingBack != null ? botBugFacingBack : botBugFacingFront;
            case LEFT:
                if (botBugMoving && botBugWalkLeftAnimation != null) {
                    return botBugWalkLeftAnimation.getKeyFrame(botBugStateTime, true);
                }
                return botBugFacingLeft != null ? botBugFacingLeft : botBugFacingFront;
            case RIGHT:
                if (botBugMoving && botBugWalkRightAnimation != null) {
                    return botBugWalkRightAnimation.getKeyFrame(botBugStateTime, true);
                }
                return botBugFacingRight != null ? botBugFacingRight : botBugFacingFront;
            case FRONT:
            default:
                if (botBugMoving && botBugWalkFrontAnimation != null) {
                    return botBugWalkFrontAnimation.getKeyFrame(botBugStateTime, true);
                }
                return botBugFacingFront;
        }
    }

    private TextureRegion getBug2Frame() {
        switch (bug2Direction) {
            case BACK:
                if (bug2Moving && bug2WalkBackAnimation != null) {
                    return bug2WalkBackAnimation.getKeyFrame(bug2StateTime, true);
                }
                return bug2FacingBack != null ? bug2FacingBack : bug2FacingFront;
            case LEFT:
                if (bug2Moving && bug2WalkLeftAnimation != null) {
                    return bug2WalkLeftAnimation.getKeyFrame(bug2StateTime, true);
                }
                return bug2FacingLeft != null ? bug2FacingLeft : bug2FacingFront;
            case RIGHT:
                if (bug2Moving && bug2WalkRightAnimation != null) {
                    return bug2WalkRightAnimation.getKeyFrame(bug2StateTime, true);
                }
                return bug2FacingRight != null ? bug2FacingRight : bug2FacingFront;
            case FRONT:
            default:
                if (bug2Moving && bug2WalkFrontAnimation != null) {
                    return bug2WalkFrontAnimation.getKeyFrame(bug2StateTime, true);
                }
                return bug2FacingFront;
        }
    }

    private TextureRegion getBug3Frame() {
        switch (bug3Direction) {
            case BACK:
                if (bug3Moving && bug3WalkBackAnimation != null) {
                    return bug3WalkBackAnimation.getKeyFrame(bug3StateTime, true);
                }
                return bug3FacingBack != null ? bug3FacingBack : bug3FacingFront;
            case LEFT:
                if (bug3Moving && bug3WalkLeftAnimation != null) {
                    return bug3WalkLeftAnimation.getKeyFrame(bug3StateTime, true);
                }
                return bug3FacingLeft != null ? bug3FacingLeft : bug3FacingFront;
            case RIGHT:
                if (bug3Moving && bug3WalkRightAnimation != null) {
                    return bug3WalkRightAnimation.getKeyFrame(bug3StateTime, true);
                }
                return bug3FacingRight != null ? bug3FacingRight : bug3FacingFront;
            case FRONT:
            default:
                if (bug3Moving && bug3WalkFrontAnimation != null) {
                    return bug3WalkFrontAnimation.getKeyFrame(bug3StateTime, true);
                }
                return bug3FacingFront;
        }
    }

    private void drawBotBug() {
        if (defeatedEncounters.get(BOTBUG_ENCOUNTER_ID, false)) {
            return;
        }
        if (botBugRenderWidth <= 0f || botBugRenderHeight <= 0f) {
            return;
        }

        TextureRegion frame = getBotBugFrame();
        if (frame != null) {
            batch.draw(frame, botBugPosition.x, botBugPosition.y, botBugRenderWidth, botBugRenderHeight);
        }
    }

    private void drawBug2() {
        if (defeatedEncounters.get(BUG2_ENCOUNTER_ID, false)) {
            return;
        }
        if (bug2RenderWidth <= 0f || bug2RenderHeight <= 0f) {
            return;
        }

        TextureRegion frame = getBug2Frame();
        if (frame != null) {
            batch.draw(frame, bug2Position.x, bug2Position.y, bug2RenderWidth, bug2RenderHeight);
        }
    }

    private void drawBug3() {
        if (defeatedEncounters.get(BUG3_ENCOUNTER_ID, false)) {
            return;
        }
        if (bug3RenderWidth <= 0f || bug3RenderHeight <= 0f) {
            return;
        }

        TextureRegion frame = getBug3Frame();
        if (frame != null) {
            batch.draw(frame, bug3Position.x, bug3Position.y, bug3RenderWidth, bug3RenderHeight);
        }
    }

    private void updatePortal(float delta) {
        if (portalIntroAnimation == null) {
            return;
        }

        boolean allDefeated = areAllBugsDefeated();
        if (!allDefeated) {
            if (!portalActive) {
                portalSpawnTimer = MathUtils.random(PORTAL_MIN_SPAWN_DELAY, PORTAL_MAX_SPAWN_DELAY);
            }
            return;
        }

        if (portalActive) {
            portalStateTime += delta;
            if (!portalIntroFinished && portalIntroAnimation.isAnimationFinished(portalStateTime)) {
                portalIntroFinished = true;
                portalStateTime = 0f;
            }
        } else {
            portalSpawnTimer -= delta;
            if (portalSpawnTimer <= 0f) {
                spawnPortal();
            }
        }
    }

    private void spawnPortal() {
        if (portalReferenceFrame == null) {
            return;
        }

        portalActive = true;
        portalIntroFinished = false;
        portalStateTime = 0f;

        float frameWidth = portalReferenceFrame.getRegionWidth();
        float frameHeight = portalReferenceFrame.getRegionHeight();
        portalRenderWidth = frameWidth * PORTAL_SCALE;
        portalRenderHeight = frameHeight * PORTAL_SCALE;

        float maxX = Math.max(0f, mapWidth - portalRenderWidth);
        float maxY = Math.max(0f, mapHeight - portalRenderHeight);
        float spawnX = (maxX > 0f) ? MathUtils.random(0f, maxX) : 0f;
        float spawnY = (maxY > 0f) ? MathUtils.random(0f, maxY) : 0f;
        portalPosition.set(spawnX, spawnY);

        portalSpawnTimer = MathUtils.random(PORTAL_MIN_SPAWN_DELAY, PORTAL_MAX_SPAWN_DELAY);
    }

    private void drawPortal() {
        if (!portalActive || portalReferenceFrame == null) {
            return;
        }

        TextureRegion frame;
        if (portalIntroFinished) {
            frame = portalLoopAnimation != null
                    ? portalLoopAnimation.getKeyFrame(portalStateTime, true)
                    : portalReferenceFrame;
        } else {
            frame = portalIntroAnimation.getKeyFrame(portalStateTime, false);
        }

        if (frame == null) {
            return;
        }

        float fadeMultiplier = portalIntroFinished ? 1f : MathUtils.clamp(portalStateTime / (PORTAL_LOOP_FRAME_DURATION * 3f), 0f, 1f);
        float pulse = 1f + 0.08f * MathUtils.sin((portalIntroFinished ? portalStateTime : portalStateTime * 1.6f) * 3.6f);
        float glowScale = 1.28f + 0.18f * MathUtils.sin((portalStateTime + 0.35f) * 2.8f);
        float glowAlpha = (0.3f + 0.2f * MathUtils.sin(portalStateTime * 2.4f)) * fadeMultiplier;

        float baseWidth = portalRenderWidth;
        float baseHeight = portalRenderHeight;
        float drawWidth = baseWidth * pulse;
        float drawHeight = baseHeight * pulse;
        float drawX = portalPosition.x + (baseWidth - drawWidth) / 2f;
        float drawY = portalPosition.y + (baseHeight - drawHeight) / 2f;

        batch.setColor(1f, 0.85f, 0.45f, glowAlpha);
        batch.draw(frame,
                portalPosition.x + (baseWidth - baseWidth * glowScale) / 2f,
                portalPosition.y + (baseHeight - baseHeight * glowScale) / 2f,
                baseWidth * glowScale,
                baseHeight * glowScale);

        batch.setColor(1f, 1f, 1f, 0.85f * fadeMultiplier + 0.15f);
        batch.draw(frame, drawX, drawY, drawWidth, drawHeight);
    }

    private void drawFadeOverlay(float delta) {
        if (fadeTexture == null) {
            return;
        }

        float alpha = 0f;
        if (fadeOutActive) {
            fadeOutTime += delta;
            alpha = MathUtils.clamp(fadeOutTime / FADE_TRANSITION_DURATION, 0f, 1f);
            if (fadeOutTime >= FADE_TRANSITION_DURATION) {
                fadeOutActive = false;
                transitionReady = true;
            }
        } else if (fadeInActive) {
            fadeInTime += delta;
            alpha = MathUtils.clamp(1f - (fadeInTime / FADE_TRANSITION_DURATION), 0f, 1f);
            if (alpha <= 0f) {
                fadeInActive = false;
                alpha = 0f;
            }
        }

        if (alpha <= 0f) {
            return;
        }

        batch.setColor(0f, 0f, 0f, alpha);
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();
        float originX = camera.position.x - width / 2f;
        float originY = camera.position.y - height / 2f;
        batch.draw(fadeTexture, originX, originY, width, height);
        batch.setColor(Color.WHITE);
    }

    private boolean areAllBugsDefeated() {
        return Boolean.TRUE.equals(defeatedEncounters.get(BOTBUG_ENCOUNTER_ID))
                && Boolean.TRUE.equals(defeatedEncounters.get(BUG2_ENCOUNTER_ID))
                && Boolean.TRUE.equals(defeatedEncounters.get(BUG3_ENCOUNTER_ID));
    }

    private void executePendingTransition() {
        if (transitionReady && pendingTransition != null) {
            Runnable action = pendingTransition;
            pendingTransition = null;
            transitionReady = false;
            action.run();
        }
    }

    private TextureRegion loadTextureRegion(String path) {
        if (!Gdx.files.internal(path).exists()) {
            Gdx.app.error("GreenValleyScreen", "Missing texture: " + path);
            return null;
        }
        Texture texture = loadTexture(path);
        return new TextureRegion(texture);
    }

    private Animation<TextureRegion> buildAnimation(String... framePaths) {
        Array<TextureRegion> frames = new Array<>();
        for (String path : framePaths) {
            if (!Gdx.files.internal(path).exists()) {
                Gdx.app.error("GreenValleyScreen", "Missing animation frame: " + path);
                continue;
            }
            Texture texture = loadTexture(path);
            frames.add(new TextureRegion(texture));
        }
        if (frames.isEmpty()) {
            return null;
        }
        Animation<TextureRegion> animation = new Animation<>(0.18f, frames);
        animation.setPlayMode(Animation.PlayMode.LOOP);
        return animation;
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        loadedTextures.add(texture);
        return texture;
    }

    private void createFadeTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        fadeTexture = new Texture(pixmap);
        pixmap.dispose();
        loadedTextures.add(fadeTexture);
    }

    @Override
    public void show() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
        if (!saveLoaded) {
            loadGame();
            saveLoaded = true;
        }
        fadeInActive = true;
        fadeInTime = 0f;
        fadeOutActive = false;
        fadeOutTime = 0f;
        pendingTransition = null;
        transitionReady = false;
    }

    @Override
    public void render(float delta) {
        boolean moving = handleInput(delta);
        if (moving) {
            stateTime += delta;
        } else {
            stateTime = 0f;
        }

        autoSaveTimer += delta;
        if (autoSaveTimer >= 5f) {
            saveGame();
            autoSaveTimer = 0f;
        }

        Gdx.gl.glClearColor(0.05f, 0.2f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        TextureRegion frame = getCurrentFrame(moving);

        float baselineHeightPx = resolveBaselineHeight();
        float targetHeightPx = baselineHeightPx > 0f
                ? baselineHeightPx
                : frame != null ? frame.getRegionHeight() : baseFrameHeight;

        float width;
        float height;

        if (frame != null) {
            float frameHeight = Math.max(1f, frame.getRegionHeight());
            float frameWidth = frame.getRegionWidth();
            float heightScale = targetHeightPx / frameHeight;
            height = targetHeightPx * SPRITE_SCALE;
            width = frameWidth * heightScale * SPRITE_SCALE;
        } else {
            float fallbackWidthPx = resolveBaselineWidth(targetHeightPx);
            width = fallbackWidthPx * SPRITE_SCALE;
            height = targetHeightPx * SPRITE_SCALE;
        }

        renderWidth = width;
        renderHeight = height;

        if (!positionInitialized && width > 0f && height > 0f) {
            position.set(mapWidth / 2f - width / 2f, mapHeight / 2f - height / 2f);
            positionInitialized = true;
        }

        if (width > 0f && height > 0f) {
            float maxX = Math.max(0f, mapWidth - width);
            float maxY = Math.max(0f, mapHeight - height);
            position.x = MathUtils.clamp(position.x, 0f, maxX);
            position.y = MathUtils.clamp(position.y, 0f, maxY);
        }

        if (updateBotBug(delta, width, height)) {
            return;
        }

        if (updateBug2(delta, width, height)) {
            return;
        }

        if (updateBug3(delta, width, height)) {
            return;
        }

        updatePortal(delta);
        handlePortalTeleport(width, height);

        updateCamera(width, height);
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        if (background != null) {
            batch.draw(background, 0f, 0f, mapWidth, mapHeight);
        }
        drawPortal();
        drawBotBug();
        drawBug2();
        drawBug3();
        if (frame != null) {
            batch.draw(frame, position.x, position.y, width, height);
        }
        drawFadeOverlay(delta);
        batch.end();
        executePendingTransition();
    }

    private TextureRegion getCurrentFrame(boolean moving) {
        switch (currentDirection) {
            case BACK:
                if (moving && walkBackAnimation != null) {
                    return walkBackAnimation.getKeyFrame(stateTime, true);
                }
                return facingBack != null ? facingBack : facingFront;
            case LEFT:
                if (moving && walkLeftAnimation != null) {
                    return walkLeftAnimation.getKeyFrame(stateTime, true);
                }
                return facingLeft != null ? facingLeft : facingFront;
            case RIGHT:
                if (moving && walkRightAnimation != null) {
                    return walkRightAnimation.getKeyFrame(stateTime, true);
                }
                return facingRight != null ? facingRight : facingFront;
            case FRONT:
            default:
                if (moving && walkFrontAnimation != null) {
                    return walkFrontAnimation.getKeyFrame(stateTime, true);
                }
                return facingFront;
        }
    }

    private boolean handleInput(float delta) {
        float dx = 0f;
        float dy = 0f;
        Direction newDirection = currentDirection;

        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            saveGame();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            loadGame();
        }

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            dx -= 1f;
            newDirection = Direction.LEFT;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            dx += 1f;
            newDirection = Direction.RIGHT;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            dy += 1f;
            newDirection = Direction.BACK;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            dy -= 1f;
            newDirection = Direction.FRONT;
        }

        boolean moving = dx != 0f || dy != 0f;
        if (moving) {
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0f) {
                dx /= len;
                dy /= len;
            }
            position.x += dx * MOVE_SPEED * delta;
            position.y += dy * MOVE_SPEED * delta;
        }

        currentDirection = newDirection;
        return moving;
    }

    private void handlePortalTeleport(float playerWidth, float playerHeight) {
        if (!portalActive || portalReferenceFrame == null || fadeOutActive || pendingTransition != null) {
            return;
        }

        float playerCenterX = position.x + playerWidth / 2f;
        float playerCenterY = position.y + playerHeight / 2f;
        float portalCenterX = portalPosition.x + portalRenderWidth / 2f;
        float portalCenterY = portalPosition.y + portalRenderHeight / 2f;

        float dx = playerCenterX - portalCenterX;
        float dy = playerCenterY - portalCenterY;
        float distanceSquared = dx * dx + dy * dy;
        float radius = Math.max(portalRenderWidth, portalRenderHeight) * PORTAL_EXIT_RADIUS_RATIO;
        if (distanceSquared <= radius * radius) {
            portalActive = false;
            portalIntroFinished = false;
            portalStateTime = 0f;
            portalSpawnTimer = MathUtils.random(PORTAL_MIN_SPAWN_DELAY, PORTAL_MAX_SPAWN_DELAY);

            fadeOutActive = true;
            fadeOutTime = 0f;
            pendingTransition = () -> game.setScreen(new DesertBiomeScreen(game, characterKey));
            transitionReady = false;
        }
    }

    private void updateCamera(float spriteWidth, float spriteHeight) {
        float halfViewWidth = viewport.getWorldWidth() / 2f;
        float halfViewHeight = viewport.getWorldHeight() / 2f;

        float targetX = position.x + spriteWidth / 2f;
        float targetY = position.y + spriteHeight / 2f;

        float camX;
        if (mapWidth <= viewport.getWorldWidth()) {
            camX = mapWidth / 2f;
        } else {
            camX = MathUtils.clamp(targetX, halfViewWidth, mapWidth - halfViewWidth);
        }

        float camY;
        if (mapHeight <= viewport.getWorldHeight()) {
            camY = mapHeight / 2f;
        } else {
            camY = MathUtils.clamp(targetY, halfViewHeight, mapHeight - halfViewHeight);
        }

        camera.position.set(camX, camY, 0f);
        camera.update();
    }

    private void initializeRenderSize() {
        if (baseFrameWidth <= 0f || baseFrameHeight <= 0f) {
            renderWidth = baseFrameWidth * SPRITE_SCALE;
            renderHeight = baseFrameHeight * SPRITE_SCALE;
            return;
        }

        float baselineHeight = resolveBaselineHeight();
        float targetHeight = (baselineHeight > 0f)
                ? baselineHeight * SPRITE_SCALE
                : baseFrameHeight * SPRITE_SCALE;

        float targetWidth = resolveBaselineWidth(targetHeight);

        float scale = (baseFrameHeight > 0f) ? targetHeight / baseFrameHeight : 1f;
        renderWidth = (baseFrameWidth > 0f) ? baseFrameWidth * scale : targetWidth;
        renderHeight = targetHeight;

        if (renderWidth <= 0f) {
            renderWidth = targetWidth;
        }
    }

    private float resolveBaselineHeight() {
        if (MALE_REFERENCE_HEIGHT > 0f) {
            return MALE_REFERENCE_HEIGHT;
        }

        float[] maleDimensions = fetchTextureDimensions(MALE_FRONT_FRAME);
        if (maleDimensions != null) {
            MALE_REFERENCE_WIDTH = maleDimensions[0];
            MALE_REFERENCE_HEIGHT = maleDimensions[1];
            return MALE_REFERENCE_HEIGHT;
        }

        return baseFrameHeight;
    }

    private float resolveBaselineWidth(float targetHeight) {
        if (MALE_REFERENCE_WIDTH > 0f && MALE_REFERENCE_HEIGHT > 0f) {
            return (MALE_REFERENCE_WIDTH / MALE_REFERENCE_HEIGHT) * targetHeight;
        }

        float[] maleDimensions = fetchTextureDimensions(MALE_FRONT_FRAME);
        if (maleDimensions != null && maleDimensions[1] > 0f) {
            MALE_REFERENCE_WIDTH = maleDimensions[0];
            MALE_REFERENCE_HEIGHT = maleDimensions[1];
            return (MALE_REFERENCE_WIDTH / MALE_REFERENCE_HEIGHT) * targetHeight;
        }

        return baseFrameWidth * (targetHeight / (baseFrameHeight > 0f ? baseFrameHeight : 1f));
    }

    private float[] fetchTextureDimensions(String path) {
        if (!Gdx.files.internal(path).exists()) {
            return null;
        }

        Texture texture = new Texture(Gdx.files.internal(path));
        float width = texture.getWidth();
        float height = texture.getHeight();
        texture.dispose();

        return new float[]{width, height};
    }

    private Vector2[] createDefaultBotBugPath() {
        return new Vector2[]{
                new Vector2(200f, 150f),
                new Vector2(400f, 150f),
                new Vector2(400f, 300f),
                new Vector2(200f, 300f)
        };
    }

    private Vector2[] createDefaultBug2Path() {
        return new Vector2[]{
                new Vector2(600f, 200f),
                new Vector2(800f, 200f),
                new Vector2(800f, 350f),
                new Vector2(600f, 350f)
        };
    }

    private Vector2[] createDefaultBug3Path() {
        return new Vector2[]{
                new Vector2(300f, 400f),
                new Vector2(500f, 400f),
                new Vector2(500f, 550f),
                new Vector2(300f, 550f)
        };
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
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.stop();
        }
        saveGame();
    }

    @Override
    public void dispose() {
        saveGame();
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
        for (Texture texture : loadedTextures) {
            if (texture != null) {
                texture.dispose();
            }
        }
        batch.dispose();
    }
}
