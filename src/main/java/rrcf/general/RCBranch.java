package rrcf.general;

import java.io.Serializable;

import rrcf.general.RCTree.Cut;
 
/* 
 * Represents a node with two children
 * Represents a branch in a BST on [cutDimension, cutValue]
 */
public class RCBranch extends RCNode implements Serializable {
    public Cut cut;
    public RCNode left;
    public RCNode right;

    public RCBranch(Cut c, RCNode l, RCNode r, int n) {
        cut = c;
        left = l;
        right = r;
        num = n;
    }
}
