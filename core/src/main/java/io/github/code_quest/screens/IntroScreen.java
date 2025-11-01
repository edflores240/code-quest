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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;

public class IntroScreen implements Screen {
    private final Main game;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final Texture overlayPixel;
    private boolean transitionScheduled;
    private float elapsed;
    private float fadeAlpha = 1.0f;
    private boolean isFadingIn = true;
    private Animation<TextureRegion> animation;
    private float stateTime = 0f;
    private boolean showGif = false;
    private TextureRegion currentFrame;

    public IntroScreen(Main game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new FitViewport(800f, 480f, camera);
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.25f);
        font.setColor(Color.WHITE);
        overlayPixel = createOverlayPixel();
        
        // Load animation frames
        try {
            // Create a simple animation with a few frames
            // Replace these with your actual animation frames
            TextureRegion[] frames = new TextureRegion[4]; // Example: 4 frames
            for (int i = 0; i < frames.length; i++) {
                // Replace with your frame loading logic
                // For example: frames[i] = new TextureRegion(new Texture(Gdx.files.internal("animation/frame" + i + ".png")));
                // For now, we'll create colored rectangles as placeholders
                frames[i] = createColorFrame(1f, 1f, 1f, 1f);
            }
            
            // Create animation with 0.1 second frame duration and loop it
            animation = new Animation<>(0.1f, frames);
            animation.setPlayMode(Animation.PlayMode.LOOP);
            showGif = true;
            stateTime = 0f;
            
        } catch (Exception e) {
            Gdx.app.error("IntroScreen", "Error creating animation: " + e.getMessage());
            e.printStackTrace();
            scheduleNextScreen();
        }
    }


    private Texture createOverlayPixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void show() {
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        camera.position.set(viewport.getWorldWidth() * 0.5f, viewport.getWorldHeight() * 0.5f, 0f);
        camera.update();
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        // Handle fade in/out
        if (isFadingIn) {
            fadeAlpha = Math.max(0, fadeAlpha - delta);
            if (fadeAlpha <= 0) {
                isFadingIn = false;
            }
        }

        // Handle skip button press
        if (showGif && !transitionScheduled && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.justTouched())) {
            showGif = false;
            scheduleNextScreen();
        }

        // Update animation state time
        if (showGif) {
            stateTime += delta;
            // Show the animation for 5 seconds
            if (stateTime >= 5.0f) {
                showGif = false;
                scheduleNextScreen();
            }
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (!showGif) {
            float base = 0.2f + 0.1f * MathUtils.sin(elapsed * 1.2f);
            batch.setColor(0.08f + base, 0.08f, 0.16f + base, 1f);
            batch.draw(overlayPixel, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());
            batch.setColor(1f, 1f, 1f, 1f);
        }

        // Draw the animation if it should be shown
        if (showGif && animation != null) {
            // Get current frame of animation
            currentFrame = animation.getKeyFrame(stateTime, true);
            
            if (currentFrame != null) {
                float width = viewport.getWorldWidth();
                float height = viewport.getWorldHeight();
                float aspectRatio = (float)currentFrame.getRegionWidth() / currentFrame.getRegionHeight();
                float drawWidth = width;
                float drawHeight = width / aspectRatio;
                
                if (drawHeight > height) {
                    drawHeight = height;
                    drawWidth = height * aspectRatio;
                }
                
                float x = (width - drawWidth) / 2f;
                float y = (height - drawHeight) / 2f;
                
                batch.draw(currentFrame, x, y, drawWidth, drawHeight);
            }
        }

        float pulse = 0.25f + 0.15f * MathUtils.sin(elapsed * 6f);
        batch.setColor(1f, 0.2f, 0.4f, pulse);
        batch.draw(overlayPixel, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(0.2f, 0.6f, 1f, pulse * 0.6f);
        batch.draw(overlayPixel, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(Color.WHITE);

        float textAlpha = 0.45f + 0.4f * MathUtils.sin(elapsed * 4f);
        font.setColor(1f, 1f, 1f, MathUtils.clamp(textAlpha, 0f, 1f));
        font.draw(batch, "Press Enter or Tap to skip", viewport.getWorldWidth() * 0.25f, 50f);

        batch.end();
    }

    private void scheduleNextScreen() {
        if (transitionScheduled) {
            return;
        }
        transitionScheduled = true;
        game.setScreen(new GameScreen(game));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(viewport.getWorldWidth() * 0.5f, viewport.getWorldHeight() * 0.5f, 0f);
        camera.update();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    private TextureRegion createColorFrame(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(100, 100, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegion(texture);
    }
    
    @Override public void hide() {}

    @Override
    public void dispose() {
        // Clean up resources
        if (animation != null) {
            for (TextureRegion frame : animation.getKeyFrames()) {
                if (frame != null && frame.getTexture() != null) {
                    frame.getTexture().dispose();
                }
            }
        }
        batch.dispose();
        font.dispose();
        overlayPixel.dispose();
    }
}