package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;
import io.github.code_quest.entities.ModernGlitchParticles;
// Removed particle imports for cleaner design

public class MenuScreen implements Screen {
    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final SpriteBatch batch;

    private Texture menuBackground;    // assets/images/menu-background.png
    private Texture titleNormal;       // assets/images/menu-title.png
    private Texture titleGlitch;      // assets/images/menu-title-glitch.png
    private Texture startButton;      // assets/images/start-button.png

    private Image backgroundImage;
    private Image titleImage;
    private Image startButtonImage;

    private Table root;
    private TextButton startBtn;
    private ModernGlitchParticles glitchParticles;
    private Music backgroundMusic;
    private Sound confirmSound;
    private boolean startTriggered;


    public MenuScreen(Main game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.stage = new Stage(new FitViewport(800, 480), batch);
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        // FIX: ensure default-font exists
        if (!skin.has("default-font", BitmapFont.class)) {
            skin.add("default-font", new BitmapFont());
        }

        loadOptionalAssets();
        loadMusic();
        loadSfx();
        buildUI();
        wireInput();
    }

    private void loadOptionalAssets() {
        // Load the specific assets for the new design
        menuBackground = loadIfExists("images/menu-background.png");
        titleNormal = loadIfExists("images/menu-title.png");
        titleGlitch = loadIfExists("images/menu-title-glitch.png");
        startButton = loadIfExists("images/start-button.png");
    }

    private void loadMusic() {
        // Load and configure background music
        if (Gdx.files.internal("sounds/loadingscreenmusic.wav").exists()) {
            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/loadingscreenmusic.wav"));
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(0.5f); // Set volume to 50%
        }
    }

    private void loadSfx() {
        if (Gdx.files.internal("assets/sounds/afterselectingcharacter (1).mp3").exists()) {
            try {
                confirmSound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/afterselectingcharacter (1).mp3"));
            } catch (Exception e) {
                Gdx.app.error("MenuScreen", "Failed to load start sound", e);
            }
        } else {
            Gdx.app.error("MenuScreen", "Missing start sound: assets/sounds/afterselectingcharacter (1).mp3");
        }
    }

    private Texture loadIfExists(String path) {
        if (Gdx.files.internal(path).exists()) return new Texture(Gdx.files.internal(path));
        return null;
    }

    private Image solidPlaceholder(Color color, int w, int h) {
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setColor(color);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        Image img = new Image(t);
        img.setScaling(Scaling.stretch);
        return img;
    }

    private void buildUI() {
        // Background: use the menu-background.png
        if (menuBackground != null) {
            backgroundImage = new Image(menuBackground);
            backgroundImage.setScaling(Scaling.stretch);
        } else {
            backgroundImage = solidPlaceholder(new Color(0.05f, 0.1f, 0.05f, 1f), 8, 8);
        }
        backgroundImage.setFillParent(true);
        stage.addActor(backgroundImage);

       // Modern glitch particles for background
        glitchParticles = new ModernGlitchParticles(800, 480, 40, new Color(0.8f, 1f, 0.8f, 0.3f));
        stage.addActor(glitchParticles);

        // Root layout - centered design
        root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Create glitchy title animation
        createGlitchyTitle();

        // Create animated start button
        createAnimatedStartButton();

        // Removed subtitle for cleaner look

        // Center everything vertically
        root.add().expandY().row();
        if (titleNormal != null && titleGlitch != null) {
            root.add(titleImage).size(600, 150).padTop(100).padBottom(20).row();

        } else {
            // Add the fallback title label directly - smaller size
            Label.LabelStyle titleStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.valueOf("9EE493"));
            Label titleLabel = new Label("CodeQuest", titleStyle);
            titleLabel.setFontScale(2.0f);
            titleLabel.setAlignment(Align.center);
            root.add(titleLabel).padBottom(40).row();

        }
        if (startButton != null) {
            root.add(startButtonImage).size(200, 80).padBottom(20).row();
        } else {
            // Add the fallback button directly - smaller size
            startBtn = new TextButton("START", skin);
            startBtn.setColor(Color.valueOf("9EE493"));
            startBtn.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    onStart();
                }
            });
            root.add(startBtn).size(150, 50).padBottom(20).row();
        }
        root.add().expandY();

        // Input processor
        Gdx.input.setInputProcessor(stage);
    }

    private void createGlitchyTitle() {
        if (titleNormal != null && titleGlitch != null) {
            // Create the normal title image
            titleImage = new Image(titleNormal);
            titleImage.setScaling(Scaling.fit);

            // Glitch animation: alternate between normal and glitch frames
            titleImage.addAction(Actions.forever(Actions.sequence(
                Actions.delay(2.0f), // Stay normal for 2 seconds
                Actions.run(() -> {
                    if (titleImage.getDrawable() != null) {
                        titleImage.setDrawable(new Image(titleNormal).getDrawable());
                    }
                }),
                Actions.delay(0.1f), // Quick glitch
                Actions.run(() -> {
                    if (titleImage.getDrawable() != null) {
                        titleImage.setDrawable(new Image(titleGlitch).getDrawable());
                    }
                }),
                Actions.delay(0.15f), // Back to normal
                Actions.run(() -> {
                    if (titleImage.getDrawable() != null) {
                        titleImage.setDrawable(new Image(titleNormal).getDrawable());
                    }
                }),
                Actions.delay(0.1f), // Another quick glitch
                Actions.run(() -> {
                    if (titleImage.getDrawable() != null) {
                        titleImage.setDrawable(new Image(titleGlitch).getDrawable());
                    }
                }),
                Actions.delay(0.1f), // Back to normal
                Actions.run(() -> {
                    if (titleImage.getDrawable() != null) {
                        titleImage.setDrawable(new Image(titleNormal).getDrawable());
                    }
                })
            )));
        } else {
            // Fallback: placeholder for when assets are missing
            titleImage = solidPlaceholder(new Color(0.1f, 0.1f, 0.1f, 0.8f), 400, 100);
        }
    }

    private void createAnimatedStartButton() {
        if (startButton != null) {
            startButtonImage = new Image(startButton);
            startButtonImage.setScaling(Scaling.fit);

            // Simple pulsing animation
            startButtonImage.addAction(Actions.forever(Actions.sequence(
                Actions.scaleTo(1.0f, 1.0f, 1.0f),
                Actions.scaleTo(1.02f, 1.02f, 1.0f),
                Actions.scaleTo(1.0f, 1.0f, 1.0f)
            )));

            // Make it clickable
            startButtonImage.addListener(new ClickListener() {
            @Override
                public void clicked(InputEvent event, float x, float y) {
                    onStart();
                }
            });
        } else {
            // Fallback: placeholder for when assets are missing
            startButtonImage = solidPlaceholder(new Color(0.2f, 0.3f, 0.2f, 0.9f), 200, 60);
        }
    }

    private void wireInput() {
        // Simple keyboard input for the new design
        stage.addListener(new InputListener() {
            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    onExit();
                    return true;
                }
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    onStart();
                    return true;
                }
                return false;
            }
        });
    }

    private void onStart() {
        if (startTriggered) {
            return;
        }
        startTriggered = true;
        if (confirmSound != null) {
            confirmSound.play(0.7f);
        }
        // Simple fade out and transition to intro screen
        stage.addAction(Actions.sequence(
            Actions.fadeOut(0.5f),
            Actions.run(() -> game.setScreen(new IntroScreen(game)))
        ));
    }

    private void onExit() {
        Gdx.app.exit();
    }

    @Override
    public void show() {
        stage.getRoot().getColor().a = 0f;
        stage.addAction(Actions.fadeIn(0.5f));
        startTriggered = false;

        // Start background music
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override
    public void hide() {
        // Stop music when leaving the menu
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.stop();
        }
    }
    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        batch.dispose();
        if (menuBackground != null) menuBackground.dispose();
        if (titleNormal != null) titleNormal.dispose();
        if (titleGlitch != null) titleGlitch.dispose();
        if (startButton != null) startButton.dispose();
        if (glitchParticles != null) glitchParticles.dispose();
        if (backgroundMusic != null) backgroundMusic.dispose();
        if (confirmSound != null) confirmSound.dispose();
    }
}
