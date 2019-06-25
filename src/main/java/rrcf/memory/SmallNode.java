package rrcf.memory;

import java.io.Serializable;

@Deprecated
public class SmallNode implements Serializable {
    public SmallBranch parent;
    public int num;
}