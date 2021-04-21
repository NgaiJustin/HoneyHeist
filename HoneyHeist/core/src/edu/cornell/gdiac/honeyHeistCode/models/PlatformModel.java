package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.honeyHeistCode.obstacle.ComplexObstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.Obstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class PlatformModel extends Obstacle {
	/** The initializing data (to avoid magic numbers) */
	private final JsonValue data;

	/** The texture filmstrip for the left animation node */
	private FilmStrip shufflingAnim;
	/** The animation phase for the walking animation */
	private boolean shuffleCycle = true;
	private final int FRAMES_PER_ANIM = 5;
	private int animFrames = 0;

	/** Cache of the texture used by bodies in this Complex Obstacle */
	protected TextureRegion texture;

	protected Array<PolygonObstacle> bodies;

	private Vector2 worldCenter;

	protected NinePatch ninePatch;

	/**
	 * Enumeration to identify the platform animations
	 */
	public enum PlatformAnimations {
		/** Walking animation */
		SHUFFLE,
		// Future animations to be supported
	};

	/**
	 * Enumeration to represent the orientation of a platform.
	 *
	 * Platforms are named in clock wise order with zero being
	 * the horizontal platform.
	 */
	private enum PlatformOrientation {
		ZERO(0),
		ONE(60),
		TWO(120),
		THREE(180),
		FOUR(240),
		FIVE(300);

		private float degree;

		PlatformOrientation(float deg){
			this.degree = deg;
		}
		public float getDeg(){
			return degree;
		}
	}

	/**
	 * Creates a new platform model with the given data.
	 *
	 * The size is expressed in physics units NOT pixels.  In order for
	 * drawing to work properly, you MUST set the drawScale. The drawScale
	 * converts the physics units to pixels.
	 *
	 * @param data  	The physics constants and polygon information for the platforms in this model
	 */
	public PlatformModel(JsonValue data, String name) {
		super(0,0);
		bodies = new Array<PolygonObstacle>();

        this.data = data;

		String pname = name;

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

	public PlatformModel(JsonValue data, Vector2 worldCenter) {
		super(0,0);
		this.worldCenter = worldCenter;
		bodies = new Array<PolygonObstacle>();

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
	
	public PlatformModel() {
		super(0,0);
		bodies = new Array<PolygonObstacle>();
		data = null;

		//Probably replace the following code with json data
		rotationAngle = (float) Math.PI/3;
		rotationSpeed = (float) Math.PI/3;
	}

	public Iterable<PolygonObstacle> getBodies() { return bodies; }

	public Array<PolygonObstacle> getArrayBodies() {return bodies;}


	/**
	 *  rotates all bodies contained in the platform model about the given point by
	 *  the given amount of degrees in radians.
	 *
	 * @param amount	the amount in radians to be rotated
	 * @param point		the point to rotate about
	 */
	@Override
	public void rotateAboutPoint(float amount, Vector2 point) {
		for(Obstacle obj : bodies) {
			obj.rotateAboutPoint(amount,point);
		}
	}

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
	public void update(float dt) {
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
		rotateAboutPoint(rotationAmount, stageCenter);
	}

	/**
	 * Creates the physics Body(s) for this object, adding them to the world.
	 *
	 * This method invokes ActivatePhysics for the individual PhysicsObjects
	 * in the list.
	 *
	 * @param world Box2D world to store body
	 *
	 * @return true if object allocation succeeded
	 */
	public boolean activatePhysics(World world) {
		bodyinfo.active = true;
		boolean success = true;

		// Create all other bodies.
		for(PolygonObstacle obj : bodies) {
			success = success && obj.activatePhysics(world);
		}

		// Clean up if we failed
		if (!success) {
			deactivatePhysics(world);
		}
		return success;
	}

	/**
	 * Destroys the physics Body(s) of this object if applicable,
	 * removing them from the world.
	 *
	 * @param world Box2D world that stores body
	 */
	public void deactivatePhysics(World world) {
		if (bodyinfo.active) {
			for (Obstacle obj : bodies) {
				obj.deactivatePhysics(world);
			}
			bodyinfo.active = false;
		}
	}

	/**
	 * Sets the drawing scale for this physics object
	 *
	 * The drawing scale is the number of pixels to draw before Box2D unit. Because
	 * mass is a function of area in Box2D, we typically want the physics objects
	 * to be small.  So we decouple that scale from the physics object.  However,
	 * we must track the scale difference to communicate with the scene graph.
	 *
	 * We allow for the scaling factor to be non-uniform.
	 *
	 * @param x  the x-axis scale for this physics object
	 * @param y  the y-axis scale for this physics object
	 */
	public void setDrawScale(float x, float y) {
		drawScale.set(x,y);
		for(Obstacle obj : bodies) {
			obj.setDrawScale(x,y);
		}
	}

	/**
	 * Returns a PlatformOrientation classification based on the rectangular
	 * bound of a platform and the center of the world.
	 * @param maxx	max X value of the platform
	 * @param minx	min X value of the platform
	 * @param maxy	max Y value of the platform
	 * @param miny	min Y value of the platform
	 * @param worldCenter	(x,y) coordinate of the center of the world
	 * @return	Classification of the platform
	 */
	private PlatformOrientation getOrientation(float maxx,  float minx,
											   float maxy, float miny,
											   Vector2 worldCenter) {
		// Defensive copies
		worldCenter = worldCenter.cpy();

		// Case for horizontal platforms
		if (maxx > worldCenter.x && worldCenter.x > minx){
			// Top horizontal platform
			if (miny > worldCenter.y && maxy > worldCenter.y){
				return PlatformOrientation.THREE;
			}
			// Bot horizontal platform
			else if (miny < worldCenter.y && maxy < worldCenter.y){
				return PlatformOrientation.ZERO;
			}
			else {
				throw new IllegalArgumentException("Not a valid platform: ");
			}
		}

		// Case for right platforms
		if (minx > worldCenter.x && maxx > worldCenter.x){

			// Case for upper right platform
			if (maxy > worldCenter.y && miny > worldCenter.y){
				return PlatformOrientation.TWO;
			}
			// Case for bottom right platform
			else if (maxy < worldCenter.y && miny < worldCenter.y){
				return PlatformOrientation.ONE;
			}
			else {
				throw new IllegalArgumentException("Not a valid platform");
			}

		}
		return null;
	}

	/**
	 * Draws the physics object.
	 *
	 * @param canvas Drawing context
	 */
	public void draw(GameCanvas canvas) {
		// Delegate to components
		for (PolygonObstacle obj : bodies) {
			// obj.draw(canvas);

			if (worldCenter != null) {
//				canvas.draw(texture, Color.WHITE,
//						texture.getRegionWidth() /2f,
//						texture.getRegionHeight() /2f,
//						obj.getCenter().x * drawScale.x,
//						obj.getCenter().y * drawScale.y,
//						getAngle(), 1f, 1f);

//				canvas.drawNinePatch(ninePatch,
//						obj.getCenter().x * drawScale.x,
//						obj.getCenter().y * drawScale.y,
//						ninePatch.getMiddleWidth()/2,
//						ninePatch.getMiddleHeight()/2,
//						obj.getWidth()*drawScale.x,
//						obj.getHeight()*drawScale.y,
//						1f, 1f, 0);
				canvas.drawNinePatch(ninePatch,
						obj.minX * drawScale.x,
						obj.minY * drawScale.y,
						ninePatch.getMiddleWidth()/2,
						ninePatch.getMiddleHeight()/2,
						obj.getWidth()*drawScale.x,
						obj.getHeight()*drawScale.y,
						 30);
				System.out.println(obj.getTrueVertices().length);
			}
		}
	}

	/**
	 * Draws the outline of the physics body.
	 *
	 * This method can be helpful for understanding issues with collisions.
	 *
	 * @param canvas Drawing context
	 */
	public void drawDebug(GameCanvas canvas) {
		// Delegate to components
		for(Obstacle obj : bodies) {
			obj.drawDebug(canvas);
		}
	}


	
	public void setTexture(TextureRegion texture) {
		this.texture = texture;
		for(PolygonObstacle obj : bodies) {
			obj.setTexture(texture);
		}
	}

	public void setNinePatch(TextureRegion topLeft, TextureRegion topCenter, TextureRegion topRight,
							 TextureRegion midLeft, TextureRegion midCenter, TextureRegion midRight,
							 TextureRegion botLeft, TextureRegion botCenter, TextureRegion botRight) {
		this.ninePatch = new NinePatch(topLeft, topCenter, topRight,
				midLeft, midCenter, midRight,
				botLeft, botCenter, botRight);
	}

	/**
	 * Sets the animation node for the given afterburner
	 *
	 * @param  anim     enumeration to identify the ant animation
	 *
	 * @param  strip 	the animation filmstrip for the given animation
	 */
	public void setAnimationStrip(PlatformAnimations anim, FilmStrip strip) {
		switch (anim) {
			case SHUFFLE:
				shufflingAnim= strip;
				break;
			default:
				assert false : "Invalid ant animation enumeration";
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
	public void animatePlatform(PlatformAnimations anim, boolean on) {
		FilmStrip node = null;
		boolean cycle = true;
		switch (anim) {
			case SHUFFLE:
				node  = shufflingAnim;
				cycle = shuffleCycle;
				break;
			// Add more cases for future animations
			default:
				assert false : "Invalid burner enumeration";
		}

		if (node != null && animFrames % FRAMES_PER_ANIM == 0 ) {
			if (on) {
				// Turn on the flames and go back and forth
				if (node.getFrame() == 0 || node.getFrame() == 1) {
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
		animFrames = (animFrames + 1) % FRAMES_PER_ANIM;

		switch (anim) {
			case SHUFFLE:
				shuffleCycle = cycle;
				break;
			// Add more cases for future animations
			default:
				assert false : "Invalid burner enumeration";
		}
	}

	public void setSensor(boolean value) {
		for(PolygonObstacle obj : bodies) {
			obj.setSensor(value);
		}
	}
	
	public TextureRegion getTexture() {
		return texture;
	}
}
