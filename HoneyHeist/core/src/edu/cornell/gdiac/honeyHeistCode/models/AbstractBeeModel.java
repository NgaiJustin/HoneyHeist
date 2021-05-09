package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;


/**
 * Model class for Enemy (Bee) in HoneyHeist.
 */
public abstract class AbstractBeeModel extends CharacterModel {

    /** True if the Bee is currently chasing the player */
    protected boolean isChasing;

    /** True if the Enemy has already been flagged as dead and the death animation has completed */
    protected boolean isTrulyDead;

    public AbstractBeeModel(JsonValue data, float x, float y, float width, float height) {
        super(data, x, y, width, height);
        setName("bee");
        sensorName = "BeeGroundSensor"+x+y;
        sensorFixtures = new ObjectSet<Fixture>();
        honeyFixtures = new ObjectSet<Fixture>();
        this.isDead = false;
        this.isTrulyDead = false;
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
     * To be overridden by the FlyBeeModel and LarvaeModel that inherit this class
     */
    public void animateDeath(){ }

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

    /**
     * Set if the enemy has finished playing death animation.
     * <p>
     * Precondition: Enemy must be dead first inorder for isTrulyDead = true
     */
    public void setIsTrulyDead(boolean b) {
        // Precondition: Enemy must be dead first
        if (this.isDead) {
            this.isTrulyDead = b;
        }
    }

    /** Return if the enemy has already died and has finish playing death animation */
    public boolean getIsTrulyDead(){return this.isTrulyDead;}
}