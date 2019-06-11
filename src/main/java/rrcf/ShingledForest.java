package rrcf;

public class ShingledForest extends Forest {
    private int shingleSize;
    private double[] buffer;
    private int shingleIndex;

    public ShingledForest(int shingleSize, int numTrees, int treeSize) {
        super(numTrees, treeSize);
        buffer = new double[shingleSize];
        shingleIndex = 0;
    }

    public Double addPoint(double value) {
        buffer[shingleIndex] = value;
        shingleIndex++;
        if (shingleIndex == shingleSize) {
            double result = super.addPoint(buffer);
            buffer = new double[shingleSize];
            shingleIndex = 0;
            return result;
        } else {
            return null;
        }
    }
}