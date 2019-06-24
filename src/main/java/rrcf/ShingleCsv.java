package rrcf;

import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import rrcf.general.SimpleShingledForest;
import rrcf.memory.RCSmallForest;

/**
 * Reads numbers from stdin
 * Outputs (index, number, score) for each data point passed in
 * 
 * Args: shingleSize numTrees treeSize
 */
public class ShingleCsv {
    public static void main(String[] args) {
        if (args.length < 6) {
            System.out.println("Args: useSimpleForest outputMemory shingleSize numTrees treeSize randomSeed");
            System.out.println("Provided: " + Arrays.toString(args));
            System.exit(1);
        }
        boolean useSimple = Boolean.parseBoolean(args[0]);
        boolean outputMemory = Boolean.parseBoolean(args[1]);
        int shingleSize = Integer.parseInt(args[2]);
        int numTrees = Integer.parseInt(args[3]);
        int treeSize = Integer.parseInt(args[4]);
        Random random = new Random(Integer.parseInt(args[5]));

        Object forest;
        if (useSimple) {
            forest = new SimpleShingledForest(random, shingleSize, numTrees, treeSize);
        } else {
            forest = new RCSmallForest(random, shingleSize, numTrees, treeSize);
        }

        Scanner input = new Scanner(System.in);
        System.out.println("\"x\",\"y\",\"value\"");
        int i = 0;
        while (input.hasNextLine()) {
            double val = Double.parseDouble(input.nextLine());
            double score;
            if (useSimple) {
                score = ((SimpleShingledForest)forest).addPoint(val);
            } else {
                score = ((RCSmallForest)forest).addPoint(val);
            }
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