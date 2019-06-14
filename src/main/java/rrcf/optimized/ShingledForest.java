package rrcf.optimized;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Random;
import java.io.Serializable;

/**
 * Memory-optimized version of ShingledForest
 * - Stores a shared buffer of all points for all trees so trees don't duplicate data
 * - Uses the shared buffer of points to feed into each individual leaf, which only needs to store a start index
 * - Doesn't store bounding boxes, only storing a delta at each branch (cut dim/value since bounding boxes only change by one item each time)
 */
public class ShingledForest extends RCForest implements Serializable {
    private int treeSize;
    private ShingledTree[] trees;
    private BoundedBuffer<Double> buffer;

    public ShingledForest(Random random, int shingleSize, int numTrees, int treeSize) {
        trees = new ShingledTree[numTrees];
        buffer = new BoundedBuffer<>(shingleSize + treeSize);
        for (int i = 0; i < numTrees; i++) {
            trees[i] = new ShingledTree(random, shingleSize);
        }
    }

    public ShingledForest(int shingleSize, int numTrees, int treeSize) {
        this(new Random(), shingleSize, numTrees, treeSize);
    }

    /**
     * Adds a point to the forest
     * @param value
     * @return Average collusive displacement from point insertion
     */
    public double addPoint(double value) {
        buffer.addLast(value);
        if (buffer.size() <= shingleSize) {
            return 0;
        } else {
            buffer.removeFirst();
            for (ShingledTree tree : trees) {
                tree.insertPoint(value);
            }
        }
    }
}