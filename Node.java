public class Node {
    public Branch parent;
    public int num;

    public double[][] point;
};

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

public class Leaf extends Node {
    public Object index;
    public int depth;

    public Leaf(double[] p, Object i, int d) {
        point = p;
        index = i;
        depth = d;
    }

    public void setPoint(double[] point) {
        minPoint = point;
        maxPoint = point;
    }
}