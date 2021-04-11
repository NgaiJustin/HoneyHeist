package edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers;

import com.badlogic.gdx.utils.Array;

import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.honeyHeistCode.models.LevelModel;
import edu.cornell.gdiac.honeyHeistCode.models.CharacterModel;
import com.badlogic.gdx.utils.JsonValue;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AIController {
    Array<AISingleCharacterController> aICharacterControllers;
    AIGraphModel aIGraphModel;
    LevelModel levelModel;

    public AIController(LevelModel levelModel, TextureRegion whiteSquare) {
        this.levelModel = levelModel;
        aICharacterControllers = new Array<AISingleCharacterController>();
        aIGraphModel = new AIGraphModel(levelModel, whiteSquare);
    }

    public void createAIForSingleCharacter(CharacterModel characterModel, JsonValue data) {
        aICharacterControllers.add(new AISingleCharacterController (levelModel, characterModel, data));
    }


    public void moveAIControlledCharacters() {
        for (AISingleCharacterController aICharacterController: aICharacterControllers) {
            aICharacterController.updateAIController();
            CharacterModel bee = aICharacterController.getControlledCharacter();
            bee.setMovement(aICharacterController.getMovementDirection().x * bee.getForce());
        }
    }

    public void updateAccessibility() {
        aIGraphModel.updateAccessibility();
    }

    public void drawDebugTileMap(GameCanvas canvas, Vector2 scale) {

        aIGraphModel.setTextures();
        aIGraphModel.drawDebug(canvas, scale);
    }

    public void drawDebugLines(GameCanvas canvas, Vector2 scale) {
        for (AISingleCharacterController aICharacterController: aICharacterControllers) {
            aICharacterController.drawDebug(canvas, scale);
        }
    }

}
