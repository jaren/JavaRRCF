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
    private ShingledTree[] trees;
    private BoundedBuffer<Float> buffer;

    public ShingledForest(Random random, int shingleSize, int numTrees, int treeSize) {
        trees = new ShingledTree[numTrees];
        assert shingleSize > 1;
        buffer = new BoundedBuffer<>(shingleSize + treeSize - 1);
        this.shingleSize = shingleSize;
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
    public float addPoint(float value) {
        if (buffer.size() < shingleSize) {
            buffer.add(value);
            return 0;
        } else {
            if (buffer.full()) {
                ShingledPoint oldestPoint = new ShingledPoint(buffer, buffer.streamStartIndex(), shingleSize);
                for (ShingledTree tree : trees) {
                    tree.forgetPoint(oldestPoint);
                }
            }
            long index = buffer.add(value);
            float val = 0;
            ShingledPoint s = new ShingledPoint(buffer, index - shingleSize, shingleSize);
            for (ShingledTree tree : trees) {
                ShingledLeaf l = tree.insertPoint(s);
                val += tree.getCollusiveDisplacement(l);
            }
            return val / trees.length;
        }
    }
}