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
 * Memory-optimized version of general.Tree
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
    private double[] rootMinPoint; // Global minimum bounds for the entire tree
    private double[] rootMaxPoint; // Global maximum bounds for the entire tree

    /**
     * Constructs a SmallTree with specified random generator and shingle size.
     * The shingle size determines the dimensionality of the data points.
     * 
     * @param r Random generator for making random cuts in the tree
     * @param shingleSize Number of dimensions in each data point
     */
    public SmallTree(Random r, int shingleSize) {
        random = r;
        dimension = shingleSize;
        rootMinPoint = null;
        rootMaxPoint = null;
    }

    /**
     * Constructs a SmallTree with default random generator and specified shingle size.
     * 
     * @param shingleSize Number of dimensions in each data point
     */
    public SmallTree(int shingleSize) {
        this(new Random(), shingleSize);
    }

    /**
     * Creates a string representation of the tree structure for debugging/visualization.
     * Shows the tree hierarchy with cuts, bounding boxes, and leaf nodes.
     * 
     * @return String representation of the tree structure
     */
    @Override
    public String toString() {
        String[] depthAndTreeString = { "", "" }; // [0] = depth indentation, [1] = tree structure
        if (root == null) return "";
        double[] currentMinBox = rootMinPoint.clone();
        double[] currentMaxBox = rootMaxPoint.clone();
        printNodeToString(root, depthAndTreeString, currentMinBox, currentMaxBox);
        return depthAndTreeString[1];
    }

    /**
     * Recursively prints a node and its subtree to the provided string array.
     * Uses Unicode box-drawing characters to create a visual tree structure.
     * Updates the given string array: { depth, tree } strings
     * 
     * @param node Current node being printed
     * @param depthAndTreeString Array containing depth indentation and tree string
     * @param currentMinBox Current minimum bounding box at this level
     * @param currentMaxBox Current maximum bounding box at this level
     */
    private void printNodeToString(SmallNode node, String[] depthAndTreeString, double[] currentMinBox, double[] currentMaxBox) {
        // Lambda for adding branch indentation
        Consumer<Character> ppush = (c) -> {
            String branch = String.format(" %c  ", c);
            depthAndTreeString[0] += branch;
        };
        // Lambda for removing branch indentation
        Runnable ppop = () -> {
            depthAndTreeString[0] = depthAndTreeString[0].substring(0, depthAndTreeString[0].length() - 4);
        };
        
        if (node instanceof SmallLeaf) {
            // For leaves, print the bounding box (which represents the point location)
            depthAndTreeString[1] += String.format("(%s)\n", Arrays.toString(currentMinBox));
        } else if (node instanceof SmallBranch) {
            SmallBranch b = (SmallBranch)node;
            // Calculate bounding boxes for left and right children
            double[] leftMinBox = currentMinBox.clone();
            double[] leftMaxBox = currentMaxBox.clone();
            double[] rightMinBox = currentMinBox.clone();
            double[] rightMaxBox = currentMaxBox.clone();
            
            // Update bounding boxes based on child directions stored in branch
            for (int i = 0; i < dimension; i++) {
                // If bit is set, left child has this bound; otherwise right child has it
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
            
            // Print branch info with Unicode box characters (9472 = ─, 9500 = ├, 9474 = │, 9492 = └)
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
     * Applies a function to all leaf nodes in the tree.
     * Useful for operations that need to be performed on all data points.
     * 
     * @param func Function to apply to each leaf
     */
    public void mapLeaves(Consumer<SmallLeaf> func) {
        mapLeaves(func, root);
    }

    /**
     * Recursively applies a function to all leaf nodes in a subtree.
     * 
     * @param func Function to apply to each leaf
     * @param n Current node being processed
     */
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

    /**
     * Returns a copy of the global minimum bounding box of the tree.
     * 
     * @return Array representing minimum bounds in each dimension
     */
    public double[] getMinBox() {
        return rootMinPoint.clone();
    }

    /**
     * Returns a copy of the global maximum bounding box of the tree.
     * 
     * @return Array representing maximum bounds in each dimension
     */
    public double[] getMaxBox() {
        return rootMaxPoint.clone();
    }

    /**
     * Applies a function to all branch nodes in the tree.
     * Useful for operations that need to be performed on internal tree structure.
     * 
     * @param func Function to apply to each branch
     */
    public void mapBranches(Consumer<SmallBranch> func) {
        mapBranches(func, root);
    }

    /**
     * Recursively applies a function to all branch nodes in a subtree.
     * Note: Applies function in post-order (children first, then parent).
     * 
     * @param func Function to apply to each branch
     * @param n Current node being processed
     */
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
     * Removes a point from the tree and returns the deleted leaf node.
     * Handles duplicate points by decrementing count, and restructures tree if necessary.
     * 
     * @param point The data point to remove from the tree
     * @return The leaf node that was removed or had its count decremented
     * @throws NoSuchElementException if the point is not found in the tree
     */
    public SmallLeaf forgetPoint(double[] point) throws NoSuchElementException {
        SmallLeaf leaf = findLeaf(point);
        
        if (leaf == null) {
            throw new NoSuchElementException(String.format("Point not found: %s", Arrays.toString(point)));
        }

        // If duplicate points exist, just decrease count for all nodes above
        if (leaf.num > 1) {
            updateLeafCountUpwards(leaf, -1);
            return leaf;
        }

        // If leaf is the only node in tree, clear everything
        if (root.equals(leaf)) {
            root = null;
            rootMinPoint = null;
            rootMaxPoint = null;
            return leaf;
        }

        // Calculate parent and sibling for tree restructuring
        SmallBranch parent = leaf.parent;
        SmallNode sibling = getSibling(leaf);

        // If parent is root, promote sibling to root and update global bounds
        if (root.equals(parent)) {
            SmallBranch bRoot = (SmallBranch)root;
            for (int i = 0; i < dimension; i++) {
                // If leaf contributed to bounding box at dimension i, update to sibling's bounds
                // XOR logic: if leaf is left child and direction bit is false, OR leaf is right child and direction bit is true
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

        // Shrink bounding boxes up the tree before restructuring
        shrinkBoxUpwards(leaf);

        // Move sibling up one level, replacing parent
        SmallBranch grandparent = parent.parent;
        sibling.parent = grandparent;
        leaf.parent = null; // Clean up for potential reuse
        
        // Update grandparent's child pointer
        if (parent.equals(grandparent.left)) {
            grandparent.left = sibling;
        } else {
            grandparent.right = sibling;
        }

        // Update leaf counts for all ancestors
        updateLeafCountUpwards(grandparent, -1);

        return leaf;
    }

    /**
     * Inserts a new point into the tree, creating a new leaf or incrementing count for duplicates.
     * Uses random cuts to maintain the tree structure for anomaly detection.
     * 
     * @param point The data point to insert
     * @return The leaf node that was created or had its count incremented
     */
    public SmallLeaf insertPoint(double[] point) {
        // Validate input dimensions
        assert point.length == dimension;

        // Handle empty tree case
        if (root == null) {
            SmallLeaf leaf = new SmallLeaf();
            root = leaf;
            rootMinPoint = point.clone();
            rootMaxPoint = point.clone();
            return leaf;
        }

        // Check for duplicates and only update counts if point already exists
        SmallLeaf duplicate = findLeaf(point);
        if (duplicate != null) {
            updateLeafCountUpwards(duplicate, 1);
            return duplicate;
        }

        // Initialize variables for tree traversal and insertion
        SmallNode node = root;
        SmallBranch parent = null;
        SmallLeaf leaf = null;
        SmallBranch branch = null;
        boolean useLeftSide = false;
        double[] minPoint = rootMinPoint.clone(); // Current bounding box during traversal
        double[] maxPoint = rootMaxPoint.clone();

        // Update global bounding box to include new point
        for (int i = 0; i < dimension; i++) {
            if (point[i] < rootMinPoint[i]) {
                rootMinPoint[i] = point[i];
            }
            if (point[i] > rootMaxPoint[i]) {
                rootMaxPoint[i] = point[i];
            }
        }

        // Traverse tree until we find where to insert the new point
        while (true) {
            Cut c = insertPointCut(point, minPoint, maxPoint);
            
            // If cut is before current bounding box, insert new leaf on left
            if (c.value < minPoint[c.dim]) {
                leaf = new SmallLeaf();
                branch = new SmallBranch(c, dimension, leaf, node, leaf.num + node.num);
                break;
            // If cut is after current bounding box and point is after cut, insert on right
            } else if (c.value >= maxPoint[c.dim] && point[c.dim] > c.value) {
                leaf = new SmallLeaf();
                branch = new SmallBranch(c, dimension, node, leaf, leaf.num + node.num);
                break;
            } else {
                // Continue traversing down the tree
                SmallBranch b = (SmallBranch) node;
                parent = b;
                BitSet minSet = (BitSet)b.childMinPointDirections.clone();
                BitSet maxSet = (BitSet)b.childMaxPointDirections.clone();
                
                // Choose which child to follow based on cut value
                if (point[b.cut.dim] <= b.cut.value) {
                    node = b.left;
                    useLeftSide = true;
                } else {
                    node = b.right;
                    useLeftSide = false;
                    // Flip bits for right side (since directions are stored relative to left)
                    minSet.flip(0, dimension);
                    maxSet.flip(0, dimension);
                }

                double[] oldMin = minPoint.clone();
                double[] oldMax = maxPoint.clone();
                
                // Update bounding boxes as we traverse down
                // The new node will be inserted under this path, so update bounds iteratively
                for (int i = minSet.nextSetBit(0); i != -1; i = minSet.nextSetBit(i + 1)) {
                    minPoint[i] = b.childMinPointValues[i];
                }
                for (int i = maxSet.nextSetBit(0); i != -1; i = maxSet.nextSetBit(i + 1)) {
                    maxPoint[i] = b.childMaxPointValues[i];
                }

                // Update branch's bounding box information to include new point
                for (int i = 0; i < dimension; i++) {
                    // Update minimum bounds
                    if (useLeftSide == b.childMinPointDirections.get(i)) {
                        // If path we took contributes to min and new point is smaller
                        if (point[i] < oldMin[i]) {
                            b.childMinPointDirections.flip(i); // Switch direction
                            b.childMinPointValues[i] = oldMin[i]; // Store old min
                        } else {
                            // Update to smaller value between current and new point
                            b.childMinPointValues[i] = Math.min(b.childMinPointValues[i], point[i]);
                        }
                    }
                    
                    // Update maximum bounds (same logic as min)
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

        // Ensure we found a valid insertion point
        assert branch != null;

        // Configure newly created branch's bounding box information
        for (int i = 0; i < dimension; i++) {
            // minPoint and maxPoint now represent the bounding box of the leaf's sibling
            // Set direction bits: true if leaf contributes to bound, false if sibling does
            branch.childMinPointDirections.set(i, (point[i] < minPoint[i]) != leaf.equals(branch.left));
            branch.childMaxPointDirections.set(i, (point[i] > maxPoint[i]) != leaf.equals(branch.left));
            
            // Store the bound values (max for min direction, min for max direction)
            branch.childMinPointValues[i] = Math.max(point[i], minPoint[i]);
            branch.childMaxPointValues[i] = Math.min(point[i], maxPoint[i]);
        }

        // Link all nodes together
        node.parent = branch;
        leaf.parent = branch;
        branch.parent = parent;
        
        // Update parent's child pointer or set as new root
        if (parent != null) {
            if (useLeftSide) {
                parent.left = branch;
            } else {
                parent.right = branch;
            }
        } else {
            root = branch;
        }
        
        // Update leaf counts for all ancestors
        updateLeafCountUpwards(parent, 1);

        return leaf;
    }

    /**
     * Shrinks bounding boxes up the tree after a leaf removal.
     * Updates parent bounding boxes when a leaf that contributed to the bounds is removed.
     * This is a complex algorithm that tracks which dimensions need updating and propagates
     * changes up the tree until the impact is fully absorbed.
     * 
     * @param leaf The leaf that was removed, used as starting point for bound updates
     */
    private void shrinkBoxUpwards(SmallLeaf leaf) {
        // Track which dimensions of the bounding box are determined by the removed leaf
        BitSet minDetermined = new BitSet();
        BitSet maxDetermined = new BitSet();
        minDetermined.set(0, dimension); // Initially all dimensions might be affected
        maxDetermined.set(0, dimension);

        SmallBranch node = leaf.parent;
        SmallNode previousNode = leaf;
        // Alternative bounds from sibling subtrees
        double[] altMins = new double[dimension];
        Arrays.fill(altMins, Double.MAX_VALUE);
        double[] altMaxes = new double[dimension];
        Arrays.fill(altMaxes, -Double.MAX_VALUE); // Not Double.MIN_VALUE (which is smallest positive)!
        
        // Travel up the tree until all affected dimensions are resolved
		while ((!minDetermined.isEmpty() || !maxDetermined.isEmpty()) && node != null) {
            // Process dimensions where removed leaf contributed to minimum bound
            for (int i = minDetermined.nextSetBit(0); i != -1; i = minDetermined.nextSetBit(i + 1)) {
                // Check if current node absorbs the impact (other child now provides bound)
                if (previousNode.equals(node.left) != node.childMinPointDirections.get(i)) {
                    // Impact continues upward, but check if sibling provides better bound
                    if (node.childMinPointValues[i] < altMins[i]) {
                        // Update direction to point to current path
                        if (previousNode.equals(node.left)) {
                            node.childMinPointDirections.set(i);
                        } else {
                            node.childMinPointDirections.clear(i);
                        }
                        // Swap values: store sibling's bound, update alt with current
                        double temp = node.childMinPointValues[i];
                        node.childMinPointValues[i] = altMins[i];
                        altMins[i] = temp;
                    }
                } else {
                    // Impact absorbed at this level, use alternative bound
                    node.childMinPointValues[i] = altMins[i];
                    minDetermined.clear(i); // No longer needs to propagate up
                }
            }
            
            // Same logic for maximum bounds
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

        // If we reached the root, update global bounding boxes
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
     * Returns the sibling node of a given node.
     * Every non-root node has exactly one sibling in a binary tree.
     * 
     * @param n The node whose sibling to find
     * @return The sibling node
     */
    private SmallNode getSibling(SmallNode n) {
        SmallBranch parent = n.parent;
        if (n.equals(parent.left)) {
            return parent.right;
        }
        return parent.left;
    }

    /**
     * Updates the leaf count for all ancestor nodes by a given increment.
     * This maintains the count of leaves in each subtree, which is important
     * for anomaly scoring calculations.
     * 
     * @param node Starting node
     * @param increment Amount to add to each ancestor's leaf count (can be negative)
     */
    private void updateLeafCountUpwards(SmallNode node, int increment) {
        while (node != null) {
            node.num += increment;
            node = node.parent;
        }
    }

    /**
     * Finds the leaf node that contains a specific point.
     * Traverses the tree following cuts until reaching a leaf, then verifies
     * the leaf's bounding box matches the search point.
     * 
     * @param point The point to search for
     * @return The leaf containing the point, or null if not found
     */
    private SmallLeaf findLeaf(double[] point) {
        SmallNode n = root;
        double[] minPoint = rootMinPoint.clone();
        double[] maxPoint = rootMaxPoint.clone();
        
        // Traverse down tree, following cuts and updating bounding box
        while (!(n instanceof SmallLeaf)) {
            SmallBranch b = (SmallBranch) n;
            BitSet min = (BitSet)b.childMinPointDirections.clone();
            BitSet max = (BitSet)b.childMaxPointDirections.clone();
            
            // Choose child based on cut dimension and value
            if (point[b.cut.dim] <= b.cut.value) {
                n = b.left;
            } else {
                n = b.right;
                // Flip direction bits for right child
                min.flip(0, dimension);
                max.flip(0, dimension);
            }

            // Update bounding box based on path taken
            for (int i = min.nextSetBit(0); i != -1; i = min.nextSetBit(i + 1)) {
                minPoint[i] = b.childMinPointValues[i];
            }
            for (int i = max.nextSetBit(0); i != -1; i = max.nextSetBit(i + 1)) {
                maxPoint[i] = b.childMaxPointValues[i];
            }
        }
        
        // Verify that leaf's bounding box exactly matches the search point
        if (!Arrays.equals(point, minPoint) || !Arrays.equals(point, maxPoint)) {
            return null;
        }
        return (SmallLeaf) n;
    }

    /**
     * Calculates the collusive displacement for anomaly scoring.
     * This estimates the maximum number of nodes displaced by removing any subset
     * of the tree that includes the given leaf. Higher values indicate more anomalous points.
     * 
     * The algorithm traverses from leaf to root, calculating the displacement metric ("codisp")
     * at each level. Note that there is no definitive algorithm to calculate this metric,
     * we use (sibling subtree size / current subtree size) to be consistent with the original implementation.
     * 
     * @param leaf The leaf node to calculate displacement for
     * @return Maximum displacement ratio found along the path to root
     */
    public int getCollusiveDisplacement(SmallLeaf leaf) {
        if (leaf.equals(root)) {
            return 0; // Root cannot be displaced
        }

        SmallNode node = leaf;
        int maxResult = -1;
        
        // Traverse from leaf to root, calculating displacement at each level
        while (node != null) {
            SmallBranch parent = node.parent;
            if (parent == null)
                break;
                    
            int deleted = node.num; // Number of leaves in current subtree
            int displacement = getSibling(node).num; // Number of leaves in sibling subtree
            maxResult = Math.max(maxResult, displacement / deleted);
            node = parent;
        }
        return maxResult;
    }

    /**
     * Generates a random cut for inserting a point into the tree.
     * The cut is chosen randomly with probability proportional to the span
     * in each dimension of the combined bounding box of the point and current box.
     * 
     * @param point The point being inserted
     * @param minPoint Current minimum bounding box
     * @param maxPoint Current maximum bounding box
     * @return A Cut object specifying dimension and value for the cut
     */
    private Cut insertPointCut(double[] point, double[] minPoint, double[] maxPoint) {
        double[] newMinBox = new double[minPoint.length];
        double[] span = new double[minPoint.length];
        double[] spanSum = new double[minPoint.length]; // Cumulative sum for weighted selection
        
        // Calculate span in each dimension and cumulative sums
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
        
        // Weighted random selection based on span sizes
        double range = spanSum[spanSum.length - 1];
        double r = random.nextDouble() * range;
        int cutDim = -1;
        
        // Find dimension corresponding to random selection
        for (int i = 0; i < dimension; i++) {
            if (spanSum[i] >= r) {
                cutDim = i;
                break;
            }
        }
        assert cutDim > -1;
        
        // Calculate cut value within the selected dimension
        double value = newMinBox[cutDim] + spanSum[cutDim] - r;
        return new Cut(cutDim, value);
    }

    /** 
     * Represents a cut in the tree - a dimension and value that splits the space.
     * Necessary because Java doesn't have tuples.
     */
    public static class Cut implements Serializable {
        public int dim;    // Dimension of the cut (0 to dimension-1)
        public double value; // Value where the cut is made in that dimension

        public Cut(int d, double v) {
            dim = d;
            value = v;
        }
    }
}
