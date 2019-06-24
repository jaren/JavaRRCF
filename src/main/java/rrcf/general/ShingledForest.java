package rrcf.general;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;
import java.io.Serializable;

/**
 * Represents a forest with support for shingling
 * Rather than adding single multi-dimensional points,
 *  multiple one-dimensional points are added and grouped together
 *  with rolling windows
 */
public class ShingledForest extends Forest implements Serializable {
    private int shingleSize;
    private Deque<Double> buffer;

    public ShingledForest(Random random, int shingleSize, int numTrees, int treeSize, double[] data) {
        super(random, numTrees, treeSize, shinglePoints(shingleSize, data));
        this.shingleSize = shingleSize;
        buffer = new ArrayDeque<>();
        for (int i = 0; i < shingleSize; i++) {
            int d = data.length - 1 - i;
            if (d < 0) {
                break;
            }

            buffer.addFirst(data[d]);
        }
    }

    public ShingledForest(Random random, int shingleSize, int numTrees, int treeSize) {
        this(random, shingleSize, numTrees, treeSize, new double[0]);
    }

    public ShingledForest(int shingleSize, int numTrees, int treeSize) {
        this(new Random(), shingleSize, numTrees, treeSize);
    }

    private static double[][] shinglePoints(int shingleSize, double[] data) {
        if (data.length < shingleSize) {
            return new double[0][shingleSize];
        }
        double[][] shingled = new double[data.length - shingleSize + 1][shingleSize];
        for (int i = 0; i < data.length - shingleSize + 1; i++) {
            System.arraycopy(data, i, shingled[i], 0, shingleSize);
        }
        return shingled;
    }

    public double addPoint(double value) {
        buffer.addLast(value);
        if (buffer.size() < shingleSize) {
            return 0;
        } else {
            if (buffer.size() > shingleSize) {
                buffer.removeFirst();
            }
            return super.addPoint(unboxArray(buffer.toArray(new Double[buffer.size()])));
        }
    }

    private double[] unboxArray(Double[] arr) {
        double[] output = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            output[i] = arr[i];
        }
        return output;
    }

    @Override
    public double addPoint(double[] value) {
        assert value.length == 1;
        return addPoint(value[0]);
    }
}