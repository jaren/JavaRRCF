package rrcf.memory;

public class RCSmallLeaf extends RCSmallNode {
    // Point is stored implicitly by traversing root's bounding box down to the leaf

    public RCSmallLeaf() {
        num = 1;
    }
}