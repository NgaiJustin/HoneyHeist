package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;

public class SleeperBeeModel extends AbstractBeeModel{
    /**
     * Creates a bee avatar with the given physics data
     *
     * @param data   The physics constants for the player Ant
     * @param width  The object width in physics units
     * @param height The object width in physics units
     */
    public SleeperBeeModel(JsonValue data, float width, float height) {
        super(data, width, height);
        isGrounded = true;
    }

    public void update(float dt) {
        if (!isRotating) {
            return;
        }

        float rotationAmount = rotationSpeed * dt;
        if (rotationAmount > remainingAngle){
            rotationAmount = remainingAngle;
            isRotating = false;
            stickTime = maxStickTime;
        }
        remainingAngle -= rotationAmount;
        if (!isClockwise) {
            rotationAmount *= -1;
        }
        rotateAboutPoint(rotationAmount, stageCenter);
    }
}
