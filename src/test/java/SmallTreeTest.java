import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import rrcf.general.ShingledForest;
import rrcf.memory.SmallShingledForest;
import rrcf.memory.SmallTree;

public class SmallTreeTest {
    @Test
    public void testAdd() {
        SmallTree tree = new SmallTree(3);
        for (int i = 2; i < 100; i++) {
            tree.insertPoint(new double[] { i - 2, i - 1, i });
        }
        // 0, 1, 2
        // 1, 2, 3
        // 2, 3, 4
        // 3, 4, 5
        // 4, 5, 6
        // 5, 6, 7
        // 6, 7, 8
        // ...
        double delta = 0.00000001;
        assertArrayEquals(new double[] { 0, 1, 2 }, tree.getMinBox(), delta);
        assertArrayEquals(new double[] { 97, 98, 99 }, tree.getMaxBox(), delta);
        tree.forgetPoint(new double[] { 0, 1, 2 });
        assertArrayEquals(new double[] { 1, 2, 3 }, tree.getMinBox(), delta);
        assertArrayEquals(new double[] { 97, 98, 99 }, tree.getMaxBox(), delta);
    }

    @Test
    public void testSin() {
        // Checks that this doesn't throw
        SmallShingledForest f = new SmallShingledForest(new Random(1), 2, 1, 2);
        ShingledForest f2 = new ShingledForest(new Random(1), 2, 1, 2);
        for (double i = 0; i < 100; i += 0.1) {
            if (i > 50 && i < 60) {
                f.addPoint(Math.sin(50) * 50);
                f2.addPoint(Math.sin(50) * 50);
            } else {
                f.addPoint(Math.sin(i) * 50);
                f2.addPoint(Math.sin(i) * 50);
            }
        }
    }
}