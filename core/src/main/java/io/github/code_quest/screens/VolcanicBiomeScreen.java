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
 * Exploration screen for the volcanic biome. Shares the same walking animations as the other
 * overworld screens but swaps in the lava background specified by the user.
 */
public class VolcanicBiomeScreen implements Screen {
    private static final String VOLCANIC_BACKGROUND_PATH = "assets/images/lavabiome.png";
    private static final float VIEWPORT_WIDTH = 700f;
    private static final float VIEWPORT_HEIGHT = 400f;
    private static final float MOVE_SPEED = 135f;
    private static final float SPRITE_SCALE = 0.04f;
    private static final float AUTO_SAVE_INTERVAL = 20f;
    private static final float FADE_IN_DURATION = 1.0f;
    private static final float BUG_COLLISION_PADDING_RATIO = 0.35f;
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
    private final Array<VolcanicBug> volcanicBugs;

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
    private boolean fadeInActive;
    private float fadeInTime;

    private float referenceFrameWidth;
    private float referenceFrameHeight;

    private boolean saveLoaded;
    private float autoSaveTimer;
    private int currentLevel;
    private int coins;
    private Array<String> inventory;
    private Animation<TextureRegion> volcanicBugFrontAnimation;
    private Animation<TextureRegion> volcanicBugBackAnimation;
    private Animation<TextureRegion> volcanicBugLeftAnimation;
    private Animation<TextureRegion> volcanicBugRightAnimation;
    private TextureRegion volcanicBugFacingFront;
    private TextureRegion volcanicBugFacingBack;
    private TextureRegion volcanicBugFacingLeft;
    private TextureRegion volcanicBugFacingRight;

    private enum Direction {
        FRONT,
        BACK,
        LEFT,
        RIGHT
    }

    public VolcanicBiomeScreen(Main game) {
        this(game, "male");
    }

    public VolcanicBiomeScreen(Main game, String characterKey) {
        this.game = game;
        this.characterKey = characterKey != null ? characterKey : "male";
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        camera.setToOrtho(false, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);

        this.batch = new SpriteBatch();
        this.loadedTextures = new Array<>();
        this.position = new Vector2();
        this.playerBounds = new Rectangle();
        this.defeatedEncounters = new ObjectMap<>();
        this.volcanicBugs = new Array<>();

        this.stateTime = 0f;
        this.currentDirection = Direction.FRONT;
        this.positionInitialized = false;
        this.fadeInActive = true;
        this.fadeInTime = 0f;
        this.saveLoaded = false;
        this.autoSaveTimer = 0f;
        this.currentLevel = 3; // volcanic biome is unlocked after desert
        this.coins = 0;
        this.inventory = new Array<>();

        background = loadTexture(VOLCANIC_BACKGROUND_PATH);
        if (background != null) {
            mapWidth = background.getWidth();
            mapHeight = background.getHeight();
        } else {
            mapWidth = VIEWPORT_WIDTH;
            mapHeight = VIEWPORT_HEIGHT;
        }

        loadAnimations();
        createFadeTexture();
        loadVolcanicBugAnimations();
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
        if (reference == null && walkFrontAnimation != null) {
            reference = walkFrontAnimation.getKeyFrame(0f);
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

        Gdx.gl.glClearColor(0.12f, 0.05f, 0.02f, 1f);
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
            float maxX = Math.max(0f, mapWidth - width);
            float maxY = Math.max(0f, mapHeight - height);
            position.x = MathUtils.clamp(position.x, 0f, maxX);
            position.y = MathUtils.clamp(position.y, 0f, maxY);
        }

        updateBugEncounters(delta, width, height);
        updateCamera(width, height);
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        if (background != null) {
            batch.draw(background, 0f, 0f, mapWidth, mapHeight);
        }
        drawVolcanicBugs();
        if (frame != null) {
            batch.draw(frame, position.x, position.y, width, height);
        }
        drawFadeOverlay();
        batch.end();
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

    private void saveGame() {
        if (!positionInitialized) {
            return;
        }
        SaveData data = new SaveData(position.x, position.y, currentLevel, coins, inventory, characterKey);
        Array<String> defeatedIds = new Array<>();
        for (ObjectMap.Entry<String, Boolean> entry : defeatedEncounters) {
            if (entry.value) {
                defeatedIds.add(entry.key);
            }
        }
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
        currentLevel = Math.max(data.getCurrentLevel(), 3);
        coins = data.getCoins();
        inventory = data.getInventory() != null ? new Array<>(data.getInventory()) : new Array<>();
        defeatedEncounters.clear();
        if (data.getDefeatedEncounterIds() != null) {
            for (String id : data.getDefeatedEncounterIds()) {
                defeatedEncounters.put(id, true);
            }
        }
        applyDefeatedState();
        autoSaveTimer = 0f;
    }

    private void ensureSaveLoaded() {
        if (!saveLoaded) {
            loadGame();
            saveLoaded = true;
        }
    }

    private void loadVolcanicBugAnimations() {
        volcanicBugFrontAnimation = buildBugAnimation(
                "assets/images/bugbotwalking/1bugwalkingfront.png",
                "assets/images/bugbotwalking/2bugwalkingfront.png"
        );
        volcanicBugBackAnimation = buildBugAnimation(
                "assets/images/bugbotwalking/1bugwalkingback.png",
                "assets/images/bugbotwalking/2bugwalkingback.png"
        );
        volcanicBugLeftAnimation = buildBugAnimation(
                "assets/images/bugbotwalking/1bugwalkingleft.png",
                "assets/images/bugbotwalking/2bugwalkingleft.png"
        );
        volcanicBugRightAnimation = buildBugAnimation(
                "assets/images/bugbotwalking/1bugwalkingright.png",
                "assets/images/bugbotwalking/2bugwalkingright.png"
        );

        volcanicBugFacingFront = firstFrame(volcanicBugFrontAnimation);
        volcanicBugFacingBack = firstFrame(volcanicBugBackAnimation);
        volcanicBugFacingLeft = firstFrame(volcanicBugLeftAnimation);
        volcanicBugFacingRight = firstFrame(volcanicBugRightAnimation);
        if (volcanicBugFacingFront == null) {
            volcanicBugFacingFront = loadTextureRegion(DEFAULT_BUG_TEXTURE);
        }
    }

    private Animation<TextureRegion> buildBugAnimation(String firstPath, String secondPath) {
        Array<TextureRegion> frames = new Array<>();
        addFrameIfExists(frames, firstPath);
        addFrameIfExists(frames, secondPath);
        if (frames.isEmpty()) {
            return null;
        }
        return new Animation<>(0.24f, frames, Animation.PlayMode.LOOP);
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

    private void initializeBugEncounters() {
        volcanicBugs.clear();
        Array<BugSpawn> spawns = new Array<>();
        spawns.add(new BugSpawn("bug1_volcanic", DEFAULT_BUG_TEXTURE, 0.22f, 0.74f, 1.05f, 46f, 34f, 52f, 0.9f));
        spawns.add(new BugSpawn("bug2_volcanic", DEFAULT_BUG_TEXTURE, 0.58f, 0.78f, 1.0f, 54f, 28f, 48f, 0.85f));
        spawns.add(new BugSpawn("bug3_volcanic", DEFAULT_BUG_TEXTURE, 0.78f, 0.56f, 1.08f, 50f, 36f, 55f, 0.8f));
        spawns.add(new BugSpawn("bug4_volcanic", DEFAULT_BUG_TEXTURE, 0.43f, 0.48f, 1.02f, 44f, 32f, 50f, 0.82f));
        spawns.add(new BugSpawn("bug5_volcanic", DEFAULT_BUG_TEXTURE, 0.31f, 0.28f, 1.12f, 58f, 30f, 53f, 0.88f));
        spawns.add(new BugSpawn("bug6_volcanic", DEFAULT_BUG_TEXTURE, 0.67f, 0.24f, 1.06f, 60f, 34f, 56f, 0.9f));

        for (BugSpawn spawn : spawns) {
            TextureRegion referenceFrame = volcanicBugFacingFront != null ? volcanicBugFacingFront : loadTextureRegion(spawn.texturePath);
            if (referenceFrame == null) {
                continue;
            }

            VolcanicBug bug = new VolcanicBug(spawn.encounterId, referenceFrame, spawn.scaleMultiplier);
            bug.frontAnimation = volcanicBugFrontAnimation;
            bug.backAnimation = volcanicBugBackAnimation;
            bug.leftAnimation = volcanicBugLeftAnimation;
            bug.rightAnimation = volcanicBugRightAnimation;
            bug.facingFront = volcanicBugFacingFront;
            bug.facingBack = volcanicBugFacingBack;
            bug.facingLeft = volcanicBugFacingLeft != null ? volcanicBugFacingLeft : referenceFrame;
            bug.facingRight = volcanicBugFacingRight != null ? volcanicBugFacingRight : referenceFrame;
            bug.renderWidth = referenceFrame.getRegionWidth() * SPRITE_SCALE * spawn.scaleMultiplier;
            bug.renderHeight = referenceFrame.getRegionHeight() * SPRITE_SCALE * spawn.scaleMultiplier;

            float maxX = Math.max(0f, mapWidth - bug.renderWidth);
            float maxY = Math.max(0f, mapHeight - bug.renderHeight);
            float posX = MathUtils.clamp(mapWidth * spawn.positionRatioX, 0f, maxX);
            float posY = MathUtils.clamp(mapHeight * spawn.positionRatioY, 0f, maxY);
            bug.position.set(posX, posY);
            bug.bounds.set(posX, posY, bug.renderWidth, bug.renderHeight);
            initializeBugWaypoints(bug, spawn, maxX, maxY);
            volcanicBugs.add(bug);
        }

        applyDefeatedState();
    }

    private void initializeBugWaypoints(VolcanicBug bug, BugSpawn spawn, float maxX, float maxY) {
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

        for (VolcanicBug bug : volcanicBugs) {
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
                startVolcanicEncounter(bug);
                break;
            }
        }
    }

    private void updateBugMovement(VolcanicBug bug, float delta) {
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

    private void drawVolcanicBugs() {
        for (VolcanicBug bug : volcanicBugs) {
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

    private void startVolcanicEncounter(final VolcanicBug bug) {
        bug.triggered = true;
        Gdx.app.postRunnable(() -> game.setScreen(new FightingBugScreen(game,
                characterKey,
                bug.encounterId,
                () -> VolcanicBiomeScreen.this,
                (id, won) -> {
                    bug.triggered = false;
                    if (won) {
                        bug.defeated = true;
                        defeatedEncounters.put(id, true);
                        saveGame();
                    }
                })));
    }

    private void applyDefeatedState() {
        for (VolcanicBug bug : volcanicBugs) {
            bug.defeated = defeatedEncounters.get(bug.encounterId, false);
        }
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

    private static class VolcanicBug {
        final String encounterId;
        final TextureRegion frame;
        final Vector2 position;
        final Rectangle bounds;
        Animation<TextureRegion> frontAnimation;
        Animation<TextureRegion> backAnimation;
        Animation<TextureRegion> leftAnimation;
        Animation<TextureRegion> rightAnimation;
        TextureRegion facingFront;
        TextureRegion facingBack;
        TextureRegion facingLeft;
        TextureRegion facingRight;
        Vector2[] waypoints;
        int targetIndex;
        int nextTargetIndex;
        float pauseTimer;
        float pauseDuration;
        float speed;
        float renderWidth;
        float renderHeight;
        boolean moving;
        boolean triggered;
        boolean defeated;
        float stateTime;
        Direction direction;

        VolcanicBug(String encounterId, TextureRegion frame, float scaleMultiplier) {
            this.encounterId = encounterId;
            this.frame = frame;
            this.position = new Vector2();
            this.bounds = new Rectangle();
            this.direction = Direction.FRONT;
            this.stateTime = 0f;
            this.renderWidth = frame.getRegionWidth() * SPRITE_SCALE * scaleMultiplier;
            this.renderHeight = frame.getRegionHeight() * SPRITE_SCALE * scaleMultiplier;
        }
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
}
