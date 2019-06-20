package rrcf.memory;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;
import java.io.Serializable;

/**
 * Memory-optimized version of ShingledForest
 * - Stores a shared buffer of all points for all trees so trees don't duplicate data
 * - Uses the shared buffer of points to feed into each individual leaf, which only needs to store a start index
 * - Doesn't store bounding boxes, only storing a delta at each branch (cut dim/value since bounding boxes only change by one item each time)
 */
public class ShingledForest implements Serializable {
    private int shingleSize;
    // TODO: Change to private
    public ShingledTree[] trees;
    private BoundedBuffer<Double> buffer;

    public ShingledForest(Random random, int shingleSize, int numTrees, int treeSize) {
        trees = new ShingledTree[numTrees];
        assert shingleSize > 1;
        buffer = new BoundedBuffer<>(shingleSize + treeSize - 1);
        this.shingleSize = shingleSize;
        for (int i = 0; i < numTrees; i++) {
            trees[i] = new ShingledTree(random, buffer, shingleSize);
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
        if (buffer.full()) {
            long oldestPoint = buffer.streamStartIndex();
            for (ShingledTree tree : trees) {
                tree.forgetPoint(oldestPoint);
            }
        }
        long index = buffer.add(value);
        double val = 0;
        if (buffer.size() < shingleSize) {
            return 0;
        }
        long startIndex = index - shingleSize + 1;
        for (ShingledTree tree : trees) {
            ShingledLeaf l = tree.insertPoint(startIndex);
            val += tree.getCollusiveDisplacement(l);
        }
        return val / trees.length;
    }
}