package rrcf.memory;

import java.util.BitSet;

import rrcf.memory.RCSmallTree.Cut;

public class RCSmallBranch extends RCSmallNode {
    public Cut cut;
    public RCSmallNode left;
    public RCSmallNode right;

    public double[] childMinPointValues;
    public double[] childMaxPointValues;
    // Where the point values in the double array go (true = left, false = right)
    public BitSet childMinPointDirections;
    public BitSet childMaxPointDirections;

    public RCSmallBranch(Cut c, int dimension, RCSmallNode l, RCSmallNode r, int n) {
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