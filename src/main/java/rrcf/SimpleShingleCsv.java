package rrcf;

import java.util.Random;
import java.util.Scanner;

import rrcf.general.SimpleShingledForest;

/**
 * Reads numbers from stdin
 * Outputs (index, number, score) for each data point passed in
 * 
 * Args: shingleSize numTrees treeSize
 */
public class SimpleShingleCsv {
    public static void main(String[] args) {
        int shingleSize = Integer.parseInt(args[0]);
        int numTrees = Integer.parseInt(args[1]);
        int treeSize = Integer.parseInt(args[2]);
        Random random = new Random(Integer.parseInt(args[3]));
        Scanner input = new Scanner(System.in);
        System.out.println("\"x\",\"y\",\"value\"");
        SimpleShingledForest f = new SimpleShingledForest(random, shingleSize, numTrees, treeSize);
        int i = 0;
        while (input.hasNextLine()) {
            float val = Float.parseFloat(input.nextLine());
            System.out.printf("%d,%f,%f\n", i, val, f.addPoint(val));
            i++;
        }
        input.close();
    }
}