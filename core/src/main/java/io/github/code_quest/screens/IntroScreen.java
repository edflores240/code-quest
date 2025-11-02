package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.util.Comparator;

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
    private final Array<Texture> storyImages = new Array<>();
    private int currentImageIndex = 0;
    private float displayTime = 0f;
    private static final float TIME_PER_IMAGE = 5.0f; // seconds per image

    public IntroScreen(Main game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new FitViewport(800f, 480f, camera);
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.25f);
        font.setColor(Color.WHITE);
        overlayPixel = createOverlayPixel();
        
        // Load story images from 1.png to 18.png
        try {
            // Try multiple possible base directories
            String[] possibleBaseDirs = {
                "",  // Current working directory
                "assets/",
                "core/assets/",
                "../assets/",
                "codeQuest/assets/"
            };
            
            // List of all image filenames in order
            String[] imageFilenames = {
                "1 (2).png", "2 (2).png", "3 (2).png", "4 (2).png", "5.png",
                "6.png", "7.png", "8.png", "9.png", "10.png", "11.png",
                "12.1.png", "12.2.png", "13.png", "14.png", "15.png",
                "16.png", "17.png", "18.png"
            };
            
            // Load each image in order
            for (String filename : imageFilenames) {
                
                boolean loaded = false;
                
                // Try each possible base directory
                for (String baseDir : possibleBaseDirs) {
                        String path = baseDir + "images/gamepic/" + filename;
                    
                    // Try internal path first
                    FileHandle file = Gdx.files.internal(path);
                    if (!file.exists()) {
                        // Try external path if internal not found
                        file = Gdx.files.external(path);
                    }
                    
                    if (file.exists()) {
                        try {
                            Texture texture = new Texture(file);
                            storyImages.add(texture);
                            Gdx.app.log("IntroScreen", "Loaded: " + path);
                            loaded = true;
                            break;
                        } catch (Exception e) {
                            Gdx.app.error("IntroScreen", "Error loading " + path + ": " + e.getMessage());
                        }
                    }
                }
                
                if (!loaded) {
                    Gdx.app.error("IntroScreen", "Could not load image: " + filename);
                }
            }
            
            if (storyImages.size == 0) {
                Gdx.app.error("IntroScreen", "No story images were loaded");
                scheduleNextScreen();
            }
        } catch (Exception e) {
            Gdx.app.error("IntroScreen", "Error loading story images: " + e.getMessage());
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
        
        // Handle input for advancing the story
        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (currentImageIndex < storyImages.size - 1) {
                // Go to next image
                currentImageIndex++;
                displayTime = 0f;
                isFadingIn = true;
                fadeAlpha = 1.0f;
            } else if (!transitionScheduled) {
                // Last image - go to game
                scheduleNextScreen();
                return;
            }
        }
        
        // Auto-advance after TIME_PER_IMAGE seconds
        if (currentImageIndex < storyImages.size) {
            displayTime += delta;
            if (displayTime >= TIME_PER_IMAGE) {
                if (currentImageIndex < storyImages.size - 1) {
                    currentImageIndex++;
                    displayTime = 0f;
                    isFadingIn = true;
                    fadeAlpha = 1.0f;
                } else if (!transitionScheduled) {
                    scheduleNextScreen();
                    return;
                }
            }
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Draw background
        float base = 0.2f + 0.1f * MathUtils.sin(elapsed * 1.2f);
        batch.setColor(0.08f + base, 0.08f, 0.16f + base, 1f);
        batch.draw(overlayPixel, 0f, 0f, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(1f, 1f, 1f, 1f);

        // Draw the current story image with fade effect
        if (currentImageIndex < storyImages.size) {
            Texture currentImage = storyImages.get(currentImageIndex);
            if (currentImage != null) {
                float width = viewport.getWorldWidth();
                float height = viewport.getWorldHeight();
                float aspectRatio = (float)currentImage.getWidth() / currentImage.getHeight();
                float drawWidth = width;
                float drawHeight = width / aspectRatio;
                
                if (drawHeight > height) {
                    drawHeight = height;
                    drawWidth = height * aspectRatio;
                }
                
                float x = (width - drawWidth) / 2f;
                float y = (height - drawHeight) / 2f;
                
                // Apply fade effect
                batch.setColor(1, 1, 1, 1 - fadeAlpha);
                
                batch.draw(currentImage, x, y, drawWidth, drawHeight);
                batch.setColor(1, 1, 1, 1); // Reset color
            }
        }

        // Draw continue prompt on the last image
        if (currentImageIndex == storyImages.size - 1) {
            String continueText = "Tap or press ENTER to continue";
            float textWidth = font.getLineHeight() * 10; // Approximate width
            float x = (viewport.getWorldWidth() - textWidth) / 2;
            float y = 50; // Position from bottom
            
            // Fading effect for the continue prompt
            float alpha = 0.5f + 0.5f * MathUtils.sin(elapsed * 2);
            font.setColor(1, 1, 1, alpha);
            font.draw(batch, continueText, x, y);
            font.setColor(Color.WHITE);
        }

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
    
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        overlayPixel.dispose();
        if (storyImages != null) {
            for (Texture texture : storyImages) {
                if (texture != null) {
                    texture.dispose();
                }
            }
            storyImages.clear();
        }
    }
}