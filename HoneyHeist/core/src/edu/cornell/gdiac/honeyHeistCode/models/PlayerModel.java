package edu.cornell.gdiac.honeyHeistCode.models;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.util.FilmStrip;

/**
 *  Model class for player in HoneyHeist.
 */
public class PlayerModel extends CharacterModel {
    // Walking animation fields
    /** The texture filmstrip for the left animation node */
    private FilmStrip walkingAnim;
    /** The animation phase for the walking animation */
    private boolean walkCycle = true;

    /**
     * Enumeration to identify the ant animations
     */
    public enum AntAnimations {
        /** Walking animation */
        WALK,
        // Future animations to be supported
    };

    /**
     * Creates a ant avatar with the given physics data
     *
     * @param data  	The physics constants for the player Ant
     * @param width		The object width in physics units
     * @param height	The object width in physics units
     */
    public PlayerModel(JsonValue data, float x, float y, float width, float height){
        super(data, x, y, width, height);
        setName("ant");
        sensorName = "AntGroundSensor";

    }

    /**
     * Sets the animation node for the given afterburner
     *
     * @param  anim     enumeration to identify the ant animation
     *
     * @param  strip 	the animation filmstrip for the given animation
     */
    public void setAnimationStrip(AntAnimations anim, FilmStrip strip) {
        switch (anim) {
            case WALK:
                 walkingAnim= strip;
                 break;
            default:
                assert false : "Invalid ant animation enumeration";
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
    public void animateAnt(AntAnimations anim, boolean on) {
        FilmStrip node = null;
        boolean  cycle = true;

        switch (anim) {
            case WALK:
                node  = walkingAnim;
                cycle = walkCycle;
                break;
                // Add more cases for future animations
            default:
                assert false : "Invalid burner enumeration";
        }

        if (on) {
            // Turn on the flames and go back and forth
            if (node.getFrame() == 0 || node.getFrame() == 1) {
                cycle = true;
            } else if (node.getFrame() == node.getSize()-1) {
                cycle = false;
            }

            // Increment
            if (cycle) {
                node.setFrame(node.getFrame()+1);
            } else {
                node.setFrame(node.getFrame()-1);
            }
        } else {
            node.setFrame(0);
        }

        switch (anim) {
            case WALK:
                walkCycle = cycle;
                break;
                // Add more cases for future animations
            default:
                assert false : "Invalid burner enumeration";
        }
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = this.faceRight ? 1.0f : -1.0f;
        // Walking Animation
        if (walkingAnim != null) {
            float offsety = walkingAnim.getRegionHeight()-origin.y;
            canvas.draw(walkingAnim,Color.WHITE,origin.x,offsety,getX()*drawScale.x,getY()*drawScale.x,getAngle(),effect,1);
        }
        // Stationary ant
        else {
            canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y, getAngle(), effect, 1.0f);
        }
    }
}
