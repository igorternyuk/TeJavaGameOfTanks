package com.igorternyuk.tanks.gamestate;

import com.igorternyuk.tanks.gameplay.Game;
import com.igorternyuk.tanks.gameplay.GameStatus;
import com.igorternyuk.tanks.gameplay.entities.Direction;
import com.igorternyuk.tanks.gameplay.entities.Entity;
import com.igorternyuk.tanks.gameplay.entities.EntityManager;
import com.igorternyuk.tanks.gameplay.entities.bonuses.Bonus;
import com.igorternyuk.tanks.gameplay.entities.bonuses.BonusType;
import com.igorternyuk.tanks.gameplay.entities.explosion.Explosion;
import com.igorternyuk.tanks.gameplay.entities.explosion.ExplosionType;
import com.igorternyuk.tanks.gameplay.entities.player.Player;
import com.igorternyuk.tanks.gameplay.entities.player.PlayerTankIdentifier;
import com.igorternyuk.tanks.gameplay.entities.player.PlayerTankType;
import com.igorternyuk.tanks.gameplay.entities.tank.Alliance;
import com.igorternyuk.tanks.gameplay.entities.tank.Heading;
import com.igorternyuk.tanks.gameplay.entities.tank.TankColor;
import com.igorternyuk.tanks.gameplay.entities.tank.enemytank.EnemyTank;
import com.igorternyuk.tanks.gameplay.entities.tank.enemytank.EnemyTankIdentifier;
import com.igorternyuk.tanks.gameplay.entities.tank.enemytank.EnemyTankType;
import com.igorternyuk.tanks.gameplay.entities.tank.protection.Protection;
import com.igorternyuk.tanks.gameplay.entities.tank.protection.ProtectionType;
import com.igorternyuk.tanks.gameplay.tilemap.TileMap;
import com.igorternyuk.tanks.graphics.images.TextureAtlas;
import com.igorternyuk.tanks.graphics.spritesheets.SpriteSheetIdentifier;
import com.igorternyuk.tanks.graphics.spritesheets.SpriteSheetManager;
import java.awt.Graphics2D;
import com.igorternyuk.tanks.input.KeyboardState;
import com.igorternyuk.tanks.resourcemanager.ImageIdentifier;
import com.igorternyuk.tanks.utils.Painter;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
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
    
    private Map<EnemyTankIdentifier, BufferedImage> enemyTankSpriteSheetMap;
    private Map<PlayerTankIdentifier, BufferedImage> playerSpriteSheetMap;
    private SpriteSheetManager spriteSheetManager;
    
    private TileMap tileMap;
    private TextureAtlas atlas;
    
    private Player player;
    private EntityManager entityManager;
    private GameStatus gameStatus = GameStatus.PLAY;
    private boolean loaded = false;

    public LevelState(GameStateManager gsm) {
        super(gsm);
        this.entityManager = new EntityManager();
    }

    public Map<EnemyTankIdentifier, BufferedImage> getEnemyTankSpriteSheetMap() {
        return this.enemyTankSpriteSheetMap;
    }

    public Map<PlayerTankIdentifier, BufferedImage> getPlayerSpriteSheetMap() {
        return this.playerSpriteSheetMap;
    }

    public SpriteSheetManager getSpriteSheetManager() {
        return this.spriteSheetManager;
    }

    /*public Player getPlayer() {
        return this.player;
    }*/

    public GameStatus getGameStatus() {
        return this.gameStatus;
    }
    
    public boolean isLoaded() {
        return this.loaded;
    }

    public TileMap getTileMap() {
        return this.tileMap;
    }

    public int getMapWidth() {
        return 13 * 16;
    }

    public int getMapHeight() {
        return 13 * 16;
    }

    public List<Entity> getEntities() {
        return this.entityManager.getAllEntities();
    }

    @Override
    public void load() {
        System.out.println("Level state loading...");
        this.resourceManager.loadImage(ImageIdentifier.TEXTURE_ATLAS,
                "/images/texture_atlas.png");
        this.atlas = new TextureAtlas(this.resourceManager.getImage(
                ImageIdentifier.TEXTURE_ATLAS));
        loadSprites();
        loadTankSpriteSheetMaps();
        startNewGame();
        this.loaded = true;
    }

    private void loadSprites() {
        this.spriteSheetManager = SpriteSheetManager.getInstance();
        for (SpriteSheetIdentifier identifier : SpriteSheetIdentifier.values()) {
            this.spriteSheetManager.put(identifier, this.atlas);
        }
    }
    
    private void loadTankSpriteSheetMaps(){
        
        this.enemyTankSpriteSheetMap = new HashMap<>();
        this.playerSpriteSheetMap = new HashMap<>();
        for (TankColor color : TankColor.values()) {
            for (Alliance alliance : Alliance.values()) {
                Point topLeft = color.
                        getOffsetFromTankSpriteSheetTopLeftCorner();
                topLeft.x += alliance.
                        getOffsetFromSameColorTankSpriteSheetTopLeftCorner().x;
                topLeft.y += alliance.
                        getOffsetFromSameColorTankSpriteSheetTopLeftCorner().y;
                
                for (Heading heading : Heading.values()) {
                    for (EnemyTankType type : EnemyTankType.values()) {
                        int dx = heading.getSpriteSheetPositionX();
                        int dy = type.getSpriteSheetPositionY();
                        EnemyTankIdentifier key = new EnemyTankIdentifier(color,
                                heading, type);
                        BufferedImage spriteSheet = spriteSheetManager.get(
                                SpriteSheetIdentifier.TANK);
                        BufferedImage sprite = spriteSheet.getSubimage(topLeft.x
                                + dx, topLeft.y + dy,
                                2 * Game.TILE_SIZE, Game.TILE_SIZE);
                        enemyTankSpriteSheetMap.put(key, sprite);
                    }
                    
                    for (PlayerTankType type : PlayerTankType.values()) {
                        int dx = heading.getSpriteSheetPositionX();
                        int dy = type.getSpriteSheetPositionY();
                        PlayerTankIdentifier key = new PlayerTankIdentifier(color,
                                heading, type);
                        SpriteSheetManager manager = SpriteSheetManager.
                                getInstance();
                        BufferedImage spriteSheet = manager.get(
                                SpriteSheetIdentifier.TANK);
                        BufferedImage sprite = spriteSheet.getSubimage(topLeft.x
                                + dx, topLeft.y + dy,
                                2 * Game.TILE_SIZE, Game.TILE_SIZE);
                        playerSpriteSheetMap.put(key, sprite);
                    }                        
                }
            }
        }
    
    }

    private void startNewGame() {
        this.entityManager.removeAllEntities();
        createEntities();
        gameStatus = GameStatus.PLAY;
    }

    private void createEntities() {
        Explosion explosion = new Explosion(this, ExplosionType.TANK,
                64, 64);
        this.entityManager.addEntity(explosion);
        /*Splash s = new Splash(this, SplashType.BONUS, 0, 0);
        this.entities.add(s);*/

        Bonus bonus = new Bonus(this, BonusType.CLOCK, 13 * 5, 13 * 2);
        bonus.startInfiniteBlinking(0.25);
        this.entityManager.addEntity(bonus);
        
        Bonus bonus1 = new Bonus(this, BonusType.GRENADE, 13 * 7, 13 * 2);
        bonus1.startInfiniteBlinking(0.25);
        this.entityManager.addEntity(bonus1);
        
        Bonus bonus2 = new Bonus(this, BonusType.GUN, 13 * 9, 13 * 2);
        bonus2.startInfiniteBlinking(0.25);
        this.entityManager.addEntity(bonus2);

        /*EnemyTankCountIndicator indicator = new EnemyTankCountIndicator(this,
                0, 0);
        indicator.setTankCount(17);
        this.entities.add(indicator);
         */
        Protection protection = new Protection(this, ProtectionType.REGULAR,
                13 * 7, 13 * 7);
        //this.entities.add(protection);

        /*Projectile projectile = new Projectile(this, ProjectileType.PLAYER, 13
                * 2, 13 * 2, 0, Direction.WEST);
        this.entities.add(projectile);*/
        Player tanque = new Player(this, PlayerTankType.HEAVY, 13 * 9, 13 * 9,
                Direction.NORTH);
        this.player = tanque;
        tanque.attachChild(protection);
        this.entityManager.addEntity(tanque);
        
        EnemyTank tank = new EnemyTank(this, EnemyTankType.HEAVY, 13 * 11, 13 * 11,
                Direction.NORTH);
        tank.setGleaming(true);
        this.entityManager.addEntity(tank);
        //TODO create EntityManager
    }

    @Override
    public void unload() {
        //this.player = null;
        this.tileMap = null;
    }

    private void checkCollisions() {

    }

    private void checkGameStatus() {
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
            case KeyEvent.VK_SPACE:
                togglePause();
                break;
            case KeyEvent.VK_N:
                startNewGame();
                break;
            case KeyEvent.VK_F:
                this.player.setCanFire(true);
                break;
            default:
                break;
        }
    }

    private void togglePause() {
        if (this.gameStatus == GameStatus.PLAY) {
            this.gameStatus = GameStatus.PAUSED;
        } else if (this.gameStatus == GameStatus.PAUSED) {
            this.gameStatus = GameStatus.PLAY;
        }
    }

    private void drawGameStatus(Graphics2D g) {
        Painter.drawCenteredString(g, this.gameStatus.getDescription(),
                FONT_GAME_STATUS, this.gameStatus.getColor(), Game.HEIGHT / 2);
    }

    @Override
    public void update(KeyboardState keyboardState, double frameTime) {
        if (!this.loaded) {
            return;
        }
        if (this.gameStatus != GameStatus.PLAY /*|| this.player == null*/) {
            return;
        }
        //System.out.println("numEntities.size() = " + this.entities.size());
        this.entityManager.update(keyboardState, frameTime);
        checkCollisions();
        checkGameStatus();
    }

    @Override
    public void draw(Graphics2D g) {
        if (!this.loaded) {
            return;
        }
        /*if (this.tileMap != null) {
            this.tileMap.draw(g);
        }*/
        this.entityManager.draw(g);
        drawGameStatus(g);

    }
}
