package rrcf.general;

import java.io.Serializable;

import rrcf.general.Tree.Cut;
 
/* 
 * Represents a node with two children
 * Represents a branch in a BST on [cutDimension, cutValue]
 */
public class Branch extends Node implements Serializable {
    public Cut cut;
    public Node left;
    public Node right;

    public Branch(Cut c, Node l, Node r, int n) {
        cut = c;
        left = l;
        right = r;
        num = n;
    }
}
