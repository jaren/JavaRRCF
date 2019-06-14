package rrcf.general;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Random;
import java.io.Serializable;

/**
 * Represents a forest with support for shingling
 * Rather than adding single multi-dimensional points,
 *  multiple one-dimensional points are added and grouped together
 *  with rolling windows
 */
public class SimpleShingledForest extends RCForest implements Serializable {
    private int shingleSize;
    private Deque<Float> buffer;

    public SimpleShingledForest(Random random, int shingleSize, int numTrees, int treeSize) {
        super(random, numTrees, treeSize);
        this.shingleSize = shingleSize;
        buffer = new ArrayDeque<>();
    }

    public SimpleShingledForest(int shingleSize, int numTrees, int treeSize) {
        this(new Random(), shingleSize, numTrees, treeSize);
    }

    public float addPoint(float value) {
        buffer.addLast(value);
        if (buffer.size() <= shingleSize) {
            return 0;
        } else {
            buffer.removeFirst();
            return super.addPoint(unboxArray(buffer.toArray(new Float[buffer.size()])));
        }
    }

    private float[] unboxArray(Float[] arr) {
        float[] output = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            output[i] = arr[i];
        }
        return output;
    }

    @Override
    public float addPoint(float[] value) {
        assert value.length == 1;
        return addPoint(value[0]);
    }
}