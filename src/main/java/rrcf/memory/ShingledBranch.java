package rrcf.memory;

import rrcf.memory.ShingledTree.Cut;

public class ShingledBranch extends ShingledNode {
    public Cut cut;
    public ShingledNode left;
    public ShingledNode right;

    public ShingledBranch(Cut c, ShingledNode l, ShingledNode r, int n) {
        cut = c;
        left = l;
        right = r;
        num = n;
    }
}