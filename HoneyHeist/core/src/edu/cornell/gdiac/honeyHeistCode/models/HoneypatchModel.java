package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;

public class HoneypatchModel extends PlatformModel {

    private float slowSpeed;

    public HoneypatchModel(JsonValue data, float slowSpeed){
        super(data,"honeypatch");
        this.slowSpeed = slowSpeed;
    }

    public float getSlowSpeed() {
        return slowSpeed;
    }
}


