import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import rrcf.general.SimpleShingledForest;
import rrcf.memory.ShingledForest;

public class TreeComparisonTest {
    @Test
    public void ComparisonTestForest() {
        for (long i = 2; i < 10000000; i=i*i) {
            for (int numTrees = 1; numTrees < 100; numTrees += 9) {
                for (int treeSize = 10; treeSize < 1000; treeSize += 100) {
                    for (int shingleSize = 2; shingleSize < 50; shingleSize += 10) {
                        System.out.printf("Testing with %d %d %d %d\n", i, numTrees, treeSize, shingleSize);
                        Random r = new Random(i);
                        ShingledForest test1 = new ShingledForest(r, shingleSize, numTrees, treeSize);
                        Random r2 = new Random(i);
                        SimpleShingledForest test2 = new SimpleShingledForest(r2, shingleSize, numTrees, treeSize);
                        Random rTest = new Random(1);
                        for (int e = 0; e < 2000; e++) {
                            double val = rTest.nextDouble() * 1000;
                            System.out.printf("Inserting %f\n", val);
                            double v2 = test2.addPoint(val);
                            double v1 = test1.addPoint(val);
                            System.out.println("Expected:");
                            System.out.println(test2.trees[0].toString());
                            System.out.println("Actual:");
                            System.out.println(test1.trees[0].toString());
                            assertEquals(v2, v1, 0.000001);
                            assertEquals(test2.trees[0].toString(), test1.trees[0].toString());
                        }
                    }
                }
            }
        }
    }
}