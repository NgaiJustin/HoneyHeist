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
package edu.cornell.gdiac.honeyHeistCode.controllers;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundBuffer;
import edu.cornell.gdiac.honeyHeistCode.GameplayController;
import edu.cornell.gdiac.honeyHeistCode.WorldController;
import edu.cornell.gdiac.honeyHeistCode.models.*;
import edu.cornell.gdiac.honeyHeistCode.obstacle.BoxObstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.Obstacle;

/**
 * Gameplay specific controller for the platformer game.
 * <p>
 * You will notice that asset loading is not done with static methods this time.
 * Instance asset loading makes it easier to process our game modes in a loop, which
 * is much more scalable. However, we still want the assets themselves to be static.
 * This is the purpose of our AssetState variable; it ensures that multiple instances
 * place nicely with the static assets.
 */
public class LevelController extends GameplayController implements ContactListener {
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
    /**
     * Physics constants for initialization
     */
    private JsonValue constants;
    /**
     * Reference to the level model
     */
    private LevelModel level;


    /**
     * Mark set to handle more sophisticated collision callbacks
     */
    protected ObjectSet<Fixture> sensorFixtures;

    /**
     * Creates and initialize a new instance of the platformer game
     * <p>
     * The game has default gravity and other settings
     */
    public LevelController() {
//        super(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_GRAVITY);
        setDebug(false);
        setComplete(false);
        setFailure(false);
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();
        level = new LevelModel();
        level.setOrigin(new Vector2(bounds.width / 2, bounds.height / 2));
    }

    /**
     * Gather the assets for this controller.
     * <p>
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     *
     * @param directory Reference to global asset manager.
     */
    @Override
    public void gatherAssets(AssetDirectory directory) {
        avatarTexture = new TextureRegion(directory.getEntry("platform:ant", Texture.class));
        chaserBeeTexture = new TextureRegion(directory.getEntry("platform:chaserBee", Texture.class));
        sleeperBeeTexture = new TextureRegion(directory.getEntry("platform:sleeperBee", Texture.class));

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
    public void populateLevel() {
        // Add level goal
        float dwidth = goalTile.getRegionWidth() / scale.x;
        float dheight = goalTile.getRegionHeight() / scale.y;

        JsonValue goal = constants.get("goal");
        JsonValue goalpos = goal.get("pos");

        BoxObstacle goalDoor = new BoxObstacle(goalpos.getFloat(0), goalpos.getFloat(1), dwidth, dheight);
        goalDoor.setBodyType(BodyDef.BodyType.StaticBody);
        goalDoor.setDensity(goal.getFloat("density", 0));
        goalDoor.setFriction(goal.getFloat("friction", 0));
        goalDoor.setRestitution(goal.getFloat("restitution", 0));
        goalDoor.setSensor(true);
        goalDoor.setDrawScale(scale);
        goalDoor.setTexture(goalTile);
        goalDoor.setName("goal");
        addObject(goalDoor);
        level.setGoalDoor(goalDoor);

        JsonValue defaults = constants.get("defaults");

        // Create platforms
        PlatformModel platforms = new PlatformModel(constants.get("platforms"));
        platforms.setDrawScale(scale);
        platforms.setTexture(earthTile);
        addObject(platforms);
        level.setPlatforms(platforms);

        // This world is heavier
        world.setGravity(new Vector2(0, defaults.getFloat("gravity", 0)));

        // Create player (ant)
        dwidth = avatarTexture.getRegionWidth() / scale.x;
        dheight = avatarTexture.getRegionHeight() / scale.y;
        PlayerModel avatar = new PlayerModel(constants.get("player"), dwidth, dheight);
        avatar.setDrawScale(scale);
        avatar.setTexture(avatarTexture);
        addObject(avatar);
        level.setPlayer(avatar);

        // Create one chaser bee
        dwidth = chaserBeeTexture.getRegionWidth() / scale.x;
        dheight = chaserBeeTexture.getRegionHeight() / scale.y;
        ChaserBeeModel chaserBee = new ChaserBeeModel(constants.get("chaserBee"), dwidth, dheight);
        chaserBee.setDrawScale(scale);
        chaserBee.setTexture(chaserBeeTexture);
        Array<AbstractBeeModel> bees = new Array<AbstractBeeModel>();
        bees.add(chaserBee);
        addObject(chaserBee);

        // Create one sleeper bee
        dwidth = sleeperBeeTexture.getRegionWidth() / scale.x;
        dheight = sleeperBeeTexture.getRegionHeight() / scale.y;
        SleeperBeeModel sleeperBee = new SleeperBeeModel(constants.get("sleeperBee"), dwidth, dheight);
        sleeperBee.setDrawScale(scale);
        sleeperBee.setTexture(sleeperBeeTexture);
        // Sleeper bee doesn't move
        sleeperBee.setBodyType(BodyDef.BodyType.StaticBody);
        bees.add(sleeperBee);
        addObject(sleeperBee);
        level.setBees(bees);

        volume = constants.getFloat("volume", 1.0f);
    }

    /**
     * Start rotation.
     *
     * @param isClockwise true if the rotation direction is clockwise, false if counterclockwise.
     * @param platformNotRotating true if the platform model isn't rotating when the rotate function is called.
     */
    public void rotate(boolean isClockwise, boolean platformNotRotating){
        PlatformModel platforms = level.getPlatforms();
        PlayerModel avatar = level.getPlayer();
        Array<AbstractBeeModel> bees = level.getBees();
        Vector2 origin = level.getOrigin();

        platforms.startRotation(isClockwise, origin);
        if (avatar.isGrounded()&&platformNotRotating){
            avatar.setBodyType(BodyDef.BodyType.StaticBody);
            System.out.println(origin);
            avatar.startRotation(isClockwise, origin);
        }
        for(AbstractBeeModel bee : bees){
            if(bee.isGrounded() && platformNotRotating) {
                bee.setBodyType(BodyDef.BodyType.StaticBody);
                bee.startRotation(isClockwise, origin);
            }
        }
    }

    /**
     * Start clockwise rotation.
     * Will only rotate once, and spamming will not queue more rotations.
     */
    public void rotateClockwise() {
        rotate(true, !level.getPlatforms().getIsRotating());
    }

    /**
     * Start counterclockwise rotation.
     * Will only rotate once, and spamming will not queue more rotations.
     */
    public void rotateCounterClockwise() {
        rotate(false, !level.getPlatforms().getIsRotating());
    }

    /**
     * Moves the ant based on the direction given
     *
     * @param direction -1 = left, 1 = right, 0 = still
     */
    public void moveAnt(float direction) {
        level.getPlayer().setMovement(direction * level.getPlayer().getForce());
    }

    /**
     * Moves the chaserBee based on the direction given by AIController
     *
     * @param direction -1 = left, 1 = right, 0 = still
     */
    public void moveChaserBee(float direction, ChaserBeeModel bee) {
        bee.setMovement(direction * bee.getForce());
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

        if (!isFailure() && level.getPlayer().getY() < -1) {
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

        level.getPlayer().applyForce();

        // Process AI action
        // 1. Loop over all chaser bee,
        // 2. For each bee, moveChaserBee(...);
        // TO BE IMPLEMENTED
        for(AbstractBeeModel bee : level.getBees()){
            bee.applyForce();
        }


        if (InputController.getInstance().didRotate()) {
            rotateClockwise();
        } else if (InputController.getInstance().didAntiRotate()) {
            rotateCounterClockwise();
        }


    }

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

        PlayerModel avatar = level.getPlayer();
        Array<AbstractBeeModel> bees = level.getBees();
        BoxObstacle goalDoor = level.getGoalDoor();

        try {
            Obstacle bd1 = (Obstacle) body1.getUserData();
            Obstacle bd2 = (Obstacle) body2.getUserData();

            // See if we have landed on the ground.
            if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                    (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
                avatar.setGrounded(true);
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground
            }
            // ITERATE OVER ALL CHASER BEES
            for(AbstractBeeModel bee : bees) {
                if ((bee.getSensorName().equals(fd2) && bee != bd1) ||
                        (bee.getSensorName().equals(fd1) && bee != bd2)) {
                    bee.setGrounded(true);
                    sensorFixtures.add(bee == bd1 ? fix2 : fix1); // Could have more than one ground
                }
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

        PlayerModel avatar = level.getPlayer();
        Array<AbstractBeeModel> bees = level.getBees();

        if ((avatar.getSensorName().equals(fd2) && avatar != bd1) ||
                (avatar.getSensorName().equals(fd1) && avatar != bd2)) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
            }
        }
        for(AbstractBeeModel bee : bees) {
            if ((bee.getSensorName().equals(fd2) && bee != bd1) ||
                    (bee.getSensorName().equals(fd1) && bee != bd2)) {
                sensorFixtures.remove(bee == bd1 ? fix2 : fix1);
                if (sensorFixtures.size == 0) {
                    bee.setGrounded(false);
                }
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
}