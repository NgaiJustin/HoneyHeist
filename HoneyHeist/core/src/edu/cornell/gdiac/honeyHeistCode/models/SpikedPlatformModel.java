package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.honeyHeistCode.models.PlatformModel;

public class SpikedPlatformModel extends PlatformModel{

    /**
     * Creates a new platform model with the given data.
     * <p>
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     * @param data The physics constants and polygon information for the platforms in this model
     */
    public SpikedPlatformModel(JsonValue data) {
        super(data, "spiked");
    }
    public SpikedPlatformModel() {
        super();
    }
}
