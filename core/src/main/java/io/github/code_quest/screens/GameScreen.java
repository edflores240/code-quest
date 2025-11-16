package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
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
import io.github.code_quest.audio.BackgroundMusicManager;
import io.github.code_quest.save.SaveManager;
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

    private final Sound selectionSound;
    private final Sound confirmSound;
    private boolean musicRegistered;

    // Skip button properties
    private Rectangle skipButtonBounds;
    private float skipButtonPulse = 0f;
    private boolean skipButtonHovered = false;
    private float skipButtonClickEffect = 0f;
    private boolean skipButtonInitialized = false;

    // Transition animations
    private boolean isTransitioning = false;
    private float transitionTime = 0f;
    private static final float TRANSITION_DURATION = 0.5f;

    private static final String BACKGROUND_MUSIC_PATH = "assets/sounds/loadingscreenmusic.wav";

    // Input handling
    private int selectedIndex = 0;
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
            this.background = new Texture(Gdx.files.internal("assets/images/gamepic/12.2.png"));
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

        // Initialize skip button will be done in show() method after viewport is set up

        stateTime = 0f;

        selectionSound = loadSoundIfExists("assets/sounds/selectingcharacter.mp3", "GameScreen");
        confirmSound = loadSoundIfExists("assets/sounds/afterselectingcharacter (1).mp3", "GameScreen");

        BackgroundMusicManager.playLoop(BACKGROUND_MUSIC_PATH, 0.5f);
        musicRegistered = true;
    }

    private Sound loadSoundIfExists(String path, String tag) {
        if (Gdx.files.internal(path).exists()) {
            try {
                return Gdx.audio.newSound(Gdx.files.internal(path));
            } catch (Exception e) {
                Gdx.app.error(tag, "Failed to load sound: " + path, e);
            }
        } else {
            Gdx.app.error(tag, "Missing sound: " + path);
        }
        return null;
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

        float cardWidth = 150f;  // Compact card width
        float cardHeight = 190f; // Slightly shorter cards
        float spacing = 120f;     // Comfortable distance between cards
        float centerY = 95f;     // Balanced vertical placement

        // Calculate centered positions for two cards
        float totalWidth = (cardWidth * 2) + spacing;
        float startX = (viewport.getWorldWidth() - totalWidth) / 2;

        if (characterAnimations.containsKey("male")) {
            characterCards.put("male", new CharacterCard(
                "male", "Jam",
                new Color(0.3f, 0.6f, 1f, 1f),  // Bright blue
                startX, centerY, cardWidth, cardHeight
            ));
            characterKeys.add("male");
        }

        if (characterAnimations.containsKey("female")) {
            characterCards.put("female", new CharacterCard(
                "female", "Jelay",
                new Color(0.4f, 1f, 0.5f, 1f),  // Bright green
                startX + cardWidth + spacing, centerY, cardWidth, cardHeight
            ));
            characterKeys.add("female");
        }
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

    @Override
    public void render(float delta) {
        stateTime += delta;
        selectionPulse += delta * 2f;

        // Update skip button animations
        // DISABLED - using simple ESC key instead
        // skipButtonPulse += delta * 3f;
        // if (skipButtonClickEffect > 0) {
        //     skipButtonClickEffect -= delta * 2f;
        // }

        // Check mouse hover for skip button
        // DISABLED - using simple ESC key instead
        // Vector2 mousePos = getMousePosition();
        // skipButtonHovered = skipButtonBounds != null && skipButtonBounds.contains(mousePos);

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
            float charScale = 0.1f + card.hoverProgress * 0.02f;
            float charWidth = anim.getKeyFrame(0).getRegionWidth() * charScale;
            float charHeight = anim.getKeyFrame(0).getRegionHeight() * charScale;
            float charX = card.bounds.x + (card.bounds.width - charWidth) / 2f;
            float charY = card.bounds.y + (card.bounds.height - charHeight) / 2f + 15f;

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

        String title = "CODE QUEST";
        String subtitle = "Choose Your Hero";

        // Modern clean title at top
        titleFont.getData().setScale(3.8f);
        GlyphLayout titleLayout = new GlyphLayout(titleFont, title);
        float titleX = (viewport.getWorldWidth() - titleLayout.width) / 2f;
        float titleY = viewport.getWorldHeight() - 40f;

        // Title shadow for depth
        titleFont.setColor(0f, 0f, 0f, 0.4f);
        titleFont.draw(batch, title, titleX + 3f, titleY - 3f);
        
        // Main title with gradient effect
        titleFont.setColor(1f, 1f, 1f, 1f);
        titleFont.draw(batch, title, titleX, titleY);

        // Subtitle
        font.getData().setScale(1.5f);
        GlyphLayout subLayout = new GlyphLayout(font, subtitle);
        float subX = (viewport.getWorldWidth() - subLayout.width) / 2f;
        float subY = titleY - 50f;
        
        font.setColor(0.7f, 0.7f, 0.7f, 1f);
        font.draw(batch, subtitle, subX, subY);

        batch.end();

        // Reset font settings
        titleFont.setColor(Color.WHITE);
        titleFont.getData().setScale(2.5f);
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
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
        float liftOffset = card.hoverProgress * 3f;
        y += liftOffset;

        // Scale effect on hover/select
        float scaleBoost = (card.hoverProgress * 0.02f) + (card.selectProgress * 0.02f);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Modern card background with rounded corners effect
        Color cardBg = new Color(0.14f, 0.14f, 0.19f, 0.95f);
        shapeRenderer.setColor(cardBg);
        shapeRenderer.rect(x, y, w, h);

        // Inner panel
        Color innerPanel = new Color(0.2f, 0.2f, 0.25f, 1f);
        shapeRenderer.setColor(innerPanel);
        shapeRenderer.rect(x + 6f, y + 6f, w - 12f, h - 12f);

        // Selection highlight border
        if (card.selectProgress > 0.01f) {
            float borderThickness = 4f;
            Color selectColor = card.accentColor.cpy();
            selectColor.a = card.selectProgress;
            shapeRenderer.setColor(selectColor);
            
            // Top border
            shapeRenderer.rect(x, y + h - borderThickness, w, borderThickness);
            // Bottom border
            shapeRenderer.rect(x, y, w, borderThickness);
            // Left border
            shapeRenderer.rect(x, y, borderThickness, h);
            // Right border
            shapeRenderer.rect(x + w - borderThickness, y, borderThickness, h);

            // Soft glow outline
            Color glowColor = card.accentColor.cpy();
            glowColor.a = 0.15f * card.selectProgress * (0.6f + 0.4f * MathUtils.sin(selectionPulse * 2.5f));
            shapeRenderer.setColor(glowColor);
            shapeRenderer.rectLine(x - 6f, y - 6f, x + w + 6f, y - 6f, 2f);
            shapeRenderer.rectLine(x - 6f, y + h + 6f, x + w + 6f, y + h + 6f, 2f);
            shapeRenderer.rectLine(x - 6f, y - 6f, x - 6f, y + h + 6f, 2f);
            shapeRenderer.rectLine(x + w + 6f, y - 6f, x + w + 6f, y + h + 6f, 2f);
        }

        // Bottom name bar
        Color nameBarColor = new Color(0.1f, 0.1f, 0.15f, 0.9f);
        shapeRenderer.setColor(nameBarColor);
        shapeRenderer.rect(x + 6f, y + 6f, w - 12f, 40f);

        // Accent line on name bar
        Color accentLine = card.accentColor.cpy();
        accentLine.a = 0.8f;
        shapeRenderer.setColor(accentLine);
        shapeRenderer.rect(x + 6f, y + 44f, w - 12f, 3f);

        shapeRenderer.end();

        // Draw character
        batch.begin();

        // Character sprite with scale
        float baseScale = 0.10f;
        float charScale = baseScale + scaleBoost;
        float charWidth = frame.getRegionWidth() * charScale;
        float charHeight = frame.getRegionHeight() * charScale;

        // Center character in card
        float charX = x + (w - charWidth) / 2;
        float charY = y + (h - charHeight) / 2 + 15f;

        // Floating animation
        float floatOffset = MathUtils.sin(stateTime * 2f + (card.characterKey.equals("male") ? 0 : MathUtils.PI)) * 3f;
        charY += floatOffset;

        // Character shadow
        batch.setColor(0, 0, 0, 0.3f);
        batch.draw(frame, charX + 3, charY - 3, charWidth, charHeight);

        // Glow effect when selected
        if (card.selectProgress > 0.1f) {
            float glowSize = 8f * card.selectProgress;
            Color glowColor = card.accentColor.cpy();
            glowColor.a = 0.4f * card.selectProgress;
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

        // Character name label
        font.getData().setScale(1.6f);
        GlyphLayout nameLayout = new GlyphLayout(font, card.displayName);
        float nameX = x + (w - nameLayout.width) / 2;
        float nameY = y + 30f;

        // Name shadow
        font.setColor(0f, 0f, 0f, 0.6f);
        font.draw(batch, card.displayName, nameX + 1.5f, nameY - 1.5f);

        // Name text with accent color
        Color nameColor = card.accentColor.cpy();
        nameColor.a = 1f;
        font.setColor(nameColor);
        font.draw(batch, card.displayName, nameX, nameY);

        // Selection indicator text
        if (card.selectProgress > 0.5f) {
            font.getData().setScale(1.0f);
            String selectText = "PRESS ENTER";
            GlyphLayout selectLayout = new GlyphLayout(font, selectText);
            float selectX = x + (w - selectLayout.width) / 2;
            float selectY = y + h - 15f;
            
            float pulse = 0.6f + 0.4f * MathUtils.sin(selectionPulse * 4f);
            font.setColor(1f, 1f, 1f, pulse * card.selectProgress);
            font.draw(batch, selectText, selectX, selectY);
        }

        batch.end();

        // Reset font
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
    }

    private void drawNameEditingUI() {
        // Name editing UI removed - now handled by CharacterCustomizationScreen
    }

    private void drawInstructions() {
        // Instructions removed - only characters and title will be displayed
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
                    // TODO: Implement new map screen
                    // game.setScreen(new GreenValleyScreen(game, playerName.trim()));
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
                                // TODO: Implement new map screen
                                // game.setScreen(new GreenValleyScreen(game, playerName.trim()));
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

        boolean keyPressed = false;
        boolean selectionChanged = false;

        // Arrow key navigation without cooldown
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            if (selectedIndex > 0) {
                selectedIndex--;
                if (selectedIndex >= 0 && selectedIndex < characterKeys.size()) {
                    selectedCharacter = characterKeys.get(selectedIndex);
                    hoveredCharacter = selectedCharacter; // Update hover state for animation
                    keyPressed = true;
                    selectionChanged = true;
                }
            }
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            if (selectedIndex < characterKeys.size() - 1) {
                selectedIndex++;
                if (selectedIndex >= 0 && selectedIndex < characterKeys.size()) {
                    selectedCharacter = characterKeys.get(selectedIndex);
                    hoveredCharacter = selectedCharacter; // Update hover state for animation
                    keyPressed = true;
                    selectionChanged = true;
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
            // Update all character cards to ensure proper hover and selection state
            for (Map.Entry<String, CharacterCard> entry : characterCards.entrySet()) {
                CharacterCard card = entry.getValue();
                if (entry.getKey().equals(selectedCharacter)) {
                    card.hoverProgress = 1f; // Force hover state for selected character
                    card.selectProgress = 1f; // Force selection state for arrow to show
                } else {
                    card.hoverProgress = 0f; // Clear hover for others
                    card.selectProgress = 0f; // Clear selection for others
                }
            }
        }

        if (selectionChanged && selectionSound != null) {
            selectionSound.play(0.7f);
        }

        // ENTER/SPACE key to select character and go to customization
        if ((Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) && selectedCharacter != null) {
            // Check if skip button was clicked
            // DISABLED - using simple ESC key instead
            // if (skipButtonHovered) {
            //     skipButtonClickEffect = 1f;
            //     // Skip directly to game with default character and name
            //     game.setScreen(new GreenValleyScreen(game, "Player"));
            //     return;
            // }
            if (confirmSound != null) {
                confirmSound.play(0.7f);
            }
            Gdx.app.log("GameScreen", "Selected character: " + selectedCharacter + ", going to customization");
            game.setScreen(new CharacterCustomizationScreen(game, selectedCharacter));
            return;
        }

        // Handle mouse/touch input for skip button
        // DISABLED - using simple ESC key instead
        // if (Gdx.input.justTouched()) {
        //     if (skipButtonHovered) {
        //         skipButtonClickEffect = 1f;
        //         // Skip directly to game with default character and name
        //         game.setScreen(new GreenValleyScreen(game, "Player"));
        //         return;
        //     }
        // }

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
                        // TODO: Implement new map screen
                        // game.setScreen(new GreenValleyScreen(game, trimmedName));
                    } catch (Exception e) {
                        Gdx.app.error("GameScreen", "Error transitioning to new map screen: " + e.getMessage());
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
                    if (selectionSound != null) {
                        selectionSound.play(0.7f);
                    }
                    if (confirmSound != null) {
                        confirmSound.play(0.7f);
                    }
                    // Go to customization when a character is clicked
                    game.setScreen(new CharacterCustomizationScreen(game, selectedCharacter));
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

        // Initialize skip button now that viewport is properly set up
        setupSkipButton();

        if (!musicRegistered) {
            BackgroundMusicManager.playLoop(BACKGROUND_MUSIC_PATH, 0.5f);
            musicRegistered = true;
        }
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        if (musicRegistered) {
            BackgroundMusicManager.release();
            musicRegistered = false;
        }
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
        if (selectionSound != null) {
            selectionSound.dispose();
        }
        if (confirmSound != null) {
            confirmSound.dispose();
        }
        if (musicRegistered) {
            BackgroundMusicManager.release();
            musicRegistered = false;
        }
    }
}
