package com.igorternyuk.tanks.gameplay.entities;

import com.igorternyuk.tanks.gameplay.Game;
import com.igorternyuk.tanks.gamestate.LevelState;
import com.igorternyuk.tanks.graphics.animations.AnimationManager;
import com.igorternyuk.tanks.input.KeyboardState;
import java.awt.Graphics2D;

/**
 *
 * @author igor
 * @param <I> Animation identifier
 */
public abstract class AnimatedEntity<I> extends Entity {

    protected AnimationManager<I> animationManager = new AnimationManager<>();

    public AnimatedEntity(LevelState level, EntityType type, double x, double y,
            double speed, Direction direction) {
        super(level, type, x, y, speed, direction);
    }

    public abstract void loadAnimations();

    @Override
    public int getWidth() {
        return this.animationManager.getCurrentAnimation().
                getCurrentFrameWidth();
    }

    @Override
    public int getHeight() {
        return this.animationManager.getCurrentAnimation().
                getCurrentFrameHeight();
    }

    @Override
    public void update(KeyboardState keyboardState, double frameTime) {
        super.update(keyboardState, frameTime);
        this.animationManager.update(frameTime);
    }

    @Override
    public void draw(Graphics2D g) {
        super.draw(g);
        this.animationManager.draw(g, (int) getX(), (int) getY(),
                Game.SCALE, Game.SCALE);
    }

}
