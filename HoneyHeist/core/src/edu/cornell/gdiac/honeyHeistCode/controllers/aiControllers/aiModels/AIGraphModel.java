package edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers.aiModels;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.ai.pfa.Graph;
import com.badlogic.gdx.ai.pfa.Connection;
import edu.cornell.gdiac.honeyHeistCode.models.LevelModel;
import edu.cornell.gdiac.honeyHeistCode.models.PlatformModel;
import java.util.HashMap;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AIGraphModel implements Graph<AINodeModel> {
    LevelModel levelModel;
    HashMap<Vector2, AINodeModel> nodes;
    int count;

    //DEBUGGING
    TextureRegion whiteSquare;

    public AIGraphModel(LevelModel levelModel, TextureRegion whiteSquare) {
        this.levelModel = levelModel;
        nodes = new HashMap<Vector2, AINodeModel>();
        this.whiteSquare = whiteSquare;
        boolean evenRow = false;
        for (float y = levelModel.getBounds().y; y < levelModel.getBounds().height; y += (AINodeModel.MIN_NODE_RADIUS)) {
            evenRow = !evenRow;

            for (float x = evenRow ? (float) (levelModel.getBounds().x + AINodeModel.MAX_NODE_RADIUS * 1.5) : levelModel.getBounds().x;
                 x < levelModel.getBounds().width; x += (AINodeModel.MAX_NODE_RADIUS * 3)){
                addNode(new AINodeModel(x,y, true));
            }
        }
    }

    public void updateAccessibility() {
         PlatformModel platformModel = levelModel.getPlatforms();
    }

    public int getNodeCount () {
        return count;
    }

    public Array<Connection<AINodeModel>> getConnections (AINodeModel fromNode) {
        return fromNode.getConnections();
    }

    private void addNode(AINodeModel node) {
        nodes.put(node.getPosition(), node);
        count ++;
    }

    private void addConnection(AINodeModel node1, AINodeModel node2) {
        node1.addConnection(node2);
        node2.addConnection(node1);
    }

    public void drawDebug(GameCanvas canvas, Vector2 drawScale) {
        for (AINodeModel node : nodes.values()) {
            node.drawDebug(canvas, drawScale);
        }
    }

    public void setTextures() {
        for (AINodeModel node : nodes.values()) {
            node.setTexture(whiteSquare);
        }
    }
}


