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

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundBuffer;
import edu.cornell.gdiac.honeyHeistCode.WorldController;
import edu.cornell.gdiac.honeyHeistCode.models.*;
import edu.cornell.gdiac.honeyHeistCode.obstacle.BoxObstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.Obstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;

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
     * Reference to the AI Controllers
     */
    private Array<AIController> aIControllers;

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
        super(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_GRAVITY);
        setDebug(false);
        setComplete(false);
        setFailure(false);
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();
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
        sensorFixtures.clear();

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

        JsonValue defaults = constants.get("defaults");
        /*
        PolygonObstacle obj;
        obj = new PolygonObstacle(platformPointsFromJson(constants.get("testPlatform")), 0, 0);
        obj.setBodyType(BodyDef.BodyType.StaticBody);
        obj.setDensity(defaults.getFloat( "density", 0.0f ));
        obj.setFriction(defaults.getFloat( "friction", 0.0f ));
        obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
        obj.setName("testPlatform");
        obj.setDrawScale(scale);
        obj.setTexture(earthTile);
        addObject(obj);

         */

        // Create the hexagon level

        /*
        JsonValue c = constants.get("testPlatform2");
        for (int a=1; a<=4; a++) {
            float r = 2*a;
            float l = 0.5f;
            //float h = c.getFloat("height");
            float h = 2 * r / (float) Math.sqrt(3) + l / (float) Math.sqrt(3);
            for (int i = 0; i < 6; i++) {
                float theta = (float) Math.PI / 3 * i + (float) Math.PI / 6;
                float x = r * (float) Math.cos(theta) + 16;
                float y = r * (float) Math.sin(theta) + 9;
                float[] points = platformPointsFromPoint(x, y, l, h, theta);
                for (int j = 0; j < points.length; j++) {
                    System.out.print(points[j] + ", ");
                }
                System.out.println("");
                PolygonObstacle obj;
                obj = new PolygonObstacle(points, 0, 0);
                obj.setBodyType(BodyDef.BodyType.StaticBody);
                obj.setDensity(defaults.getFloat("density", 0.0f));
                obj.setFriction(defaults.getFloat("friction", 0.0f));
                obj.setRestitution(defaults.getFloat("restitution", 0.0f));
                obj.setName("testPlatform");
                obj.setDrawScale(scale);
                obj.setTexture(earthTile);
                addObject(obj);
            }
        }
        */


        // Create platforms
        PlatformModel platforms = new PlatformModel(constants.get("mediumLevel"));
        platforms.setDrawScale(scale);
        platforms.setTexture(earthTile);
        addObject(platforms);

        // This world is heavier
        world.setGravity(new Vector2(0, defaults.getFloat("gravity", 0)));

        // Create player (ant)
        dwidth = avatarTexture.getRegionWidth() / scale.x;
        dheight = avatarTexture.getRegionHeight() / scale.y;
        PlayerModel avatar = new PlayerModel(constants.get("player"), dwidth, dheight);
        avatar.setDrawScale(scale);
        avatar.setTexture(avatarTexture);
        addObject(avatar);

        // Create chaser bees

        Array<AbstractBeeModel> bees = new Array<AbstractBeeModel>();
        level = new LevelModel(avatar,bees,goalDoor,platforms,new Vector2(bounds.width / 2, bounds.height / 2));


        aIControllers = new Array<AIController>();

        JsonValue.JsonIterator groundedBeeIterator = constants.get("groundedBees").iterator();

        dwidth = chaserBeeTexture.getRegionWidth() / scale.x;
        dheight = chaserBeeTexture.getRegionHeight() / scale.y;
        while (groundedBeeIterator.hasNext()){
            ChaserBeeModel chaserBee = new ChaserBeeModel(groundedBeeIterator.next(), dwidth, dheight);
            chaserBee.setDrawScale(scale);
            chaserBee.setTexture(chaserBeeTexture);
            bees.add(chaserBee);
            addObject(chaserBee);
            AIController chaserBeeAIController = new AIController(level, avatar.getPosition(), chaserBee, AIController.CharacterType.GROUNDED_CHARACTER);
            aIControllers.add(chaserBeeAIController);
        }
        //level = new LevelModel(avatar,bees,goalDoor,platforms,new Vector2(bounds.width / 2, bounds.height / 2));

        /*
        aIControllers = new Array<AIController>();
        //Adds AI Controller for chaserBee
        AIController chaserBeeAIController = new AIController(level, avatar.getPosition(), chaserBee, AIController.CharacterType.GROUNDED_CHARACTER);
        aIControllers.add(chaserBeeAIController);

         */

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
            avatar.startRotation(isClockwise, origin);
        }
        for(AbstractBeeModel bee : bees){
            if(bee.isGrounded() && platformNotRotating) {
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
     * TO BE LATER DEPRECATED
     *
     */
    private void moveChaserBeeFromStoredAIControllers() {
        for (AIController aIController: aIControllers) {
            aIController.updateAIController();
            AbstractBeeModel bee = aIController.getControlledCharacter();
            //System.out.println(aIController.getMovementHorizontalDirection());
            bee.setMovement(aIController.getMovementHorizontalDirection() * bee.getForce());
        }
    }

//    /**
//     * Returns whether to process the update loop
//     * <p>
//     * At the start of the update loop, we check if it is time
//     * to switch to a new game mode.  If not, the update proceeds
//     * normally.
//     *
//     * @param dt Number of seconds since last animation frame
//     * @return whether to process the update loop
//     */
//    public boolean preUpdate(float dt) {
//        if (!super.preUpdate(dt)) {
//            return false;
//        }
//
//        if (!isFailure() && level.getPlayer().getY() < -1) {
//            setFailure(true);
//            return false;
//        }
//
//        return true;
//    }

//    /**
//     * Returns whether to process the update loop
//     * <p>
//     * At the start of the update loop, we check if it is time
//     * to switch to a new game mode.  If not, the update proceeds
//     * normally.
//     *
//     * @param dt Number of seconds since last animation frame
//     * @return whether to process the update loop
//     */
//    public boolean preUpdate(float dt, boolean isFailure) {
//        if (!super.preUpdate(dt)) {
//            return false;
//        }
//
//        if (!isFailure && level.getPlayer().getY() < -1) {
//            setFailure(true);
//            return false;
//        }
//
//        return true;
//    }
    public boolean preUpdate(boolean temp, boolean isFailure) {
        if (!temp) {
            return false;
        }

        if (!isFailure && level.getPlayer().getY() < -1) {
            setFailure(true);
            return false;
        }

        return true;
    }


//    /**
//     * The core gameplay loop of this world.
//     * <p>
//     * This method contains the specific update code for this mini-game. It does
//     * not handle collisions, as those are managed by the parent class WorldController.
//     * This method is called after input is read, but before collisions are resolved.
//     * The very last thing that it should do is apply forces to the appropriate objects.
//     *
//     * @param dt Number of seconds since last animation frame
//     */
//    public void update(float dt) {
//        // Process actions in object model
//        moveAnt(InputController.getInstance().getHorizontal());
//
//        level.getPlayer().applyForce();
//
//        // Process AI action
//        // 1. Loop over all chaser bee,
//        // 2. For each bee, moveChaserBee(...);
//        // TO BE IMPLEMENTED
//        moveChaserBeeFromStoredAIControllers();
//        for(AbstractBeeModel bee : level.getBees()){
//            bee.applyForce();
//            if(!bee.isGrounded()){
//                bee.getSensorFixtures().clear();
//            }
//        }
//
//
//        if (InputController.getInstance().didRotate()) {
//            rotateClockwise();
//        } else if (InputController.getInstance().didAntiRotate()) {
//            rotateCounterClockwise();
//        }
//
//        if(!level.getPlayer().isGrounded()){
//            sensorFixtures.clear();
//        }
//    }
    public void update(float horizontal, boolean didRotate, boolean didAntiRotate) {
        // Process actions in object model
        moveAnt(horizontal);
        level.getPlayer().applyForce();

        // Process AI action
        // 1. Loop over all chaser bee,
        // 2. For each bee, moveChaserBee(...);
        // TO BE IMPLEMENTED
        moveChaserBeeFromStoredAIControllers();
        for(AbstractBeeModel bee : level.getBees()){
            bee.applyForce();
            if(!bee.isGrounded()){
                bee.getSensorFixtures().clear();
            }
        }

        if (didRotate) {
            rotateClockwise();
        } else if (didAntiRotate) {
            rotateCounterClockwise();
        }

        if(!level.getPlayer().isGrounded()){
            sensorFixtures.clear();
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
            if (((avatar.getSensorName().equals(fd2) && avatar != bd1) && (bd1.getClass() == PolygonObstacle.class) &&
                        !sensorFixtures.contains(fix1)) ||
                ((avatar.getSensorName().equals(fd1)&& avatar != bd2) && (bd2.getClass() == PolygonObstacle.class) &&
                        !sensorFixtures.contains(fix2))) {
                avatar.setGrounded(true);
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground
            }
            // ITERATE OVER ALL CHASER BEES
            for(AbstractBeeModel bee : bees) {
                if (((bee.getSensorName().equals(fd2) && bee != bd1)&&(bd1.getClass() == PolygonObstacle.class) &&
                        !bee.getSensorFixtures().contains(fix1)) ||
                    ((bee.getSensorName().equals(fd1) && bee != bd2)&&(bd2.getClass() == PolygonObstacle.class) &&
                        !bee.getSensorFixtures().contains(fix2))) {
                    bee.setGrounded(true);
                    bee.getSensorFixtures().add(bee == bd1 ? fix2 : fix1); // Could have more than one ground
                }
            }
            // Check for win condition
            if (!isFailure() && !isComplete() &&
                    (bd1 == avatar && bd2.getClass().getSuperclass() == AbstractBeeModel.class) ||
                    (bd1.getClass().getSuperclass() == AbstractBeeModel.class && bd2 == avatar)) {
                setFailure(true);
            }
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

        if (((avatar.getSensorName().equals(fd2) && avatar != bd1)&&(bd1.getClass() == PolygonObstacle.class))  ||
                ((avatar.getSensorName().equals(fd1) && avatar != bd2)&&(bd2.getClass() == PolygonObstacle.class))) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
            }
        }
        for(AbstractBeeModel bee : bees) {
            if (((bee.getSensorName().equals(fd2) && bee != bd1)&&(bd1.getClass() == PolygonObstacle.class)) ||
                    ((bee.getSensorName().equals(fd1) && bee != bd2)&&(bd2.getClass() == PolygonObstacle.class))) {
                bee.getSensorFixtures().remove(bee == bd1 ? fix2 : fix1);
                if (bee.getSensorFixtures().size == 0) {
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

    public float[] platformPointsFromPoint(float x, float y, float width, float height, float rotation){
        float w = width/2;
        float h = height/2;
        float rot = rotation; /* rotation * 2 * (float)Math.PI/360; */
        float[] points = new float[]{-w, h, -w, -h, w, -h, w, h};
        float cos = (float)Math.cos(rot);
        float sin = (float)Math.sin(rot);

        float temp;
        for (int i=0; i<points.length; i+=2){
            temp = points[i]*cos - points[i+1]*sin + x;
            points[i+1] = points[i]*sin+points[i+1]*cos + y;
            points[i] = temp;
        }
        return points;
    }
}