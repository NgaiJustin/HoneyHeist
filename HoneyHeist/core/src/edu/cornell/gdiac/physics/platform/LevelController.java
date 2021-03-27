/*
 * PlatformController.java
 *
 * You SHOULD NOT need to modify this file.  However, you may learn valuable lessons
 * for the rest of the lab by looking at it.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * Updated asset version, 2/6/2021
 */
package edu.cornell.gdiac.physics.platform;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundBuffer;
import edu.cornell.gdiac.physics.InputController;
import edu.cornell.gdiac.physics.WorldController;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;

/**
 * Gameplay specific controller for the platformer game.
 * <p>
 * You will notice that asset loading is not done with static methods this time.
 * Instance asset loading makes it easier to process our game modes in a loop, which
 * is much more scalable. However, we still want the assets themselves to be static.
 * This is the purpose of our AssetState variable; it ensures that multiple instances
 * place nicely with the static assets.
 */
public class LevelController extends WorldController implements ContactListener {
    /**
     * Texture asset for player avatar
     */
    private TextureRegion avatarTexture;
    /**
     * Texture asset for chaser bee avatar
     */
    private TextureRegion chaserBeeTexture;
    /**
     * Texture asset for testEnemy avatar
     */
    private TextureRegion sleeperBeeTexture;
//	/** Texture asset for the spinning barrier */
//	private TextureRegion barrierTexture;
//	/** Texture asset for the bullet */
//	private TextureRegion bulletTexture;
//	/** Texture asset for the bridge plank */
//	private TextureRegion bridgeTexture;

    /**
     * The jump sound.  We only want to play once.
     */
    private SoundBuffer jumpSound;
    private long jumpId = -1;
    /**
     * The weapon fire sound.  We only want to play once.
     */
    private SoundBuffer fireSound;
    private long fireId = -1;
    /**
     * The weapon pop sound.  We only want to play once.
     */
    private SoundBuffer plopSound;
    private long plopId = -1;
    /**
     * The default sound volume
     */
    private float volume;

    // Physics objects for the game
    /**
     * Physics constants for initialization
     */
    private JsonValue constants;
    /**
     * Reference to the character avatar
     */
    private AntModel avatar;
    /**
     * Reference to the chaserBee model
     */
    private ChaserBeeModel chaserBee;
    /**
     * Reference to the sleeperBee model
     */
    private SleeperBeeModel sleeperBee;
    /**
     * Reference to the goalDoor (for collision detection)
     */
    private BoxObstacle goalDoor;
    /**
     * Reference to the platform model
     */
    private PlatformModel platforms;

    /**
     * Mark set to handle more sophisticated collision callbacks
     */
    protected ObjectSet<Fixture> sensorFixtures;

    /**
     * Origin of the world
     */
    private Vector2 origin;

    /**
     * Creates and initialize a new instance of the platformer game
     * <p>
     * The game has default gravity and other settings
     */
    public LevelController() {
        super(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_GRAVITY);
        setDebug(false);
        setComplete(false);
        setFailure(false);
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();
        origin = new Vector2(bounds.width / 2, bounds.height / 2);
    }

    /**
     * Gather the assets for this controller.
     * <p>
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     *
     * @param directory Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        avatarTexture = new TextureRegion(directory.getEntry("platform:ant", Texture.class));
        chaserBeeTexture = new TextureRegion(directory.getEntry("platform:chaserBee", Texture.class));
        sleeperBeeTexture = new TextureRegion(directory.getEntry("platform:sleeperBee", Texture.class));
//		barrierTexture = new TextureRegion(directory.getEntry("platform:barrier",Texture.class));
//		bulletTexture = new TextureRegion(directory.getEntry("platform:bullet",Texture.class));
//		bridgeTexture = new TextureRegion(directory.getEntry("platform:rope",Texture.class));

        jumpSound = directory.getEntry("platform:jump", SoundBuffer.class);
        fireSound = directory.getEntry("platform:pew", SoundBuffer.class);
        plopSound = directory.getEntry("platform:plop", SoundBuffer.class);

        constants = directory.getEntry("platform:constants", JsonValue.class);
        super.gatherAssets(directory);
    }

    /**
     * Resets the status of the game so that we can play again.
     * <p>
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        Vector2 gravity = new Vector2(world.getGravity());

        for (Obstacle obj : objects) {
            obj.deactivatePhysics(world);
        }
        objects.clear();
        addQueue.clear();
        world.dispose();

        world = new World(gravity, false);
        world.setContactListener(this);
        setComplete(false);
        setFailure(false);
        populateLevel();
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel() {
        // Add level goal
        float dwidth = goalTile.getRegionWidth() / scale.x;
        float dheight = goalTile.getRegionHeight() / scale.y;

        JsonValue goal = constants.get("goal");
        JsonValue goalpos = goal.get("pos");
        goalDoor = new BoxObstacle(goalpos.getFloat(0), goalpos.getFloat(1), dwidth, dheight);
        goalDoor.setBodyType(BodyDef.BodyType.StaticBody);
        goalDoor.setDensity(goal.getFloat("density", 0));
        goalDoor.setFriction(goal.getFloat("friction", 0));
        goalDoor.setRestitution(goal.getFloat("restitution", 0));
        goalDoor.setSensor(true);
        goalDoor.setDrawScale(scale);
        goalDoor.setTexture(goalTile);
        goalDoor.setName("goal");
        addObject(goalDoor);

        String wname = "wall";
        JsonValue walljv = constants.get("walls");
        JsonValue defaults = constants.get("defaults");
        for (int ii = 0; ii < walljv.size; ii++) {
            PolygonObstacle obj;
            obj = new PolygonObstacle(walljv.get(ii).asFloatArray(), 0, 0);
            obj.setBodyType(BodyDef.BodyType.StaticBody);
            obj.setDensity(defaults.getFloat("density", 0.0f));
            obj.setFriction(defaults.getFloat("friction", 0.0f));
            obj.setRestitution(defaults.getFloat("restitution", 0.0f));
            obj.setDrawScale(scale);
            obj.setTexture(earthTile);
            obj.setName(wname + ii);
            addObject(obj);
        }

        // Create platforms
        platforms = new PlatformModel(constants.get("platforms"));
        platforms.setDrawScale(scale);
        platforms.setTexture(earthTile);
        addObject(platforms);

        //Create test platform from new json data
        //DELETE THIS
        JsonValue platformdata = constants.get("testPlatform");
        PolygonObstacle obj = new PolygonObstacle(platformPointsFromJson(platformdata), 0, 0);
        obj.setBodyType(BodyDef.BodyType.StaticBody);
        obj.setDensity(defaults.getFloat("density", 0.0f));
        obj.setFriction(defaults.getFloat("friction", 0.0f));
        obj.setRestitution(defaults.getFloat("restitution", 0.0f));
        obj.setDrawScale(scale);
        obj.setTexture(earthTile);
        obj.setName("testPlatform");
        addObject(obj);


        // This world is heavier
        world.setGravity(new Vector2(0, defaults.getFloat("gravity", 0)));

        // Create player (ant)
        dwidth = avatarTexture.getRegionWidth() / scale.x;
        dheight = avatarTexture.getRegionHeight() / scale.y;
        avatar = new AntModel(constants.get("player"), dwidth, dheight);
        avatar.setDrawScale(scale);
        avatar.setTexture(avatarTexture);
        addObject(avatar);

        // Create one chaser bee
        dwidth = chaserBeeTexture.getRegionWidth() / scale.x;
        dheight = chaserBeeTexture.getRegionHeight() / scale.y;
        chaserBee = new ChaserBeeModel(constants.get("chaserBee"), dwidth, dheight);
        chaserBee.setDrawScale(scale);
        chaserBee.setTexture(chaserBeeTexture);
        addObject(chaserBee);

        // Create one sleeper bee
        dwidth = sleeperBeeTexture.getRegionWidth() / scale.x;
        dheight = sleeperBeeTexture.getRegionHeight() / scale.y;
        sleeperBee = new SleeperBeeModel(constants.get("sleeperBee"), dwidth, dheight);
        sleeperBee.setDrawScale(scale);
        sleeperBee.setTexture(sleeperBeeTexture);
        // Sleeper bee doesn't move
        sleeperBee.setBodyType(BodyDef.BodyType.StaticBody);
        addObject(sleeperBee);

        // Create rope bridge
//		dwidth  = bridgeTexture.getRegionWidth()/scale.x;
//		dheight = bridgeTexture.getRegionHeight()/scale.y;
//		RopeBridge bridge = new RopeBridge(constants.get("bridge"), dwidth, dheight);
//		bridge.setTexture(bridgeTexture);
//		bridge.setDrawScale(scale);
//		addObject(bridge);

        // Create spinning platform
//		dwidth  = barrierTexture.getRegionWidth()/scale.x;
//		dheight = barrierTexture.getRegionHeight()/scale.y;
//		Spinner spinPlatform = new Spinner(constants.get("spinner"),dwidth,dheight);
//		spinPlatform.setDrawScale(scale);
//		spinPlatform.setTexture(barrierTexture);
//		addObject(spinPlatform);

        volume = constants.getFloat("volume", 1.0f);
    }

	/**
	 * Start rotation.
	 *
	 * @param isClockwise true if the rotation direction is clockwise, false if counterclockwise.
	 * @param antRotating true if the ant also needs to be rotated with the stage.
	 */
	public void rotate(boolean isClockwise, boolean antRotating){
		platforms.startRotation(isClockwise, origin);
		if (antRotating){
			avatar.setBodyType(BodyDef.BodyType.StaticBody);
			avatar.startRotation(isClockwise, origin);
		}
	}

    /**
     * Start rotation for a single enemy chaserBee
     *
     * @param isClockwise true if the rotation direction is clockwise, false if counterclockwise.
     */
    public void rotateChaserBee(boolean isClockwise, boolean platformCheck, ChaserBeeModel bee) {
        System.out.println("Bee is grounded status: " + bee.isGrounded());
        System.out.println("Platform rotation status: " + bee.isGrounded());
        if(bee.isGrounded() && platformCheck) {
            chaserBee.setBodyType(BodyDef.BodyType.StaticBody);
            System.out.println("Bee stuck");
            chaserBee.startRotation(isClockwise, origin);
        }
    }


    /**
     * Start clockwise rotation.
     * Will only rotate once, and spamming will not queue more rotations.
     */
    public void rotateClockwise() {
        //platforms.startRotation(true, origin);
        boolean platformNotRotating = !platforms.getIsRotating();

        rotate(true, avatar.isGrounded()&&platformNotRotating);

        // Iterate over the list of bees
        rotateChaserBee(true, platformNotRotating, chaserBee);
    }

    /**
     * Start counterclockwise rotation.
     * Will only rotate once, and spamming will not queue more rotations.
     */
    public void rotateCounterClockwise() {
        //platforms.startRotation(false, origin);
        boolean platformNotRotating = !platforms.getIsRotating();

        rotate(false, avatar.isGrounded()&&platformNotRotating);

        // Iterate over the list of bees
        rotateChaserBee(false, platformNotRotating, chaserBee);
    }

    /**
     * Moves the ant based on the direction given
     *
     * @param direction -1 = left, 1 = right, 0 = still
     */
    public void moveAnt(float direction) {
        avatar.setMovement(direction * avatar.getForce());
    }

    /**
     * Moves the chaserBee based on the direction given by AIController
     *
     * @param direction -1 = left, 1 = right, 0 = still
     */
    public void moveChaserBee(float direction, ChaserBeeModel bee) {
        bee.setMovement(direction * bee.getForce());
    }


    public PlatformModel getPlatforms() {
        return platforms;
    }

    public AntModel getAvatar() {
        return avatar;
    }

    /**
     * Returns whether to process the update loop
     * <p>
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode.  If not, the update proceeds
     * normally.
     *
     * @param dt Number of seconds since last animation frame
     * @return whether to process the update loop
     */
    public boolean preUpdate(float dt) {
        if (!super.preUpdate(dt)) {
            return false;
        }

        if (!isFailure() && avatar.getY() < -1) {
            setFailure(true);
            return false;
        }

        return true;
    }

    /**
     * The core gameplay loop of this world.
     * <p>
     * This method contains the specific update code for this mini-game. It does
     * not handle collisions, as those are managed by the parent class WorldController.
     * This method is called after input is read, but before collisions are resolved.
     * The very last thing that it should do is apply forces to the appropriate objects.
     *
     * @param dt Number of seconds since last animation frame
     */
    public void update(float dt) {
        // Process actions in object model
        moveAnt(InputController.getInstance().getHorizontal());
        //avatar.setJumping(InputController.getInstance().didPrimary());
        //avatar.setShooting(InputController.getInstance().didSecondary());

        // Add a bullet if we fire
        //if (avatar.isShooting()) {
        //	createBullet();
        //}

        avatar.applyForce();
        //if (avatar.isJumping()) {
        // 	jumpId = playSound( jumpSound, jumpId, volume );
        //}

        // Process AI action
        // 1. Loop over all chaser bee,
        // 2. For each bee, moveChaserBee(...);
        // TO BE IMPLEMENTED
        chaserBee.applyForce();

        if (platforms != null) {
            //Vector2 worldPoint = new Vector2(16f, 9f);
            //platforms.rotateAboutPoint(0.1f*dt,worldPoint);
            if (InputController.getInstance().didRotate()) {
                rotateClockwise();
            } else if (InputController.getInstance().didAntiRotate()) {
                rotateCounterClockwise();
            }
        }


    }

//	/** Add a new bullet to the world and send it in the right direction. */
//	private void createBullet() {
//		JsonValue bulletjv = constants.get("bullet");
//		float offset = bulletjv.getFloat("offset",0);
//		offset *= (avatar.isFacingRight() ? 1 : -1);
//		float radius = bulletTexture.getRegionWidth()/(2.0f*scale.x);
//		WheelObstacle bullet = new WheelObstacle(avatar.getX()+offset, avatar.getY(), radius);
//
//	    bullet.setName("bullet");
//		bullet.setDensity(bulletjv.getFloat("density", 0));
//	    bullet.setDrawScale(scale);
//	    bullet.setTexture(bulletTexture);
//	    bullet.setBullet(true);
//	    bullet.setGravityScale(0);
//
//		// Compute position and velocity
//		float speed = bulletjv.getFloat( "speed", 0 );
//		speed  *= (avatar.isFacingRight() ? 1 : -1);
//		bullet.setVX(speed);
//		addQueuedObject(bullet);
//
//		fireId = playSound( fireSound, fireId );
//	}

//	/**
//	 * Remove a new bullet from the world.
//	 *
//	 * @param  bullet   the bullet to remove
//	 */
//	public void removeBullet(Obstacle bullet) {
//	    bullet.markRemoved(true);
//	    plopId = playSound( plopSound, plopId );
//	}


    /**
     * Callback method for the start of a collision
     * <p>
     * This method is called when we first get a collision between two objects.  We use
     * this method to test if it is the "right" kind of collision.  In particular, we
     * use it to test if we made it to the win door.
     *
     * @param contact The two bodies that collided
     */
    public void beginContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        try {
            Obstacle bd1 = (Obstacle) body1.getUserData();
            Obstacle bd2 = (Obstacle) body2.getUserData();

//			// Test bullet collision with world
//			if (bd1.getName().equals("bullet") && bd2 != avatar) {
//		        removeBullet(bd1);
//			}

//			if (bd2.getName().equals("bullet") && bd1 != avatar) {
//		        removeBullet(bd2);
//			}

            // See if we have landed on the ground.
            if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                    (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
                avatar.setGrounded(true);
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground
            }
            // ITERATE OVER ALL CHASER BEES
            if ((chaserBee.getSensorName().equals(fd2) && chaserBee != bd1) ||
                    (chaserBee.getSensorName().equals(fd1) && chaserBee != bd2)) {
                chaserBee.setGrounded(true);
                sensorFixtures.add(chaserBee == bd1 ? fix2 : fix1); // Could have more than one ground
            }
            // Check for win condition
            if ((bd1 == avatar && bd2 == goalDoor) ||
                    (bd1 == goalDoor && bd2 == avatar)) {
                setComplete(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Callback method for the start of a collision
     * <p>
     * This method is called when two objects cease to touch.  The main use of this method
     * is to determine when the characer is NOT on the ground.  This is how we prevent
     * double jumping.
     */
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bd1 = body1.getUserData();
        Object bd2 = body2.getUserData();

        if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
            }
        }
        if ((chaserBee.getSensorName().equals(fd2) && chaserBee != bd1) ||
                (chaserBee.getSensorName().equals(fd1) && chaserBee != bd2)) {
            sensorFixtures.remove(chaserBee == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                chaserBee.setGrounded(false);
            }
        }
    }

    /**
     * Unused ContactListener method
     */
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }

    /**
     * Unused ContactListener method
     */
    public void preSolve(Contact contact, Manifold oldManifold) {
    }

    /**
     * Called when the Screen is paused.
     * <p>
     * We need this method to stop all sounds when we pause.
     * Pausing happens when we switch game modes.
     */
    public void pause() {
        if (jumpSound.isPlaying(jumpId)) {
            jumpSound.stop(jumpId);
        }
        if (plopSound.isPlaying(plopId)) {
            plopSound.stop(plopId);
        }
        if (fireSound.isPlaying(fireId)) {
            fireSound.stop(fireId);
        }
    }

    public float[] platformPointsFromJson(JsonValue platformData){
        JsonValue pos = platformData.get("position");
        JsonValue scale = platformData.get("scale");
        float x = pos.getFloat("x");
        float y = pos.getFloat("y");
        float width = scale.getFloat("width");
        float w = width/2;
        float height = scale.getFloat("height");
        float h = height/2;
        float rot = platformData.getFloat("local_rotation") * 2 * (float)Math.PI/360;
        float[] points = new float[]{-w, h, -w, -h, w, -h, w, h};
        float cos = (float)Math.cos(rot);
        float sin = (float)Math.sin(rot);

        float temp;
        for (int i=0; i<points.length; i+=2){
            temp = points[i]*cos - points[i+1]*sin + x;
            points[i+1] = points[i]*sin + points[i+1]*cos + y;
            points[i] = temp;
        }

        return points;
    }

}