import static org.junit.Assert.assertEquals;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import rrcf.general.RCTree;
import rrcf.general.SimpleShingledForest;
import rrcf.memory.BoundedBuffer;
import rrcf.memory.ShingledForest;
import rrcf.memory.ShingledTree;

public class TreeComparisonTest {
    // TODO: toString forest method
    @Test
    public void ComparisonTestForest() {
        Random rTest = new Random(1);
        testWithNum(rTest, 1, 1, 10, 2, 300);
        for (long i = 0; i < 100; i++) {
            long randomSeed = rTest.nextLong();
            int numTrees = rTest.nextInt(10) + 1;
            int treeSize = rTest.nextInt(100) + 5;
            int shingleSize = rTest.nextInt(20) + 2;
            int numInserts = rTest.nextInt(treeSize * 3) + treeSize;
            testWithNum(rTest, randomSeed, numTrees, treeSize, shingleSize, numInserts);
        }
    }

    private void testWithNum(Random rTest, long randomSeed, int numTrees, int treeSize, int shingleSize, int numInserts) {
        System.out.printf("Testing with (seed %d, trees %d, treeSize %d, shingleSize %d)\n", randomSeed, numTrees,
                treeSize, shingleSize);
        ShingledForest testUnknown = new ShingledForest(new Random(randomSeed), shingleSize, numTrees, treeSize);
        SimpleShingledForest testVerify = new SimpleShingledForest(new Random(randomSeed), shingleSize, numTrees, treeSize);
        for (int e = 0; e < numInserts; e++) {
            double val = rTest.nextDouble() * 1000;
            System.out.printf("Inserting %f\n", val);
            double v2 = testVerify.addPoint(val);
            double v1 = testUnknown.addPoint(val);
            System.out.println("Expected:");
            System.out.println(testUnknown.trees[0].toString());
            System.out.println("Actual:");
            System.out.println(testVerify.trees[0].toString());
            assertEquals(v2, v1, 0.000001);
            assertEquals(testVerify.trees[0].toString(), testUnknown.trees[0].toString());
        }
    }

    @Test
    public void ComparisonTestIndividualTree() {
        Random rTest = new Random(1);
        long randomSeed = rTest.nextLong();
        int shingleSize = rTest.nextInt(5) + 2;
        int iters = rTest.nextInt(1000) + 100;
        int maxTreeSize = rTest.nextInt(10) + 10;
        BoundedBuffer<Double> b = new BoundedBuffer<>(shingleSize * iters);
        ShingledTree testUnknown = new ShingledTree(new Random(randomSeed), b, shingleSize);
        RCTree testVerify = new RCTree(new Random(randomSeed));
        Set<Integer> points = new HashSet<>();
        // Technically not using shingling for points
        for (int i = 0; i < iters; i++) {
            System.out.printf("Iteration %d\n", i);
            if (!points.isEmpty() && (rTest.nextDouble() > 0.8 || testVerify.size() > maxTreeSize)) {
                Object[] keys = points.toArray();
                Integer k = (Integer)keys[rTest.nextInt(keys.length)];
                System.out.printf("Removing %d\n", k);
                testUnknown.forgetPoint(k);
                testVerify.forgetPoint(k);
                points.remove(k);
            } else {
                long start = b.streamStartIndex() + b.size();
                double[] fullPoint = new double[shingleSize];
                for (int d = 0; d < shingleSize; d++) {
                    double val = rTest.nextInt(10000);
                    b.add(val);
                    fullPoint[d] = val;
                }
                System.out.printf("Inserting %d\n", start);
                points.add((int)start);
                testVerify.insertPoint(fullPoint, (int)start);
                testUnknown.insertPoint(start);
            }
            System.out.println("Expected:");
            System.out.println(testVerify.toString());
            System.out.println("Verify:");
            System.out.println(testUnknown.toString());
            System.out.println("\n");
            assertEquals(testVerify.toString(), testUnknown.toString());
        }
    }
}