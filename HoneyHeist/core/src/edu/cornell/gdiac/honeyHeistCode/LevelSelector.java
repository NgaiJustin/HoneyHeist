package edu.cornell.gdiac.honeyHeistCode;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.*;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.audio.SoundBuffer;
import edu.cornell.gdiac.util.*;

public class LevelSelector implements Screen {
    /** Internal assets for this loading screen */
    private AssetDirectory internal;
    /** The actual assets to be loaded */
    private AssetDirectory assets;

    /** Background texture for start-up */
    private Texture background;
    private TextureRegion backgroundRegion;
    /** Title texture */
    private Texture title;
    /** selected level number */
    private int currentLevelNum;
    /** number of total levels */
    private int totalLevelNum;
    /** number of levels per page */
    private int LEVEL_PER_PAGE = 7;
    /** number of levels per row */
    private int LEVEL_PER_ROW = 4;
    /** current page. Initially it's 0. */
    private int currentPage;
    /** JsonValue data for all level data */
    private JsonValue allLevelData;
    /** The String that tells the file path of selected level data */
    private String selectedLevelData;
    /** level buttons, should have the size specified by allLevelData */
    private TextButton[] levelButtons;
    /** Exit code for going to the playing level */
    public static final int EXIT_PLAY = 0;
    /** Exit code for going to the editor */
    public static final int EXIT_EDITOR = 1;
    /** Exit code for exit the game */
    public static final int EXIT_QUIT = 2;

    /** Standard window size (for scaling) */
    private static int STANDARD_WIDTH  = 800;
    /** Standard window height (for scaling) */
    private static int STANDARD_HEIGHT = 700;

    /** Reference to GameCanvas created by the root */
    private GameCanvas canvas;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;

    /** The y-coordinate of the center of the progress bar */
    private int centerY;
    /** The x-coordinate of the center of the progress bar */
    private int centerX;
    /** The height of the canvas window (necessary since sprite origin != screen origin) */
    private int heightY;
    /** Scaling factor for when the student changes the resolution. */
    private float scale;

    /** The current state of the play button; it should be one bigger than the button index. */
    private int   pressState;

    /** Whether or not this player mode is still active */
    private boolean active;

    /** The constants */
    private JsonValue constants;

    private SoundBuffer menuBgm;
    private long menuBgmId = 1;

    private float volume = 1f;

    // Scene2d
    private Stage stage;
    private Skin skin;
    private Table table;
    // Whether the level buttons are pressed.
    private boolean[] pressStates;
    private boolean isPressLevelEditor;
    private boolean isQuit;
    // magic numbers for widgets
    private int EDITOR_WIDTH = 150;
    private int EDITOR_HEIGHT = 70;
    private int EDITOR_POSX = 100;
    // left/right arrow button
    private Button leftArrow;
    private Button rightArrow;
    private ScrollPane scroller;

    /**
     * Returns the press state.
     *
     * @return the current press state.
     */
    public int getPressState() {
        return pressState;
    }

    /**
     * Returns the selected level number.
     *
     * @return the selected level number.
     */
    public int getCurrentLevelNum() {
        return currentLevelNum;
    }

    /**
     * Returns the selected level data.
     *
     * @return the selected level data.
     */
    public String getLevelData() {
        return selectedLevelData;
    }

    public JsonValue getAllLevelData() {
        return allLevelData;
    }

    /**
     * Returns true if all assets are loaded and the player is ready to go.
     *
     * @return true if the player is ready to go
     */
    public boolean isReady() {
        return pressState == 0;
    }

    /**
     * Returns the asset directory produced by this loading screen
     *
     * This asset loader is NOT owned by this loading scene, so it persists even
     * after the scene is disposed.  It is your responsbility to unload the
     * assets in this directory.
     *
     * @return the asset directory produced by this loading screen
     */
    public AssetDirectory getAssets() {
        return assets;
    }

    /**
     * Creates a LoadingMode with the default size and position.
     *
     * The budget is the number of milliseconds to spend loading assets each animation
     * frame.  This allows you to do something other than load assets.  An animation
     * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to
     * do something else.  This is how game companies animate their loading screens.
     *
     * @param directory  	The asset directory to load in the background
     * @param canvas 	The game canvas to draw to
     */
    public LevelSelector(AssetDirectory directory, GameCanvas canvas, int currentLevelNum) {
        this.canvas  = canvas;

        // Compute the dimensions from the canvas
        resize(canvas.getWidth(),canvas.getHeight());

        // We need these files loaded immediately
        internal = new AssetDirectory( "levelSelector.json" );
        internal.loadAssets();
        internal.finishLoading();

        // get the level data
        allLevelData = internal.getEntry("levelData", JsonValue.class).get("levels");
        totalLevelNum = allLevelData.size;

        background = internal.getEntry( "background", Texture.class );
        background.setFilter( TextureFilter.Linear, TextureFilter.Linear );
        title = internal.getEntry("title", Texture.class);

        // No progress so far.
        pressState = 0;
        isPressLevelEditor = false;
        isQuit = false;
        this.currentLevelNum = currentLevelNum;
        currentPage = this.currentLevelNum == 0? 0 : (currentLevelNum-1)/LEVEL_PER_PAGE;
        System.out.println("currentPage: " + currentPage);

        // Start loading the real assets
        assets = directory;
        active = true;

        pressStates = new boolean[totalLevelNum];
        // stage
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        // root table
        table = new Table();
        table.align(Align.top);
        // size the root table to the stage (but should generally only be used on the root table)
        table.setFillParent(true);
        stage.addActor(table);
        // turn on debug lines to visualize the layout
//        table.setDebug(true);
        // add assets to skin
        skin = new Skin();
        skin.add("background", background);
        skin.add("title", title);
        skin.add("font", internal.getEntry("times", BitmapFont.class));
        skin.add("levelEditor", internal.getEntry("levelEditor", Texture.class));
        // set background
        table.setBackground(skin.getDrawable("background"));

//        // *** levelEditor button *** //
        TextureRegion levelEditorImage = new TextureRegion(internal.getEntry("levelEditor", Texture.class));
        TextureRegionDrawable levelEditorDrawable = new TextureRegionDrawable(levelEditorImage);
        TextButtonStyle levelEditorStyle = new TextButtonStyle();
//        levelEditorStyle.up = levelEditorDrawable;
//        levelEditorStyle.down = levelEditorDrawable.tint(Color.GRAY);
//        levelEditorStyle.font = skin.getFont("font");
//        TextButton levelEditor = new TextButton("Edit", levelEditorStyle);
//        // change the size
//        levelEditor.invalidate();
//        levelEditor.setSize(EDITOR_WIDTH, EDITOR_HEIGHT);
//        levelEditor.setPosition(stage.getWidth()*0.05f, stage.getHeight()*0.8f);
//        levelEditor.validate();
//        // add listen to pressing event
//        levelEditor.addListener(new ChangeListener() {
//            public void changed(ChangeEvent event, Actor actor) {
//                isPressLevelEditor = true;
//            }
//        });
//        stage.addActor(levelEditor);

        // *** quit button *** //
        TextButtonStyle quitStyle = new TextButtonStyle();
        quitStyle.up = levelEditorDrawable;
        quitStyle.down = levelEditorDrawable.tint(Color.GRAY);
        quitStyle.font = skin.getFont("font");
        TextButton quitButton = new TextButton("Quit", quitStyle);
        // change the size
        quitButton.invalidate();
        quitButton.setSize(EDITOR_WIDTH, EDITOR_HEIGHT);
//        quitButton.setPosition(stage.getWidth()*0.9f-EDITOR_WIDTH, stage.getHeight()*0.6f);
        quitButton.setPosition(stage.getWidth()*0.95f-EDITOR_WIDTH, stage.getHeight()*0.8f);
        quitButton.validate();
        // add listen to pressing event
        quitButton.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                isQuit = true;
            }
        });
        stage.addActor(quitButton);

        // *** title *** //
        Image titleImage = new Image(title);
        // adjust the position of titleCell
        Cell titleCell = table.add(titleImage).colspan(2).
                height(Value.percentHeight(2f)).width(Value.percentWidth(2f));
        titleCell.padTop(stage.getHeight()/8).padBottom(stage.getHeight()/32);

        // *** level buttons *** //
        table.row();
        Table scrollTable = new Table();

        // level buttons
        TextureRegion buttonImage = new TextureRegion(internal.getEntry("unlock_button", Texture.class));
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonImage);
        TextButtonStyle buttonStyle = new TextButtonStyle();
//        buttonStyle.unpressedOffsetY=-10.0f;
//        buttonStyle.pressedOffsetY=-10.0f;
//        buttonStyle.checkedOffsetY=-10.0f;
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable.tint(Color.GRAY);
        buttonStyle.font = skin.getFont("font");
        levelButtons = new TextButton[totalLevelNum];

        TextureRegion lockButtonImage = new TextureRegion(internal.getEntry("lock_button", Texture.class));
        TextureRegionDrawable lockButtonDrawable = new TextureRegionDrawable(lockButtonImage);
        TextButtonStyle lockButtonStyle = new TextButtonStyle();
        lockButtonStyle.unpressedOffsetY=-10.0f;
        lockButtonStyle.pressedOffsetY=-10.0f;
        lockButtonStyle.checkedOffsetY=-10.0f;
        lockButtonStyle.up = lockButtonDrawable;
        lockButtonStyle.down = lockButtonDrawable.tint(Color.GRAY);
        lockButtonStyle.font = skin.getFont("font");

//        TextureRegion completeButtonImage = new TextureRegion(internal.getEntry("complete_button", Texture.class));
//        TextureRegionDrawable completeButtonDrawable = new TextureRegionDrawable(completeButtonImage);
//        TextButtonStyle completeButtonStyle = new TextButtonStyle();
//        completeButtonStyle.unpressedOffsetY=-10.0f;
//        completeButtonStyle.pressedOffsetY=-10.0f;
//        completeButtonStyle.checkedOffsetY=-10.0f;
//        completeButtonStyle.up = completeButtonDrawable;
//        completeButtonStyle.down = completeButtonDrawable.tint(Color.GRAY);
//        completeButtonStyle.font = skin.getFont("font");

        // create the button and scroll page
        int numberOfPage = totalLevelNum/LEVEL_PER_PAGE;
        for (int idx=0; idx <= numberOfPage; idx++) {
            Table page = new Table();
            Table levelTable = new Table();
            // implementation 1
            boolean isUnlock;
            boolean isComplete;
            for (int i = 0; i < totalLevelNum; i++) {
                isUnlock = allLevelData.get(i).get("unlock").asBoolean();
                isComplete = allLevelData.get(i).get("complete").asBoolean();
                // first line has one more level button than the second line
                if (i % (LEVEL_PER_ROW * 2 - 1) == LEVEL_PER_ROW && i / LEVEL_PER_PAGE == idx) {
                    page.row();
                    levelTable = new Table();
                    page.add(levelTable).colspan(2).pad(10f);
                } else if (i % (LEVEL_PER_ROW * 2 - 1) == 0 && i / LEVEL_PER_PAGE == idx) {
                    page.row();
                    levelTable = new Table();
                    page.add(levelTable).colspan(2);
                }
                // create buttons for this specific page
                if (i / LEVEL_PER_PAGE == idx) {
//                    if (isComplete) {
//                        levelButtons[i] = new TextButton(String.valueOf(i+1), completeButtonStyle);
//                    } else
                        if (isUnlock) {
                        levelButtons[i] = new TextButton(String.valueOf(i + 1), buttonStyle);
                    } else {
                        levelButtons[i] = new TextButton(String.valueOf(i + 1), lockButtonStyle);
                    }
                    // finalI is used for inner class
                    final int finalI = i;
                    levelButtons[i].addListener(new ChangeListener() {
                        public void changed(ChangeEvent event, Actor actor) {
                            // "the button can only be pressed if the level is unlocked"
                            if (finalI < totalLevelNum && allLevelData.get(finalI).get("unlock").asBoolean()) {
                                pressState = finalI + 1;
                                selectedLevelData = allLevelData.get(pressState - 1).get("file").asString();
                            }
                        }
                    });
                    levelTable.add(levelButtons[i]).height(100).width(120).
                            padLeft(5f).padRight(5f);
                }
            }
            scrollTable.add(page).width(stage.getWidth()*0.6f);
        }
        scroller = new ScrollPane(scrollTable);
        table.add(scroller).size(stage.getWidth()*0.6f, stage.getHeight()*0.5f).colspan(2);

        // *** left & right buttons *** //
        table.row();
        TextureRegion leftArrowImage = new TextureRegion(internal.getEntry("left", Texture.class));
        TextureRegionDrawable leftArrowDrawable = new TextureRegionDrawable(leftArrowImage);
        TextButtonStyle leftArrowStyle = new TextButtonStyle();
        leftArrowStyle.up = leftArrowDrawable;
        leftArrowStyle.down = leftArrowDrawable.tint(Color.GRAY);
        leftArrowStyle.font = skin.getFont("font");
        TextureRegion rightArrowImage = new TextureRegion(internal.getEntry("right", Texture.class));
        TextureRegionDrawable rightArrowDrawable = new TextureRegionDrawable(rightArrowImage);
        TextButtonStyle rightArrowStyle = new TextButtonStyle();
        rightArrowStyle.up = rightArrowDrawable;
        rightArrowStyle.down = rightArrowDrawable.tint(Color.GRAY);
        rightArrowStyle.font = skin.getFont("font");
        leftArrow = new Button(leftArrowStyle);
        final float pageWidth = stage.getWidth()*0.6f;
        scroller.scrollTo(pageWidth*currentPage, scroller.getHeight(), pageWidth, scroller.getHeight());
        leftArrow.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                if (currentPage > 0) currentPage --;
                scroller.scrollTo(pageWidth*currentPage, scroller.getHeight(), pageWidth, scroller.getHeight());
            }
        });
        table.add(leftArrow).left().height(Value.percentHeight(3f)).width(Value.percentWidth(3f)).top();
        rightArrow = new Button(rightArrowStyle);
        rightArrow.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                if (currentPage < totalLevelNum/LEVEL_PER_PAGE) currentPage ++;
                scroller.scrollTo(pageWidth*currentPage, scroller.getHeight(), pageWidth, scroller.getHeight());
            }
        });
        table.add(rightArrow).right().height(Value.percentHeight(3f)).width(Value.percentWidth(3f)).top();
        constants = internal.getEntry("constants", JsonValue.class);
        volume = constants.get("defaults").getFloat("volume");
        menuBgm = directory.getEntry("audio:soundtrack_mainmenu", SoundBuffer.class);
        menuBgmId = loopSound(menuBgm, menuBgmId, volume);
    }

    public long loopSound(SoundBuffer sound, long soundId, float vol) {
        if (soundId != -1 && sound.isPlaying( soundId )) {
            sound.stop( soundId );
        }
        return sound.loop(vol);
    }

    public void stopAllSounds(){
        menuBgm.stop(menuBgmId);
    }

    /**
     * Called when this screen should release all resources.
     */
    public void dispose() {
        internal.unloadAssets();
        internal.dispose();

        if (stage != null) {
            stage.dispose();
        }
    }

    /**
     * Called when this the current level is completed and go to the next level.
     */
    public String nextLevelData() {
        if (currentLevelNum < totalLevelNum) {
            currentLevelNum ++;
        }
        return allLevelData.get(currentLevelNum - 1).get("file").asString();
    }

    /**
     * Draw the status of this player mode.
     *
     * We prefer to separate update and draw from one another as separate methods, instead
     * of using the single render() method that LibGDX does.  We will talk about why we
     * prefer this in lecture.
     */
    private void draw() {
        canvas.begin();
        canvas.end();
    }

    // ADDITIONAL SCREEN METHODS
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
            Gdx.gl.glClearColor(1, 1, 1, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            stage.act();
            stage.draw();
//            if (isPressLevelEditor) {
//                listener.exitScreen(this, EXIT_EDITOR);
//                return;
//            }
            if (pressState != 0) {
                currentLevelNum = pressState;
                listener.exitScreen(this, EXIT_PLAY);
                return;
            }
            if (isQuit) {
                listener.exitScreen(this, EXIT_QUIT);
                return;
            }
        }
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
        // Compute the drawing scale
        float sx = ((float)width)/STANDARD_WIDTH;
        float sy = ((float)height)/STANDARD_HEIGHT;
        scale = (sx < sy ? sx : sy);

        centerY = height/2;
        centerX = width/2;
        heightY = height;
//        stage.getViewport().update(width,height,true);
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
}
