package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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

import io.github.code_quest.screens.FightingBugScreen;

/**
 * Exploration screen for the desert biome. Uses a tighter viewport than {@link GreenValleyScreen}
 * to deliver a more zoomed-in presentation of the playable character and background.
 */
public class DesertBiomeScreen implements Screen {
    private static final String DESERT_BACKGROUND_PATH = "assets/images/desertbiome.png";
    private static final float VIEWPORT_WIDTH = 700f; // more zoomed in than GreenValleyScreen
    private static final float VIEWPORT_HEIGHT = 400f;
    private static final float MOVE_SPEED = 130f;
    private static final float SPRITE_SCALE = 0.04f; // even smaller character
    private static final String PORTAL_BASE_PATH = "assets/images/portal/portal";
    private static final int PORTAL_FRAME_COUNT = 5;
    private static final float PORTAL_SCALE = 0.3f;
    private static final float PORTAL_INTRO_FRAME_DURATION = 0.12f;
    private static final float PORTAL_LOOP_FRAME_DURATION = 0.18f;
    private static final float PORTAL_EXIT_RADIUS_RATIO = 0.35f;
    private static final float PORTAL_FADE_OUT_DURATION = 1.6f;
    private static final float BUG_COLLISION_PADDING_RATIO = 0.35f;
    private static final float AUTO_SAVE_INTERVAL = 20f;
    private static final String DEFAULT_BUG_TEXTURE = "assets/images/bugbotwalking/bugfacingfront.png";

    private final Main game;
    private final String characterKey;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final SpriteBatch batch;
    private final Array<Texture> loadedTextures;
    private final Vector2 position;
    private final Rectangle playerBounds;
    private final ObjectMap<String, Boolean> defeatedEncounters;
    private final Array<DesertBug> desertBugs;
    private Animation<TextureRegion> desertBugFrontAnimation;
    private Animation<TextureRegion> desertBugBackAnimation;
    private Animation<TextureRegion> desertBugLeftAnimation;
    private Animation<TextureRegion> desertBugRightAnimation;
    private TextureRegion desertBugFacingFront;
    private TextureRegion desertBugFacingBack;
    private TextureRegion desertBugFacingLeft;
    private TextureRegion desertBugFacingRight;

    private Texture background;
    private Texture fadeTexture;
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

    private float stateTime;
    private Direction currentDirection;
    private boolean positionInitialized;

    private float referenceFrameWidth;
    private float referenceFrameHeight;

    private Animation<TextureRegion> portalIntroAnimation;
    private Animation<TextureRegion> portalLoopAnimation;
    private TextureRegion portalReferenceFrame;
    private final Vector2 portalPosition;
    private boolean portalActive;
    private boolean portalIntroFinished;
    private boolean portalFading;
    private float portalStateTime;
    private float portalRenderWidth;
    private float portalRenderHeight;
    private float portalFadeOutTimer;
    private boolean portalUnlocked;
    private boolean fadeInActive;
    private float fadeInTime;
    private static final float FADE_IN_DURATION = 1.0f;
    private boolean saveLoaded;
    private float autoSaveTimer;
    private int currentLevel;
    private int coins;
    private Array<String> inventory;
    private boolean pendingVolcanicTransition;
    private float lastPlayerRenderWidth;
    private float lastPlayerRenderHeight;

    private enum Direction {
        FRONT,
        BACK,
        LEFT,
        RIGHT
    }

    public DesertBiomeScreen(Main game) {
        this(game, "male");
    }

    public DesertBiomeScreen(Main game, String characterKey) {
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
        this.defeatedEncounters = new ObjectMap<>();
        this.desertBugs = new Array<>();

        this.stateTime = 0f;
        this.currentDirection = Direction.FRONT;
        this.positionInitialized = false;
        this.portalActive = false;
        this.portalIntroFinished = false;
        this.portalFading = false;
        this.portalStateTime = 0f;
        this.portalRenderWidth = 0f;
        this.portalRenderHeight = 0f;
        this.portalFadeOutTimer = 0f;
        this.portalUnlocked = false;
        this.fadeInActive = true;
        this.fadeInTime = 0f;
        this.saveLoaded = false;
        this.autoSaveTimer = 0f;
        this.currentLevel = 2;
        this.coins = 0;
        this.inventory = new Array<>();
        this.pendingVolcanicTransition = false;
        this.lastPlayerRenderWidth = 0f;
        this.lastPlayerRenderHeight = 0f;

        background = loadTexture(DESERT_BACKGROUND_PATH);
        if (background != null) {
            mapWidth = background.getWidth();
            mapHeight = background.getHeight();
        } else {
            mapWidth = VIEWPORT_WIDTH;
            mapHeight = VIEWPORT_HEIGHT;
        }

        loadAnimations();
        loadPortalAssets();
        initializePortalPosition();
        createFadeTexture();
        loadDesertBugAnimation();
        initializeBugEncounters();
    }

    private void loadAnimations() {
        String key = characterKey.toLowerCase();

        if ("female".equals(key)) {
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
            String maleBase = "assets/images/walkingcharactermale/";
            facingFront = loadTextureRegion(maleBase + "facingfront.png");
            facingBack = loadTextureRegion(maleBase + "facingback.png");

            walkFrontAnimation = buildAnimation(
                    maleBase + "walkingfront (1).png",
                    maleBase + "walkingfront (2).png"
            );
            walkBackAnimation = buildAnimation(
                    maleBase + "walkingback (1).png",
                    maleBase + "walkingback (2).png"
            );
            walkLeftAnimation = buildAnimation(
                    maleBase + "walkingleft (1).png",
                    maleBase + "walkingleft (2).png"
            );
            walkRightAnimation = buildAnimation(
                    maleBase + "walkingright (1).png",
                    maleBase + "walkingright (2).png"
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

        TextureRegion reference = facingFront != null ? facingFront : facingBack;
        if (reference == null) {
            reference = walkFrontAnimation != null ? walkFrontAnimation.getKeyFrame(0f) : null;
        }
        if (reference != null) {
            referenceFrameWidth = reference.getRegionWidth();
            referenceFrameHeight = reference.getRegionHeight();
        }
    }

    private TextureRegion loadTextureRegion(String path) {
        if (!Gdx.files.internal(path).exists()) {
            return null;
        }
        Texture texture = loadTexture(path);
        return new TextureRegion(texture);
    }

    private Animation<TextureRegion> buildAnimation(String... framePaths) {
        Array<TextureRegion> frames = new Array<>();
        for (String path : framePaths) {
            if (!Gdx.files.internal(path).exists()) {
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
        fadeInActive = true;
        fadeInTime = 0f;
        ensureSaveLoaded();
    }

    @Override
    public void render(float delta) {
        ensureSaveLoaded();
        boolean moving = handleInput(delta);
        if (moving) {
            stateTime += delta;
        } else {
            stateTime = 0f;
        }

        autoSaveTimer += delta;
        if (autoSaveTimer >= AUTO_SAVE_INTERVAL) {
            autoSaveTimer = 0f;
            saveGame();
        }

        Gdx.gl.glClearColor(0.15f, 0.12f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        TextureRegion frame = getCurrentFrame(moving);
        float[] dimensions = resolveRenderDimensions(frame);
        float width = dimensions[0];
        float height = dimensions[1];

        if (!positionInitialized && width > 0f && height > 0f) {
            position.set(mapWidth / 2f - width / 2f, mapHeight / 2f - height / 2f);
            positionInitialized = true;
        }

        if (width > 0f && height > 0f) {
            lastPlayerRenderWidth = width;
            lastPlayerRenderHeight = height;
        }

        if (width > 0f && height > 0f) {
            float maxX = Math.max(0f, mapWidth - width);
            float maxY = Math.max(0f, mapHeight - height);
            position.x = MathUtils.clamp(position.x, 0f, maxX);
            position.y = MathUtils.clamp(position.y, 0f, maxY);
        }

        updateBugEncounters(delta, width, height);
        updatePortal(delta, width, height);

        updateCamera(width, height);
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        if (background != null) {
            batch.draw(background, 0f, 0f, mapWidth, mapHeight);
        }
        drawDesertBugs();
        drawPortal();
        if (frame != null) {
            batch.draw(frame, position.x, position.y, width, height);
        }
        drawFadeOverlay();
        batch.end();
    }

    private void loadPortalAssets() {
        Array<TextureRegion> frames = new Array<>();
        for (int i = 1; i <= PORTAL_FRAME_COUNT; i++) {
            String path = PORTAL_BASE_PATH + i + ".png";
            if (!Gdx.files.internal(path).exists()) {
                continue;
            }
            Texture texture = loadTexture(path);
            frames.add(new TextureRegion(texture));
        }

        if (frames.isEmpty()) {
            portalIntroAnimation = null;
            portalLoopAnimation = null;
            portalReferenceFrame = null;
            portalActive = false;
            portalUnlocked = false;
            return;
        }

        portalIntroAnimation = new Animation<>(PORTAL_INTRO_FRAME_DURATION, frames, Animation.PlayMode.NORMAL);

        Array<TextureRegion> loopFrames = new Array<>();
        for (int i = 2; i < frames.size; i++) {
            loopFrames.add(frames.get(i));
        }
        portalLoopAnimation = loopFrames.isEmpty()
                ? null
                : new Animation<>(PORTAL_LOOP_FRAME_DURATION, loopFrames, Animation.PlayMode.LOOP);

        portalReferenceFrame = frames.first();
        if (portalReferenceFrame != null) {
            portalRenderWidth = portalReferenceFrame.getRegionWidth() * PORTAL_SCALE;
            portalRenderHeight = portalReferenceFrame.getRegionHeight() * PORTAL_SCALE;
        }

        portalUnlocked = false;
        portalActive = false;
        portalIntroFinished = false;
        portalFading = false;
        portalStateTime = 0f;
        portalFadeOutTimer = 0f;
    }

    private void initializePortalPosition() {
        if (portalReferenceFrame == null) {
            return;
        }
        float drawWidth = portalRenderWidth;
        float drawHeight = portalRenderHeight;
        portalPosition.set(
                Math.max(0f, mapWidth / 2f - drawWidth / 2f),
                Math.max(0f, mapHeight / 2f - drawHeight / 2f)
        );
    }

    private void spawnPortalAtRandomPosition() {
        if (portalReferenceFrame == null) {
            return;
        }
        float maxX = Math.max(0f, mapWidth - portalRenderWidth);
        float maxY = Math.max(0f, mapHeight - portalRenderHeight);

        float spawnX = maxX > 0f ? MathUtils.random(0f, maxX) : 0f;
        float spawnY = maxY > 0f ? MathUtils.random(0f, maxY) : 0f;

        portalPosition.set(spawnX, spawnY);
        portalActive = true;
        portalUnlocked = true;
        portalIntroFinished = (portalIntroAnimation == null);
        portalFading = false;
        portalStateTime = 0f;
        portalFadeOutTimer = 0f;
        pendingVolcanicTransition = false;
    }

    private void createFadeTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        fadeTexture = new Texture(pixmap);
        pixmap.dispose();
        loadedTextures.add(fadeTexture);
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

    private float[] resolveRenderDimensions(TextureRegion frame) {
        float width = referenceFrameWidth > 0f ? referenceFrameWidth * SPRITE_SCALE : 32f;
        float height = referenceFrameHeight > 0f ? referenceFrameHeight * SPRITE_SCALE : 48f;
        return new float[]{width, height};
    }

    private boolean handleInput(float delta) {
        float dx = 0f;
        float dy = 0f;
        Direction newDirection = currentDirection;

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

    private void updatePortal(float delta, float playerWidth, float playerHeight) {
        if (portalReferenceFrame == null) {
            portalActive = false;
            return;
        }

        if (!portalUnlocked) {
            portalActive = false;
            portalFading = false;
            portalFadeOutTimer = 0f;
            portalIntroFinished = false;
            portalStateTime = 0f;
            return;
        }

        if (!portalIntroFinished) {
            portalStateTime += delta;
            if (portalIntroAnimation != null && portalIntroAnimation.isAnimationFinished(portalStateTime)) {
                portalIntroFinished = true;
                portalStateTime = 0f;
            }
        } else {
            portalStateTime += delta;
        }

        float playerCenterX = position.x + playerWidth / 2f;
        float playerCenterY = position.y + playerHeight / 2f;
        float portalCenterX = portalPosition.x + portalRenderWidth / 2f;
        float portalCenterY = portalPosition.y + portalRenderHeight / 2f;

        float dx = playerCenterX - portalCenterX;
        float dy = playerCenterY - portalCenterY;
        float distanceSquared = dx * dx + dy * dy;
        float radius = Math.max(portalRenderWidth, portalRenderHeight) * PORTAL_EXIT_RADIUS_RATIO;
        boolean insideRadius = distanceSquared <= radius * radius;

        if (insideRadius) {
            portalActive = true;
            portalFading = false;
            portalFadeOutTimer = 0f;
            if (portalUnlocked && !pendingVolcanicTransition) {
                startVolcanicTransition();
            }
        } else if (portalIntroFinished) {
            portalFading = true;
            if (portalActive) {
                portalFadeOutTimer += delta;
                if (portalFadeOutTimer >= PORTAL_FADE_OUT_DURATION) {
                    portalFadeOutTimer = PORTAL_FADE_OUT_DURATION;
                    portalActive = false;
                }
            }
        }
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
            frame = portalIntroAnimation != null
                    ? portalIntroAnimation.getKeyFrame(portalStateTime, false)
                    : portalReferenceFrame;
        }

        if (frame == null) {
            return;
        }

        float fadeMultiplier = portalFading
                ? MathUtils.clamp(1f - (portalFadeOutTimer / PORTAL_FADE_OUT_DURATION), 0f, 1f)
                : 1f;
        float pulse = 1f + 0.07f * MathUtils.sin((portalIntroFinished ? portalStateTime : portalStateTime * 1.5f) * 3.2f);
        float glowScale = 1.24f + 0.16f * MathUtils.sin((portalStateTime + 0.28f) * 2.6f);
        float glowAlpha = (0.28f + 0.18f * MathUtils.sin(portalStateTime * 2.2f)) * fadeMultiplier;

        float baseWidth = portalRenderWidth;
        float baseHeight = portalRenderHeight;
        float drawWidth = baseWidth * pulse;
        float drawHeight = baseHeight * pulse;
        float drawX = portalPosition.x + (baseWidth - drawWidth) / 2f;
        float drawY = portalPosition.y + (baseHeight - drawHeight) / 2f;

        batch.setColor(1f, 0.82f, 0.42f, glowAlpha);
        batch.draw(frame,
                portalPosition.x + (baseWidth - baseWidth * glowScale) / 2f,
                portalPosition.y + (baseHeight - baseHeight * glowScale) / 2f,
                baseWidth * glowScale,
                baseHeight * glowScale);

        batch.setColor(1f, 1f, 1f, 0.85f * fadeMultiplier + 0.15f);
        batch.draw(frame, drawX, drawY, drawWidth, drawHeight);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    private void drawFadeOverlay() {
        if (!fadeInActive || fadeTexture == null) {
            return;
        }

        fadeInTime += Gdx.graphics.getDeltaTime();
        float alpha = MathUtils.clamp(1f - (fadeInTime / FADE_IN_DURATION), 0f, 1f);
        if (alpha <= 0f) {
            fadeInActive = false;
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

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
        saveGame();
    }

    @Override
    public void resume() {
        // no-op
    }

    @Override
    public void hide() {
        saveGame();
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (background != null) {
            background.dispose();
            background = null;
        }
        for (Texture texture : loadedTextures) {
            if (texture != null) {
                texture.dispose();
            }
        }
        loadedTextures.clear();
    }

    private void initializeBugEncounters() {
        desertBugs.clear();
        Array<BugSpawn> spawns = new Array<>();
        spawns.add(new BugSpawn("bug1_desert", DEFAULT_BUG_TEXTURE, 0.24f, 0.72f, 1.05f, 42f, 28f, 45f, 0.9f));
        spawns.add(new BugSpawn("bug2_desert", DEFAULT_BUG_TEXTURE, 0.65f, 0.68f, 1.0f, 48f, 34f, 40f, 0.8f));
        spawns.add(new BugSpawn("bug3_desert", DEFAULT_BUG_TEXTURE, 0.42f, 0.52f, 1.1f, 52f, 30f, 46f, 0.85f));
        spawns.add(new BugSpawn("bug4_desert", DEFAULT_BUG_TEXTURE, 0.18f, 0.38f, 1.0f, 36f, 26f, 38f, 0.8f));
        spawns.add(new BugSpawn("bug5_desert", DEFAULT_BUG_TEXTURE, 0.74f, 0.34f, 1.08f, 58f, 32f, 44f, 0.9f));

        for (BugSpawn spawn : spawns) {
            TextureRegion referenceFrame = desertBugFacingFront != null ? desertBugFacingFront : loadTextureRegion(spawn.texturePath);
            if (referenceFrame == null) {
                continue;
            }

            DesertBug bug = new DesertBug(spawn.encounterId, referenceFrame, spawn.scaleMultiplier);
            bug.frontAnimation = desertBugFrontAnimation;
            bug.backAnimation = desertBugBackAnimation;
            bug.leftAnimation = desertBugLeftAnimation;
            bug.rightAnimation = desertBugRightAnimation;
            bug.facingFront = desertBugFacingFront;
            bug.facingBack = desertBugFacingBack;
            bug.facingLeft = desertBugFacingLeft != null ? desertBugFacingLeft : referenceFrame;
            bug.facingRight = desertBugFacingRight != null ? desertBugFacingRight : referenceFrame;
            bug.renderWidth = referenceFrame.getRegionWidth() * SPRITE_SCALE * spawn.scaleMultiplier;
            bug.renderHeight = referenceFrame.getRegionHeight() * SPRITE_SCALE * spawn.scaleMultiplier;

            float maxX = Math.max(0f, mapWidth - bug.renderWidth);
            float maxY = Math.max(0f, mapHeight - bug.renderHeight);
            float posX = MathUtils.clamp(mapWidth * spawn.positionRatioX, 0f, maxX);
            float posY = MathUtils.clamp(mapHeight * spawn.positionRatioY, 0f, maxY);
            bug.position.set(posX, posY);
            bug.bounds.set(posX, posY, bug.renderWidth, bug.renderHeight);
            initializeBugWaypoints(bug, spawn, maxX, maxY);
            desertBugs.add(bug);
        }

        applyDefeatedState();
    }

    private void initializeBugWaypoints(DesertBug bug, BugSpawn spawn, float maxX, float maxY) {
        float baseX = bug.position.x;
        float baseY = bug.position.y;

        float horizontalReach = MathUtils.clamp(spawn.patrolWidth, 10f, Math.max(10f, maxX));
        float verticalReach = MathUtils.clamp(spawn.patrolHeight, 10f, Math.max(10f, maxY));

        float x2 = MathUtils.clamp(baseX + horizontalReach, 0f, maxX);
        float y2 = baseY;
        float x3 = x2;
        float y3 = MathUtils.clamp(baseY - verticalReach, 0f, maxY);
        float x4 = baseX;
        float y4 = y3;

        bug.waypoints = new Vector2[]{
                new Vector2(baseX, baseY),
                new Vector2(x2, y2),
                new Vector2(x3, y3),
                new Vector2(x4, y4)
        };
        bug.targetIndex = 1;
        bug.nextTargetIndex = 2;
        bug.pauseTimer = 0f;
        bug.pauseDuration = spawn.pauseSeconds;
        bug.speed = spawn.speed;
    }

    private void drawDesertBugs() {
        for (DesertBug bug : desertBugs) {
            if (bug.defeated || bug.frame == null) {
                continue;
            }
            TextureRegion toDraw;
            if (bug.moving) {
                switch (bug.direction) {
                    case BACK:
                        toDraw = bug.backAnimation != null ? bug.backAnimation.getKeyFrame(bug.stateTime, true) : bug.facingBack;
                        break;
                    case LEFT:
                        toDraw = bug.leftAnimation != null ? bug.leftAnimation.getKeyFrame(bug.stateTime, true) : bug.facingLeft;
                        break;
                    case RIGHT:
                        toDraw = bug.rightAnimation != null ? bug.rightAnimation.getKeyFrame(bug.stateTime, true) : bug.facingRight;
                        break;
                    case FRONT:
                    default:
                        toDraw = bug.frontAnimation != null ? bug.frontAnimation.getKeyFrame(bug.stateTime, true) : bug.facingFront;
                        break;
                }
            } else {
                switch (bug.direction) {
                    case BACK:
                        toDraw = bug.facingBack != null ? bug.facingBack : bug.frame;
                        break;
                    case LEFT:
                        toDraw = bug.facingLeft != null ? bug.facingLeft : bug.frame;
                        break;
                    case RIGHT:
                        toDraw = bug.facingRight != null ? bug.facingRight : bug.frame;
                        break;
                    case FRONT:
                    default:
                        toDraw = bug.facingFront != null ? bug.facingFront : bug.frame;
                        break;
                }
            }
            batch.draw(toDraw, bug.position.x, bug.position.y, bug.renderWidth, bug.renderHeight);
        }
    }

    private void loadDesertBugAnimation() {
        desertBugFrontAnimation = buildBugAnimation(
                "assets/images/bugbotwalking/1bugwalkingfront.png",
                "assets/images/bugbotwalking/2bugwalkingfront.png"
        );
        desertBugBackAnimation = buildBugAnimation(
                "assets/images/bugbotwalking/1bugwalkingback.png",
                "assets/images/bugbotwalking/2bugwalkingback.png"
        );
        desertBugLeftAnimation = buildBugAnimation(
                "assets/images/bugbotwalking/1bugwalkingleft.png",
                "assets/images/bugbotwalking/2bugwalkingleft.png"
        );
        desertBugRightAnimation = buildBugAnimation(
                "assets/images/bugbotwalking/1bugwalkingright.png",
                "assets/images/bugbotwalking/2bugwalkingright.png"
        );

        desertBugFacingFront = firstFrame(desertBugFrontAnimation);
        desertBugFacingBack = firstFrame(desertBugBackAnimation);
        desertBugFacingLeft = firstFrame(desertBugLeftAnimation);
        desertBugFacingRight = firstFrame(desertBugRightAnimation);
        if (desertBugFacingFront == null) {
            desertBugFacingFront = loadTextureRegion(DEFAULT_BUG_TEXTURE);
        }
    }

    private Animation<TextureRegion> buildBugAnimation(String firstPath, String secondPath) {
        Array<TextureRegion> frames = new Array<>();
        addFrameIfExists(frames, firstPath);
        addFrameIfExists(frames, secondPath);
        if (frames.isEmpty()) {
            return null;
        }
        Animation<TextureRegion> animation = new Animation<>(0.24f, frames, Animation.PlayMode.LOOP);
        return animation;
    }

    private void addFrameIfExists(Array<TextureRegion> frames, String path) {
        if (!Gdx.files.internal(path).exists()) {
            return;
        }
        Texture texture = loadTexture(path);
        frames.add(new TextureRegion(texture));
    }

    private TextureRegion firstFrame(Animation<TextureRegion> animation) {
        if (animation == null) {
            return null;
        }
        return animation.getKeyFrame(0f);
    }

    private void updateBugEncounters(float delta, float playerWidth, float playerHeight) {
        if (playerWidth <= 0f || playerHeight <= 0f) {
            return;
        }

        float playerPaddingX = playerWidth * BUG_COLLISION_PADDING_RATIO;
        float playerPaddingY = playerHeight * BUG_COLLISION_PADDING_RATIO;
        float playerCollisionWidth = Math.max(0f, playerWidth - playerPaddingX);
        float playerCollisionHeight = Math.max(0f, playerHeight - playerPaddingY);

        playerBounds.set(
                position.x + (playerWidth - playerCollisionWidth) / 2f,
                position.y + (playerHeight - playerCollisionHeight) / 2f,
                playerCollisionWidth,
                playerCollisionHeight
        );

        for (DesertBug bug : desertBugs) {
            if (bug.defeated || bug.frame == null) {
                continue;
            }

            updateBugMovement(bug, delta);

            float bugPaddingX = bug.renderWidth * BUG_COLLISION_PADDING_RATIO;
            float bugPaddingY = bug.renderHeight * BUG_COLLISION_PADDING_RATIO;
            float bugCollisionWidth = Math.max(0f, bug.renderWidth - bugPaddingX);
            float bugCollisionHeight = Math.max(0f, bug.renderHeight - bugPaddingY);
            bug.bounds.set(
                    bug.position.x + (bug.renderWidth - bugCollisionWidth) / 2f,
                    bug.position.y + (bug.renderHeight - bugCollisionHeight) / 2f,
                    bugCollisionWidth,
                    bugCollisionHeight
            );

            if (!bug.triggered && playerBounds.overlaps(bug.bounds)) {
                startDesertEncounter(bug);
                break;
            }
        }
    }

    private void startDesertEncounter(final DesertBug bug) {
        bug.triggered = true;
        Gdx.app.postRunnable(() -> game.setScreen(new FightingBugScreen(game,
                characterKey,
                bug.encounterId,
                () -> DesertBiomeScreen.this,
                (id, won) -> {
                    bug.triggered = false;
                    if (won) {
                        bug.defeated = true;
                        defeatedEncounters.put(id, true);
                        saveGame();
                        unlockPortalIfReady();
                    }
                })));
    }

    private void saveGame() {
        if (!positionInitialized) {
            return;
        }

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

        defeatedEncounters.clear();
        if (data.getDefeatedEncounterIds() != null) {
            for (String id : data.getDefeatedEncounterIds()) {
                defeatedEncounters.put(id, true);
            }
        }

        applyDefeatedState();
        unlockPortalIfReady();
    }

    private void applyDefeatedState() {
        for (DesertBug bug : desertBugs) {
            bug.defeated = defeatedEncounters.get(bug.encounterId, false);
        }
    }

    private void ensureSaveLoaded() {
        if (!saveLoaded) {
            loadGame();
            saveLoaded = true;
            unlockPortalIfReady();
        }
    }

    private void updateBugMovement(DesertBug bug, float delta) {
        if (bug.waypoints == null || bug.waypoints.length < 2) {
            return;
        }

        if (bug.pauseTimer > 0f) {
            bug.pauseTimer -= delta;
            if (bug.pauseTimer <= 0f) {
                bug.pauseTimer = 0f;
            }
            bug.moving = false;
            return;
        }

        Vector2 target = bug.waypoints[bug.targetIndex];
        Vector2 toTarget = new Vector2(target).sub(bug.position);
        float distance = toTarget.len();
        float moveAmount = bug.speed * delta;

        if (distance <= moveAmount) {
            bug.position.set(target);
            bug.pauseTimer = bug.pauseDuration;
            bug.targetIndex = bug.nextTargetIndex;
            bug.nextTargetIndex = (bug.nextTargetIndex + 1) % bug.waypoints.length;
            bug.moving = false;
            bug.stateTime = 0f;
        } else {
            if (distance > 0f) {
                toTarget.nor().scl(moveAmount);
                bug.position.add(toTarget);
            }
            bug.moving = true;
            bug.stateTime += delta;
            float moveX = toTarget.x;
            float moveY = toTarget.y;
            if (Math.abs(moveX) > Math.abs(moveY)) {
                bug.direction = moveX > 0f ? Direction.RIGHT : Direction.LEFT;
            } else {
                bug.direction = moveY > 0f ? Direction.BACK : Direction.FRONT;
            }
        }
    }

    private void unlockPortalIfReady() {
        if (portalReferenceFrame == null) {
            return;
        }
        if (!portalUnlocked && allDesertBugsDefeated()) {
            spawnPortalAtRandomPosition();
        }
    }

    private boolean allDesertBugsDefeated() {
        for (DesertBug bug : desertBugs) {
            if (!bug.defeated) {
                return false;
            }
        }
        return desertBugs.size > 0;
    }

    private void startVolcanicTransition() {
        pendingVolcanicTransition = true;
        Gdx.app.postRunnable(() -> game.setScreen(new VolcanicBiomeScreen(game, characterKey)));
    }

    private static class BugSpawn {
        final String encounterId;
        final String texturePath;
        final float positionRatioX;
        final float positionRatioY;
        final float scaleMultiplier;
        final float patrolWidth;
        final float patrolHeight;
        final float speed;
        final float pauseSeconds;

        BugSpawn(String encounterId,
                 String texturePath,
                 float positionRatioX,
                 float positionRatioY,
                 float scaleMultiplier,
                 float patrolWidth,
                 float patrolHeight,
                 float speed,
                 float pauseSeconds) {
            this.encounterId = encounterId;
            this.texturePath = texturePath;
            this.positionRatioX = positionRatioX;
            this.positionRatioY = positionRatioY;
            this.scaleMultiplier = scaleMultiplier;
            this.patrolWidth = patrolWidth;
            this.patrolHeight = patrolHeight;
            this.speed = speed;
            this.pauseSeconds = pauseSeconds;
        }
    }

    private static class DesertBug {
        final String encounterId;
        final TextureRegion frame;
        final Vector2 position;
        final Rectangle bounds;
        final float scaleMultiplier;
        float renderWidth;
        float renderHeight;
        boolean triggered;
        boolean defeated;
        Vector2[] waypoints;
        int targetIndex;
        int nextTargetIndex;
        float pauseTimer;
        float pauseDuration;
        float speed;
        Animation<TextureRegion> frontAnimation;
        Animation<TextureRegion> backAnimation;
        Animation<TextureRegion> leftAnimation;
        Animation<TextureRegion> rightAnimation;
        TextureRegion facingFront;
        TextureRegion facingBack;
        TextureRegion facingLeft;
        TextureRegion facingRight;
        float stateTime;
        boolean moving;
        Direction direction;

        DesertBug(String encounterId, TextureRegion frame, float scaleMultiplier) {
            this.encounterId = encounterId;
            this.frame = frame;
            this.scaleMultiplier = scaleMultiplier;
            this.position = new Vector2();
            this.bounds = new Rectangle();
            this.renderWidth = 0f;
            this.renderHeight = 0f;
            this.triggered = false;
            this.defeated = false;
            this.waypoints = null;
            this.targetIndex = 0;
            this.nextTargetIndex = 1;
            this.pauseTimer = 0f;
            this.pauseDuration = 0f;
            this.speed = 0f;
            this.stateTime = 0f;
            this.moving = false;
            this.frontAnimation = null;
            this.backAnimation = null;
            this.leftAnimation = null;
            this.rightAnimation = null;
            this.facingFront = frame;
            this.facingBack = frame;
            this.facingLeft = frame;
            this.facingRight = frame;
            this.direction = Direction.FRONT;
        }
    }
}
