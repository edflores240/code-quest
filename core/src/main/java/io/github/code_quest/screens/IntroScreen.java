package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
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
    private final Array<String> storyTexts = new Array<>();
    private Texture textBoxGreen;
    private Texture textBoxViolet;
    private Texture textBoxBlue;
    private boolean useFallbackTextures = false;
    private final Pixmap greenPixmap = createColoredPixmap(new Color(0.2f, 0.8f, 0.2f, 0.8f));
    private final Pixmap violetPixmap = createColoredPixmap(new Color(0.8f, 0.2f, 0.8f, 0.8f));
    private final Pixmap bluePixmap = createColoredPixmap(new Color(0.2f, 0.2f, 1.0f, 0.8f));
    private int currentImageIndex = 0;
    private float displayTime = 0f;
    private static final float TIME_PER_IMAGE = 5.0f; // seconds per image
    private final Music backgroundMusic;

    // Skip button properties
    private Rectangle skipButtonBounds;
    private float skipButtonPulse = 0f;
    private boolean skipButtonHovered = false;
    private float skipButtonClickEffect = 0f;
    private boolean skipButtonInitialized = false;
    private final ShapeRenderer shapeRenderer;

    public IntroScreen(Main game) {
        this.game = game;
        camera = new OrthographicCamera();
        viewport = new FitViewport(800f, 480f, camera);
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.25f);
        font.setColor(Color.WHITE);
        overlayPixel = createOverlayPixel();
        shapeRenderer = new ShapeRenderer();

        // Initialize skip button will be done in show() method after viewport is set up

        // Try to load text box textures
        try {
            // Try multiple possible base directories
            String[] possibleBaseDirs = {
                "",  // Current working directory
                "assets/",
                "core/assets/",
                "../assets/",
                "codeQuest/assets/"
            };

            boolean loadedGreen = false;
            boolean loadedViolet = false;
            boolean loadedBlue = false;

            for (String baseDir : possibleBaseDirs) {
                if (!loadedGreen) {
                    try {
                        // Try both possible paths
                        String[] possiblePaths = {
                            baseDir + "images/gamepic/textboxgreen.png",
                            baseDir + "images/textboxgreen.png"
                        };

                        for (String path : possiblePaths) {
                            FileHandle file = Gdx.files.internal(path);
                            if (file.exists()) {
                                textBoxGreen = new Texture(file);
                                Gdx.app.log("IntroScreen", "Loaded text box: " + path);
                                loadedGreen = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Gdx.app.error("IntroScreen", "Error loading green text box: " + e.getMessage());
                    }
                }

                if (!loadedViolet) {
                    try {
                        // Try both possible paths
                        String[] possiblePaths = {
                            baseDir + "images/gamepic/textboxviolet.png",
                            baseDir + "images/textboxviolet.png"
                        };

                        for (String path : possiblePaths) {
                            FileHandle file = Gdx.files.internal(path);
                            if (file.exists()) {
                                textBoxViolet = new Texture(file);
                                Gdx.app.log("IntroScreen", "Loaded text box: " + path);
                                loadedViolet = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Gdx.app.error("IntroScreen", "Error loading violet text box: " + e.getMessage());
                    }
                }

                if (!loadedBlue) {
                    try {
                        // Try both possible paths
                        String[] possiblePaths = {
                            baseDir + "images/gamepic/textboxblue.png",
                            baseDir + "images/textboxblue.png"
                        };

                        for (String path : possiblePaths) {
                            FileHandle file = Gdx.files.internal(path);
                            if (file.exists()) {
                                textBoxBlue = new Texture(file);
                                Gdx.app.log("IntroScreen", "Loaded text box: " + path);
                                loadedBlue = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Gdx.app.error("IntroScreen", "Error loading blue text box: " + e.getMessage());
                    }
                }

                if (loadedGreen && loadedViolet && loadedBlue) break;
            }

            // If any texture failed to load, use fallback colored rectangles
            if (!loadedGreen || !loadedViolet || !loadedBlue) {
                useFallbackTextures = true;
                if (textBoxGreen == null) textBoxGreen = new Texture(greenPixmap);
                if (textBoxViolet == null) textBoxViolet = new Texture(violetPixmap);
                if (textBoxBlue == null) textBoxBlue = new Texture(bluePixmap);
                Gdx.app.log("IntroScreen", "Using fallback colored text boxes");
            }
        } catch (Exception e) {
            Gdx.app.error("IntroScreen", "Error loading text box textures: " + e.getMessage());
        }

        // Initialize story texts
        String[] texts = {
            "There was once a peaceful and prosperous village located \nin a valley greener than ever...", // 1
            "Villagers thought that their lives were perfect and could not \nget any better as long as they maintained their \nvillage. Or so they thought...", // 2
            "When unknown meteors came falling from the skies above...", // 3
            "Heading straight into their world...", // 4
            "All the villagers could do is watch in terror...", // 5
            "and hope that everything would be ok. as the unidentified meteor \nfell into the different parts of their world...", // 6
            "Every part of the Dessert...", // 7
            "The Icy mountain...", // 8
            "And finally crashed near the village of the ice...", // 9
            "The Swamp Village...", // 10
            "Just like that... their peaceful world turned into chaos...", // 11
            "The Volcanic Area...", // 12.1
            "As the world fall into chaos and destruction, forcing \neveryone into hiding. but, Except for one.", // 12.2
            "A hero mustered the courage to embarked on a journey to \nfind out the reason behind the chaos and restore the world that everyone knew. The hero swore...", // 13
            "Hero: I will defeat the ruler of these aliens and find \nout what the origin of those meteors, I will return this \nworld to what it was once", // 14
            "As the hero began tracking down the source of this calamity. \nThe hero encountered something...", // 15
            "due to the meteors, code bug like aliens started roaming \ntheir world bringing destruction into their way", // 16
            "as the last hope of humanity, it is up for the hero to face \nthem and cleanse the world from these bug aliens and return the tranquil world.", // 17
            "The Story just beginning..."  // 18
        };
        storyTexts.addAll(texts);

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

        Music music = null;
        String musicPath = "assets/sounds/soud/storytelling.mp3";
        if (Gdx.files.internal(musicPath).exists()) {
            try {
                music = Gdx.audio.newMusic(Gdx.files.internal(musicPath));
                music.setLooping(true);
                music.setVolume(0.5f);
            } catch (Exception e) {
                Gdx.app.error("IntroScreen", "Failed to load music: " + musicPath, e);
            }
        } else {
            Gdx.app.error("IntroScreen", "Missing music: " + musicPath);
        }
        backgroundMusic = music;
    }


    private void setupSkipButton() {
        // Prevent multiple initialization
        if (skipButtonInitialized) return;
        
        // Position skip button in upper left corner
        float buttonWidth = 120;
        float buttonHeight = 40;
        float buttonX = 20; // Upper left position
        float buttonY = viewport.getWorldHeight() - buttonHeight - 20;
        
        skipButtonBounds = new Rectangle(buttonX, buttonY, buttonWidth, buttonHeight);
        skipButtonInitialized = true;
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
        
        // Initialize skip button now that viewport is properly set up
        setupSkipButton();

        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        // Update skip button animations
        // DISABLED - starting fresh with simple approach
        // skipButtonPulse += delta * 3f;
        // if (skipButtonClickEffect > 0) {
        //     skipButtonClickEffect -= delta * 2f;
        // }

        // Check mouse hover for skip button
        // DISABLED - starting fresh with simple approach
        // Vector2 mousePos = getMousePosition();
        // skipButtonHovered = skipButtonBounds != null && skipButtonBounds.contains(mousePos);

        // Handle fade in/out
        if (isFadingIn) {
            fadeAlpha = Math.max(0, fadeAlpha - delta);
            if (fadeAlpha <= 0) {
                isFadingIn = false;
            }
        }

        // Handle input for advancing the story
        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            // Check if skip button was clicked
            // DISABLED - using simple ESC key instead
            // if (skipButtonHovered) {
            //     skipButtonClickEffect = 1f;
            //     scheduleNextScreen();
            //     return;
            // }
            
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

        // Simple TAB key to skip
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            scheduleNextScreen();
            return;
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
                float screenWidth = viewport.getWorldWidth();
                float screenHeight = viewport.getWorldHeight();
                float imageAspect = (float)currentImage.getWidth() / currentImage.getHeight();
                float screenAspect = screenWidth / screenHeight;

                float drawWidth, drawHeight;
                float x, y;

                // Calculate scale to fill screen while maintaining aspect ratio
                float scaleX = screenWidth / (float)currentImage.getWidth();
                float scaleY = screenHeight / (float)currentImage.getHeight();
                float scale = Math.max(scaleX, scaleY);

                // Calculate dimensions to fill screen
                drawWidth = currentImage.getWidth() * scale;
                drawHeight = currentImage.getHeight() * scale;

                // Center the image
                x = (screenWidth - drawWidth) / 2f;
                y = (screenHeight - drawHeight) / 2f;

                // Apply fade effect
                batch.setColor(1, 1, 1, 1 - fadeAlpha);
                batch.draw(currentImage, x, y, drawWidth, drawHeight);
                batch.setColor(1, 1, 1, 1); // Reset color

                // Debug log for text box rendering
                Gdx.app.log("IntroScreen", "Rendering image " + (currentImageIndex + 1) +
                    ", text available: " + (currentImageIndex < storyTexts.size && storyTexts.get(currentImageIndex) != null) +
                    ", fadeAlpha: " + fadeAlpha);

                // Draw text box if we have text for this image
                if (currentImageIndex < storyTexts.size && storyTexts.get(currentImageIndex) != null) {
                    // Choose text box based on image index
                    Texture textBoxTexture = null;
                    String boxType = "";

                    if (currentImageIndex < 2) { // 1 & 2 - green
                        textBoxTexture = textBoxGreen;
                        boxType = "green";
                    } else if (currentImageIndex == 13 || currentImageIndex == 12 ) { // 12.2 - blue
                        textBoxTexture = textBoxBlue;
                        boxType = "blue";
                    } else if (currentImageIndex < 19) { // 3-12.1, 13-18 - violet
                        textBoxTexture = textBoxViolet;
                        boxType = "violet";
                    } else { // 19+ - blue
                        textBoxTexture = textBoxBlue;
                        boxType = "blue";
                    }

                    Gdx.app.log("IntroScreen", "Using " + boxType + " text box, texture: " +
                        (textBoxTexture != null ? "loaded" : "null"));

                    if (textBoxTexture != null) {
                    // Draw a low text box strip at the very bottom
                    float minHeight = 250f; // Increased height for better text display
                    float textBoxHeight = minHeight; // Fixed low height
                    float textBoxWidth = screenWidth * 0.8f; // 90% of screen width
                    float textBoxX = (screenWidth - textBoxWidth) / 2f;
                    // Position at the very bottom of the screen
                    float textBoxY = -60f; // Slightly higher from the bottom edge

                    Gdx.app.log("IntroScreen", String.format("Drawing text box at (%.1f, %.1f) size (%.1f x %.1f)",
                        textBoxX, textBoxY, textBoxWidth, textBoxHeight));

                    // Draw the text box with 70% opacity
                    batch.setColor(1, 1, 1, 0.75f * (1 - fadeAlpha));
                    // Draw the text box at bottom
                    batch.draw(textBoxTexture, textBoxX, textBoxY, textBoxWidth, textBoxHeight);
                    batch.setColor(1, 1, 1, 1 - fadeAlpha); // Reset color for other elements

                    // Draw the text
                    String text = storyTexts.get(currentImageIndex);
                    if (text != null) {
                        float textX = textBoxX + 70f; // Left padding
                        float textY = textBoxY + textBoxHeight - 90f; // Slightly less top padding for low strip

                        // Set font color based on text box type for better contrast


                        // Draw text with word wrap and slightly increased line height
                        // Using the correct draw method signature with explicit float parameters
                        font.draw(batch,
                                text,
                                textX,
                                textY,
                                (int)(textBoxWidth - 80f),  // Text area width with padding
                                Align.left,
                                true);
                        font.setColor(Color.WHITE); // Reset font color
                    }
                }
                }
            }
        }

        // Draw continue prompt on the last image
       /* if (currentImageIndex == storyImages.size - 1) {
            String continueText = "Tap or press ENTER to continue";
            float textWidth = font.getLineHeight() * 10f; // Approximate width
            float x = (viewport.getWorldWidth() - textWidth) / 2;
            float y = 50f; // Position from bottom

            // Fading effect for the continue prompt
            float alpha = 0.5f + 0.5f * MathUtils.sin(elapsed * 2);
            font.setColor(1, 1, 1, alpha);
            font.draw(batch, continueText, x, y);
            font.setColor(Color.WHITE);
        }

      /*  float textAlpha = 0.45f + 0.4f * MathUtils.sin(elapsed * 4f);
        font.setColor(1f, 1f, 1f, MathUtils.clamp(textAlpha, 0f, 1f));
        font.draw(batch, "Press Enter or Tap to skip", viewport.getWorldWidth() * 0.25f, 50f); */

        // Draw skip button
        // Simple text-based skip instead of complex button
        font.setColor(1f, 1f, 1f, 0.7f);
        font.draw(batch, "Press TAB to skip", 20, viewport.getWorldHeight() - 20);

        batch.end();
    }

    private Vector2 getMousePosition() {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.input.getY();
        Vector2 worldCoords = viewport.unproject(new Vector2(mouseX, mouseY));
        return worldCoords;
    }

    private void drawSkipButton() {
        // Skip drawing if button bounds or shapeRenderer haven't been initialized yet
        if (skipButtonBounds == null || shapeRenderer == null) return;
        
        // Draw simple button background
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Main button background
        shapeRenderer.setColor(0.9f, 0.3f, 0.1f, 0.8f);
        shapeRenderer.rect(skipButtonBounds.x, skipButtonBounds.y, skipButtonBounds.width, skipButtonBounds.height);
        
        // Button border
        shapeRenderer.setColor(1f, 0.7f, 0.2f, 1f);
        shapeRenderer.rect(skipButtonBounds.x, skipButtonBounds.y, skipButtonBounds.width, 2);
        shapeRenderer.rect(skipButtonBounds.x, skipButtonBounds.y + skipButtonBounds.height - 2, skipButtonBounds.width, 2);
        shapeRenderer.rect(skipButtonBounds.x, skipButtonBounds.y, 2, skipButtonBounds.height);
        shapeRenderer.rect(skipButtonBounds.x + skipButtonBounds.width - 2, skipButtonBounds.y, 2, skipButtonBounds.height);
        
        shapeRenderer.end();
        
        // Draw button text
        batch.begin();
        font.getData().setScale(1.0f);
        font.setColor(1f, 1f, 1f, 1f);
        
        // Center text in button
        float textX = skipButtonBounds.x + skipButtonBounds.width / 2 - 20;
        float textY = skipButtonBounds.y + skipButtonBounds.height / 2 + font.getCapHeight() / 2;
        
        font.draw(batch, "SKIP", textX, textY);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void scheduleNextScreen() {
        if (!transitionScheduled) {
            transitionScheduled = true;
            isFadingIn = false;

            if (backgroundMusic != null && backgroundMusic.isPlaying()) {
                backgroundMusic.stop();
            }

            // Schedule the screen transition on the next frame
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    game.setScreen(new GameScreen(game));
                    dispose();
                }
            });
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        camera.position.set(viewport.getWorldWidth() * 0.5f, viewport.getWorldHeight() * 0.5f, 0f);
        camera.update();
    }

    @Override public void pause() {}
    @Override public void resume() {}

    @Override public void hide() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.stop();
        }
    }

    private Pixmap createColoredPixmap(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        return pixmap;
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        overlayPixel.dispose();
        shapeRenderer.dispose();
        for (Texture texture : storyImages) {
            if (texture != null) {
                texture.dispose();
            }
        }
        if (textBoxGreen != null) textBoxGreen.dispose();
        if (textBoxViolet != null) textBoxViolet.dispose();
        if (textBoxBlue != null) textBoxBlue.dispose();
        storyImages.clear();
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
    }
}
