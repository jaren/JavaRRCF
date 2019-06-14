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
    private long streamIndex;

    public BoundedBuffer(int bound) {
        buffer = (T[])new Object[bound];
        bufferStartIndex = 0;
        size = 0;
        streamIndex = 0;
    }

    public boolean full() {
        // Should never be greater
        return size() >= buffer.length;
    }

    public int size() {
        return size;
    }

    public long streamStartIndex() {
        return streamIndex;
    }
    
    public long streamLatestIndex() {
        return streamIndex + size;
    }

    /**
     * Gets an item at a logical index in a stream
     * @param index Index relative to stream start
     * @return Value at index
     */
    public T get(long index) {
        int adjustedIndex = (int)(index - streamIndex);
        if (adjustedIndex > size) {
            throw new ArrayIndexOutOfBoundsException("Element out of bounds of buffer");
        }
        return buffer[getOffsetBufferIndex(adjustedIndex)];
    }

    /**
     * Adds an item to the buffer
     * Removes first item if necessary
     * @return Removed item or null
     */
    public T add(T value) {
        T obj = buffer[getOffsetBufferIndex(size)];
        buffer[getOffsetBufferIndex(size)] = value;
        if (!full()) {
            size++;
        } else {
            bufferStartIndex = getOffsetBufferIndex(1);
        }
        return obj;
    }

    private int getOffsetBufferIndex(int num) {
        return (bufferStartIndex + num) % buffer.length;
    }
}