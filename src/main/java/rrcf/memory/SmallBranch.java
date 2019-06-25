package rrcf.memory;

import java.io.Serializable;
import java.util.BitSet;

import rrcf.memory.SmallTree.Cut;

public class SmallBranch extends SmallNode implements Serializable {
    public Cut cut;
    public SmallNode left;
    public SmallNode right;

    public double[] childMinPointValues;
    public double[] childMaxPointValues;
    // Where the point values in the double array go (true = left, false = right)
    public BitSet childMinPointDirections;
    public BitSet childMaxPointDirections;

    public SmallBranch(Cut c, int dimension, SmallNode l, SmallNode r, int n) {
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