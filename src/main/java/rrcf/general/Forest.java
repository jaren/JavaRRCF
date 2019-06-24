package rrcf.general;

import java.io.Serializable;
import java.util.Random;

/**
 * Represents a collection of trees
 * Handles point addition and score averaging
 */
public class Forest implements Serializable {
    public Tree[] trees;
    private int treeSize;
    private int currentIndex;

    public Forest(Random random, int numTrees, int size, double[][] points) {
        trees = new Tree[numTrees];
        for (int i = 0; i < numTrees; i++) {
            trees[i] = new Tree(random, points);
        }
        currentIndex = 0;
        treeSize = size;
    }
    
    public Forest(Random random, int numTrees, int size) {
        this(random, numTrees, size, new double[0][0]);
    }

    public Forest(int numTrees, int size) {
        this(new Random(), numTrees, size);
    }

    @Override
    public String toString() {
        String[] vals = new String[trees.length];
        for (int i = 0; i < trees.length; i++) {
            vals[i] = trees[i].toString();
        }
        return String.join("\n", vals);
    }

    public double getDisplacement(Object key) {
        double accum = 0;
        for (Tree tree : trees) {
            accum += tree.getCollusiveDisplacement(key);
        }
        return accum / trees.length;
    }
    public double addPoint(double[] point) {
        double accum = 0;
        for (Tree t : trees) {
            if (t.size() >= treeSize) {
                t.forgetPoint(currentIndex - treeSize);
            }
            t.insertPoint(point, currentIndex);
            accum += t.getCollusiveDisplacement(currentIndex);
        }
        currentIndex++;
        return accum / trees.length;
    }
}