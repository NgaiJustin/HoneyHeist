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
import edu.cornell.gdiac.honeyHeistCode.models.AbstractBeeModel;
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
    private float wanderRadius;
    private float wanderSpeedFactor;
    private float chaseSpeedFactor;
    private boolean checkIfItWillFallOffPlatform;

    private FSMState state;
    private long ticks;
    private long ticksSinceLastChangeInDirection;
    private int ticksBeforeChangeInRandomDirection;
	private static final float checkLength = 1.5f;
    private Vector2 target;
    private DirectedLineSegment lineToTarget;
    private DirectedLineSegment tempLineSegment;
    private DirectedLineSegment bottomChecker;
    private DirectedLineSegment poisonChecker;
    private DirectedLineSegment direction;
    private Vector2 positionAtLastWander;
    private Vector2 currentDirection;
    private Vector2 temp;
	private int ticksUntilChangeMustOccur;

    Random random = new Random();

    /**
	 * Creates an AI Controller for the given enemy model.
	 *
	 * This is the class that uses the information stored in the "ai_controller_options" in the json.
	 * Currently, "ai_controller_options" has 5 parameters.
	 * "enemy_type": is the type of enemy. It currently supports two types.
	 * 		Flying enemy (represented by putting in "0") and grounded enemy (represented by putting in "1").
	 * "chase_distance": The distance that the enemy needs to be to it's target in order to chase the target.
	 * "target": The target that the enemy AI has. Now it only has one option (the player, which is represented by putting in "0")
	 * "wander_speed_factor": The factor in which speed is multiplied during the wander phase.
	 * "chase_speed_factor": The factor in which speed is multiplied during the chase phase.
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
		this.checkIfItWillFallOffPlatform = data.getInt("check_fall_off_platform") == 1 ? true : false;
		this.wanderRadius = data.getFloat("wander_radius");
		this.ticksBeforeChangeInRandomDirection = data.getInt("ticksBeforeChangeInRandomDirection");
		this.ticksUntilChangeMustOccur = data.getInt("ticksUntilChangeMustOccur");

		state = FSMState.WANDER;
		lineToTarget = new DirectedLineSegment(controlledCharacter.getPosition(), target);
		tempLineSegment = new DirectedLineSegment();
		bottomChecker = new DirectedLineSegment();
		poisonChecker = new DirectedLineSegment();
		direction = new DirectedLineSegment();
		temp = new Vector2();
		currentDirection = new Vector2();
		positionAtLastWander = new Vector2();
		positionAtLastWander.set(controlledCharacter.getPosition());
		ticksSinceLastChangeInDirection = 0;
		ticks = 0;
		initDirection();
	}

	/**
	 * Updates the state of the AI Controller
	 */
	public void updateAIController() {
		updateLineToTarget();
		updateFSMState();
		updateDirectionBasedOnState();
		ticksSinceLastChangeInDirection ++;
		ticks ++;
	}

	/**
	 * Returns the direction which the controlled enemy should move.
	 *
	 * @return The direction that the enemy should move.
	 */
	public Vector2 getMovementDirection() {
		currentDirection.set(direction.getDirection());
		currentDirection.nor();
		if (characterType == CharacterType.GROUNDED_CHARACTER) {
			currentDirection.set(setVectorToLeftOrRight(currentDirection));
		}
		if (state == FSMState.WANDER) {
			return currentDirection.scl(wanderSpeedFactor);
		}
		else {
			return currentDirection.scl(chaseSpeedFactor);
		}
	}


	/**
	 * Returns the horizontal direction which the controlled enemy should move.
	 *
	 * @return The direction that the enemy should move.
	 */
	private Vector2 setVectorToLeftOrRight(Vector2 direction) {
		temp.set(direction);
		if (temp.x > 0) {
			return temp.setAngleDeg(0);
		} else if (temp.x == 0) {
			return Vector2.Zero;
		} else {
			return temp.setAngleDeg(180);
		}
	}




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
		if (levelModel.getPlatforms().isRotating()) {
			updatePositionAtLastWander();
		}
		switch (this.state) {
			case WANDER:
				if (distanceToPlayer < chaseRadius && !isLineCollidingWithAPlatform(lineToTarget)) {
					if (characterType == CharacterType.FLYING_CHARACTER) {
						this.state = FSMState.CHASE;
					} else {
						if (Math.abs(lineToTarget.getDirection().y) < .75f) {
							this.state = FSMState.CHASE;
						}
					}
				}
				break;
			case CHASE:
				if (distanceToPlayer > chaseRadius || isLineCollidingWithAPlatform(lineToTarget)) {
					this.state = FSMState.WANDER;
					controlledCharacter.haltMovement();
					updatePositionAtLastWander();
				}
		}
		boolean isChasing = this.state == FSMState.CHASE;
		((AbstractBeeModel) controlledCharacter).setIsChasing(isChasing);
	}

	private void updatePositionAtLastWander() {
		positionAtLastWander.set(controlledCharacter.getPosition());
	}

	/**
	 * Updates the direction that the AI Controller has decided based on the state.
	 */
	private void updateDirectionBasedOnState() {
		switch (this.state) {
			case WANDER:
				if (characterType == CharacterType.GROUNDED_CHARACTER) {
					rotateWanderDirectionForGroundedEnemy((float)Math.toDegrees(controlledCharacter.getAngle()));
					wanderDirectionForGroundedEnemy();
				} else {
					wanderDirectionForFlyingEnemy();
				}
				direction.setByVector(controlledCharacter.getPosition(),direction.getDirection().nor().scl(checkLength));
				break;

			case CHASE:
				setDirectionToGoTowardsTarget();
				break;
		}
	}

	private void initDirection() {
		if (characterType == CharacterType.GROUNDED_CHARACTER) {
			setDirectionToRandomHorizontal();
		}
		else {
			setDirectionToRandom12();
		}
	}

	private void rotateWanderDirectionForGroundedEnemy(float rotation) {
		if (controlledCharacter.getMovement() <= 0) {
			rotation = 180 + rotation;
		}
		direction.setByVector(controlledCharacter.getPosition(), direction.getDirection().setAngleDeg(rotation));
	}

	private void wanderDirectionForGroundedEnemy() {
		direction.setByVector(controlledCharacter.getPosition(), direction.getDirection());
		if (ticksSinceLastChangeInDirection >= ticksBeforeChangeInRandomDirection && isCharacterGroundedOnSlantedPlatform()) {
			if (isLineCollidingWithAPlatform(direction)) {
				changeToOppositeDirection();
				ticksSinceLastChangeInDirection = 0;
			}
			if (checkIfItWillFallOffPlatform && willCharacterFallOffPlatform()) {
				changeToOppositeDirection();
				ticksSinceLastChangeInDirection = 0;
			}
			if (ticksSinceLastChangeInDirection >= ticksUntilChangeMustOccur && !controlledCharacter.isInHoney()) {
				changeToOppositeDirection();
				ticksSinceLastChangeInDirection = 0;
			}
		}

	}

	private boolean isCharacterGroundedOnSlantedPlatform() {
		return controlledCharacter.isGrounded() && (Math.toDegrees(controlledCharacter.getAngle()) != 0 || Math.toDegrees(controlledCharacter.getAngle()) != 180);
	}

	private boolean isExitingWanderRadius() {
		return controlledCharacter.getPosition().dst(positionAtLastWander) >= wanderRadius;
	}

	private void wanderDirectionForFlyingEnemy() {
		direction.setByVector(controlledCharacter.getPosition(), direction.getDirection());
		if (ticks % ticksBeforeChangeInRandomDirection == 0) {
			setDirectionToRandom12();
			for (int i = 0; i < 5; i++) {
				if (isLineCollidingWithAPlatform(direction)) {
					setDirectionToRandom12();
				}
			}
		}
		if (isLineCollidingWithAPoisonPlatform(direction)) {
			changeToOppositeDirection();
			positionAtLastWander.add(direction.getDirection());
		}
		else if (isLineCollidingWithAPlatform(direction)) {
			getViableDirection12();
		}
		else if (isExitingWanderRadius() && !controlledCharacter.isInHoney()) {
			direction.set(controlledCharacter.getPosition(), positionAtLastWander);
			controlledCharacter.haltMovement();
		}
		else if (controlledCharacter.isInHoney()) {
			updatePositionAtLastWander();
		}

	}

	/**
	 * Sets the direction vector to be a random direction.
	 */
	private void setDirectionToRandomDirection() {
		int angle = random.nextInt(360);
		temp.set(checkLength,0);
		temp.setAngleDeg(angle);
		temp.nor();
		direction.setByVector(controlledCharacter.getPosition(), temp.scl(chaseSpeedFactor));
	}

	/**
	 * Sets the direction vector to be a random direction limited to 4 cardinal directions.
	 */
	private void setDirectionToRandom4CardinalDirection() {
		int angle = random.nextInt(4);
		temp.set(1,0);
		temp.setAngleDeg(angle * 90);
		temp.nor();
		direction.setByVector(controlledCharacter.getPosition(), temp);
	}

	/**
	 * Sets the direction vector to be a random direction limited to 12 cardinal directions each sepearated by 30 degree increments.
	 */
	private void setDirectionToRandom12() {
		int angle = random.nextInt(12);
		temp.set(1,0);
		temp.setAngleDeg(angle * 30);
		temp.nor();
		direction.setByVector(controlledCharacter.getPosition(), temp);
	}

	private void getViableDirection12() {
		temp.set(1,0).scl(checkLength);
		for (int i = 0; i < 360; i += 30) {
			temp.setAngleDeg(i * 30);
			direction.setByVector(controlledCharacter.getPosition(), temp);
			if (!isLineCollidingWithAPlatform(direction)) {
				break;
			}
		}
	}

	private void setDirectionToRandomHorizontal() {
		int fiftyFifty = random.nextInt(2);
		if (fiftyFifty > 0) {
			temp.set(1, 0);
		} else {
			temp.set(-1, 0);
		}
		direction.setByVector(controlledCharacter.getPosition(), temp);
	}

	private void changeToOppositeDirection() {
		temp.set(direction.getDirection());
		temp.scl(-1);
		direction.setByVector(controlledCharacter.getPosition(), temp);
	}

	/**
	 * Set the direction vector to go towards the specified target.
	 */
	private void setDirectionToGoTowardsTarget() {
		temp.set(lineToTarget.getDirection());
		if (characterType == CharacterType.GROUNDED_CHARACTER) {
			if (temp.x >= 0) {
				temp.set(1,0);
			} else {
				temp.set(-1,0);
			}
		}
		temp.nor();
		direction.setByVector(controlledCharacter.getPosition(),temp);
	}

	private boolean willCharacterCollideWithAPoisonPlatform(DirectedLineSegment line) {
		temp.set(line.getDirection());
		float angle = temp.angleDeg();
		for (int i = -10; i < 10; i+= 5) {
			temp.setAngleDeg(angle + i);
			poisonChecker.setByVector(controlledCharacter.getPosition(), temp);
			if (isLineCollidingWithAPoisonPlatform(poisonChecker)) {
				return true;
			}
		}
		return false;
	}

	private boolean willCharacterFallOffPlatform() {
		temp.set(0,-1);
		Vector2 position = controlledCharacter.getPosition();
		bottomChecker.setByVector(position.x + direction.getDirection().x, position.y, temp.x, temp.y);
		return !isLineCollidingWithAPlatform(bottomChecker);
	}

	private boolean isLineCollidingWithAPoisonPlatform(DirectedLineSegment line) {
		PlatformModel poisonPlatforms = levelModel.getSpikedPlatforms();
		for (PolygonObstacle platform : poisonPlatforms.getBodies()) {
			if (doesPolygonIntersectLine(line, platform.getTrueVertices())) {
				return true;
			}
		}
		return false;
	}



	/**
	 * Checks if the given path to target is blocked by a platform.
	 * @return
	 */
    private boolean isLineCollidingWithAPlatform(DirectedLineSegment line) {
		PlatformModel platforms = levelModel.getPlatforms();
		for (PolygonObstacle platform : platforms.getBodies()) {
			if (doesPolygonIntersectLine(line, platform.getTrueVertices())) {
				return true;
			}
		}
		return willCharacterCollideWithAPoisonPlatform(line);
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

		if (isLineCollidingWithAPlatform(direction)) {
			gameCanvas.drawLine(Color.RED, direction.x1, direction.y1, direction.x2, direction.y2, scale.x, scale.y);
		}
		else {
			gameCanvas.drawLine(Color.BLUE, direction.x1, direction.y1, direction.x2, direction.y2, scale.x, scale.y);
		}

		if(willCharacterFallOffPlatform()){
			gameCanvas.drawLine(Color.RED, bottomChecker.x1, bottomChecker.y1, bottomChecker.x2, bottomChecker.y2, scale.x, scale.y);
		}
		else {
			gameCanvas.drawLine(Color.GREEN, bottomChecker.x1, bottomChecker.y1, bottomChecker.x2, bottomChecker.y2, scale.x, scale.y);
		}

		gameCanvas.drawCircle(wanderRadius, Color.CYAN, positionAtLastWander.x, positionAtLastWander.y, scale.x, scale.y);

	}

}