package edu.cornell.gdiac.honeyHeistCode;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.ControllerMapping;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.util.*;

import java.util.Arrays;

public class LevelSelector implements Screen {
    /** Internal assets for this loading screen */
    private AssetDirectory internal;
    /** The actual assets to be loaded */
    private AssetDirectory assets;

    /** Background texture for start-up */
    private Texture background;
    private TextureRegion backgroundRegion;
    /** levelEditor button */
    private Texture levelEditor;
    /** is levelEditor button pressed */
    private boolean pressLevelEditor;
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
    /** The font for numbers of level displayed */
    private BitmapFont displayFont;
    /** Offset for the number message on the screen */
    private static final float COUNTER_OFFSET   = 10.0f;
    /** JsonValue data for all level data */
    private JsonValue allLevelData;
    /** The String that tells the file path of selected level data */
    private String selectedLevelData;
    /** level buttons, should have the size specified by allLevelData */
    private Texture[] buttons;
    private TextButton[] levelButtons;
    /** Exit code for going to the playing level */
    public static final int EXIT_PLAY = 0;
    /** Exit code for going to the editor */
    public static final int EXIT_EDITOR = 1;
    /** Exit code for exit the game */
    public static final int EXIT_QUIT = 2;
    private static final float Y_OFFSET = 50.0f;
    private static final float TITLE_OFFSET = 100.0f;

    /** Default budget for asset loader (do nothing but load 60 fps) */
    private static int DEFAULT_BUDGET = 15;
    /** Standard window size (for scaling) */
    private static int STANDARD_WIDTH  = 800;
    /** Standard window height (for scaling) */
    private static int STANDARD_HEIGHT = 700;
//    /** Ratio of the bar width to the screen */
    private static float BAR_WIDTH_RATIO  = 0.66f;
//    /** Ration of the bar height to the screen */
    private static float BAR_HEIGHT_RATIO = 0.25f;
    /** Height of the progress bar */
    private static float BUTTON_SCALE  = 1f;

    private static float LEVELSELECT_SCALE  = 0.5f;
    private static float TITLE_SCALE  = 2.5f;

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

    /** Current progress (0 to 1) of the asset manager */
    private float progress;
    /** The current state of the play button; it should be one bigger than the button index. */
    private int   pressState;
    /** The amount of time to devote to loading assets (as opposed to on screen hints, etc.) */
    private int   budget;

    /** Whether or not this player mode is still active */
    private boolean active;

    // Scene2d
    private Stage stage;
    private Skin skin;
    private Table table;
//    private int LEVEL_PER_ROW = 6;
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
     * Returns the budget for the asset loader.
     *
     * The budget is the number of milliseconds to spend loading assets each animation
     * frame.  This allows you to do something other than load assets.  An animation
     * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to
     * do something else.  This is how game companies animate their loading screens.
     *
     * @return the budget in milliseconds
     */
    public int getBudget() {
        return budget;
    }

    /**
     * Sets the budget for the asset loader.
     *
     * The budget is the number of milliseconds to spend loading assets each animation
     * frame.  This allows you to do something other than load assets.  An animation
     * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to
     * do something else.  This is how game companies animate their loading screens.
     *
     * @param millis the budget in milliseconds
     */
    public void setBudget(int millis) {
        budget = millis;
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
     * @param millis The loading budget in milliseconds
     */
    public LevelSelector(AssetDirectory directory, GameCanvas canvas, int millis, int currentLevelNum) {
        this.canvas  = canvas;
        budget = millis;

        // Compute the dimensions from the canvas
        resize(canvas.getWidth(),canvas.getHeight());

        // We need these files loaded immediately
        internal = new AssetDirectory( "levelSelector.json" );
        internal.loadAssets();
        internal.finishLoading();

        // get the level data
        allLevelData = internal.getEntry("levelData", JsonValue.class).get("levels");
        totalLevelNum = allLevelData.size;

        buttons = new Texture[totalLevelNum];
        // initailize the buttons to null
        Arrays.fill(buttons, null);

        background = internal.getEntry( "background", Texture.class );
        background.setFilter( TextureFilter.Linear, TextureFilter.Linear );
        title = internal.getEntry("title", Texture.class);
        displayFont = internal.getEntry("times",BitmapFont.class);
        // new
        levelEditor = internal.getEntry("levelEditor", Texture.class);
        pressLevelEditor = false;

        // No progress so far.
        progress = 0;
        pressState = 0;
        isPressLevelEditor = false;
        isQuit = false;
        this.currentLevelNum = currentLevelNum;
        currentPage = this.currentLevelNum == 0? 0 : currentLevelNum/LEVEL_PER_PAGE;
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
//        skin.add("levelButton", internal.getEntry("button", Texture.class));
        skin.add("font", internal.getEntry("times", BitmapFont.class));
        skin.add("levelEditor", internal.getEntry("levelEditor", Texture.class));
//        skin.add("lockedLevelButton", internal.getEntry("lockbutton", Texture.class));
//        skin.add
        // set background
        table.setBackground(skin.getDrawable("background"));

        // *** levelEditor button *** //
        TextureRegion levelEditorImage = new TextureRegion(internal.getEntry("levelEditor", Texture.class));
        TextureRegionDrawable levelEditorDrawable = new TextureRegionDrawable(levelEditorImage);
        TextButtonStyle levelEditorStyle = new TextButtonStyle();
        levelEditorStyle.up = levelEditorDrawable;
        levelEditorStyle.down = levelEditorDrawable.tint(Color.GRAY);
        levelEditorStyle.font = skin.getFont("font");
        TextButton levelEditor = new TextButton("Edit", levelEditorStyle);
        // change the size
        levelEditor.invalidate();
        levelEditor.setSize(EDITOR_WIDTH, EDITOR_HEIGHT);
        levelEditor.setPosition(stage.getWidth()*0.1f, stage.getHeight()*0.6f);
        levelEditor.validate();
        // add listen to pressing event
        levelEditor.addListener(new ChangeListener() {
            public void changed(ChangeEvent event, Actor actor) {
                isPressLevelEditor = true;
            }
        });
        stage.addActor(levelEditor);

        // *** quit button *** //
        TextButtonStyle quitStyle = new TextButtonStyle();
        quitStyle.up = levelEditorDrawable;
        quitStyle.down = levelEditorDrawable.tint(Color.GRAY);
        quitStyle.font = skin.getFont("font");
        TextButton quitButton = new TextButton("Quit", levelEditorStyle);
        // change the size
        quitButton.invalidate();
        quitButton.setSize(EDITOR_WIDTH, EDITOR_HEIGHT);
        quitButton.setPosition(stage.getWidth()*0.9f-EDITOR_WIDTH, stage.getHeight()*0.6f);
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
        buttonStyle.unpressedOffsetY=-10.0f;
        buttonStyle.pressedOffsetY=-10.0f;
        buttonStyle.checkedOffsetY=-10.0f;
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

        TextureRegion completeButtonImage = new TextureRegion(internal.getEntry("complete_button", Texture.class));
        TextureRegionDrawable completeButtonDrawable = new TextureRegionDrawable(completeButtonImage);
        TextButtonStyle completeButtonStyle = new TextButtonStyle();
        completeButtonStyle.unpressedOffsetY=-10.0f;
        completeButtonStyle.pressedOffsetY=-10.0f;
        completeButtonStyle.checkedOffsetY=-10.0f;
        completeButtonStyle.up = completeButtonDrawable;
        completeButtonStyle.down = completeButtonDrawable.tint(Color.GRAY);
        completeButtonStyle.font = skin.getFont("font");

        int numberOfPage = totalLevelNum/LEVEL_PER_PAGE;
//        System.out.println("total page number = " + numberOfPage);
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
                    if (isComplete) {
                        levelButtons[i] = new TextButton(String.valueOf(i+1), completeButtonStyle);
                    } else if (isUnlock) {
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

                // implementation 2
//            levelButtons[i] = new TextButton(String.valueOf(i+1), buttonStyle);
//            table.add(levelButtons[i]).height(Value.percentHeight(1.2f)).width(Value.percentWidth(1.25f)).
//                    padLeft(20f).padRight(20f);
//            levelTable.add(levelButtons[i]).fillX().fillY();
                // 4 levels per row
//            if (i%LEVEL_PER_ROW == (LEVEL_PER_ROW-1)) {
//                if (i/LEVEL_PER_ROW%2 == 0) {
////                    table.row().spaceLeft(50f);
//                    table.row().right();
//                } else {
////                    table.row().spaceRight(50f);
//                    table.row().left();
//                }
////                levelTable.row();
//            }
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
     * Update the status of this player mode.
     *
     * We prefer to separate update and draw from one another as separate methods, instead
     * of using the single render() method that LibGDX does.  We will talk about why we
     * prefer this in lecture.
     *
     * @param delta Number of seconds since last animation frame
     */
//    private void update(float delta) {
//        boolean flag = true;
//        for (TextButton button : levelButtons) {
//            if (button != null) {
//                flag = false;
//                break;
//            }
//        }
//        if (flag) {
//            assets.update(budget);
//            this.progress = assets.getProgress();
//            if (progress >= 1.0f) {
//                this.progress = 1.0f;
//                for (int i=0; i<buttons.length; i++) {
//                    buttons[i] = internal.getEntry("button", Texture.class);
//                }
//            }
//        }
//    }

    /**
     * Draw the status of this player mode.
     *
     * We prefer to separate update and draw from one another as separate methods, instead
     * of using the single render() method that LibGDX does.  We will talk about why we
     * prefer this in lecture.
     */
    private void draw() {
        canvas.begin();
//        canvas.draw(background, 0, 0);
//        canvas.draw(title, Color.WHITE, title.getWidth()/2f, title.getHeight()/2f,
//                centerX, canvas.getHeight()-TITLE_OFFSET, 0, TITLE_SCALE*scale,
//                TITLE_SCALE*scale);
//        Color tint;
//        // new
//        if (levelEditor != null) {
//            tint = (pressState == -2 ? Color.GRAY: Color.WHITE);
//            canvas.draw(levelEditor, tint, levelEditor.getWidth()/2f, levelEditor.getHeight()/2f,
//                    centerX/3f, canvas.getHeight()-TITLE_OFFSET, 0, LEVELSELECT_SCALE*scale,
//                    LEVELSELECT_SCALE*scale);
//        }
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
            if (isPressLevelEditor) {
                listener.exitScreen(this, EXIT_EDITOR);
                return;
            }
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

//    // PROCESSING PLAYER INPUT
//    /**
//     * Called when the screen was touched or a mouse button was pressed.
//     *
//     * This method checks to see if the play button is available and if the click
//     * is in the bounds of the play button.  If so, it signals the that the button
//     * has been pressed and is currently down. Any mouse button is accepted.
//     *
//     * @param screenX the x-coordinate of the mouse on the screen
//     * @param screenY the y-coordinate of the mouse on the screen
//     * @param pointer the button or touch finger number
//     * @return whether to hand the event to other listeners.
//     */
//    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
//        boolean flag = levelEditor == null;
//        for (Texture b : buttons) {
//            if (b != null) {
//                flag = false;
//                break;
//            }
//        }
//        if (flag || pressState == 0) {
//            return true;
//        }
////        if ((levelOne == null && levelTwo == null && levelThree == null) || pressState == 0) {
////            return true;
////        }
//
//        // Flip to match graphics coordinates
//        screenY = heightY-screenY;
//
//        // TODO: Fix scaling
//        // Play button is a circle.
//        float radius, dist;
//        for (int i=0; i<buttons.length; i++) {
//            // "the button can only be pressed if the level is unlocked"
//            if (buttons[i] != null && allLevelData.get(i).get("unlock").asBoolean()) {
//                radius = BUTTON_SCALE*scale*buttons[i].getWidth()/2.0f;
//                float offset = (i+1)/(float)totalLevelNum;
//                dist = (screenX-centerX*offset)*(screenX-centerX*offset)+(screenY-centerY-Y_OFFSET)*
//                        (screenY-centerY-Y_OFFSET);
//                if (dist < radius*radius) {
//                    pressState = i+1;
//                }
//            }
//        }
//        if (levelEditor != null) {
//            radius = LEVELSELECT_SCALE*scale*levelEditor.getWidth()/2.0f;
//            dist = (screenX-centerX/3f)*(screenX-centerX/3f)+(screenY-(canvas.getHeight()-TITLE_OFFSET))*
//                    (screenY-(canvas.getHeight()-TITLE_OFFSET));
//            if (dist < radius*radius) {
//                pressState = -2;
//                pressLevelEditor = true;
//            }
//        }
//        return false;
//    }
//    /**
//     * Called when a finger was lifted or a mouse button was released.
//     *
//     * This method checks to see if the play button is currently pressed down. If so,
//     * it signals the that the player is ready to go.
//     *
//     * @param screenX the x-coordinate of the mouse on the screen
//     * @param screenY the y-coordinate of the mouse on the screen
//     * @param pointer the button or touch finger number
//     * @return whether to hand the event to other listeners.
//     */
//    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
//        if (pressState >= 1 && pressState <= totalLevelNum) {
////        if (pressState == 1 || pressState == 2 || pressState == 3) {
//            // set the selected level number according to the pressState
//            levelNumber = pressState;
//            selectedLevelData = allLevelData.get(levelNumber-1).get("file").asString();
//            System.out.println(selectedLevelData);
//            pressState = 0;
//            return false;
//        } else if (pressState == -2) {
//            // press the level editor
//            pressState = 0;
//            return false;
//        }
//        return true;
//    }
//
//    /**
//     * Called when a button on the Controller was pressed.
//     *
//     * The buttonCode is controller specific. This listener only supports the start
//     * button on an X-Box controller.  This outcome of this method is identical to
//     * pressing (but not releasing) the play button.
//     *
//     * @param controller The game controller
//     * @param buttonCode The button pressed
//     * @return whether to hand the event to other listeners.
//     */
//    public boolean buttonDown (Controller controller, int buttonCode) {
//        // for now XBOX can only go to the first level
//        if (pressState == -1) {
//            ControllerMapping mapping = controller.getMapping();
//            if (mapping != null && buttonCode == mapping.buttonStart ) {
//                pressState = 1;
//                return false;
//            }
//        }
//        return true;
//    }
//
//    /**
//     * Called when a button on the Controller was released.
//     *
//     * The buttonCode is controller specific. This listener only supports the start
//     * button on an X-Box controller.  This outcome of this method is identical to
//     * releasing the the play button after pressing it.
//     *
//     * @param controller The game controller
//     * @param buttonCode The button pressed
//     * @return whether to hand the event to other listeners.
//     */
//    public boolean buttonUp (Controller controller, int buttonCode) {
//        if (pressState == 1 || pressState == 2 || pressState == 3) {
//            ControllerMapping mapping = controller.getMapping();
//            if (mapping != null && buttonCode == mapping.buttonStart ) {
//                pressState = 0;
//                return false;
//            }
//        }
//        return true;
//    }
//
//    // UNSUPPORTED METHODS FROM InputProcessor
//
//    /**
//     * Called when a key is pressed (UNSUPPORTED)
//     *
//     * @param keycode the key pressed
//     * @return whether to hand the event to other listeners.
//     */
//    public boolean keyDown(int keycode) {
//        return true;
//    }
//
//    /**
//     * Called when a key is typed (UNSUPPORTED)
//     *
//     //	 * @param keycode the key typed
//     * @return whether to hand the event to other listeners.
//     */
//    public boolean keyTyped(char character) {
//        return true;
//    }
//
//    /**
//     * Called when a key is released (UNSUPPORTED)
//     *
//     * @param keycode the key released
//     * @return whether to hand the event to other listeners.
//     */
//    public boolean keyUp(int keycode) {
//        return true;
//    }
//
//    /**
//     * Called when the mouse was moved without any buttons being pressed. (UNSUPPORTED)
//     *
//     * @param screenX the x-coordinate of the mouse on the screen
//     * @param screenY the y-coordinate of the mouse on the screen
//     * @return whether to hand the event to other listeners.
//     */
//    public boolean mouseMoved(int screenX, int screenY) {
//        return true;
//    }
//
//    /**
//     * Called when the mouse wheel was scrolled. (UNSUPPORTED)
//     *
//     * @param dx the amount of horizontal scroll
//     * @param dy the amount of vertical scroll
//     *
//     * @return whether to hand the event to other listeners.
//     */
//    public boolean scrolled(float dx, float dy) {
//        return true;
//    }
//
//    /**
//     * Called when the mouse or finger was dragged. (UNSUPPORTED)
//     *
//     * @param screenX the x-coordinate of the mouse on the screen
//     * @param screenY the y-coordinate of the mouse on the screen
//     * @param pointer the button or touch finger number
//     * @return whether to hand the event to other listeners.
//     */
//    public boolean touchDragged(int screenX, int screenY, int pointer) {
//        return true;
//    }
//
//    // UNSUPPORTED METHODS FROM ControllerListener
//
//    /**
//     * Called when a controller is connected. (UNSUPPORTED)
//     *
//     * @param controller The game controller
//     */
//    public void connected (Controller controller) {}
//
//    /**
//     * Called when a controller is disconnected. (UNSUPPORTED)
//     *
//     * @param controller The game controller
//     */
//    public void disconnected (Controller controller) {}
//
//    /**
//     * Called when an axis on the Controller moved. (UNSUPPORTED)
//     *
//     * The axisCode is controller specific. The axis value is in the range [-1, 1].
//     *
//     * @param controller The game controller
//     * @param axisCode 	The axis moved
//     * @param value 	The axis value, -1 to 1
//     * @return whether to hand the event to other listeners.
//     */
//    public boolean axisMoved (Controller controller, int axisCode, float value) {
//        return true;
//    }
//
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
