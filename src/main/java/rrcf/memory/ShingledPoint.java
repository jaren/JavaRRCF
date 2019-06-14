package rrcf.memory;

/**
 * Represents a shingled point stored in a BoundedBuffer
 */
public class ShingledPoint {
    private int size;
    private BoundedBuffer<Float> buffer;
    private long startIndex;

    public ShingledPoint(BoundedBuffer<Float> buffer, long startIndex, int size) {
        this.buffer = buffer;
        this.startIndex = startIndex;
        this.size = size;
    }

    public int size() {
        return size;
    }

    public float get(int index) {
        if (index > size) {
            throw new ArrayIndexOutOfBoundsException("Invalid point access index");
        }
        return buffer.get(startIndex + index);
    }

    public float[] toArray() {
        float[] arr = new float[size];
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