package rrcf;

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import rrcf.ShingledForest;

/**
 * Reads numbers from stdin
 * Outputs (index, number, score) for each data point passed in
 */
public class RunForestCsv {
    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Args: onlyOutputMemory? shingleSize numTrees treeSize randomSeed");
            System.out.println("Provided: " + Arrays.toString(args));
            System.exit(1);
        }
        boolean outputMemory = Boolean.parseBoolean(args[0]);
        int shingleSize = Integer.parseInt(args[1]);
        int numTrees = Integer.parseInt(args[2]);
        int treeSize = Integer.parseInt(args[3]);
        Random random = new Random(Integer.parseInt(args[4]));

        ShingledForest forest = new ShingledForest(random, shingleSize, numTrees, treeSize);

        Scanner input = new Scanner(System.in);
        System.out.println("\"x\",\"y\",\"value\"");
        int i = 0;
        while (input.hasNextLine()) {
            double val = Double.parseDouble(input.nextLine());
            double score = forest.addPoint(val);
            if (outputMemory) {
                System.out.printf("%d,%f,%d\n", i, val, Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            } else {
                System.out.printf("%d,%f,%f\n", i, val, score);
            }
            i++;
        }
        input.close();
    }
}