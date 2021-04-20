package edu.cornell.gdiac.honeyHeistCode.controllers.aiControllers;

import com.badlogic.gdx.math.Vector2;

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

    public DirectedLineSegment(float x1, float y1, float x2, float y2) {
        this();
        set(x1, y1, x2, y2);
    }

    public DirectedLineSegment(Vector2 startPoint, Vector2 endPoint) {
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

    public void set(DirectedLineSegment line) {
        set(line.x1, line.y1, line.x2, line.y2);
    }

    public void setByVector(float x1, float y1, float xV2, float yV2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x1 + xV2;
        this.y2 = y1 + yV2;
        direction.set((x2 - x1), (y2 - y1));
    }

    public void setByVector(Vector2 startPoint, Vector2 vector) {
        setByVector(startPoint.x, startPoint.y, vector.x, vector.y);
    }

    public float dst() {
        return (float) Math.sqrt(Math.pow((x2 - x1), 2) + Math.pow((y2 - y1), 2));
    }

    public Vector2 getDirection() {
        return direction;
    }

    public boolean intersects(DirectedLineSegment line) {
        int dir1 = orientation(this.x1, this.y1, this.x2, this.y2, line.x1, line.y1);
        int dir2 = orientation(this.x1, this.y1, this.x2, this.y2, line.x2, line.y2);
        int dir3 = orientation(line.x1, line.y1, line.x2, line.y2, this.x1, this.y1);
        int dir4 = orientation(line.x1, line.y1, line.x2, line.y2, this.x2, this.y2);

        if (dir1 != dir2 && dir3 != dir4) {
            return true;
        }
        return false;
    }

    private int orientation(float x1, float y1, float x2, float y2, float x3, float y3) {
        float val = (y2 - y1) * (x3 - x2) - (x2 - x1) * (y3 - y2);
        if (val == 0) {
            return 0;
        } else if (val < 0) {
            return -1;
        } else {
            return 1;
        }
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
