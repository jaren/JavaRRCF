import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import rrcf.general.SimpleShingledForest;
import rrcf.memory.BoundedBuffer;
import rrcf.memory.ShingledForest;
import rrcf.memory.ShingledPoint;
import rrcf.memory.ShingledTree;

public class ShingledTreeTest {
    @Test
    public void testAdd() {
        ShingledTree tree = new ShingledTree(3);
        BoundedBuffer<Double> b = new BoundedBuffer<>(100);
        b.add(0d);
        b.add(1d);
        for (int i = 2; i < 100; i++) {
            b.add((double)i);
            tree.insertPoint(new ShingledPoint(b, i - 2, 3));
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
        tree.forgetPoint(new ShingledPoint(b, 0, 3));
        assertArrayEquals(new double[] { 1, 2, 3 }, tree.getMinBox(), delta);
        assertArrayEquals(new double[] { 97, 98, 99 }, tree.getMaxBox(), delta);
    }

    @Test
    public void testSin() {
        // Checks that this doesn't throw
        ShingledForest f = new ShingledForest(new Random(1), 2, 1, 2);
        SimpleShingledForest f2 = new SimpleShingledForest(new Random(1), 2, 1, 2);
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