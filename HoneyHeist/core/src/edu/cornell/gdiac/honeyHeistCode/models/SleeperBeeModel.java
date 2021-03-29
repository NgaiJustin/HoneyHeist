package edu.cornell.gdiac.honeyHeistCode.models;

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
    }
}
