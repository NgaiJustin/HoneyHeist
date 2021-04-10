package edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers;

import com.badlogic.gdx.utils.BinaryHeap;
import edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers.aiModels.AIConnectionModel;
import edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers.aiModels.AIGraphModel;
import edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers.aiModels.AINodeModel;
import java.util.HashMap;
import com.badlogic.gdx.math.Vector2;

public class AStarPathFinderController  {
    AIGraphModel graph;
    HashMap<Vector2, NodeRecord> discoveredNodes;
    private int searchId;
    NodeRecord current;


    private static final int UNVISITED = 0;
    private static final int OPEN = 1;
    private static final int CLOSED = 2;


    /**
     * This nested class is used to keep track of the information we need for each node during the search.
     *      Author: davebaol
     *     Type parameters: <N> – Type of node
     */

    static class NodeRecord extends BinaryHeap.Node {
        /** The reference to the node. */
        AINodeModel node;

        /** The incoming connection to the node */
        AIConnectionModel connection;

        /** The actual cost from the start node. */
        float costSoFar;

        /** The node category: {@link #UNVISITED}, {@link #OPEN} or {@link #CLOSED}. */
        int category;

        /** ID of the current search. */
        int searchId;

        /** Creates a {@code NodeRecord}. */
        public NodeRecord () {
            super(0);
        }

        /** Returns the estimated total cost. */
        public float getEstimatedTotalCost () {
            return getValue();
        }
    }

}
