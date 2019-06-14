package rrcf.general;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;
import java.io.Serializable;

/**
 * Robust random cut tree data structure used for anomaly detection on streaming
 * data
 * 
 * Represents a single random cut tree, supporting generalized data points of any dimension
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
public class RCTree implements Serializable {
    private RCNode root;
    // Number of dimensions for each point
    private int ndim;
    // Allows leaves to be accessed with external key
    private Map<Object, RCLeaf> leavesMap;
    private Random random;

    public RCTree(Random r) {
        leavesMap = new HashMap<>();
        random = r;
    }

    public RCTree() {
        this(new Random());
    }

    @Override
    public String toString() {
        String[] depthAndTreeString = { "", "" };
        printNodeToString(root, depthAndTreeString);
        return depthAndTreeString[1];
    }

    /**
     * Number of points stored in the tree
     */
    public int size() {
        return leavesMap.size();
    }

    /**
     * Prints a node to provided string
     * Updates the given string array: { depth, tree } strings
     */
    private void printNodeToString(RCNode node, String[] depthAndTreeString) {
        Consumer<Character> ppush = (c) -> {
            String branch = String.format(" %c  ", c);
            depthAndTreeString[0] += branch;
        };
        Runnable ppop = () -> {
            depthAndTreeString[0] = depthAndTreeString[0].substring(0, depthAndTreeString[0].length() - 4);
        };
        if (node instanceof RCLeaf) {
            depthAndTreeString[1] += String.format("(%s)\n", Arrays.toString(node.point));
        } else if (node instanceof RCBranch) {
            depthAndTreeString[1] += String.format("%c+\n", 9472);
            depthAndTreeString[1] += String.format("%s %c%c%c", depthAndTreeString[0], 9500, 9472, 9472);
            ppush.accept((char) 9474);
            printNodeToString(((RCBranch) node).left, depthAndTreeString);
            ppop.run();
            depthAndTreeString[1] += String.format("%s %c%c%c", depthAndTreeString[0], 9492, 9472, 9472);
            ppush.accept(' ');
            printNodeToString(((RCBranch) node).right, depthAndTreeString);
            ppop.run();
        }
    }

    public void mapLeaves(Consumer<RCLeaf> func) {
        mapLeaves(func, root);
    }

    private void mapLeaves(Consumer<RCLeaf> func, RCNode n) {
        if (n instanceof RCLeaf) {
            func.accept((RCLeaf) n);
        } else {
            RCBranch b = (RCBranch) n;
            if (b.left != null) {
                mapLeaves(func, b.left);
            }
            if (b.right != null) {
                mapLeaves(func, b.right);
            }
        }
    }

    public void mapBranches(Consumer<RCBranch> func) {
        mapBranches(func, root);
    }

    private void mapBranches(Consumer<RCBranch> func, RCNode n) {
        if (!(n instanceof RCLeaf)) {
            RCBranch b = (RCBranch) n;
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
    public RCNode forgetPoint(Object index) {
        RCNode leaf = leavesMap.get(index);

        // If duplicate points exist, decrease num for all nodes above
        if (leaf.num > 1) {
            updateLeafCountUpwards(leaf, -1);
            return leavesMap.remove(index);
        }

        // If leaf is root
        if (root.equals(leaf)) {
            root = null;
            ndim = -1;
            return leavesMap.remove(index);
        }

        // Calculate parent and sibling
        RCBranch parent = leaf.parent;
        RCNode sibling = getSibling(leaf);

        // If parent is root, set sibling to root and update depths
        if (root.equals(parent)) {
            sibling.parent = null;
            leaf.parent = null; // In case the returned node is used somehow
            root = sibling;
            increaseLeafDepth(-1, sibling);
            return leavesMap.remove(index);
        }

        // Move sibling up a layer and link nodes
        RCBranch grandparent = parent.parent;
        sibling.parent = grandparent;
        if (parent.equals(grandparent.left)) {
            grandparent.left = sibling;
        } else {
            grandparent.right = sibling;
        }
        parent = grandparent;

        // Update depths
        increaseLeafDepth(-1, sibling);
        // Update leaf counts for each branch
        updateLeafCountUpwards(parent, -1);
        // Update bounding boxes
        shrinkBoxUp(parent, leaf.point[0]);
        return leavesMap.remove(index);
    }

    /**
     * Insert a point into the tree with a given index and create a new leaf
     */
    public RCLeaf insertPoint(float[] point, Object index) {
        // If no points, set necessary variables
        if (root == null) {
            RCLeaf leaf = new RCLeaf(point, 0);
            root = leaf;
            ndim = point.length;
            return leavesMap.put(index, leaf);
        }

        // Check that dimensions are consistent and index doesn't exist
        assert point.length == ndim;
        assert !leavesMap.containsKey(index);

        // Check for duplicates and only update counts if it exists
        RCLeaf duplicate = findLeaf(point);
        if (duplicate != null) {
            updateLeafCountUpwards(duplicate, 1);
            leavesMap.put(index, duplicate);
            return duplicate;
        }

        // No duplicates found, continue
        RCNode node = root;
        RCBranch parent = null;
        RCLeaf leaf = null;
        RCBranch branch = null;
        boolean useLeftSide = false;
        // Traverse tree until insertion spot found
        for (int i = 0; i < size(); i++) {
            float[][] bbox = node.point;
            Cut c = insertPointCut(point, bbox);
            if (c.value <= bbox[0][c.dim]) {
                leaf = new RCLeaf(point, i);
                branch = new RCBranch(c, leaf, node, leaf.num + node.num);
                break;
            } else if (c.value >= bbox[bbox.length - 1][c.dim]) {
                leaf = new RCLeaf(point, i);
                branch = new RCBranch(c, node, leaf, leaf.num + node.num);
                break;
            } else {
                RCBranch b = (RCBranch) node;
                parent = b;
                if (point[b.cut.dim] <= b.cut.value) {
                    node = b.left;
                    useLeftSide = true;
                } else {
                    node = b.right;
                    useLeftSide = false;
                }
            }
        }

        // Check if cut was found
        assert branch != null;

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

        increaseLeafDepth(1, branch);
        updateLeafCountUpwards(parent, 1);
        expandBoxUp(branch);
        leavesMap.put(index, leaf);
        return leaf;
    }

    /**
     * Gets the sibling of a node
     */
    private RCNode getSibling(RCNode n) {
        RCBranch parent = n.parent;
        if (n.equals(parent.left)) {
            return parent.right;
        }
        return parent.left;
    }

    /**
     * Increases the leaf number for all ancestors above a given node by increment
     */
    private void updateLeafCountUpwards(RCNode node, int increment) {
        while (node != null) {
            node.num += increment;
            node = node.parent;
        }
    }

    /**
     * When a point is deleted, contract bounding box of nodes above point
     * If the deleted point was on the boundary for any dimension
     */
    private void shrinkBoxUp(RCBranch node, float[] point) {
        while (node != null) {
            // Check if any of the current box's values match the point
            // Can exit otherwise, no shrinking necessary
            for (int i = 0; i < ndim; i++) {
                if (node.point[0][i] == point[i] || node.point[node.point.length - 1][i] == point[i]) {
                    break;
                }
                if (i == ndim - 1) {
                    // None equal
                    return;
                }
            }
            node.point = mergeChildrenBoxes(node);
            node = node.parent;
        }
    }

    /**
     * When a point is inserted, expand bounding box of nodes above new point
     */
    private void expandBoxUp(RCBranch node) {
        float[][] bbox = mergeChildrenBoxes(node);
        node.point = bbox;
        node = node.parent;
        while (node != null) {
            boolean anyChanged = false;
            for (int i = 0; i < ndim; i++) {
                if (bbox[0][i] < node.point[0][i]) {
                    node.point[0][i] = bbox[0][i];
                    anyChanged = true;
                }
                if (bbox[bbox.length - 1][i] > node.point[node.point.length - 1][i]) {
                    node.point[node.point.length - 1][i] = bbox[bbox.length - 1][i];
                    anyChanged = true;
                }
            }
            if (!anyChanged) {
                return;
            }
            node = node.parent;
        }
    }

    /**
     * Get bounding box of branch based on its children
     */
    private float[][] mergeChildrenBoxes(RCBranch node) {
        float[][] box = new float[2][ndim];
        for (int i = 0; i < ndim; i++) {
            box[0][i] = Math.min(node.left.point[0][i], node.right.point[0][i]);
            box[1][i] = Math.max(node.left.point[node.left.point.length - 1][i],
                    node.right.point[node.right.point.length - 1][i]);
        }
        return box;
    }

    /**
     * Adds `increment` to all leaves' depths under a node
     */
    private void increaseLeafDepth(int increment, RCNode n) {
        mapLeaves((leaf) -> {
            leaf.depth += increment;
        }, n);
    }

    /**
     * Wrapper for query from root
     */
    public RCLeaf query(float[] point) {
        return query(point, root);
    }

    /**
     * Finds the closest leaf to a point under a specified node
     */
    private RCLeaf query(float[] point, RCNode n) {
        while (!(n instanceof RCLeaf)) {
            RCBranch b = (RCBranch) n;
            if (point[b.cut.dim] <= b.cut.value) {
                n = b.left;
            } else {
                n = b.right;
            }
        }
        return (RCLeaf) n;
    }

    /**
     * Wrapper for getDisplacment by leaf
     */
    public int getDisplacement(Object key) {
        return getDisplacement(leavesMap.get(key));
    }

    /**
     * The number of nodes displaced by removing a leaf
     * Removing a leaf shorts the sibling to the leaf's grandparent, so displacement is the sibling's count
     * Can serve as a measure for outliers, but is affected by masking
     */
    public int getDisplacement(RCLeaf leaf) {
        if (leaf.equals(root)) {
            return 0;
        }
        RCBranch parent = leaf.parent;
        RCNode sibling = getSibling(leaf);
        return sibling.num;
    }

    /**
     * Wrapper for gcd by leaf
     */
    public int getCollusiveDisplacement(Object key) {
        return getCollusiveDisplacement(leavesMap.get(key));
    }

    /**
     * The maximum number of nodes displaced by removing any subset of the tree including a leaf 
     * In practice, there are too many subsets to consider so it can be estimated by looking up the tree
     * There is no definitive algorithm to empirically calculate codisp, so the ratio of sibling num to node num is used
     */
    public int getCollusiveDisplacement(RCLeaf leaf) {
        if (leaf.equals(root)) {
            return 0;
        }

        RCNode node = leaf;
        int maxResult = -1;
        for (int i = 0; i < leaf.depth; i++) {
            RCBranch parent = node.parent;
            if (parent == null)
                break;
            RCNode sibling;
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
    public RCLeaf findLeaf(float[] point) {
        RCLeaf nearest = query(point);
        if (nearest.point[0].equals(point)) {
            return nearest;
        }
        return null;
    }

    /**
     * Generates a random cut from the span of a point and bounding box
     */
    private Cut insertPointCut(float[] point, float[][] bbox) {
        float[][] newBox = new float[bbox.length][bbox[0].length];
        float[] span = new float[bbox[0].length];
        // Cumulative sum of span
        float[] spanSum = new float[bbox[0].length];
        for (int i = 0; i < ndim; i++) {
            newBox[0][i] = Math.min(bbox[0][i], point[i]);
            newBox[newBox.length - 1][i] = Math.max(bbox[bbox.length - 1][i], point[i]);
            span[i] =  newBox[newBox.length - 1][i] - newBox[0][i];
            if (i > 0) {
                spanSum[i] = spanSum[i - 1] + span[i];
            } else {
                spanSum[i] = span[0];
            }
        }
        // Weighted random with each dimension's span
        float range = spanSum[spanSum.length - 1];
        float r = random.nextFloat() * range;
        int dimension = -1;
        for (int i = 0; i < ndim; i++) {
            // Finds first value greater or equal to chosen
            if (spanSum[i] >= r) {
                dimension = i;
                break;
            }
        }
        assert dimension > -1;
        float value = newBox[0][dimension] + spanSum[dimension] - r;
        return new Cut(dimension, value);
    }

    /** 
     * Java doesn't have tuples :(
     */
    public static class Cut {
        // Dimension of cut
        public int dim;
        // Value of cut
        public float value;

        public Cut(int d, float v) {
            dim = d;
            value = v;
        }
    }
}