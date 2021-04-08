package edu.cornell.gdiac.honeyHeistCode.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundBuffer;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.honeyHeistCode.GameplayController;
import edu.cornell.gdiac.honeyHeistCode.models.*;
import edu.cornell.gdiac.honeyHeistCode.obstacle.BoxObstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.Obstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.PooledList;
import edu.cornell.gdiac.util.ScreenListener;

import java.util.Iterator;

public class EditorController implements Screen {
    /** The texture for walls and platforms */
    protected TextureRegion earthTile;
    /** The texture for the exit condition */
    protected TextureRegion goalTile;
    /** The texture for the background */
    protected TextureRegion background;
    /** The font for giving messages to the player */
    protected BitmapFont displayFont;

    /**
     * Texture asset for player avatar
     */
    private TextureRegion avatarTexture;
    /**
     * Texture filmstrip for player walking animation
     */
    private FilmStrip walkingPlayer;
    /**
     * Texture asset for chaser bee avatar
     */
    private TextureRegion chaserBeeTexture;
    /**
     * Texture asset for testEnemy avatar
     */
    private TextureRegion sleeperBeeTexture;

    private BitmapFont modeFont;

    /** Exit code for quitting the game */
    public static final int EXIT_QUIT = 0;
    /** Exit code for advancing to next level */
    public static final int EXIT_NEXT = 1;
    /** Exit code for jumping back to previous level */
    public static final int EXIT_PREV = 2;
    /** How many frames after winning/losing do we continue? */
    public static final int EXIT_COUNT = 100;

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
    protected boolean complete;
    /** Whether we have failed at this world (and need a reset) */
    protected boolean failed;
    /** Whether or not debug mode is active */
    protected boolean debug;
    /** Countdown active for winning or losing */
    private int countdown;

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
     * Constant data across levels
     */
    private JsonValue constants;
    /**
     * Data for the level
     */
    private JsonValue levelData;
    /**
     * Reference to the level model
     */
    private LevelModel level;

    private Array<Vector2> clickCache;

    private int mode;

    private float platWidth = 0.5f;

    private PolygonShape outline;

    private boolean drawOutline;

    /** Filehandler for saving and loading jsons */
    private FileHandle file = Gdx.files.local("bin/savedLevel.json");

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

    /**
     * Creates and initialize a new instance of the platformer game
     * <p>
     * The game has default gravity and other settings
     */
    public EditorController() {
        world = new World(new Vector2(0,DEFAULT_GRAVITY),false);
        this.bounds = new Rectangle(0,0,DEFAULT_WIDTH,DEFAULT_HEIGHT);
        this.scale = new Vector2(1,1);
        complete = false;
        failed = false;
        debug  = false;
        active = false;
        countdown = -1;
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

        walkingPlayer = directory.getEntry( "platform:walk.pacing", FilmStrip.class );

        jumpSound = directory.getEntry("platform:jump", SoundBuffer.class);
        fireSound = directory.getEntry("platform:pew", SoundBuffer.class);
        plopSound = directory.getEntry("platform:plop", SoundBuffer.class);

        constants = directory.getEntry("platform:constants2", JsonValue.class);
        levelData = directory.getEntry("platform:prototypeLevel", JsonValue.class);
        modeFont = directory.getEntry("shared:marker",BitmapFont.class);

        earthTile = new TextureRegion(directory.getEntry( "shared:earth", Texture.class ));
        goalTile  = new TextureRegion(directory.getEntry( "shared:goal", Texture.class ));
        background = new TextureRegion(directory.getEntry( "shared:background",  Texture.class ));
        displayFont = directory.getEntry( "shared:retro" ,BitmapFont.class);
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
        setComplete(false);
        setFailure(false);
        populateLevel();
        drawOutline = false;
    }

    /**
     * Lays out the game geography.
     */
    public void populateLevel() {

        volume = constants.getFloat("volume", 1.0f);

        level = new LevelModel();
        level.setBees(new Array<AbstractBeeModel>());
        level.setPlatforms(new PlatformModel());
        clickCache = new Array<Vector2>();
        outline = new PolygonShape();
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
        InputController input = InputController.getInstance();
        input.readInput(bounds, scale);
        if (listener == null) {
            return true;
        }

        // Toggle debug
        if (input.didDebug()) {
            debug = !debug;
        }

        // Handle resets
        if (input.didReset()) {
            reset();
        }

        // Now it is time to maybe switch screens.
        if (input.didExit()) {
            pause();
            listener.exitScreen(this, EXIT_QUIT);
            return false;
        } else if (input.didAdvance()) {
            pause();
            listener.exitScreen(this, EXIT_NEXT);
            return false;
//		} else if (input.didRetreat()) {
//			pause();
//			listener.exitScreen(this, EXIT_PREV);
//			return false;
        } else if (countdown > 0) {
            countdown--;
        } else if (countdown == 0) {
            if (failed) {
                reset();
            } else if (complete) {
                pause();
                listener.exitScreen(this, EXIT_NEXT);
                return false;
            }
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
        InputController input = InputController.getInstance();
        if (input.didMode()){
            mode = (mode+1) % 5;
            clickCache.clear();
            drawOutline = false;
        }
        if (input.didMouseClick()){
            clickCache.add(new Vector2(input.getCrossHair().x,input.getCrossHair().y));
            //PLACE PLATFORM MODE
            if (mode == 0) {
                if (clickCache.size>=2){
                    snapClick();

                    Vector2 previousClick = clickCache.get(clickCache.size-2);
                    Vector2 currentClick = clickCache.get(clickCache.size-1);
                    float[] points = rectFromTwoPoints(previousClick,currentClick);

                    PolygonObstacle obj;
                    obj = new PolygonObstacle(points, 0, 0);
                    obj.setBodyType(BodyDef.BodyType.StaticBody);
                    obj.setDrawScale(scale);
                    obj.setTexture(earthTile);
                    addObject(obj);
                    obj.setActive(false);
                    level.getPlatforms().getArrayBodies().add(obj);

                    clickCache.clear();
                    drawOutline = false;
                }
            }
            //PLACE PLAYER MODE
            if (mode == 1) {
                if (level.getPlayer() == null) {
                    float dwidth = avatarTexture.getRegionWidth() / scale.x;
                    float dheight = avatarTexture.getRegionHeight() / scale.y;

                    PlayerModel avatar = new PlayerModel(constants.get("player"),
                            clickCache.get(0).x, clickCache.get(0).y, dwidth, dheight);

                    avatar.setDrawScale(scale);
                    avatar.setTexture(avatarTexture);
                    avatar.setAnimationStrip(PlayerModel.AntAnimations.WALK, walkingPlayer);
                    addObject(avatar);
                    avatar.setActive(false);
                    level.setPlayer(avatar);
                }
                clickCache.clear();
            }

            //PLACE BEE MODE
            if (mode == 2) {
                float dwidth =  chaserBeeTexture.getRegionWidth() / scale.x;
                float dheight = chaserBeeTexture.getRegionHeight() / scale.y;

                ChaserBeeModel chaserBee = new ChaserBeeModel(constants.get("GroundedBee"),
                        clickCache.get(0).x, clickCache.get(0).y, dwidth, dheight);

                chaserBee.setDrawScale(scale);
                chaserBee.setTexture(chaserBeeTexture);
                level.getBees().add(chaserBee);
                addObject(chaserBee);
                chaserBee.setActive(false);

                clickCache.clear();
            }

            //PLACE GOAL MODE
            if (mode == 3) {
                if (level.getGoalDoor() == null) {
                    float dwidth = goalTile.getRegionWidth() / scale.x;
                    float dheight = goalTile.getRegionHeight() / scale.y;

                    BoxObstacle goalDoor = new BoxObstacle(clickCache.get(0).x, clickCache.get(0).y,
                            dwidth, dheight);

                    goalDoor.setBodyType(BodyDef.BodyType.StaticBody);
                    goalDoor.setDrawScale(scale);
                    goalDoor.setTexture(goalTile);
                    level.setGoalDoor(goalDoor);
                    addObject(goalDoor);
                    goalDoor.setActive(false);

                    clickCache.clear();
                }
            }

            //SELECT MODE
            if (mode == 4){
                clickCache.clear();
            }
        }
        if (input.didSave()){
            convertToJson();
        }

        //if clicked once and in platform mode, update outline
        if (mode == 0 && clickCache.size==1){
            Vector2 currentClick = clickCache.get(0);
            Vector2 nearest = nearestPointAngle(currentClick,input.getCrossHair(),Math.PI/3);
            if(currentClick.x != nearest.x || currentClick.y != nearest.y){
                outline.set(rectFromTwoPoints(currentClick, nearest));
                drawOutline = true;
            }
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
        world.step(WORLD_STEP,WORLD_VELOC,WORLD_POSIT);

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
     * Snap the most recent click in click cache to the nearest 60 degree increment
     */
    private void snapClick(){
        Vector2 previousClick = clickCache.get(clickCache.size-2);
        Vector2 currentClick = clickCache.get(clickCache.size-1);
        clickCache.set(clickCache.size-1,nearestPointAngle(previousClick, currentClick, Math.PI/3));
    }

    /**
     * Returns the nearest point to endPoint along a ray from startPoint with an angle of some multiple of
     * angleIncrement
     *
     * @param startPoint the start point from which angles are determined
     * @param endPoint the point which will be approximated onto the nearest angle increment line
     * @param angleIncrement the desired angle increment
     * @return the nearest Vector2 point to endPoint along an angle increment line
     */
    private Vector2 nearestPointAngle(Vector2 startPoint, Vector2 endPoint, double angleIncrement){
        Vector2 vec = new Vector2(endPoint.x-startPoint.x,endPoint.y-startPoint.y);
        double length = Math.sqrt(Math.pow(vec.x,2)+Math.pow(vec.y,2));

        double currAngle = Math.atan(vec.y/vec.x);
        double newAngle;

        newAngle = Math.abs(Math.round(currAngle / angleIncrement)) * angleIncrement;

        if (currAngle < 0) {
            newAngle = -newAngle;
        }

        Vector2 result = new Vector2((float)(length*Math.cos(newAngle)),
                (float)(length*Math.sin(newAngle)));

        if (vec.x<0){
            result.x = -result.x;
            result.y = -result.y;
        }

        return result.add(startPoint);
    }


    /**
     * Creates a rectangle based on the two points a and b
     *
     * @param a the first point
     * @param b the second point
     * @return the list of vertices comprising the rectangle
     */
    private float[] rectFromTwoPoints(Vector2 a, Vector2 b) {
        Vector2 vec = new Vector2(b.x-a.x,b.y-a.y);
        double angle = Math.atan(vec.y/vec.x);
        angle += Math.PI/2;
        Vector2 offset = new Vector2((float)Math.cos(angle)*platWidth/2,(float)Math.sin(angle)*platWidth/2);
        float[] result = {
                a.x+offset.x,a.y+offset.y,
                a.x-offset.x,a.y-offset.y,
                b.x-offset.x,b.y-offset.y,
                b.x+offset.x,b.y+offset.y,
        };
        return result;
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
        canvas.clear();

        canvas.begin();
        canvas.draw(background, 0, 0);
        for(Obstacle obj : objects) {
            obj.draw(canvas);
        }

        canvas.end();

        if (debug) {
            canvas.beginDebug();
            for(Obstacle obj : objects) {
                obj.drawDebug(canvas);
            }
            canvas.endDebug();
        }

        // Set mode text
        String modeText = "MODE: ";
        if(mode == 0){modeText += "Place Platform";}
        if(mode == 1){modeText += "Place Player";}
        if(mode == 2){modeText += "Place Bee";}
        if(mode == 3){modeText += "Place Goal Door";}
        if(mode == 4){modeText += "Select";}

        // Draw mode text
        canvas.begin();
        modeFont.setColor(Color.WHITE);
        canvas.drawTextCentered(modeText,modeFont, -canvas.getWidth()/4f);
        canvas.end();

        // Draw platform outline
        if(drawOutline){
            canvas.beginDebug();
            //System.out.print("drawline");
            canvas.drawPhysics(outline,Color.RED,0,0,0,scale.x,scale.y);
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
     * Called when the Screen should render itself.
     *
     * We defer to the other methods update() and draw().  However, it is VERY important
     * that we only quit AFTER a draw.
     *
     * @param delta Number of seconds since last animation frame
     */
    public void render(float delta) {
        if (active) {
            if (preUpdate(delta)) {
                update(delta); // This is the one that must be defined.
                postUpdate(delta);
            }
            draw(delta);
        }
    }


    /**
     * Called when the Screen is resumed from a paused state.
     *
     * This is usually when it regains focus.
     */
    public void resume() {
        // TODO Auto-generated method stub
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

    public class Level{
        public float[] goalPos;
        public float[] playerPos;
        public float[][] beePos;
        public float[][] platformPos;

        public Level(){

        }

        public void setGoal(float[] goalPos) { this.goalPos = goalPos; }
        public void setPlayer(float[] playerPos) { this.playerPos = playerPos; }
        public void setBee(float[][] beePos) { this.beePos = beePos; }
        public void setPlatform(float[][] platformPos) { this.platformPos = platformPos; }

    }

    public void convertToJson(){
        Level jsonLevel = new Level();

        if (level.getGoalDoor()!=null){
            Vector2 goalPos = level.getGoalDoor().getPosition();
            float[] goalArray = new float[]{goalPos.x, goalPos.y};
            jsonLevel.setGoal(goalArray);
        }

        if (level.getPlayer()!=null){
            Vector2 playerPos = level.getPlayer().getPosition();
            float[] playerArray = new float[] {playerPos.x, playerPos.y};
            jsonLevel.setPlayer(playerArray);
        }

        if (level.getBees()!=null){
            Array<AbstractBeeModel> bees = level.getBees();
            float[][] beeArray = new float[bees.size][2];
            for (int i=0; i<beeArray.length; i++) {
                Vector2 beePos = bees.get(i).getPosition();
                beeArray[i][0] = beePos.x;
                beeArray[i][1] = beePos.y;
            }
            jsonLevel.setBee(beeArray);
        }

        if (level.getPlatforms()!=null){
            Array<PolygonObstacle> platforms = level.getPlatforms().getArrayBodies();
            float[][] platformArray = new float[platforms.size][8];
            for (int i=0; i<platformArray.length; i++){
                platformArray[i] = platforms.get(i).getTrueVertices();
            }
            jsonLevel.setPlatform(platformArray);
        }


        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        file.writeString(json.prettyPrint(jsonLevel), false);
        System.out.println("saved");
    }
}
