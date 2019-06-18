import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import rrcf.general.SimpleShingledForest;
import rrcf.memory.ShingledForest;

public class TreeComparisonTest {
    @Test
    public void ComparisonTestForest() {
        Random rTest = new Random(1);
        for (long i = 0; i < 100; i++) {
            long randomSeed = rTest.nextLong();
            int numTrees = rTest.nextInt(15) + 1;
            int treeSize = rTest.nextInt(200) + 20;
            int shingleSize = rTest.nextInt(50) + 2;
            int numInserts = rTest.nextInt(treeSize * 3) + treeSize;
            System.out.printf("Testing with (seed %d, trees %d, treeSize %d, shingleSize %d)\n", i, numTrees, treeSize, shingleSize);
            ShingledForest testUnknown = new ShingledForest(new Random(randomSeed), shingleSize, numTrees, treeSize);
            SimpleShingledForest testVerify = new SimpleShingledForest(new Random(randomSeed), shingleSize, numTrees, treeSize);
            for (int e = 0; e < numInserts; e++) {
                double val = rTest.nextDouble() * 1000;
                //System.out.printf("Inserting %f\n", val);
                double v2 = testVerify.addPoint(val);
                double v1 = testUnknown.addPoint(val);
                //System.out.println("Expected:");
                //System.out.println(test2.trees[0].toString());
                //System.out.println("Actual:");
                //System.out.println(test1.trees[0].toString());
                assertEquals(v2, v1, 0.000001);
                assertEquals(testVerify.trees[0].toString(), testUnknown.trees[0].toString());
            }
        }
    }
}