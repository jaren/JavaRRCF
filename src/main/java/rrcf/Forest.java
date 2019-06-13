package rrcf;

import java.io.Serializable;

/**
 * Represents a collection of trees
 * Handles point addition and score averaging
 */
public class Forest implements Serializable {
    private Tree[] trees;
    private int treeSize;
    private int currentIndex;

    public Forest(int numTrees, int size) {
        trees = new Tree[numTrees];
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < numTrees; i++) {
            trees[i] = new Tree(r);
        }
        currentIndex = 0;
        treeSize = size;
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