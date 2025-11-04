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

public class GameScreen implements Screen {
    private final Main game;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final BitmapFont titleFont;
    private final FitViewport viewport;
    private Texture background;
    private Texture whitePixelTexture;
    private final Map<String, Animation<TextureRegion>> characterAnimations;
    private final Map<String, CharacterCard> characterCards;
    private String selectedCharacter = null;
    private String hoveredCharacter = null;
    private float stateTime;
    private float selectionPulse = 0f;
    private boolean isEditingName = false;
    private String playerName = "";
    private float editCursorBlink = 0f;
    private final ShapeRenderer shapeRenderer;

    // Transition animations
    private boolean isTransitioning = false;
    private float transitionTime = 0f;
    private static final float TRANSITION_DURATION = 0.5f;

    // Input handling
    private int selectedIndex = 0;
    private float keyCooldown = 0f;
    private static final float KEY_COOLDOWN_TIME = 0.15f;
    private List<String> characterKeys;

    // Character card class to manage individual character UI
    private static class CharacterCard {
        Rectangle bounds;
        String characterKey;
        String displayName;
        Color accentColor;
        float hoverProgress = 0f;
        float selectProgress = 0f;
        Vector2 position;

        public CharacterCard(String key, String name, Color color, float x, float y, float w, float h) {
            this.characterKey = key;
            this.displayName = name;
            this.accentColor = color;
            this.bounds = new Rectangle(x, y, w, h);
            this.position = new Vector2(x, y);
        }
    }

    public GameScreen(Main game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(800, 480, camera);
        // Center the camera on the screen
        camera.setToOrtho(false, 800, 480);
        camera.position.set(400, 240, 0);
        camera.update();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Initialize fonts with default LibGDX font
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);

        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);
        titleFont.setColor(Color.WHITE);

        // Load background
        try {
            this.background = new Texture(Gdx.files.internal("images/gamepic/background.png"));
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Background image not found, using solid color");
            this.background = null;
        }

        // Create white pixel for drawing shapes
        whitePixelTexture = createWhitePixel();

        // Initialize character animations
        characterAnimations = new HashMap<>();
        loadCharacterAnimations();

        // Initialize character cards
        characterCards = new HashMap<>();
        setupCharacterCards();

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

    private void setupCharacterCards() {
        // Initialize character keys list for keyboard navigation
        characterKeys = new ArrayList<>();

        float cardWidth = 180;  // Increased width to better fit characters
        float cardHeight = 250; // Increased height to better fit characters
        float spacing = 200;    // Increased spacing between cards
        float centerY = 100;    // Slightly lower position

        // Calculate centered positions for two cards
        float totalWidth = (cardWidth * 2) + spacing;
        float startX = (viewport.getWorldWidth() - totalWidth) / 2;

        if (characterAnimations.containsKey("male")) {
            characterCards.put("male", new CharacterCard(
                "male", "JAM",
                new Color(0.2f, 0.5f, 0.8f, 0.8f),  // Reduced opacity for a softer look
                startX, centerY, cardWidth, cardHeight
            ));
            characterKeys.add("male");
        }

        if (characterAnimations.containsKey("female")) {
            characterCards.put("female", new CharacterCard(
                "female", "JANE",
                new Color(0.2f, 0.8f, 0.3f, 1f),  // Green color for female character
                startX + cardWidth + spacing, centerY, cardWidth, cardHeight
            ));
            characterKeys.add("female");
        }
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
        selectionPulse += delta * 2f;

        // Update cursor blink for name editing
        if (isEditingName) {
            editCursorBlink = (editCursorBlink + delta * 2f) % 2f; // Blink cursor every 1 second
        }

        // Update transition animations
        if (isTransitioning) {
            transitionTime += delta;
            if (transitionTime >= TRANSITION_DURATION) {
                isTransitioning = false;
                transitionTime = 0f;
            }
        }

        // Clear screen
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);

        // Draw background
        batch.begin();
        if (background != null) {
            batch.draw(background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        }
        batch.end();

        // Keep character animation running during name editing
        if (isEditingName && selectedCharacter != null) {
            CharacterCard card = characterCards.get(selectedCharacter);
            if (card != null) {
                card.hoverProgress = 1f; // Keep the hover state
            }
        }

        // Draw main UI elements
        if (!isTransitioning) {
            drawTitle();
            drawCharacterCards();
            drawInstructions();
        } else {
            drawTransition();
        }

        // Draw name editing UI on top of everything if in name editing mode
        if (isEditingName) {
            drawNameEditingUI();
        }

        // Handle input
        handleInput();
    }

    private void updateInteraction(float delta) {
        Vector2 mousePos = getMousePosition();
        hoveredCharacter = null;

        for (Map.Entry<String, CharacterCard> entry : characterCards.entrySet()) {
            CharacterCard card = entry.getValue();
            Animation<TextureRegion> anim = characterAnimations.get(card.characterKey);
            if (anim == null) continue;

            // Calculate character sprite bounds
            float charScale = 0.1f + card.hoverProgress * 0.1f;
            float charWidth = anim.getKeyFrame(0).getRegionWidth() * charScale;
            float charHeight = anim.getKeyFrame(0).getRegionHeight() * charScale;
            float charX = card.bounds.x + (card.bounds.width - charWidth) / 5;
            float charY = card.bounds.y + card.bounds.height * 0.55f - charHeight / 5;

            // Create a rectangle for the character sprite
            Rectangle charBounds = new Rectangle(charX, charY, charWidth, charHeight);

            boolean isHovered = charBounds.contains(mousePos);
            boolean isSelected = entry.getKey().equals(selectedCharacter);

            if (isHovered) {
                hoveredCharacter = entry.getKey();
            }

            // Animate hover progress
            float targetHover = isHovered ? 1f : 0f;
            card.hoverProgress = MathUtils.lerp(card.hoverProgress, targetHover, delta * 8f);

            // Animate selection progress
            float targetSelect = isSelected ? 1f : 0f;
            card.selectProgress = MathUtils.lerp(card.selectProgress, targetSelect, delta * 6f);
        }
    }

    private Vector2 getMousePosition() {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        Vector2 worldCoords = viewport.unproject(new Vector2(mouseX, mouseY));
        return worldCoords;
    }

    private void drawTitle() {
        batch.begin();

        // Title with gradient and effects
        String title = "ADVENTURE AWAITS";
        String subtitle = "Choose your hero";

        // Title shadow and glow
        titleFont.getData().setScale(3.5f);
        GlyphLayout titleLayout = new GlyphLayout(titleFont, title);
        float titleX = (viewport.getWorldWidth() - titleLayout.width) / 2;
        float titleY = viewport.getWorldHeight() - 20;  // Moved title higher

        // Title glow effect
        float glowPulse = 0.8f + 0.2f * MathUtils.sin(selectionPulse * 2f);
        titleFont.setColor(0.3f, 0.5f, 1f, 0.4f * glowPulse);
        for (int i = 0; i < 4; i++) {
            float offset = i * 2f;
            titleFont.draw(batch, title, titleX - offset, titleY - offset);
            titleFont.draw(batch, title, titleX + offset, titleY - offset);
        }

        // Main title with gradient
        titleFont.setColor(0.9f, 0.95f, 1f, 1f);
        titleFont.draw(batch, title, titleX, titleY);

        // Title highlight
        titleFont.setColor(1, 1, 1, 0.8f);
        titleFont.draw(batch, title, titleX - 1, titleY + 1);

        // Subtitle
        font.getData().setScale(1.2f);
        GlyphLayout subLayout = new GlyphLayout(font, subtitle);
        float subX = (viewport.getWorldWidth() - subLayout.width) / 2;
        float subY = titleY - 55;

        font.setColor(0.8f, 0.9f, 1f, 0.9f);
        font.draw(batch, subtitle, subX, subY);

        // Reset font settings
        titleFont.setColor(Color.WHITE);
        titleFont.getData().setScale(2.5f);
        font.getData().setScale(1.2f);

        batch.end();
    }

    private void drawCharacterCards() {
        // Enable blending for transparency
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        for (Map.Entry<String, CharacterCard> entry : characterCards.entrySet()) {
            CharacterCard card = entry.getValue();
            Animation<TextureRegion> anim = characterAnimations.get(card.characterKey);
            if (anim == null) continue;

            drawCard(card, anim);
        }
    }

    private void drawCard(CharacterCard card, Animation<TextureRegion> animation) {
        float x = card.bounds.x;
        float y = card.bounds.y;
        float w = card.bounds.width;
        float h = card.bounds.height;

        // Calculate animation state based on hover or selection
        boolean shouldAnimate = card.hoverProgress > 0.1f || (selectedCharacter != null && selectedCharacter.equals(card.characterKey));
        float animSpeed = shouldAnimate ? stateTime : 0f;
        TextureRegion frame = animation.getKeyFrame(animSpeed, true);

        // Hover lift effect
        float liftOffset = card.hoverProgress * 8f;
        y += liftOffset;

        // Selection pulse
        float selectPulse = card.selectProgress * 5f * MathUtils.sin(selectionPulse * 3f);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Outer glow when selected
        if (card.selectProgress > 0.01f) {
            float glowSize = 15f + selectPulse;
            Color glowColor = card.accentColor.cpy();
            glowColor.a = card.selectProgress * 0.6f * (0.5f + 0.5f * MathUtils.sin(selectionPulse * 2f));
            shapeRenderer.setColor(glowColor);
            shapeRenderer.rect(x - glowSize, y - glowSize, w + glowSize * 2, h + glowSize * 2);
        }

        shapeRenderer.end();

        // Draw character
        batch.begin();

        // Character sprite - smaller size
        float charScale = 0.15f;  // Smaller character scale
        float charWidth = frame.getRegionWidth() * charScale;
        float charHeight = frame.getRegionHeight() * charScale;

        // Calculate character position to be centered
        float charX = x + (w - charWidth) / 2;
        float charY = y + (h - charHeight) / 2 + 15; // Move up slightly for name below

        // Floating animation
        float floatOffset = MathUtils.sin(stateTime * 2f + (card.characterKey.equals("male") ? 0 : MathUtils.PI)) * 3f;
        charY += floatOffset;

        // Character shadow
        batch.setColor(0, 0, 0, 0.4f);
        batch.draw(frame, charX + 4, charY - 4, charWidth, charHeight);

        // Main character with glow effect when selected
        if (card.selectProgress > 0.1f) {
            // Add glow effect
            float glowSize = 8f * card.selectProgress;
            Color glowColor = card.accentColor.cpy();
            glowColor.a = 0.3f * card.selectProgress;
            batch.setColor(glowColor);
            batch.draw(frame,
                charX - glowSize/2,
                charY - glowSize/2,
                charWidth + glowSize,
                charHeight + glowSize);
        }

        // Main character
        batch.setColor(Color.WHITE);
        batch.draw(frame, charX, charY, charWidth, charHeight);

        batch.end();

        // Draw cool name design below character
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Name button dimensions
        float buttonWidth = 110;
        float buttonHeight = 32;
        float buttonX = x + (w - buttonWidth) / 2;
        float buttonY = y + 20;

        // Button shadow
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(buttonX + 10, buttonY - 10, buttonWidth, buttonHeight);

        // Button gradient background based on character
        if ("male".equals(card.characterKey)) {
            // Blue gradient for male
            shapeRenderer.rect(buttonX, buttonY, buttonWidth, buttonHeight,
                new Color(0.2f, 0.4f, 0.7f, 0.95f),  // Bright blue top
                new Color(0.2f, 0.4f, 0.7f, 0.95f),  // Bright blue top
                new Color(0.1f, 0.2f, 0.4f, 0.95f),  // Dark blue bottom
                new Color(0.1f, 0.2f, 0.4f, 0.95f)   // Dark blue bottom
            );
        } else {
            // Green gradient for female
            shapeRenderer.rect(buttonX, buttonY, buttonWidth, buttonHeight,
                new Color(0.2f, 0.7f, 0.4f, 0.95f),  // Bright green top
                new Color(0.2f, 0.7f, 0.4f, 0.95f),  // Bright green top
                new Color(0.1f, 0.4f, 0.2f, 0.95f),  // Dark green bottom
                new Color(0.1f, 0.4f, 0.2f, 0.95f)   // Dark green bottom
            );
        }

        // Glowing border effect
        float borderIntensity = Math.max(card.hoverProgress, card.selectProgress);
        float borderPulse = 0.5f + 0.5f * MathUtils.sin(stateTime * 3f);
        Color borderColor = card.accentColor.cpy();
        borderColor.a = (0.6f + borderIntensity * 0.4f) * borderPulse;
        shapeRenderer.setColor(borderColor);

        // Top border
        shapeRenderer.rect(buttonX, buttonY + buttonHeight - 2, buttonWidth, 2);
        // Bottom border
        shapeRenderer.rect(buttonX, buttonY, buttonWidth, 2);
        // Left border
        shapeRenderer.rect(buttonX, buttonY, 2, buttonHeight);
        // Right border
        shapeRenderer.rect(buttonX + buttonWidth - 2, buttonY, 2, buttonHeight);

        shapeRenderer.end();

        batch.begin();

        // Draw name text in button
        font.getData().setScale(1.3f);
        String nameText = card.displayName;
        GlyphLayout nameLayout = new GlyphLayout(font, nameText);
        float nameX = buttonX + (buttonWidth - nameLayout.width) / 2;
        float nameY = buttonY + buttonHeight / 2 + nameLayout.height / 2;

        // Name shadow
        font.setColor(0, 0, 0, 0.8f);
        font.draw(batch, nameText, nameX + 1, nameY - 1);

        // Name with effects
        if (card.selectProgress > 0.01f) {
            // Bright white with pulse when selected
            float pulse = 1f + 0.15f * MathUtils.sin(stateTime * 5f);
            font.setColor(pulse, pulse, pulse, 1f);
        } else if (card.hoverProgress > 0.01f) {
            // Bright accent color when hovered
            Color hoverColor = card.accentColor.cpy();
            hoverColor.lerp(Color.WHITE, 0.5f); // Mix with white for brightness
            font.setColor(hoverColor);
        } else {
            // White when normal
            font.setColor(1f, 1f, 1f, 0.95f);
        }

        font.draw(batch, nameText, nameX, nameY);

        // Reset font
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
        batch.end();
    }

    private void drawNameEditingUI() {
        batch.begin();

        // Get character-specific colors
        boolean isFemale = "female".equals(selectedCharacter);
        float overlayR = isFemale ? 0.05f : 0.05f;
        float overlayG = isFemale ? 0.1f : 0.05f;
        float overlayB = isFemale ? 0.05f : 0.1f;

        float panelR = isFemale ? 0.1f : 0.15f;
        float panelG = isFemale ? 0.2f : 0.15f;
        float panelB = isFemale ? 0.1f : 0.25f;

        float glowR = isFemale ? 0.2f : 0.3f;
        float glowG = isFemale ? 0.5f : 0.6f;
        float glowB = isFemale ? 0.2f : 1f;

        // Animated gradient overlay
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        float overlayAlpha = 0.5f + (float) Math.sin(stateTime * 2) * 0.1f;
        shapeRenderer.setColor(overlayR, overlayG, overlayB, overlayAlpha);
        shapeRenderer.rect(0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        shapeRenderer.end();

        // Calculate center position
        float centerX = viewport.getWorldWidth() / 2;
        float centerY = viewport.getWorldHeight() / 2;

        // Animated panel scale
        float panelScale = 1f + (float) Math.sin(stateTime * 3) * 0.02f;

        // Main panel dimensions
        float panelWidth = 450 * panelScale;
        float panelHeight = 320 * panelScale;
        float panelX = centerX - panelWidth / 2;
        float panelY = centerY - panelHeight / 2;

        // Draw main panel with gradient effect
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Panel background with gradient simulation
        shapeRenderer.setColor(panelR, panelG, panelB, 0.95f);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);

        // Inner glow effect
        shapeRenderer.setColor(glowR * 0.7f, glowG * 0.7f, glowB * 0.5f, 0.3f);
        shapeRenderer.rect(panelX + 10, panelY + 10, panelWidth - 20, panelHeight - 20);

        // Animated border
        float borderGlow = 0.5f + (float) Math.sin(stateTime * 4) * 0.3f;
        shapeRenderer.setColor(glowR, glowG + borderGlow * 0.4f, glowB, 0.9f);
        shapeRenderer.rect(panelX, panelY, panelWidth, 3); // Top
        shapeRenderer.rect(panelX, panelY + panelHeight - 3, panelWidth, 3); // Bottom
        shapeRenderer.rect(panelX, panelY, 3, panelHeight); // Left
        shapeRenderer.rect(panelX + panelWidth - 3, panelY, 3, panelHeight); // Right
        shapeRenderer.end();

        // Draw title with glow effect
        font.getData().setScale(1.8f);
        font.setColor(1f, 1f, 1f, 1f);
        String title = "✦ ENTER YOUR NAME ✦";
        GlyphLayout titleLayout = new GlyphLayout(font, title);

        // Title glow
        font.setColor(glowR, glowG, glowB, 0.3f);
        font.draw(batch, title,
            centerX - titleLayout.width / 2 + 2,
            panelY + panelHeight - 40 + 2);

        // Main title
        float titlePulse = 0.8f + (float) Math.sin(stateTime * 2.5f) * 0.2f;
        font.setColor(1f, 1f, 1f, titlePulse);
        font.draw(batch, title,
            centerX - titleLayout.width / 2,
            panelY + panelHeight - 40);

        // Draw input field background
        float inputFieldWidth = 350;
        float inputFieldHeight = 60;
        float inputFieldX = centerX - inputFieldWidth / 2;
        float inputFieldY = centerY - inputFieldHeight / 2;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Input field background
        shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 0.9f);
        shapeRenderer.rect(inputFieldX, inputFieldY, inputFieldWidth, inputFieldHeight);

        // Input field border with animation
        float inputBorderGlow = 0.6f + (float) Math.sin(stateTime * 3) * 0.4f;
        shapeRenderer.setColor(glowR * 0.8f, glowG + inputBorderGlow * 0.3f, glowB * 0.9f, 0.9f);
        shapeRenderer.rect(inputFieldX, inputFieldY, inputFieldWidth, 2);
        shapeRenderer.rect(inputFieldX, inputFieldY + inputFieldHeight - 2, inputFieldWidth, 2);
        shapeRenderer.rect(inputFieldX, inputFieldY, 2, inputFieldHeight);
        shapeRenderer.rect(inputFieldX + inputFieldWidth - 2, inputFieldY, 2, inputFieldHeight);
        shapeRenderer.end();

        // Draw current name with cursor
        String displayText = playerName;
        if (editCursorBlink < 1f && isEditingName) {
            displayText += "|";
        }

        font.getData().setScale(2.0f);
        font.setColor(1f, 1f, 1f, 1f);
        GlyphLayout nameLayout = new GlyphLayout(font, displayText);
        font.draw(batch, displayText,
            centerX - nameLayout.width / 2,
            inputFieldY + inputFieldHeight / 2 + nameLayout.height / 2 + 10);

        // Draw instructions with icons
        font.getData().setScale(1.0f);
        String[] instructions = {
            "⌨ Type your name (max 15 chars)",
            "✓ Press ENTER to continue",
            "✕ Press ESC to cancel"
        };

        for (int i = 0; i < instructions.length; i++) {
            float instructionAlpha = 0.6f + (float) Math.sin(stateTime * 2 + i * 0.5f) * 0.2f;
            font.setColor(0.8f, 0.8f, 0.9f, instructionAlpha);
            GlyphLayout instructionLayout = new GlyphLayout(font, instructions[i]);
            font.draw(batch, instructions[i],
                centerX - instructionLayout.width / 2,
                panelY + 30 - i * 25);
        }

        // Draw decorative elements
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // Corner decorations
        float cornerSize = 20;
        float cornerGlow = 0.3f + (float) Math.sin(stateTime * 2.5f) * 0.2f;

        // Corner decorations with character-specific colors
        shapeRenderer.setColor(glowR * 0.8f, glowG * 0.8f, glowB, cornerGlow);
        shapeRenderer.rect(panelX, panelY + panelHeight - cornerSize, cornerSize, cornerSize);
        shapeRenderer.rect(panelX + panelWidth - cornerSize, panelY + panelHeight - cornerSize, cornerSize, cornerSize);
        shapeRenderer.rect(panelX, panelY, cornerSize, cornerSize);
        shapeRenderer.rect(panelX + panelWidth - cornerSize, panelY, cornerSize, cornerSize);
        shapeRenderer.end();

        font.getData().setScale(1.2f);
        batch.end();
    }

    private void drawInstructions() {
        batch.begin();

        font.getData().setScale(1.1f);

        // Animated instruction text based on selection state
        String instruction = selectedCharacter == null ?
            "← → Navigate | Select a character to continue" :
            "✓ Character Selected! Press ENTER to customize name | ESC to deselect";

        // Add pulsing effect to the instruction
        float instructionAlpha = 0.7f + (float) Math.sin(stateTime * 2) * 0.3f;

        // Draw instruction background panel
        float instructionWidth = 600;
        float instructionHeight = 50;
        float instructionX = (viewport.getWorldWidth() - instructionWidth) / 2;
        float instructionY = viewport.getWorldHeight() - 100;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 0.8f);
        shapeRenderer.rect(instructionX, instructionY, instructionWidth, instructionHeight);

        // Animated border for instruction panel
        float borderGlow = 0.4f + (float) Math.sin(stateTime * 3) * 0.3f;
        shapeRenderer.setColor(0.3f, 0.6f + borderGlow * 0.4f, 1f, 0.8f);
        shapeRenderer.rect(instructionX, instructionY, instructionWidth, 2);
        shapeRenderer.rect(instructionX, instructionY + instructionHeight - 2, instructionWidth, 2);
        shapeRenderer.rect(instructionX, instructionY, 2, instructionHeight);
        shapeRenderer.rect(instructionX + instructionWidth - 2, instructionY, 2, instructionHeight);
        shapeRenderer.end();

        // Draw instruction text with glow effect
        GlyphLayout layout = new GlyphLayout(font, instruction);

        // Glow effect
        font.setColor(0.3f, 0.6f, 1f, instructionAlpha * 0.3f);
        font.draw(batch, instruction,
            (viewport.getWorldWidth() - layout.width) / 2 + 1,
            instructionY + instructionHeight / 2 + layout.height / 2 + 1);

        // Main instruction text
        if (selectedCharacter != null) {
            font.setColor(0.3f, 1f, 0.6f, instructionAlpha); // Green when character selected
        } else {
            font.setColor(1f, 1f, 1f, instructionAlpha); // White when no selection
        }

        font.draw(batch, instruction,
            (viewport.getWorldWidth() - layout.width) / 2,
            instructionY + instructionHeight / 2 + layout.height / 2);

        // Additional hint for mobile/touch
        if (Gdx.app.getType().toString().equals("ANDROID") || Gdx.app.getType().toString().equals("IOS")) {
            font.getData().setScale(0.9f);
            String touchHint = selectedCharacter == null ?
                "Or tap on a character to select" :
                "Or tap ENTER button below";

            float touchAlpha = 0.5f + (float) Math.sin(stateTime * 1.5f) * 0.2f;
            font.setColor(0.7f, 0.7f, 0.8f, touchAlpha);
            GlyphLayout touchLayout = new GlyphLayout(font, touchHint);
            font.draw(batch, touchHint,
                (viewport.getWorldWidth() - touchLayout.width) / 2,
                instructionY - 25);
        }

        font.getData().setScale(1.0f);
        batch.end();
    }

    private void drawTransition() {
        batch.begin();

        float progress = Math.min(transitionTime / TRANSITION_DURATION, 1f);
        float alpha = Interpolation.pow2In.apply(progress);

        // Fade to black
        batch.setColor(0, 0, 0, alpha);
        batch.draw(whitePixelTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        batch.setColor(Color.WHITE);

        // Show selected character growing
        if (selectedCharacter != null) {
            Animation<TextureRegion> anim = characterAnimations.get(selectedCharacter);
            if (anim != null) {
                TextureRegion frame = anim.getKeyFrame(stateTime, true);
                float scale = 2f + progress * 2f;
                float w = frame.getRegionWidth() * scale;
                float h = frame.getRegionHeight() * scale;
                float x = (viewport.getWorldWidth() - w) / 2;
                float y = (viewport.getWorldHeight() - h) / 2;

                batch.draw(frame, x, y, w, h);
            }
        }

        batch.end();
    }

    // Helper method to draw rounded corners
    private void drawRoundedCorner(ShapeRenderer shapeRenderer, float x, float y, float radius, float startAngle, float thickness) {
        int segments = 8;
        float angle = 90f / segments;
        float angleLen = 90f / segments;

        for (int i = 0; i < segments; i++) {
            float a = startAngle + (i * angleLen);
            float a2 = startAngle + ((i + 1) * angleLen);

            float x1 = x + radius + (float)Math.cos(Math.toRadians(a)) * radius;
            float y1 = y + radius + (float)Math.sin(Math.toRadians(a)) * radius;
            float x2 = x + radius + (float)Math.cos(Math.toRadians(a2)) * radius;
            float y2 = y + radius + (float)Math.sin(Math.toRadians(a2)) * radius;

            shapeRenderer.rectLine(x1, y1, x2, y2, thickness);
        }
    }

    private void handleInput() {
        if (isTransitioning) return;

        // Handle name editing input
        if (isEditingName) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                // Save the name and proceed to game
                if (!playerName.trim().isEmpty()) {
                    game.setScreen(new GreenValleyScreen(game, playerName.trim()));
                }
                return;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                // Cancel name editing
                isEditingName = false;
                playerName = "";
                return;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && playerName.length() > 0) {
                // Handle backspace
                playerName = playerName.substring(0, playerName.length() - 1);
            }

            // Show text input dialog when starting to edit
            if (playerName.isEmpty()) {
                Gdx.input.getTextInput(new Input.TextInputListener() {
                    @Override
                    public void input(String text) {
                        if (text != null && !text.trim().isEmpty()) {
                            playerName = text.substring(0, Math.min(text.length(), 12));
                            // Auto-confirm when dialog is closed with text
                            if (!playerName.trim().isEmpty()) {
                                game.setScreen(new GreenValleyScreen(game, playerName.trim()));
                            } else {
                                isEditingName = false;
                            }
                        } else {
                            isEditingName = false;
                        }
                    }

                    @Override
                    public void canceled() {
                        isEditingName = false;
                        playerName = "";
                    }
                }, "Enter your name", "", "Player");
            }

            return;
        }

        // Ensure characterKeys is initialized and not empty
        if (characterKeys == null || characterKeys.isEmpty()) {
            return;
        }

        // Handle keyboard input with cooldown
        keyCooldown -= Gdx.graphics.getDeltaTime();
        boolean keyPressed = false;

        if (keyCooldown <= 0) {
            // Arrow key navigation
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
                if (selectedIndex > 0) {
                    selectedIndex--;
                    if (selectedIndex >= 0 && selectedIndex < characterKeys.size()) {
                        selectedCharacter = characterKeys.get(selectedIndex);
                        hoveredCharacter = selectedCharacter; // Update hover state for animation
                        keyPressed = true;
                    }
                }
            } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
                if (selectedIndex < characterKeys.size() - 1) {
                    selectedIndex++;
                    if (selectedIndex >= 0 && selectedIndex < characterKeys.size()) {
                        selectedCharacter = characterKeys.get(selectedIndex);
                        hoveredCharacter = selectedCharacter; // Update hover state for animation
                        keyPressed = true;
                    }
                }
            } else if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
                // For future vertical navigation if needed
                keyPressed = true;
            } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
                // For future vertical navigation if needed
                keyPressed = true;
            }

            if (keyPressed) {
                keyCooldown = KEY_COOLDOWN_TIME;
                // Update all character cards to ensure proper hover state
                for (Map.Entry<String, CharacterCard> entry : characterCards.entrySet()) {
                    CharacterCard card = entry.getValue();
                    if (entry.getKey().equals(selectedCharacter)) {
                        card.hoverProgress = 1f; // Force hover state for selected character
                    } else {
                        card.hoverProgress = 0f; // Clear hover for others
                    }
                }
            }
        }

        // ENTER/SPACE key to select character and show name input
        if ((Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) && selectedCharacter != null && !isEditingName) {
            Gdx.app.log("GameScreen", "Entering name editing mode for character: " + selectedCharacter);
            isEditingName = true;
            playerName = "Player"; // Default name
            return;
        }

        // Handle name editing input
        if (isEditingName) {
            // Handle backspace
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && playerName.length() > 0) {
                playerName = playerName.substring(0, playerName.length() - 1);
            }

            // Handle ENTER to confirm name
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                String trimmedName = playerName.trim();
                if (!trimmedName.isEmpty()) {
                    try {
                        game.setScreen(new GreenValleyScreen(game, trimmedName));
                    } catch (Exception e) {
                        Gdx.app.error("GameScreen", "Error transitioning to GreenValleyScreen: " + e.getMessage());
                        isEditingName = false; // Go back to character selection on error
                    }
                } else {
                    // If name is empty, show a message or keep editing
                    playerName = "Player"; // Set default name
                }
                return;
            }

            // Handle ESC to cancel name editing
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                isEditingName = false;
                return;
            }

            // Handle text input for letters A-Z and numbers 0-9
            for (int i = Input.Keys.A; i <= Input.Keys.Z; i++) {
                if (Gdx.input.isKeyJustPressed(i) && playerName.length() < 15) {
                    char c = (char) ('a' + (i - Input.Keys.A));
                    if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) {
                        c = Character.toUpperCase(c);
                    }
                    playerName += c;
                }
            }

            // Handle space key
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && playerName.length() < 15) {
                playerName += " ";
            }

            // Handle numbers (0-9)
            for (int i = Input.Keys.NUMPAD_0; i <= Input.Keys.NUMPAD_9; i++) {
                if (Gdx.input.isKeyJustPressed(i) && playerName.length() < 15) {
                    playerName += (char) ('0' + (i - Input.Keys.NUMPAD_0));
                }
            }
            return;
        }

        // ESC key handling
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (selectedCharacter != null) {
                selectedCharacter = null;
            } else {
                game.setScreen(new MenuScreen(game));
            }
        }

        // Mouse click handling
        if (Gdx.input.justTouched()) {
            Vector2 mousePos = getMousePosition();

            // Removed Green Valley button click handling

            // Check character selection
            int index = 0;
            for (Map.Entry<String, CharacterCard> entry : characterCards.entrySet()) {
                if (entry.getValue().bounds.contains(mousePos)) {
                    selectedCharacter = entry.getKey();
                    selectedIndex = index;
                    // Start name editing when a character is clicked
                    isEditingName = true;
                    playerName = "";
                    return;
                }
                index++;
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void show() {
        selectedCharacter = null;
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
        shapeRenderer.dispose();
    }
}
