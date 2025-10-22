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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;
import io.github.code_quest.entities.actors.CodeRainActor;

public class IntroScreen implements Screen {
    private final Main game;
    private final boolean female;
    private final Stage stage;
    private final Skin skin;
    private final SpriteBatch batch;

    private Texture bgCorrupted;   // optional art: ui/backgrounds/corrupted_biome.png
    private Texture enemyBug;      // optional art: ui/enemies/bug.png
    private Texture avatarMale;    // optional art: ui/avatars/male.png
    private Texture avatarFemale;  // optional art: ui/avatars/female.png

    public IntroScreen(Main game, boolean femaleSelected) {
        this.game = game;
        this.female = femaleSelected;
        this.batch = new SpriteBatch();
        this.stage = new Stage(new FitViewport(800, 480), batch);
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        
        // FIX: ensure default-font exists
        if (!skin.has("default-font", BitmapFont.class)) {
            skin.add("default-font", new BitmapFont());
        }
        
        loadOptionalAssets();
        buildUI();
        Gdx.input.setInputProcessor(stage);
    }

    private void loadOptionalAssets() {
        bgCorrupted = loadIfExists("ui/backgrounds/corrupted_biome.png");
        enemyBug = loadIfExists("ui/enemies/bug.png");
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
        // Dark corrupted biome with red static glitches mood
        Image bg;
        if (bgCorrupted != null) {
            bg = new Image(bgCorrupted);
            bg.setScaling(Scaling.stretch);
        } else {
            bg = solidPlaceholder(new Color(0.05f, 0.0f, 0.02f, 1f), 8, 8);
        }
        bg.setFillParent(true);
        stage.addActor(bg);

        // Red-tinted code rain, slower, for ominous feel
        CodeRainActor redRain = new CodeRainActor(800, 480, skin.getFont("default-font"), new Color(1f, 0.4f, 0.4f, 0.20f));
        stage.addActor(redRain);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Left: Player avatar
        Image avatar = (female ? avatarFemale : avatarMale) != null
                ? new Image(female ? avatarFemale : avatarMale)
                : solidPlaceholder(new Color(0.18f, 0.18f, 0.18f, 1f), 96, 128);

        // Right: Enemy bug
        Image bug = enemyBug != null
                ? new Image(enemyBug)
                : solidPlaceholder(new Color(0.6f, 0.1f, 0.1f, 1f), 96, 96);

        // Middle-top: holographic message
        Label.LabelStyle holoStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.valueOf("D2F1FF"));
        Label holo = new Label("Welcome to the Digital Realm. The system is infected.\nOnly your Java skills can save it.", holoStyle);
        holo.setAlignment(com.badlogic.gdx.utils.Align.center);
        holo.setColor(0.8f, 1f, 1f, 0.88f);
        holo.addAction(Actions.forever(Actions.sequence(
            Actions.alpha(0.7f, 0.6f), Actions.alpha(0.9f, 0.6f)
        )));

        // Ground strip to suggest half-natural, half-digital terrain
        Image groundNatural = solidPlaceholder(new Color(0.1f, 0.2f, 0.1f, 1f), 8, 8);
        groundNatural.setColor(0.12f, 0.22f, 0.12f, 1f);
        Image groundDigital = solidPlaceholder(new Color(0.5f, 0.0f, 0.0f, 1f), 8, 8);
        groundDigital.setColor(0.8f, 0.1f, 0.1f, 1f);

        Table top = new Table();
        top.add(holo).padTop(30).padBottom(10).growX().center();

        Table middle = new Table();
        middle.add(avatar).size(140, 160).expand().right().padRight(30);
        middle.add(bug).size(120, 120).expand().left().padLeft(30);

        Table bottom = new Table();
        bottom.add(groundNatural).height(30).growX().expandX().width(400);
        bottom.add(groundDigital).height(30).growX().expandX().width(400);

        root.top().pad(10);
        root.add(top).growX().row();
        root.add(middle).expand().row();
        root.add(bottom).growX();

        // Continue on any key / click
        Label cont = new Label("Press Enter to continue...", skin);
        cont.setColor(Color.valueOf("F2C0C0"));
        cont.addAction(Actions.forever(Actions.sequence(
            Actions.fadeOut(0.6f), Actions.fadeIn(0.6f)
        )));
        root.row();
        root.add(cont).pad(10);

        stage.getRoot().getColor().a = 0f;
        stage.addAction(Actions.fadeIn(0.6f));
    }

    @Override public void show() {}
    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.justTouched()) {
            // TODO: switch to the first gameplay/level screen later
            stage.addAction(Actions.sequence(
                Actions.fadeOut(0.4f),
                Actions.run(() -> game.setScreen(new MenuScreen(game)))
            ));
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int width, int height) { stage.getViewport().update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        stage.dispose();
        skin.dispose();
        batch.dispose();
        if (bgCorrupted != null) bgCorrupted.dispose();
        if (enemyBug != null) enemyBug.dispose();
        if (avatarMale != null) avatarMale.dispose();
        if (avatarFemale != null) avatarFemale.dispose();
    }
}