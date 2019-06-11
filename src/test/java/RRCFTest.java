import java.util.Random;

import rrcf.ShingledForest;

public class RRCFTest {
    public static void main(String[] args) {
        System.out.println("\"x\",\"y\",\"value\"");
        ShingledForest f = new ShingledForest(4, 40, 256);
        for (float i = 0; i < 30; i += 0.05) {
            double[] k = { 50 * Math.sin(i) + 100 };
            if (i > 19 && i < 20) {
                k[0] = 80;
            }
            Double res = f.addPoint(k);
            if (res != null) {
                System.out.printf("%f, %f, %f\n", i * 50, k[0], res);
            }
        }
        // System.out.println(tree.toString());
    }
}