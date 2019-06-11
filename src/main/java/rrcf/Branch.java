package rrcf;

public class Branch extends Node {
    public int cutDimension;
    public double cutValue;
    public Node left;
    public Node right;

    public Branch(int dim, double cut, Node l, Node r, int n) {
        cutDimension = dim;
        cutValue = cut;
        left = l;
        right = r;
        num = n;
    }
}
