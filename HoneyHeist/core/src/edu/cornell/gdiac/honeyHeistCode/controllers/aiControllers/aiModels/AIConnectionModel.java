package edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers.aiModels;
import com.badlogic.gdx.ai.pfa.Connection;

public class AIConnectionModel implements Connection<AINodeModel>{
    private AINodeModel sourceNode;
    private AINodeModel destinationNode;

    private float cost;

    public AIConnectionModel(AINodeModel sourceNode, AINodeModel destinationNode, float cost) {
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.cost = cost;
    }
    
    public float getCost () {
        return cost;
    }

    public AINodeModel getFromNode () {
        return sourceNode;
    }

    public AINodeModel getToNode () {
        return destinationNode;
    }
}
