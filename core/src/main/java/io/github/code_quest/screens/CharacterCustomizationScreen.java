package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CharacterCustomizationScreen implements Screen {
    private final Main game;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final BitmapFont titleFont;
    private final BitmapFont glitchFont;
    private final FitViewport viewport;
    private Texture background;
    private Texture whitePixelTexture;
    private final Map<String, Animation<TextureRegion>> characterAnimations;
    private String selectedCharacter;
    private float stateTime;
    private float selectionPulse = 0f;
    private final ShapeRenderer shapeRenderer;

    // Input fields
    private String playerName = "";
    private String playerAge = "";
    private String playerCourse = "";
    private int currentField = 0; // 0: name, 1: age, 2: course
    private float editCursorBlink = 0f;

    // UI elements
    private Rectangle[] fieldBounds;
    private String[] fieldLabels = {"NAME", "AGE", "COLLEGE \nCOURSE"};
    private String[] fieldPlaceholders = {"Enter your name", "Enter your age", "Enter your course"};

    // Transition animations
    private boolean isTransitioning = false;
    private float transitionTime = 0f;
    private static final float TRANSITION_DURATION = 0.5f;

    // Background particles
    private static final int PARTICLE_COUNT = 50;
    private final float[] particleX = new float[PARTICLE_COUNT];
    private final float[] particleY = new float[PARTICLE_COUNT];
    private final float[] particleVX = new float[PARTICLE_COUNT];
    private final float[] particleVY = new float[PARTICLE_COUNT];
    private final float[] particleSize = new float[PARTICLE_COUNT];
    private final float[] particleAlpha = new float[PARTICLE_COUNT];

    // Floating hexagons
    private static final int HEXAGON_COUNT = 5;
    private final float[] hexX = new float[HEXAGON_COUNT];
    private final float[] hexY = new float[HEXAGON_COUNT];
    private final float[] hexRotation = new float[HEXAGON_COUNT];
    private final float[] hexScale = new float[HEXAGON_COUNT];

    public CharacterCustomizationScreen(Main game, String selectedCharacter) {
        this.game = game;
        this.selectedCharacter = selectedCharacter;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(800, 480, camera);
        camera.setToOrtho(false, 800, 480);
        camera.position.set(400, 240, 0);
        camera.update();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Initialize fonts with different styles
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);

        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.8f);
        titleFont.setColor(Color.WHITE);

        glitchFont = new BitmapFont();
        glitchFont.getData().setScale(2.2f);
        glitchFont.setColor(Color.WHITE);

        // Load background
        try {
            this.background = new Texture(Gdx.files.internal("images/gamepic/background.png"));
        } catch (Exception e) {
            Gdx.app.error("CharacterCustomizationScreen", "Background image not found, using solid color");
            this.background = null;
        }

        // Create white pixel for drawing shapes
        whitePixelTexture = createWhitePixel();

        // Initialize character animations
        characterAnimations = new HashMap<>();
        loadCharacterAnimations();

        // Initialize input field bounds
        fieldBounds = new Rectangle[3];
        setupInputFields();

        // Initialize background effects
        initializeParticles();
        initializeHexagons();

        stateTime = 0f;
    }

    private Texture createWhitePixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void loadCharacterAnimations() {
        // Load male character animation
        Array<TextureRegion> maleFrames = new Array<>();
        for (int i = 1; i <= 12; i++) {
            String path = String.format("images/malecharacter/%dframe.png", i);
            if (Gdx.files.internal(path).exists()) {
                Texture texture = new Texture(path);
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                maleFrames.add(new TextureRegion(texture));
            }
        }
        if (maleFrames.size > 0) {
            characterAnimations.put("male", new Animation<>(0.15f, maleFrames, Animation.PlayMode.LOOP));
        }

        // Load female character animation
        Array<TextureRegion> femaleFrames = new Array<>();
        for (int i = 1; i <= 9; i++) {
            String path = String.format("images/femalecharacter/frame%d.png", i);
            if (Gdx.files.internal(path).exists()) {
                Texture texture = new Texture(path);
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                femaleFrames.add(new TextureRegion(texture));
            }
        }
        if (femaleFrames.size > 0) {
            characterAnimations.put("female", new Animation<>(0.15f, femaleFrames, Animation.PlayMode.LOOP));
        }
    }

    private void setupInputFields() {
        float fieldWidth = 320;
        float fieldHeight = 55;
        float spacing = 100;

        // Different positions for male and female
        boolean isFemale = "female".equals(selectedCharacter);
        float startX, startY;

        if (isFemale) {
            // Female input fields - positioned to the left
            startX = 115;
            startY = 315;
        } else {
            // Male input fields - centered position
            startX = 480;
            startY = 315;
        }

        for (int i = 0; i < 3; i++) {
            fieldBounds[i] = new Rectangle(startX, startY - i * spacing, fieldWidth, fieldHeight);
        }
    }

    private void initializeParticles() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleX[i] = MathUtils.random(800);
            particleY[i] = MathUtils.random(480);
            particleVX[i] = MathUtils.random(-20f, 20f);
            particleVY[i] = MathUtils.random(-30f, 30f);
            particleSize[i] = MathUtils.random(1f, 4f);
            particleAlpha[i] = MathUtils.random(0.1f, 0.4f);
        }
    }

    private void initializeHexagons() {
        for (int i = 0; i < HEXAGON_COUNT; i++) {
            hexX[i] = MathUtils.random(800);
            hexY[i] = MathUtils.random(480);
            hexRotation[i] = MathUtils.random(360);
            hexScale[i] = MathUtils.random(0.5f, 1.5f);
        }
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
        selectionPulse += delta * 2f;

        // Update cursor blink
        editCursorBlink = (editCursorBlink + delta * 2f) % 2f;

        // Update transition animations
        if (isTransitioning) {
            transitionTime += delta;
            if (transitionTime >= TRANSITION_DURATION) {
                isTransitioning = false;
                transitionTime = 0f;
            }
        }

        // Update particles
        updateParticles(delta);
        updateHexagons(delta);

        // Clear screen
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);

        // Draw background
        batch.begin();
        if (background != null) {
            batch.setColor(0.3f, 0.3f, 0.4f, 0.3f);
            batch.draw(background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
            batch.setColor(Color.WHITE);
        }
        batch.end();

        // Draw background effects
        drawBackgroundEffects();

        // Draw main UI elements
        if (!isTransitioning) {
            drawTitle();
            drawCharacter();
            drawInputFields();
            drawInstructions();
        } else {
            drawTransition();
        }

        // Handle input
        handleInput();
    }

    private void updateParticles(float delta) {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particleX[i] += particleVX[i] * delta;
            particleY[i] += particleVY[i] * delta;

            // Wrap around screen
            if (particleX[i] < 0) particleX[i] = 800;
            if (particleX[i] > 800) particleX[i] = 0;
            if (particleY[i] < 0) particleY[i] = 480;
            if (particleY[i] > 480) particleY[i] = 0;

            // Pulse alpha
            particleAlpha[i] = 0.1f + MathUtils.sin(stateTime * 2f + i) * 0.1f;
        }
    }

    private void updateHexagons(float delta) {
        for (int i = 0; i < HEXAGON_COUNT; i++) {
            hexRotation[i] += delta * 20f * (i % 2 == 0 ? 1 : -1);
            hexScale[i] = 1f + MathUtils.sin(stateTime + i) * 0.3f;

            // Slow floating movement
            hexX[i] += MathUtils.sin(stateTime * 0.5f + i) * 5f * delta;
            hexY[i] += MathUtils.cos(stateTime * 0.3f + i) * 3f * delta;

            // Keep on screen
            if (hexX[i] < -50) hexX[i] = 850;
            if (hexX[i] > 850) hexX[i] = -50;
            if (hexY[i] < -50) hexY[i] = 530;
            if (hexY[i] > 530) hexY[i] = -50;
        }
    }

    private void drawBackgroundEffects() {
        // Draw particles
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        boolean isFemale = "female".equals(selectedCharacter);
        Color particleColor = isFemale ? new Color(0.2f, 0.8f, 0.3f, 1f) : new Color(0.2f, 0.4f, 0.9f, 1f);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            shapeRenderer.setColor(particleColor.r, particleColor.g, particleColor.b, particleAlpha[i]);
            shapeRenderer.rect(particleX[i], particleY[i], particleSize[i], particleSize[i]);
        }
        shapeRenderer.end();

        // Draw floating hexagons
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Color hexColor = isFemale ? new Color(0.1f, 0.4f, 0.2f, 1f) : new Color(0.1f, 0.2f, 0.5f, 1f);

        for (int i = 0; i < HEXAGON_COUNT; i++) {
            shapeRenderer.setColor(hexColor.r, hexColor.g, hexColor.b, 0.1f);
            drawHexagon(hexX[i], hexY[i], 30f * hexScale[i], hexRotation[i]);
        }
        shapeRenderer.end();
    }

    private void drawHexagon(float x, float y, float size, float rotation) {
        int segments = 6;
        float angleStep = 360f / segments;

        for (int i = 0; i < segments; i++) {
            float angle1 = (i * angleStep + rotation) * MathUtils.degreesToRadians;
            float angle2 = ((i + 1) * angleStep + rotation) * MathUtils.degreesToRadians;

            float x1 = x + MathUtils.cos(angle1) * size;
            float y1 = y + MathUtils.sin(angle1) * size;
            float x2 = x + MathUtils.cos(angle2) * size;
            float y2 = y + MathUtils.sin(angle2) * size;

            shapeRenderer.triangle(x, y, x1, y1, x2, y2);
        }
    }

    private void drawTitle() {
        batch.begin();

        // Animated title with glitch effect
        String title = "CHARACTER CUSTOMIZATION";
        titleFont.getData().setScale(2.8f);
        GlyphLayout titleLayout = new GlyphLayout(titleFont, title);
        float titleX = (viewport.getWorldWidth() - titleLayout.width) / 2;
        float titleY = viewport.getWorldHeight() - 40;

        // Random glitch effect
        if (MathUtils.random() < 0.05f) {
            titleFont.setColor(1f, 0.2f, 0.3f, 0.8f);
            titleFont.draw(batch, title, titleX + MathUtils.random(-4f, 4f), titleY + MathUtils.random(-2f, 2f));
        }

        // Multiple glow layers
        boolean isFemale = "female".equals(selectedCharacter);
        Color glowColor = isFemale ? new Color(0.2f, 0.8f, 0.3f, 1f) : new Color(0.2f, 0.4f, 0.9f, 1f);

        for (int i = 3; i > 0; i--) {
            float offset = i * 2f;
            float alpha = 0.3f / i;
            titleFont.setColor(glowColor.r, glowColor.g, glowColor.b, alpha);
            titleFont.draw(batch, title, titleX - offset, titleY - offset);
            titleFont.draw(batch, title, titleX + offset, titleY - offset);
        }

        // Main title with pulse
        float pulse = 0.8f + MathUtils.sin(selectionPulse * 2f) * 0.2f;
        titleFont.setColor(0.9f, 0.95f, 1f, pulse);
        titleFont.draw(batch, title, titleX, titleY);

        // White highlight
        titleFont.setColor(1f, 1f, 1f, 0.6f);
        titleFont.draw(batch, title, titleX - 1f, titleY + 1f);

        batch.end();
    }

    private void drawCharacter() {
        if (selectedCharacter == null) return;

        Animation<TextureRegion> anim = characterAnimations.get(selectedCharacter);
        if (anim == null) return;

        batch.begin();
        //CHARACTER POSITION
        TextureRegion frame = anim.getKeyFrame(stateTime, true);
        float charScale = 0.25f;
        float charWidth = frame.getRegionWidth() * charScale;
        float charHeight = frame.getRegionHeight() * charScale;

        // Different positions for male and female characters
        float charX, charY;

        if ("female".equals(selectedCharacter)) {
            // Female character on the right side
            charX = 600 - charWidth / 2;
            charY = 20;
        } else {
            // Male character on the opposite side (left)
            charX = -150; // Left side position
            charY = 20;
        }

        // Floating animation
        float floatOffset = MathUtils.sin(stateTime * 2f) * 10f;
        charY += floatOffset;

        // Multiple shadow layers for depth
        for (int i = 3; i > 0; i--) {
            batch.setColor(0, 0, 0, 0.1f * i);
            batch.draw(frame, charX + i * 3f, charY - i * 3f, charWidth, charHeight);
        }

        // Glowing aura effect
        boolean isFemale = "female".equals(selectedCharacter);
        Color auraColor = isFemale ? new Color(0.2f, 0.8f, 0.3f, 1f) : new Color(0.2f, 0.4f, 0.9f, 1f);

        for (int i = 5; i > 0; i--) {
            float glowSize = i * 3f;
            float alpha = 0.1f / i;
            batch.setColor(auraColor.r, auraColor.g, auraColor.b, alpha);
            batch.draw(frame, charX - glowSize, charY - glowSize, charWidth + glowSize * 2, charHeight + glowSize * 2);
        }

        // Main character
        batch.setColor(Color.WHITE);
        batch.draw(frame, charX, charY, charWidth, charHeight);

        batch.end();
    }

    private void drawInputFields() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < 3; i++) {
            Rectangle bounds = fieldBounds[i];
            boolean isSelected = i == currentField;
            boolean isFemale = "female".equals(selectedCharacter);

            // Field background with gradient effect
            if (isSelected) {
                // Animated selected background
                float pulse = MathUtils.sin(stateTime * 3f + i) * 0.1f + 0.2f;
                shapeRenderer.setColor(isFemale ? 0.1f : 0.05f, isFemale ? 0.4f : 0.15f, isFemale ? 0.1f : 0.4f, 0.95f);

                // Inner glow
                shapeRenderer.setColor(isFemale ? 0.3f : 0.1f, isFemale ? 0.6f : 0.2f, isFemale ? 0.3f : 0.6f, pulse);
            } else {
                shapeRenderer.setColor(0.08f, 0.08f, 0.12f, 0.8f);
            }
            shapeRenderer.rect(bounds.x, bounds.y, bounds.width, bounds.height);

            // Animated border
            if (isSelected) {
                float borderGlow = 0.5f + MathUtils.sin(stateTime * 4f) * 0.5f;
                Color borderColor = isFemale ? new Color(0.2f, 0.8f, 0.3f, 1f) : new Color(0.2f, 0.4f, 0.9f, 1f);
                shapeRenderer.setColor(borderColor.r, borderColor.g, borderColor.b, borderGlow);

                // Thicker animated border
                shapeRenderer.rect(bounds.x - 2, bounds.y - 2, bounds.width + 4, 4);
                shapeRenderer.rect(bounds.x - 2, bounds.y + bounds.height - 2, bounds.width + 4, 4);
                shapeRenderer.rect(bounds.x - 2, bounds.y - 2, 4, bounds.height + 4);
                shapeRenderer.rect(bounds.x + bounds.width - 2, bounds.y - 2, 4, bounds.height + 4);

                // Corner accents
                float cornerSize = 8f + MathUtils.sin(stateTime * 5f) * 2f;
                shapeRenderer.rect(bounds.x - 4, bounds.y + bounds.height - 4, cornerSize, 4);
                shapeRenderer.rect(bounds.x + bounds.width - cornerSize, bounds.y + bounds.height - 4, cornerSize, 4);
                shapeRenderer.rect(bounds.x - 4, bounds.y, cornerSize, 4);
                shapeRenderer.rect(bounds.x + bounds.width - cornerSize, bounds.y, cornerSize, 4);
            } else {
                shapeRenderer.setColor(0.3f, 0.3f, 0.4f, 0.4f);
                shapeRenderer.rect(bounds.x, bounds.y, bounds.width, 2);
                shapeRenderer.rect(bounds.x, bounds.y + bounds.height - 2, bounds.width, 2);
                shapeRenderer.rect(bounds.x, bounds.y, 2, bounds.height);
                shapeRenderer.rect(bounds.x + bounds.width - 2, bounds.y, 2, bounds.height);
            }
        }

        shapeRenderer.end();

        batch.begin();

        // Draw field labels with effects
        for (int i = 0; i < 3; i++) {
            Rectangle bounds = fieldBounds[i];
            boolean isSelected = i == currentField;

            // Field label with glow
            font.getData().setScale(1.0f);
            if (isSelected) {
                // Glowing effect for selected label
                boolean isFemale = "female".equals(selectedCharacter);
                Color labelColor = isFemale ? new Color(0.2f, 0.8f, 0.3f, 1f) : new Color(0.2f, 0.4f, 0.9f, 1f);

                // Glow layers
                for (int j = 2; j > 0; j--) {
                    font.setColor(labelColor.r, labelColor.g, labelColor.b, 0.3f / j);
                    font.draw(batch, fieldLabels[i], bounds.x - 90 + j, bounds.y + bounds.height / 2 + 6 + j);
                }

                font.setColor(labelColor.r, labelColor.g, labelColor.b, 1f);
            } else {
                font.setColor(0.6f, 0.6f, 0.7f, 0.8f);
            }
            font.draw(batch, fieldLabels[i], bounds.x - 90, bounds.y + bounds.height / 2 + 6);

            // Field value with typing effect
            font.getData().setScale(1.3f);
            String value = getFieldText(i);
            if (isSelected && editCursorBlink < 1f) {
                value += "_";
            }

            if (value.isEmpty()) {
                font.setColor(0.4f, 0.4f, 0.5f, 0.6f);
                font.draw(batch, fieldPlaceholders[i], bounds.x + 15, bounds.y + bounds.height / 2 + -30);
            } else {
                // Typed text with subtle glow
                if (isSelected) {
                    font.setColor(1f, 1f, 1f, 0.9f);
                    // Shadow
                    font.setColor(0, 0, 0, 0.5f);
                    font.draw(batch, value, bounds.x + 12, bounds.y + bounds.height / 2 + -27);
                    font.setColor(1f, 1f, 1f, 1f);
                } else {
                    font.setColor(0.8f, 0.8f, 0.9f, 0.9f);
                }
                font.draw(batch, value, bounds.x + 15, bounds.y + bounds.height / 2 + -30);
            }
        }

        batch.end();
    }

    private String getFieldText(int fieldIndex) {
        switch (fieldIndex) {
            case 0: return playerName;
            case 1: return playerAge;
            case 2: return playerCourse;
            default: return "";
        }
    }

    private void setFieldText(int fieldIndex, String text) {
        switch (fieldIndex) {
            case 0:
                playerName = text;
                break;
            case 1:
                playerAge = text;
                break;
            case 2:
                playerCourse = text;
                break;
        }
    }

    private void drawInstructions() {
        batch.begin();

        // Animated instructions with typewriter effect
        font.getData().setScale(0.9f);
        String[] instructions = {
            "↑ ↓ NAVIGATE FIELDS",
            "TYPE TO CUSTOMIZE",
            "ENTER → START | BACKSPACE ← BACK"
        };

        for (int i = 0; i < instructions.length; i++) {
            float alpha = 0.6f + MathUtils.sin(stateTime * 2f + i * 0.8f) * 0.3f;
            boolean isFemale = "female".equals(selectedCharacter);
            Color instColor = isFemale ? new Color(0.2f, 0.8f, 0.3f, 1f) : new Color(0.2f, 0.4f, 0.9f, 1f);

            // Glow effect
            font.setColor(instColor.r, instColor.g, instColor.b, alpha * 0.3f);
            GlyphLayout layout = new GlyphLayout(font, instructions[i]);
            font.draw(batch, instructions[i],
                (viewport.getWorldWidth() - layout.width) / 2 + 1f,
                70 - i * 25f + 1f);

            // Main text
            font.setColor(instColor.r, instColor.g, instColor.b, alpha);
            font.draw(batch, instructions[i],
                (viewport.getWorldWidth() - layout.width) / 2,
                70 - i * 25f);
        }

        batch.end();
    }

    private void drawTransition() {
        batch.begin();

        float progress = Math.min(transitionTime / TRANSITION_DURATION, 1f);
        float alpha = Interpolation.pow2In.apply(progress);

        // Fade to black with color effect
        boolean isFemale = "female".equals(selectedCharacter);
        Color fadeColor = isFemale ? new Color(0.1f, 0.4f, 0.1f, 1f) : new Color(0.05f, 0.1f, 0.3f, 1f);
        batch.setColor(fadeColor.r, fadeColor.g, fadeColor.b, alpha);
        batch.draw(whitePixelTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(Color.WHITE);

        batch.end();
    }

    private void handleInput() {
        if (isTransitioning) return;

        // Navigate between fields
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            currentField = Math.max(0, currentField - 1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            currentField = Math.min(2, currentField + 1);
        }

        // Handle text input for current field
        String currentText = getFieldText(currentField);

        // Handle backspace
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && currentText.length() > 0) {
            currentText = currentText.substring(0, currentText.length() - 1);
            setFieldText(currentField, currentText);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && currentText.length() == 0) {
            game.setScreen(new GameScreen(game));
            return;
        }

        // Handle letters A-Z
        for (int i = Input.Keys.A; i <= Input.Keys.Z; i++) {
            if (Gdx.input.isKeyJustPressed(i)) {
                char c = (char) ('a' + (i - Input.Keys.A));
                if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
                    c = Character.toUpperCase(c);
                }

                // Add character if within limit
                int maxLength = (currentField == 1) ? 3 : 20; // Age max 3 chars, others max 20
                if (currentText.length() < maxLength) {
                    if (currentField == 1) {
                        // Only allow numbers for age
                        if (Character.isDigit(c)) {
                            setFieldText(currentField, currentText + c);
                        }
                    } else {
                        setFieldText(currentField, currentText + c);
                    }
                }
            }
        }

        // Handle numbers for age field
        if (currentField == 1) {
            for (int i = Input.Keys.NUM_0; i <= Input.Keys.NUM_9; i++) {
                if (Gdx.input.isKeyJustPressed(i) && currentText.length() < 3) {
                    char c = (char) ('0' + (i - Input.Keys.NUM_0));
                    setFieldText(currentField, currentText + c);
                }
            }
        }

        // Handle space key
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && currentField != 1 && currentText.length() < 20) {
            setFieldText(currentField, currentText + " ");
        }

        // ENTER to continue
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (!playerName.trim().isEmpty() && !playerAge.trim().isEmpty() && !playerCourse.trim().isEmpty()) {
                // Start transition to game
                isTransitioning = true;
                transitionTime = 0f;

                // After transition, go to game screen
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        game.setScreen(new GreenValleyScreen(game, playerName.trim()));
                    }
                });
            }
        }

        // ESC to go back
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new GameScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void show() {
        currentField = 0;
        isTransitioning = false;
        transitionTime = 0f;
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (background != null) {
            background.dispose();
        }

        if (whitePixelTexture != null) {
            whitePixelTexture.dispose();
        }

        for (Animation<TextureRegion> anim : characterAnimations.values()) {
            for (TextureRegion frame : anim.getKeyFrames()) {
                if (frame != null && frame.getTexture() != null) {
                    frame.getTexture().dispose();
                }
            }
        }

        batch.dispose();
        font.dispose();
        titleFont.dispose();
        glitchFont.dispose();
        shapeRenderer.dispose();
    }
}
