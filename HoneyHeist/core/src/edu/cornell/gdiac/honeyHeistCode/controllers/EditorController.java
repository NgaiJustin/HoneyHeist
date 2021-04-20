package edu.cornell.gdiac.honeyHeistCode.controllers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
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
import edu.cornell.gdiac.honeyHeistCode.WorldController;
import edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers.AIController;
import edu.cornell.gdiac.honeyHeistCode.models.*;
import edu.cornell.gdiac.honeyHeistCode.obstacle.BoxObstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.Obstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.ObstacleSelector;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class EditorController extends WorldController implements InputProcessor {
    /** Texture asset for mouse crosshairs */
    private TextureRegion crosshairTexture;
    /** The texture for the background */
    protected TextureRegion background;
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
     * Texture asset for chaser bee avatar
     */
    private TextureRegion flyingBeeTexture;
    /**
     * Texture asset for testEnemy avatar
     */
    private TextureRegion sleeperBeeTexture;
    /**
     * Texture asset for tilesBackground
     */
    private TextureRegion tilesBackground;

    /** The texture for spiked platforms */
    protected TextureRegion poisonTile;

    private BitmapFont modeFont;

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

    /** Mouse selector to move the ragdoll */
    private ObstacleSelector selector;

    private Array<Vector2> clickCache;

    private int mode;

    private float platWidth = 0.5f;

    private PolygonShape outline;

    private boolean drawOutline;

    private PolygonObstacle honeypatchPreview;

    private String loadPath = "savedLevel";

    private AssetDirectory directory;

    // Fields for the Editor controller GUI
//    private EditorOverlay overlay;
//    private Stage stage;
    private int buttonNum = 10;

    private Texture antButton;
    private Texture larvaButton;
    private Texture beeButton;
    private Texture honeyPatchButton;
    private Texture platformButton;
    private Texture spikedPlatformButton;
    private Texture goalButton;
    private Texture selectModeButton;
    private Texture saveButton;
    private Texture resetButton;

    private Boolean abPressed = false;  // Ant button
    private Boolean lbPressed = false;  // Larva button
    private Boolean bbPressed = false;  // Bee button
    private Boolean hpbPressed = false; // Honey patch button
    private Boolean pbPressed = false;  // Platform button
    private Boolean spbPressed = false; // Spiked platform button
    private Boolean gbPressed = false;  // Goal button
    private Boolean smbPressed = false; // Select mode button
    private Boolean sbPressed = false;  // Save button
    private Boolean rbPressed = false;  // Reset button

    private static float BUTTON_SCALE  = 0.3f;

    public String getLoadPath(){return loadPath;}

    private float buttonX(){
        return 11 * canvas.getWidth()/12;
    }

    private float smbY(){
        return canvas.getHeight()/(buttonNum + 1) ;
    }

    private float sbY() {return smbY() * 2;}

    private float rbY() {return smbY() * 3;}

    private float spbY() {return smbY() * 4;}

    private float pbY(){
        return smbY() * 5;
    }

    private float hpbY() {return smbY() * 6;}

    private float gbY(){
        return smbY() * 7;
    }

    private float bbY(){
        return smbY() * 8;
    }

    private float lbY() {return smbY() * 9;}

    private float abY(){
        return smbY() * 10;
    }

    private void resetButtons(){
        abPressed = false;
        bbPressed = false;
        pbPressed = false;
        gbPressed = false;
        smbPressed = false;
        sbPressed = false;
        rbPressed = false;
        lbPressed = false;
        hpbPressed = false;
        spbPressed = false;
    }



    /** Filehandler for saving and loading jsons */
    private FileHandle file = Gdx.files.local("savedLevel.json");

    /**
     * Creates and initialize a new instance of the platformer game
     * <p>
     * The game has default gravity and other settings
     */
    public EditorController() {
        super(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_GRAVITY);
        setDebug(false);
        setComplete(false);
        setFailure(false);
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
        this.directory = directory;

        crosshairTexture  = new TextureRegion(directory.getEntry( "shared:crosshair", Texture.class ));
        background = new TextureRegion(directory.getEntry( "shared:background",  Texture.class ));
        avatarTexture = new TextureRegion(directory.getEntry("platform:ant", Texture.class));
        chaserBeeTexture = new TextureRegion(directory.getEntry("platform:larvae", Texture.class));
        flyingBeeTexture = new TextureRegion(directory.getEntry("platform:flyingBee", Texture.class));
        sleeperBeeTexture = new TextureRegion(directory.getEntry("platform:sleeperBee", Texture.class));
        tilesBackground = new TextureRegion(directory.getEntry("shared:tilesBackground", Texture.class));
        poisonTile = new TextureRegion(directory.getEntry( "shared:poisonWall", Texture.class));

        walkingPlayer = directory.getEntry( "platform:walk.pacing", FilmStrip.class );

        jumpSound = directory.getEntry("platform:jump", SoundBuffer.class);
        fireSound = directory.getEntry("platform:pew", SoundBuffer.class);
        plopSound = directory.getEntry("platform:plop", SoundBuffer.class);

        constants = directory.getEntry("platform:constants2", JsonValue.class);
        levelData = directory.getEntry("platform:defaultLevel", JsonValue.class);
        modeFont = directory.getEntry("shared:marker",BitmapFont.class);

        beeButton = directory.getEntry("editor:beeButton", Texture.class);
        antButton = directory.getEntry("editor:antButton", Texture.class);
        larvaButton = directory.getEntry("editor:larvaButton", Texture.class);
        honeyPatchButton = directory.getEntry("editor:honeyPatchButton", Texture.class);
        platformButton = directory.getEntry("editor:platformButton", Texture.class);
        spikedPlatformButton = directory.getEntry("editor:spikedPlatformButton", Texture.class);
        goalButton = directory.getEntry("editor:goalButton", Texture.class);
        selectModeButton = directory.getEntry("editor:selectModeButton", Texture.class);
        resetButton = directory.getEntry("editor:resetButton", Texture.class);
        saveButton = directory.getEntry("editor:saveButton", Texture.class);


        super.gatherAssets(directory);
    }

    public void gatherLevelData(AssetDirectory directory){
        this.directory = directory;
        levelData = directory.getEntry(loadPath, JsonValue.class);
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

        JsonValue defaults = constants.get("defaults");
        //Create background
        PolygonObstacle levelBackground;
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
            //addObject(levelBackground);
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
        PlatformModel platforms = new PlatformModel(levelData.get("platformPos"));
        platforms.setDrawScale(scale);
        platforms.setTexture(earthTile);
        for(PolygonObstacle platform : platforms.getBodies()){
            addObject(platform);
        }

        // Create spiked platforms
        SpikedPlatformModel spikedPlatforms = new SpikedPlatformModel(levelData.get("spikedPlatformPos"));
        spikedPlatforms.setDrawScale(scale);
        spikedPlatforms.setTexture(poisonTile); //TODO: Change spikedPlatform texture
        for(PolygonObstacle spiked : spikedPlatforms.getBodies()){
            addObject(spiked);
        }


        // Create honeypatches
        HoneypatchModel honeyPatches = new HoneypatchModel(levelData.get("honeypatchPos"),0.4f);
        honeyPatches.setDrawScale(scale);
        honeyPatches.setTexture(earthTile); //TODO: Change honeyPatch texture
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
        addObject(avatar);
        avatar.setGravityScale(0);

        // Create chaser bees

        Array<AbstractBeeModel> bees = new Array<AbstractBeeModel>();
        level = new LevelModel(avatar,bees,goalDoor,platforms, spikedPlatforms, honeyPatches, levelBackground, new Rectangle(bounds));

        dwidth = chaserBeeTexture.getRegionWidth() / scale.x;
        dheight = chaserBeeTexture.getRegionHeight() / scale.y;
        //JsonValue.JsonIterator groundedBeeIterator = constants.get("groundedBees").iterator();
        JsonValue groundedBeePositions = levelData.get("groundedBeePos");
        for (int i=0; i<groundedBeePositions.size; i++){
            float[] pos = groundedBeePositions.get(i).asFloatArray();
            ChaserBeeModel chaserBee = new ChaserBeeModel(constants.get("GroundedBee"), pos[0], pos[1], dwidth, dheight);
            chaserBee.setDrawScale(scale);
            chaserBee.setTexture(chaserBeeTexture);
            bees.add(chaserBee);
            addObject(chaserBee);
            chaserBee.setGravityScale(0);
        }

        JsonValue flyingBeePositions = levelData.get("flyingBeePos");
        for (int i=0; i<flyingBeePositions.size; i++){
            float[] pos = flyingBeePositions.get(i).asFloatArray();
            FlyingBeeModel flyingBee = new FlyingBeeModel(constants.get("FlyingBee"), pos[0], pos[1], dwidth, dheight);
            flyingBee.setDrawScale(scale);
            flyingBee.setTexture(flyingBeeTexture);
            bees.add(flyingBee);
            addObject(flyingBee);
            flyingBee.setGravityScale(0);
        }

        //add honeypatches last so that they cover other objects
        //addObject(honeyPatches);
        for(PolygonObstacle honeypatch : honeyPatches.getArrayBodies()){
            addObject(honeypatch);
        }

        clickCache = new Array<Vector2>();
        outline = new PolygonShape();

        selector = new ObstacleSelector(world);
        selector.setTexture(crosshairTexture);
        selector.setDrawScale(scale);

        /*//Background
        PolygonObstacle levelBackground;
        levelBackground = new PolygonObstacle(levelData.get("background").asFloatArray(), 0, 0);
        levelBackground.setBodyType(BodyDef.BodyType.StaticBody);
        levelBackground.setName("background");
        levelBackground.setDrawScale(scale);
        levelBackground.setTexture(tilesBackground);
        levelBackground.setSensor(true);
        addObject(levelBackground);
        level.setLevelBackground(levelBackground);

        //Bees
        level.setBees(new Array<AbstractBeeModel>());

        level.setSpikedPlatforms(new SpikedPlatformModel());
        HoneypatchModel honeypatches = new HoneypatchModel();
        addObject(honeypatches);
        level.setHoneyPatches(honeypatches);

        //Platforms
        PlatformModel platforms = new PlatformModel(levelData.get("platformPos"),"platform");
        platforms.setDrawScale(scale);
        platforms.setTexture(earthTile);
        addObject(platforms);
        level.setPlatforms(platforms);

        clickCache = new Array<Vector2>();
        outline = new PolygonShape();

        selector = new ObstacleSelector(world);
        selector.setTexture(crosshairTexture);
        selector.setDrawScale(scale);*/
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
        InputController input = InputController.getInstance();
        input.readInput(bounds, scale);
        if (listener == null) {
            return true;
        }

        // Toggle debug
//		if (input.didDebug()) {
//			debug = !debug;
//		}

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
            convertToJson();
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
//        if (input.didMode()){
//            mode = (mode+1) % 5;
//            clickCache.clear();
//            drawOutline = false;
//            selector.deselect();
//        }

        // Process the switching of modes by clicking a button
        if(input.didMouseClick() &&
                Math.abs(buttonX() - input.getCrossHair().x * scale.x) <
                        selectModeButton.getWidth()*BUTTON_SCALE/2){
            float clickX = input.getCrossHair().x * scale.x;
            float clickY = input.getCrossHair().y * scale.y;

            if (Math.abs(buttonX() - clickX) < selectModeButton.getWidth()*BUTTON_SCALE/2) {
                resetButtons();
                // ANT BUTTON CLICKED
                if (Math.abs(abY() - clickY) < antButton.getHeight()*BUTTON_SCALE/2){
                    this.abPressed = true;
                    this.mode = 1;
                }
                // LARVA BUTTON CLICKED
                else if (Math.abs(lbY() - clickY) < larvaButton.getHeight()*BUTTON_SCALE/2){
                    this.lbPressed = true;
                    this.mode = 2;
                }
                // BEE BUTTON CLICKED
                else if (Math.abs(bbY() - clickY) < beeButton.getHeight()*BUTTON_SCALE/2){
                    this.bbPressed = true;
                    this.mode = 7;
                }
                // HONEY PATCH BUTTON CLICKED
                else if (Math.abs(hpbY() - clickY) < honeyPatchButton.getHeight()*BUTTON_SCALE/2){
                    this.hpbPressed = true;
                    this.mode = 6;
                }
                // SPIKED PLATFORM BUTTON CLICKED
                else if (Math.abs(spbY() - clickY) < spikedPlatformButton.getHeight()*BUTTON_SCALE/2){
                    this.spbPressed = true;
                    this.mode = 5;
                }
                // PLATFORM BUTTON CLICKED
                else if (Math.abs(pbY() - clickY) < platformButton.getHeight()*BUTTON_SCALE/2){
                    this.pbPressed = true;
                    this.mode = 0;
                }
                // GOAL BUTTON CLICKED
                else if (Math.abs(gbY() - clickY) < goalButton.getHeight()*BUTTON_SCALE/2){
                    this.gbPressed = true;
                    this.mode = 3;
                }
                // RESET BUTTON CLICKED
                else if (Math.abs(rbY() - clickY) < resetButton.getHeight()*BUTTON_SCALE/2){
                    this.rbPressed = true;
                    levelData = directory.getEntry("platform:defaultLevel", JsonValue.class);
                    reset();
                }
                // SAVE BUTTON CLICKED
                else if (Math.abs(sbY() - clickY) < saveButton.getHeight()*BUTTON_SCALE/2){
                    this.sbPressed = true;
                    convertToJson();
                }
                // SELECT MODE BUTTON CLICKED
                else if (Math.abs(smbY() - clickY) < selectModeButton.getHeight()*BUTTON_SCALE/2){
                    this.smbPressed = true;
                    this.mode = 4;
                }
                clickCache.clear();
                drawOutline = false;
                selector.deselect();
            }

//            System.out.println((input.getCrossHair().x * scale.x + ", " + input.getCrossHair().y * scale.y));
//            System.out.println(buttonX() + ", " + beeButton.getHeight());
        }
        else {

            //PLACE PLATFORM MODE
            if (mode == 0) {
                if (input.didMouseClick()) {
                    clickCache.add(new Vector2(input.getCrossHair().x, input.getCrossHair().y));
                    if (clickCache.size >= 2) {
                        snapClick();

                        Vector2 previousClick = clickCache.get(clickCache.size - 2);
                        Vector2 currentClick = clickCache.get(clickCache.size - 1);

                        if (currentClick.x != previousClick.x || currentClick.y != previousClick.y) {
                            newPlatform(rectFromTwoPoints(previousClick, currentClick));
                        }
                        clickCache.clear();
                        drawOutline = false;
                    }
                }
                //if clicked once and in platform mode, update outline
                if (clickCache.size == 1) {
                    Vector2 currentClick = clickCache.get(0);
                    Vector2 nearest = nearestPointAngle(currentClick, input.getCrossHair(), Math.PI / 3);
                    if (currentClick.x != nearest.x || currentClick.y != nearest.y) {
                        outline.set(rectFromTwoPoints(currentClick, nearest));
                        drawOutline = true;
                    }
                }
                if (input.didMouseRightClick()) {
                    clickCache.clear();
                    drawOutline = false;
                }
            }

            //PLACE PLAYER MODE
            if (mode == 1) {
                if (input.didMouseClick()) {
                    clickCache.add(new Vector2(input.getCrossHair().x, input.getCrossHair().y));
                    if (level.getPlayer() == null) {
                        newPlayer();
                    }
                    clickCache.clear();
                }
            }

            //PLACE LARVA MODE
            if (mode == 2) {
                if (input.didMouseClick()) {
                    clickCache.add(new Vector2(input.getCrossHair().x, input.getCrossHair().y));
                    newChaserBee();
                    clickCache.clear();
                }
            }

            //PLACE GOAL MODE
            if (mode == 3) {
                if (input.didMouseClick()) {
                    clickCache.add(new Vector2(input.getCrossHair().x, input.getCrossHair().y));
                    if (level.getGoalDoor() == null) {
                        newGoalDoor();

                        clickCache.clear();
                    }
                }
            }

            //SELECT MODE
            if (mode == 4) {
                //If clicked, reselect
                if (input.didMouseClick()) {
                    selector.deselect();
                    selector.select(input.getCrossHair().x, input.getCrossHair().y);
                }

                if (selector.isSelected()) {

                    //if dragging, move selected
                    if (input.didMouseDrag()) {
                        if (selector.getObstacle().getClass() == PolygonObstacle.class) {
                            PolygonObstacle obj = (PolygonObstacle) selector.getObstacle();
                            Vector2 offset = obj.getCenter(false);
                            float length = (float) Math.sqrt(Math.pow(offset.x, 2) + Math.pow(offset.y, 2));
                            float theta = (float) Math.atan((double) (offset.y / offset.x));
                            float angle = obj.getAngle();
                            offset.x = length * (float) Math.cos(theta + angle);
                            offset.y = length * (float) Math.sin(theta + angle);
                            selector.moveTo(input.getCrossHair().sub(offset));
                        } else {
                            selector.moveTo(input.getCrossHair());
                        }
                    }

                    //rotate only effects polygon obstacles
                    if (input.didRotate()) {
                        if (selector.getObstacle().getClass() == PolygonObstacle.class) {
                            PolygonObstacle obj = (PolygonObstacle) selector.getObstacle();
                            obj.rotateAboutPoint((float) Math.PI / 3, obj.getCenter());
                        }
                    }
                    if (input.didAntiRotate()) {
                        if (selector.getObstacle().getClass() == PolygonObstacle.class) {
                            PolygonObstacle obj = (PolygonObstacle) selector.getObstacle();
                            obj.rotateAboutPoint((float) -Math.PI / 3, obj.getCenter());
                        }
                    }

                    //right click to deselect
                    if (input.didMouseRightClick()) {
                        selector.deselect();
                    }

                    //have to remove objects from level model + mark them to be removed
                    if (input.didDelete() && selector.getObstacle() != null) {
                        if (selector.getObstacle().getName().contains("platform")) {
                            PolygonObstacle obj = (PolygonObstacle) selector.getObstacle();
                            level.getPlatforms().getArrayBodies().removeValue(obj, false);
                        }
                        if (selector.getObstacle().getName().contains("spiked")) {
                            PolygonObstacle obj = (PolygonObstacle) selector.getObstacle();
                            level.getSpikedPlatforms().getArrayBodies().removeValue(obj, false);
                        }
                        if (selector.getObstacle().getName().contains("honeypatch")) {
                            PolygonObstacle obj = (PolygonObstacle) selector.getObstacle();
                            level.getHoneyPatches().getArrayBodies().removeValue(obj, false);
                        }
                        if (selector.getObstacle().getClass() == PlayerModel.class) {
                            level.setPlayer(null);
                        }
                        if (selector.getObstacle().getClass().getSuperclass() == AbstractBeeModel.class) {
                            AbstractBeeModel obj = (AbstractBeeModel) selector.getObstacle();
                            level.getBees().removeValue(obj, false);
                        }
                        if (selector.getObstacle().getName() == "goal") {
                            level.setGoalDoor(null);
                        }
                        selector.getObstacle().markRemoved(true);
                    }
                }
            }

            //PLACE SPIKED PLATFORM MODE
            if (mode == 5){
                if (input.didMouseClick()) {
                    clickCache.add(new Vector2(input.getCrossHair().x, input.getCrossHair().y));
                    if (clickCache.size >= 2) {
                        snapClick();

                        Vector2 previousClick = clickCache.get(clickCache.size - 2);
                        Vector2 currentClick = clickCache.get(clickCache.size - 1);
                        if (currentClick.x != previousClick.x || currentClick.y != previousClick.y) {
                            newSpikedPlatform(rectFromTwoPoints(previousClick, currentClick));
                        }
                        clickCache.clear();
                        drawOutline = false;
                    }
                }
                //if clicked once and in platform mode, update outline
                if (clickCache.size == 1) {
                    Vector2 currentClick = clickCache.get(0);
                    Vector2 nearest = nearestPointAngle(currentClick, input.getCrossHair(), Math.PI / 3);
                    if (currentClick.x != nearest.x || currentClick.y != nearest.y) {
                        outline.set(rectFromTwoPoints(currentClick, nearest));
                        drawOutline = true;
                    }
                }
                if (input.didMouseRightClick()) {
                    clickCache.clear();
                    drawOutline = false;
                }
            }

            //PLACE HONEYPATCH MODE
            if (mode == 6){
                if (input.didMouseClick()) {
                    clickCache.add(new Vector2(input.getCrossHair().x, input.getCrossHair().y));
                }
                //if clicked once and in honeypatch mode, just draw line
                if (clickCache.size == 1) {
                    Vector2 currentClick = clickCache.get(0);
                    if (currentClick.x != input.getCrossHair().x || currentClick.y != input.getCrossHair().y) {
                        outline.set(rectFromTwoPoints(currentClick, input.getCrossHair(),0.01f));
                        drawOutline = true;
                    }
                }
                //if clicked twice or more, draw preview based on mouse pos
                if (clickCache.size >= 2) {
                    Vector2 currentClick = clickCache.get(clickCache.size-1);
                    if (currentClick.x != input.getCrossHair().x || currentClick.y != input.getCrossHair().y) {
                        drawOutline = false;
                        float[] points = getPolyPoints(input.getCrossHair());

                        //clear honeypatch preview
                        if(honeypatchPreview !=null) {
                            level.getHoneyPatches().getArrayBodies().removeValue(honeypatchPreview, false);
                            honeypatchPreview.markRemoved(true);
                        }
                        //set new honeypatch preview
                        honeypatchPreview = newHoneypatch(points);

                        //right click to finalize shape
                        if (input.didMouseRightClick()){
                            clickCache.clear();
                            honeypatchPreview = null;
                        }
                    }
                }
            }

            //PLACE BEE MODE
            if (mode == 7){
                if (input.didMouseClick()) {
                    clickCache.add(new Vector2(input.getCrossHair().x, input.getCrossHair().y));
                    newFlyingBee();
                    clickCache.clear();
                }
            }
        }

        if (input.didSave()){
            convertToJson();
        }

    }

    private void newGoalDoor() {
        float dwidth = goalTile.getRegionWidth() / scale.x;
        float dheight = goalTile.getRegionHeight() / scale.y;

        BoxObstacle goalDoor = new BoxObstacle(clickCache.get(0).x, clickCache.get(0).y,
                dwidth, dheight);

        goalDoor.setBodyType(BodyDef.BodyType.StaticBody);
        goalDoor.setDrawScale(scale);
        goalDoor.setTexture(goalTile);
        goalDoor.setName("goal");
        goalDoor.setSensor(true);
        level.setGoalDoor(goalDoor);
        addObject(goalDoor);
        //goalDoor.setActive(false);
    }

    private void newPlayer() {
        float dwidth = avatarTexture.getRegionWidth() / scale.x;
        float dheight = avatarTexture.getRegionHeight() / scale.y;

        PlayerModel avatar = new PlayerModel(constants.get("player"),
                clickCache.get(0).x, clickCache.get(0).y, dwidth, dheight);

        avatar.setDrawScale(scale);
        avatar.setTexture(avatarTexture);
        avatar.setAnimationStrip(PlayerModel.AntAnimations.WALK, walkingPlayer);
        addObject(avatar);
        //avatar.setActive(false);
        avatar.setGravityScale(0);
        level.setPlayer(avatar);
    }

    private void newPlatform(float[] points) {
        PolygonObstacle obj;
        obj = new PolygonObstacle(points, 0, 0);
        obj.setBodyType(BodyDef.BodyType.StaticBody);
        obj.setDrawScale(scale);
        obj.setTexture(earthTile);
        addObject(obj);
        //obj.setActive(false);
        obj.setName("platform");
        level.getPlatforms().getArrayBodies().add(obj);
    }

    private void newSpikedPlatform(float[] points) {
        PolygonObstacle obj;
        obj = new PolygonObstacle(points, 0, 0);
        obj.setBodyType(BodyDef.BodyType.StaticBody);
        obj.setDrawScale(scale);
        obj.setTexture(poisonTile);
        addObject(obj);
        //obj.setActive(false);
        obj.setName("spiked");
        level.getSpikedPlatforms().getArrayBodies().add(obj);
    }

    private PolygonObstacle newHoneypatch(float[] points) {
        PolygonObstacle obj;
        obj = new PolygonObstacle(points, 0, 0);
        obj.setBodyType(BodyDef.BodyType.StaticBody);
        obj.setDrawScale(scale);
        obj.setTexture(earthTile);
        addObject(obj);
        //obj.setActive(false);
        obj.setName("honeypatch");
        obj.setSensor(true);
        level.getHoneyPatches().getArrayBodies().add(obj);
        return obj;
    }

    private void newChaserBee() {
        float dwidth = chaserBeeTexture.getRegionWidth() / scale.x;
        float dheight = chaserBeeTexture.getRegionHeight() / scale.y;

        ChaserBeeModel chaserBee = new ChaserBeeModel(constants.get("GroundedBee"),
                clickCache.get(0).x, clickCache.get(0).y, dwidth, dheight);

        chaserBee.setDrawScale(scale);
        chaserBee.setTexture(chaserBeeTexture);
        level.getBees().add(chaserBee);
        addObject(chaserBee);
        //chaserBee.setActive(false);
        chaserBee.setGravityScale(0);
    }

    private void newFlyingBee() {
        float dwidth = flyingBeeTexture.getRegionWidth() / scale.x;
        float dheight = flyingBeeTexture.getRegionHeight() / scale.y;

        FlyingBeeModel flyingBee = new FlyingBeeModel(constants.get("FlyingBee"),
                clickCache.get(0).x, clickCache.get(0).y, dwidth, dheight);

        flyingBee.setDrawScale(scale);
        flyingBee.setTexture(flyingBeeTexture);
        level.getBees().add(flyingBee);
        addObject(flyingBee);
        //chaserBee.setActive(false);
        flyingBee.setGravityScale(0);
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
     * Creates a rectangle based on the two points a and b,
     * with a default width or a specified width
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
    private float[] rectFromTwoPoints(Vector2 a, Vector2 b, float width) {
        Vector2 vec = new Vector2(b.x-a.x,b.y-a.y);
        double angle = Math.atan(vec.y/vec.x);
        angle += Math.PI/2;
        Vector2 offset = new Vector2((float)Math.cos(angle)*width/2,(float)Math.sin(angle)*width/2);
        float[] result = {
                a.x+offset.x,a.y+offset.y,
                a.x-offset.x,a.y-offset.y,
                b.x-offset.x,b.y-offset.y,
                b.x+offset.x,b.y+offset.y,
        };
        return result;
    }

    /**
     * returns an array of floats based on the click cache and the current cursor position
     * that can be used to construct a polygon.
     *
     * @param cursor    the current cursor position
     * @return
     */
    private float[] getPolyPoints(Vector2 cursor) {
        int size = clickCache.size*2+2;
        float[] points = new float[size];
        for(int i = 0; i<clickCache.size*2; i+=2){
            Vector2 current = clickCache.get(i/2);
            points[i] = current.x;
            points[i+1] = current.y;
        }
        points[size-2] = cursor.x;
        points[size-1] = cursor.y;
        return points;
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
        level.getLevelBackground().draw(canvas);
        for(Obstacle obj : objects) {
            if(obj.getClass() == PolygonObstacle.class) {
                if (!obj.getName().contains("honeypatch")) {
                    obj.draw(canvas);
                } else{
                    Color tint = Color.ORANGE;
                    tint.set(tint.r,tint.g,tint.b,0.7f);
                    ((PolygonObstacle) obj).draw(canvas,tint);
                }
            }else{
                obj.draw(canvas);
            }
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
        if(mode == 2){modeText += "Place Larva";}
        if(mode == 3){modeText += "Place Goal Door";}
        if(mode == 4){modeText += "Select";}
        if(mode == 5){modeText += "Place Spiked Platform";}
        if(mode == 6){modeText += "Place Honeypatch";}
        if(mode == 7){modeText += "Place Bee";}

        // Draw mode text
        canvas.begin();
        modeFont.setColor(Color.WHITE);
        canvas.drawTextCentered(modeText,modeFont, -canvas.getWidth()/3f);
        canvas.end();

        // Draw platform outline
        if(drawOutline){
            canvas.beginDebug();
            //System.out.print("drawline");
            canvas.drawPhysics(outline, Color.RED, 0, 0, 0, scale.x, scale.y);
            canvas.endDebug();
        }

        // Draw selected highlight
        for(Obstacle obj : objects) {
            if(obj == selector.getObstacle()){
                canvas.beginDebug();
                obj.drawDebug(canvas);
                canvas.endDebug();
            }
        }

        // Draw UI Buttons
        float BUTTON_X  = this.buttonX();
        canvas.begin();
        // First button
        canvas.draw(beeButton, bbPressed ? Color.GRAY : Color.WHITE, beeButton.getWidth() / 2,
                beeButton.getHeight() / 2, BUTTON_X, bbY(), 0,
                BUTTON_SCALE, BUTTON_SCALE);
        // Second Button
        canvas.draw(antButton,  abPressed ? Color.GRAY : Color.WHITE, antButton.getWidth() / 2,
                antButton.getHeight() / 2, BUTTON_X, abY(), 0,
                BUTTON_SCALE, BUTTON_SCALE);
        // Third Button
        canvas.draw(platformButton,  pbPressed ? Color.GRAY : Color.WHITE, platformButton.getWidth() / 2,
                platformButton.getHeight() / 2, BUTTON_X, pbY(), 0,
                BUTTON_SCALE, BUTTON_SCALE);
        // Fourth Button
        canvas.draw(goalButton,  gbPressed ? Color.GRAY : Color.WHITE, goalButton.getWidth() / 2,
                goalButton.getHeight() / 2, BUTTON_X, gbY(), 0,
                BUTTON_SCALE, BUTTON_SCALE);
        // Fifth Button
        canvas.draw(selectModeButton,  smbPressed ? Color.GRAY : Color.WHITE, selectModeButton.getWidth() / 2,
                selectModeButton.getHeight() / 2, BUTTON_X, smbY(), 0,
                BUTTON_SCALE, BUTTON_SCALE);
        canvas.draw(resetButton,  rbPressed ? Color.GRAY : Color.WHITE, resetButton.getWidth() / 2,
                resetButton.getHeight() / 2, BUTTON_X, rbY(), 0,
                BUTTON_SCALE, BUTTON_SCALE);
        canvas.draw(saveButton,  sbPressed ? Color.GRAY : Color.WHITE, saveButton.getWidth() / 2,
                saveButton.getHeight() / 2, BUTTON_X, sbY(), 0,
                BUTTON_SCALE, BUTTON_SCALE);
        canvas.draw(larvaButton,  lbPressed ? Color.GRAY : Color.WHITE, larvaButton.getWidth() / 2,
                larvaButton.getHeight() / 2, BUTTON_X, lbY(), 0,
                BUTTON_SCALE, BUTTON_SCALE);
        canvas.draw(spikedPlatformButton,  spbPressed ? Color.GRAY : Color.WHITE, spikedPlatformButton.getWidth() / 2,
                larvaButton.getHeight() / 2, BUTTON_X, spbY(), 0, BUTTON_SCALE, BUTTON_SCALE);
        canvas.draw(honeyPatchButton,  hpbPressed ? Color.GRAY : Color.WHITE, honeyPatchButton.getWidth() / 2,
                honeyPatchButton.getHeight() / 2, BUTTON_X, hpbY(), 0, BUTTON_SCALE, BUTTON_SCALE);
        canvas.end();


//        // Final message
//        if (complete && !failed) {
//            displayFont.setColor(Color.YELLOW);
//            canvas.begin(); // DO NOT SCALE
//            canvas.drawTextCentered("VICTORY!", displayFont, 0.0f);
//            canvas.end();
//        } else if (failed) {
//            displayFont.setColor(Color.RED);
//            canvas.begin(); // DO NOT SCALE
//            canvas.drawTextCentered("FAILURE!", displayFont, 0.0f);
//            canvas.end();
//        }
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
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    // Not supported
    @Override
    public boolean keyDown(int keycode) {
        return true;
    }
    // Not supported
    @Override
    public boolean keyUp(int keycode) {
        return true;
    }
    // Not supported
    @Override
    public boolean keyTyped(char character) {
        return true;
    }
    // Not supported
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return true;
    }
    // Not supported
    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return true;
    }
    // Not supported
    @Override
    public boolean scrolled(float amountX, float amountY) {
        return true;
    }

    public class Level{
        public float[] goalPos;
        public float[] playerPos;
        public float[][] groundedBeePos;
        public float[][] flyingBeePos;
        public float[][] platformPos;
        public float[][] spikedPlatformPos;
        public float[][] honeyPatchPos;
        public float[] background;

        public Level(){

        }

        public void setGoal(float[] goalPos) { this.goalPos = goalPos; }
        public void setPlayer(float[] playerPos) { this.playerPos = playerPos; }
        public void setLarva(float[][] larvaPos) { this.groundedBeePos = larvaPos; }
        public void setBee(float[][] beePos) { this.flyingBeePos = beePos; }
        public void setPlatform(float[][] platformPos) { this.platformPos = platformPos; }
        public void setSpikedPlatform(float[][] spikedPlatformPos) {this.spikedPlatformPos = spikedPlatformPos; }
        public void setHoneyPatch(float[][] honeyPatchPos) { this.honeyPatchPos = honeyPatchPos; }
        public void setBackground(float[] backgroundPos) { this.background = backgroundPos; }

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
            Array<AbstractBeeModel> larva = new Array<>();
            Array<AbstractBeeModel> flyingBees = new Array<>();
            for (AbstractBeeModel bee : bees){
                if (bee.getClass() == ChaserBeeModel.class){
                    larva.add(bee);
                } else {
                    flyingBees.add(bee);
                }
            }
            float[][] larvaArray = new float[larva.size][2];
            float[][] beeArray = new float[flyingBees.size][2];
            for (int i=0; i<larvaArray.length; i++) {
                Vector2 larvaPos = larva.get(i).getPosition();
                larvaArray[i][0] = larvaPos.x;
                larvaArray[i][1] = larvaPos.y;
            }
            for (int i=0; i<beeArray.length; i++){
                Vector2 beePos = flyingBees.get(i).getPosition();
                beeArray[i][0] = beePos.x;
                beeArray[i][1] = beePos.y;
            }
            jsonLevel.setLarva(larvaArray);
            jsonLevel.setBee(beeArray);
        }

        if (level.getLevelBackground()!=null){
            jsonLevel.setBackground(level.getLevelBackground().getTruePoints());
        }

        if (level.getPlatforms()!=null){
            jsonLevel.setPlatform(getPlatforms(0));
        }

        if (level.getSpikedPlatforms().getArrayBodies().size > 0){
            jsonLevel.setSpikedPlatform(getPlatforms(1));
        }

        if (level.getHoneyPatches().getArrayBodies().size > 0){
            jsonLevel.setHoneyPatch(getPlatforms(2));
        }


        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        file.writeString(json.prettyPrint(jsonLevel), false);
        System.out.println("saved");
    }

    private float[][] getPlatforms(int platformType){
        Array<PolygonObstacle> platforms;
        switch (platformType){
            case 0: platforms = level.getPlatforms().getArrayBodies(); break;
            case 1: platforms = level.getSpikedPlatforms().getArrayBodies(); break;
            case 2: platforms = level.getHoneyPatches().getArrayBodies(); break;
            default: platforms = new Array<>();
        }
        float[][] platformArray = new float[platforms.size][platforms.get(0).getTruePoints().length];
        for (int i=0; i<platformArray.length; i++){
            platformArray[i] = platforms.get(i).getTruePoints();
        }
        return platformArray;
    }

    // Methods for the GUI

}
