package com.igorternyuk.tanks.gamestate;

import com.igorternyuk.tanks.gameplay.Game;
import com.igorternyuk.tanks.gameplay.GameStatus;
import com.igorternyuk.tanks.gameplay.entities.player.Player;
import com.igorternyuk.tanks.gameplay.entities.player.PlayerIdentifier;
import com.igorternyuk.tanks.gameplay.entities.tank.enemytank.EnemyTankType;
import com.igorternyuk.tanks.graphics.spritesheets.SpriteSheetManager;
import com.igorternyuk.tanks.input.KeyboardState;
import com.igorternyuk.tanks.resourcemanager.AudioIdentifier;
import com.igorternyuk.tanks.resourcemanager.FontIdentifier;
import com.igorternyuk.tanks.resourcemanager.ResourceManager;
import com.igorternyuk.tanks.utils.Painter;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author igor
 */
public class ScoreScreen {

    private static final Color COLOR_TOTAL_SCORE = new Color(238, 188, 96);
    private static final double DELAY = 0.1;
    private static final double DELAY_AFTER_ANIMATION = 5;
    private Font fontLarger;
    private Font fontSmaller;
    private LevelState level;
    private List<Player> players = new ArrayList<>();
    private List<Map<EnemyTankType, Integer>> currentlyDisplayingStatisticalMaps =
            new ArrayList<>();
    private EnemyTankType currTankType = EnemyTankType.BASIC;
    private double animationTimer = 0;
    private double afterTimer = 0;
    private List<Boolean> animationFinished = new ArrayList<>();
    private boolean readyToNextStage = false;
    private int currentPlayerIndex = 0;

    public ScoreScreen(LevelState level) {
        this.level = level;
        this.players = this.level.getPlayers();
        this.fontLarger = ResourceManager.getInstance().getFont(
                FontIdentifier.BATTLE_CITY).deriveFont(Font.BOLD, 22);
        this.fontSmaller = ResourceManager.getInstance().getFont(
                FontIdentifier.BATTLE_CITY).deriveFont(Font.BOLD, 18);
        for (int i = 0; i < this.players.size(); ++i) {
            this.currentlyDisplayingStatisticalMaps.add(new HashMap<>());
            for (EnemyTankType type : EnemyTankType.values()) {
                this.currentlyDisplayingStatisticalMaps.get(i).put(type, 0);
            }
        }
        for (int i = 0; i < this.players.size(); ++i) {
            this.animationFinished.add(false);
        }
    }

    public boolean isReadyToNextStage() {
        return this.readyToNextStage;
    }

    public void reset() {
        this.animationTimer = 0;
        this.players = this.level.getPlayers();
        this.animationFinished.clear();
        for (int i = 0; i < this.players.size(); ++i) {
            this.animationFinished.add(false);
        }
        this.currTankType = EnemyTankType.BASIC;
        this.currentlyDisplayingStatisticalMaps.forEach(map -> {
            map.keySet().forEach(key -> map.put(key, 0));
        });
        this.afterTimer = 0;
        this.readyToNextStage = false;
        this.currentPlayerIndex = 0;
    }

    public void update(KeyboardState keyboardState, double frameTime) {
        if (this.readyToNextStage && this.level.getGameStatus()
                == GameStatus.GAME_OVER) {
            return;
        }
        if (this.animationFinished.stream().allMatch(item -> item)) {
            if (!this.readyToNextStage) {
                this.afterTimer += frameTime;
                if (this.afterTimer >= DELAY_AFTER_ANIMATION) {
                    this.readyToNextStage = true;
                }
            }
            return;
        }
        updateStatisticalTable(frameTime);
    }

    private void updateStatisticalTable(double frameTime) {
        
        if(this.animationFinished.size() != this.players.size()){
            int toRemoveCount = this.animationFinished.size() - this.players.size();
            for(int i = 0; i < toRemoveCount; ++i){
                this.animationFinished.remove(this.animationFinished.size() - 1);
            }
        }
        
        if(this.currentPlayerIndex >= this.players.size()){
            return;
        }
        this.animationTimer += frameTime;
        if (this.animationTimer >= DELAY) {
            this.animationTimer = 0;
            int prev = this.currentlyDisplayingStatisticalMaps.get(
                    this.currentPlayerIndex).get(
                            this.currTankType);
            ++prev;
            int max = this.players.get(this.currentPlayerIndex).getStatistics().
                    getKilledEnemyTanks().get(
                            this.currTankType);
            boolean next = false;
            if (prev >= max) {
                prev = max;
                next = true;
            }
            this.currentlyDisplayingStatisticalMaps.get(this.currentPlayerIndex).put(
                    this.currTankType, prev);
            if (next) {
                this.currTankType = this.currTankType.next();
                if (this.currTankType == EnemyTankType.BASIC) {
                    this.animationFinished.set(this.currentPlayerIndex, true);
                    ++this.currentPlayerIndex;
                }
            }
            ResourceManager.getInstance().getAudio(
                    AudioIdentifier.SCORE_SCREEN).
                    play();
        }
    }

    public void draw(Graphics2D g) {
        if(this.currentlyDisplayingStatisticalMaps.isEmpty()){
            return;
        }
        drawHighestScore(g);
        drawStatistics(g);
        drawKilledEnemiesTotals(g);
    }

    private void drawStatistics(Graphics2D g) {
        if(this.players.isEmpty()){
            return;
        }
        g.setColor(Color.white);
        g.setFont(this.fontSmaller);

        EnemyTankType[] enemyTankTypes = EnemyTankType.values();
        for (int i = 0; i < enemyTankTypes.length; ++i) {
            EnemyTankType currEnemyTankType = enemyTankTypes[i];
            int killedTanksWithCurrType =
                    this.currentlyDisplayingStatisticalMaps.get(0).get(
                            currEnemyTankType);
            int pointsForCurrTankType = killedTanksWithCurrType
                    * currEnemyTankType.getScore();
            int currY = 200 + 48 * i;
            int textY = currY + 25;
            g.setColor(this.players.get(0).getId().getTankColor().getColor());
            g.drawString(String.valueOf(pointsForCurrTankType), 10, textY);
            g.drawString(" PTS ", 80, textY);
            g.drawString(String.valueOf(killedTanksWithCurrType), 175, textY);
            g.drawString("<", 215, textY);

            BufferedImage currTankTypeImage = SpriteSheetManager.getInstance().
                    fetchStatisticsTankImage(currEnemyTankType);
            g.drawImage(currTankTypeImage, (Game.WIDTH - Game.TILE_SIZE) / 2,
                    currY, null);

            if (this.players.size() > 1) {
                g.setColor(this.players.get(1).getId().getTankColor().getColor());
                g.drawString(">", 265, textY);
                int killedTanksWithCurrType2 =
                        this.currentlyDisplayingStatisticalMaps.get(1).get(
                                currEnemyTankType);
                int pointsForCurrTankType2 = killedTanksWithCurrType2
                        * currEnemyTankType.getScore();
                g.drawString(String.valueOf(killedTanksWithCurrType2), 287,
                        textY);
                g.drawString(" PTS ", 305, textY);
                g.drawString(String.valueOf(pointsForCurrTankType2), 392, textY);
            }
        }
    }

    private void drawHighestScore(Graphics2D g) {
        g.setFont(this.fontLarger);
        g.setColor(Color.red);
        g.drawString("HI-SCORE", 10, 30);
        g.setColor(COLOR_TOTAL_SCORE);
        g.drawString("" + this.level.getHighestScore(), 250, 30);
        Painter.drawCenteredString(g, "STAGE " + this.level.getStageNumber(),
                fontLarger, Color.white, 70);
        g.setColor(PlayerIdentifier.FIRST.getTankColor().getColor());
        g.drawString("I-PLAYER", 5, 110);
        g.setColor(PlayerIdentifier.SECOND.getTankColor().getColor());
        g.drawString("II-PLAYER", Game.WIDTH / 2, 110);
        g.setColor(COLOR_TOTAL_SCORE);
        for (int i = 0; i < this.players.size(); ++i) {
            g.setColor(this.players.get(i).getId().getTankColor().getColor());
            g.drawString("" + this.players.get(i).getStatistics().
                    getTotalScore(), 50 + i * 190, 150);
        }
    }

    private void drawKilledEnemiesTotals(Graphics2D g) {
        int lineWidth = 12 * Game.TILE_SIZE;
        g.setColor(Color.white);
        g.fillRect((Game.WIDTH - lineWidth) / 2, 400, lineWidth, 4);
        g.drawString("TOTAL ", 5, 430);
        for (int i = 0; i < this.players.size(); ++i) {
            int totalTanks = 0;
            Collection<Integer> tankCounts =
                    this.currentlyDisplayingStatisticalMaps.get(i).values();
            Iterator<Integer> it = tankCounts.iterator();
            while (it.hasNext()) {
                totalTanks += it.next();
            }
            g.setColor(this.players.get(i).getId().getTankColor().getColor());
            g.drawString(" " + totalTanks, 140 + i * 140, 430);
        }
    }
}
