package rrcf.memory;

import java.util.BitSet;

import rrcf.memory.ShingledTree.Cut;

public class ShingledBranch extends ShingledNode {
    public Cut cut;
    public ShingledNode left;
    public ShingledNode right;

    public double[] childMinPointValues;
    public double[] childMaxPointValues;
    // Where the point values in the double array go (true = left, false = right)
    public BitSet childMinPointDirections;
    public BitSet childMaxPointDirections;

    public ShingledBranch(Cut c, ShingledNode l, ShingledNode r, int n) {
        cut = c;
        left = l;
        right = r;
        num = n;
    }
}