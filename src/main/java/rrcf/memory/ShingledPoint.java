package rrcf.memory;

import java.util.Arrays;

/**
 * Represents a shingled point stored in a BoundedBuffer
 */
public class ShingledPoint {
    private int size;
    private BoundedBuffer<Double> buffer;
    private int startIndex;

    public ShingledPoint(BoundedBuffer<Double> buffer, int startIndex, int size) {
        this.buffer = buffer;
        this.startIndex = startIndex;
        this.size = size;
    }

    public int size() {
        return size;
    }

    public double get(int index) {
        if (index > size) {
            throw new ArrayIndexOutOfBoundsException("Invalid point access index");
        }
        return buffer.get(startIndex + index);
    }

    public double[] toArray() {
        double[] arr = new double[size];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = get(i);
        }
        return arr;
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ShingledPoint) {
            ShingledPoint s = (ShingledPoint)other;
            if (s.size() != size()) {
                return false;
            }
            for (int i = 0; i < size; i++) {
                if (s.get(i) != get(i)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}