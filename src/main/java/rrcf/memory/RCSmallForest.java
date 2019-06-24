package rrcf.memory;

import java.util.Deque;
import java.util.Iterator;
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
public class RCSmallForest implements Serializable {
    private int shingleSize;
    private int bufferLength;
    private RCSmallTree[] trees;
    private ArrayDeque<Double> buffer;

    public RCSmallForest(Random random, int shingleSize, int numTrees, int treeSize) {
        trees = new RCSmallTree[numTrees];
        assert shingleSize > 1;
        bufferLength = shingleSize + treeSize - 1;
        buffer = new ArrayDeque<>(bufferLength);
        this.shingleSize = shingleSize;
        for (int i = 0; i < numTrees; i++) {
            trees[i] = new RCSmallTree(random, shingleSize);
        }
    }

    public RCSmallForest(int shingleSize, int numTrees, int treeSize) {
        this(new Random(), shingleSize, numTrees, treeSize);
    }

    @Override
    public String toString() {
        String[] vals = new String[trees.length];
        for (int i = 0; i < trees.length; i++) {
            vals[i] = trees[i].toString();
        }
        return String.join("\n", vals);
    }

    private double[] getFirstPoint() {
        return iteratorToShingle(buffer.iterator(), false);
    }

    private double[] getLastPoint() {
        return iteratorToShingle(buffer.descendingIterator(), true);
    }

    private double[] iteratorToShingle(Iterator<Double> it, boolean reverse) {
        double[] arr = new double[shingleSize];
        for (int i = 0; i < shingleSize; i++) {
            int insert = i;
            if (reverse) {
                insert = shingleSize - 1 - i;
            }
            arr[insert] = it.next();
        }
        return arr;
    }

    /**
     * Adds a point to the forest
     * @param value
     * @return Average collusive displacement from point insertion
     */
    public double addPoint(double value) {
        if (buffer.size() == bufferLength) {
            double[] oldestPoint = getFirstPoint();
            for (RCSmallTree tree : trees) {
                tree.forgetPoint(oldestPoint);
            }
            buffer.removeFirst();
        }
        buffer.addLast(value);
        if (buffer.size() < shingleSize) {
            return 0;
        }
        double val = 0;
        double[] lastPoint = getLastPoint();
        for (RCSmallTree tree : trees) {
            RCSmallLeaf l = tree.insertPoint(lastPoint);
            val += tree.getCollusiveDisplacement(l);
        }
        return val / trees.length;
    }
}