import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rrcf.memory.BoundedBuffer;

public class BoundedBufferTest {
    @Test
    public void testBasic() {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(3);
        buffer.add(0);
        buffer.add(1);
        buffer.add(2);
        buffer.add(3);
        buffer.add(4);
        assertEquals(4, buffer.get(4).intValue());
        assertEquals(3, buffer.get(3).intValue());
        buffer.add(123);
        assertEquals(4, buffer.get(4).intValue());
        assertEquals(3, buffer.get(3).intValue());
        buffer.add(456);
        assertEquals(4, buffer.get(4).intValue());
    }
}