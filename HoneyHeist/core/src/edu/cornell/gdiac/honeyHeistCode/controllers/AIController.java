/**
The AI Controller for HoneyHeist. 

All enemies which need to interact with it's enviorment (specifically the player)
will have an AI Controller assigned to them.

It is dependent on Level Model in order for the character to get information about the level.
 */
package edu.cornell.gdiac.honeyHeistCode.controllers;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.honeyHeistCode.models.AbstractBeeModel;
import edu.cornell.gdiac.honeyHeistCode.models.LevelModel;
import edu.cornell.gdiac.honeyHeistCode.models.PlatformModel;
import edu.cornell.gdiac.honeyHeistCode.obstacle.Obstacle;

import java.util.Random;

/**
 * This is the AI Controller which is responsible for character pathfinding and decision making.
 * The AI Controller will calculate a decision/pathfinding towards a "target" (currently specified as a Vector2)
 * and return a vector which is the direction in which the controlled character should move.
 * The AI Controlled supports getting the direction as a Vector2 through getDirection(). It also supports getting the
 * character it is controlling through getControlledCharacter(). Both will be needed in order to pass on the information to
 * Level Controller.
 * Currently, there is only one behavior that the AI Controller supports which is returning the direction of the target.
 *
 * If you want only horizontal or vertical directions, there are function that support that.
 *
 * If you want the AIController to target the player, you will need to put the position vector of the player into target.
 * Since that vector 2 is a reference, it should update with the movement of the player during runtime.
 */
public class AIController {
	/**
	 * Enumeration of the type of AI Controller
	 */
	public static enum CharacterType {
		/** The character can fly. */
		FLYING_CHARACTER,

		/** The character cannot fly */
		GROUNDED_CHARACTER
	}

    /**
	 * Enumeration to encode the finite state machine.
	 */
	private static enum FSMState {
		/** The enemy is patrolling around since it is too far from the player */
		WANDER,
		/** The enemy is near the player and is trying to chace the player*/
		CHASE
	}

	private CharacterType characterType;
    private AbstractBeeModel controlledCharacter;
    private LevelModel levelModel;
    private FSMState state;
    private long ticks;
    private static final int ticksBeforeChangeInRandomDirection = 120;
	private static final int ticksBeforeChangeInChaseDirection = 15;
	private static final float wanderSpeedFactor = 0.5f;
    private Vector2 target;
    private Vector2 offset;
    private Vector2 lineToTarget;
    private Vector2 direction;

    Random random = new Random();

    private static final float CHASE_RADIUS = 3;

	/**
	 * Creates an AI Controller for the given enemy model
	 *
	 * @param levelModel the level that the enemy is in.
	 * @param target the target which this AI Controller is trying to chase.
	 * @param controlledCharacter the enemy that this AI Controller controls.
	 */
	public AIController(LevelModel levelModel, Vector2 target, AbstractBeeModel controlledCharacter, CharacterType characterType) {
		this.levelModel = levelModel;
		this.target = target;
		this.offset = new Vector2(0,0);
        this.controlledCharacter = controlledCharacter;
        this.characterType = characterType;
		state = FSMState.WANDER;
		ticks = 0;
		lineToTarget = new Vector2();
		direction = new Vector2();
	}

	/**
	 * Creates an AI Controller for the given enemy model
	 *
	 * @param levelModel the level that the enemy is in.
	 * @param target the target which this AI Controller is trying to chase.
	 * @param controlledCharacter the enemy that this AI Controller controls.
	 * @param offsetX the x of the offset to the target.
	 * @param offsetY the y of the offset to the target.
	 */
	public AIController(LevelModel levelModel, Vector2 target, AbstractBeeModel controlledCharacter, CharacterType characterType, float offsetX, float offsetY) {
		this.levelModel = levelModel;
		this.target = target;
		this.offset = new Vector2(offsetX, offsetY);
		this.controlledCharacter = controlledCharacter;
		this.characterType = characterType;
		state = FSMState.WANDER;
		ticks = 0;
		lineToTarget = new Vector2();
		direction = new Vector2();
	}

	/**
	 * Updates the state of the AI Controller
	 */
	public void updateAIController() {
		updateLineToTarget();
		updateFSMState();
		updateDirectionBasedOnState();
		ticks ++;
	}

	/**
	 * Returns the direction which the controlled enemy should move.
	 *
	 * @return The direction that the enemy should move.
	 */
	public Vector2 getMovementDirection() {
		return direction;
	}

	/**
	 * Returns the horizontal direction which the controlled enemy should move.
	 *
	 * @return The direction that the enemy should move.
	 */
	public float getMovementHorizontalDirection() {
		return direction.x;
	}

	/**
	 * Returns the horizontal direction which the controlled enemy should move.
	 *
	 * @return The direction that the enemy should move.
	 */
	public float getMovementHorizontalDirection1orNeg1() {
		if (direction.x > 0) {
			return 1;
		} else if (direction.x == 0) {
			return 0;
		} else {
			return -1;
		}
	}

	/**
	 * Returns the horizontal direction which the controlled enemy should move.
	 *
	 * @return The direction that the enemy should move.
	 */
	public float getMovementVerticalDirection() {
		return direction.y;
	}

	/**
	 * Returns the horizontal direction which the controlled enemy should move.
	 *
	 * @return The direction that the enemy should move.
	 */
	public float getMovementVerticalDirection1orNeg1() {
		if (direction.y > 0) {
			return 1;
		} else if (direction.y == 0) {
			return 0;
		} else {
			return -1;
		}
	}

	/**
	 * Returns the character which is controlled by the AI Controller
	 *
	 * @return the character model which is controlled by the AI Controller.
	 */
	public AbstractBeeModel getControlledCharacter() {
		return controlledCharacter;
	}

	/**
	 * Returns the target vector.
	 * @return return target
	 */
	public Vector2 getTarget() {
		return target;
	}

	/**
	 * Sets the target.
	 */
	public void setTarget(Vector2 target) {
		this.target = target;
	}

	/**
	 * Sets the offset through two floats.
	 */
	public void setOffset(float x, float y) {
		this.offset.set(x, y);
	}
	
	/**
	 * Updates the vector 2 between the enemy object and the target.
	 */
	private void updateLineToTarget() {
        lineToTarget.set(target);
        lineToTarget.add(offset);
        lineToTarget.sub(controlledCharacter.getPosition());
    }


	/**
	 * Updates the state of the AI Controller to either WANDER or CHASE.
	 */
	private void updateFSMState() {
		float distanceToPlayer = controlledCharacter.getPosition().dst(levelModel.getPlayer().getPosition());
		switch (this.state) {
			case WANDER:
				if (distanceToPlayer < CHASE_RADIUS) {
					this.state = FSMState.CHASE;
				}
				break;
			case CHASE:
				if (distanceToPlayer > CHASE_RADIUS) {
					this.state = FSMState.WANDER;
				}
		}
	}

	/**
	 * Updates the direction that the AI Controller has decided based on the state.
	 */
	private void updateDirectionBasedOnState() {
		switch (this.state) {
			case WANDER:
				if (ticks % ticksBeforeChangeInRandomDirection == 0) {
					if (direction.isZero()) {
						setDirectionToRandomHorizontal();
					}
					changeToOppositeDirection();
					direction.nor();
					direction.scl(wanderSpeedFactor);
				}

				break;

			case CHASE:
				if (ticks % ticksBeforeChangeInChaseDirection == 0) {
					setDirectionToGoTowardsTarget();
				}
				break;
		}
	}

	/**
	 * Sets the direction vector to be a random direction.
	 */
	private void setDirectionToRandomDirection() {
		int angle = random.nextInt(360);
		direction.set(1,0);
		direction.setAngleDeg(angle);
		direction.nor();
	}

	/**
	 * Sets the direction vector to be a random direction limited to 4 cardinal directions.
	 */
	private void setDirectionToRandom4CardinalDirection() {
		int angle = random.nextInt(4);
		direction.set(1,0);
		direction.setAngleDeg(angle * 90);
		direction.nor();
	}

	private void setDirectionToRandomHorizontal() {
		int fiftyFifty = random.nextInt(2);
		if (fiftyFifty > 0) {
			direction.set(1, 0);
		} else {
			direction.set(-1, 0);
		}
	}

	/**
	 * Sets the direction to the opposite direction of it's current direction.
	 */
	private void changeToOppositeDirection() {
		direction.scl(-1);
	}
	/**
	 * Set the direction vector to go towards the specified target.
	 */
	private void setDirectionToGoTowardsTarget() {
		direction.set(lineToTarget);
		if (characterType == CharacterType.GROUNDED_CHARACTER) {
			if (direction.x >= 0) {
				direction.set(1,0);
			} else {
				direction.set(-1,0);
			}
		}
		direction.nor();
	}

	/**
	 * Still under construction. Checks if the given path to target is blocked by a platform.
	 * @return
	 */
    private boolean isLineCollidingWithAPlatform() {
		PlatformModel platform = levelModel.getPlatforms();
		for (Obstacle platforms : platform.getBodies()) {

		}
		return false;
    }

}