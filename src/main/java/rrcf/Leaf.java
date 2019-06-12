package rrcf;

/**
 * Represents a node with no children
 * Stores a single point or duplicate points
 */
public class Leaf extends Node {
    // How to access this leaf in the tree
    // Only stored for printing
    // TODO: Can probably remove this later
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