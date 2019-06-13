package rrcf;

import java.io.Serializable;

/**
 * Represents a node with no children
 * Stores a single point or duplicate points
 */
public class Leaf extends Node implements Serializable {
    // How to access this leaf in the tree
    // Only stored for printing
    public Object index;
    public int depth;

    public Leaf(double[] p, Object i, int d) {
        point = new double[1][p.length];
        point[0] = p;
        index = i;
        depth = d;
        num = 1;
    }
}