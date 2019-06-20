package rrcf.memory;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

import rrcf.memory.ShingledBranch;
import rrcf.memory.ShingledLeaf;
import rrcf.memory.ShingledNode;

import java.io.Serializable;

/**
 * Robust random cut tree data structure used for anomaly detection on streaming
 * data
 * 
 * Implemented optimizations:
 * - Store the values for each leaf in a shared buffer between all trees
 * - Store only half of the bounding boxes at each branch
 * 
 * Represents a single random cut tree, supporting shingled data points of one dimension
 */
public class ShingledTree implements Serializable {
    // TODO: Test with leaves map / array instead of getting leaves at runtime
    // TODO: Replace min/max determined with single array and bitset
    // TODO: Collapse unnecessary classes (shingledpoint) into nodes, don't store unnecessary references
    // TODO: Replace with floats again, find a way around imprecision
    // TODO: Merge memory tree and normal tree?
    // TODO: Bounded buffer find cases where index is negative (from rollover)
    // TODO: Try to reduce pointer number, look into ways of storing trees (implicit?)
    private ShingledNode root;
    private BoundedBuffer<Double> buffer;
    private int dimension;
    private Random random;
    private double[] rootMinPoint;
    private double[] rootMaxPoint;

    public ShingledTree(Random r, BoundedBuffer<Double> b, int shingleSize) {
        random = r;
        buffer = b;
        dimension = shingleSize;
        rootMinPoint = null;
        rootMaxPoint = null;
    }

    public ShingledTree(BoundedBuffer<Double> b, int shingleSize) {
        this(new Random(), b, shingleSize);
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
    private void printNodeToString(ShingledNode node, String[] depthAndTreeString, double[] currentMinBox, double[] currentMaxBox) {
        Consumer<Character> ppush = (c) -> {
            String branch = String.format(" %c  ", c);
            depthAndTreeString[0] += branch;
        };
        Runnable ppop = () -> {
            depthAndTreeString[0] = depthAndTreeString[0].substring(0, depthAndTreeString[0].length() - 4);
        };
        if (node instanceof ShingledLeaf) {
            depthAndTreeString[1] += String.format("(%s)\n", Arrays.toString(((ShingledLeaf)node).toArray(buffer, dimension)));
        } else if (node instanceof ShingledBranch) {
            ShingledBranch b = (ShingledBranch)node;
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

    public void mapLeaves(Consumer<ShingledLeaf> func) {
        mapLeaves(func, root);
    }

    private void mapLeaves(Consumer<ShingledLeaf> func, ShingledNode n) {
        if (n instanceof ShingledLeaf) {
            func.accept((ShingledLeaf) n);
        } else {
            ShingledBranch b = (ShingledBranch) n;
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

    public void mapBranches(Consumer<ShingledBranch> func) {
        mapBranches(func, root);
    }

    private void mapBranches(Consumer<ShingledBranch> func, ShingledNode n) {
        if (n instanceof ShingledBranch) {
            ShingledBranch b = (ShingledBranch) n;
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
    public ShingledLeaf forgetPoint(long startIndex) throws NoSuchElementException {
        ShingledLeaf leaf = findLeaf(startIndex);
        
        if (leaf == null) {
            throw new NoSuchElementException(String.format("Point not found: %d", startIndex));
        }

        // If duplicate points exist, decrease num for all nodes above
        if (leaf.num > 1) {
            updateLeafCountUpwards(leaf, -1);
            return leaf;
        }

        // If leaf is root
        if (root.equals(leaf)) {
            root = null;
            rootMinPoint = null;
            rootMaxPoint = null;
            return leaf;
        }

        // Calculate parent and sibling
        ShingledBranch parent = leaf.parent;
        ShingledNode sibling = getSibling(leaf);

        // If parent is root, set sibling to root and update depths
        if (root.equals(parent)) {
            ShingledBranch bRoot = (ShingledBranch)root;
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
        ShingledBranch grandparent = parent.parent;
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
    public ShingledLeaf insertPoint(long startIndex) {
        // If no points, set necessary variables
        if (root == null) {
            ShingledLeaf leaf = new ShingledLeaf(startIndex);
            root = leaf;
            rootMinPoint = leaf.toArray(buffer, dimension).clone();
            rootMaxPoint = leaf.toArray(buffer, dimension).clone();
            return leaf;
        }

        // Check for duplicates and only update counts if it exists
        ShingledLeaf duplicate = findLeaf(startIndex);
        if (duplicate != null) {
            updateLeafCountUpwards(duplicate, 1);
            return duplicate;
        }

        // No duplicates found, continue
        ShingledNode node = root;
        ShingledBranch parent = null;
        ShingledLeaf leaf = null;
        ShingledBranch branch = null;
        boolean useLeftSide = false;
        double[] minPoint = rootMinPoint.clone();
        double[] maxPoint = rootMaxPoint.clone();

        // Update main bounding box
        for (int i = 0; i < dimension; i++) {
            if (getPointValue(startIndex, i) < rootMinPoint[i]) {
                rootMinPoint[i] = getPointValue(startIndex, i);
            }
            if (getPointValue(startIndex, i) > rootMaxPoint[i]) {
                rootMaxPoint[i] = getPointValue(startIndex, i);
            }
        }

        // Traverse tree until insertion spot found
        while (true) {
            Cut c = insertPointCut(startIndex, minPoint, maxPoint);
            // Has to be less than because less than or equal goes to the left
            // Equal would make node go to the right, excluding some points from query
            if (c.value < minPoint[c.dim]) {
                leaf = new ShingledLeaf(startIndex);
                branch = new ShingledBranch(c, dimension, leaf, node, leaf.num + node.num);
                break;
            // Shouldn't result in going down too far because dimensions with 0 variance have a 0 probability of being chosen?
            } else if (c.value >= maxPoint[c.dim] && getPointValue(startIndex, c.dim) > c.value) {
                leaf = new ShingledLeaf(startIndex);
                branch = new ShingledBranch(c, dimension, node, leaf, leaf.num + node.num);
                break;
            } else {
                ShingledBranch b = (ShingledBranch) node;
                parent = b;
                BitSet minSet = (BitSet)b.childMinPointDirections.clone();
                BitSet maxSet = (BitSet)b.childMaxPointDirections.clone();
                if (getPointValue(startIndex, b.cut.dim) <= b.cut.value) {
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
                        if (getPointValue(startIndex, i) < oldMin[i]) {
                            b.childMinPointDirections.flip(i);
                            b.childMinPointValues[i] = oldMin[i];
                        // Otherwise update the value if necessary
                        } else {
                            b.childMinPointValues[i] = Math.min(b.childMinPointValues[i], getPointValue(startIndex, i));
                        }
                    }
                    // Same for max box
                    if (useLeftSide == b.childMaxPointDirections.get(i)) {
                        if (getPointValue(startIndex, i) > oldMax[i]) {
                            b.childMaxPointDirections.flip(i);
                            b.childMaxPointValues[i] = oldMax[i];
                        } else {
                            b.childMaxPointValues[i] = Math.max(b.childMaxPointValues[i], getPointValue(startIndex, i));
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
            branch.childMinPointDirections.set(i, (getPointValue(leaf.startIndex, i) < minPoint[i]) != leaf.equals(branch.left));
            branch.childMaxPointDirections.set(i, (getPointValue(leaf.startIndex, i) > maxPoint[i]) != leaf.equals(branch.left));
            // Set the point values to the value which is NOT the min and NOT the max respectively
            // aka the max and min, flipped
            branch.childMinPointValues[i] = Math.max(getPointValue(leaf.startIndex, i), minPoint[i]);
            branch.childMaxPointValues[i] = Math.min(getPointValue(leaf.startIndex, i), maxPoint[i]);
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
    private void shrinkBoxUpwards(ShingledLeaf leaf) {
        // The bits of parent's bounding box which are determined by the child
        // --> Whether or not the leaf forms the edge of the bounding box 
        BitSet minDetermined = new BitSet();
        BitSet maxDetermined = new BitSet();
        minDetermined.set(0, dimension);
        maxDetermined.set(0, dimension);

        ShingledBranch node = leaf.parent;
        ShingledNode previousNode = leaf;
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
     * Gets the sibling of a node
     */
    private ShingledNode getSibling(ShingledNode n) {
        ShingledBranch parent = n.parent;
        if (n.equals(parent.left)) {
            return parent.right;
        }
        return parent.left;
    }

    /**
     * Increases the leaf number for all ancestors above a given node by increment
     */
    private void updateLeafCountUpwards(ShingledNode node, int increment) {
        while (node != null) {
            node.num += increment;
            node = node.parent;
        }
    }

    /**
     * Finds the closest leaf to a point under a specified node
     */
    private ShingledLeaf query(long startIndex) {
        ShingledNode n = root;
        while (!(n instanceof ShingledLeaf)) {
            ShingledBranch b = (ShingledBranch) n;
            if (getPointValue(startIndex, b.cut.dim) <= b.cut.value) {
                n = b.left;
            } else {
                n = b.right;
            }
        }
        return (ShingledLeaf) n;
    }

    /**
     * The maximum number of nodes displaced by removing any subset of the tree including a leaf 
     * In practice, there are too many subsets to consider so it can be estimated by looking up the tree
     * There is no definitive algorithm to empirically calculate codisp, so the ratio of sibling num to node num is used
     */
    public int getCollusiveDisplacement(ShingledLeaf leaf) {
        if (leaf.equals(root)) {
            return 0;
        }

        ShingledNode node = leaf;
        int maxResult = -1;
        while (node != null) {
            ShingledBranch parent = node.parent;
            if (parent == null)
                break;
            ShingledNode sibling;
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
     * Returns a leaf containing a point if it exists
     */
    public ShingledLeaf findLeaf(long startIndex) {
        ShingledLeaf nearest = query(startIndex);
        if (nearest.startIndex == startIndex) {
            return nearest;
        }
        return null;
    }

    /**
     * Generates a random cut from the span of a point and bounding box
     */
    private Cut insertPointCut(long startIndex, double[] minPoint, double[] maxPoint) {
        double[] newMinBox = new double[minPoint.length];
        double[] span = new double[minPoint.length];
        // Cumulative sum of span
        double[] spanSum = new double[minPoint.length];
        for (int i = 0; i < dimension; i++) {
            newMinBox[i] = Math.min(minPoint[i], getPointValue(startIndex, i));
            double maxI = Math.max(maxPoint[i], getPointValue(startIndex, i));
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

    private double getPointValue(long startIndex, int offset) {
        return buffer.get(startIndex + offset);
    }

    /** 
     * Java doesn't have tuples :(
     */
    public static class Cut {
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