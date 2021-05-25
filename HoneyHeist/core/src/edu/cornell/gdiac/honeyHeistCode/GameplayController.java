/*
 * WorldController.java
 *
 * This is the most important new class in this lab.  This class serves as a combination
 * of the CollisionController and GameplayController from the previous lab.  There is not
 * much to do for collisions; Box2d takes care of all of that for us.  This controller
 * invokes Box2d and then performs any after the fact modifications to the data
 * (e.g. gameplay).
 *
 * If you study this class, and the contents of the edu.cornell.cs3152.physics.obstacles
 * package, you should be able to understand how the Physics engine works.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * LibGDX version, 2/6/2015
 */
package edu.cornell.gdiac.honeyHeistCode;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundBuffer;
import edu.cornell.gdiac.honeyHeistCode.controllers.EditorController;
import edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers.AISingleCharacterController;
import edu.cornell.gdiac.honeyHeistCode.controllers.InputController;
import edu.cornell.gdiac.honeyHeistCode.obstacle.Obstacle;
import edu.cornell.gdiac.util.PooledList;
import edu.cornell.gdiac.util.ScreenListener;
import edu.cornell.gdiac.honeyHeistCode.controllers.LevelController;

import javax.swing.*;
//import org.

/**
 * Base class for a world-specific controller.
 *
 *
 * A world has its own objects, assets, and input controller.  Thus this is 
 * really a mini-GameEngine in its own right.  The only thing that it does
 * not do is create a GameCanvas; that is shared with the main application.
 *
 * You will notice that asset loading is not done with static methods this time.  
 * Instance asset loading makes it easier to process our game modes in a loop, which 
 * is much more scalable. However, we still want the assets themselves to be static.
 * This is the purpose of our AssetState variable; it ensures that multiple instances
 * place nicely with the static assets.
 */
public class GameplayController implements Screen, InputProcessor {
	/** The texture for walls and platforms */
	protected TextureRegion earthTile;
	/** The texture for the exit condition */
	protected TextureRegion goalTile;
	/** The texture for the background */
	protected TextureRegion background;
	/** The font for giving messages to the player */
	protected BitmapFont menuFont;

	/** Exit code for quitting the game */
	public static final int EXIT_QUIT = 0;
	/** Exit code for advancing to next level */
	public static final int EXIT_NEXT = 1;
	/** Exit code for jumping back to previous level */
	public static final int EXIT_PREV = 2;
	/** Exit code for going to the editor */
	public static final int EXIT_EDITOR = 3;
	/** Exit code for going to the menu */
	public static final int EXIT_MENU = 4;
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
    private boolean complete;
    /** Whether we have failed at this world (and need a reset) */
    private boolean failed;
    /** Whether or not debug mode is active */
    private boolean debug;
    /** Countdown active for winning or losing */
    private int countdown;
    // ------------------------------- newly added variables
    /** Which level the game is currently in */
    private int level;
    /** List of all GameplayControllers */
    private GameplayController[] controllers;
    /** Level Controller */
    private LevelController levelController;
    /** AI Controller */
    private AISingleCharacterController aiController;
    /** JsonValue constants for AI Controller */
    private JsonValue aiConstants;
    /** JsonValue constants for level Controller */
    private JsonValue levelConstants;

	// ------------------------------- pause
	/** Pause Menu scale */
	private float menuScale = 1f;
	/** Pause Menu Background texture */
	private Texture pauseBackground;
	/** Constants for the position of pauseBackground */
	private final float PAUSE_BG_XPOS = Gdx.graphics.getWidth()*0.668f;
	private final float PAUSE_BG_YPOS = Gdx.graphics.getHeight()*0.77f;
	private float PAUSE_BG_SCALE = 1.3f;
	/** Pause Menu Quit texture */
	private Texture pauseQuit;
	/** If the quit button is pressed */
	private boolean quitPressed;
	/** If the game is ready to quit */
	private boolean quitReady;
	/** Constants for the position of pauseQuit */
	private final float PAUSE_QUIT_XPOS = Gdx.graphics.getWidth()*0.63f;
	private final float PAUSE_QUIT_YPOS = Gdx.graphics.getHeight()*0.4f;
	/** Pause Menu Resume texture */
	private Texture pauseResume;
	/** If the resume button is pressed */
	private boolean resumePressed;

	private JsonValue allLevelData;
	private int currentLevelNum;

//	/** If the game is ready to resume */
//	private boolean resumeReady;
	/** Constants for the position of pauseResume */
	private final float PAUSE_RESUME_XPOS = Gdx.graphics.getWidth()*0.63f;
	private final float PAUSE_RESUME_YPOS = Gdx.graphics.getHeight()*0.7f;
	/** Pause Menu MainMenu texture */
	private Texture pauseMenu;
//	/** If the main menu button is pressed */
//	private boolean menuPressed;
	/** Constants for the position of pauseMenu */
	private final float PAUSE_MENU_XPOS = Gdx.graphics.getWidth()*0.63f;
	private final float PAUSE_MENU_YPOS = Gdx.graphics.getHeight()*0.55f;
	/** Pause button texture */
    private Texture pauseButton;
	/** If the pause button is pressed */
	private boolean pausePressed;
	/** If it's in the pause state */
	private boolean isPaused;
	/** Constants for the position of pause button */
	private final float PAUSE_XPOS = Gdx.graphics.getWidth()*0.94f;
	private final float PAUSE_YPOS = Gdx.graphics.getHeight()*0.94f;
	private final float PAUSE_SCALE = 0.4f;
	/** Menu button texture */
	private Texture menuButton;
	/** Offset for the menu word on the button */
	private static final float MENU_XOFFSET   = 50.0f;
	private static final float MENU_YOFFSET   = 10.0f;
	/** If menu button is pressed */
	private boolean menuPressed;
	/** If menu is ready to go */
	private boolean menuReady;
	/** Constants for the position of menu button */
	private final float MENU_XPOS = Gdx.graphics.getWidth()*0.15f;
	private final float MENU_YPOS = Gdx.graphics.getHeight()*0.85f;
	private final float MENU_XSCALE = 0.15f;
	private final float MENU_YSCALE = 0.3f;
	/** Standard window size (for scaling) */
	private static int STANDARD_WIDTH  = 800;
	/** Standard window height (for scaling) */
	private static int STANDARD_HEIGHT = 700;
	/** Scaling factor for when the student changes the resolution. */
	private float scaleFactor;
	/** The height of the canvas window (necessary since sprite origin != screen origin) */
	private int heightY;
	/** The y-coordinate of the center of the progress bar */
	private int centerY;
	/** The x-coordinate of the center of the progress bar */
	private int centerX;

    /**
     * Returns levelController
     *
     * @return LevelController
     */
    public LevelController getLevelController( ) {
        return levelController;
    }

	/**
	 * Returns the current level number
	 *
	 * @return currentLevelNum
	 */
	public int getCurrentLevelNum( ) {
		return currentLevelNum;
	}

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
        return levelController.isComplete();
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
            levelController.setCountdown(EXIT_COUNT);
        }
        levelController.setComplete(value);
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
//        this.canvas = canvas;
//        this.scale.x = canvas.getWidth()/bounds.getWidth();
//        this.scale.y = canvas.getHeight()/bounds.getHeight();
        levelController.setCanvas(canvas);
        this.canvas = levelController.getCanvas();
        Vector2 scale = levelController.getScale();
        this.scale.x = scale.x;
        this.scale.y = scale.y;
		// Compute the dimensions from the canvas
		resize(canvas.getWidth(),canvas.getHeight());
    }
    // --------------------------------------- root controller -- end

	/**
	 * Creates a new game world
	 *
	 * The game world is scaled so that the screen coordinates do not agree
	 * with the Box2d coordinates.  The bounds are in terms of the Box2d
	 * world, not the screen.
	 *
	 * @param width  	The width in Box2d coordinates
	 * @param height	The height in Box2d coordinates
	 * @param gravity	The downward gravity
	 */
	protected GameplayController(float width, float height, float gravity) {
		this(new Rectangle(0,0,width,height), new Vector2(0,gravity));
	}

	/**
	 * Creates a default new game world
	 */
	protected GameplayController() {
		this(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_GRAVITY);
	}

	/**
	 * Creates a new game world
	 *
	 * The game world is scaled so that the screen coordinates do not agree
	 * with the Box2d coordinates.  The bounds are in terms of the Box2d
	 * world, not the screen.
	 *
	 * @param bounds	The game bounds in Box2d coordinates
	 * @param gravity	The gravitational force on this Box2d world
	 */
	protected GameplayController(Rectangle bounds, Vector2 gravity) {
		this.bounds = new Rectangle(bounds);
		this.scale = new Vector2(1,1);
		complete = false;
		failed = false;
		debug  = false;
		active = false;
		isPaused = false;
		countdown = -1;
		// initialize the level controller
		levelController = new LevelController();
	}

	/**
	 * Dispose of all (non-static) resources allocated to this mode.
	 */
//	public void dispose() {
//		for(Obstacle obj : objects) {
//			obj.deactivatePhysics(world);
//		}
//		objects.clear();
//		addQueue.clear();
//		world.dispose();
//		objects = null;
//		addQueue = null;
//		bounds = null;
//		scale  = null;
//		world  = null;
//		canvas = null;
//	}
	public void dispose() {
		levelController.dispose();
	}

	/**
	 * Gather the assets for this controller.
	 *
	 * This method extracts the asset variables from the given asset directory. It
	 * should only be called after the asset directory is completed.
	 *
	 * @param directory	Reference to global asset manager.
	 */
	public void gatherAssets(AssetDirectory directory, String levelData, JsonValue allLevelData, int currentLevelNum) {
		// Allocate the tiles
		this.allLevelData = allLevelData;
		this.currentLevelNum = currentLevelNum;
		// background
		background = new TextureRegion(directory.getEntry( "shared:background",  Texture.class ));
		menuFont = directory.getEntry( "menuFont" ,BitmapFont.class);
		pauseButton = directory.getEntry("shared:pauseButton", Texture.class);
		menuButton = directory.getEntry("shared:menuButton", Texture.class);
		levelController.gatherAssets(directory, levelData);
		pauseBackground = directory.getEntry("shared:pauseBackground", Texture.class);
		pauseQuit = directory.getEntry("shared:pauseQuit", Texture.class);
		pauseResume = directory.getEntry("shared:pauseResume", Texture.class);
		pauseMenu = directory.getEntry("shared:pauseMainMenu", Texture.class);
	}

	public void gatherLevelData(AssetDirectory directory, String dataFilePath){
		levelController.gatherLevelData(directory, dataFilePath);
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
     *
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        // TODO
		pausePressed = false;
		menuPressed = false;
		menuReady = false;
		quitPressed = false;
		quitReady = false;
		resumePressed = false;
//		resumeReady = false;
		isPaused = false;
        levelController.reset();
    }

    /**
     * Returns whether to process the update loop
     *
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode.  If not, the update proceeds
     * normally.
     *
     * @param dt	Number of seconds since last animation frame
     *
     * @return whether to process the update loop
     */
    public boolean preUpdate(float dt) {
		// If in the pause state, do not update
        boolean temp = preUpdateHelper(dt);
        boolean result = levelController.preUpdate(temp);
		// MENU button has a higher priority then the PAUSE button
		// so preUpdateHelper needs to proceed first
		if (isPaused) { return false; }
//        setFailure(levelController.isFailure());
        return result;
    }

    private boolean preUpdateHelper(float dt) {
        InputController input = InputController.getInstance();
        input.readInput(bounds, scale);
        if (listener == null) {
            return true;
        }

        // Toggle debug
        /*if (input.didDebug()) {
//            debug = !debug;
            levelController.setDebug(!levelController.isDebug());
        }

        if (input.didDebugAI()) {
        	levelController.setAIDebug(!levelController.isAIDebug());
		}

        // Handle resets
        if (input.didReset()) {
            reset();
        }*/

        // Now it is time to maybe switch screens.
        if (quitReady) {
			pause();
			listener.exitScreen(this, EXIT_QUIT);
			return false;
		} else if (input.didExit()) {
        	isPaused = !isPaused;
        	return false;
//        } else if (input.didAdvance()) {
//			pause();
//			listener.exitScreen(this, EXIT_EDITOR);
//			return false;
		} else if (menuReady) {
        	pause();
        	levelController.stopAllSounds();
        	levelController.setBgmId(1);
        	listener.exitScreen(this, EXIT_MENU);
        	return false;
		} else if (levelController.getCountdown() > 0) {
			levelController.decreaseCountdown();
		} else if (levelController.getCountdown() == 0) {
			if (levelController.isFailure()) {
				reset();
			} else if (levelController.isComplete()) {
				pause();
//				saveData();
				currentLevelNum ++;
				System.out.println("current level: " + currentLevelNum);
				listener.exitScreen(this, EXIT_NEXT);
				return false;
//				reset();
			}
		}
		return true;
	}

	/**
	 * The core gameplay loop of this world.
	 *
	 * This method contains the specific update code for this mini-game. It does
	 * not handle collisions, as those are managed by the parent class WorldController.
	 * This method is called after input is read, but before collisions are resolved.
	 * The very last thing that it should do is apply forces to the appropriate objects.
	 *
	 * @param dt	Number of seconds since last animation frame
	 */
//	public abstract void update(float dt);
	public void update(float dt) {
		float horizontal = InputController.getInstance().getHorizontal();
		boolean isRotate = InputController.getInstance().didRotate();
		boolean isAntiRotate = InputController.getInstance().didAntiRotate();
		levelController.update(horizontal, isRotate, isAntiRotate);
	};
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
//		// Add any objects created by actions
//		while (!addQueue.isEmpty()) {
//			addObject(addQueue.poll());
//		}
//
//		// Turn the physics engine crank.
//		world.step(WORLD_STEP,WORLD_VELOC,WORLD_POSIT);
//
//		// Garbage collect the deleted objects.
//		// Note how we use the linked list nodes to delete O(1) in place.
//		// This is O(n) without copying.
//		Iterator<PooledList<Obstacle>.Entry> iterator = objects.entryIterator();
//		while (iterator.hasNext()) {
//			PooledList<Obstacle>.Entry entry = iterator.next();
//			Obstacle obj = entry.getValue();
//			if (obj.isRemoved()) {
//				obj.deactivatePhysics(world);
//				entry.remove();
//			} else {
//				// Note that update is called last!
//				obj.update(dt);
//			}
//		}
		levelController.postUpdate(dt);
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
		// resize the image to have width and height fit for the Gdx graphics (screen)
		canvas.draw(background, Color.WHITE, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Color tint;
		if (pauseButton != null) {
			tint = (pausePressed ? Color.GRAY: Color.WHITE);
			canvas.draw(pauseButton, tint, pauseButton.getWidth(), pauseButton.getHeight(),
					PAUSE_XPOS, PAUSE_YPOS, 0, PAUSE_SCALE*scaleFactor, PAUSE_SCALE*scaleFactor);
		}
		levelController.draw(dt);
		canvas.begin();
		if (isPaused) {
			if (pauseBackground !=null) {
				canvas.draw(pauseBackground, Color.WHITE, pauseBackground.getWidth(), pauseBackground.getHeight(),
						PAUSE_BG_XPOS, PAUSE_BG_YPOS, 0, menuScale*scaleFactor*PAUSE_BG_SCALE,
						menuScale*scaleFactor*PAUSE_BG_SCALE);
			}
			if (pauseMenu != null) {
				tint = (menuPressed ? Color.GRAY: Color.WHITE);
				canvas.draw(pauseMenu, tint, pauseMenu.getWidth(), pauseMenu.getHeight(), PAUSE_MENU_XPOS,
						PAUSE_MENU_YPOS, 0, menuScale*scaleFactor, menuScale*scaleFactor);
			}
			if (pauseResume != null) {
				tint = (resumePressed ? Color.GRAY: Color.WHITE);
				canvas.draw(pauseResume, tint, pauseResume.getWidth(), pauseResume.getHeight(),
						PAUSE_RESUME_XPOS, PAUSE_RESUME_YPOS, 0, menuScale*scaleFactor,
						menuScale*scaleFactor);
			}
			if (pauseQuit != null) {
				tint = (quitPressed ? Color.GRAY: Color.WHITE);
				canvas.draw(pauseQuit, tint, pauseQuit.getWidth(), pauseQuit.getHeight(), PAUSE_QUIT_XPOS,
						PAUSE_QUIT_YPOS, 0, menuScale*scaleFactor, menuScale*scaleFactor);
			}
		}
		canvas.end();
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
		// Compute the drawing scale
		float sx = ((float)width)/STANDARD_WIDTH;
		float sy = ((float)height)/STANDARD_HEIGHT;
		scaleFactor = (sx < sy ? sx : sy);
		menuScale = 1/scaleFactor;
		heightY = height;
		centerY = height/2;
		centerX = width/2;
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
	 * Called when the Screen is paused.
	 *
	 * This is usually when it's not active or visible on screen. An Application is
	 * also paused before it is destroyed.
	 */
	public void pause() {
		// TODO Auto-generated method stub
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
		Gdx.input.setInputProcessor( this );
	}

	/**
	 * Called when the mouse button was pressed.
	 *
	 * @param screenX The x coordinate, origin is in the upper left corner
	 * @param screenY The y coordinate, origin is in the upper left corner
	 * @param pointer the pointer for the event.
	 * @param button  the button
	 * @return whether the input was processed
	 */
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (pauseButton==null && menuButton == null) return true;

		// Flip to match graphics coordinates
		screenY = heightY-screenY;

		// MENU button has a higher priority then the PAUSE button
		float width, height;
		if (isPaused) {
			// MENU button
			if (menuButton!=null) {
				width = menuScale * scaleFactor * pauseMenu.getWidth() / 2.0f;
				height = menuScale * scaleFactor * pauseMenu.getHeight() / 2.0f;
				if (Math.abs(screenX - PAUSE_MENU_XPOS + width) < Math.abs(width) && Math.abs(screenY -
						PAUSE_MENU_YPOS + height) < Math.abs(height)) {
					menuPressed = true;
				}
			}
			// QUIT button
			if (pauseQuit!=null) {
				width = menuScale * scaleFactor * pauseQuit.getWidth() / 2.0f;
				height = menuScale * scaleFactor * pauseQuit.getHeight() / 2.0f;
				if (Math.abs(screenX - PAUSE_QUIT_XPOS + width) < Math.abs(width) && Math.abs(screenY -
						PAUSE_QUIT_YPOS + height) < Math.abs(height)) {
					quitPressed = true;
				}
			}
			// RESUME button
			if (pauseMenu!=null) {
				width = menuScale * scaleFactor * pauseResume.getWidth() / 2.0f;
				height = menuScale * scaleFactor * pauseResume.getHeight() / 2.0f;
				if (Math.abs(screenX - PAUSE_RESUME_XPOS + width) < Math.abs(width) && Math.abs(screenY -
						PAUSE_RESUME_YPOS + height) < Math.abs(height)) {
					resumePressed = true;
				}
			}
		}
		// PAUSE button
		if (pauseButton!=null) {
			float radius, dist;
//			Rectangle textureBounds=new Rectangle(PAUSE_XPOS,PAUSE_YPOS,PAUSE_SCALE * scaleFactor *
//					pauseButton.getWidth(), PAUSE_SCALE * scaleFactor * pauseButton.getHeight());
//			if(textureBounds.contains(screenX,screenY)) {
//				pausePressed = true;
//			}
			radius = PAUSE_SCALE*scaleFactor*pauseButton.getWidth()/2.0f;
			dist = (screenX-PAUSE_XPOS+radius)*(screenX-PAUSE_XPOS+radius)+(screenY-PAUSE_YPOS+radius)*(screenY-PAUSE_YPOS+radius);
			if (dist < radius*radius) {
				pausePressed = true;
			}
//			width = PAUSE_SCALE * scaleFactor * pauseButton.getWidth() /2.0f;
//			height = PAUSE_SCALE * scaleFactor * pauseButton.getHeight() /2.0f;
//			if (Math.abs(screenX - PAUSE_XPOS + width) < Math.abs(width) && Math.abs(screenY -
//					PAUSE_YPOS + height) < Math.abs(height)) {
//				pausePressed = true;
//			}
		}
		return false;
	}

	/**
	 * Called when the mouse button was released.
	 *
	 * @param screenX
	 * @param screenY
	 * @param pointer the pointer for the event.
	 * @param button  the button
	 * @return whether the input was processed
	 */
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// MENU button has a higher priority then the PAUSE button
		if (menuPressed) {
			menuPressed = false;
			menuReady = true;
			return false;
		}
		if (pausePressed) {
			pausePressed = false;
//			isPaused = !isPaused;
			isPaused = true;
			return false;
		}
		if (resumePressed) {
			resumePressed = false;
			isPaused = false;
			return false;
		}
		if (quitPressed) {
			quitPressed = false;
			quitReady = true;
			return false;
		}
		return true;
	}

	public class Levels{
		public Level[] levels;
		public Levels(){
		}
		public void setLevel(Level[] levels) { this.levels = levels; }
	}

	public class Level{
		public boolean unlock;
		public boolean complete;
		public String file;
		public Level(){
		}
		public void setUnlock(boolean unlock) { this.unlock = unlock; }
		public void setComplete(boolean complete) { this.complete = complete; }
		public void setFile(String file) { this.file = file; }
	}

	public Levels convertToJsonLevel() {
		Levels jsonLevels = new Levels();
		Level[] levelArray = new Level[allLevelData.size];
		for (int i=0; i<allLevelData.size; i++) {
			Level level = new Level();
			level.setComplete(allLevelData.get(i).get("complete").asBoolean());
			level.setUnlock(allLevelData.get(i).get("unlock").asBoolean());
			level.setFile(allLevelData.get(i).get("file").asString());
			levelArray[i] = level;
		}
		jsonLevels.setLevel(levelArray);

		return jsonLevels;
	}

//	public void saveData() {
//		String localPath = Gdx.files.getLocalStoragePath();
//		FileHandle file = Gdx.files.absolute(localPath + "savedGameData.json");
////		FileHandle gamefile = Gdx.files.external("savedGameData.json");
//		int size = allLevelData.size;
//		allLevelData.get(currentLevelNum - 1).get("complete").set(true);
//		if (currentLevelNum < size) {
//			allLevelData.get(currentLevelNum).get("unlock").set(true);
//		}
//		Levels jsonLevels = convertToJsonLevel();
//		Json json = new Json();
//		json.setOutputType(JsonWriter.OutputType.json);
//		file.writeString(json.prettyPrint(jsonLevels), false);
//		System.out.println("saved");
//	}

	/**
	 * Called when a key was pressed (UNSUPPORTED)
	 *
	 * @param keycode one of the constants in {@link Input.Keys}
	 * @return whether the input was processed
	 */
	@Override
	public boolean keyDown(int keycode) {
		return true;
	}

	/**
	 * Called when a key was released (UNSUPPORTED)
	 *
	 * @param keycode one of the constants in {@link Input.Keys}
	 * @return whether the input was processed
	 */
	@Override
	public boolean keyUp(int keycode) {
		return true;
	}

	/**
	 * Called when a key was typed (UNSUPPORTED)
	 *
	 * @param character The character
	 * @return whether the input was processed
	 */
	@Override
	public boolean keyTyped(char character) {
		return true;
	}

	/**
	 * Called when a finger or the mouse was dragged. (UNSUPPORTED)
	 *
	 * @param screenX
	 * @param screenY
	 * @param pointer the pointer for the event.
	 * @return whether the input was processed
	 */
	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return true;
	}

	/**
	 * Called when the mouse was moved without any buttons being pressed. Will not be called on iOS. (UNSUPPORTED)
	 *
	 * @param screenX
	 * @param screenY
	 * @return whether the input was processed
	 */
	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return true;
	}

	/**
	 * Called when the mouse wheel was scrolled. Will not be called on iOS. (UNSUPPORTED)
	 *
	 * @param amountX the horizontal scroll amount, negative or positive depending on the direction the wheel was scrolled.
	 * @param amountY the vertical scroll amount, negative or positive depending on the direction the wheel was scrolled.
	 * @return whether the input was processed.
	 */
	@Override
	public boolean scrolled(float amountX, float amountY) {
		return true;
	}
}