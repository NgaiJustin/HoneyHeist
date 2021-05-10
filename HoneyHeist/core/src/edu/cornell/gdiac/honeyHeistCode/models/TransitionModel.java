package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.graphics.Color;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.honeyHeistCode.obstacle.BoxObstacle;
import edu.cornell.gdiac.util.FilmStrip;

public class TransitionModel extends BoxObstacle {

    FilmStrip transitionAnim;
    private final int FRAMES_PER_ANIM = 3;
    private int animFrames = 0;
    private boolean reversed;
    private boolean finished = false;

    public TransitionModel(float x, float y, boolean reversed){
        super(x,y,1,1);
        this.reversed = reversed;
    }

    public void setAnimationStrip(FilmStrip strip) {
        transitionAnim = strip.copy();
        if (reversed) transitionAnim.setFrame(transitionAnim.getSize()-1);
        else transitionAnim.setFrame(0);
    }

    public void update(float dt) {
        super.update(dt);

        if (animFrames % FRAMES_PER_ANIM == 0) {
            if(reversed){
                if(transitionAnim.getFrame() != 0) {
                    transitionAnim.setFrame(transitionAnim.getFrame() - 1);
                }
                else {
                    finished = true;
                }
            }
            else if(transitionAnim.getFrame() != transitionAnim.getSize()-1) {
                transitionAnim.setFrame(transitionAnim.getFrame() + 1);
            }
        }
        animFrames++;
    }

    public boolean isFinished() {
        return finished;
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {

        float offsetx = transitionAnim.getRegionWidth()/2;
        float offsety = transitionAnim.getRegionHeight()/2;
        canvas.draw(transitionAnim, Color.WHITE, offsetx, offsety, getX() * drawScale.x, getY() * drawScale.y, getAngle(), 1.0f, 1.0f);

    }
}
