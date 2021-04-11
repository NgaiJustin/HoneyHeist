/**
The AI Controller for HoneyHeist. 

All enemies which need to interact with it's enviorment (specifically the player)
will have an AI Controller assigned to them.

It is dependent on Level Model in order for the character to get information about the level.
 */
package edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.honeyHeistCode.models.CharacterModel;
import edu.cornell.gdiac.honeyHeistCode.models.LevelModel;
import edu.cornell.gdiac.honeyHeistCode.models.PlatformModel;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;
import com.badlogic.gdx.utils.JsonValue;

import java.util.Random;


/**
 * This is the AI Single Character Controller which is responsible for character pathfinding and decision making.
 * The AI Controller will calculate a decision/pathfinding towards a "target" (currently specified as a Vector2)
 * and return a vector which is the direction in which the controlled character should move.
 * The AI Controlled supports getting the direction as a Vector2 through getDirection(). It also supports getting the
 * character it is controlling through getControlledCharacter(). Both will be needed in order to pass on the information to
 * Level Controller.
 *
 * If you want the AIController to target the player, you will need to put the position vector of the player into target.
 * Since that vector 2 is a reference, it should update with the movement of the player during runtime.
 *
 *
 */
public class AISingleCharacterController {
	/**
	 * Enumeration of the type of AI Controller
	 */
	public enum CharacterType {
		/** The character can fly. */
		FLYING_CHARACTER,

		/** The character cannot fly */
		GROUNDED_CHARACTER;

		public static CharacterType fromInteger(int value) {
			switch(value) {
				case 0:
					return FLYING_CHARACTER;
				case 1:
					return GROUNDED_CHARACTER;
			}
			return null;
		}
	}

    /**
	 * Enumeration to encode the finite state machine.
	 */
	private enum FSMState {
		/** The enemy is patrolling around since it is too far from the player */
		WANDER,
		/** The enemy is near the player and is trying to chace the player*/
		CHASE
	}

	private CharacterType characterType;
    private CharacterModel controlledCharacter;
    private LevelModel levelModel;
    private float chaseRadius;
    private float wanderSpeedFactor;
    private float chaseSpeedFactor;

    private FSMState state;
    private long ticks;
    private static final int ticksBeforeChangeInRandomDirection = 120;
	private static final int ticksBeforeChangeInChaseDirection = 15;
    private Vector2 target;
    private DirectedLineSegment lineToTarget;
    private DirectedLineSegment tempLineSegment;
    private Vector2 direction;
    private Vector2 temp;

    Random random = new Random();

    /**
	 * Creates an AI Controller for the given enemy model
	 *
	 * @param levelModel the level that the enemy is in.
	 * @param controlledCharacter the enemy that this AI Controller controls.
	 *
	 */
	public AISingleCharacterController(LevelModel levelModel, CharacterModel controlledCharacter, JsonValue data) {
		this.levelModel = levelModel;
        this.controlledCharacter = controlledCharacter;

		this.characterType = CharacterType.fromInteger(data.getInt("enemy_type"));
		this.chaseRadius = data.getFloat("chase_distance");
		switch (data.getInt("target")) {
			case 0:
				this.target = levelModel.getPlayer().getPosition();
		}
		this.wanderSpeedFactor = data.getFloat("wander_speed_factor");
		this.chaseSpeedFactor = data.getFloat("chase_speed_factor");

		state = FSMState.WANDER;
		ticks = 0;
		lineToTarget = new DirectedLineSegment(controlledCharacter.getPosition(), target);
		tempLineSegment = new DirectedLineSegment();
		direction = new Vector2();
		temp = new Vector2();
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

//	/**
//	 * Returns the horizontal direction which the controlled enemy should move.
//	 *
//	 * @return The direction that the enemy should move.
//	 */
//	public float getMovementHorizontalDirection() {
//		return direction.x;
//	}
//
//	/**
//	 * Returns the horizontal direction which the controlled enemy should move.
//	 *
//	 * @return The direction that the enemy should move.
//	 */
//	public float getMovementHorizontalDirection1orNeg1() {
//		if (direction.x > 0) {
//			return 1;
//		} else if (direction.x == 0) {
//			return 0;
//		} else {
//			return -1;
//		}
//	}

//	/**
//	 * Returns the vertical direction which the controlled enemy should move.
//	 *
//	 * @return The direction that the enemy should move.
//	 */
//	public float getMovementVerticalDirection() {
//		return direction.y;
//	}


	/**
	 * Returns the character which is controlled by the AI Controller
	 *
	 * @return the character model which is controlled by the AI Controller.
	 */
	public CharacterModel getControlledCharacter() {
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
	 * Updates the vector 2 between the enemy object and the target.
	 */
	private void updateLineToTarget() {
        lineToTarget.set(controlledCharacter.getPosition(), target);
    }


	/**
	 * Updates the state of the AI Controller to either WANDER or CHASE.
	 */
	private void updateFSMState() {
		float distanceToPlayer = controlledCharacter.getPosition().dst(levelModel.getPlayer().getPosition());
//		System.out.println(isLineCollidingWithAPlatform(lineToTarget));
		switch (this.state) {
			case WANDER:
				if (distanceToPlayer < chaseRadius && !isLineCollidingWithAPlatform(lineToTarget)) {
					this.state = FSMState.CHASE;
				}
				break;
			case CHASE:
				if (distanceToPlayer > chaseRadius || isLineCollidingWithAPlatform(lineToTarget)) {
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
		direction.set(lineToTarget.getDirection());
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
    private boolean isLineCollidingWithAPlatform(DirectedLineSegment line) {
		PlatformModel platforms = levelModel.getPlatforms();
		for (PolygonObstacle platform : platforms.getBodies()) {
			if (doesPolygonIntersectLine(line, platform.getTrueVertices())) {
				return true;
			}
		}
		return false;
    }

    public boolean doesPolygonIntersectLine(DirectedLineSegment line, float[] vertices) {
		for (int i = 0; i < vertices.length; i += 2) {
			int x1Index = i;
			int y1Index = i + 1;
			int x2Index;
			int y2Index;
			if (i + 2 < vertices.length) {
				x2Index = i + 2;
				y2Index = i + 3;
			} else {
				x2Index = 0;
				y2Index = 1;
			}
			tempLineSegment.set(vertices[x1Index], vertices[y1Index], vertices[x2Index], vertices[y2Index]);
			if (tempLineSegment.intersects(line)) {
				return true;
			}
		}
		return false;
	}

    private boolean pointContainedInPolygon(Vector2 point, float[] vertices) {
		for (int i = 0; i < vertices.length; i += 2) {
			int x1Index = i;
			int y1Index = i + 1;
			int x2Index;
			int y2Index;
			if (i + 2 < vertices.length) {
				x2Index = i + 2;
				y2Index = i + 3;
			} else {
				x2Index = 0;
				y2Index = 1;
			}
			float Ypart = (vertices[x2Index] - vertices[x1Index]) * (point.y - vertices[y1Index]);
			float Xpart = (point.x - vertices[x1Index]) * (vertices[y2Index] - vertices[y1Index]);
			if (0 > (Ypart - Xpart)) {
				return false;
			}
		}
		return true;
	}

	public void drawDebug(GameCanvas gameCanvas, Vector2 scale) {
    	if (state == FSMState.CHASE) {
			gameCanvas.drawCircle(chaseRadius, Color.RED, controlledCharacter.getPosition().x, controlledCharacter.getPosition().y, scale.x, scale.y);
		}
    	else {
			gameCanvas.drawCircle(chaseRadius, Color.YELLOW, controlledCharacter.getPosition().x, controlledCharacter.getPosition().y, scale.x, scale.y);
		}

    	if (isLineCollidingWithAPlatform(lineToTarget)) {
    		gameCanvas.drawLine(Color.RED, lineToTarget.x1, lineToTarget.y1, lineToTarget.x2, lineToTarget.y2, scale.x, scale.y);
		}
		else {
			gameCanvas.drawLine(Color.YELLOW, lineToTarget.x1, lineToTarget.y1, lineToTarget.x2, lineToTarget.y2, scale.x, scale.y);
		}
	}

}