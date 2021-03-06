package com.igorternyuk.tanks.gameplay.entities.tank.enemytank;

import com.igorternyuk.tanks.gameplay.Game;
import com.igorternyuk.tanks.gameplay.entities.Direction;
import com.igorternyuk.tanks.gameplay.entities.Entity;
import com.igorternyuk.tanks.gameplay.entities.EntityType;
import com.igorternyuk.tanks.gameplay.entities.bonuses.PowerUp;
import com.igorternyuk.tanks.gameplay.entities.player.Player;
import com.igorternyuk.tanks.gameplay.entities.player.PlayerIdentifier;
import com.igorternyuk.tanks.gameplay.entities.player.PlayerTankType;
import com.igorternyuk.tanks.gameplay.entities.projectiles.Projectile;
import com.igorternyuk.tanks.gameplay.entities.projectiles.ProjectileType;
import com.igorternyuk.tanks.gameplay.entities.rockets.Rocket;
import com.igorternyuk.tanks.gameplay.entities.rockets.RocketType;
import com.igorternyuk.tanks.gameplay.entities.tank.Heading;
import com.igorternyuk.tanks.gameplay.entities.tank.ShootingMode;
import com.igorternyuk.tanks.gameplay.entities.tank.Tank;
import com.igorternyuk.tanks.gameplay.entities.tank.TankColor;
import com.igorternyuk.tanks.gameplay.entities.text.ScoreIcrementText;
import com.igorternyuk.tanks.gameplay.pathfinder.Pathfinder;
import com.igorternyuk.tanks.gameplay.pathfinder.Pathfinder.Spot;
import com.igorternyuk.tanks.gameplay.tilemap.TileMap.FiringSpot;
import com.igorternyuk.tanks.gamestate.LevelState;
import com.igorternyuk.tanks.graphics.animations.Animation;
import com.igorternyuk.tanks.graphics.animations.AnimationPlayMode;
import com.igorternyuk.tanks.input.KeyboardState;
import com.igorternyuk.tanks.resourcemanager.AudioIdentifier;
import com.igorternyuk.tanks.resourcemanager.ResourceManager;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 *
 * @author igor
 */
public class EnemyTank extends Tank<EnemyTankIdentifier> {

    private static final int[] BONUS_TANKS_NUMBERS = {4, 11, 18};
    private static final int TANK_DIMENSION = 2;
    private static final double COLOR_CHANGING_PERIOD = 0.15;
    private static final double TARGET_CHANGING_PERIOD = 10;
    private double shootingPeriod = 2;

    private int number;
    private EnemyTankIdentifier tankId;
    private boolean bonus = false;
    private boolean gleaming = false;
    private double gleamingTimer;

    private boolean redBlinking = false;
    private double redBlinkingTimer = 0;

    private boolean movingAlongShortestPath = false;
    private List<Spot> shortestPath = new ArrayList<>();
    private Spot nextPosition;

    private Spot currTarget;
    private double targetTimer = 0;
    private List<FiringSpot> firingSpots;
    private boolean gotStuck = false;
    private double shootingTimer = 0;
    private boolean firingSpotReached = false;
    private final Random random = new Random();

    public EnemyTank(LevelState level, int number, EnemyTankType type, double x,
            double y, Direction direction) {
        super(level, EntityType.ENEMY_TANK, x, y, type.getSpeed(), direction);
        this.number = number;
        this.health = type.getHealth();
        this.tankId = new EnemyTankIdentifier(getTankColor(type),
                Heading.getHeading(direction), type);
        checkIfSpecialTank();
        loadAnimations();
        updateAnimation();
        this.firingSpots = getFiringSpotsToAttackTheEagle();
        selectRandomFiringPointToAttackEagle();
        this.moving = true;
    }

    @Override
    public void update(KeyboardState keyboardState, double frameTime) {
        super.update(keyboardState, frameTime);
        updateGleamingColor(frameTime);
        updateRedBlinking(frameTime);
        executeMovementLogic(frameTime);
        updateShootingTimer(frameTime);
        updateAnimation();
    }

    @Override
    public void draw(Graphics2D g) {
        super.draw(g);
        this.shortestPath.forEach(spot -> spot.draw(g));
    }
    
    public void setShootingMode(ShootingMode shootingMode){
        this.shootingMode = shootingMode;
    }

    public boolean isBonus() {
        return this.bonus;
    }

    private void checkIfSpecialTank() {
        if (!checkIfBonus()) {
            if (this.random.nextDouble() < calcRedBlinkingProbability()) {
                if (this.tankId.getType() != EnemyTankType.ARMORED) {
                    turnRed();
                }
            }
        }
    }

    private double calcRedBlinkingProbability() {
        return (1.25 * this.level.getStageNumber() + 8.75) / 100;
    }

    public final void turnRed() {
        int shotsToKillRequired = this.random.nextInt(5) + 2;
        this.health = shotsToKillRequired * PlayerTankType.BASIC.
                getProjectileDamage();
        this.redBlinking = true;
    }

    public void executeMovementLogic(double frameTime) {
        if (this.frozen) {
            handleIfFrozen(frameTime);
            return;
        }

        if (this.firingSpotReached) {
            return;
        }

        if (!this.moving && this.gotStuck) {
            selectRandomDirrection();
            return;
        }
        if (this.moving) {
            move(frameTime);

            if (!checkCollisions(frameTime)) {
                updateTarget(frameTime);
                updateDirection();
            } else {
                this.movingAlongShortestPath = false;
            }

            fixBounds();
            checkIfFiringSpotToAttackEagleReached();
        }
    }

    public EnemyTankType getType() {
        return this.tankId.getType();
    }

    @Override
    public void promote() {
        EnemyTankType currType = this.tankId.getType();
        if (currType == EnemyTankType.ARMORED) {
            return;
        }
        setTankType(currType.next());
    }

    @Override
    public void promoteToHeavy() {
        setTankType(EnemyTankType.ARMORED);
    }

    private void setTankType(EnemyTankType tankType) {
        this.tankId.setType(tankType);
        this.speed = tankType.getSpeed();
    }

    public int getScore() {
        return this.tankId.getType().getScore();
    }

    public boolean isGleaming() {
        return gleaming;
    }

    public void setGleaming(boolean gleaming) {
        this.gleaming = gleaming;
    }

    public EnemyTankIdentifier getTankId() {
        return this.tankId;
    }

    private boolean checkCollisions(double frameTime) {

        if (checkMapCollision()) {
            selectRandomDirrection();
            move(frameTime);
            //System.out.println("Map collision");
            return true;
        }

        if (handleCollisionsWithSplashes()) {
            selectRandomDirrection();
            move(frameTime);
            //System.out.println("Collision with splash");
            return true;
        }

        return handleCollisionsWithOtherTanks(frameTime);
    }

    @Override
    protected void handleCollisionWithOtherTank(Tank other, double frameTime) {
        super.handleCollisionWithOtherTank(other, frameTime);
        reverse();
        move(frameTime);
    }

    private void createPowerUp() {
        this.level.createRandomPowerUp();
    }

    @Override
    protected List<Tank> getOtherTanks() {
        List<Tank> otherTanks = super.getOtherTanks();
        List<Player> players = this.level.getPlayers();
        players.forEach(player -> otherTanks.add((Tank) player));
        return otherTanks;
    }

    private void updateAnimation() {
        this.animationManager.setCurrentAnimation(this.tankId);
        this.animationManager.getCurrentAnimation().start(
                AnimationPlayMode.LOOP);
    }

    private Spot getCurrentSpot() {
        int currRow = (int) getY() / Game.HALF_TILE_SIZE;
        int currCol = (int) getX() / Game.HALF_TILE_SIZE;
        Spot currSpot = new Spot(currRow, currCol, true);
        return currSpot;
    }

    private Spot getPlayerSpot(int playerId) {
        int index = playerId - 1;
        if (index > this.level.getPlayers().size() - 1) {
            index = 0;
        }
        Player player = this.level.getPlayers().get(index);
        return getEntitySpot(player);
    }

    private Spot getEntitySpot(Entity entity) {
        int targetCol = (int) entity.getX() / Game.HALF_TILE_SIZE;
        int targetRow = (int) entity.getY() / Game.HALF_TILE_SIZE;
        return new Spot(targetRow, targetCol, true);
    }

    private void updateDirection() {
        if (this.movingAlongShortestPath) {
            moveAlongShortestPath();
        } else {
            if (checkIfNeedRecalculateShortestPath()) {
                selectShortestPathDirection();
            }
        }
    }

    @Override
    public void setDirection(Direction direction) {
        super.setDirection(direction);
        this.tankId.setHeading(Heading.getHeading(direction));
    }

    private List<Direction> getAllPossibleDirections() {
        List<Direction> allPossibleDirections = new ArrayList<>(Direction.
                values().length);
        for (Direction dir : Direction.values()) {
            if (canMoveInDirection(dir)) {
                allPossibleDirections.add(dir);
            }
        }
        return allPossibleDirections;
    }

    private void selectRandomDirrection() {
        List<Direction> allPossibleDirections = getAllPossibleDirections();

        if (allPossibleDirections.isEmpty()) {
            System.out.println("Got stuck!");
            this.gotStuck = true;
            checkMapCollision();
            this.moving = false;
            //this.shootingPeriod /= 4;
            return;
        } else {
            this.gotStuck = false;
            //this.shootingPeriod *= 4;
        }
        int rand = this.random.nextInt(allPossibleDirections.size());
        setDirection(allPossibleDirections.get(rand));
        this.moving = true;
    }

    private void selectShortestPathDirection() {

        if (this.currTarget == null) {
            selectRandomDirrection();
            return;
        }

        Pathfinder pathfinder = new Pathfinder(this);
        if (pathfinder.calcPath(getCurrentSpot(), this.currTarget,
                TANK_DIMENSION)) {
            this.shortestPath = pathfinder.getOptimalPath();
            if (!this.shortestPath.isEmpty()) {
                this.movingAlongShortestPath = true;
                this.nextPosition = this.shortestPath.get(0);
                Direction selectedDir = this.nextPosition.getDirFromPrev();
                setDirection(selectedDir);
            } else {
                selectRandomDirrection();
            }

        } else {
            selectRandomDirrection();
        }
    }

    private boolean checkIfNeedRecalculateShortestPath() {
        if (this.direction.isHorizontal()) {
            if ((int) getX() % Game.TILE_SIZE == 0) {
                return true;
            }
        } else if (this.direction.isVertical()) {
            if ((int) getY() % Game.TILE_SIZE == 0) {
                return true;
            }
        }
        return false;
    }

    private void moveAlongShortestPath() {
        if (checkIfNextPositionReached()) {
            this.shortestPath.remove(this.nextPosition);
            if (!this.shortestPath.isEmpty()) {
                this.nextPosition = this.shortestPath.get(0);
                setDirection(this.nextPosition.getDirFromPrev());
            } else {
                selectRandomDirrection();
                this.movingAlongShortestPath = false;
            }
        }
    }

    private boolean checkIfNextPositionReached() {
        return this.movingAlongShortestPath
                && getCurrentSpot().equals(this.nextPosition);
    }

    private void updateTarget(double frameTime) {
        this.targetTimer += frameTime;
        if (this.targetTimer < TARGET_CHANGING_PERIOD) {
            if (this.number % 4 == 0) {
                targetPowerUp();
            } else if(this.number % 2 == 0){
                targetPlayer(PlayerIdentifier.FIRST);
            } else {
                targetEagle();
            }
        } else if (this.targetTimer < 2 * TARGET_CHANGING_PERIOD) {
            if (this.number % 4 == 0) {
                targetEagle();
            } else if(this.number % 2 == 0){
                targetPowerUp();
            } else {
                targetPlayer(PlayerIdentifier.SECOND);
            }
        } else if (this.targetTimer < 3 * TARGET_CHANGING_PERIOD){
            if (this.number % 4 == 0) {
                targetPlayer(PlayerIdentifier.FIRST);
            } else if(this.number % 2 == 0){
                targetPlayer(PlayerIdentifier.SECOND);
            } else {
                targetEagle();
            }
        } else {
            this.targetTimer = 0;
        }
    }

    private void targetPowerUp() {
        if (this.level.isPowerUpOnField()) {
            List<PowerUp> powerups = this.level.getEntityManager().
                    getEntitiesByType(EntityType.POWER_UP)
                    .stream().map(entity -> (PowerUp) entity).collect(
                    Collectors.toList());
            if (!powerups.isEmpty()) {
                this.currTarget = getEntitySpot(powerups.get(0));
                this.movingAlongShortestPath = false;
            }

        }
    }

    private void targetPlayer(PlayerIdentifier identifier) {
        Spot playerSpot = getPlayerSpot(identifier.getId());
        this.currTarget = playerSpot;
        this.movingAlongShortestPath = false;
    }
    
    private void targetEagle() {
        boolean isCurrTargetEagle = this.firingSpots.stream().anyMatch(
                firingSpot -> {
            return firingSpot.getSpot().equals(this.currTarget);
        });

        if (!isCurrTargetEagle) {
            selectRandomFiringPointToAttackEagle();
            this.movingAlongShortestPath = false;
        }
    }

    private void updateShootingTimer(double frameTime) {
        this.shootingTimer += frameTime;
        if (this.shootingTimer >= shootingPeriod) {
            this.shootingTimer = 0;
            this.canFire = true;
            if (!this.frozen) {
                if (this.firingSpotReached || this.gotStuck
                        || isFireLineFreeOfPartnerTanks()) {
                    fire();
                }
            }
        }
    }

    @Override
    public void fire() {
        if (!this.canFire) {
            return;
        }
        List<Departure> departures = calcDeparturePoints(this.shootingMode);

        if (this.shootingMode == ShootingMode.ROCKET) {
            departures.forEach(departure -> {
                Rocket rocket = new Rocket(level,
                        RocketType.ENEMY,
                        departure.getPoint().x, departure.getPoint().y,
                        RocketType.ENEMY.getSpeed(),
                        departure.getDirection());
                this.level.getEntityManager().addEntity(rocket);
                ++this.rocketsLaunched;
                this.canFire = false;
            });
        } else {
            departures.forEach(departure -> {
                Projectile projectile = new Projectile(level,
                        ProjectileType.ENEMY,
                        departure.getPoint().x, departure.getPoint().y,
                        this.tankId.getType().getProjectileSpeed(),
                        departure.getDirection());
                projectile.
                        setDamage(this.tankId.getType().getProjectileDamage());
                if (this.tankId.getType() == EnemyTankType.ARMORED) {
                    projectile.setAntiarmour(true);
                }
                projectile.setBushCrearing(this.canClearBushes);
                this.level.getEntityManager().addEntity(projectile);
            });
        }

        this.canFire = false;
        ResourceManager.getInstance().getAudio(AudioIdentifier.SHOT).play();
    }

    private void checkIfFiringSpotToAttackEagleReached() {
        for (int i = 0; i < this.firingSpots.size(); ++i) {
            if (firingSpots.get(i).getSpot().distanceEuclidian(getCurrentSpot())
                    <= 4) {
                this.firingSpotReached = true;
                setDirection(firingSpots.get(i).getFireDirection());
                this.moving = false;
                return;
            }
        }
    }

    @Override
    public void hit(int damage) {
        super.hit(damage);
        if (isAlive()) {
            if (!this.bonus && this.tankId.getType()
                    == EnemyTankType.ARMORED) {
                this.tankId.setColor(calcColorDependingOnHealth());
                updateAnimation();
            }
        } else {
            explode();
        }
    }

    public void explodeWithGrenade() {
        super.explode();
        destroy();
    }

    @Override
    public void explode() {
        super.explode();
        ScoreIcrementText text = new ScoreIcrementText(this.level, this.
                getScore(), getX(), getY());
        text.startInfiniteBlinking(0.2);
        int dx = (getWidth() - text.getWidth()) / 2;
        int dy = (getHeight() - text.getHeight()) / 2;
        text.setPosition(getX() + dx, getY() + dy);
        this.level.getEntityManager().addEntity(text);
        if (this.bonus) {
            createPowerUp();
        }
        destroy();
    }

    private List<FiringSpot> getFiringSpotsToAttackTheEagle() {
        return this.level.getTileMap().getFiringSpots();
    }

    private void selectRandomFiringPointToAttackEagle() {
        int rand = this.random.nextInt(this.firingSpots.size());
        this.currTarget = this.firingSpots.get(rand).getSpot();
    }

    private boolean isFireLineFreeOfPartnerTanks() {
        List<Entity> partners = this.level.getEntityManager().getEntitiesByType(
                EntityType.ENEMY_TANK);

        List<Rectangle> damageAreas = new ArrayList<>(4);
        if (this.shootingMode == ShootingMode.FOUR_WAY_SHOT) {
            Direction[] allDirections = Direction.values();
            for (Direction dir : allDirections) {
                Rectangle currDamageArea = calcDamageArea(dir);
                damageAreas.add(currDamageArea);
            }
        } else {
            Rectangle damageArea = calcDamageArea(this.direction);
            damageAreas.add(damageArea);
        }
        return partners.stream().noneMatch(entity -> {
            return damageAreas.stream().anyMatch(area -> entity.
                    getBoundingRect().intersects(area));
        });
    }

    private Rectangle calcDamageArea(Direction direction) {
        switch (direction) {
            case NORTH:
                return new Rectangle((int) left() - getWidth(),
                        0,
                        3 * getWidth(),
                        (int) top());
            case SOUTH:
                return new Rectangle((int) left() - getWidth(),
                        (int) bottom(),
                        3 * getWidth(),
                        this.level.getMapHeight() - (int) top());
            case EAST:
                return new Rectangle((int) right(),
                        (int) top() - getHeight(),
                        this.level.getMapWidth() - (int) left(),
                        3 * getHeight());
            default:
                return new Rectangle(0,
                        (int) top() - getHeight(),
                        (int) left(),
                        3 * getHeight());
        }
    }

    private TankColor getTankColor(EnemyTankType tankType) {
        return tankType == EnemyTankType.ARMORED ? TankColor.GREEN :
                getBaseColor();
    }

    private void updateGleamingColor(double frameTime) {
        if (this.gleaming) {
            this.gleamingTimer += frameTime;
            if (this.gleamingTimer >= COLOR_CHANGING_PERIOD) {
                TankColor currColor = this.tankId.getColor();
                this.tankId.setColor(currColor.next());
                this.gleamingTimer = 0;
            }
        }
    }

    private TankColor getBaseColor() {
        return this.number % 2 == 0 ? TankColor.GREEN : TankColor.GRAY;
    }

    private void updateRedBlinking(double frameTime) {
        if (this.redBlinking) {
            this.redBlinkingTimer += frameTime;
            if (this.redBlinkingTimer >= COLOR_CHANGING_PERIOD) {
                if (this.tankId.getColor() == TankColor.RED) {
                    this.tankId.setColor(getBaseColor());
                } else {
                    this.tankId.setColor(TankColor.RED);
                }
                this.redBlinkingTimer = 0;
            }
        }
    }

    private TankColor calcColorDependingOnHealth() {
        if (this.health < 25) {
            return TankColor.GRAY;
        } else if (this.health < 50) {
            return TankColor.YELLOW;
        } else if (this.health < 75) {
            return TankColor.RED;
        } else {
            return TankColor.GREEN;
        }
    }

    private boolean checkIfBonus() {
        for (int num : BONUS_TANKS_NUMBERS) {
            if (this.number == num) {
                this.bonus = true;
                this.gleaming = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public final void loadAnimations() {

        this.level.getEnemyTankSpriteSheetMap().keySet().forEach(key -> {
            this.animationManager.addAnimation(key, new Animation(
                    this.level.getEnemyTankSpriteSheetMap().get(key), 0.5,
                    0, 0, Game.TILE_SIZE, Game.TILE_SIZE, 2, Game.TILE_SIZE
            ));
        });
    }

    @Override
    public TankColor getTankColor() {
        return this.tankId.getColor();
    }
}
