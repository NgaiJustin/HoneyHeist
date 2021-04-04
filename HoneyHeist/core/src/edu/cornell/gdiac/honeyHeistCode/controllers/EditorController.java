package edu.cornell.gdiac.honeyHeistCode.controllers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundBuffer;
import edu.cornell.gdiac.honeyHeistCode.GameplayController;
import edu.cornell.gdiac.honeyHeistCode.models.*;
import edu.cornell.gdiac.honeyHeistCode.obstacle.BoxObstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.Obstacle;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class EditorController extends GameplayController {
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

    private PolygonShape line;

    private boolean drawLine;

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
        setComplete(false);
        setFailure(false);
        populateLevel();
        drawLine = false;
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
        line = new PolygonShape();
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
            drawLine = false;
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
                    drawLine = false;
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

        //if clicked once and in platform mode, update outline
        if (mode == 0 && clickCache.size==1){
            Vector2 currentClick = clickCache.get(0);
            Vector2 nearest = nearestPointAngle(currentClick,input.getCrossHair(),Math.PI/3);
            if(currentClick.x != nearest.x || currentClick.y != nearest.y){
                line.set(rectFromTwoPoints(currentClick, nearest));
                drawLine = true;
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
        if(drawLine){
            canvas.beginDebug();
            //System.out.print("drawline");
            canvas.drawPhysics(line,Color.RED,0,0,0,scale.x,scale.y);
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
