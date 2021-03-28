/**
The AI Controller for HoneyHeist. 

All enemies which need to interact with it's enviorment (specifically the player)
will have an AI Controller assigned to them.

It is dependent on Level Model in order for the character to get information about the level.
 */
package edu.cornell.gdiac.physics.platform;

import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.physics.obstacle.ComplexObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.obstacle.PolygonObstacle;


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
    private Vector2 lineToPlayer;

    private static final float CHASE_RADIUS = 150f;

    /**
	 * Creates an AI Controller for the given enemy model
	 *
	 * @param levelModel the level that the enemy is in.
	 * @param controlledEnemy the enemy that this AI Controller controlls.
	 */
	public AIController(LevelModel levelModel, ChaserBeeModel controlledEnemy) {
		this.levelModel = levelModel;
        this.controlledEnemy = controlledEnemy;
		state = FSMState.WANDER;
		ticks = 0;
        lineToPlayer = new Vector2();
	}

    private void updateLineToPlayer() {
        lineToPlayer.set(levelModel.getAvatar().getPosition());
        lineToPlayer.sub(controlledEnemy.getPosition());
    }

    public void update() {

	}

	private void updateFSMState() {
//		switch (this.state) {
//			case WANDER:
//				if ()
//		}
	}


    private boolean isLineCollidingWithAPlatform() {
		PlatformModel platform = levelModel.getPlatforms();
		for (Obstacle platforms : platform.getBodies()) {

		}
		return false;
    }
}