/*
 * PlatformController.java
 *
 * You SHOULD NOT need to modify this file.  However, you may learn valuable lessons
 * for the rest of the lab by looking at it.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * Updated asset version, 2/6/2021
 */
package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import edu.cornell.gdiac.honeyHeistCode.obstacle.BoxObstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;

/**
 * A class holding all game objects contained within a level,
 * with getters and setters for all game objects.
 *
 * Game objects include the character avatar, the list of bees,
 * the goalDoor, the platform model, and the origin of the world.
 */
public class LevelModel {
    /**
     * Reference to the character avatar
     */
    private PlayerModel playerModel;
    /**
     * Reference to the list of bees
     */
    private Array<AbstractBeeModel> bees;
    /**
     * Reference to the goalDoor (for collision detection)
     */
    private BoxObstacle goalDoor;
    /**
     * Reference to the platform model
     */
    private PlatformModel platforms;

    /**
     * Reference to the spiked platforms
     */
    private SpikedPlatformModel spikedPlatforms;

    /**
     * Reference to the honeypatches
     */
    private HoneypatchModel honeyPatches;
    /**
     * Reference to the levelBackground
     */
    private PolygonObstacle levelBackground;
    /**
     * Reference to the origin of the world
     */
    private Vector2 origin;
    /**
     * Reference to the bounds of the world
     */
    private Rectangle bounds;

    /**
     * Creates and initialize a new instance of the platformer game
     * <p>
     * The game has default gravity and other settings
     */
    public LevelModel(PlayerModel playerModel, Array<AbstractBeeModel> bees, BoxObstacle goalDoor,
                      PlatformModel platforms, SpikedPlatformModel spikedPlatforms,
                      HoneypatchModel honeyPatches, PolygonObstacle levelBackground, Rectangle bounds) {
        this.playerModel = playerModel;
        this.bees = bees;
        this.goalDoor = goalDoor;
        this.platforms = platforms;
        this.spikedPlatforms = spikedPlatforms;
        this.honeyPatches = honeyPatches;
        this.levelBackground = levelBackground;
        this.origin = new Vector2(bounds.width/2,bounds.height/2);
        this.bounds = bounds;
    }

    public LevelModel() {
    }

    public PlatformModel getPlatforms() {
        return platforms;
    }

    public SpikedPlatformModel getSpikedPlatforms() {return spikedPlatforms;}

    public void setPlatforms(PlatformModel platforms) { this.platforms = platforms; }

    public PlayerModel getPlayer() {
        return playerModel;
    }

    public void setPlayer(PlayerModel playerModel) { this.playerModel = playerModel; }

    public Array<AbstractBeeModel> getBees() {return bees;}

    public void setBees(Array<AbstractBeeModel> bees) { this.bees = bees; }

    public BoxObstacle getGoalDoor() {return goalDoor;}

    public void setGoalDoor(BoxObstacle goalDoor) { this.goalDoor = goalDoor; }

    public HoneypatchModel getHoneyPatches() {return honeyPatches;}

    public void setHoneyPatches(HoneypatchModel honeyPatches) {this.honeyPatches = honeyPatches;}

    public Vector2 getOrigin() {return origin;}

    public void setOrigin(Vector2 origin) { this.origin = origin; }

    public Rectangle getBounds() {return bounds;}

    public void setLevelBackground(PolygonObstacle bg) {this.levelBackground = bg; }

    public PolygonObstacle getLevelBackground() { return levelBackground; }
}