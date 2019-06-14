package rrcf.optimized;

/**
 * Represents an infinite buffer for data
 * Only stores the last N points but items are accessed by absolute index
 */
public class BoundedBuffer<T> {
    private ArrayDeque<T> buffer;
    private long startIndex;

    public BoundedBuffer(int bound) {
        buffer = new ArrayDeque();

    }

    public T get(long index) {
    }
}