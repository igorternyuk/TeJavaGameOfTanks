package com.igorternyuk.tanks.gamestate;

import com.igorternyuk.tanks.gameplay.Game;
import com.igorternyuk.tanks.gameplay.GameStatus;
import com.igorternyuk.tanks.gameplay.entities.Entity;
import com.igorternyuk.tanks.gameplay.entities.EntityType;
import com.igorternyuk.tanks.gameplay.entities.player.Player;
import com.igorternyuk.tanks.gameplay.tilemap.TileMap;
import com.igorternyuk.tanks.graphics.images.Background;
import java.awt.Graphics2D;
import com.igorternyuk.tanks.input.KeyboardState;
import com.igorternyuk.tanks.resourcemanager.ImageIdentifier;
import com.igorternyuk.tanks.resourcemanager.ResourceManager;
import com.igorternyuk.tanks.utils.Painter;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    private Background background;
    private Player player;
    private List<Entity> entities = new ArrayList<>();
    private GameStatus gameStatus = GameStatus.PLAY;

    public LevelState(GameStateManager gsm, ResourceManager rm) {
        super(gsm, rm);
        initEntityCreator();
    }
    
    
    private void initEntityCreator() {
        
    }

    public List<Entity> getEntities() {
        return this.entities;
    }

    public TileMap getTileMap() {
        return this.tileMap;
    }

    @Override
    public void load() {
        System.out.println("Level state loading...");
        /*this.resourceManager.loadImage(ImageIdentifier.LEVEL_BACKGROUND,
                "/Backgrounds/grassbg1.gif");
        this.background = new Background(this.resourceManager
                .getImage(ImageIdentifier.LEVEL_BACKGROUND));
        this.tileMap = new TileMap(this.resourceManager, Game.TILE_SIZE);
        this.tileMap.loadTileSet("/Tilesets/grasstileset.gif");
        this.tileMap.loadMap("/Maps/level1.map");
        this.resourceManager.loadImage(ImageIdentifier.PLAYER_SPRITE_SHEET,
                "/Sprites/Player/playerSpriteSheet.png");
        this.resourceManager.loadImage(ImageIdentifier.FIRE_BALL,
                "/Sprites/Player/fireball.gif");
        this.resourceManager.loadImage(ImageIdentifier.SNAIL,
                "/Sprites/Enemies/snail.gif");
        this.resourceManager.loadImage(ImageIdentifier.SPIDER,
                "/Sprites/Enemies/spider.gif");
        this.resourceManager.loadImage(ImageIdentifier.EXPLOSION,
                "/Sprites/Enemies/explosion.gif");
        this.resourceManager.loadImage(ImageIdentifier.HUD,
                "/HUD/hud.gif");
        this.resourceManager.loadImage(ImageIdentifier.POWERUPS,
                "/Sprites/Powerups/powerups.png");
        startNewGame();*/
    }

    private void startNewGame() {
        this.entities.clear();
        createEntities();
        gameStatus = GameStatus.PLAY;
    }
    
    private void createEntities() {
        Map<Point, EntityType> enemyPosiitons = this.tileMap.
                getEntityPositions();
        enemyPosiitons.keySet().forEach((Point point) -> {
            EntityType type = enemyPosiitons.get(point);
            /*if(this.entityCreatorMap.containsKey(type)){
                this.entityCreatorMap.get(type).accept(point);
            }*/
        });
    }

    @Override
    public void unload() {
        this.player = null;
        this.resourceManager.unloadImage(ImageIdentifier.LEVEL_BACKGROUND);
        this.tileMap = null;
    }

    @Override
    public void update(KeyboardState keyboardState, double frameTime) {
        if(this.gameStatus != GameStatus.PLAY || this.player == null)
            return;
        updateEntities(keyboardState, frameTime);
        checkCollisions();
        checkGameStatus();
    }
    
    private void updateEntities(KeyboardState keyboardState, double frameTime){
        //Remove the dead entities
        //this.entities.removeIf(e -> e != this.player && !e.isAlive());

        //Update all entitites
        for (int i = this.entities.size() - 1; i >= 0; --i) {
            //this.entities.get(i).update(keyboardState, frameTime);
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
    public void draw(Graphics2D g) {
        if (this.background != null) {
            this.background.draw(g);
        }

        if (this.tileMap != null) {
            this.tileMap.draw(g);
        }

        for (int i = this.entities.size() - 1; i >= 0; --i) {
           // this.entities.get(i).draw(g);
        }
        
        drawGameStatus(g);
    }
}
