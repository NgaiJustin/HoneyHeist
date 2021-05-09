package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.util.FilmStrip;

public class FlyingBeeModel extends AbstractBeeModel{

    private float vMovement;

    // Flying animation fields
    /** The texture filmstrip for the left animation node */
    private FilmStrip flyingAnim;
    private FilmStrip flailingAnim;
    private FilmStrip dyingAnim;
    private FilmStrip chasingAnim;

    /** The animation phase for the walking animation */
    private boolean flyCycle = true;
    private boolean flailCycle = true;
    private boolean deathCycle = false;
    private boolean chaseCycle = true;
    private final int FRAMES_PER_ANIM = 7;
    private int animFrames = 0;


    /**
     * Enumeration to identify the ant animations
     */
    public enum BeeAnimations {
        /** Walking animation */
        FLY,
        /** Flailing animation */
        FLAIL,
        /** Death animation */
        DEATH,
        /** Chasing animation */
        CHASE,
        // Future animations to be supported
    };

    /**
     * Creates a bee avatar with the given physics data
     *
     * @param data   The physics constants for the player Ant
     * @param width  The object width in physics units
     * @param height The object width in physics units
     */
    public FlyingBeeModel(JsonValue data, float x, float y, float width, float height) {
        super(data, x, y, width, height);
        setName("FlyingBee");
        setGravityScale(0);
        setFixedRotation(true);
    }

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
        Vector2 sensorCenter = new Vector2(0, -getHeight() / 8);
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

    public void setVMovement (float value) {
        vMovement = value ;
    }

    public float getVMovement () {
        return vMovement;
    }

    @Override
    public void applyForce() {
        if (!isActive()) {
            return;
        }
        // Don't want to be moving. Damp out player motion
        if ((getMovement() == 0f) && (getVMovement() == 0f)) {
            forceCache.set(-getDamping() * getVX(), -getDamping() * getVY());
            body.applyForce(forceCache, getPosition(), true);
        }
        else if (getVMovement() == 0f && getVY() != 0f) {
            forceCache.set(0, -getDamping() * getVY());
            body.applyForce(forceCache, getPosition(), true);
        }
        else if (getMovement() == 0f){
            forceCache.set(-getDamping() * getVX(), 0);
            body.applyForce(forceCache, getPosition(), true);
        }

        // Velocity too high, clamp it
        if (Math.abs(getVX()) >= getMaxSpeed()) {
            setVX(Math.signum(getVX()) * getMaxSpeed());
        }
        if (Math.abs(getVY()) >= getMaxSpeed()) {
            setVY(Math.signum(getVY()) * getMaxSpeed());
        }
        // If not trying to move in a direction currently moving OR if not faster than cap
        if((Math.copySign(1.0f,getVX())!=Math.copySign(1.0f,getMovement()))||!(Math.abs(getVX()) >= getMaxSpeed())){
            forceCache.set(getMovement(), 0);
            body.applyForce(forceCache, getPosition(), true);
        }
        if((Math.copySign(1.0f,getVY())!=Math.copySign(1.0f,getMovement()))||!(Math.abs(getVY()) >= getMaxSpeed())){
            forceCache.set(0, getVMovement());
            body.applyForce(forceCache, getPosition(), true);
        }

//        if (isGrounded&&(Math.abs(getVY()) >= getMaxSpeed())) {
//            setVY(Math.signum(getVY()) * getMaxSpeed());
//        }
//
//        if(!isGrounded){
//            setVY(Math.min(0f,getVY()));
//        }

        /*if(isGrounded){
            setVY(Math.min(-0.145f,getVY()));
        }*/
    }
    /**
     * Sets the animation node for the given afterburner
     *
     * @param  anim     enumeration to identify the ant animation
     *
     * @param  strip 	the animation filmstrip for the given animation
     */
    public void setAnimationStrip(BeeAnimations anim, FilmStrip strip) {
        switch (anim) {
            case FLY:
                flyingAnim= strip.copy();
                break;
            case FLAIL:
                flailingAnim= strip.copy();
                break;
            case DEATH:
                dyingAnim= strip.copy();
                break;
            case CHASE:
                chasingAnim= strip.copy();
                break;
            default:
                assert false : "Invalid Bee animation enumeration";
        }
    }

    /**
     * Animates the given animation.
     *
     * If the animation is not active, it will reset to the initial animation frame.
     *
     * @param  anim   The reference to the type of animation
     * @param  on       Whether the animation is active
     */
    public void animateBee(BeeAnimations anim, boolean on) {
        FilmStrip node = null;
        boolean  cycle = true;

        switch (anim) {
            case FLY:
                node  = flyingAnim;
                cycle = flyCycle;
                break;
            case FLAIL:
                node = flailingAnim;
                cycle = flailCycle;
                break;
            case DEATH:
                node = dyingAnim;
                cycle = deathCycle;
                break;
            case CHASE:
                node = chasingAnim;
                cycle = chaseCycle;
                break;
            // Add more cases for future animations
            default:
                assert false : "Invalid burner enumeration";
        }

        // If do not wish to cycle, only play animation once
        if (!cycle && node.getFrame() == node.getSize() - 1) {
            // Finished playing death animation, bee can be removed
            if (node == dyingAnim) {
                this.setIsTrulyDead(true);
            }
            return;
        }

        if (animFrames % FRAMES_PER_ANIM == 0) {
            if (node == dyingAnim){
                int nextFrame = (node.getFrame() + 1) % node.getSize();
                node.setFrame(nextFrame);
            }
            else {
                if (on) {
                    // Turn on the flames and go back and forth
                    if (node.getFrame() == 0) {
                        cycle = true;
                    } else if (node.getFrame() == node.getSize() - 1) {
                        cycle = false;
                    }

                    // Increment
                    if (cycle) {
                        node.setFrame(node.getFrame() + 1);
                    } else {
                        node.setFrame(0);
                    }
                } else {
                    node.setFrame(0);
                }
            }
        }
        animFrames++;

        switch (anim) {
            case FLY:
                flyCycle = cycle;
                break;
            case FLAIL:
                flailCycle = cycle;
                break;
            case DEATH:
                deathCycle = cycle;
                break;
            case CHASE:
                chaseCycle = cycle;
                break;
            // Add more cases for future animations
            default:
                assert false : "Invalid bee animation enumeration";
        }
    }

    @Override
    public void animateDeath(){
        System.out.println("Flying Bee Dying");
        this.animateBee(BeeAnimations.DEATH, true);
    }

    /**
     * Sets left/right movement of this character.
     * <p>
     * This is the result of input times ant's force.
     *
     * @param value left/right movement of this character.
     */
    public void setMovement(float value) {
        super.setMovement(value);
        animateBee(BeeAnimations.FLY, true);
        animateBee(BeeAnimations.FLAIL, true);
        animateBee(BeeAnimations.CHASE, true);
    }



    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = this.faceRight ? -1.0f : 1.0f;
        // FilmStrip currAnim = this.isChasing ? chasingAnim : flyingAnim;
        FilmStrip currAnim = flyingAnim;
        if (this.isDead){
            currAnim = dyingAnim;
        }
        else if (this.isChasing){
            currAnim = chasingAnim;
        }
        else if (this.isInHoney){
            // Can change this if it looks weird
            currAnim = flailingAnim;
        }
        // Walking Animation
        if (currAnim != null) {
            float offsety = currAnim.getRegionHeight()-origin.y;
            canvas.draw(currAnim, Color.WHITE,origin.x,offsety,getX()*drawScale.x,getY()*drawScale.x,getAngle(),effect,1);
        }
        // Stationary Bee
        else {
            System.out.println("MISSING FILMSTRIP");
            canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y, getAngle(), effect, 1.0f);
        }
    }


}
