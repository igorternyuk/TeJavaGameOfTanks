package com.igorternyuk.tanks.graphics.spritesheets;

import com.igorternyuk.tanks.gameplay.Game;
import com.igorternyuk.tanks.graphics.images.TextureAtlas;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author igor
 */
public class SpriteSheetManager {
    
    private static SpriteSheetManager instance;
    
    public static synchronized SpriteSheetManager getInstance(){
        if(instance == null){
            instance = new SpriteSheetManager();
        }
        return instance;
    }
    
    private SpriteSheetManager(){
    }

    private Map<SpriteSheetIdentifier, BufferedImage> spriteSheets =
            new HashMap<>();

    public void put(SpriteSheetIdentifier identifier, TextureAtlas atlas) {
        this.spriteSheets.put(identifier, atlas.cutOut(identifier.
                getBoundingRect()));
    }

    public void remove(SpriteSheetIdentifier identifier) {
        this.spriteSheets.remove(identifier);
    }

    public BufferedImage get(SpriteSheetIdentifier identifier) {
        BufferedImage sprite = this.spriteSheets.get(identifier);
        return sprite;
    }

    public BufferedImage fetchDigitSprite(int digit) {
        if (!this.spriteSheets.containsKey(SpriteSheetIdentifier.DIGITS)) {
            throw new RuntimeException(
                    "The spritesheet with digits was not loaded");
        }
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("The digit is out of 0..9 range");
        }
        BufferedImage digitSpriteSheet = this.spriteSheets.
                get(SpriteSheetIdentifier.DIGITS);
        int sourceX = (digit % 5) * Game.HALF_TILE_SIZE;
        int sourceY = (digit / 5) * Game.HALF_TILE_SIZE;
        return digitSpriteSheet.getSubimage(sourceX, sourceY,
                Game.HALF_TILE_SIZE, Game.HALF_TILE_SIZE);
    }
}
