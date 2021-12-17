package rrcf.general;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Random;
import java.io.Serializable;

/**
 * Represents a forest with support for shingling
 * now working also with multidimensional points
 */
public class MultiShingledForest extends Forest implements Serializable {
	  private int shingleSize;
    private Deque<Double[]> buffer;

    public MultiShingledForest(Random random, int shingleSize, int numTrees, int treeSize, double[][] data) {
        super(random, numTrees, treeSize, shinglePoints(shingleSize, data));
        this.shingleSize = shingleSize;
        buffer = new ArrayDeque<>();
        for (int i = 0; i < shingleSize; i++) {
            int d = data.length - 1 - i;
            if (d < 0) {
                break;
            }

            buffer.addFirst(doubleToDouble(data[d]));
        }
    }

    public MultiShingledForest(Random random, int shingleSize, int numTrees, int treeSize) {
        this(random, shingleSize, numTrees, treeSize, new double[0][0]);
    }

    public MultiShingledForest(int shingleSize, int numTrees, int treeSize) {
        this(new Random(), shingleSize, numTrees, treeSize);
    }

    private static double[][] shinglePoints(int shingleSize, double[][] data) {
        if (data.length < shingleSize) {
            return new double[0][shingleSize];
        }
        double[][] shingled = new double[data.length - shingleSize + 1][shingleSize];
        for (int i = 0; i < data.length - shingleSize + 1; i++) {
            System.arraycopy(data, i, shingled[i], 0, shingleSize);
        }
        return shingled;
    }

    private double[] unboxArray(Double[][] arr) {
        double[] output = new double[arr.length*arr[0].length];
        for (int i = 0; i < arr.length; i++) 
        	for (int j = 0; j < arr[0].length; j++) 
        		output[i*arr[0].length + j] = arr[i][j];
        return output;
    }

    @Override
    public double addPoint(double[] value) {
    	buffer.addLast(doubleToDouble(value));
        if (buffer.size() < shingleSize) {
            return 0;
        } else {
            if (buffer.size() > shingleSize) {
                buffer.removeFirst();
            }
            return super.addPoint(unboxArray(buffer.toArray(new Double[buffer.size()][value.length])));
        }
    }
    
    //TODO import org.apache.commons.lang3.ArrayUtils;
    public Double[] doubleToDouble(double[] d) {
    	Double[] n = new Double[d.length];
    	for (int i = 0; i < d.length; i++)
			n[i] = d[i];
    	return n;
    }
}
