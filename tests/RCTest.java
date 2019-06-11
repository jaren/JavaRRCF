import java.util.Random;

public class RCTest {
    public static void main(String[] args) {
        System.out.println("\"x\",\"y\",\"value\"");
        RCTree[] forest = new RCTree[40];
        for (int i = 0; i < forest.length; i++) {
            forest[i] = new RCTree();
        }
        for (float i = 0; i < 30; i+=0.01) {
            double[] k = { Math.sin(i) };
            if (i > 20 && i < 23) {
                k[0] = 0.2;
            }
            int accum = 0;
            for (RCTree t : forest) {
                t.insertPoint(k, i);
                accum += t.getCollusiveDisplacement(i);
            }
            System.out.printf("%f, %f, %f\n", i, k[0], accum / (double)forest.length);
        }
        //System.out.println(tree.toString());
    }
}