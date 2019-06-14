package rrcf.general;

import java.io.Serializable;
import java.util.Random;

/**
 * Represents a collection of trees
 * Handles point addition and score averaging
 */
public class RCForest implements Serializable {
    private RCTree[] trees;
    private int treeSize;
    private int currentIndex;
    
    public RCForest(Random random, int numTrees, int size) {
        trees = new RCTree[numTrees];
        for (int i = 0; i < numTrees; i++) {
            trees[i] = new RCTree(random);
        }
        currentIndex = 0;
        treeSize = size;
    }

    public RCForest(int numTrees, int size) {
        this(new Random(), numTrees, size);
    }

    public float addPoint(float[] point) {
        float accum = 0;
        for (RCTree t : trees) {
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