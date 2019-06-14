package rrcf.general;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Random;
import java.io.Serializable;

/**
 * WARNING: This class has been superseded by rrcf.optimized.ShingledForest
 * The alternative is much more space-efficient
 * 
 * Represents a forest with support for shingling
 * Rather than adding single multi-dimensional points,
 *  multiple one-dimensional points are added and grouped together
 *  with rolling windows
 */
@Deprecated
public class NaiveShingledForest extends RCForest implements Serializable {
    private int shingleSize;
    private Deque<Double> buffer;

    public NaiveShingledForest(Random random, int shingleSize, int numTrees, int treeSize) {
        super(random, numTrees, treeSize);
        this.shingleSize = shingleSize;
        buffer = new ArrayDeque<>();
    }

    public NaiveShingledForest(int shingleSize, int numTrees, int treeSize) {
        this(new Random(), shingleSize, numTrees, treeSize);
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