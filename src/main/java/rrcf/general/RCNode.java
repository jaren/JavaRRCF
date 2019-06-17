package rrcf.general;

import java.io.Serializable;

/**
 * Base class for nodes in a tree
 */
public class RCNode implements Serializable {
    public RCBranch parent;
    // Represents either number of leaves or number of duplicates in a leaf
    // Representation depends on whether the node is a branch or leaf
    public int num;

    // Represents either a bounding box (length 2) or point (length 1)
    // Representation depends on whether the node is a branch or leaf
    public double[][] point;
}