package io.github.code_quest;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.code_quest.screens.MenuScreen;

public class Main extends Game {
    public SpriteBatch batch;
    private Screen previousScreen;

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
    public void setScreen(Screen screen) {
        // Store the current screen as previous before changing
        if (this.screen != null) {
            this.previousScreen = this.screen;
        }
        super.setScreen(screen);
    }
    
    /**
     * Gets the previously active screen
     * @return the previous screen, or null if there isn't one
     */
    public Screen getPreviousScreen() {
        return previousScreen;
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
}
