package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.honeyHeistCode.MyTenPatch;
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

	public final float D_THICKNESS = 0.5f;

	protected NinePatch ninePatch; // TODO: TO BE REPLACED WHEN TENPATCH IS COMPLETE

	protected MyTenPatch tenPatch;

	// To draw the tilled center tiles
	private TextureRegion UMid;
	private TextureRegion MMid;
	private TextureRegion BMid;

	// Edge tiles
	private TextureRegion ULeft;
	private TextureRegion URight;

	/**
	 * Enumeration to identify the platform animations
	 */
	public enum PlatformAnimations {
		/** Walking animation */
		SHUFFLE,
		// Future animations to be supported
	};


	/**
	 * Creates a new platform model with the given data.
	 *
	 * The size is expressed in physics units NOT pixels.  In order for
	 * drawing to work properly, you MUST set the drawScale. The drawScale
	 * converts the physics units to pixels.
	 *
	 * @param data  	The physics constants and polygon information for the platforms in this model
	 */
	public PlatformModel(JsonValue data, String name, Vector2 worldCenter) {
		super(0,0);
		this.worldCenter = worldCenter;
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
		//rotationAngle = (float) Math.PI/3;
		//rotationSpeed = ((float) Math.PI/3)*1.3f;
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
		//rotationAngle = (float) Math.PI/3;
		//rotationSpeed = ((float) Math.PI/3)*1.3f;
	}
	
	public PlatformModel() {
		super(0,0);
		bodies = new Array<PolygonObstacle>();
		data = null;

		//Probably replace the following code with json data
		//rotationAngle = (float) Math.PI/3;
		//rotationSpeed = ((float) Math.PI/3)*1.3f;
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

		getRotSpeed(dt);
		float rotationAmount = currentSpeed * dt;
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
	 * Draws the physics object.
	 *
	 * @param canvas Drawing context
	 */
	public void draw(GameCanvas canvas) {
		// Delegate to components
		for (PolygonObstacle obj : bodies) {
			// obj.draw(canvas);
			if (worldCenter != null) {
				float[] corners = obj.getTruePoints();
				assert corners.length == 8;
				Vector2 topLeft = new Vector2(corners[0], corners[1]);
				Vector2 botLeft = new Vector2(corners[2], corners[3]);
				Vector2 botRight = new Vector2(corners[4], corners[5]);
				Vector2 topRight = new Vector2(corners[6], corners[7]);
				float trueWidth = Math.max(botLeft.dst(botRight), botLeft.dst(topLeft));
				float trueHeight = obj.PLATFORM_HEIGHT;

				Vector2 objCenter = obj.getCenter();
				Vector2 scaledPlatCenter = new Vector2(
						objCenter.x * drawScale.x,
						objCenter.y * drawScale.y);

				Vector2 scaledWorldCenter = new Vector2(
						worldCenter.x * drawScale.x,
						worldCenter.y * drawScale.y
				);

				Vector2 botLeftToRight = new Vector2(botRight.cpy().sub(botLeft.cpy()));
				float angle = botLeftToRight.angleDeg();


				float step = D_THICKNESS;

				// Draw the tiled topCenter, midCenter and botCenter tiles.
				// Vector2 topStart = nextVert(objCenter, angle + 90, step);
				Vector2 midStart = objCenter.cpy();
				// Vector2 botStart = nextVert(objCenter, angle - 90, step);

				float minx = Math.min(Math.min(Math.min(corners[0], corners[2]), corners[4]), corners[6]);
				float maxx = Math.max(Math.max(Math.max(corners[0], corners[2]), corners[4]), corners[6]);
				float miny = Math.min(Math.min(Math.min(corners[1], corners[3]), corners[5]), corners[7]);
				float maxy = Math.max(Math.max(Math.max(corners[1], corners[3]), corners[5]), corners[7]);

				int numCenters = (int) (trueWidth / step);
				// FloatArray topTileCenters = computerCenters(topStart, angle, step, numCenters, minx, maxx, miny, maxy);
				FloatArray midTileCenters = computerCenters(midStart, angle, step, numCenters, minx, maxx, miny, maxy);
				// FloatArray botTileCenters = computerCenters(botStart, angle, step, numCenters, minx, maxx, miny, maxy);

				// Tile center tiles first
				assert UMid != null: "Missing upper center tile texture";
				assert MMid != null: "Missing middle center tile texture";
				assert BMid != null: "Missing bottom center tile texture";
				for (int ii = 0; ii < midTileCenters.size; ii += 2){
					canvas.draw(UMid, Color.WHITE,
							UMid.getRegionHeight()/2f,
							UMid.getRegionWidth()/2f,
							midTileCenters.get(ii) * drawScale.x,
							midTileCenters.get(ii+1) * drawScale.y,
							angle * MathUtils.degRad, 1f, 1f);
				}

				// Draw left edge tiles
				float xstep = MathUtils.cosDeg(angle) * (trueWidth/2 - step/2);
				float ystep = MathUtils.sinDeg(angle) * (trueWidth/2 - step/2);
				Vector2 leftEdgeCenter = midStart.cpy().add(-xstep, -ystep);
				canvas.draw(ULeft, Color.WHITE,
						ULeft.getRegionHeight()/2f,
						ULeft.getRegionWidth()/2f,
						leftEdgeCenter.x * drawScale.x,
						leftEdgeCenter.y * drawScale.y,
						angle * MathUtils.degRad, 1f, 1f);

				// Draw right edge tiles
				Vector2 rightEdgeCenter = midStart.cpy().add(xstep, ystep);
				canvas.draw(URight, Color.WHITE,
						URight.getRegionHeight()/2f,
						URight.getRegionWidth()/2f,
						rightEdgeCenter.x * drawScale.x,
						rightEdgeCenter.y * drawScale.y,
						angle * MathUtils.degRad, 1f, 1f);

				// TODO: Replace with myTenPatch
			}
		}
	}

	/**
	 * Returns the result of moving a step amount in the given angle from v
	 * @param v 	Starting coordinate
	 * @param angle	Travel angle (degrees)
	 * @param step	Distance to travel
	 * @return		Float array for new coordinate [x, y]
	 */
	private Vector2 nextVert(Vector2 v, float angle, float step){
		float cos = MathUtils.cosDeg(angle) * step;
		float sin = MathUtils.sinDeg(angle) * step;
		return v.cpy().add(cos, sin);
	}

	/** Returns an array of x,y coordinates denote where the tiled centers should be */
	private FloatArray computerCenters(Vector2 startVert, float angle, float step, int centerNum,
									   float minx, float maxx, float miny, float maxy){

		FloatArray temp = new FloatArray(2*centerNum);
		float cos = MathUtils.cosDeg(angle) * step;
		float sin = MathUtils.sinDeg(angle) * step;

		// First half of centers: moving forward
		Vector2 v = startVert.cpy();
		float[] vbound = getSquareBounds(v.x, v.y, angle, step/2);
		float vminx = vbound[0];
		float vmaxx = vbound[1];
		float vminy = vbound[2];
		float vmaxy = vbound[3];

		// Only draw if the square is within the bound of the platform
		while(minx <= vminx && vmaxx <= maxx && miny <= vminy && vmaxy <= maxy){
			temp.add(v.x);
			temp.add(v.y);
			v.add(cos, sin);

			// Compute new bounds of the square to be drawn
			vbound = getSquareBounds(v.x, v.y, angle, step/2);
			vminx = vbound[0];
			vmaxx = vbound[1];
			vminy = vbound[2];
			vmaxy = vbound[3];
		}

		// Second half of centers: moving backwards
		v = startVert.cpy().add(-cos, -sin);
		vbound = getSquareBounds(v.x, v.y, angle, step/2);
		vminx = vbound[0];
		vmaxx = vbound[1];
		vminy = vbound[2];
		vmaxy = vbound[3];

		// Only draw if the square is within the bound of the platform
		while(minx <= vminx && vmaxx <= maxx && miny <= vminy && vmaxy <= maxy){
			temp.add(v.x);
			temp.add(v.y);
			v.add(-cos, -sin);

			// Compute new bounds of the square to be drawn
			vbound = getSquareBounds(v.x, v.y, angle, step/2);
			vminx = vbound[0];
			vmaxx = vbound[1];
			vminy = vbound[2];
			vmaxy = vbound[3];
		}
		return temp;
	}

	private float[] getSquareBounds(float cx, float cy, float angle, float radius){
		float xmin = cx;
		float xmax = cx;
		float ymin = cy;
		float ymax = cy;
		for (int q = 45; q < 360; q += 90){
			float x = MathUtils.cosDeg(angle + q ) * radius + cx;
			float y = MathUtils.sinDeg(angle + q ) * radius + cy;
			xmin = Math.min(xmin, x);
			xmax = Math.max(xmax, x);
			ymin = Math.min(ymin, y);
			ymax = Math.max(ymax, y);
		}

		return new float[]{xmin, xmax, ymin, ymax};
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

	public void setNinePatch(NinePatch patch){
		this.ninePatch = patch;
	}

	public void setTenPatch(TextureRegion topLeft, TextureRegion topCenter, TextureRegion topRight,
							 TextureRegion midLeft, TextureRegion midCenter, TextureRegion midRight,
							 TextureRegion botLeft, TextureRegion botCenter, TextureRegion botRight) {
		// TODO: Remove unnecessary texture regions
		this.UMid = topCenter;
		this.MMid = midCenter;
		this.BMid = botCenter;
		this.ULeft = topLeft;
		this.URight = topRight;
		TextureRegion[] t = new TextureRegion[]{
				topLeft, topCenter, topRight,
				midLeft, midCenter, midRight,
				botLeft, botCenter, botRight
		};
		this.tenPatch = new MyTenPatch(t);
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
