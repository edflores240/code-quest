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
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;
import java.util.function.Supplier;

/**
 * Lightweight presentation screen for introducing the BotBug NPC.
 * Designed to be launched from exploration maps (e.g., GreenValleyScreen)
 * before handing control back to the originating screen.
 */
public class BotBugScreen implements Screen {
    private static final String BOTBUG_TEXTURE_PATH = "assets/images/npcs/botbug.png";
    private static final String BACKGROUND_TEXTURE_PATH = "assets/images/npcs/botbug_background.png";

    private final Main game;
    private final Supplier<Screen> returnScreenSupplier;

    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private final FitViewport viewport;

    private final BitmapFont titleFont;
    private final BitmapFont bodyFont;
    private final BitmapFont hintFont;
    private final GlyphLayout glyphLayout;

    private final Array<Texture> managedTextures;
    private Texture whitePixelTexture;
    private Texture backgroundTexture;
    private Texture botBugTexture;

    private float stateTime;
    private int currentHintIndex;

    private static final String[] BOTBUG_FACTS = {
            "Designation: BotBug",
            "Role: Debugging NPC keeping the valley glitch-free",
            "Temperament: Curious, friendly, obsessed with tidy code",
            "Interaction: Shares tips, grants minor buffs, trades spare parts"
    };

    private static final String[] BOTBUG_HINTS = {
            "Hint: BotBug patrols the old terminal near the valley bridge.",
            "Hint: Bring a spare circuit board to unlock a mini quest.",
            "Hint: Chat daily to receive rotating debugging advice.",
            "Hint: If the lights on BotBug glow green, it has new loot."
    };

    public BotBugScreen(Main game) {
        this(game, () -> new GreenValleyScreen(game));
    }

    public BotBugScreen(Main game, Supplier<Screen> returnScreenSupplier) {
        this.game = game;
        this.returnScreenSupplier = returnScreenSupplier;

        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(800f, 480f, camera);
        camera.setToOrtho(false, 800f, 480f);
        camera.position.set(400f, 240f, 0f);
        camera.update();

        this.batch = new SpriteBatch();
        this.glyphLayout = new GlyphLayout();

        this.titleFont = new BitmapFont();
        this.bodyFont = new BitmapFont();
        this.hintFont = new BitmapFont();
        configureFonts();

        this.managedTextures = new Array<>();
        this.whitePixelTexture = createWhitePixel();
        loadOptionalTextures();

        this.stateTime = 0f;
        this.currentHintIndex = 0;
    }

    private void configureFonts() {
        titleFont.getData().setScale(2.4f);
        titleFont.setColor(Color.valueOf("9EE493"));

        bodyFont.getData().setScale(1.1f);
        bodyFont.setColor(Color.WHITE);

        hintFont.getData().setScale(0.95f);
        hintFont.setColor(Color.valueOf("C1F2B0"));
    }

    private Texture createWhitePixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void loadOptionalTextures() {
        backgroundTexture = loadIfExists(BACKGROUND_TEXTURE_PATH, true);
        botBugTexture = loadIfExists(BOTBUG_TEXTURE_PATH, true);

        if (backgroundTexture == null) {
            backgroundTexture = loadIfExists("assets/images/greenvalley.png", false);
        }
    }

    private Texture loadIfExists(String path, boolean logFailure) {
        if (Gdx.files.internal(path).exists()) {
            Texture texture = new Texture(Gdx.files.internal(path));
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            managedTextures.add(texture);
            return texture;
        }
        if (logFailure) {
            Gdx.app.debug("BotBugScreen", "Optional texture not found: " + path);
        }
        return null;
    }

    @Override
    public void show() {
        // no-op
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
        handleInput();

        Gdx.gl.glClearColor(0.04f, 0.07f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        drawBackground();
        drawInfoPanel();
        drawBotBug();
        drawInstructions();

        batch.end();
    }

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            returnToPreviousScreen();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            cycleHint();
        }
    }

    private void returnToPreviousScreen() {
        if (returnScreenSupplier != null) {
            game.setScreen(returnScreenSupplier.get());
        } else {
            game.setScreen(new GreenValleyScreen(game));
        }
    }

    private void cycleHint() {
        currentHintIndex = (currentHintIndex + 1) % BOTBUG_HINTS.length;
    }

    private void drawBackground() {
        float width = viewport.getWorldWidth();
        float height = viewport.getWorldHeight();

        if (backgroundTexture != null) {
            batch.draw(backgroundTexture, 0f, 0f, width, height);
        } else if (whitePixelTexture != null) {
            batch.setColor(0.05f, 0.1f, 0.08f, 1f);
            batch.draw(whitePixelTexture, 0f, 0f, width, height);
            batch.setColor(Color.WHITE);
        }
    }

    private void drawInfoPanel() {
        float panelX = 60f;
        float panelY = 70f;
        float panelWidth = 360f;
        float panelHeight = viewport.getWorldHeight() - 140f;

        if (whitePixelTexture != null) {
            float pulse = 0.85f + 0.15f * MathUtils.sin(stateTime * 2f);
            batch.setColor(0f, 0.18f * pulse, 0.12f * pulse, 0.78f);
            batch.draw(whitePixelTexture, panelX, panelY, panelWidth, panelHeight);
            batch.setColor(Color.WHITE);
        }

        float textX = panelX + 28f;
        float textTop = panelY + panelHeight - 30f;

        titleFont.draw(batch, "BOTBUG // NPC", textX, textTop);

        float bodyY = textTop - 48f;
        for (String fact : BOTBUG_FACTS) {
            bodyFont.draw(batch, "- " + fact, textX, bodyY);
            bodyY -= 28f;
        }

        bodyY -= 12f;
        hintFont.draw(batch, BOTBUG_HINTS[currentHintIndex], textX, bodyY);
    }

    private void drawBotBug() {
        float viewportWidth = viewport.getWorldWidth();
        float viewportHeight = viewport.getWorldHeight();

        float baseWidth = 240f;
        float baseHeight = 260f;

        float bob = MathUtils.sin(stateTime * 1.6f) * 8f;
        float spriteX = viewportWidth - baseWidth - 100f;
        float spriteY = (viewportHeight - baseHeight) / 2f + bob;

        if (botBugTexture != null) {
            float textureWidth = botBugTexture.getWidth();
            float textureHeight = botBugTexture.getHeight();
            if (textureHeight > 0f) {
                float scale = baseHeight / textureHeight;
                float drawWidth = textureWidth * scale;
                float drawX = viewportWidth - drawWidth - 120f;
                batch.draw(botBugTexture, drawX, spriteY, drawWidth, baseHeight);
                drawGlow(drawX, spriteY, drawWidth, baseHeight);
                return;
            }
        }

        if (whitePixelTexture != null) {
            drawGlow(spriteX, spriteY, baseWidth, baseHeight);
            batch.setColor(0.3f, 0.9f, 0.4f, 1f);
            batch.draw(whitePixelTexture, spriteX, spriteY, baseWidth, baseHeight);
            batch.setColor(0.15f, 0.45f, 0.18f, 1f);
            batch.draw(whitePixelTexture, spriteX + 18f, spriteY + 18f, baseWidth - 36f, baseHeight - 36f);
            batch.setColor(Color.WHITE);
        }
    }

    private void drawGlow(float x, float y, float width, float height) {
        if (whitePixelTexture == null) {
            return;
        }
        float glowStrength = 0.3f + 0.2f * Math.abs(MathUtils.sin(stateTime * 2.2f));
        batch.setColor(0.2f, 0.9f, 0.4f, glowStrength);
        batch.draw(whitePixelTexture, x - 16f, y - 16f, width + 32f, height + 32f);
        batch.setColor(Color.WHITE);
    }

    private void drawInstructions() {
        String instruction = "Press ENTER to return // Press TAB for next hint";
        glyphLayout.setText(hintFont, instruction);
        float x = (viewport.getWorldWidth() - glyphLayout.width) / 2f;
        float y = 46f;
        hintFont.draw(batch, instruction, x, y);
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
        titleFont.dispose();
        bodyFont.dispose();
        hintFont.dispose();
        if (whitePixelTexture != null) {
            whitePixelTexture.dispose();
        }
        for (Texture texture : managedTextures) {
            if (texture != null) {
                texture.dispose();
            }
        }
    }
}
