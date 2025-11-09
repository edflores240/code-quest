package io.github.code_quest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;
import io.github.code_quest.save.SaveData;
import io.github.code_quest.save.SaveManager;

/**
 * Simple screen that lists available save slots and lets the player pick one to resume.
 */
public class LoadSaveScreen implements Screen {

    private static final float VIEWPORT_WIDTH = 800f;
    private static final float VIEWPORT_HEIGHT = 480f;
    private static final Color BACKGROUND_COLOR = new Color(0.05f, 0.05f, 0.08f, 1f);

    private final Main game;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout layout;

    private final Array<FileHandle> saves;
    private int selectedIndex;
    private float keyCooldown;
    private static final float KEY_COOLDOWN = 0.18f;

    public LoadSaveScreen(Main game) {
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, camera);
        camera.setToOrtho(false, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        camera.position.set(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT / 2f, 0f);
        camera.update();

        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);
        this.font.setColor(Color.WHITE);
        this.layout = new GlyphLayout();

        saves = new Array<>(SaveManager.listSaves());
        if (SaveManager.exists() && !containsFile(saves, SaveManager.getActiveFile())) {
            FileHandle active = Gdx.files.local("saves/" + SaveManager.getActiveFile());
            if (active.exists()) {
                saves.add(active);
            }
        }
        if (saves.isEmpty()) {
            // No saves → go back to menu automatically
            Gdx.app.postRunnable(() -> game.setScreen(new MenuScreen(game)));
        }
        selectedIndex = 0;
        keyCooldown = 0f;
    }

    private boolean containsFile(Array<FileHandle> files, String name) {
        for (FileHandle handle : files) {
            if (handle.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        if (keyCooldown > 0f) {
            keyCooldown -= delta;
        }

        handleInput();

        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, BACKGROUND_COLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float startY = VIEWPORT_HEIGHT - 80f;
        float lineHeight = 42f;

        font.setColor(Color.valueOf("9EE493"));
        layout.setText(font, "Select a Save Slot");
        font.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2f, startY);

        startY -= 60f;
        for (int i = 0; i < saves.size; i++) {
            FileHandle handle = saves.get(i);
            String fileName = handle.nameWithoutExtension();
            boolean selected = (i == selectedIndex);

            if (selected) {
                font.setColor(Color.valueOf("FEEA8B"));
            } else {
                font.setColor(Color.LIGHT_GRAY);
            }

            SaveData preview = SaveManager.loadRaw(handle);
            String description;
            if (preview != null) {
                description = String.format("%s - Level %d - Coins %d", fileName, preview.getCurrentLevel(), preview.getCoins());
            } else {
                description = fileName + " - (corrupted)";
            }
            layout.setText(font, description);
            font.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2f, startY - i * lineHeight);
        }

        font.setColor(Color.GRAY);
        font.getData().setScale(0.85f);
        String hint = "ENTER to load, DEL to delete, ESC to cancel";
        layout.setText(font, hint);
        font.draw(batch, layout, (VIEWPORT_WIDTH - layout.width) / 2f, 40f);
        font.getData().setScale(1.2f);

        batch.end();
    }

    private void handleInput() {
        if (saves.isEmpty()) {
            return;
        }

        if ((Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) && keyCooldown <= 0f) {
            selectedIndex = (selectedIndex + 1) % saves.size;
            keyCooldown = KEY_COOLDOWN;
        } else if ((Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) && keyCooldown <= 0f) {
            selectedIndex = (selectedIndex - 1 + saves.size) % saves.size;
            keyCooldown = KEY_COOLDOWN;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            activateSelected();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DEL)) {
            deleteSelected();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.setScreen(new MenuScreen(game));
        }
    }

    private void activateSelected() {
        if (saves.isEmpty()) {
            return;
        }
        FileHandle handle = saves.get(selectedIndex);
        SaveManager.setActiveFile(handle.name());
        SaveData data = SaveManager.load(handle.name());
        if (data == null) {
            return;
        }
        Gdx.app.postRunnable(() -> game.setScreen(new GreenValleyScreen(game, data.getCharacterKey())));
    }

    private void deleteSelected() {
        if (saves.isEmpty()) {
            return;
        }
        FileHandle handle = saves.removeIndex(selectedIndex);
        SaveManager.delete(handle.name());
        if (saves.isEmpty()) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        selectedIndex = Math.min(selectedIndex, saves.size - 1);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
