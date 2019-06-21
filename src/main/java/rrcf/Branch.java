package rrcf;

import java.util.BitSet;

import rrcf.RandomCutTree.Cut;

public class Branch extends Node {
    public Cut cut;
    public Node left;
    public Node right;

    public double[] childMinPointValues;
    public double[] childMaxPointValues;
    // Where the point values in the double array go (true = left, false = right)
    public BitSet childMinPointDirections;
    public BitSet childMaxPointDirections;

    public Branch(Cut c, int dimension, Node l, Node r, int n) {
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