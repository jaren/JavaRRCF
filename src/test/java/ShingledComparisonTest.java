import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import rrcf.general.Forest;
import rrcf.general.ShingledForest;

public class ShingledComparisonTest {
    @Test
    public void testShingled() {
        double[] dat = new double[] { 1, 2, 3, 3, 3, 4, 5 };
        ShingledForest sForest = new ShingledForest(new Random(1), 2, 1, 20, dat);
        assertEquals(6, sForest.trees[0].size());
        double[][] noShingleDat = new double[][] {
            { 1, 2 },
            { 2, 3 },
            { 3, 3 },
            { 3, 3 },
            { 3, 4 },
            { 4, 5 }
        };
        Forest forest = new Forest(new Random(1), 1, 20, noShingleDat);
        System.out.println("Normal:");
        System.out.println(forest.toString());
        System.out.println("Shingled:");
        System.out.println(sForest.toString());
        assertEquals(forest.toString(), sForest.toString());
        forest.addPoint(new double[] { 5, 6 });
        sForest.addPoint(6);
        assertEquals(forest.toString(), sForest.toString());
        for (int i = 0; i < 3; i++) {
            double dispF = forest.getCollusiveDisplacement(i);
            double dispS = sForest.getCollusiveDisplacement(i);
            assertTrue(dispF >= 0);
            assertTrue(dispS >= 0);
            assertEquals(dispF, dispS, 0.00000000001);
            System.out.println(dispF);
            System.out.println(dispS);
        }
    }
}