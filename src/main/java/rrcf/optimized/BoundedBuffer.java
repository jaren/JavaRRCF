package rrcf.optimized;

/**
 * Represents a buffer for infinite streaming data
 * Only stores the last N points, but allows items to be
 *  accessed by index in stream
 */
public class BoundedBuffer<T> {
    private T[] buffer;
    private int bufferStartIndex;
    private int size;
    private long streamStartIndex;

    public BoundedBuffer(int bound) {
        buffer = (T[])new Object[bound];
        bufferStartIndex = 0;
        size = 0;
        streamStartIndex = 0;
    }

    public int size() {
        return size;
    }

    /**
     * Gets an item at a logical index in a stream
     * @param index Index relative to stream start
     * @return Value at index
     */
    public T get(long index) {
        int adjustedIndex = index - streamStartIndex;
        if (adjustedIndex > size) {
            throw new ArrayIndexOutOfBoundsException("Element out of bounds of buffer");
        }
        return buffer[getOffsetBufferIndex(adjustedIndex)];
    }

    /**
     * Adds an item to the buffer
     * @return Whether or not the first item was deleted
     */
    public boolean add(T value) {
        buffer[getOffsetBufferIndex(size)] = value;
        if (size < buffer.length) {
            size++;
            return false;
        } else {
            bufferStartIndex = getOffsetBufferIndex(1);
            return true;
        }
    }

    private int getOffsetBufferIndex(int num) {
        return (bufferStartIndex + num) % buffer.length;
    }
}