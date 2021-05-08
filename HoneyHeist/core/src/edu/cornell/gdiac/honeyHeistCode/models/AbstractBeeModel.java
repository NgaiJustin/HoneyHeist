package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.honeyHeistCode.obstacle.CapsuleObstacle;


/**
 * Model class for Enemy (Bee) in HoneyHeist.
 */
public abstract class AbstractBeeModel extends CharacterModel {

    /** True if the Bee is currently chasing the player */
    protected boolean isChasing;

    public AbstractBeeModel(JsonValue data, float x, float y, float width, float height) {
        super(data, x, y, width, height);
        setName("bee");
        sensorName = "BeeGroundSensor"+x+y;
        sensorFixtures = new ObjectSet<Fixture>();
        honeyFixtures = new ObjectSet<Fixture>();
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = faceRight ? 1.0f : -1.0f;
        canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y, getAngle(), effect, 1.0f);
    }

    /**
     * Set the status of this enemy as chasing. The chasing
     * art will render when isChasing is true
     * @param b
     */
    public void setIsChasing(boolean b) {
        this.isChasing = b;
    }

    /**
     * Returns if the enemy is currently chasing the player
     * @return
     */
    public boolean getIsChasing() {
        return this.isChasing;
    }
}