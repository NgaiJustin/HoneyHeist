package edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers.aiModels;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.ai.pfa.Connection;
import edu.cornell.gdiac.honeyHeistCode.GameCanvas;
import edu.cornell.gdiac.honeyHeistCode.obstacle.PolygonObstacle;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Node used for pathfinding in AI. Nodes are essentially devisions of the level into squares. Each square is a
 * node in which a character may use as a target to get towards. Each node stores it's position in world coordinates,
 * and whether or not the node is accessible by moving characters.
 *
 * Polygon Obstacle used to debugging purposes.
 */
public class AINodeModel{
    private Vector2 position;
    static final float MAX_NODE_RADIUS = .5f;
    static final float MIN_NODE_RADIUS = (float)Math.sqrt(3) * (MAX_NODE_RADIUS/2);
    private boolean accessible;
    Array<Connection<AINodeModel>> connections;

    // Debugging
    Texture texture;
    PolygonObstacle polygonObstacle;

    //DEBUGGER
    static final float[] HEXAGON_COORDINATES = getHexagonPoints(MAX_NODE_RADIUS);

    public AINodeModel(float x, float y, boolean accessible) {
        position = new Vector2(x, y);
        this.accessible = accessible;

        //DEBUGGING
        polygonObstacle = new PolygonObstacle(HEXAGON_COORDINATES, x, y);
        polygonObstacle.setBodyType(BodyDef.BodyType.StaticBody);
        polygonObstacle.setActive(false);
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

    /*
    DEBUGGING TOOLS TO BE REMOVED LATER
     */

    public void drawDebug(GameCanvas canvas, Vector2 scale) {
        polygonObstacle.setDrawScale(scale);
        if (accessible) {
            polygonObstacle.draw(canvas, Color.WHITE);
        }
        else {
            polygonObstacle.draw(canvas, Color.RED);
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