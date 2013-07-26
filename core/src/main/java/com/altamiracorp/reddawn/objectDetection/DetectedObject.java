package com.altamiracorp.reddawn.objectDetection;

public class DetectedObject {

    private int x1;
    private int y1;
    private int x2;
    private int y2;

    public DetectedObject (int x1, int y1, int x2, int y2) {
        setX1(x1);
        setY1(y1);
        setX2(x2);
        setY2(y2);
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    public String[] getCoordStrings() {
        String [] coords = new String [4];
        coords[0] = Integer.toString(getX1());
        coords[1] = Integer.toString(getY1());
        coords[2] = Integer.toString(getX2());
        coords[3] = Integer.toString(getY2());

        return coords;
    }
}
