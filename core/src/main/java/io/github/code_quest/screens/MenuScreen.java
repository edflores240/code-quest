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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;
import io.github.code_quest.entities.actors.CodeRainActor;
import io.github.code_quest.entities.GlitchParticles;

public class MenuScreen implements Screen {
    private final Main game;
    private final Stage stage;
    private final Skin skin;
    private final SpriteBatch batch;

    private Texture bgGreenValley;     // optional art: ui/backgrounds/green_valley.png
    private Texture logoTexture;       // optional art: ui/logo-codequest.png
    private Texture avatarMale;        // optional art: ui/avatars/male.png
    private Texture avatarFemale;      // optional art: ui/avatars/female.png
    private Image avatarImage;

    private boolean femaleSelected = false;

    private Image backgroundImage;
    private CodeRainActor codeRain;
    private GlitchParticles glitchParticles;

    private Table root;
    private TextButton startBtn, loadBtn, settingsBtn, exitBtn;
    private Label subtitleLabel;

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
        buildUI();
        wireInput();
    }

    private void loadOptionalAssets() {
        // Try loading optional textures. If missing, we fallback to solid-color placeholders.
        bgGreenValley = loadIfExists("ui/backgrounds/green_valley.png");
        logoTexture = loadIfExists("ui/logo-codequest.png");
        avatarMale = loadIfExists("ui/avatars/male.png");
        avatarFemale = loadIfExists("ui/avatars/female.png");
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
        // Background: warm glowing digital valley with subtle green/teal tint if art missing
        if (bgGreenValley != null) {
            backgroundImage = new Image(bgGreenValley);
            backgroundImage.setScaling(Scaling.stretch);
        } else {
            backgroundImage = solidPlaceholder(new Color(0.07f, 0.12f, 0.06f, 1f), 8, 8);
        }
        backgroundImage.setFillParent(true);
        stage.addActor(backgroundImage);

        // Subtle code rain behind UI
        codeRain = new CodeRainActor(800, 480, skin.getFont("default-font"), new Color(0.8f, 1f, 0.8f, 0.18f));
        stage.addActor(codeRain);

        // Faint glitch particles
        glitchParticles = new GlitchParticles(800, 480, 30, new Color(0.8f, 1f, 0.8f, 0.25f));
        stage.addActor(glitchParticles);

        // Centered logo is added in the center table below

        // Root layout
        root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Left: avatar
        Table left = new Table(); left.pad(20);

        // Avatar image and glow
        Texture avatarTx = femaleSelected ? avatarFemale : avatarMale;
        Image avatar = (avatarTx != null) ? new Image(avatarTx) : solidPlaceholder(new Color(0.15f, 0.18f, 0.15f, 1f), 96, 128);
        avatar.setScaling(Scaling.fit);

        // Glow overlay using a semi-transparent white square pulsing
        Image glow = solidPlaceholder(new Color(1f, 1f, 1f, 0.12f), 64, 64);
        glow.setColor(1f, 1f, 1f, 0.14f);
        glow.addAction(Actions.forever(Actions.sequence(
            Actions.scaleTo(1.08f, 1.08f, 1.0f),
            Actions.scaleTo(1.0f, 1.0f, 1.0f)
        )));

        Table avatarWrap = new Table();
        avatarWrap.add(glow).size(110, 110).expandX().center().row();
        avatarWrap.add(avatar).size(110, 140).padTop(-90f).center();
        avatarImage = avatar; // keep reference

        // Gender toggle
        TextButton maleBtn = new TextButton("Male", skin);
        TextButton femaleBtn = new TextButton("Female", skin);
        maleBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                femaleSelected = false; updateAvatar();
            }
        });
        femaleBtn.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                femaleSelected = true; updateAvatar();
            }
        });

        left.add(avatarWrap).expand().center().row();
        left.add(maleBtn).padTop(10).left();
        left.add(femaleBtn).padTop(10).left();

        // Center: logo and subtitle
        Table center = new Table(); center.padTop(30);
        if (logoTexture != null) {
            center.add(new Image(logoTexture)).width(420).height(120).padBottom(10).row();
        } else {
            Label.LabelStyle logoStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.valueOf("9EE493"));
            Label logoLbl = new Label("CodeQuest", logoStyle);
            logoLbl.setFontScale(2.2f);
            logoLbl.setAlignment(Align.center);
            center.add(logoLbl).padBottom(10).row();
        }

        subtitleLabel = new Label("Green Valley — a warm digital meadow. Faint glitches drift in the wind...", skin);
        subtitleLabel.setColor(Color.valueOf("B6F6C1"));
        center.add(subtitleLabel).padBottom(20).row();

        // Right: menu options
        Table right = new Table(); right.pad(20);

        startBtn = new TextButton("Start", skin);
        loadBtn = new TextButton("Load Game", skin);
        settingsBtn = new TextButton("Settings", skin);
        exitBtn = new TextButton("Exit", skin);

        ChangeListener menuHandler = new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (actor == startBtn) onStart();
                else if (actor == loadBtn) onLoad();
                else if (actor == settingsBtn) onSettings();
                else if (actor == exitBtn) onExit();
            }
        };
        startBtn.addListener(menuHandler);
        loadBtn.addListener(menuHandler);
        settingsBtn.addListener(menuHandler);
        exitBtn.addListener(menuHandler);

        float w = 220f, h = 44f, pad = 10f;
        right.add(startBtn).width(w).height(h).pad(pad).row();
        right.add(loadBtn).width(w).height(h).pad(pad).row();
        right.add(settingsBtn).width(w).height(h).pad(pad).row();
        right.add(exitBtn).width(w).height(h).pad(pad).row();

        // Compose root: three columns
        root.add(left).width(220).growY();
        root.add(center).grow().center();
        root.add(right).width(260).growY();

        // Input processor
        Gdx.input.setInputProcessor(stage);
    }

    private void updateAvatar() {
        Texture avatarTx = femaleSelected ? avatarFemale : avatarMale;
        if (avatarTx != null) {
            avatarImage.setDrawable(new Image(avatarTx).getDrawable());
        }
    }

    private void wireInput() {
        // Keyboard navigation: Up/Down to move focus, Enter/Space to activate, Esc to exit
        final TextButton[] buttons = new TextButton[] { startBtn, loadBtn, settingsBtn, exitBtn };
        stage.setKeyboardFocus(startBtn);

        stage.addListener(new InputListener() {
            private int focusedIndex = 0;

            private void focusIndex(int idx) {
                focusedIndex = ((idx % buttons.length) + buttons.length) % buttons.length;
                stage.setKeyboardFocus(buttons[focusedIndex]);
            }

            @Override public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    onExit();
                    return true;
                }
                if (keycode == Input.Keys.UP) {
                    focusIndex(focusedIndex - 1);
                    return true;
                }
                if (keycode == Input.Keys.DOWN) {
                    focusIndex(focusedIndex + 1);
                    return true;
                }
                if (keycode == Input.Keys.ENTER || keycode == Input.Keys.SPACE) {
                    // Programmatically fire the button's ChangeListener
                    buttons[focusedIndex].toggle();
                    return true;
                }
                return false;
            }
        });
    }

    private void onStart() {
        // Fade out and transition to intro scene
        stage.addAction(Actions.sequence(
            Actions.fadeOut(0.5f),
            Actions.run(() -> game.setScreen(new IntroScreen(game, femaleSelected)))
        ));
    }

    private void onLoad() {
        // TODO: implement load flow
        subtitleLabel.setText("Load feature coming soon. Prepare your save slots!");
        subtitleLabel.addAction(Actions.sequence(Actions.alpha(0.6f, 0.15f), Actions.alpha(1f, 0.35f)));
    }

    private void onSettings() {
        // TODO: implement settings screen
        subtitleLabel.setText("Settings incoming: keybinds, audio, pixel scaling, and more.");
        subtitleLabel.addAction(Actions.sequence(Actions.alpha(0.6f, 0.15f), Actions.alpha(1f, 0.35f)));
    }

    private void onExit() {
        Gdx.app.exit();
    }

    @Override
    public void show() {
        stage.getRoot().getColor().a = 0f;
        stage.addAction(Actions.fadeIn(0.5f));
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
    @Override public void hide() {}
    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        batch.dispose();
        if (bgGreenValley != null) bgGreenValley.dispose();
        if (logoTexture != null) logoTexture.dispose();
        if (avatarMale != null) avatarMale.dispose();
        if (avatarFemale != null) avatarFemale.dispose();
    }
}