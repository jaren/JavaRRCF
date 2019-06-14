package rrcf.optimized;

/**
 * Represents a shingled point stored in a BoundedBuffer
 */
public class ShingledPoint {
    private int size;
    private BoundedBuffer<Double> buffer;
    private long startIndex;

    public ShingledPoint(BoundedBuffer<Double> buffer, long startIndex, int size) {
        this.buffer = buffer;
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
    public boolean equals(Object other) {
        if (other instanceof ShingledPoint) {
            ShingledPoint s = (ShingledPoint)other;
            return buffer.equals(s.buffer)
                && size == s.size
                && startIndex == s.startIndex;
        }
        return false;
    }
}