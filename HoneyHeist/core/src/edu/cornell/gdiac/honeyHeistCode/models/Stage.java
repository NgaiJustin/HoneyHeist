/*
 * Spinner.java
 *
 * This class provides a spinning rectangle on a fixed pin.  We did not really need
 * a separate class for this, as it has no update.  However, ComplexObstacles always
 * make joint management easier.
 * 
 * This is one of the files that you are expected to modify. Please limit changes to 
 * the regions that say INSERT CODE HERE.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * Updated asset version, 2/6/2021
 */
package edu.cornell.gdiac.physics.platform;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.physics.box2d.joints.WeldJoint;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.ComplexObstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;
import edu.cornell.gdiac.physics.obstacle.WheelObstacle;

public class Stage extends ComplexObstacle {
	/** The initializing data (to avoid magic numbers) */
	private final JsonValue data;

	/** The primary spinner obstacle */
	private BoxObstacle barrier;

	private WheelObstacle centerPin;

	/**
	 * Creates a new spinner with the given physics data.
	 *
	 * The size is expressed in physics units NOT pixels.  In order for
	 * drawing to work properly, you MUST set the drawScale. The drawScale
	 * converts the physics units to pixels.
	 *
	 * @param data  	The physics constants for this rope bridge
	 * @param width		The object width in physics units
	 * @param height	The object width in physics units
	 */
	public Stage(JsonValue data, float width, float height, TextureRegion texture) {
        //super(data.get("pos").getFloat(0),data.get("pos").getFloat(1));
		super(16, 10);
        setName("stage");
        this.data = data;

        // Create the barrier
		//float x = data.get("pos").getFloat(0);
		//float y = data.get("pos").getFloat(1);
		float x = 16;
		float y = 10;

		JsonValue defaults = data.get("defaults");
		JsonValue platjv = data.get("platforms");
		String pname = "platform";

		for (int ii = 0; ii < platjv.size; ii++) {

			PolygonObstacle obj;
			obj = new PolygonObstacle(platjv.get(ii).asFloatArray(), 0, 0);
			obj.setBodyType(BodyDef.BodyType.KinematicBody);
			obj.setDensity(defaults.getFloat("density", 0.0f));
			obj.setFriction(defaults.getFloat("friction", 0.0f));
			obj.setRestitution(defaults.getFloat("restitution", 0.0f));
			//obj.setDrawScale(scale);
			obj.setTexture(texture);
			obj.setName(pname + ii);
			bodies.add(obj);
		}

		/*
        barrier = new BoxObstacle(x,y,width,height);
        barrier.setName("barrier");
        barrier.setDensity(data.getFloat("high_density", 0));
        bodies.add(barrier);

		 */
        
		//#region INSERT CODE HERE
        // Create a pin to anchor the barrier 
        // Radius:  data.getFloat("radius")
        // Density: data.getFloat("low_density")
		// Name: "pin"

		//WheelObstacle pin = new WheelObstacle(x, y, data.getFloat("radius"));
		WheelObstacle pin = new WheelObstacle(x, y, 5);
		pin.setName("pin");
		//pin.setDensity(data.getFloat("low_density"));
		pin.setDensity(0);
		pin.setBodyType(BodyDef.BodyType.DynamicBody);
		centerPin = pin;
		bodies.add(pin);
        		
        //#endregion
    }
	
	/**
	 * Creates the joints for this object.
	 * 
	 * We implement our custom logic here.
	 *
	 * @param world Box2D world to store joints
	 *
	 * @return true if object allocation succeeded
	 */
	protected boolean createJoints(World world) {
		assert bodies.size > 0;

		//#region INSERT CODE HERE
		// Attach the barrier to the pin here

		for (int i=1; i<bodies.size; i++){
			WeldJointDef jointDef = new WeldJointDef();
			jointDef.bodyA = bodies.get(0).getBody();
			jointDef.bodyB = bodies.get(i).getBody();
			jointDef.localAnchorA.set(new Vector2());
			jointDef.localAnchorB.set(new Vector2());
			jointDef.collideConnected = false;
			Joint joint = world.createJoint(jointDef);
			joints.add(joint);
		}

		//#endregion

		return true;
	}
	
	public void setTexture(TextureRegion texture) {
		barrier.setTexture(texture);
	}
	
	public TextureRegion getTexture() {
		return barrier.getTexture();
	}

	public void rotate(){
		centerPin.setAngle(centerPin.getAngle()+10);
	}
}
