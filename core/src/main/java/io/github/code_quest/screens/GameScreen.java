package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;
import java.util.HashMap;
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
    private final ShapeRenderer shapeRenderer;
    
    // Transition animations
    private boolean isTransitioning = false;
    private float transitionTime = 0f;
    private static final float TRANSITION_DURATION = 0.5f;

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
        camera.setToOrtho(false, 800, 480);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        
        // Create fonts with different sizes
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
        float cardWidth = 120;
        float cardHeight = 180;
        float spacing = 60;
        float centerY = 200;
        
        // Calculate centered positions for two cards
        float totalWidth = (cardWidth * 2) + spacing;
        float startX = (viewport.getWorldWidth() - totalWidth) / 2;
        
        if (characterAnimations.containsKey("male")) {
            characterCards.put("male", new CharacterCard(
                "male",
                "WARRIOR",
                new Color(0.3f, 0.7f, 1f, 1f),
                startX,
                centerY - cardHeight / 2,
                cardWidth,
                cardHeight
            ));
        }
        
        if (characterAnimations.containsKey("female")) {
            characterCards.put("female", new CharacterCard(
                "female",
                "RANGER",
                new Color(1f, 0.4f, 0.7f, 1f),
                startX + cardWidth + spacing,
                centerY - cardHeight / 2,
                cardWidth,
                cardHeight
            ));
        }
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
        selectionPulse += delta * 2f;
        
        if (isTransitioning) {
            transitionTime += delta;
            if (transitionTime >= TRANSITION_DURATION) {
                // Transition complete - move to next screen or game
                // game.setScreen(new NextScreen(game, selectedCharacter));
            }
        }
        
        // Update hover and selection states
        updateInteraction(delta);
        
        // Clear screen with dark background
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Draw background
        batch.begin();
        if (background != null) {
            batch.setColor(0.6f, 0.6f, 0.7f, 1f);
            batch.draw(background, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
            batch.setColor(Color.WHITE);
        }
        batch.end();
        
        // Draw UI elements
        if (!isTransitioning) {
            drawTitle();
            drawCharacterCards();
            drawInstructions();
        } else {
            drawTransition();
        }
        
        // Handle input
        handleInput();
    }
    
    private void updateInteraction(float delta) {
        Vector2 mousePos = getMousePosition();
        hoveredCharacter = null;
        
        for (Map.Entry<String, CharacterCard> entry : characterCards.entrySet()) {
            CharacterCard card = entry.getValue();
            boolean isHovered = card.bounds.contains(mousePos);
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
        
        // Title with shadow
        String title = "CHOOSE YOUR HERO";
        GlyphLayout layout = new GlyphLayout(titleFont, title);
        float titleX = (viewport.getWorldWidth() - layout.width) / 2;
        float titleY = viewport.getWorldHeight() - 60;
        
        // Shadow
        titleFont.setColor(0, 0, 0, 0.5f);
        titleFont.draw(batch, title, titleX + 3, titleY - 3);
        
        // Main title with pulse effect
        float pulse = 1f + 0.05f * MathUtils.sin(selectionPulse);
        titleFont.setColor(pulse, pulse, pulse, 1f);
        titleFont.draw(batch, title, titleX, titleY);
        titleFont.setColor(Color.WHITE);
        
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
        
        // Calculate animation state based on hover
        float animSpeed = card.hoverProgress > 0.1f ? stateTime : 0f;
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
        
        // Card background shadow
        shapeRenderer.setColor(0, 0, 0, 0.4f);
        shapeRenderer.rect(x + 4, y - 4, w, h);
        
        // Card background
        Color bgColor = new Color(0.15f, 0.15f, 0.2f, 0.95f);
        shapeRenderer.setColor(bgColor);
        shapeRenderer.rect(x, y, w, h);
        
        // Hover border
        if (card.hoverProgress > 0.01f || card.selectProgress > 0.01f) {
            float borderAlpha = Math.max(card.hoverProgress, card.selectProgress) * 0.8f;
            Color borderColor = card.accentColor.cpy();
            borderColor.a = borderAlpha;
            shapeRenderer.setColor(borderColor);
            
            float borderThickness = 3f + card.selectProgress * 2f;
            // Top
            shapeRenderer.rect(x, y + h - borderThickness, w, borderThickness);
            // Bottom
            shapeRenderer.rect(x, y, w, borderThickness);
            // Left
            shapeRenderer.rect(x, y, borderThickness, h);
            // Right
            shapeRenderer.rect(x + w - borderThickness, y, borderThickness, h);
        }
        
        shapeRenderer.end();
        
        // Draw character
        batch.begin();
        
        // Character sprite with pixel-perfect scaling
        float charScale = 2.2f + card.hoverProgress * 0.15f;
        float charWidth = frame.getRegionWidth() * charScale;
        float charHeight = frame.getRegionHeight() * charScale;
        float charX = x + (w - charWidth) / 2;
        float charY = y + h * 0.55f - charHeight / 2;
        
        // Floating animation
        float floatOffset = MathUtils.sin(stateTime * 2f + (card.characterKey.equals("male") ? 0 : MathUtils.PI)) * 3f;
        charY += floatOffset;
        
        // Draw character with subtle shadow
        batch.setColor(0, 0, 0, 0.3f);
        batch.draw(frame, charX + 2, charY - 2, charWidth, charHeight);
        batch.setColor(Color.WHITE);
        batch.draw(frame, charX, charY, charWidth, charHeight);
        
        // Character name with accent color
        font.getData().setScale(1.3f);
        GlyphLayout nameLayout = new GlyphLayout(font, card.displayName);
        float nameX = x + (w - nameLayout.width) / 2;
        float nameY = y + 35;
        
        // Name shadow
        font.setColor(0, 0, 0, 0.6f);
        font.draw(batch, card.displayName, nameX + 2, nameY - 2);
        
        // Name with accent color
        Color nameColor = card.accentColor.cpy();
        nameColor.lerp(Color.WHITE, 0.3f);
        if (card.selectProgress > 0.01f) {
            float pulse = 1f + card.selectProgress * 0.2f * MathUtils.sin(selectionPulse * 3f);
            nameColor.r *= pulse;
            nameColor.g *= pulse;
            nameColor.b *= pulse;
        }
        font.setColor(nameColor);
        font.draw(batch, card.displayName, nameX, nameY);
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
        
        // "SELECTED" label
        if (card.selectProgress > 0.5f) {
            font.getData().setScale(0.9f);
            String selectedText = "SELECTED";
            GlyphLayout selectedLayout = new GlyphLayout(font, selectedText);
            float selectedX = x + (w - selectedLayout.width) / 2;
            float selectedY = y + h - 15;
            
            font.setColor(card.accentColor);
            font.draw(batch, selectedText, selectedX, selectedY);
            font.setColor(Color.WHITE);
            font.getData().setScale(1.2f);
        }
        
        batch.end();
    }
    
    private void drawInstructions() {
        batch.begin();
        
        font.getData().setScale(0.9f);
        
        String instruction = selectedCharacter == null ? 
            "Click a character to select  |  ESC to return" :
            "Press ENTER to continue  |  ESC to go back";
        
        GlyphLayout layout = new GlyphLayout(font, instruction);
        float instrX = (viewport.getWorldWidth() - layout.width) / 2;
        float instrY = 30;
        
        // Pulsing alpha for attention
        float alpha = 0.6f + 0.3f * MathUtils.sin(selectionPulse);
        font.setColor(1f, 1f, 1f, alpha);
        font.draw(batch, instruction, instrX, instrY);
        font.setColor(Color.WHITE);
        font.getData().setScale(1.2f);
        
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
    
    private void handleInput() {
        if (isTransitioning) return;
        
        // ESC key handling
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (selectedCharacter != null) {
                selectedCharacter = null;
            } else {
                game.setScreen(new MenuScreen(game));
            }
        }
        
        // ENTER key to confirm selection
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && selectedCharacter != null) {
            isTransitioning = true;
            transitionTime = 0f;
        }
        
        // Mouse click handling
        if (Gdx.input.justTouched()) {
            Vector2 mousePos = getMousePosition();
            
            for (Map.Entry<String, CharacterCard> entry : characterCards.entrySet()) {
                if (entry.getValue().bounds.contains(mousePos)) {
                    selectedCharacter = entry.getKey();
                    break;
                }
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