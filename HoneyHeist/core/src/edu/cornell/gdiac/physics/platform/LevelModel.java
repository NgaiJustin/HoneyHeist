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
package edu.cornell.gdiac.physics.platform;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundBuffer;
import edu.cornell.gdiac.physics.InputController;
import edu.cornell.gdiac.physics.WorldController;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import org.graalvm.compiler.core.common.type.ArithmeticOpTable;

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
    private AntModel avatar;
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
     * Reference to the origin of the world
     */
    private Vector2 origin;

    /**
     * Creates and initialize a new instance of the platformer game
     * <p>
     * The game has default gravity and other settings
     */
    public LevelModel() {
        bees = new Array<AbstractBeeModel>();
    }

    public PlatformModel getPlatforms() {
        return platforms;
    }

    public void setPlatforms(PlatformModel platforms) { this.platforms = platforms; }

    public AntModel getAvatar() {
        return avatar;
    }

    public void setAvatar(AntModel avatar) { this.avatar = avatar; }

    public Array<AbstractBeeModel> getBees() {return bees;}

    public void setBees(Array<AbstractBeeModel> bees) { this.bees = bees; }

    public BoxObstacle getGoalDoor() {return goalDoor;}

    public void setGoalDoor(BoxObstacle goalDoor) { this.goalDoor = goalDoor; }

    public Vector2 getOrigin() {return origin;}

    public void setOrigin(Vector2 origin) { this.origin = origin; }
}