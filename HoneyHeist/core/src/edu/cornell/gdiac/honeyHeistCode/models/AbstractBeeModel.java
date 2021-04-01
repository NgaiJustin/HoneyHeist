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
public abstract class AbstractBeeModel extends CapsuleObstacle {

    /**
     * The initializing data (to avoid magic numbers)
     */
    private final JsonValue data;
    /**
     * The factor to multiply by the input
     */
    private final float force;
    /**
     * The amount to slow the character down
     */
    private final float damping;
    /**
     * The maximum character speed
     */
    private final float maxspeed;
    /**
     * Identifier to allow us to track the sensor in ContactListener
     */
    private final String sensorName;

    /**
     * The current horizontal movement of the character
     */
    private float movement;
    /**
     * Which direction is the character facing
     */
    private boolean faceRight;
    /**
     * Whether our feet are on the ground
     */
    protected boolean isGrounded;
    /**
     * Sensor fixtures for isGrounded detection
     */
    protected ObjectSet<Fixture> sensorFixtures;
    /**
     * The physics shape of this object
     */
    private PolygonShape sensorShape;

    /**
     * Cache for internal force calculations
     */
    private final Vector2 forceCache = new Vector2();


    /**
     * Returns left/right movement of this character.
     * <p>
     * This is the result of input times ant's force.
     *
     * @return left/right movement of this character.
     */
    public float getMovement() {
        return movement;
    }

    /**
     * Sets left/right movement of this character.
     * <p>
     * This is the result of input times ant's force.
     *
     * @param value left/right movement of this character.
     */
    public void setMovement(float value) {
        movement = value;

        // Change facing if appropriate
        if (movement < 0) {
            faceRight = false;
        } else if (movement > 0) {
            faceRight = true;
        }
    }

    /**
     * Returns true if the bee is on the ground.
     *
     * @return true if the bee is on the ground.
     */
    public boolean isGrounded() {
        return isGrounded;
    }

    /**
     * Sets whether the bee is on the ground.
     *
     * @param value whether the ant is on the ground.
     */
    public void setGrounded(boolean value) {
        isGrounded = value;
    }

    /**
     * Returns how much force to apply to get the ant moving
     * <p>
     * Multiply this by the input to get the movement value.
     *
     * @return how much force to apply to get the bee moving
     */
    public float getForce() {
        return force;
    }

    /**
     * Returns how hard the brakes are applied to get the bee to stop moving
     *
     * @return how hard the brakes are applied to get the bee to stop moving
     */
    public float getDamping() {
        return damping;
    }

    /**
     * Returns the upper limit on bee's left-right movement.
     * <p>
     * This does NOT apply to vertical movement.
     *
     * @return the upper limit on bee's left-right movement.
     */
    public float getMaxSpeed() {
        return maxspeed;
    }

    public ObjectSet<Fixture> getSensorFixtures() {
        return sensorFixtures;
    }

    /**
     * Returns the name of the ground sensor
     * <p>
     * This is used by ContactListener
     *
     * @return the name of the ground sensor
     */
    public String getSensorName() {
        return sensorName;
    }

    /**
     * Returns true if this character is facing right
     *
     * @return true if this character is facing right
     */
    public boolean isFacingRight() {
        return faceRight;
    }

    /**
     * Creates a bee avatar with the given physics data
     *
     * @param data   The physics constants for the player Ant
     * @param width  The object width in physics units
     * @param height The object width in physics units
     */
    public AbstractBeeModel(JsonValue data, float width, float height) {
        super(data.get("pos").getFloat(0),
                data.get("pos").getFloat(1),
                width * data.get("shrink").getFloat(0),
                height * data.get("shrink").getFloat(1));
        setDensity(data.getFloat("density", 0));
        setFriction(data.getFloat("friction", 0));
        setFixedRotation(true);

        maxspeed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        sensorName = "BeeGroundSensor"+data.get("pos").getFloat(0)+data.get("pos").getFloat(1);
        this.data = data;

        // Gameplay attributes
        isGrounded = false;
        faceRight = true;

        setName("bee");

        //Probably replace the following code with json data
        rotationAngle = (float) Math.PI/3;
        rotationSpeed = (float) Math.PI/3;

        sensorFixtures = new ObjectSet<Fixture>();
    }

    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     * <p>
     * This method overrides the base method to keep your ship from spinning.
     *
     * @param world Box2D world to store body
     * @return true if object allocation succeeded
     */
    public boolean activatePhysics(World world) {
        // Create the box from our superclass
        if (!super.activatePhysics(world)) {
            return false;
        }

        // Ground Sensor
        // -------------
        // To determine whether or not the ant is on the ground,
        // we create a thin sensor under his feet, which reports
        // collisions with the world but has no collision response.
        Vector2 sensorCenter = new Vector2(0, -getHeight() / 2);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = data.getFloat("density", 0);
        sensorDef.isSensor = true;
        sensorShape = new PolygonShape();
        JsonValue sensorjv = data.get("sensor");
        sensorShape.setAsBox(sensorjv.getFloat("shrink", 0) * getWidth()/1.6f ,
                sensorjv.getFloat("height", 0)*3f, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Ground sensor to represent our feet
        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorFixture.setUserData(getSensorName());

        return true;
    }

    public void update(float dt) {
        if (!isRotating) {
            if(stickTime>0){
                stickTime -= dt;
            }
            else{
                if(sticking){
                    setBodyType(BodyDef.BodyType.DynamicBody);
                    sticking = false;
                    isGrounded = false;
                    this.setAngle(0);
                }
            }
            return;
        }

        float rotationAmount = rotationSpeed * dt;
        if (rotationAmount > remainingAngle){
            rotationAmount = remainingAngle;
            isRotating = false;
            stickTime = maxStickTime;
        }
        remainingAngle -= rotationAmount;
        if (!isClockwise) {
            rotationAmount *= -1;
        }
        rotateAboutPoint(rotationAmount, stageCenter);
    }

    /**
     * Applies the force to the body of this bee
     * <p>
     * This method should be called after the force attribute is set.
     */
    public void applyForce() {
        if (!isActive()) {
            return;
        }

        // Don't want to be moving. Damp out player motion
        if (getMovement() == 0f) {
            forceCache.set(-getDamping() * getVX(), 0);
            body.applyForce(forceCache, getPosition(), true);
        }

        // Velocity too high, clamp it
        if (Math.abs(getVX()) >= getMaxSpeed()) {
            setVX(Math.signum(getVX()) * getMaxSpeed());
        } else {
            forceCache.set(getMovement(), 0);
            body.applyForce(forceCache, getPosition(), true);
        }

        if (isGrounded&&(Math.abs(getVY()) >= getMaxSpeed())) {
            setVY(Math.signum(getVY()) * getMaxSpeed());
        }

        if(!isGrounded){
            setVY(Math.min(0f,getVY()));
        }
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = faceRight ? 1.0f : -1.0f;
        // Reset Bee rotation if falling
        //if (!isGrounded()){
        //    this.setAngle(0);
        //}
        canvas.draw(texture, Color.WHITE, origin.x-6.0f, origin.y, getX() * drawScale.x, getY() * drawScale.y, getAngle(), effect, 1.0f);
    }

    /**
     * Draws the outline of the physics body.
     *
     * @param canvas Drawing context
     */
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        canvas.drawPhysics(sensorShape, Color.RED, getX(), getY(), getAngle(), drawScale.x, drawScale.y);
    }



}