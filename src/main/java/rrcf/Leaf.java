package rrcf;

public class Leaf extends Node {
    public Object index;
    public int depth;

    public Leaf(double[] p, Object i, int d) {
        point = new double[1][p.length];
        point[0] = p;
        index = i;
        depth = d;
        num = 1;
    }
}