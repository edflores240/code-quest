package io.github.code_quest.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import io.github.code_quest.Main;

import java.util.Random;

// Note: We're not importing GameScreen to avoid circular dependency

public class GreenValleyScreen implements Screen {
    private final Main game;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final FitViewport viewport;
    
    // Tiled map variables
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    
    // Textures and assets
    private Texture groundTexture;
    private Texture waterTexture;
    private Texture objectsTexture;
    private TextureRegion[][] groundTiles;
    private TextureRegion[][] waterTiles;
    private TextureRegion[][] objectTiles;
    private Array<Vector2> waterTilesPositions;
    private Array<Vector2> objectPositions;
    private Random random;
    
    // Camera control
    private Vector2 cameraTarget;
    private float cameraLerp = 0.1f; // Smooth camera follow speed
    private float zoomSpeed = 0.1f;
    private float minZoom = 0.5f;
    private float maxZoom = 2.0f;
    
    // Player
    private final String playerName;
    private float playerX, playerY;
    
    // Effects
    private boolean nightMode = false;
    
    public GreenValleyScreen(Main game) {
        this(game, "Player");
    }
    
    public GreenValleyScreen(Main game, String playerName) {
        this.game = game;
        this.playerName = playerName;
        this.random = new Random();
        this.waterTilesPositions = new Array<>();
        this.objectPositions = new Array<>();
        
        // Set up the camera and viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 480, camera);
        viewport.apply();
        
        // Initialize camera target
        cameraTarget = new Vector2(400, 240);
        
        // Initialize batch and font
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.0f);
        
        // Load textures and initialize map
        loadTextures();
        generateMap(100, 100); // Generate a 100x100 tile map
        
        // Set up input processor for camera controls
        Gdx.input.setInputProcessor(new InputAdapter() {
            private Vector2 lastMousePos = new Vector2();
            private boolean isDragging = false;
            
            @Override
            public boolean scrolled(float amountX, float amountY) {
                // Zoom in/out with mouse wheel
                camera.zoom = MathUtils.clamp(camera.zoom + amountY * 0.1f, 0.5f, 2f);
                return true;
            }
            
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.LEFT) {
                    lastMousePos.set(screenX, screenY);
                    isDragging = true;
                    return true;
                }
                return false;
            }
            
            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (button == Input.Buttons.LEFT) {
                    isDragging = false;
                    return true;
                }
                return false;
            }
            
            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isDragging) {
                    // Pan the camera
                    Vector3 worldCoords = camera.unproject(new Vector3(lastMousePos.x, lastMousePos.y, 0));
                    Vector3 newWorldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
                    camera.translate(worldCoords.x - newWorldCoords.x, worldCoords.y - newWorldCoords.y);
                    camera.update();
                    lastMousePos.set(screenX, screenY);
                    return true;
                }
                return false;
            }
        });
    }
    
    @Override
    public void show() {
        // Called when this screen becomes the current screen
    }


    private void loadTextures() {
        try {
            // Load ground texture
            FileHandle groundFile = Gdx.files.absolute("/home/john/GAMEDEV/code_quest/assets/images/craftpix-net-695666-free-undead-tileset-top-down-pixel-art/Tiled_files/Ground_rocks.png");
            if (groundFile.exists()) {
                groundTexture = new Texture(groundFile);
                groundTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                groundTiles = TextureRegion.split(groundTexture, 16, 16);
                Gdx.app.log("GreenValleyScreen", "Ground texture loaded successfully");
            }

            // Load water texture with animation frames
            FileHandle waterFile = Gdx.files.absolute("/home/john/GAMEDEV/code_quest/assets/images/craftpix-net-695666-free-undead-tileset-top-down-pixel-art/Tiled_files/water_detilazation_v2.png");
            if (waterFile.exists()) {
                waterTexture = new Texture(waterFile);
                waterTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                waterTiles = TextureRegion.split(waterTexture, 16, 16);
                Gdx.app.log("GreenValleyScreen", "Water texture loaded successfully");
            }

            // Load objects
            FileHandle objectsFile = Gdx.files.absolute("/home/john/GAMEDEV/code_quest/assets/images/craftpix-net-695666-free-undead-tileset-top-down-pixel-art/Tiled_files/Objects.png");
            if (objectsFile.exists()) {
                objectsTexture = new Texture(objectsFile);
                objectsTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                objectTiles = TextureRegion.split(objectsTexture, 16, 16);
                Gdx.app.log("GreenValleyScreen", "Objects texture loaded successfully");
            }
        } catch (Exception e) {
            Gdx.app.error("GreenValleyScreen", "Error loading textures: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateMap(int width, int height) {
        // Generate water tiles
        for (int x = 0; x < width; x += 5) {
            for (int y = 0; y < height; y += 5) {
                if (random.nextFloat() < 0.3) {
                    waterTilesPositions.add(new Vector2(x * 16, y * 16));
                }
            }
        }

        // Generate random objects (trees, rocks, etc.)
        for (int i = 0; i < 100; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            objectPositions.add(new Vector2(x * 16, y * 16));
        }
    }

    
    @Override
    public void render(float delta) {
        // Clear the screen with a dark color
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Update camera position with smooth follow
        if (cameraTarget != null) {
            camera.position.lerp(new Vector3(cameraTarget.x, cameraTarget.y, 0), cameraLerp);
        }
        camera.update();
        
        // Begin batch
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        
        // Draw ground tiles
        if (groundTiles != null) {
            int tileSize = 16;
            int tilesX = (int)(viewport.getWorldWidth() / tileSize / camera.zoom) + 4;
            int tilesY = (int)(viewport.getWorldHeight() / tileSize / camera.zoom) + 4;
            
            int startX = (int)((camera.position.x - viewport.getWorldWidth() * 0.5f / camera.zoom) / tileSize) - 1;
            int startY = (int)((camera.position.y - viewport.getWorldHeight() * 0.5f / camera.zoom) / tileSize) - 1;
            
            // Draw ground
            for (int x = 0; x < tilesX; x++) {
                for (int y = 0; y < tilesY; y++) {
                    int tileX = (startX + x + 1000) % 26; // Wrap around using modulo
                    int tileY = (startY + y + 1000) % 26;
                    TextureRegion tile = groundTiles[tileX][tileY % groundTiles[0].length];
                    
                    batch.draw(tile, 
                        (startX + x) * tileSize, 
                        (startY + y) * tileSize, 
                        tileSize, tileSize);
                }
            }
            
            // Draw water with animation
            if (waterTiles != null) {
                float animTime = (System.currentTimeMillis() % 2000) / 2000f; // 2 second animation loop
                int frame = (int)(animTime * 4) % 4;
                
                for (Vector2 pos : waterTilesPositions) {
                    if (isInViewport(pos.x, pos.y, 16, 16)) {
                        TextureRegion waterFrame = waterTiles[frame * 2][0]; // Animate water
                        batch.draw(waterFrame, pos.x, pos.y, 16, 16);
                    }
                }
            }
            
            // Draw objects
            if (objectTiles != null) {
                for (Vector2 pos : objectPositions) {
                    if (isInViewport(pos.x, pos.y, 16, 16)) {
                        // Randomly select an object tile (adjust indices based on your tileset)
                        int objX = random.nextInt(5);
                        int objY = random.nextInt(5);
                        batch.draw(objectTiles[objX][objY], pos.x, pos.y, 16, 16);
                    }
                }
            }
        }
        
        batch.end();
        
        // Draw UI elements
        batch.begin();
        font.draw(batch, "Green Valley - Undead Lands", 
            camera.position.x - viewport.getWorldWidth() * 0.4f, 
            camera.position.y + viewport.getWorldHeight() * 0.45f);
            
        font.draw(batch, "WASD/Arrows: Move  |  Mouse Wheel: Zoom  |  ESC: Back", 
            camera.position.x - viewport.getWorldWidth() * 0.45f, 
            camera.position.y - viewport.getWorldHeight() * 0.45f);
            
        batch.end();
        
        // Handle input
        handleInput(delta);
    }
    
    private boolean isInViewport(float x, float y, float width, float height) {
        float viewportWidth = viewport.getWorldWidth() / camera.zoom;
        float viewportHeight = viewport.getWorldHeight() / camera.zoom;
        float cameraLeft = camera.position.x - viewportWidth * 0.5f;
        float cameraBottom = camera.position.y - viewportHeight * 0.5f;
        
        return x + width > cameraLeft && 
               x < cameraLeft + viewportWidth &&
               y + height > cameraBottom && 
               y < cameraBottom + viewportHeight;
    }

    private void handleInput(float delta) {
        // Camera movement with keyboard (faster when zoomed out)
        float moveSpeed = 200f * delta * camera.zoom;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            camera.translate(0, moveSpeed, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            camera.translate(0, -moveSpeed, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            camera.translate(-moveSpeed, 0, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            camera.translate(moveSpeed, 0, 0);
        }
        
        
        // Keep camera in map bounds
        if (map != null) {
            int mapWidth = map.getProperties().get("width", Integer.class) * map.getProperties().get("tilewidth", Integer.class);
            int mapHeight = map.getProperties().get("height", Integer.class) * map.getProperties().get("tileheight", Integer.class);
            
            float cameraHalfWidth = camera.viewportWidth * camera.zoom * 0.5f;
            float cameraHalfHeight = camera.viewportHeight * camera.zoom * 0.5f;
            
            camera.position.x = MathUtils.clamp(camera.position.x, cameraHalfWidth, mapWidth - cameraHalfWidth);
            camera.position.y = MathUtils.clamp(camera.position.y, cameraHalfHeight, mapHeight - cameraHalfHeight);
        }
        
        // ESC to go back
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Main mainGame = (Main) game;
            Screen previousScreen = mainGame.getPreviousScreen();
            if (previousScreen != null) {
                game.setScreen(previousScreen);
            } else {
                // Fallback to creating a new GameScreen if no previous screen is available
                game.setScreen(new GameScreen(mainGame));
            }
        }
    }
    
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (map != null) map.dispose();
        if (renderer != null) renderer.dispose();
        if (groundTexture != null) groundTexture.dispose();
        if (waterTexture != null) waterTexture.dispose();
        if (objectsTexture != null) objectsTexture.dispose();
    }
}
