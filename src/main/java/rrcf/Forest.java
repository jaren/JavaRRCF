package rrcf;

public class Forest {
    private Tree[] trees;
    private int treeSize;
    private int currentIndex;

    public Forest(int numTrees, int treeSize) {
        trees = new Tree[numTrees];
        for (int i = 0; i < numTrees; i++) {
            trees[i] = new Tree();
        }
        currentIndex = 0;
    }

    public double addPoint(double[] point) {
        double accum = 0;
        for (Tree t : trees) {
            if (t.size() > treeSize) {
                t.forgetPoint(currentIndex - treeSize);
            }
            t.insertPoint(point, currentIndex);
            accum += t.getCollusiveDisplacement(currentIndex);
        }
        currentIndex++;
        return accum / trees.length;
    }
}