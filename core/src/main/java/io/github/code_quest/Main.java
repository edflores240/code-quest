package io.github.code_quest;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.code_quest.screens.MenuScreen;

public class Main extends Game {
    public SpriteBatch batch;

    @Override
    public void create() {
        batch = new SpriteBatch();
        setScreen(new MenuScreen(this)); // start with main menu
    }

    @Override
    public void render() {
        super.render(); // delegate rendering to the active screen
    }

    @Override
    public void dispose() {
        batch.dispose();
        getScreen().dispose();
    }
}
