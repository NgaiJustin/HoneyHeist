package edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers;


import java.util.HashMap;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.honeyHeistCode.models.FlyingBeeModel;
import edu.cornell.gdiac.honeyHeistCode.models.LevelModel;
import edu.cornell.gdiac.honeyHeistCode.models.CharacterModel;
import com.badlogic.gdx.utils.JsonValue;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AIController {
    HashMap<CharacterModel, AISingleCharacterController> aICharacterControllers;
    AIGraphModel aIGraphModel;
    LevelModel levelModel;

    public AIController(LevelModel levelModel) {
        this.levelModel = levelModel;
        aICharacterControllers = new HashMap<CharacterModel, AISingleCharacterController>();
    }

    public void createAIForSingleCharacter(CharacterModel characterModel, JsonValue data) {
        aICharacterControllers.put(characterModel, new AISingleCharacterController (levelModel, characterModel, data));
    }

    public void deleteAIForSingleCharacter(CharacterModel characterModel) {
        aICharacterControllers.remove(characterModel);
    }


    public void moveAIControlledCharacters() {
        for (AISingleCharacterController aICharacterController: aICharacterControllers.values()) {
            aICharacterController.updateAIController();
            CharacterModel bee = aICharacterController.getControlledCharacter();
            bee.setMovement(aICharacterController.getMovementDirection().x * bee.getForce());
            if (bee.getClass() == FlyingBeeModel.class) {
                ((FlyingBeeModel) bee).setVMovement(aICharacterController.getMovementDirection().y * bee.getForce());
            }
        }
    }

    public void drawDebugLines(GameCanvas canvas, Vector2 scale) {
        for (AISingleCharacterController aICharacterController: aICharacterControllers.values()) {
            aICharacterController.drawDebug(canvas, scale);
        }
    }

}
