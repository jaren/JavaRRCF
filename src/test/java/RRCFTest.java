import java.util.Random;

import rrcf.ShingledForest;

public class RRCFTest {
    public static void main(String[] args) {
        System.out.println("\"x\",\"y\",\"value\"");
        ShingledForest f = new ShingledForest(4, 40, 256);
        for (float i = 0; i < 730; i++) {
            double k = 50 * Math.sin(2 * Math.PI * i / (double)100) + 100;
            if (i >= 235 && i < 255) {
                k = 80;
            }
            System.out.printf("%f, %f, %f\n", i * 50, k, f.addPoint(k));
        }
        // System.out.println(tree.toString());
    }
}