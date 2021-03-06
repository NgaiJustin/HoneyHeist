/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game.  It is the "static main" of
 * LibGDX.  In the first lab, we extended ApplicationAdapter.  In previous lab
 * we extended Game.  This is because of a weird graphical artifact that we do not
 * understand.  Transparencies (in 3D only) is failing when we use ApplicationAdapter. 
 * There must be some undocumented OpenGL code in setScreen.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * Updated asset version, 2/6/2021
 */
 package edu.cornell.gdiac.honeyHeistCode;

import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.honeyHeistCode.controllers.LevelController;
import edu.cornell.gdiac.honeyHeistCode.controllers.EditorController;
import edu.cornell.gdiac.honeyHeistCode.controllers.LoadingMode;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.assets.*;

/**
 * Root class for a LibGDX.  
 * 
 * This class is technically not the ROOT CLASS. Each platform has another class above
 * this (e.g. PC games use DesktopLauncher) which serves as the true root.  However, 
 * those classes are unique to each platform, while this class is the same across all 
 * plaforms. In addition, this functions as the root class all intents and purposes, 
 * and you would draw it as a root class in an architecture specification.  
 */
public class GDXRoot extends Game implements ScreenListener {
	/** AssetManager to load game assets (textures, sounds, etc.) */
	AssetDirectory directory;
	/** Drawing context to display graphics (VIEW CLASS) */
	private GameCanvas canvas; 
	/** Player mode for the asset loading screen (CONTROLLER CLASS) */
	private LoadingMode loading;
	/** Player mode for level selector (CONTROLLER CLASS) */
	private LevelSelector levelSelector;
	/** Player mode for the the game proper (CONTROLLER CLASS) */
	private int current;
//	/** List of all WorldControllers */
//	private WorldController[] controllers;
	// new editing
	/** GameplayController */
	private GameplayController controller;
	/** Level Editor Controller + GUI (Screen) */
	private EditorController editorController;

	/**
	 * Creates a new game from the configuration settings.
	 *
	 * This method configures the asset manager, but does not load any assets
	 * or assign any screen.
	 */
	public GDXRoot() { }

	/**
	 * Called when the Application is first created.
	 *
	 * This is method immediately loads assets for the loading screen, and prepares
	 * the asynchronous loader for all other assets.
	 */
	public void create() {
		canvas  = new GameCanvas();
		loading = new LoadingMode("assets.json",canvas,1);

		// Initialize the game world
//		controllers = new WorldController[1];
//		controllers[0] = new LevelController();
		controller = new GameplayController();
		// current = 0;
		// Initialize editor controller and modes
		editorController = new EditorController();
		loading.setScreenListener(this);

		setScreen(loading);
	}

	/** 
	 * Called when the Application is destroyed. 
	 *
	 * This is preceded by a call to pause().
	 */
	public void dispose() {
		// Call dispose on our children
		setScreen(null);
		// new editing
//		for(int ii = 0; ii < controllers.length; ii++) {
//			controllers[ii].dispose();
//		}
		controller.dispose();
		// new editing ends

		canvas.dispose();
		canvas = null;
	
		// Unload all of the resources
		if (directory != null) {
			directory.unloadAssets();
			directory.dispose();
			directory = null;
		}
		super.dispose();
	}
	
	/**
	 * Called when the Application is resized. 
	 *
	 * This can happen at any point during a non-paused state but will never happen 
	 * before a call to create().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		canvas.resize();
		super.resize(width,height);
	}
	
	/**
	 * The given screen has made a request to exit its player mode.
	 *
	 * The value exitCode can be used to implement menu options.
	 *
	 * @param screen   The screen requesting to exit
	 * @param exitCode The state of the screen upon exit
	 */
	public void exitScreen(Screen screen, int exitCode) {
		if (screen == loading) {
			// new editing start
//			for(int ii = 0; ii < controllers.length; ii++) {
//				directory = loading.getAssets();
//				controllers[ii].gatherAssets(directory);
//				controllers[ii].setScreenListener(this);
//				controllers[ii].setCanvas(canvas);
//			}
//			controllers[current].reset();
//			setScreen(controllers[current]);

//			directory = loading.getAssets();
//			controller.gatherAssets(directory);
//			controller.setScreenListener(this);
//			controller.setCanvas(canvas);
//			controller.reset();
//			setScreen(controller);
			directory = loading.getAssets();
			levelSelector = new LevelSelector(directory, canvas, 0);
			levelSelector.setScreenListener(this);
			setScreen(levelSelector);
			loading.dispose();
			loading = null;
		} else if (screen == levelSelector && exitCode == LevelSelector.EXIT_PLAY) {
//			directory = levelSelector.getAssets();
			levelSelector.stopAllSounds();
			String levelData = levelSelector.getLevelData();
			JsonValue allLevelData = levelSelector.getAllLevelData();
			int currentLevelNum = levelSelector.getCurrentLevelNum();
			controller.gatherAssets(directory, levelData, allLevelData, currentLevelNum);
			editorController.gatherAssets(directory);
			controller.setScreenListener(this);
			editorController.setScreenListener(this);
			controller.setCanvas(canvas);
			editorController.setCanvas(canvas);
			controller.reset();
			// set the level number

			setScreen(controller);

//			levelSelector.dispose();
//			levelSelector = null;
		} else if(screen == levelSelector && exitCode == LevelSelector.EXIT_EDITOR) {
			levelSelector.stopAllSounds();
			directory = levelSelector.getAssets();
			JsonValue allLevelData = levelSelector.getAllLevelData();
			editorController.gatherAssets(directory);
//			int currentLevelNum =
			controller.gatherAssets(directory, "platform:defaultLevel", allLevelData, 1);
			editorController.setScreenListener(this);
			controller.setScreenListener(this);
			editorController.setCanvas(canvas);
			controller.setCanvas(canvas);
			editorController.reset();
			editorController.populateLevel();
			setScreen(editorController);
		} else if (screen == levelSelector && exitCode == LevelSelector.EXIT_QUIT) {
			// We quit the main application
			Gdx.app.exit();
		} else if (screen == editorController && exitCode == WorldController.EXIT_NEXT) {
//			current = (current+1) % controllers.length;
//			controllers[current].reset();
//			setScreen(controllers[current]);
			AssetDirectory temp = new AssetDirectory("editorassets.json");
			temp.loadAssets();
			temp.finishLoading();
			controller.gatherLevelData(temp,editorController.getLoadPath());
			controller.reset();
			setScreen(controller);
//		} else if (exitCode == WorldController.EXIT_PREV) {
		} else if (exitCode == GameplayController.EXIT_NEXT && editorController.getLoadPath() != "platform:defaultLevel") {
			controller.reset();
			setScreen(controller);
		} else if (exitCode == GameplayController.EXIT_MENU) {
			int currentLevelNum = controller.getCurrentLevelNum();
			System.out.println("current level number: "+currentLevelNum);
			levelSelector = new LevelSelector(directory, canvas, currentLevelNum);
			levelSelector.setScreenListener(this);
			editorController = new EditorController();
			setScreen(levelSelector);
//			controller.dispose();
		} else if (exitCode == GameplayController.EXIT_NEXT) {
//			current = (current+1) % controllers.length;
//			controllers[current].reset();
//			setScreen(controllers[current]);
			String nextLevel = levelSelector.nextLevelData();
			System.out.println(nextLevel);
			controller.gatherLevelData(directory, nextLevel);
			controller.reset();
			setScreen(controller);
//		} else if (exitCode == WorldController.EXIT_PREV) {
		} else if (exitCode == GameplayController.EXIT_PREV) {
//			current = (current+controllers.length-1) % controllers.length;
//			controllers[current].reset();
//			setScreen(controllers[current]);
			controller.reset();
			setScreen(controller);
//		} else if (exitCode == WorldController.EXIT_QUIT) {
			// new editing end
		} else if(exitCode == GameplayController.EXIT_EDITOR) {
			if (editorController.getLoadPath() != "platform:defaultLevel") {
				AssetDirectory temp = new AssetDirectory("editorassets.json");
				temp.loadAssets();
				temp.finishLoading();
				editorController.gatherLevelData(temp);
			}
			editorController.reset();
			editorController.populateLevel();
			setScreen(editorController);
		} else if (exitCode == GameplayController.EXIT_QUIT) {
			// We quit the main application
			Gdx.app.exit();
		}
	}

}
