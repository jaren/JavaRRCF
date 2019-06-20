import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rrcf.memory.BoundedBuffer;
import rrcf.memory.ShingledTree;

public class ShingledTreeTest {
    @Test
    public void testAdd() {
        BoundedBuffer<Double> b = new BoundedBuffer<>(100);
        ShingledTree tree = new ShingledTree(b, 3);
        b.add(0d);
        b.add(1d);
        for (int i = 2; i < 100; i++) {
            b.add((double)i);
            tree.insertPoint(i - 2);
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
        tree.forgetPoint(0);
        assertArrayEquals(new double[] { 1, 2, 3 }, tree.getMinBox(), delta);
        assertArrayEquals(new double[] { 97, 98, 99 }, tree.getMaxBox(), delta);
    }

    @Test
    public void testRemove() {

    }
}