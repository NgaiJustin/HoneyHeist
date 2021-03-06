package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.honeyHeistCode.obstacle.CapsuleObstacle;

public class CharacterModel extends CapsuleObstacle {

    /**
     * The initializing data (to avoid magic numbers)
     */
    protected final JsonValue data;
    /**
     * The factor to multiply by the input
     */
    private final float force;
    /**
     * The amount to slow the character down
     */
    protected float damping;
    /**
     * The maximum character speed
     */
    private float maxspeed;

    /**
     * The default maximum character speed
     */
    private final float defaultMaxspeed;

    /**
     * The current horizontal movement of the character
     */
    private float movement;
    /**
     * Which direction is the character facing
     */
    protected boolean faceRight;
    /**
     * Whether our feet are on the ground
     */
    protected boolean isGrounded;
    /**
     * Whether the character is in honey
     */
    protected boolean isInHoney;
    /**
     * Whether the character has died, used to trigger death animation
     */
    protected boolean isDead;
    /**
     * Sensor fixtures for isGrounded detection
     */
    protected ObjectSet<Fixture> sensorFixtures;
    /**
     * Sensor fixtures for isInHoney detection
     */
    protected ObjectSet<Fixture> honeyFixtures;
    /**
     * The physics shape of this object
     */
    protected PolygonShape sensorShape;
    /**
     * Identifier to allow us to track the sensor in ContactListener
     */
    protected String sensorName;

    protected float honeyTime;

    /**
     * Cache for internal force calculations
     */
    protected final Vector2 forceCache = new Vector2();


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
     * Returns true if the bee is in honey.
     *
     * @return true if the bee is in honey.
     */
    public boolean isInHoney() {
        return isInHoney;
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
     * Sets whether the bee is in honey.
     *
     * @param value whether the ant is in honey.
     */
    public void setInHoney(boolean value) { isInHoney = value; }

    /**
     * Sets whether the character is dead
     */
    public void setIsDead (boolean value) {isDead = value;}

    /**
     * Get whether the character is dead
     */
    public boolean getIsDead () {return isDead;}

    public void setMaxspeed(float speed){ maxspeed = speed; }

    public void setDefaultMaxspeed(){ maxspeed = defaultMaxspeed; }

    public void setHoneyTime(float time) { honeyTime = time; }

    public float getHoneyTime() { return honeyTime; }

    /**
     * Remove all forces on the bee - Halts movement
     */
    public void haltMovement(){
        if (body != null) {
            body.setAngularVelocity(0);
            body.setLinearVelocity(0, 0);
        }
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

    public ObjectSet<Fixture> getHoneyFixtures() {
        return honeyFixtures;
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

    public void startRotation(boolean isClockwise, Vector2 point){
        if (isRotating) return;
        currentSpeed = 0f;
        if(isGrounded) {
            setBodyType(BodyDef.BodyType.StaticBody);
            sticking = true;
        }
        stageCenter = point;
        isRotating = true;
        this.isClockwise = isClockwise;
        addRotation(rotationAngle);
    }
    public void startRotation(float rotationAmount, boolean isClockwise, Vector2 point){
        if (isRotating) return;
        currentSpeed = 0f;
        if(isGrounded) {
            setBodyType(BodyDef.BodyType.StaticBody);
            sticking = true;
        }
        stageCenter = point;
        isRotating = true;
        this.isClockwise = isClockwise;
        addRotation(rotationAmount);
    }

    public CharacterModel(JsonValue data, float x, float y, float width, float height){
        super(x, y,
                width * data.get("shrink").getFloat(0),
                height * data.get("shrink").getFloat(1));
        setDensity(data.getFloat("density", 0));
        setFriction(data.getFloat("friction", 0));
        setFixedRotation(false);

        maxspeed = data.getFloat("maxspeed", 0);
        defaultMaxspeed = maxspeed;
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        setGravityScale(data.getFloat("gravityScale", 1));
        this.data = data;

        // Gameplay attributes
        isGrounded = false;
        faceRight = true;

        //Probably replace the following code with json data
        //rotationAngle = (float) Math.PI/3;
        //rotationSpeed = ((float) Math.PI/3)*1.3f;

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
                sensorjv.getFloat("height", 0)*1.5f, sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Ground sensor to represent our feet
        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorFixture.setUserData(getSensorName());

        return true;
    }

    public void update(float dt) {
        if (!isRotating) {
            /*if(honeyTime>0){
                honeyTime -= dt;
            }*/
            if(stickTime>0){
                stickTime -= dt;
            }
            else{
                if(sticking){
                    setBodyType(BodyDef.BodyType.DynamicBody);
                    sticking = false;
                    isGrounded = false;
                }
            }
            if(!isGrounded){
                float angle = getAngle();
                float rotSpeed = ((isInHoney) ? 4f : 13f);
                if(angle<-0.05) {
                    setAngularVelocity(Math.min(rotSpeed,-angle/dt));
                }
                else if(angle>0.05) {
                    setAngularVelocity(Math.max(-rotSpeed,-angle/dt));
                }
                else{
                    setAngularVelocity(0f);
                }
                //setAngle(0);
            }
            return;
        }

        getRotSpeed(dt);
        float rotationAmount = currentSpeed * dt;
        if (rotationAmount > remainingAngle){
            rotationAmount = remainingAngle;
            isRotating = false;
            if(isGrounded) {
                stickTime = maxStickTime;
            }
        }
        remainingAngle -= rotationAmount;
        if (!isClockwise) {
            rotationAmount *= -1;
        }
        rotateAboutPoint(rotationAmount, stageCenter);
    }

    public void rotateAboutPoint(float amount, Vector2 point) {
        Body body = getBody();
        assert(body != null);
        Transform bT = body.getTransform();
        Vector2 p = bT.getPosition().sub(point);
        float c = (float) Math.cos(amount);
        float s = (float) Math.sin(amount);
        float x = p.x * c - p.y * s;
        float y = p.x * s + p.y * c;
        Vector2 pos = new Vector2(x, y).add(point);
        float angle = 0;
        if(isGrounded) {
            angle = bT.getRotation() + amount;
        }
        body.setTransform(pos, angle);
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
        }
        if((Math.copySign(1.0f,getVX())!=Math.copySign(1.0f,getMovement()))||!(Math.abs(getVX()) >= getMaxSpeed())){
            forceCache.set(getMovement(), 0);
            body.applyForce(forceCache, getPosition(), true);
        }

        if ((isGrounded||isInHoney)&&(Math.abs(getVY()) >= getMaxSpeed())) {
            setVY(Math.signum(getVY()) * getMaxSpeed());
        }

        if(!isGrounded){
            setVY(Math.min(0f,getVY()));
        }

        /*if(isGrounded){
            setVY(Math.min(-0.145f,getVY()));
        }*/
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
