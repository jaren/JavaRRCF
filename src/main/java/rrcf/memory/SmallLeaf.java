package rrcf.memory;

public class SmallLeaf extends SmallNode {
    // Point is stored implicitly by traversing root's bounding box down to the leaf

    public SmallLeaf() {
        num = 1;
    }
}