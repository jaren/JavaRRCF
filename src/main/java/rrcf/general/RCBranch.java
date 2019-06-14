package rrcf.general;

import java.io.Serializable;

/**
 * Represents a node with two children
 * Represents a branch in a BST on [cutDimension, cutValue]
 */
public class RCBranch extends RCNode implements Serializable {
    public int cutDimension;
    public double cutValue;
    public RCNode left;
    public RCNode right;

    public RCBranch(int dim, double cut, RCNode l, RCNode r, int n) {
        cutDimension = dim;
        cutValue = cut;
        left = l;
        right = r;
        num = n;
    }
}
