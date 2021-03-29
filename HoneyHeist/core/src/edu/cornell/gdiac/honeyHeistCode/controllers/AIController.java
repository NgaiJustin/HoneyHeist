/**
The AI Controller for HoneyHeist. 

All enemies which need to interact with it's enviorment (specifically the player)
will have an AI Controller assigned to them.

It is dependent on Level Model in order for the character to get information about the level.
 */
package edu.cornell.gdiac.honeyHeistCode.controllers;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.honeyHeistCode.models.ChaserBeeModel;
import edu.cornell.gdiac.honeyHeistCode.models.LevelModel;
import edu.cornell.gdiac.honeyHeistCode.models.PlatformModel;
import edu.cornell.gdiac.honeyHeistCode.obstacle.Obstacle;

import java.util.Random;


public class AIController {
    /**
	 * Enumeration to encode the finite state machine.
	 */
	private static enum FSMState {
		/** The enemy is patrolling around since it is too far from the player */
		WANDER,
		/** The enemy is near the player and is trying to chace the player*/
		CHASE
	}

    private ChaserBeeModel controlledEnemy;
    private LevelModel levelModel;
    private FSMState state;
    private long ticks;
    private Vector2 target;
    private Vector2 lineToTarget;
    private Vector2 direction;

    Random random = new Random();

    private static final float CHASE_RADIUS = 150f;

    /**
	 * Creates an AI Controller for the given enemy model
	 *
	 * @param levelModel the level that the enemy is in.
	 * @param controlledEnemy the enemy that this AI Controller controlls.
	 */
	public AIController(LevelModel levelModel, Vector2 target, ChaserBeeModel controlledEnemy) {
		this.levelModel = levelModel;
		this.target = target;
        this.controlledEnemy = controlledEnemy;
		state = FSMState.WANDER;
		ticks = 0;
        lineToTarget = new Vector2();
        direction = new Vector2();
	}

	public void update() {
		updateLineToPlayer();
		updateFSMState();
		updateDirectionBasedOnState();
	}

	/**
	 * Returns the direction which the controlled enemy should move.
	 *
	 * @return The direction that the enemy should move.
	 */
	public Vector2 getMovementDirection() {
		return direction;
	}

    private void updateLineToPlayer() {
        lineToTarget.set(levelModel.getPlayer().getPosition());
        lineToTarget.sub(controlledEnemy.getPosition());
    }

	private void updateFSMState() {
		float distanceToPlayer = controlledEnemy.getPosition().dst(levelModel.getPlayer().getPosition());
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

	private void updateDirectionBasedOnState() {
		switch (this.state) {
			case WANDER:
				setDirectionToRandomDirection();
				break;

			case CHASE:
				setDirectionToGoTowardsTarget();
				break;
		}
	}

	/**
	 * Sets the direction vector to be a random direction.
	 */
	private void setDirectionToRandomDirection() {
		int angle = random.nextInt(360);
		direction.setAngleDeg(angle);
		direction.nor();
	}

	/**
	 * Set the direction vector to go towards the specified target.
	 */
	private void setDirectionToGoTowardsTarget() {
		direction.set(lineToTarget);
		direction.nor();
	}

    private boolean isLineCollidingWithAPlatform() {
		PlatformModel platform = levelModel.getPlatforms();
		for (Obstacle platforms : platform.getBodies()) {

		}
		return false;
    }

}