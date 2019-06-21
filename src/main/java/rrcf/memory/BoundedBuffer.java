package rrcf.memory;

import java.util.Arrays;

/**
 * Represents a buffer for infinite streaming data
 * Only stores the last N points, but allows items to be
 *  accessed by index in stream
 */
public class BoundedBuffer<T> {
    private T[] buffer;
    private int bufferStartIndex;
    private int size;
    // Should handle overflows fine?
    // Only the difference matters
    private int streamIndex;

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

    public int streamStartIndex() {
        return streamIndex;
    }

    /**
     * Gets an item at a logical index in a stream
     * @param index Index relative to stream start
     * @return Value at index
     */
    public T get(int index) {
        int adjustedIndex = index - streamIndex;
        if (adjustedIndex > size) {
            throw new ArrayIndexOutOfBoundsException("Element out of bounds of buffer");
        }
        if (index < 0) {
            throw new ArrayIndexOutOfBoundsException("Invalid index");
        }
        if (adjustedIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("Element already removed from buffer");
        }
        return buffer[getOffsetBufferIndex(adjustedIndex)];
    }

    /**
     * Adds an item to the buffer
     * Removes oldest item if necessary
     * @return Index of item
     */
    public int add(T value) {
        buffer[getOffsetBufferIndex(size)] = value;
        if (!full()) {
            size++;
        } else {
            streamIndex++;
            bufferStartIndex = getOffsetBufferIndex(1);
        }
        return streamIndex + size - 1;
    }

    private int getOffsetBufferIndex(int num) {
        return (bufferStartIndex + num) % buffer.length;
    }

    public T[] toArray() {
        T[] arr = (T[])new Object[size];
        for (int i = 0; i < size; i++) {
            arr[i] = get(streamIndex + i);
        }
        return arr;
    }
}