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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundBuffer;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers.AIController;
import edu.cornell.gdiac.honeyHeistCode.models.*;
import edu.cornell.gdiac.honeyHeistCode.obstacle.BoxObstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.Obstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.PooledList;
import edu.cornell.gdiac.util.ScreenListener;

import java.util.Iterator;

/**
 * Gameplay specific controller for the platformer game.
 * <p>
 * You will notice that asset loading is not done with static methods this time.
 * Instance asset loading makes it easier to process our game modes in a loop, which
 * is much more scalable. However, we still want the assets themselves to be static.
 * This is the purpose of our AssetState variable; it ensures that multiple instances
 * place nicely with the static assets.
 */
public class LevelController implements ContactListener {
    /** The texture for walls and platforms */
    protected TextureRegion earthTile;
    /** The texture for honeyPatches */
    protected TextureRegion honeyPatchTexture;
    /** The texture for spiked platforms */
    protected TextureRegion poisonTile;
    /** The texture for the exit condition */
    protected TextureRegion goalTile;
    /** The texture for the background */
    protected TextureRegion background;
    /** The texture for the tilesBackground */
    protected TextureRegion tilesBackground;
    /** The texture for AI Nodes, used for debugging */
    protected TextureRegion whiteSquare;
    /** The font for giving messages to the player */
    protected BitmapFont displayFont;


    /** \ code for quitting the game */
    public static final int EXIT_QUIT = 0;
    /** Exit code for advancing to next level */
    public static final int EXIT_NEXT = 1;
    /** Exit code for jumping back to previous level */
    public static final int EXIT_PREV = 2;
    /** How many frames after winning/losing do we continue? */
    public static final int EXIT_COUNT = 120;

    /** The amount of time for a physics engine step. */
    public static final float WORLD_STEP = 1/60.0f;
    /** Number of velocity iterations for the constrain solvers */
    public static final int WORLD_VELOC = 6;
    /** Number of position iterations for the constrain solvers */
    public static final int WORLD_POSIT = 2;

    /** Width of the game world in Box2d units */
    protected static final float DEFAULT_WIDTH  = 32.0f;
    /** Height of the game world in Box2d units */
    protected static final float DEFAULT_HEIGHT = 18.0f;
    /** The default value of gravity (going down) */
    protected static final float DEFAULT_GRAVITY = -4.9f;

    /** Reference to the game canvas */
    protected GameCanvas canvas;
    /** All the objects in the world. */
    protected PooledList<Obstacle> objects  = new PooledList<Obstacle>();
    /** Queue for adding objects */
    protected PooledList<Obstacle> addQueue = new PooledList<Obstacle>();
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

    /** The Box2D world */
    protected World world;
    /** The boundary of the world */
    protected Rectangle bounds;
    /** The world scale */
    protected Vector2 scale;

    /** Whether or not this is an active controller */
    private boolean active;
    /** Whether we have completed this level */
    private boolean complete;
    /** Whether we have failed at this world (and need a reset) */
    private boolean failed;
    /** Whether or not debug mode is active */
    private boolean debug;
    /** Whether or not debug mode is active for AI */
    private boolean aIDebug;
    /** Countdown active for winning or losing */
    private int countdown;

    /** Whether additional rotations were queued or not */
    private boolean didQueueClockwise = false;
    private boolean didQueueCounterClockwise = false;
    private boolean isRotating = false;
    private float angleLeft = 0f;

    /**
     * Returns true if debug mode is active.
     *
     * If true, all objects will display their physics bodies.
     *
     * @return true if debug mode is active.
     */
    public boolean isDebug( ) {
        return debug;
    }

    /**
     * Sets whether debug mode is active.
     *
     * If true, all objects will display their physics bodies.
     *
     * @param value whether debug mode is active.
     */
    public void setDebug(boolean value) {
        debug = value;
    }

    /**
     * Returns true if debug mode is active.
     *
     * If true, all objects will display their physics bodies.
     *
     * @return true if debug mode is active.
     */
    public boolean isAIDebug( ) {
        return aIDebug;
    }

    /**
     * Sets whether debug mode is active.
     *
     * If true, all objects will display their physics bodies.
     *
     * @param value whether debug mode is active.
     */
    public void setAIDebug(boolean value) {
        aIDebug = value;
    }
    /**
     * Returns true if the level is completed.
     *
     * If true, the level will advance after a countdown
     *
     * @return true if the level is completed.
     */
    public boolean isComplete( ) {
        return complete;
    }

    /**
     * Sets whether the level is completed.
     *
     * If true, the level will advance after a countdown
     *
     * @param value whether the level is completed.
     */
    public void setComplete(boolean value) {
        if (value) {
            countdown = EXIT_COUNT;
        }
        complete = value;
    }

    /**
     * Returns countdown time.
     *
     * @return int countdown
     */
    public int getCountdown( ) {
        return countdown;
    }

    /**
     * Set the countdown time.
     *
     * @param number the new countdown number
     */
    public void setCountdown(int number ) {
        countdown = number;
    }

    /**
     * Returns the scale.
     *
     * @return Vector2 scale.
     */
    public Vector2 getScale() {
        return scale;
    }

    /**
     * Set the scale.
     *
     * @param x scale.x
     * @param y scale.y
     */
    public void setCountdown(float x, float y) {
        scale.x = x;
        scale.y = y;
    }

    /**
     * Decreases countdown time by one.
     *
     */
    public void decreaseCountdown() {
        countdown--;
    }

    /**
     * Returns true if the level is failed.
     *
     * If true, the level will reset after a countdown
     *
     * @return true if the level is failed.
     */
    public boolean isFailure( ) {
        return failed;
    }

    /**
     * Sets whether the level is failed.
     *
     * If true, the level will reset after a countdown
     *
     * @param value whether the level is failed.
     */
    public void setFailure(boolean value) {
        if (value) {
            countdown = EXIT_COUNT;
        }
        failed = value;
    }

    /**
     * Returns true if this is the active screen
     *
     * @return true if this is the active screen
     */
    public boolean isActive( ) {
        return active;
    }

    /**
     * Returns the canvas associated with this controller
     *
     * The canvas is shared across all controllers
     *
     * @return the canvas associated with this controller
     */
    public GameCanvas getCanvas() {
        return canvas;
    }

    /**
     * Sets the canvas associated with this controller
     *
     * The canvas is shared across all controllers.  Setting this value will compute
     * the drawing scale from the canvas size.
     *
     * @param canvas the canvas associated with this controller
     */
    public void setCanvas(GameCanvas canvas) {
        this.canvas = canvas;
        this.scale.x = canvas.getWidth()/bounds.getWidth();
        this.scale.y = canvas.getHeight()/bounds.getHeight();
    }

    /** Player texture and filmstrip */
    private TextureRegion avatarTexture;
    private FilmStrip walkingPlayer;
    private FilmStrip flailingPlayer;
    private FilmStrip dyingPlayer;

    /** Larvae texture and filmstrip */
    private TextureRegion larvaeTexture;
    private FilmStrip walkingLarvae;
    private FilmStrip flailingLarvae;
    private FilmStrip dyingLarvae;
    private FilmStrip chasingLarvae;

    /** Bee texture and filmstrip */
    private TextureRegion flyingBeeTexture;
    private FilmStrip flyingBeeStrip;
    private FilmStrip flailingBeestrip;
    private FilmStrip dyingBeestrip;
    private FilmStrip chasingBeeStrip;

    /** Platform texture */
    private TextureRegion ULeft;
    private TextureRegion UMid;
    private TextureRegion URight;
    private TextureRegion MLeft;
    private TextureRegion MMid;
    private TextureRegion MRight;
    private TextureRegion BLeft;
    private TextureRegion BMid;
    private TextureRegion BRight;

    /** Spike textures */
    private TextureRegion SpikeULeft;
    private TextureRegion SpikeUMid;
    private TextureRegion SpikeURight;
    private TextureRegion SpikeMLeft;
    private TextureRegion SpikeMMid;
    private TextureRegion SpikeMRight;
    private TextureRegion SpikeBLeft;
    private TextureRegion SpikeBMid;
    private TextureRegion SpikeBRight;

    /** NinePatches TO BE REPLACED WITH TENPATCH WHEN COMPLETED */
    private NinePatch platNinePatch;
    private NinePatch spikeNinePatch;

    /** The jump sound.  We only want to play once. */
    private SoundBuffer bgm;
    private long bgmId = 1;

    /** The jump sound.  We only want to play once */
    private SoundBuffer deathSound;
    private long deathId = -1;

    /** The weapon fire sound.  We only want to play once. */
    private SoundBuffer trackingSound;
    private long trackingId = -1;

    /** The weapon pop sound.  We only want to play once. */
    private SoundBuffer winSound;
    private long winId = -1;

    /** The default sound volume */
    private float volume;

    /** Constant data across levels */
    private JsonValue constants;

    /** Data for the level */
    private JsonValue levelData;

    /** Reference to the level model */
    private LevelModel level;


    /** Reference to the AI Controller */
    private AIController aIController;

    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> honeyFixtures;

//    OrthographicCamera camera;

    /**
     * Creates and initialize a new instance of the platformer game
     * <p>
     * The game has default gravity and other settings
     */
    public LevelController() {
        // using default value
        this(new Rectangle(0,0,DEFAULT_WIDTH,DEFAULT_HEIGHT), new Vector2(0,DEFAULT_GRAVITY));
    }

    /**
     * Creates and initialize a new instance of the platformer game
     * <p>
     * The game has default gravity and other settings
     */
    public LevelController(Rectangle bounds, Vector2 gravity) {
        // initialize the world
        world = new World(gravity,false);
        this.bounds = new Rectangle(bounds);
        this.scale = new Vector2(1,1);
        complete = false;
        failed = false;
        debug  = false;
        active = false;
        countdown = -1;
        // initialize the level
        setDebug(false);
        setComplete(false);
        setFailure(false);
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();
        honeyFixtures = new ObjectSet<Fixture>();
//        this.camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /**
     * Gather the assets for this controller.
     * <p>
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     *
     * @param directory Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory, String dataFilePath) {
        avatarTexture    = new TextureRegion(directory.getEntry("platform:ant", Texture.class));
        larvaeTexture = new TextureRegion(directory.getEntry("platform:larvae", Texture.class));
        flyingBeeTexture = new TextureRegion(directory.getEntry("platform:flyingBee", Texture.class));

        walkingPlayer   = directory.getEntry( "platform:playerWalk.pacing", FilmStrip.class );
        flailingPlayer  = directory.getEntry("platform:playerFlail.pacing", FilmStrip.class);
        dyingPlayer     = directory.getEntry( "platform:playerDeath.pacing", FilmStrip.class );

        walkingLarvae   = directory.getEntry( "platform:larvaeWalk.pacing", FilmStrip.class );
        flailingLarvae  = directory.getEntry("platform:larvaeFlail.pacing", FilmStrip.class);
        dyingLarvae     = directory.getEntry("platform:larvaeDeath.pacing", FilmStrip.class);
        chasingLarvae   = directory.getEntry( "platform:larvaeChase.pacing", FilmStrip.class );

        flyingBeeStrip   = directory.getEntry( "platform:beeFly.pacing", FilmStrip.class );
        flailingBeestrip = directory.getEntry("platform:beeFlail.pacing", FilmStrip.class);
        dyingBeestrip    = directory.getEntry("platform:beeDeath.pacing", FilmStrip.class);
        chasingBeeStrip  = directory.getEntry( "platform:beeChase.pacing", FilmStrip.class );

        SpikeULeft  = new TextureRegion(directory.getEntry("platform:spikeULeft", Texture.class));
        SpikeUMid   = new TextureRegion(directory.getEntry("platform:spikeUMid", Texture.class));
        SpikeURight = new TextureRegion(directory.getEntry("platform:spikeURight", Texture.class));
        SpikeMLeft  = new TextureRegion(directory.getEntry("platform:spikeMLeft", Texture.class));
        SpikeMMid   = new TextureRegion(directory.getEntry("platform:spikeMMid", Texture.class));
        SpikeMRight = new TextureRegion(directory.getEntry("platform:spikeMRight", Texture.class));
        SpikeBLeft  = new TextureRegion(directory.getEntry("platform:spikeBLeft", Texture.class));
        SpikeBMid   = new TextureRegion(directory.getEntry("platform:spikeBMid", Texture.class));
        SpikeBRight = new TextureRegion(directory.getEntry("platform:spikeBRight", Texture.class));

        ULeft  = new TextureRegion(directory.getEntry("platform:ULeft", Texture.class));
        UMid   = new TextureRegion(directory.getEntry("platform:UMid", Texture.class));
        URight = new TextureRegion(directory.getEntry("platform:URight", Texture.class));
        MLeft  = new TextureRegion(directory.getEntry("platform:MLeft", Texture.class));
        MMid   = new TextureRegion(directory.getEntry("platform:MMid", Texture.class));
        MRight = new TextureRegion(directory.getEntry("platform:MRight", Texture.class));
        BLeft  = new TextureRegion(directory.getEntry("platform:BLeft", Texture.class));
        BMid   = new TextureRegion(directory.getEntry("platform:BMid", Texture.class));
        BRight = new TextureRegion(directory.getEntry("platform:BRight", Texture.class));

        platNinePatch  = new NinePatch(directory.getEntry("platform:platNinePatch", Texture.class),  16, 16 ,16 ,16 );
        spikeNinePatch = new NinePatch(directory.getEntry("platform:spikeNinePatch", Texture.class),  16, 16 ,16 ,16 );

        deathSound = directory.getEntry("audio:soundeffect_death_pixel", SoundBuffer.class);
        trackingSound = directory.getEntry("audio:soundeffect_tracking", SoundBuffer.class);
        winSound = directory.getEntry("audio:soundeffect_win", SoundBuffer.class);
        bgm = directory.getEntry("audio:bgm", SoundBuffer.class);

        constants = directory.getEntry("platform:constants2", JsonValue.class);
        System.out.println("DatafilePath = " + dataFilePath);
        levelData = directory.getEntry(dataFilePath, JsonValue.class);
//        levelData = directory.getEntry("platform:prototypeLevel", JsonValue.class);
//        super.gatherAssets(directory);

        // Allocate the world tiles
        earthTile         = new TextureRegion(directory.getEntry( "shared:earth", Texture.class ));
        honeyPatchTexture = new TextureRegion(directory.getEntry( "shared:honeyPatch", Texture.class ));
        poisonTile        = new TextureRegion(directory.getEntry( "shared:poisonWall", Texture.class));
        goalTile          = new TextureRegion(directory.getEntry( "shared:goal", Texture.class ));
        background        = new TextureRegion(directory.getEntry( "shared:background",  Texture.class ));
        tilesBackground   = new TextureRegion(directory.getEntry("shared:tilesBackground", Texture.class));
        displayFont       = directory.getEntry( "shared:retro" ,BitmapFont.class);

        // This is just for Debugging.
        whiteSquare = new TextureRegion(directory.getEntry( "shared:whiteSquare", Texture.class ));
    }

    public void gatherLevelData(AssetDirectory directory, String dataFilePath){
        levelData = directory.getEntry(dataFilePath, JsonValue.class);
    }

    /**
     * Dispose of all (non-static) resources allocated to this mode.
     */
    public void dispose() {
        for(Obstacle obj : objects) {
            obj.deactivatePhysics(world);
        }
        objects.clear();
        addQueue.clear();
        world.dispose();
        objects = null;
        addQueue = null;
        bounds = null;
        scale  = null;
        world  = null;
        canvas = null;
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
        honeyFixtures.clear();

        world = new World(gravity, false);
        world.setContactListener(this);
        setComplete(false);
        setFailure(false);
        populateLevel();
        bgmId = loopSound(bgm, bgmId);
    }

    /**
     *
     * Adds a physics object in to the insertion queue.
     *
     * Objects on the queue are added just before collision processing.  We do this to
     * control object creation.
     *
     * param obj The object to add
     */
    public void addQueuedObject(Obstacle obj) {
        assert inBounds(obj) : "Object is not in bounds";
        addQueue.add(obj);
    }

    /**
     * Immediately adds the object to the physics world
     *
     * param obj The object to add
     */
    protected void addObject(Obstacle obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
    }

    /**
     * Returns true if the object is in bounds.
     *
     * This assertion is useful for debugging the physics.
     *
     * @param obj The object to check.
     *
     * @return true if the object is in bounds.
     */
    public boolean inBounds(Obstacle obj) {
        boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x+bounds.width);
        boolean vert  = (bounds.y <= obj.getY() && obj.getY() <= bounds.y+bounds.height);
        return horiz && vert;
    }

    /**
     * Lays out the game geography.
     */
    public void populateLevel() {
        JsonValue defaults = constants.get("defaults");
        //Create background
        PolygonObstacle levelBackground;
        // Find center of the game
        Vector2 worldCenter = bounds.getCenter(new Vector2());
        if (!levelData.get("background").isNull()) {
            levelBackground = new PolygonObstacle(levelData.get("background").asFloatArray(), 0, 0);
            levelBackground.setBodyType(BodyDef.BodyType.StaticBody);
            levelBackground.setDensity(defaults.getFloat("density", 0.0f));
            levelBackground.setFriction(defaults.getFloat("friction", 0.0f));
            levelBackground.setRestitution(defaults.getFloat("restitution", 0.0f));
            levelBackground.setName("background");
            levelBackground.setDrawScale(scale);
            levelBackground.setTexture(tilesBackground);
            levelBackground.setSensor(true);
            addObject(levelBackground);
        } else {
            levelBackground = null;
        }

        // Add level goal
        float dwidth = goalTile.getRegionWidth() / scale.x;
        float dheight = goalTile.getRegionHeight() / scale.y;

        JsonValue goal = constants.get("goal");
        float[] goalPos = levelData.get("goalPos").asFloatArray();

        BoxObstacle goalDoor = new BoxObstacle(goalPos[0], goalPos[1], dwidth, dheight);
        goalDoor.setBodyType(BodyDef.BodyType.StaticBody);
        goalDoor.setDensity(goal.getFloat("density", 0));
        goalDoor.setFriction(goal.getFloat("friction", 0));
        goalDoor.setRestitution(goal.getFloat("restitution", 0));
        goalDoor.setSensor(true);
        goalDoor.setDrawScale(scale);
        goalDoor.setTexture(goalTile);
        goalDoor.setName("goal");
        addObject(goalDoor);


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
        PlatformModel platforms = new PlatformModel(levelData.get("platformPos"), worldCenter);
        platforms.setDrawScale(scale);
        platforms.setTexture(earthTile);
        platforms.setNinePatch(platNinePatch);
        platforms.setTenPatch(
                ULeft, UMid, URight,
                MLeft, MMid, MRight,
                BLeft, BMid, BRight
        );
        addObject(platforms);

        // Create spiked platforms
        SpikedPlatformModel spikedPlatforms = new SpikedPlatformModel(levelData.get("spikedPlatformPos"), worldCenter);
        spikedPlatforms.setDrawScale(scale);
        spikedPlatforms.setNinePatch(spikeNinePatch);
        spikedPlatforms.setTenPatch(
                SpikeULeft, SpikeUMid, SpikeURight,
                SpikeMLeft, SpikeMMid, SpikeMRight,
                SpikeBLeft, SpikeBMid, SpikeBRight
        );
        // spikedPlatforms.setAnimationStrip(PlatformModel.PlatformAnimations.SHUFFLE, spikeCenter);
        //addObject(spikedPlatforms);

        // Create honeypatches
        HoneypatchModel honeyPatches = new HoneypatchModel(levelData.get("honeypatchPos"),0.4f, worldCenter);
        honeyPatches.setDrawScale(scale);
        honeyPatches.setTexture(honeyPatchTexture);
        //dont add yet so that it can overlap
        //addObject(honeyPatches);

        // This world is heavier
        world.setGravity(new Vector2(0, defaults.getFloat("gravity", 0)));

        // Create player (ant)
        dwidth = avatarTexture.getRegionWidth() / scale.x;
        dheight = avatarTexture.getRegionHeight() / scale.y;
        float[] playerPos = levelData.get("playerPos").asFloatArray();
        PlayerModel avatar = new PlayerModel(constants.get("player"), playerPos[0], playerPos[1], dwidth, dheight);
        avatar.setDrawScale(scale);
        avatar.setTexture(avatarTexture);
        avatar.setAnimationStrip(PlayerModel.AntAnimations.WALK, walkingPlayer);
        avatar.setAnimationStrip(PlayerModel.AntAnimations.FLAIL, flailingPlayer);
        avatar.setAnimationStrip(PlayerModel.AntAnimations.DEATH, dyingPlayer);
        avatar.setIsDead(false);
        avatar.setIsTrulyDead(false);
        addObject(avatar);

        // Create chaser bees
        Array<AbstractBeeModel> bees = new Array<AbstractBeeModel>();
        level = new LevelModel(avatar,bees,goalDoor,platforms, spikedPlatforms, honeyPatches, levelBackground, new Rectangle(bounds));
      
        aIController = new AIController(level);

        dwidth = larvaeTexture.getRegionWidth() / scale.x;
        dheight = larvaeTexture.getRegionHeight() / scale.y;
        //JsonValue.JsonIterator groundedBeeIterator = constants.get("groundedBees").iterator();
        JsonValue groundedBeePositions = levelData.get("groundedBeePos");
        for (int i=0; i<groundedBeePositions.size; i++){
            float[] pos = groundedBeePositions.get(i).asFloatArray();
            LarvaeModel larvae = new LarvaeModel(constants.get("GroundedBee"), pos[0], pos[1], dwidth, dheight);
            larvae.setDrawScale(scale);
            larvae.setTexture(larvaeTexture);
            larvae.setAnimationStrip(LarvaeModel.LarvaeAnimations.WALK, walkingLarvae);
            larvae.setAnimationStrip(LarvaeModel.LarvaeAnimations.FLAIL, flailingLarvae);
            larvae.setAnimationStrip(LarvaeModel.LarvaeAnimations.DEATH, dyingLarvae);
            larvae.setAnimationStrip(LarvaeModel.LarvaeAnimations.CHASE, chasingLarvae);
            larvae.setIsDead(false);
            larvae.setIsTrulyDead(false);
            bees.add(larvae);
            addObject(larvae);
            aIController.createAIForSingleCharacter(larvae, constants.get("GroundedBee").get("ai_controller_options"));
        }

        JsonValue flyingBeePositions = levelData.get("flyingBeePos");
        for (int i=0; i<flyingBeePositions.size; i++){
            float[] pos = flyingBeePositions.get(i).asFloatArray();
            FlyingBeeModel flyingBee = new FlyingBeeModel(constants.get("FlyingBee"), pos[0], pos[1], dwidth, dheight);
            flyingBee.setDrawScale(scale);
            flyingBee.setTexture(flyingBeeTexture);
            flyingBee.setAnimationStrip(FlyingBeeModel.BeeAnimations.FLY, flyingBeeStrip);
            flyingBee.setAnimationStrip(FlyingBeeModel.BeeAnimations.FLAIL, flailingBeestrip);
            flyingBee.setAnimationStrip(FlyingBeeModel.BeeAnimations.DEATH, dyingBeestrip);
            flyingBee.setAnimationStrip(FlyingBeeModel.BeeAnimations.CHASE, chasingBeeStrip);
            flyingBee.setIsDead(false);
            flyingBee.setIsTrulyDead(false);
            bees.add(flyingBee);
            addObject(flyingBee);
            aIController.createAIForSingleCharacter(flyingBee, constants.get("FlyingBee").get("ai_controller_options"));
        }

        //establish draw order:

        addObject(honeyPatches);
        addObject(spikedPlatforms);


        /*
        while (groundedBeeIterator.hasNext()){
            ChaserBeeModel chaserBee = new ChaserBeeModel(groundedBeeIterator.next(), dwidth, dheight);
            chaserBee.setDrawScale(scale);
            chaserBee.setTexture(chaserBeeTexture);
            bees.add(chaserBee);
            addObject(chaserBee);
            AIController chaserBeeAIController = new AIController(level, avatar.getPosition(), chaserBee, AIController.CharacterType.GROUNDED_CHARACTER);
            aIControllers.add(chaserBeeAIController);
        }
         */
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
        SpikedPlatformModel spikedPlatforms = level.getSpikedPlatforms();
        PlayerModel avatar = level.getPlayer();
        Array<AbstractBeeModel> bees = level.getBees();
        Vector2 origin = level.getOrigin();

        platforms.startRotation(isClockwise, origin);
        spikedPlatforms.startRotation(isClockwise, origin);
        level.getHoneyPatches().startRotation(isClockwise,origin);
        level.getLevelBackground().startRotation(isClockwise,origin);

        level.getGoalDoor().startRotation(isClockwise,origin);
        if ((avatar.isGrounded()||avatar.isInHoney())&&platformNotRotating){
            avatar.startRotation(isClockwise, origin);
        }
        for(AbstractBeeModel bee : bees){
            if((bee.isGrounded()||bee.isInHoney()) && platformNotRotating) {
                bee.startRotation(isClockwise, origin);
            }
        }
    }

    /**
     * Start clockwise rotation.
     * Will only rotate once, and spamming will not queue more rotations.
     */
    public void rotateClockwise() {
        rotate(true, !level.getPlatforms().isRotating());
    }

    /**
     * Start counterclockwise rotation.
     * Will only rotate once, and spamming will not queue more rotations.
     */
    public void rotateCounterClockwise() {
        rotate(false, !level.getPlatforms().isRotating());
    }

    /**
     * Moves the ant based on the direction given
     *
     * @param direction -1 = left, 1 = right, 0 = still
     */
    public void moveAnt(float direction) {
        PlayerModel player = level.getPlayer();
        player.setMovement(direction * level.getPlayer().getForce());
        player.animateAnt(PlayerModel.AntAnimations.WALK, direction != 0);
    }


    /**
     * Returns whether to process the update loop
     * <p>
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode.  If not, the update proceeds
     * normally.
     *
//     * @param dt Number of seconds since last animation frame
     * @return whether to process the update loop
     */
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
    public boolean preUpdate(boolean temp) {
        if (!temp) {
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
     * dt Number of seconds since last animation frame
     */
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
//
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
        PlayerModel avatar  = level.getPlayer();
        PlatformModel platforms = level.getPlatforms();

        // Only move if player is not dead
        if (!avatar.getIsDead()) {
            moveAnt(horizontal);
            avatar.applyForce();
        }

        platforms.animatePlatform(PlatformModel.PlatformAnimations.SHUFFLE, true);

        if((platforms.isRotating() && !avatar.isRotating()) && (avatar.isGrounded() || avatar.isInHoney())){
                //&&((avatar.isGrounded() && !avatar.isInHoney())||(avatar.isInHoney() && avatar.getHoneyTime()==0))){
            avatar.startRotation(platforms.getRemainingAngle(), platforms.isClockwise(), level.getOrigin());
            avatar.setCurrentSpeed(platforms.getCurrentSpeed());
        }

        if(!avatar.isGrounded()){
            sensorFixtures.clear();
        }
        if(!avatar.isInHoney()){
            honeyFixtures.clear();
        }
        if(avatar.getIsDead()){
            if (avatar.getIsTrulyDead()) {
                avatar.markRemoved(true);
            } else {
                avatar.animateAnt(PlayerModel.AntAnimations.DEATH, true);
                avatar.haltMovement();
            }
        }

        // Process AI action
        // 1. Loop over all chaser bee,
        // 2. For each bee, moveChaserBee(...);
        // TO BE IMPLEMENTED

        aIController.moveAIControlledCharacters();
//        aIController.updateAccessibility();

        for(AbstractBeeModel bee : level.getBees()){
            // Only move enemy if not dead
            if (!bee.getIsDead()) {
                bee.applyForce();

                if ((platforms.isRotating() && !bee.isRotating()) && (bee.isGrounded() || bee.isInHoney())) {
                    //&&((bee.isGrounded() && !bee.isInHoney())||(bee.isInHoney() && bee.getHoneyTime()==0))){
                    bee.startRotation(platforms.getRemainingAngle(), platforms.isClockwise(), level.getOrigin());
                    bee.setCurrentSpeed(platforms.getCurrentSpeed());
                }
                if(!bee.isGrounded()){
                    bee.getSensorFixtures().clear();
                }
                if (!bee.isInHoney()) {
                    bee.getHoneyFixtures().clear();
                }
                if (bee.getIsChasing()){
                    if (!bee.getPlayedChaseSound()){
                        trackingId = playSound(trackingSound, trackingId);
                        bee.setPlayedChaseSound(true);
                    }
                } else if (bee.getPlayedChaseSound()){
                    bee.setPlayedChaseSound(false);
                }
            }
            if (bee.getIsDead()) {
              if (bee.getIsTrulyDead()){
                  bee.markRemoved(true);
              }
              else {
                  bee.animateDeath();
              }
            }
        }

        isRotating = platforms.isRotating();
        angleLeft = platforms.getRemainingAngle();
        if (didRotate) {
            rotateClockwise();
            if (angleLeft <= 2*Math.PI/24 && isRotating && !didQueueCounterClockwise){
            //if (isRotating && !didQueueCounterClockwise){
                System.out.print("Queue Clockwise\n");
                didQueueClockwise = true;
            }
        } else if (didAntiRotate) {
            rotateCounterClockwise();
            if (angleLeft <= 2*Math.PI/24 && isRotating && !didQueueClockwise){
            //if (isRotating && !didQueueClockwise){
                System.out.print("Queue Counter Clockwise\n");
                didQueueCounterClockwise = true;
            }
        }
        if (!isRotating && didQueueClockwise){
            rotateClockwise();
            didQueueClockwise = false;
        } else if (!isRotating && didQueueCounterClockwise){
            rotateCounterClockwise();
            didQueueCounterClockwise = false;
        }
    }
    /**
     * Processes physics
     *
     * Once the update phase is over, but before we draw, we are ready to handle
     * physics.  The primary method is the step() method in world.  This implementation
     * works for all applications and should not need to be overwritten.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void postUpdate(float dt) {
        // Add any objects created by actions
        while (!addQueue.isEmpty()) {
            addObject(addQueue.poll());
        }

        // Turn the physics engine crank.
        world.step(WORLD_STEP, WORLD_VELOC, WORLD_POSIT);

        // Garbage collect the deleted objects.
        // Note how we use the linked list nodes to delete O(1) in place.
        // This is O(n) without copying.
        Iterator<PooledList<Obstacle>.Entry> iterator = objects.entryIterator();
        while (iterator.hasNext()) {
            PooledList<Obstacle>.Entry entry = iterator.next();
            Obstacle obj = entry.getValue();
            if (obj.isRemoved()) {
                obj.deactivatePhysics(world);
                entry.remove();
            } else {
                // Note that update is called last!
                obj.update(dt);
            }
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

            // See if anything collided with a spikedPlatform
            boolean bd1isCharacterModel = bd1.getClass().getSuperclass().getSimpleName().equals("CharacterModel") ||
                    bd1.getClass().getSuperclass().getSuperclass().getSimpleName().equals("CharacterModel");
            boolean bd2isCharacterModel = bd2.getClass().getSuperclass().getSimpleName().equals("CharacterModel") ||
                    bd2.getClass().getSuperclass().getSuperclass().getSimpleName().equals("CharacterModel");

            if (((bd1.getName().contains("spiked")) && bd2isCharacterModel) ||
            bd2.getName().contains("spiked") && bd1isCharacterModel){
                if ((avatar == bd1 || avatar == bd2) && !isComplete()){
                    // Player is dead
                    // System.out.println("PLAYER DIED");
                    avatar.setIsDead(true);
                    deathId = playSound(deathSound, deathId, 0.1f);
                    setFailure(true);
                }
                else if (bd1isCharacterModel){
                    AbstractBeeModel bee = (AbstractBeeModel) bd1;
                    // System.out.println("ENEMY DIED: "+bee.getSensorName());
                    bee.setIsDead(true);
                    deathId = playSound(deathSound, deathId, 0.1f);
                    // Marked for removed, moved to the update loop
                    // enemy is only removed when the death animation finishes playing
                    // bd1.markRemoved(true);
                } else {
                    AbstractBeeModel bee = (AbstractBeeModel) bd2;
                    // System.out.println("ENEMY DIED: "+bee.getSensorName());
                    bee.setIsDead(true);
                    deathId = playSound(deathSound, deathId, 0.1f);
                    // Marked for removed, moved to the update loop
                    // enemy is only removed when the death animation finishes playing
                    // bd2.markRemoved(true);
                }
            }

            // See if we have landed on the ground.
            if (((avatar.getSensorName().equals(fd2) && avatar != bd1) && (bd1.getName().contains("platform")) &&
                        !sensorFixtures.contains(fix1)) ||
                ((avatar.getSensorName().equals(fd1)&& avatar != bd2) && (bd2.getName().contains("platform")) &&
                        !sensorFixtures.contains(fix2))) {
                avatar.setGrounded(true);
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground
            }
            // ITERATE OVER ALL CHASER BEES
            for(AbstractBeeModel bee : bees) {
                if ((((bee.getSensorName().equals(fd2) && bee != bd1)&&(bd1.getName().contains("platform")) &&
                        !bee.getSensorFixtures().contains(fix1)) ||
                    ((bee.getSensorName().equals(fd1) && bee != bd2)&&(bd2.getName().contains("platform")) &&
                        !bee.getSensorFixtures().contains(fix2)))&&bee.getClass()== LarvaeModel.class) {
                    bee.setGrounded(true);
                    bee.getSensorFixtures().add(bee == bd1 ? fix2 : fix1); // Could have more than one ground
                }
                if (((bee.getSensorName().equals(fd2) && bee != bd1)&& fix2.isSensor()&&(bd1.getName().contains("honeypatch")) &&
                        !bee.getHoneyFixtures().contains(fix1)) ||
                    ((bee.getSensorName().equals(fd1) && bee != bd2)&& fix1.isSensor()&&(bd2.getName().contains("honeypatch"))&&
                            !bee.getHoneyFixtures().contains(fix2))) {
                    //System.out.print(bee.getSensorName()+" in honeypatch\n");
                    //bee.setGrounded(true);
                    bee.setInHoney(true);
                    //bee.setHoneyTime(0.5f);
                    bee.getHoneyFixtures().add(bee == bd1 ? fix2 : fix1); // Could have more than one ground
                    bee.setMaxspeed(level.getHoneyPatches().getSlowSpeed());
                }
            }
            // Check for honeypatch
            if (((avatar.getSensorName().equals(fd2) && avatar != bd1)&& fix2.isSensor() && (bd1.getName().contains("honeypatch"))&&
                    !honeyFixtures.contains(fix1)) ||
                ((avatar.getSensorName().equals(fd1) && avatar != bd2)&& fix1.isSensor() && (bd2.getName().contains("honeypatch"))&&
                        !honeyFixtures.contains(fix2))) {
                //avatar.setGrounded(true);
                //System.out.print("honeypatch!\n");
                avatar.setInHoney(true);
                //avatar.setHoneyTime(0.5f);
                honeyFixtures.add(avatar == bd1 ? fix2 : fix1);
                avatar.setMaxspeed(level.getHoneyPatches().getSlowSpeed());
            }

            // Check for contact with enemy
            if (!isFailure() && !isComplete() &&
                    ((bd1 == avatar && bd2.getClass().getSuperclass() == AbstractBeeModel.class) ||
                    (bd1.getClass().getSuperclass() == AbstractBeeModel.class && bd2 == avatar))) {
                avatar.setIsDead(true);
                deathId = playSound(deathSound, deathId, 0.1f);
                setFailure(true);
            }

            // Check for win condition
            if (((bd1 == avatar && bd2 == goalDoor) ||
                    (bd1 == goalDoor && bd2 == avatar))&&!isComplete()) {
                winId = playSound(winSound, winId);
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

        //Object bd1 = body1.getUserData();
        //Object bd2 = body2.getUserData();

        PlayerModel avatar = level.getPlayer();
        Array<AbstractBeeModel> bees = level.getBees();

        try {
            Obstacle bd1 = (Obstacle) body1.getUserData();
            Obstacle bd2 = (Obstacle) body2.getUserData();
            if (((avatar.getSensorName().equals(fd2) && avatar != bd1)&&(bd1.getName().contains("platform")))  ||
                    ((avatar.getSensorName().equals(fd1) && avatar != bd2)&&(bd2.getName().contains("platform")))) {
                sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
                if (sensorFixtures.size == 0) {
                    avatar.setGrounded(false);
                }
            }
            if (((avatar.getSensorName().equals(fd2) && avatar != bd1)&& fix2.isSensor()&&(bd1.getName().contains("honeypatch")))  ||
                    ((avatar.getSensorName().equals(fd1)  && avatar != bd2)&& fix1.isSensor()&&(bd2.getName().contains("honeypatch")))) {
                honeyFixtures.remove(avatar == bd1 ? fix2 : fix1);
                if(honeyFixtures.size == 0) {
                    avatar.setDefaultMaxspeed();
                    avatar.setInHoney(false);
                }
                //if (sensorFixtures.size == 0) {
                //    avatar.setGrounded(false);
                //}
            }

            for(AbstractBeeModel bee : bees) {
                if (((bee.getSensorName().equals(fd2) && bee != bd1)&&(bd1.getName().contains("platform"))) ||
                        ((bee.getSensorName().equals(fd1) && bee != bd2)&&(bd2.getName().contains("platform")))) {
                    bee.getSensorFixtures().remove(bee == bd1 ? fix2 : fix1);
                    if (bee.getSensorFixtures().size == 0) {
                        bee.setGrounded(false);
                    }
                }
                if (((bee.getSensorName().equals(fd2) && bee != bd1)&& fix2.isSensor()&&(bd1.getName().contains("honeypatch"))) ||
                        ((bee.getSensorName().equals(fd1) && bee != bd2)&& fix1.isSensor()&&(bd2.getName().contains("honeypatch")))) {
                    bee.getHoneyFixtures().remove(bee == bd1 ? fix2 : fix1);
                    //System.out.print("exiting honeypatch!\n");
                    if (bee.getHoneyFixtures().size == 0) {
                        bee.setDefaultMaxspeed();
                        bee.setInHoney(false);
                    }
                    //if (bee.getSensorFixtures().size == 0) {
                    //    bee.setGrounded(false);
                    //}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Draw the physics objects to the canvas
     *
     * For simple worlds, this method is enough by itself.  It will need
     * to be overriden if the world needs fancy backgrounds or the like.
     *
     * The method draws all objects in the order that they were added.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void draw(float dt) {
        for(Obstacle obj : objects) {
            obj.draw(canvas);
        }
        canvas.end();

        if (debug) {
            canvas.beginDebug();
            for(Obstacle obj : objects) {
                obj.drawDebug(canvas);
            }
            if (aIDebug) {
                aIController.drawDebugLines(canvas, scale);
            }
            canvas.endDebug();
        }

        // Final message
        if (complete && !failed) {
            displayFont.setColor(Color.YELLOW);
            canvas.begin(); // DO NOT SCALE
            canvas.drawTextCentered("VICTORY!", displayFont, 0.0f);
            canvas.end();
        } else if (failed) {
            displayFont.setColor(Color.RED);
            canvas.begin(); // DO NOT SCALE
            canvas.drawTextCentered("FAILURE!", displayFont, 0.0f);
            canvas.end();
        }
    }

    /**
     * Method to ensure that a sound asset is only played once.
     *
     * Every time you play a sound asset, it makes a new instance of that sound.
     * If you play the sounds to close together, you will have overlapping copies.
     * To prevent that, you must stop the sound before you play it again.  That
     * is the purpose of this method.  It stops the current instance playing (if
     * any) and then returns the id of the new instance for tracking.
     *
     * @param sound		The sound asset to play
     * @param soundId	The previously playing sound instance
     *
     * @return the new sound instance for this asset.
     */
    public long playSound(SoundBuffer sound, long soundId) {
        return playSound( sound, soundId, 1.0f );
    }


    /**
     * Method to ensure that a sound asset is only played once.
     *
     * Every time you play a sound asset, it makes a new instance of that sound.
     * If you play the sounds to close together, you will have overlapping copies.
     * To prevent that, you must stop the sound before you play it again.  That
     * is the purpose of this method.  It stops the current instance playing (if
     * any) and then returns the id of the new instance for tracking.
     *
     * @param sound		The sound asset to play
     * @param soundId	The previously playing sound instance
     * @param volume	The sound volume
     *
     * @return the new sound instance for this asset.
     */
    public long playSound(SoundBuffer sound, long soundId, float volume) {
        if (soundId != -1 && sound.isPlaying( soundId )) {
            sound.stop( soundId );
        }
        return sound.play(volume);
    }

    /**
     * Same as playSound but it loops
     * @param sound     Sound asset to play
     * @param soundId   Sound instance
     * @return  the new sound instance
     */
    public long loopSound(SoundBuffer sound, long soundId){
        if (soundId != -1 && sound.isPlaying (soundId)){
            sound.stop(soundId);
        }
        return sound.loop(1f);
    }

    /**
     * Stops all sounds that are playing
     */
    public void stopAllSounds(){
        bgm.stop(bgmId);
        deathSound.stop(deathId);
        trackingSound.stop(trackingId);
        winSound.stop(winId);
    }

    /**
     * Called when the Screen is resized.
     *
     * This can happen at any point during a non-paused state but will never happen
     * before a call to show().
     *
     * @param width  The new width in pixels
     * @param height The new height in pixels
     */
    public void resize(int width, int height) {
        // IGNORE FOR NOW
    }

    /**
     * Called when the Screen is resumed from a paused state.
     *
     * This is usually when it regains focus.
     */
    public void resume() {
        // IGNORE FOR NOW
    }

    /**
     * Called when this screen becomes the current screen for a Game.
     */
    public void show() {
        // Useless if called in outside animation loop
        active = true;
    }

    /**
     * Called when this screen is no longer the current screen for a Game.
     */
    public void hide() {
        // Useless if called in outside animation loop
        active = false;
    }

    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
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
        if (deathSound.isPlaying(deathId)) {
            deathSound.stop(deathId);
        }
        if (winSound.isPlaying(winId)) {
            winSound.stop(winId);
        }
        if (trackingSound.isPlaying(trackingId)) {
            trackingSound.stop(trackingId);
        }
        if (bgm.isPlaying(bgmId)){
            bgm.stop(bgmId);
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