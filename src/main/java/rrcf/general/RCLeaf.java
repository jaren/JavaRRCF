package rrcf.general;

import java.io.Serializable;

/**
 * Represents a node with no children
 * Stores a single point or duplicate points
 */
public class RCLeaf extends RCNode implements Serializable {
    public int depth;

    public RCLeaf(double[] p, int d) {
        point = new double[1][p.length];
        point[0] = p;
        depth = d;
        num = 1;
    }
}