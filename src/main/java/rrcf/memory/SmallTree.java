package rrcf.memory;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

import rrcf.memory.SmallBranch;
import rrcf.memory.SmallLeaf;
import rrcf.memory.SmallNode;

import java.io.Serializable;

/**
 * Robust random cut tree data structure used for anomaly detection on streaming
 * data
 * 
 * Represents a single random cut tree, supporting generalized data points of any dimension
 * Memory-optimized version of general/Tree
 * - Doesn't store bounding boxes, only storing a delta at each branch (cut dim/value since bounding boxes only change by one item each time)
 * - Don't store individual leaf points, they're calculated implicitly by traversing down the tree
 * 
 * Modified from: rrcf: Implementation of the Robust Random Cut Forest algorithm
 * for anomaly detection on streams Matthew D. Bartos1, Abhiram Mullapudi1, and
 * Sara C. Troutman
 * 
 * Original paper: S. Guha, N. Mishra, G. Roy, & O. Schrijvers. Robust random
 * cut forest based anomaly detection on streams, in Proceedings of the 33rd
 * International conference on machine learning, New York, NY, 2016 (pp.
 * 2712-2721).
 */
public class SmallTree implements Serializable {
    private SmallNode root;
    private int dimension;
    private Random random;
    private double[] rootMinPoint;
    private double[] rootMaxPoint;

    public SmallTree(Random r, int shingleSize) {
        random = r;
        dimension = shingleSize;
        rootMinPoint = null;
        rootMaxPoint = null;
    }

    public SmallTree(int shingleSize) {
        this(new Random(), shingleSize);
    }

    @Override
    public String toString() {
        String[] depthAndTreeString = { "", "" };
        if (root == null) return "";
        double[] currentMinBox = rootMinPoint.clone();
        double[] currentMaxBox = rootMaxPoint.clone();
        printNodeToString(root, depthAndTreeString, currentMinBox, currentMaxBox);
        return depthAndTreeString[1];
    }

    /**
     * Prints a node to provided string
     * Updates the given string array: { depth, tree } strings
     */
    private void printNodeToString(SmallNode node, String[] depthAndTreeString, double[] currentMinBox, double[] currentMaxBox) {
        Consumer<Character> ppush = (c) -> {
            String branch = String.format(" %c  ", c);
            depthAndTreeString[0] += branch;
        };
        Runnable ppop = () -> {
            depthAndTreeString[0] = depthAndTreeString[0].substring(0, depthAndTreeString[0].length() - 4);
        };
        if (node instanceof SmallLeaf) {
            depthAndTreeString[1] += String.format("(%s)\n", Arrays.toString(currentMinBox));
        } else if (node instanceof SmallBranch) {
            SmallBranch b = (SmallBranch)node;
            double[] leftMinBox = currentMinBox.clone();
            double[] leftMaxBox = currentMaxBox.clone();
            double[] rightMinBox = currentMinBox.clone();
            double[] rightMaxBox = currentMaxBox.clone();
            for (int i = 0; i < dimension; i++) {
                if (b.childMinPointDirections.get(i)) {
                    leftMinBox[i] = b.childMinPointValues[i];
                } else {
                    rightMinBox[i] = b.childMinPointValues[i];
                }

                if (b.childMaxPointDirections.get(i)) {
                    leftMaxBox[i] = b.childMaxPointValues[i];
                } else {
                    rightMaxBox[i] = b.childMaxPointValues[i];
                }
            }
            depthAndTreeString[1] += String.format("%c+ cut: (%d, %f), box: (%s, %s)\n", 9472, b.cut.dim, b.cut.value, Arrays.toString(currentMinBox), Arrays.toString(currentMaxBox));
            depthAndTreeString[1] += String.format("%s %c%c%c", depthAndTreeString[0], 9500, 9472, 9472);
            ppush.accept((char) 9474);
            printNodeToString(b.left, depthAndTreeString, leftMinBox, leftMaxBox);
            ppop.run();
            depthAndTreeString[1] += String.format("%s %c%c%c", depthAndTreeString[0], 9492, 9472, 9472);
            ppush.accept(' ');
            printNodeToString(b.right, depthAndTreeString, rightMinBox, rightMaxBox);
            ppop.run();
        }
    }
	
    /**
     * Apply function to all leaves (nodes with no children)
     */
    public void mapLeaves(Consumer<SmallLeaf> func) {
        mapLeaves(func, root);
    }

    private void mapLeaves(Consumer<SmallLeaf> func, SmallNode n) {
        if (n instanceof SmallLeaf) {
            func.accept((SmallLeaf) n);
        } else {
            SmallBranch b = (SmallBranch) n;
            if (b.left != null) {
                mapLeaves(func, b.left);
            }
            if (b.right != null) {
                mapLeaves(func, b.right);
            }
        }
    }

    public double[] getMinBox() {
        return rootMinPoint.clone();
    }

    public double[] getMaxBox() {
        return rootMaxPoint.clone();
    }

    /**
     * Apply function to all branches (nodes with two children)
     */
    public void mapBranches(Consumer<SmallBranch> func) {
        mapBranches(func, root);
    }

    private void mapBranches(Consumer<SmallBranch> func, SmallNode n) {
        if (n instanceof SmallBranch) {
            SmallBranch b = (SmallBranch) n;
            if (b.left != null) {
                mapBranches(func, b.left);
            }
            if (b.right != null) {
                mapBranches(func, b.right);
            }
            func.accept(b);
        }
    }

    /**
     * Delete a leaf (found from index) from the tree and return deleted node
     */
    public SmallLeaf forgetPoint(double[] point) throws NoSuchElementException {
        SmallLeaf leaf = findLeaf(point);
        
        if (leaf == null) {
            throw new NoSuchElementException(String.format("Point not found: %s", Arrays.toString(point)));
        }

        // If duplicate points exist, decrease num for all nodes above
        if (leaf.num > 1) {
            updateLeafCountUpwards(leaf, -1);
            return leaf;
        }

        // If leaf is root, clear the tree
        if (root.equals(leaf)) {
            root = null;
            rootMinPoint = null;
            rootMaxPoint = null;
            return leaf;
        }

        // Calculate parent and sibling
        SmallBranch parent = leaf.parent;
        SmallNode sibling = getSibling(leaf);

        // If parent is root, set sibling to root and update depths
        if (root.equals(parent)) {
            SmallBranch bRoot = (SmallBranch)root;
            for (int i = 0; i < dimension; i++) {
                // If leaf made up bounding box at dimension, set to other side
                if ((leaf.equals(parent.left)) != bRoot.childMinPointDirections.get(i)) {
                    rootMinPoint[i] = bRoot.childMinPointValues[i];
                }
                if ((leaf.equals(parent.left)) != bRoot.childMaxPointDirections.get(i)) {
                    rootMaxPoint[i] = bRoot.childMaxPointValues[i];
                }
            }

            sibling.parent = null;
            root = sibling;

            return leaf;
        }

        // Shrink bounding box before updating pointers
        shrinkBoxUpwards(leaf);

        // Move sibling up a layer and link nodes
        SmallBranch grandparent = parent.parent;
        sibling.parent = grandparent;
        // In case the returned node is used somehow
        leaf.parent = null;
        if (parent.equals(grandparent.left)) {
            grandparent.left = sibling;
        } else {
            grandparent.right = sibling;
        }

        // Update leaf counts for each branch
        updateLeafCountUpwards(grandparent, -1);

        return leaf;
    }

    /**
     * Insert a point into the tree with a given index and create a new leaf
     */
    public SmallLeaf insertPoint(double[] point) {
        // Check that dimensions are consistent and index doesn't exist
        assert point.length == dimension;

        // If no points, set necessary variables
        if (root == null) {
            SmallLeaf leaf = new SmallLeaf();
            root = leaf;
            rootMinPoint = point.clone();
            rootMaxPoint = point.clone();
            return leaf;
        }

        // Check for duplicates and only update counts if it exists
        SmallLeaf duplicate = findLeaf(point);
        if (duplicate != null) {
            updateLeafCountUpwards(duplicate, 1);
            return duplicate;
        }

        // No duplicates found, continue
        SmallNode node = root;
        SmallBranch parent = null;
        SmallLeaf leaf = null;
        SmallBranch branch = null;
        boolean useLeftSide = false;
        double[] minPoint = rootMinPoint.clone();
        double[] maxPoint = rootMaxPoint.clone();

        // Update main bounding box
        for (int i = 0; i < dimension; i++) {
            if (point[i] < rootMinPoint[i]) {
                rootMinPoint[i] = point[i];
            }
            if (point[i] > rootMaxPoint[i]) {
                rootMaxPoint[i] = point[i];
            }
        }

        // Traverse tree until insertion spot found
        while (true) {
            Cut c = insertPointCut(point, minPoint, maxPoint);
            // Has to be less than because less than or equal goes to the left
            // Equal would make node go to the right, excluding some points from query
            if (c.value < minPoint[c.dim]) {
                leaf = new SmallLeaf();
                branch = new SmallBranch(c, dimension, leaf, node, leaf.num + node.num);
                break;
            // Shouldn't result in going down too far because dimensions with 0 variance have a 0 probability of being chosen?
            } else if (c.value >= maxPoint[c.dim] && point[c.dim] > c.value) {
                leaf = new SmallLeaf();
                branch = new SmallBranch(c, dimension, node, leaf, leaf.num + node.num);
                break;
            } else {
                SmallBranch b = (SmallBranch) node;
                parent = b;
                BitSet minSet = (BitSet)b.childMinPointDirections.clone();
                BitSet maxSet = (BitSet)b.childMaxPointDirections.clone();
                if (point[b.cut.dim] <= b.cut.value) {
                    node = b.left;
                    useLeftSide = true;
                } else {
                    node = b.right;
                    useLeftSide = false;
                    minSet.flip(0, dimension);
                    maxSet.flip(0, dimension);
                }

                double[] oldMin = minPoint.clone();
                double[] oldMax = maxPoint.clone();
                // Update bounding boxes at each step down the tree
                // The new node is guaranteed to be inserted under this path, so it's easier to do now
                // Through b and not node since values are stored in the parent
                for (int i = minSet.nextSetBit(0); i != -1; i = minSet.nextSetBit(i + 1)) {
                    minPoint[i] = b.childMinPointValues[i];
                }
                for (int i = maxSet.nextSetBit(0); i != -1; i = maxSet.nextSetBit(i + 1)) {
                    maxPoint[i] = b.childMaxPointValues[i];
                }

                for (int i = 0; i < dimension; i++) {
                    // If the path did not make up the min point
                    if (useLeftSide == b.childMinPointDirections.get(i)) {
                        // Update the value and direction if it's a new min
                        if (point[i] < oldMin[i]) {
                            b.childMinPointDirections.flip(i);
                            b.childMinPointValues[i] = oldMin[i];
                        // Otherwise update the value if necessary
                        } else {
                            b.childMinPointValues[i] = Math.min(b.childMinPointValues[i], point[i]);
                        }
                    }
                    // Same for max box
                    if (useLeftSide == b.childMaxPointDirections.get(i)) {
                        if (point[i] > oldMax[i]) {
                            b.childMaxPointDirections.flip(i);
                            b.childMaxPointValues[i] = oldMax[i];
                        } else {
                            b.childMaxPointValues[i] = Math.max(b.childMaxPointValues[i], point[i]);
                        }
                    }
                }
            }
        }

        // Check if cut was found
        assert branch != null;

        // Sets values for newly created branch
        for (int i = 0; i < dimension; i++) {
            // In this case, minPoint and maxPoint represent the bounding box of leaf's sibling
            // They've been slowly cut down from traversing down the tree
            // Set the point directions for branch by checking if leaf or the other node is a min/max
            branch.childMinPointDirections.set(i, (point[i] < minPoint[i]) != leaf.equals(branch.left));
            branch.childMaxPointDirections.set(i, (point[i] > maxPoint[i]) != leaf.equals(branch.left));
            // Set the point values to the value which is NOT the min and NOT the max respectively
            // aka the max and min, flipped
            branch.childMinPointValues[i] = Math.max(point[i], minPoint[i]);
            branch.childMaxPointValues[i] = Math.min(point[i], maxPoint[i]);
        }

        node.parent = branch;
        leaf.parent = branch;
        branch.parent = parent;
        if (parent != null) {
            if (useLeftSide) {
                parent.left = branch;
            } else {
                parent.right = branch;
            }
        } else {
            root = branch;
        }
        // Increase leaf counts
        updateLeafCountUpwards(parent, 1);

        return leaf;
    }

    /**
     * Shrinks the box up the tree, starting from a node
     * Expected to be called on removal with the removed leaf
     */
    private void shrinkBoxUpwards(SmallLeaf leaf) {
        // The bits of parent's bounding box which are determined by the child
        // --> Whether or not the leaf forms the edge of the bounding box 
        BitSet minDetermined = new BitSet();
        BitSet maxDetermined = new BitSet();
        minDetermined.set(0, dimension);
        maxDetermined.set(0, dimension);

        SmallBranch node = leaf.parent;
        SmallNode previousNode = leaf;
        double[] altMins = new double[dimension];
        Arrays.fill(altMins, Double.MAX_VALUE);
        double[] altMaxes = new double[dimension];
        // Not Double.MIN_VALUE!
        Arrays.fill(altMaxes, -Double.MAX_VALUE); 
		while ((!minDetermined.isEmpty() || !maxDetermined.isEmpty()) && node != null) {
            // For the bits that are determined
            for (int i = minDetermined.nextSetBit(0); i != -1; i = minDetermined.nextSetBit(i + 1)) {
                // Check if new node makes them undetermined
                if (previousNode.equals(node.left) != node.childMinPointDirections.get(i)) {
                    // If not, then this dimension continues up
                    // Check against the other child
                    // If the other child sets a new min/max, update directions to current child
                    if (node.childMinPointValues[i] < altMins[i]) {
                        // Update values to other child
                        if (previousNode.equals(node.left)) {
                            node.childMinPointDirections.set(i);
                        } else {
                            node.childMinPointDirections.clear(i);
                        }
                        double temp = node.childMinPointValues[i];
                        node.childMinPointValues[i] = altMins[i];
                        // Expand alt min/max box if necessary
                        altMins[i] = temp;
                    }
                // If bit is lost further up
                } else {
                    // The removed point must make up the pointValues, so set it to the alt value
                    node.childMinPointValues[i] = altMins[i];
                    // Forget the bit for later iterations
                    minDetermined.clear(i);
                }
            }
            // Same for max
            for (int i = maxDetermined.nextSetBit(0); i != -1; i = maxDetermined.nextSetBit(i + 1)) {
                if (previousNode.equals(node.left) != node.childMaxPointDirections.get(i)) {
                    if (node.childMaxPointValues[i] > altMaxes[i]) {
                        if (previousNode.equals(node.left)) {
                            node.childMaxPointDirections.set(i);
                        } else {
                            node.childMaxPointDirections.clear(i);
                        }
                        double temp = node.childMaxPointValues[i];
                        node.childMaxPointValues[i] = altMaxes[i];
                        altMaxes[i] = temp;
                    }
                } else {
                    node.childMaxPointValues[i] = altMaxes[i];
                    maxDetermined.clear(i);
                }
            }

            previousNode = node;
            node = node.parent;
        }

        // Root reached, update main boxes
        if (node == null) {
            for (int i = minDetermined.nextSetBit(0); i != -1; i = minDetermined.nextSetBit(i + 1)) {
                rootMinPoint[i] = altMins[i];
            }
            for (int i = maxDetermined.nextSetBit(0); i != -1; i = maxDetermined.nextSetBit(i + 1)) {
                rootMaxPoint[i] = altMaxes[i];
            }
        }
    }

    /**
     * Gets the sibling of a node (other child of its parent)
     */
    private SmallNode getSibling(SmallNode n) {
        SmallBranch parent = n.parent;
        if (n.equals(parent.left)) {
            return parent.right;
        }
        return parent.left;
    }

    /**
     * Increases the leaf number for all ancestors above a given node by increment
     */
    private void updateLeafCountUpwards(SmallNode node, int increment) {
        while (node != null) {
            node.num += increment;
            node = node.parent;
        }
    }

    /**
     * Finds the leaf corresponding to a point
     */
    private SmallLeaf findLeaf(double[] point) {
        SmallNode n = root;
        double[] minPoint = rootMinPoint.clone();
        double[] maxPoint = rootMaxPoint.clone();
        // Traverse down tree, following cuts
        while (!(n instanceof SmallLeaf)) {
            SmallBranch b = (SmallBranch) n;
            BitSet min = (BitSet)b.childMinPointDirections.clone();
            BitSet max = (BitSet)b.childMaxPointDirections.clone();
            if (point[b.cut.dim] <= b.cut.value) {
                n = b.left;
            } else {
                n = b.right;
                min.flip(0, dimension);
                max.flip(0, dimension);
            }

            // Keep track of bounding box on the way down
            for (int i = min.nextSetBit(0); i != -1; i = min.nextSetBit(i + 1)) {
                minPoint[i] = b.childMinPointValues[i];
            }
            for (int i = max.nextSetBit(0); i != -1; i = max.nextSetBit(i + 1)) {
                maxPoint[i] = b.childMaxPointValues[i];
            }
        }
        // Check that a leaf is reached and the points are the same
        if (!Arrays.equals(point, minPoint) || !Arrays.equals(point, maxPoint)) {
            return null;
        }
        return (SmallLeaf) n;
    }

    /**
     * The maximum number of nodes displaced by removing any subset of the tree including a leaf 
     * In practice, there are too many subsets to consider so it can be estimated by looking up the tree
     * There is no definitive algorithm to empirically calculate codisp, so the ratio of sibling num to node num is used
     */
    public int getCollusiveDisplacement(SmallLeaf leaf) {
        if (leaf.equals(root)) {
            return 0;
        }

        SmallNode node = leaf;
        int maxResult = -1;
        while (node != null) {
            SmallBranch parent = node.parent;
            if (parent == null)
                break;
            SmallNode sibling;
            if (node.equals(parent.left)) {
                sibling = parent.right;
            } else {
                sibling = parent.left;
            }
            int deleted = node.num;
            int displacement = sibling.num;
            maxResult = Math.max(maxResult, displacement / deleted);
            node = parent;
        }
        return maxResult;
    }

    /**
     * Generates a random cut from the span of a point and bounding box
     */
    private Cut insertPointCut(double[] point, double[] minPoint, double[] maxPoint) {
        double[] newMinBox = new double[minPoint.length];
        double[] span = new double[minPoint.length];
        // Cumulative sum of span
        double[] spanSum = new double[minPoint.length];
        for (int i = 0; i < dimension; i++) {
            newMinBox[i] = Math.min(minPoint[i], point[i]);
            double maxI = Math.max(maxPoint[i], point[i]);
            span[i] = maxI - newMinBox[i];
            if (i > 0) {
                spanSum[i] = spanSum[i - 1] + span[i];
            } else {
                spanSum[i] = span[0];
            }
        }
        // Weighted random with each dimension's span
        double range = spanSum[spanSum.length - 1];
        double r = random.nextDouble() * range;
        int cutDim = -1;
        for (int i = 0; i < dimension; i++) {
            // Finds first value greater or equal to chosen
            if (spanSum[i] >= r) {
                cutDim = i;
                break;
            }
        }
        assert cutDim > -1;
        double value = newMinBox[cutDim] + spanSum[cutDim] - r;
        return new Cut(cutDim, value);
    }

    /** 
     * Helper class representing a cut point along some dimension
     */
    public static class Cut implements Serializable {
        // Dimension of cut
        public int dim;
        // Value of cut
        public double value;

        public Cut(int d, double v) {
            dim = d;
            value = v;
        }
    }
}
