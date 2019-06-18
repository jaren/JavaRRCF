import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rrcf.memory.BoundedBuffer;
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
        assertEquals(new double[] { 0, 1, 2 }, tree.getMinBox());
        assertEquals(new double[] { 97, 98, 99 }, tree.getMaxBox());
        tree.forgetPoint(new ShingledPoint(b, 0, 3));
        assertEquals(new double[] { 1, 2, 3 }, tree.getMinBox());
        assertEquals(new double[] { 97, 98, 99 }, tree.getMaxBox());
    }

    @Test
    public void testRemove() {

    }
}