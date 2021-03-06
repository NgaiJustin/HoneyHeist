package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.honeyHeistCode.models.PlatformModel;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;

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
    public SpikedPlatformModel(JsonValue data, Vector2 worldCenter) {

        super(data, "spikedplatform", worldCenter);
    }

    public SpikedPlatformModel() {
        super();
    }

    @Override
    public boolean activatePhysics(World world) {
        boolean check = super.activatePhysics(world);
        Filter filter = new Filter();
        filter.categoryBits = 0x0002;
        for(PolygonObstacle body: getBodies()){
            for(Fixture fix : body.getBody().getFixtureList()){
                fix.setFilterData(filter);
            }
        }
        return check;
    }
}
