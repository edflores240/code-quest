package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.github.code_quest.Main;
import io.github.code_quest.save.SaveData;
import io.github.code_quest.save.SaveManager;

public class GreenValleyScreen implements Screen {
    private static final String MALE_SPRITE_BASE = "assets/images/walkingcharactermale/";
    private static final String MALE_FRONT_FRAME = MALE_SPRITE_BASE + "walkingfront (1).png";

    private static float MALE_REFERENCE_HEIGHT = -1f;
    private static float MALE_REFERENCE_WIDTH = -1f;

    private static final float VIEWPORT_WIDTH = 1100f;
    private static final float VIEWPORT_HEIGHT = 640f;
    private static final float MOVE_SPEED = 150f;
    private static final float SPRITE_SCALE = 0.13f;

    private static final String BOTBUG_BASE_PATH = "assets/images/bugbotwalking/";
    private static final String BOTBUG_ENCOUNTER_ID = "botbug_green_valley";
    private static final float BOTBUG_HEIGHT_RATIO = 0.5f;
    private static final float BOTBUG_SPEED = 45f;
    private static final float BOTBUG_PAUSE_DURATION = 1.2f;
    private static final float COLLISION_PADDING_RATIO = 0.25f;

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

    private int currentLevel;
    private int coins;
    private Array<String> inventory;
    private boolean saveLoaded;
    private float autoSaveTimer;

    private enum Direction {
        FRONT,
        BACK,
        LEFT,
        RIGHT
    }

    private void saveGame() {
        SaveData data = new SaveData(position.x, position.y, currentLevel, coins, inventory, characterKey);
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

        this.stateTime = 0f;
        this.currentDirection = Direction.FRONT;
        this.positionInitialized = false;

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

        this.background = loadTexture("assets/images/corruptedgreenvalley.png");
        if (background != null) {
            mapWidth = background.getWidth();
            mapHeight = background.getHeight();
        } else {
            mapWidth = VIEWPORT_WIDTH;
            mapHeight = VIEWPORT_HEIGHT;
        }
        loadAnimations();
        loadBotBugAssets();

        this.currentLevel = 1;
        this.coins = 0;
        this.inventory = new Array<>();
        this.saveLoaded = false;
        this.autoSaveTimer = 0f;

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

    private Vector2[] createDefaultBotBugPath() {
        Vector2[] path = new Vector2[4];
        for (int i = 0; i < path.length; i++) {
            path[i] = new Vector2();
        }
        return path;
    }

    private boolean updateBotBug(float delta, float playerWidth, float playerHeight) {
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
            Gdx.app.postRunnable(() -> game.setScreen(new FightingBugScreen(game, characterKey, BOTBUG_ENCOUNTER_ID)));
            return true;
        }

        return false;
    }

    private void advanceBotBugTarget() {
        botBugTargetIndex = botBugNextTargetIndex;
        botBugNextTargetIndex = (botBugNextTargetIndex + 1) % botBugWaypoints.length;
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

    private void drawBotBug() {
        if (botBugRenderWidth <= 0f || botBugRenderHeight <= 0f) {
            return;
        }

        TextureRegion frame = getBotBugFrame();
        if (frame != null) {
            batch.draw(frame, botBugPosition.x, botBugPosition.y, botBugRenderWidth, botBugRenderHeight);
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

    @Override
    public void show() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
        if (!saveLoaded) {
            loadGame();
            saveLoaded = true;
        }
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

        updateCamera(width, height);
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        if (background != null) {
            batch.draw(background, 0f, 0f, mapWidth, mapHeight);
        }
        drawBotBug();
        if (frame != null) {
            batch.draw(frame, position.x, position.y, width, height);
        }
        batch.end();
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
