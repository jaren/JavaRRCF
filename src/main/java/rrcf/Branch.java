package rrcf;

import java.io.Serializable;

/**
 * Represents a node with two children
 * Represents a branch in a BST on [cutDimension, cutValue]
 */
public class Branch extends Node implements Serializable {
    public int cutDimension;
    public double cutValue;
    public Node left;
    public Node right;

    public Branch(int dim, double cut, Node l, Node r, int n) {
        cutDimension = dim;
        cutValue = cut;
        left = l;
        right = r;
        num = n;
    }
}
