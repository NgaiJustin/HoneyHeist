package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;

public class FlyingBeeModel extends AbstractBeeModel{
    /**
     * Creates a bee avatar with the given physics data
     *
     * @param data   The physics constants for the player Ant
     * @param width  The object width in physics units
     * @param height The object width in physics units
     */
    public FlyingBeeModel(JsonValue data, float x, float y, float width, float height) {
        super(data, x, y, width, height);
        setGravityScale(0);
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
        Vector2 sensorCenter = new Vector2(0, -getHeight() / 6);
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

}
