package rrcf;

import java.util.Deque;
import java.util.ArrayDeque;
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

    public ShingledForest(int shingleSize, int numTrees, int treeSize) {
        super(numTrees, treeSize);
        this.shingleSize = shingleSize;
        buffer = new ArrayDeque<>();
    }

    public double addPoint(double value) {
        buffer.addLast(value);
        if (buffer.size() <= shingleSize) {
            return 0;
        } else {
            buffer.removeFirst();
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