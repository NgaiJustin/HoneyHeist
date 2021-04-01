package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.utils.JsonValue;

public class ChaserBeeModel extends AbstractBeeModel{
    /**
     * Creates a bee avatar with the given physics data
     *
     * @param data   The physics constants for the player Ant
     * @param width  The object width in physics units
     * @param height The object width in physics units
     */
    public ChaserBeeModel(JsonValue data, float x, float y, float width, float height)
            {
        super(data, x, y, width, height);
    }
}
