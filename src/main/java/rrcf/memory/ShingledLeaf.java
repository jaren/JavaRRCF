package rrcf.memory;

public class ShingledLeaf extends ShingledNode {
    // Trying to avoid storing as much information as possible in each leaf 
    public long startIndex;

    public ShingledLeaf(long index) {
        startIndex = index;
        num = 1;
    }

    public double[] toArray(BoundedBuffer<Double> buffer, int length) {
        double[] arr = new double[length];
        for (int i = 0; i < length; i++) {
            arr[i] = getValue(buffer, i);
        }
        return arr;
    }

    public double getValue(BoundedBuffer<Double> buffer, int index) {
        return buffer.get(startIndex + index).doubleValue();
    }
}