package rrcf.memory;

import java.util.BitSet;

import rrcf.memory.ShingledTree.Cut;

public class ShingledBranch extends ShingledNode {
    public Cut cut;
    public ShingledNode left;
    public ShingledNode right;

    // Only stores half of the bounding box at each step
    // The other half is inherited from its parent
    // Each min / max value in each dimension is guaranteed to be shared with either left or right
    // Only the bounding values for the node not containing the min / max is stored
    public double[] childMinPointValues;
    public double[] childMaxPointValues;
    // Where the point values in the double array go (true = left, false = right)
    public BitSet childMinPointDirections;
    public BitSet childMaxPointDirections;

    public ShingledBranch(Cut c, int dimension, ShingledNode l, ShingledNode r, int n) {
        cut = c;
        left = l;
        right = r;
        num = n;

        childMinPointValues = new double[dimension];
        childMaxPointValues = new double[dimension];
        childMinPointDirections = new BitSet();
        childMaxPointDirections = new BitSet();
    }
}