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
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;
import io.github.code_quest.entities.ModernGlitchParticles;
import io.github.code_quest.save.SaveManager;

public class MenuScreen implements Screen {
    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final SpriteBatch batch;

    private Texture menuBackground;    // assets/images/menu-background.png
    private Texture titleNormal;       // assets/images/menu-title.png
    private Texture titleGlitch;      // assets/images/menu-title-glitch.png
    private Texture startButton;      // assets/images/start-button.png
    private Texture loadButtonTexture; // assets/images/LOADBUTTON.png
    private Texture arrowLeftTexture;   // assets/images/arrowpointingleftonbutton.png
    private Texture arrowRightTexture;  // assets/images/arrowpointingrightonbutton.png

    private Image backgroundImage;
    private Image titleImage;
    private Image startButtonImage;
    private Image loadButtonImage;
    private Image startLeftArrow;
    private Image startRightArrow;
    private Image loadLeftArrow;
    private Image loadRightArrow;

    private Table root;
    private TextButton startBtn;
    private ModernGlitchParticles glitchParticles;
    private Music backgroundMusic;
    private Sound confirmSound;
    private boolean startTriggered;
    private TextButton loadButtonTextButton;
    private Actor startOptionActor;
    private Actor loadOptionActor;
    private int selectedOption;

    private static final int OPTION_START = 0;
    private static final int OPTION_LOAD = 1;
    private static final float ARROW_SIZE = 44f;
    private static final float ARROW_NEAR_PAD = 1f;
    private static final float ARROW_FAR_PAD = 4f;
    private static final float ARROW_VERTICAL_PAD = 1f;
    private static final float ARROW_ANIM_DURATION = 0.45f;
    private static final float ARROW_ANIM_DELAY = 0.2f;

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
        loadButtonTexture = loadIfExists("images/LOADBUTTON.png");
        arrowLeftTexture = loadIfExists("images/arrowpointingleftonbutton.png");
        arrowRightTexture = loadIfExists("images/arrowpointingrightonbutton.png");
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
            LabelStyle titleStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.valueOf("9EE493"));
            Label titleLabel = new Label("CodeQuest", titleStyle);
            titleLabel.setFontScale(2.0f);
            titleLabel.setAlignment(Align.center);
            root.add(titleLabel).padBottom(40).row();

        }
        Actor startActor;
        float startWidth;
        float startHeight;
        if (startButton != null && startButtonImage != null) {
            startActor = startButtonImage;
            startWidth = 200f;
            startHeight = 80f;
        } else {
            startBtn = new TextButton("START", skin);
            startBtn.setColor(Color.valueOf("9EE493"));
            startBtn.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    setSelectedOption(OPTION_START);
                    onStart();
                }
            });
            startActor = startBtn;
            startWidth = 150f;
            startHeight = 50f;
        }
        startOptionActor = startActor;
        startLeftArrow = createArrowImage(arrowRightTexture);
        startRightArrow = createArrowImage(arrowLeftTexture);
        Table startRow = new Table();
        addArrowCell(startRow, startLeftArrow, true);
        startRow.add(startActor).size(startWidth, startHeight);
        addArrowCell(startRow, startRightArrow, false);
        root.add(startRow).padBottom(6f).row();

        Actor loadActor;
        float loadWidth;
        float loadHeight;
        if (loadButtonTexture != null) {
            loadButtonImage = new Image(loadButtonTexture);
            loadButtonImage.setScaling(Scaling.stretch);
            loadButtonImage.setSize(140f, 56f);
            loadButtonImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    setSelectedOption(OPTION_LOAD);
                    onLoadSave();
                }
            });
            loadActor = loadButtonImage;
            loadWidth = 140f;
            loadHeight = 56f;
        } else {
            loadButtonTextButton = new TextButton("LOAD SAVE", skin);
            loadButtonTextButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    setSelectedOption(OPTION_LOAD);
                    onLoadSave();
                }
            });
            loadActor = loadButtonTextButton;
            loadWidth = 150f;
            loadHeight = 48f;
        }
        loadOptionActor = loadActor;
        loadLeftArrow = createArrowImage(arrowRightTexture);
        loadRightArrow = createArrowImage(arrowLeftTexture);
        Table loadRow = new Table();
        addArrowCell(loadRow, loadLeftArrow, true);
        loadRow.add(loadActor).size(loadWidth, loadHeight);
        addArrowCell(loadRow, loadRightArrow, false);
        root.add(loadRow).padBottom(4f).row();
        root.add().expandY();

        // Input processor
        Gdx.input.setInputProcessor(stage);

        updateLoadButtonState(hasAvailableSaves());
        setSelectedOption(OPTION_START);
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

            // Make it clickable
            startButtonImage.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    setSelectedOption(OPTION_START);
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
                    triggerSelectedOption();
                    return true;
                }
                if (keycode == Input.Keys.UP || keycode == Input.Keys.W || keycode == Input.Keys.LEFT) {
                    changeSelection(-1);
                    return true;
                }
                if (keycode == Input.Keys.DOWN || keycode == Input.Keys.S || keycode == Input.Keys.RIGHT) {
                    changeSelection(1);
                    return true;
                }
                if (keycode == Input.Keys.L) {
                    setSelectedOption(OPTION_LOAD);
                    triggerSelectedOption();
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
        SaveManager.createNewSlot();
        if (confirmSound != null) {
            confirmSound.play(0.7f);
        }
        // Simple fade out and transition to intro screen
        stage.addAction(Actions.sequence(
            Actions.fadeOut(0.5f),
            Actions.run(() -> game.setScreen(new IntroScreen(game)))
        ));
    }

    private void onLoadSave() {
        if (!hasAvailableSaves()) {
            updateLoadButtonState(false);
            return;
        }
        if (confirmSound != null) {
            confirmSound.play(0.7f);
        }
        stage.addAction(Actions.sequence(
                Actions.fadeOut(0.3f),
                Actions.run(() -> game.setScreen(new LoadSaveScreen(game)))
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

        updateLoadButtonState(hasAvailableSaves());
    }

    private boolean hasAvailableSaves() {
        return SaveManager.exists() || SaveManager.listSaves().length > 0;
    }

    private void updateLoadButtonState(boolean enabled) {
        if (loadButtonTextButton != null) {
            loadButtonTextButton.setDisabled(!enabled);
            loadButtonTextButton.setVisible(true);
        }
        if (loadButtonImage != null) {
            loadButtonImage.setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
            loadButtonImage.getColor().a = enabled ? 1f : 0.4f;
        }
        if (!enabled && selectedOption == OPTION_LOAD) {
            setSelectedOption(OPTION_START);
        } else {
            updateSelectedVisuals();
        }
    }

    private void changeSelection(int delta) {
        if (delta == 0) {
            return;
        }
        int option = selectedOption;
        if (delta > 0) {
            option = selectedOption == OPTION_START && hasAvailableSaves() ? OPTION_LOAD : OPTION_START;
        } else {
            option = selectedOption == OPTION_LOAD ? OPTION_START : (hasAvailableSaves() ? OPTION_LOAD : OPTION_START);
        }
        setSelectedOption(option);
    }

    private void triggerSelectedOption() {
        if (selectedOption == OPTION_LOAD) {
            onLoadSave();
        } else {
            onStart();
        }
    }

    private void setSelectedOption(int option) {
        if (option == OPTION_LOAD && !hasAvailableSaves()) {
            option = OPTION_START;
        }
        if (selectedOption == option) {
            updateSelectedVisuals();
            return;
        }
        selectedOption = option;
        updateSelectedVisuals();
    }

    private void updateSelectedVisuals() {
        boolean startSelected = selectedOption == OPTION_START;
        boolean loadSelected = selectedOption == OPTION_LOAD && hasAvailableSaves();

        updateArrowAnimation(startLeftArrow, startSelected, true);
        updateArrowAnimation(startRightArrow, startSelected, false);
        updateArrowAnimation(loadLeftArrow, loadSelected, true);
        updateArrowAnimation(loadRightArrow, loadSelected, false);

        applySelectionStyle(startOptionActor, startSelected);
        applySelectionStyle(loadOptionActor, loadSelected && hasAvailableSaves());
    }

    private void updateArrowAnimation(Image arrow, boolean selected, boolean leftSide) {
        if (arrow == null) {
            return;
        }
        arrow.clearActions();
        arrow.setScale(1f);
        arrow.clearActions();
        arrow.setScale(1f);
        float width = arrow.getWidth() > 0f ? arrow.getWidth() : arrow.getPrefWidth();
        float height = arrow.getHeight() > 0f ? arrow.getHeight() : arrow.getPrefHeight();
        float originX = leftSide ? width : 0f;
        float originY = height * 0.5f;
        arrow.setOrigin(originX, originY);
        arrow.setScale(1f);

        if (selected) {
            arrow.setVisible(true);
            arrow.addAction(Actions.sequence(
                    Actions.delay(ARROW_ANIM_DELAY),
                    Actions.forever(Actions.sequence(
                            Actions.parallel(
                                    Actions.scaleTo(1.1f, 1.08f, ARROW_ANIM_DURATION)
                            ),
                            Actions.parallel(
                                    Actions.scaleTo(0.94f, 0.96f, ARROW_ANIM_DURATION)
                            )
                    ))
            ));
        } else {
            arrow.setVisible(false);
            arrow.setScale(1f);
        }
    }

    private void applySelectionStyle(Actor actor, boolean selected) {
        if (actor == null) {
            return;
        }
        if (actor instanceof TextButton) {
            ((TextButton) actor).setColor(selected ? Color.valueOf("9EE493") : Color.LIGHT_GRAY);
        } else {
            actor.setColor(selected ? Color.WHITE : new Color(0.9f, 0.9f, 0.9f, 1f));
        }
    }

    private Image createArrowImage(Texture texture) {
        Image image;
        if (texture != null) {
            image = new Image(texture);
        } else {
            image = solidPlaceholder(new Color(0f, 0f, 0f, 0f), 1, 1);
        }
        image.setVisible(false);
        image.setScaling(Scaling.fit);
        return image;
    }

    private void addArrowCell(Table row, Image arrow, boolean left) {
        if (row == null || arrow == null) {
            return;
        }
        float padLeft = left ? ARROW_FAR_PAD : ARROW_NEAR_PAD;
        float padRight = left ? ARROW_NEAR_PAD : ARROW_FAR_PAD;
        row.add(arrow)
                .size(ARROW_SIZE, ARROW_SIZE)
                .padLeft(padLeft)
                .padRight(padRight)
                .padTop(ARROW_VERTICAL_PAD)
                .padBottom(ARROW_VERTICAL_PAD);
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
