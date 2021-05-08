package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.util.FilmStrip;

public class LarvaeModel extends AbstractBeeModel{
    // Moving animation fields
    private FilmStrip walkingAnim;
    private FilmStrip chasingAnim;

    /** The animation phase for the walking animation */
    private boolean walkCycle = true;
    private boolean chaseCycle = true;
    private final int FRAMES_PER_ANIM = 7;
    private int animFrames = 0;


    /**
     * Enumeration to identify the larvae animations
     */
    public enum LarvaeAnimations {
        /** Walking animation */
        WALK,
        /** Chasing animation */
        CHASE,
    };
    /**
     * Creates a bee avatar with the given physics data
     *
     * @param data   The physics constants for the player Larvae
     * @param width  The object width in physics units
     * @param height The object width in physics units
     */
    public LarvaeModel(JsonValue data, float x, float y, float width, float height)
            {
        super(data, x, y, width, height);
    }

    /**
     * Sets the animation node for the given afterburner
     *
     * @param  anim     enumeration to identify the larvae animation
     *
     * @param  strip 	the animation filmstrip for the given animation
     */
    public void setAnimationStrip(LarvaeAnimations anim, FilmStrip strip) {
        switch (anim) {
            case WALK:
                walkingAnim= strip;
                break;
            case CHASE:
                chasingAnim= strip;
                break;
            default:
                assert false : "Invalid larvae animation enumeration";
        }
    }
    /**
     * Animates the given animation.
     *
     * If the animation is not active, it will reset to the initial animation frame.
     *
     * @param  anim   The reference to the type of animation
     * @param  on       Whether the animation is active
     */
    public void animateLarvae(LarvaeAnimations anim, boolean on) {
        FilmStrip node = null;
        boolean  cycle = true;

        switch (anim) {
            case WALK:
                node  = walkingAnim;
                cycle = walkCycle;
                break;
            case CHASE:
                node = chasingAnim;
                cycle = chaseCycle;
                break;
            // Add morecases for future animations
            default:
                assert false : "Invalid burner enumeration";
        }
        if (animFrames % FRAMES_PER_ANIM == 0) {
            if (on) {
                // Turn on the flames and go back and forth
                if (node.getFrame() == 0 || node.getFrame() == 1) {
                    cycle = true;
                } else if (node.getFrame() == node.getSize() - 1) {
                    cycle = false;
                }

                // Increment
                if (cycle) {
                    node.setFrame(node.getFrame() + 1);
                } else {
                    node.setFrame(0);
                }
            } else {
                node.setFrame(0);
            }
        }
        animFrames++;

        switch (anim) {
            case WALK:
                walkCycle = cycle;
                break;
            case CHASE:
                chaseCycle = cycle;
                break;
            // Add more cases for future animations
            default:
                assert false : "Invalid burner enumeration";
        }
    }

    /**
     * Sets left/right movement of this character.
     * <p>
     * This is the result of input times larvae's force.
     *
     * @param value left/right movement of this character.
     */
    public void setMovement(float value) {
        super.setMovement(value);
        animateLarvae(LarvaeAnimations.WALK, true);
        animateLarvae(LarvaeAnimations.CHASE, true);
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = this.faceRight ? 1.0f : -1.0f;
        FilmStrip currAnim = this.isChasing ? chasingAnim : walkingAnim;
        if (currAnim != null) {
            float offsety = currAnim.getRegionHeight()-origin.y;
            canvas.draw(currAnim, Color.WHITE,
                    origin.x,
                    offsety,
                    getX()*drawScale.x,
                    getY()*drawScale.x,
                    getAngle(),
                    effect,1);
        }
        // Stationary larvae
        else {
            System.out.println("MISSING LARVAE TEXTURE");
            canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y, getAngle(), effect, 1.0f);
        }
    }
}
