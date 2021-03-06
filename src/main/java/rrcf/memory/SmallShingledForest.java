package rrcf.memory;

import java.util.Deque;
import java.util.Iterator;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;
import java.io.Serializable;

/**
 * NOTE: Doesn't seem to actually matter much in practice
 * This class should probably not be used
 * @deprecated
 */
@Deprecated
public class SmallShingledForest implements Serializable {
    private int shingleSize;
    private int bufferLength;
    private SmallTree[] trees;
    private ArrayDeque<Double> buffer;

    public SmallShingledForest(Random random, int shingleSize, int numTrees, int treeSize) {
        trees = new SmallTree[numTrees];
        assert shingleSize > 1;
        bufferLength = shingleSize + treeSize - 1;
        buffer = new ArrayDeque<>(bufferLength);
        this.shingleSize = shingleSize;
        for (int i = 0; i < numTrees; i++) {
            trees[i] = new SmallTree(random, shingleSize);
        }
    }

    public SmallShingledForest(int shingleSize, int numTrees, int treeSize) {
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
            for (SmallTree tree : trees) {
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
        for (SmallTree tree : trees) {
            SmallLeaf l = tree.insertPoint(lastPoint);
            val += tree.getCollusiveDisplacement(l);
        }
        return val / trees.length;
    }
}