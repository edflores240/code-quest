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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

import io.github.code_quest.Main;

public class GreenValleyScreen implements Screen {
    private static final String MALE_SPRITE_BASE = "assets/images/walkingcharactermale/";
    private static final String MALE_FRONT_FRAME = MALE_SPRITE_BASE + "walkingfront (1).png";

    private static float MALE_REFERENCE_HEIGHT = -1f;
    private static float MALE_REFERENCE_WIDTH = -1f;

    private static final float VIEWPORT_WIDTH = 1280f;
    private static final float VIEWPORT_HEIGHT = 720f;
    private static final float MOVE_SPEED = 150f;
    private static final float SPRITE_SCALE = 0.16f;

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

    private enum Direction {
        FRONT,
        BACK,
        LEFT,
        RIGHT
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

        this.stateTime = 0f;
        this.currentDirection = Direction.FRONT;
        this.positionInitialized = false;

        this.background = loadTexture("assets/images/greenvalley.png");
        if (background != null) {
            mapWidth = background.getWidth();
            mapHeight = background.getHeight();
        } else {
            mapWidth = VIEWPORT_WIDTH;
            mapHeight = VIEWPORT_HEIGHT;
        }
        loadAnimations();

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
    }

    @Override
    public void render(float delta) {
        boolean moving = handleInput(delta);
        if (moving) {
            stateTime += delta;
        } else {
            stateTime = 0f;
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

        updateCamera(width, height);
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        if (background != null) {
            batch.draw(background, 0f, 0f, mapWidth, mapHeight);
        }
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
    }

    @Override
    public void dispose() {
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
