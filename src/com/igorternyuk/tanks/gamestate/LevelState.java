package com.igorternyuk.tanks.gamestate;

import com.igorternyuk.tanks.gameplay.Game;
import com.igorternyuk.tanks.gameplay.GameStatus;
import com.igorternyuk.tanks.gameplay.entities.Entity;
import com.igorternyuk.tanks.gameplay.entities.EntityType;
import com.igorternyuk.tanks.gameplay.entities.explosion.Explosion;
import com.igorternyuk.tanks.gameplay.entities.explosion.ExplosionType;
import com.igorternyuk.tanks.gameplay.entities.splash.Splash;
import com.igorternyuk.tanks.gameplay.entities.splash.SplashType;
import com.igorternyuk.tanks.gameplay.tilemap.TileMap;
import com.igorternyuk.tanks.graphics.images.Background;
import com.igorternyuk.tanks.graphics.images.TextureAtlas;
import com.igorternyuk.tanks.graphics.spritesheets.SpriteSheetIdentifier;
import com.igorternyuk.tanks.graphics.spritesheets.SpriteSheetManager;
import java.awt.Graphics2D;
import com.igorternyuk.tanks.input.KeyboardState;
import com.igorternyuk.tanks.resourcemanager.ImageIdentifier;
import com.igorternyuk.tanks.resourcemanager.ResourceManager;
import com.igorternyuk.tanks.utils.Painter;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author igor
 */
public class LevelState extends GameState {
    private static final Font FONT_GAME_STATUS = new Font("Verdana", Font.BOLD,
            48);
    public static final double SCALE = 2;
    private static final int SCREEN_HALF_WIDTH = (int) (Game.WIDTH / 2 / SCALE);
    private static final int SCREEN_HALF_HEIGHT =
            (int) (Game.HEIGHT / 2 / SCALE);
    private TileMap tileMap;
    private TextureAtlas atlas;
    private SpriteSheetManager spriteSheetManager;
    //private Player player;
    private List<Entity> entities = new ArrayList<>();
    private GameStatus gameStatus = GameStatus.PLAY;
    private boolean loaded = false;

    public LevelState(GameStateManager gsm, ResourceManager rm) {
        super(gsm, rm);
    }

    public SpriteSheetManager getSpriteSheetManager() {
        return this.spriteSheetManager;
    }

    public boolean isLoaded() {
        return loaded;
    }
    
    public TileMap getTileMap() {
        return this.tileMap;
    }
    
    public int getMapWidth(){
        return 13 * 16;
    }
    
    public int getMapHeight(){
        return 13 * 16;
    }
    
    public List<Entity> getEntities() {
        return this.entities;
    }

    @Override
    public void load() {
        System.out.println("Level state loading...");
        this.resourceManager.loadImage(ImageIdentifier.TEXTURE_ATLAS,
                "/images/texture_atlas.png");
        this.atlas = new TextureAtlas(this.resourceManager.getImage(
                ImageIdentifier.TEXTURE_ATLAS));
        this.spriteSheetManager = new SpriteSheetManager();
        loadSprites();
        startNewGame();
        loaded = true;
    }
    
    private void loadSprites(){
        System.out.println("Loading the sprites...");
        BufferedImage image = this.atlas.getAtlas();
        System.out.println("atlas width = " + image.getWidth() + " atlas height = " + image.getHeight());
        for(SpriteSheetIdentifier identifier: SpriteSheetIdentifier.values()){
            this.spriteSheetManager.put(identifier, this.atlas);
        }
    }

    private void startNewGame() {
        this.entities.clear();
        createEntities();
        gameStatus = GameStatus.PLAY;
    }
    
    private void createEntities() {
        Explosion explosion = new Explosion(this, ExplosionType.PROJECTILE,
                64, 64);
        this.entities.add(explosion);
        Splash s = new Splash(this, SplashType.BONUS, 0, 0);
        this.entities.add(s);
    }

    @Override
    public void unload() {
        //this.player = null;
        this.tileMap = null;
    }

    
    
    private void updateEntities(KeyboardState keyboardState, double frameTime){
        //Remove the dead entities
        //this.entities.removeIf(e -> e != this.player && !e.isAlive());

        //Update all entitites
        for (int i = this.entities.size() - 1; i >= 0; --i) {
            this.entities.get(i).update(keyboardState, frameTime);
        }
    }

    private void checkCollisions() {
        
    }

    private void checkGameStatus(){
        /*if(!this.player.isAlive()){
            this.gameStatus = GameStatus.PLAYER_LOST;
        }
        if(getEnemies().isEmpty()){
            this.gameStatus = GameStatus.PLAYER_WON;
        }*/
    }

    @Override
    public void onKeyPressed(int keyCode) {
    }

    @Override
    public void onKeyReleased(int keyCode) {
        switch (keyCode) {
            /*case KeyEvent.VK_F:
                this.player.resetAlreadyFired();
                break;
            case KeyEvent.VK_R:
                this.player.setCanScratch(true);
                break;*/
            case KeyEvent.VK_SPACE:
                togglePause();
                break;
            case KeyEvent.VK_N:
                startNewGame();
                break;
            default:
                break;
        }
    }
    
    private void togglePause(){
        if(this.gameStatus == GameStatus.PLAY){
            this.gameStatus = GameStatus.PAUSED;
        } else if(this.gameStatus == GameStatus.PAUSED){
            this.gameStatus = GameStatus.PLAY;
        }
    }
    
    private void drawGameStatus(Graphics2D g){
        Painter.drawCenteredString(g, this.gameStatus.getDescription(),
                FONT_GAME_STATUS, this.gameStatus.getColor(), Game.HEIGHT / 2);
    }
    
    @Override
    public void update(KeyboardState keyboardState, double frameTime) {
        if(!this.loaded)
            return;
        if(this.gameStatus != GameStatus.PLAY /*|| this.player == null*/)
            return;
        //System.out.println("numEntities.size() = " + this.entities.size());
        updateEntities(keyboardState, frameTime);
        checkCollisions();
        checkGameStatus();
    }
    
    @Override
    public void draw(Graphics2D g) {
        if(!this.loaded)
            return;
        /*if (this.tileMap != null) {
            this.tileMap.draw(g);
        }*/

        for (int i = this.entities.size() - 1; i >= 0; --i) {
            this.entities.get(i).draw(g);
        }

        drawGameStatus(g);
        
    }
}
