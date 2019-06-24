package rrcf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import rrcf.general.ShingledForest;
import rrcf.memory.SmallShingledForest;

/**
 * Reads numbers from stdin
 * Outputs (index, number, score) for each data point passed in
 * 
 * Args: shingleSize numTrees treeSize
 */
public class ShingleCsv {
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Args: useStreaming shingleSize numTrees treeSize randomSeed");
            System.out.println("Provided: " + Arrays.toString(args));
            System.exit(1);
        }
        boolean useSmall = Boolean.parseBoolean(args[0]);
        int shingleSize = Integer.parseInt(args[1]);
        int numTrees = Integer.parseInt(args[2]);
        int treeSize = Integer.parseInt(args[3]);
        Random random = new Random(Integer.parseInt(args[4]));

        System.out.println("\"x\",\"y\",\"value\"");
        Scanner input = new Scanner(System.in);
        if (useSmall) {
            // Streaming
            SmallShingledForest forest = new SmallShingledForest(random, shingleSize, numTrees, treeSize);
            int i = 0;
            while (input.hasNextLine()) {
                double val = Double.parseDouble(input.nextLine());
                double score = forest.addPoint(val);
                System.out.printf("%d,%f,%f\n", i, val, score);
                i++;
            }
            input.close();
        } else {
            // Batch
            ArrayList<Double> arr = new ArrayList<>();
            while (input.hasNextLine()) {
                arr.add(Double.parseDouble(input.nextLine()));
            }
            input.close();
            double[] a = new double[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                a[i] = arr.get(i);
            }
            ShingledForest forest = new ShingledForest(random, shingleSize, numTrees, treeSize, a);
            for (int i = 0; i < a.length - 1 - shingleSize; i++) {
                System.out.printf("%d,%f,%f\n", i, a[i + shingleSize], forest.getDisplacement(i));
            }
        }

    }
}