package edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers.aiModels;
import com.badlogic.gdx.ai.pfa.Heuristic;

public class AIHeuristicModel implements Heuristic<AINodeModel> {
    public float estimate (AINodeModel node, AINodeModel endNode) {
        return node.getDistance(endNode);
    }
}
