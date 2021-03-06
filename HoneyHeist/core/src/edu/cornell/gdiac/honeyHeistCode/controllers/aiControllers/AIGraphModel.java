package edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.Graph;
import com.badlogic.gdx.ai.pfa.Heuristic;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.honeyHeistCode.models.LevelModel;
import edu.cornell.gdiac.honeyHeistCode.models.PlatformModel;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;

import java.util.HashMap;

public class AIGraphModel implements Graph<AIGraphModel.AINodeModel> {
    Array<AINodeModel> nodes;
    float maxRadiusOfNode;
    int count;

    //DEBUGGING
    TextureRegion whiteSquare;

    public AIGraphModel(JsonValue aIGraphData, TextureRegion whiteSquare) {
        nodes = new Array<AINodeModel>();
        this.whiteSquare = whiteSquare;
        this.maxRadiusOfNode = aIGraphData.getFloat("maxRadiusOfNode");
        float minRadiusOfNode = (float)Math.sqrt(3) * (maxRadiusOfNode/2);
        float xMin = aIGraphData.getFloat("minX");
        float xMax = aIGraphData.getFloat("maxX");
        float yMin = aIGraphData.getFloat("minY");
        float yMax = aIGraphData.getFloat("maxY");
        float yMiddle = (yMax + yMin) / 2;
        boolean evenRow = true;
        for (float y = yMiddle; y <= yMax; y += minRadiusOfNode) {
            evenRow = !evenRow;
            for (float x = evenRow ? (float) (xMin + maxRadiusOfNode * 1.5) : xMin;
                 x <= xMax; x += (maxRadiusOfNode * 3)) {
                addNode(new AINodeModel(x, y, true));
            }
        }
        evenRow = false;
        for (float y = yMiddle - minRadiusOfNode; y >= yMin; y -= minRadiusOfNode) {
            evenRow = !evenRow;
            for (float x = evenRow ? (float) (xMin + maxRadiusOfNode * 1.5) : xMin;
                 x <= xMax; x += (maxRadiusOfNode * 3)) {
                addNode(new AINodeModel(x, y, true));
            }
        }
        for (int i = 0; i < aIGraphData.get("accessibility").size; i++) {
            boolean accessible = aIGraphData.get("accessibility").getInt(i) == 1;
            setAccessibility(i, accessible);
        }
    }

    private void setAccessibility(int index, boolean accessibility) {
        if (index > count) {
            return;
        }
        nodes.get(index).setAccessible(accessibility);
    }

    private AINodeModel createNodeModel(JsonValue nodeData) {
        boolean accessible = nodeData.getFloat(2) == 1;
        return new AINodeModel(nodeData.getFloat(0), nodeData.getFloat(1), accessible);
    }

    public int getNodeCount() {
        return count;
    }

    public Array<Connection<AINodeModel>> getConnections(AINodeModel fromNode) {
        return fromNode.getConnections();
    }

    private void addNode(AINodeModel node) {
        nodes.add(node);
        count++;
    }

    private void addConnection(AINodeModel node1, AINodeModel node2) {
        node1.addConnection(node2);
        node2.addConnection(node1);
    }

    public void drawDebug(GameCanvas canvas, Vector2 drawScale) {
        for (AINodeModel node : nodes) {
            node.drawDebug(canvas, drawScale);
        }
    }

    public void setTextures() {
        for (AINodeModel node : nodes) {
            node.setTexture(whiteSquare);
        }
    }

    /**
     * Node used for pathfinding in AI. Nodes are essentially devisions of the level into squares. Each square is a
     * node in which a character may use as a target to get towards. Each node stores it's position in world coordinates,
     * and whether or not the node is accessible by moving characters.
     *
     * Polygon Obstacle used to debugging purposes.
     */
    public static class AINodeModel{
        private Vector2 position;
        static final float MAX_NODE_RADIUS = .5f;
        static final float MIN_NODE_RADIUS = (float)Math.sqrt(3) * (MAX_NODE_RADIUS/2);
        private boolean accessible;
        Array<Connection<AINodeModel>> connections;
        static final float[] HEXAGON_COORDINATES = getHexagonPoints(MAX_NODE_RADIUS);

        //TempForCollisionCalculations
        DirectedLineSegment temp1;
        DirectedLineSegment temp2;

        // Debugging
        Texture texture;
        PolygonObstacle polygonObstacle;
        Color accessibleColor = new Color(1,1,1,.5f);
        Color inaccessibleColor = new Color(1,0,0,0.5f);


        public AINodeModel(float x, float y, boolean accessible) {
            position = new Vector2(x, y);
            this.accessible = accessible;

            //DEBUGGING
            polygonObstacle = new PolygonObstacle(HEXAGON_COORDINATES, x, y);
            polygonObstacle.setBodyType(BodyDef.BodyType.StaticBody);
            polygonObstacle.setActive(false);

            temp1 = new DirectedLineSegment();
            temp2 = new DirectedLineSegment();
        }

        public float getX() {
            return position.x;
        }

        public float getY() {
            return position.y;
        }

        public Vector2 getPosition() {
            return position;
        }
        /**
         * Returns the distance from this node to the node inputted as a parameter.
         * @param node the other node to measure distance from.
         * @return the distance
         */
        public float getDistance(AINodeModel node) {
            return position.dst(node.getPosition());
        }

        /**
         * Adds a connection to the node. Connections are one way.
         * @param node Node to connect to.
         */
        public void addConnection(AINodeModel node) {
            AIConnectionModel newConnection = new AIConnectionModel(this, node, getDistance(node));
            connections.add(newConnection);
        }

        /**
         * Returns the list of connections that this node has with other nodes.
         * @return list of connections.
         */
        public Array<Connection<AINodeModel>> getConnections() {
            return connections;
        }

        /**
         * Returns whether or not the tile is accessible.
         * A tile is accessible if the node does not overlap with
         * an inaccessible or impassable obstacle (essentially a static body).
         * @return if it is accessible
         */
        public boolean isAccessible() {
            return accessible;
        }

        public void setAccessible(boolean value) {
            accessible = value;
        }

        public boolean doesPolygonIntersectNode(float[] vertices) {
            for (int i = 0; i < HEXAGON_COORDINATES.length; i +=2) {
                int x1NodeIndex = i;
                int y1NodeIndex = i + 1;
                int x2NodeIndex;
                int y2NodeIndex;
                if (i + 2 < vertices.length) {
                    x2NodeIndex = i + 2;
                    y2NodeIndex = i + 3;
                } else {
                    x2NodeIndex = 0;
                    y2NodeIndex = 1;
                }
                temp2.set(vertices[x1NodeIndex] + getX(), vertices[y1NodeIndex] + getY(), vertices[x2NodeIndex] + getX(), vertices[y2NodeIndex] + getY());
                for (int j = 0; j < vertices.length; j += 2) {
                    int x1Index = j;
                    int y1Index = j + 1;
                    int x2Index;
                    int y2Index;
                    if (j + 2 < vertices.length) {
                        x2Index = j + 2;
                        y2Index = j + 3;
                    } else {
                        x2Index = 0;
                        y2Index = 1;
                    }
                    temp2.set(vertices[x1Index], vertices[y1Index], vertices[x2Index], vertices[y2Index]);
                    if (temp1.intersects(temp2)) {
                        return true;
                    }
                }
            }

            return false;
        }

        /*
        DEBUGGING TOOLS TO BE REMOVED LATER
         */

        public void drawDebug(GameCanvas canvas, Vector2 scale) {
            polygonObstacle.setDrawScale(scale);
            if (accessible) {
                polygonObstacle.draw(canvas, accessibleColor);
            }
            else {
                polygonObstacle.draw(canvas, inaccessibleColor);
            }
        }

        private static float[] getHexagonPoints(float maxRadius) {
            float [] result = new float[12];
            result[0] = -maxRadius;
            result[1] = 0;
            result[2] = -(maxRadius / 2);
            result[3] = -(float)Math.sqrt(3) * (maxRadius/2);
            result[4] = (maxRadius / 2);
            result[5] = -(float)Math.sqrt(3) * (maxRadius/2);
            result[6] = maxRadius;
            result[7] = 0;
            result[8] = (maxRadius / 2);
            result[9] = (float)Math.sqrt(3) * (maxRadius/2);
            result[10] = -(maxRadius / 2);
            result[11] = (float)Math.sqrt(3) * (maxRadius/2);
            return result;
        }

        public void setTexture(TextureRegion texture) {
            polygonObstacle.setTexture(texture);
        }

    }

    public static class AIHeuristicModel implements Heuristic<AINodeModel> {
        public float estimate(AINodeModel node, AINodeModel endNode) {
            return node.getDistance(endNode);
        }
    }

    public static class AIConnectionModel implements Connection<AINodeModel> {
        private AINodeModel sourceNode;
        private AINodeModel destinationNode;

        private float cost;

        public AIConnectionModel(AINodeModel sourceNode, AINodeModel destinationNode, float cost) {
            this.sourceNode = sourceNode;
            this.destinationNode = destinationNode;
            this.cost = cost;
        }

        public float getCost() {
            return cost;
        }

        public AINodeModel getFromNode() {
            return sourceNode;
        }

        public AINodeModel getToNode() {
            return destinationNode;
        }
    }
}
