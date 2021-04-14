package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;

public class FlyingBeeModel extends AbstractBeeModel{
    private float vMovement;
    private float vMovementScale;

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
        vMovementScale = 1.0f;
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
        vMovement = value * vMovementScale;
        if (value > 1.0f) {
//            System.out.println("vertical movement might be too fast");
        }
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
            System.out.println("slowing down x: " + getVX());
            System.out.println("slowing down y: " + getVY());
            body.applyForce(forceCache, getPosition(), true);
        }
        else if (getVMovement() == 0f && getVY() != 0f) {
            forceCache.set(0, -getDamping() * getVY());
            System.out.println("slowing down y: " + getVY());
            body.applyForce(forceCache, getPosition(), true);
        }
        else if (getMovement() == 0f){
            forceCache.set(-getDamping() * getVX(), 0);
            System.out.println("slowing down x: " + getVX());
            body.applyForce(forceCache, getPosition(), true);
        }

        // Velocity too high, clamp it
        if (Math.abs(getVX()) >= getMaxSpeed()) {
            setVX(Math.signum(getVX()) * getMaxSpeed());
        }
        if (Math.abs(getVY()) >= getMaxSpeed()) {
            setVY(Math.signum(getVY()) * getMaxSpeed());
        }
        if((Math.copySign(1.0f,getVX())!=Math.copySign(1.0f,getMovement()))||!(Math.abs(getVX()) >= getMaxSpeed())){
            forceCache.set(getMovement(), getVMovement());
            System.out.println(getMovement() + ", " + getVMovement());
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

}
