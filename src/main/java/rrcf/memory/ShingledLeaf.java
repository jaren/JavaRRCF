package rrcf.memory;

public class ShingledLeaf extends ShingledNode {
    // Point is stored implicitly by traversing root's bounding box down to the leaf

    public ShingledLeaf() {
        num = 1;
    }
}