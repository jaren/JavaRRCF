package rrcf;

import java.util.Random;
import java.util.Scanner;

import rrcf.memory.ShingledForest;

/**
 * Reads numbers from stdin
 * Outputs (index, number, score) for each data point passed in
 * 
 * Args: shingleSize numTrees treeSize
 */
public class ShingleCsv {
    public static void main(String[] args) {
        int shingleSize = Integer.parseInt(args[0]);
        int numTrees = Integer.parseInt(args[1]);
        int treeSize = Integer.parseInt(args[2]);
        Random random = new Random(Integer.parseInt(args[3]));
        Scanner input = new Scanner(System.in);
        System.out.println("\"x\",\"y\",\"value\"");
        ShingledForest f = new ShingledForest(random, shingleSize, numTrees, treeSize);
        int i = 0;
        while (input.hasNextLine()) {
            double val = Double.parseDouble(input.nextLine());
            System.out.printf("%d,%f,%f\n", i, val, f.addPoint(val));
            i++;
        }
        input.close();
    }
}