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
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.ComplexObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

public class PlatformModel extends ComplexObstacle {
	/** The initializing data (to avoid magic numbers) */
	private final JsonValue data;

	/** Cache of the texture used by bodies in this Complex Obstacle */
	protected TextureRegion texture;

	/** Origin of the stage */
	private Vector2 origin;

	/** total radians for a single rotation */
	float rotationAngle;

	/** Amount of radians remaining to be rotated */
	private float remainingAngle;

	/** Whether the platforms are rotating or not */
	private boolean isRotating = false;

	/** Speed at which the platforms rotate in radians/second */
	private float rotationSpeed;

	/** Whether the rotation is clockwise or not */
	private boolean isClockwise;


	/**
	 * Creates a new platform model with the given data.
	 *
	 * The size is expressed in physics units NOT pixels.  In order for
	 * drawing to work properly, you MUST set the drawScale. The drawScale
	 * converts the physics units to pixels.
	 *
	 * @param data  	The physics constants and polygon information for the platforms in this model
	 */
	public PlatformModel(JsonValue data) {
		super(0,0);
        this.data = data;

		String pname = "platform";

		for (int ii = 0; ii < data.size; ii++) {
			PolygonObstacle obj;
			obj = new PolygonObstacle(data.get(ii).asFloatArray(), 0, 0);
			obj.setBodyType(BodyDef.BodyType.StaticBody);
			obj.setDensity(data.getFloat( "density", 0.0f ));
			obj.setFriction(data.getFloat( "friction", 0.0f ));
			obj.setRestitution(data.getFloat( "restitution", 0.0f ));
			obj.setName(pname+ii);
			bodies.add(obj);
		}

		//Probably replace the following code with json data
		rotationAngle = (float) Math.PI/3;
		rotationSpeed = (float) Math.PI/3;
    }


	/**
	 *  rotates all bodies contained in the platform model about the given point by
	 *  the given amount of degrees in radians.
	 *
	 * @param amount	the amount in radians to be rotated
	 * @param point		the point to rotate about
	 */
	public void rotateAboutPoint(float amount, Vector2 point) {
		for(Object obj : bodies) {
			Body body = ((PolygonObstacle)obj).getBody();
			Transform bT = body.getTransform();
			Vector2 p = bT.getPosition().sub(point);
			float c = (float) Math.cos(amount);
			float s = (float) Math.sin(amount);
			float x = p.x * c - p.y * s;
			float y = p.x * s + p.y * c;
			Vector2 pos = new Vector2(x, y).add(point);
			float angle = bT.getRotation() + amount;
			body.setTransform(pos, angle);
		}
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

		//#endregion

		return true;
	}


	/**
	 * adds the specified amount (in radians) to the total remaining
	 * rotation of the platform model
	 *
	 * @param amount	the amount in radians to be added to total rotation
	 */
	public void addRotation(float amount) { remainingAngle += amount; }

	public float getRemainingAngle() { return remainingAngle; }

	/**
	 * Updates the object's physics state (NOT GAME LOGIC).
	 *
	 * This method is called AFTER the collision resolution state. Therefore, it
	 * should not be used to process actions or any other gameplay information.  Its
	 * primary purpose is to adjust changes to the fixture, which have to take place
	 * after collision.
	 *
	 * @param dt Timing values from parent loop
	 */
	public void update(float dt){
		if (!isRotating) return;


		float rotationAmount = rotationSpeed * dt;
		if (rotationAmount > remainingAngle){
			rotationAmount = remainingAngle;
			isRotating = false;
		}
		remainingAngle -= rotationAmount;
		if (!isClockwise) {
			rotationAmount *= -1;
		}
		rotateAboutPoint(rotationAmount, origin);

	}

	/**
	 * Begins rotation of the stage.
	 *
	 * @param isClockwise true if rotating clockwise, false if rotating counterclockwise.
	 */
	public void startRotation(boolean isClockwise, Vector2 point){
		if (isRotating) return;

		origin = point;
		isRotating = true;
		this.isClockwise = isClockwise;
		addRotation(rotationAngle);
	}

	
	public void setTexture(TextureRegion texture) {
		this.texture = texture;
		for(Obstacle obj : bodies) {
			((PolygonObstacle) obj).setTexture(texture);
		}

	}
	
	public TextureRegion getTexture() {
		return texture;
	}
}