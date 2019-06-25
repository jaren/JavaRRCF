package rrcf.memory;

import java.io.Serializable;

@Deprecated
public class SmallLeaf extends SmallNode implements Serializable {
    // Point is stored implicitly by traversing root's bounding box down to the leaf

    public SmallLeaf() {
        num = 1;
    }
}