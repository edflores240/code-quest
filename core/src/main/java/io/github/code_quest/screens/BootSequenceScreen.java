package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.github.code_quest.Main;

public class BootSequenceScreen implements Screen {
    private final Main game;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout layout;

    private final Array<String> messages;
    private final Array<Float> messageDurations;
    private int currentMessageIndex = 0;
    private float messageTimer = 0;
    private float globalTimer = 0;
    private final float FADE_DURATION = 0.8f;

    private Music bgMusic;
    private Sound bootSound;
    private Sound typingSound;

    private enum State { FADE_IN, SHOW_MESSAGES, FADE_OUT, COMPLETE }
    private State state = State.FADE_IN;
    private float alpha = 0;
    private final Color textColor = new Color(0.2f, 1f, 0.2f, 1f); // Green glow

    private final Array<MatrixParticle> particles;
    private final FrameBuffer scanlineBuffer;
    private float scanlineY = 0;
    private Texture whitePixel;

    public BootSequenceScreen(Main game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        font.getRegion().getTexture().setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear,
                                              com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        layout = new GlyphLayout();

        // Initialize messages and durations
        messages = new Array<>();
        messageDurations = new Array<>();

        messages.add("Initializing System...");
        messageDurations.add(1.5f);

        messages.add("Loading Quest Data...");
        messageDurations.add(1.8f);

        messages.add("Calibrating Sensors...");
        messageDurations.add(1.6f);

        messages.add("Welcome, Adventurer.");
        messageDurations.add(2.0f);

        // Initialize particles
        particles = new Array<>();
        for (int i = 0; i < 50; i++) {
            particles.add(new MatrixParticle());
        }

        scanlineBuffer = new FrameBuffer(com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888, 800, 480, false);
        createWhitePixel();
        loadAssets();
    }

    private void loadAssets() {
        try {
            bgMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/boot_sequence.ogg"));
            bootSound = Gdx.audio.newSound(Gdx.files.internal("sounds/boot_beep.ogg"));
            typingSound = Gdx.audio.newSound(Gdx.files.internal("sounds/typing.ogg"));

            if (bgMusic != null) {
                bgMusic.setLooping(true);
                bgMusic.setVolume(0.3f);
                bgMusic.play();
            }
        } catch (Exception e) {
            Gdx.app.error("BootSequenceScreen", "Error loading assets", e);
        }
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        // Draw particles
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (MatrixParticle p : particles) {
            p.render(batch);
        }

        batch.end();

        // Draw scanlines
        drawScanlines();

        // Draw current message
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (state == State.SHOW_MESSAGES || state == State.FADE_OUT) {
            String currentMessage = messages.get(currentMessageIndex);
            layout.setText(font, currentMessage);
            float width = layout.width;
            float height = layout.height;
            float x = (800 - width) / 2;
            float y = 240 + height / 2;

            // Add glow effect
            font.setColor(0.1f, 0.5f, 0.1f, alpha * 0.5f);
            font.draw(batch, currentMessage, x + 1, y + 1);
            font.draw(batch, currentMessage, x - 1, y - 1);
            font.draw(batch, currentMessage, x + 1, y - 1);
            font.draw(batch, currentMessage, x - 1, y + 1);

            // Main text
            font.setColor(textColor.r, textColor.g, textColor.b, alpha);
            font.draw(batch, currentMessage, x, y);

            // Progress indicator
            if (currentMessageIndex < messages.size - 1) {
                float progress = messageTimer / messageDurations.get(currentMessageIndex);
                drawProgressBar(progress);
            }
        }

        batch.end();

        // Draw scanline overlay
        if (scanlineY > 0) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            batch.setColor(0.2f, 1f, 0.2f, 0.1f);
            batch.draw(scanlineBuffer.getColorBufferTexture(), 0, scanlineY - 2, 800, 4);
            batch.end();
        }
    }

    private void drawScanlines() {
        scanlineY += 2;
        if (scanlineY > 480) {
            scanlineY = 0;
        }

        scanlineBuffer.begin();
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        scanlineBuffer.end();
    }

    private void createWhitePixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whitePixel = new Texture(pixmap);
        pixmap.dispose();
    }

    private void drawProgressBar(float progress) {
        batch.setColor(0.2f, 1f, 0.2f, alpha * 0.8f);
        batch.draw(whitePixel, 200, 180, 400 * progress, 4);

        batch.setColor(0.1f, 0.5f, 0.1f, alpha * 0.5f);
        batch.draw(whitePixel, 199, 179, 402, 6);
    }

    private void update(float delta) {
        globalTimer += delta;

        // Update particles
        for (MatrixParticle p : particles) {
            p.update(delta);
            if (p.position.y < -10) {
                p.reset();
            }
        }

        // State machine
        switch (state) {
            case FADE_IN:
                alpha = Math.min(1f, alpha + delta / FADE_DURATION);
                if (alpha >= 1f) {
                    state = State.SHOW_MESSAGES;
                    if (bootSound != null) bootSound.play(0.5f);
                }
                break;

            case SHOW_MESSAGES:
                messageTimer += delta;
                if (messageTimer >= messageDurations.get(currentMessageIndex)) {
                    if (typingSound != null) typingSound.play(0.2f);

                    currentMessageIndex++;
                    messageTimer = 0;

                    if (currentMessageIndex >= messages.size) {
                        state = State.FADE_OUT;
                    }
                }
                break;

            case FADE_OUT:
                alpha = Math.max(0f, alpha - delta / FADE_DURATION);
                if (alpha <= 0f) {
                    state = State.COMPLETE;
                    transitionToGame();
                }
                break;

            case COMPLETE:
                // Already transitioned
                break;
        }
    }

    private void transitionToGame() {
        // Stop music and transition to main game
        if (bgMusic != null) {
            bgMusic.stop();
        }
        game.setScreen(new GameScreen(game)); // Replace with your actual game screen
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, 800, 480);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        scanlineBuffer.dispose();
        if (whitePixel != null) whitePixel.dispose();
        if (bgMusic != null) bgMusic.dispose();
        if (bootSound != null) bootSound.dispose();
        if (typingSound != null) typingSound.dispose();
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    private class MatrixParticle {
        private final Vector2 position;
        private final Vector2 velocity;
        private char character;
        private Color color;
        private float intensity;
        private float life;

        private static final String CHARS = "01ABCDEF";

        public MatrixParticle() {
            this.position = new Vector2();
            this.velocity = new Vector2();
            this.color = new Color();
            reset();
        }

        public void reset() {
            position.set(
                MathUtils.random(800),
                480 + MathUtils.random(100)
            );
            velocity.set(
                (MathUtils.random() - 0.5f) * 20,
                -MathUtils.random(50, 150)
            );
            character = CHARS.charAt(MathUtils.random(CHARS.length() - 1));
            intensity = MathUtils.random(0.3f, 1f);
            life = 1f;
        }

        public void update(float delta) {
            position.add(velocity.x * delta, velocity.y * delta);
            life -= delta * 0.5f;
            intensity = Math.max(0, intensity - delta * 0.2f);
        }

        public void render(SpriteBatch batch) {
            if (intensity > 0 && whitePixel != null) {
                batch.setColor(0.2f * intensity, 1f * intensity, 0.2f * intensity, intensity * 0.8f);
                // Draw a small rectangle using the white pixel texture
                batch.draw(whitePixel, position.x, position.y, 8, 12);
            }
        }
    }
}
