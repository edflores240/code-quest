package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;

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

    private final Main game;
    private final String characterKey;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final SpriteBatch batch;
    private final Array<Texture> loadedTextures;
    private final Vector2 position;

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
    private boolean fadeInActive;
    private float fadeInTime;
    private static final float FADE_IN_DURATION = 1.0f;

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
        this.portalPosition = new Vector2();

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
        this.fadeInActive = true;
        this.fadeInTime = 0f;

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
    }

    @Override
    public void render(float delta) {
        boolean moving = handleInput(delta);
        if (moving) {
            stateTime += delta;
        } else {
            stateTime = 0f;
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
            float maxX = Math.max(0f, mapWidth - width);
            float maxY = Math.max(0f, mapHeight - height);
            position.x = MathUtils.clamp(position.x, 0f, maxX);
            position.y = MathUtils.clamp(position.y, 0f, maxY);
        }

        updatePortal(delta, width, height);

        updateCamera(width, height);
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        if (background != null) {
            batch.draw(background, 0f, 0f, mapWidth, mapHeight);
        }
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

        portalActive = portalReferenceFrame != null;
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
        if (!portalActive || portalReferenceFrame == null) {
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
            portalFading = false;
            portalFadeOutTimer = 0f;
        } else if (portalIntroFinished) {
            portalFading = true;
        }

        if (portalFading) {
            portalFadeOutTimer += delta;
            if (portalFadeOutTimer >= PORTAL_FADE_OUT_DURATION) {
                portalActive = false;
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
        // no-op
    }

    @Override
    public void resume() {
        // no-op
    }

    @Override
    public void hide() {
        // no-op
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
