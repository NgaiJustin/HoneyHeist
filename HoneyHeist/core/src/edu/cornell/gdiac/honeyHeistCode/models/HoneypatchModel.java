package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;

public class HoneypatchModel extends PlatformModel {

    private float slowSpeed;

    public HoneypatchModel(JsonValue data, float slowSpeed, Vector2 worldCenter){
        super(data,"honeypatch", worldCenter);
        this.setSensor(true);
        this.slowSpeed = slowSpeed;
    }
    public HoneypatchModel(){
        super();
    }

    public float getSlowSpeed() {
        return slowSpeed;
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        // Delegate to components
        for(PolygonObstacle obj : bodies) {
            Color tint = Color.ORANGE;
            tint.set(tint.r,tint.g,tint.b,0.3f);
            obj.draw(canvas,tint);
        }
    }
}


