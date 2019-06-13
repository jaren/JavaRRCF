package rrcf;

import java.io.Serializable;
import java.util.Random;

/**
 * Represents a collection of trees
 * Handles point addition and score averaging
 */
public class Forest implements Serializable {
    private Tree[] trees;
    private int treeSize;
    private int currentIndex;
    
    public Forest(Random random, int numTrees, int size) {
        trees = new Tree[numTrees];
        for (int i = 0; i < numTrees; i++) {
            trees[i] = new Tree(random);
        }
        currentIndex = 0;
        treeSize = size;
    }

    public Forest(int numTrees, int size) {
        this(new Random(), numTrees, size);
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