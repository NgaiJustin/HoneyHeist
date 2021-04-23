package edu.cornell.gdiac.honeyHeistCode;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class MyTenPatch  {

    /** Reference to GameCanvas created by the root */
    private GameCanvas canvas;

    /** Order for textures in texture[] */
    static public final int TOP_LEFT = 0;
    static public final int TOP_CENTER = 1;
    static public final int TOP_RIGHT = 2;
    static public final int MIDDLE_LEFT = 3;
    static public final int MIDDLE_CENTER = 4;
    static public final int MIDDLE_RIGHT = 5;
    static public final int BOTTOM_LEFT = 6;
    static public final int BOTTOM_CENTER = 7;
    static public final int BOTTOM_RIGHT = 8;

    /** Stores the 9 textures */
    TextureRegion [] nineTextures;


    public MyTenPatch(TextureRegion[] textures){
        assert textures.length == 9;
        this.nineTextures = textures;
    }

    public void draw (Batch batch, float x, float y, float originX, float originY,
                      float width, float height, float scaleX, float scaleY, float rotation) {
        assert canvas != null;
    }

}
