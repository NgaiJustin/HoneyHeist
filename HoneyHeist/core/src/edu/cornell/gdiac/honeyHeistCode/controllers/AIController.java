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
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;
import com.badlogic.gdx.utils.JsonValue;

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
    private AbstractBeeModel controlledCharacter;
    private LevelModel levelModel;
    private float chaseRadius;
    private float wanderSpeedFactor;
    private float chaseSpeedFactor;

    private FSMState state;
    private long ticks;
    private static final int ticksBeforeChangeInRandomDirection = 120;
	private static final int ticksBeforeChangeInChaseDirection = 15;
    private Vector2 target;
    private Vector2 offset;
    private DirectedLineSegment lineToTarget;
    private DirectedLineSegment tempLineSegment;
    private Vector2 direction;
    private Vector2 temp;

    Random random = new Random();

    /**
	 * Creates an AI Controller for the given enemy model
	 *
	 * @param levelModel the level that the enemy is in.
	 * @param target the target which this AI Controller is trying to chase.
	 * @param controlledCharacter the enemy that this AI Controller controls.
	 *
	 */
	public AIController(LevelModel levelModel, AbstractBeeModel controlledCharacter, JsonValue data) {
		this.levelModel = levelModel;
        this.controlledCharacter = controlledCharacter;

		this.characterType = CharacterType.fromInteger(data.getInt("enemy_type"));
		this.chaseRadius = data.getFloat("chase_distance");
		switch (data.getInt("target")) {
			case 0:
				this.target = levelModel.getPlayer().getPosition();
		}
		this.offset = new Vector2(data.get("offset").getFloat("x"), data.get("offset").getFloat("y"));
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
			if (doesLineSegmentIntersectsPolygon(line, platform.getVertices())) {
				return true;
			}
		}
		return false;
    }

    private boolean doesLineSegmentIntersectsPolygon(DirectedLineSegment line, float[] vertices) {
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

	public class DirectedLineSegment {
    	float x1;
    	float y1;
    	float x2;
    	float y2;

    	Vector2 direction;

    	public DirectedLineSegment() {
			this.x1 = 0;
			this.y1 = 0;
			this.x2 = 0;
			this.y2 = 0;
			direction = new Vector2();
		}

    	public DirectedLineSegment (float x1, float y1, float x2, float y2) {
    		this();
			set(x1, y1, x2, y2);
		}

		public DirectedLineSegment (Vector2 startPoint, Vector2 endPoint) {
			this();
    		set(startPoint, endPoint);
		}

		public void set(float x1, float y1, float x2, float y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			direction.set((x2 - x1), (y2 - y1));
		}

		public void set(Vector2 startPoint, Vector2 endPoint) {
			set(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
		}

		public float dst() {
    		return (float)Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
		}

		public Vector2 getDirection() {
			return direction;
		}

		public boolean intersects(DirectedLineSegment line) {
			int dir1 = orientation(this.x1, this.y1, this.x2, this.y2, line.x1, line.y1);
			int dir2 = orientation(this.x1, this.y1, this.x2, this.y2, line.x2, line.y2);
			int dir3 = orientation(line.x1, line.y1, line.x2, line.y2, this.x1, this.y1);
			int dir4 = orientation(line.x1, line.y1, line.x2, line.y2, this.x2, this.y2);

			if (dir1 != dir2 && dir3 != dir4) {return true;}
			return false;
    	}

		private int orientation(float x1, float y1, float x2, float y2, float x3, float y3) {
			float val = (y2 - y1) * (x3 - x2) - (x2 - x1) * (y3 - y2);
			if (val == 0) {return 0;}
			else if (val < 0) {return -1;}
			else {return 1;}
		}

		@Override
		public String toString() {
			return "DirectedLineSegment{" +
					"x1=" + x1 +
					", y1=" + y1 +
					", x2=" + x2 +
					", y2=" + y2 +
					", direction=" + direction +
					'}';
		}
	}
}