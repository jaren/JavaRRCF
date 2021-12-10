package rrcf.general;

import java.util.Map;
import java.util.ArrayList;
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
public class Tree implements Serializable {
    private Node root;
    // Number of dimensions for each point
    private int ndim;
    // Allows leaves to be accessed with external key
    private Map<Object, Leaf> leavesMap;
    private Random random;

    public Tree(Random r, double[][] points) {
        leavesMap = new HashMap<>();
        random = r;
        if (points.length == 0) {
            return;
        }
        int[] indices = new int[points.length];
        for (int i = 0; i < points.length; i++) {
            indices[i] = i;
        }
        // Has to be before tree building
        ndim = points[0].length;
        root = buildTreeDown(points, indices, 0);
    }

    public Tree(double[][] points) {
        this(new Random(), points);
    }

    public Tree(Random r) {
        this(r, new double[0][0]);
    }

    public Tree() {
        this(new Random());
    }

    private Node buildTreeDown(double[][] points, int[] indices, int depth) {
        // TODO: Somewhat inefficient, potentially improve?
        double[][] box = getBoxFromPoints(points);
        // Check if the array is all duplicates and return a leaf if it is
        for (int i = 0; i < box[0].length; i++) {
            if (box[0][i] != box[box.length - 1][i]) {
                break;
            }

            // All duplicates found
            if (i == box[0].length - 1) {
                Leaf leaf = new Leaf(box[0], depth);
                leaf.num = points.length;
                for (int d : indices) {
                    leavesMap.put(d, leaf);
                }
                return leaf;
            }
        }
        Cut c = insertCut(box);
        boolean[] goesLeft = new boolean[points.length];
        int leftSize = 0;
        for (int i = 0; i < points.length; i++) {
            if (points[i][c.dim] <= c.value) {
                goesLeft[i] = true;
                leftSize++;
            } else {
                goesLeft[i] = false;
            }
        }
        int[] leftI = new int[leftSize];
        double[][] leftP = new double[leftSize][points[0].length];
        int[] rightI = new int[points.length - leftSize];
        double[][] rightP = new double[points.length - leftSize][points[0].length];
        int leftIndex = 0;
        int rightIndex = 0;
        for (int i = 0; i < points.length; i++) {
            if (goesLeft[i]) {
                leftI[leftIndex] = indices[i];
                leftP[leftIndex] = points[i];
                leftIndex++;
            } else {
                rightI[rightIndex] = indices[i];
                rightP[rightIndex] = points[i];
                rightIndex++;
            }
        }
        Node left = buildTreeDown(leftP, leftI, depth + 1);
        Node right = buildTreeDown(rightP, rightI, depth + 1);
        Branch branch = new Branch(c, left, right, left.num + right.num);
        branch.point = mergeChildrenBoxes(branch);
        left.parent = branch;
        right.parent = branch;
        return branch;
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
    private void printNodeToString(Node node, String[] depthAndTreeString) {
        Consumer<Character> ppush = (c) -> {
            String branch = String.format(" %c  ", c);
            depthAndTreeString[0] += branch;
        };
        Runnable ppop = () -> {
            depthAndTreeString[0] = depthAndTreeString[0].substring(0, depthAndTreeString[0].length() - 4);
        };
        if (node instanceof Leaf) {
            depthAndTreeString[1] += String.format("(%s)\n", Arrays.toString(node.point[0]));
        } else if (node instanceof Branch) {
            Branch b = (Branch)node;
            depthAndTreeString[1] += String.format("%c+ cut: (%d, %f), box: (%s, %s)\n", 9472, b.cut.dim, b.cut.value, Arrays.toString(b.point[0]), Arrays.toString(b.point[b.point.length - 1]));
            depthAndTreeString[1] += String.format("%s %c%c%c", depthAndTreeString[0], 9500, 9472, 9472);
            ppush.accept((char) 9474);
            printNodeToString(b.left, depthAndTreeString);
            ppop.run();
            depthAndTreeString[1] += String.format("%s %c%c%c", depthAndTreeString[0], 9492, 9472, 9472);
            ppush.accept(' ');
            printNodeToString(b.right, depthAndTreeString);
            ppop.run();
        }
    }

    public void mapLeaves(Consumer<Leaf> func) {
        mapLeaves(func, root);
    }

    private void mapLeaves(Consumer<Leaf> func, Node n) {
        if (n instanceof Leaf) {
            func.accept((Leaf) n);
        } else {
            Branch b = (Branch) n;
            if (b.left != null) {
                mapLeaves(func, b.left);
            }
            if (b.right != null) {
                mapLeaves(func, b.right);
            }
        }
    }

    public void mapBranches(Consumer<Branch> func) {
        mapBranches(func, root);
    }

    private void mapBranches(Consumer<Branch> func, Node n) {
        if (!(n instanceof Leaf)) {
            Branch b = (Branch) n;
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
    public Node forgetPoint(Object index) {
        Node leaf = leavesMap.get(index);

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
        Branch parent = leaf.parent;
        Node sibling = getSibling(leaf);

        // If parent is root, set sibling to root and update depths
        if (root.equals(parent)) {
            sibling.parent = null;
            leaf.parent = null; // In case the returned node is used somehow
            root = sibling;
            increaseLeafDepth(-1, sibling);
            return leavesMap.remove(index);
        }

        // Move sibling up a layer and link nodes
        Branch grandparent = parent.parent;
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
    public Leaf insertPoint(double[] point, Object index) {
        // If no points, set necessary variables
        if (root == null) {
            Leaf leaf = new Leaf(point, 0);
            root = leaf;
            ndim = point.length;
            return leavesMap.put(index, leaf);
        }

        // Check that dimensions are consistent and index doesn't exist
        assert point.length == ndim;
        assert !leavesMap.containsKey(index);

        // Check for duplicates and only update counts if it exists
        Leaf duplicate = findLeaf(point);
        if (duplicate != null) {
            updateLeafCountUpwards(duplicate, 1);
            leavesMap.put(index, duplicate);
            return duplicate;
        }

        // No duplicates found, continue
        Node node = root;
        Branch parent = null;
        Leaf leaf = null;
        Branch branch = null;
        boolean useLeftSide = false;
        // Traverse tree until insertion spot found
        for (int i = 0; i < size(); i++) {
            double[][] bbox = node.point;
            Cut c = insertPointCut(point, bbox);
            if (c.value <= bbox[0][c.dim]) {
                leaf = new Leaf(point, i);
                branch = new Branch(c, leaf, node, leaf.num + node.num);
                break;
            } else if (c.value >= bbox[bbox.length - 1][c.dim]) {
                leaf = new Leaf(point, i);
                branch = new Branch(c, node, leaf, leaf.num + node.num);
                break;
            } else {
                Branch b = (Branch) node;
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
    private Node getSibling(Node n) {
        Branch parent = n.parent;
        if (n.equals(parent.left)) {
            return parent.right;
        }
        return parent.left;
    }

    /**
     * Increases the leaf number for all ancestors above a given node by increment
     */
    private void updateLeafCountUpwards(Node node, int increment) {
        while (node != null) {
            node.num += increment;
            node = node.parent;
        }
    }

    /**
     * When a point is deleted, contract bounding box of nodes above point
     * If the deleted point was on the boundary for any dimension
     */
    private void shrinkBoxUp(Branch node, double[] point) {
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
    private void expandBoxUp(Branch node) {
        double[][] bbox = mergeChildrenBoxes(node);
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
    private double[][] mergeChildrenBoxes(Branch node) {
        double[][] box = new double[2][ndim];
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
    private void increaseLeafDepth(int increment, Node n) {
        mapLeaves((leaf) -> {
            leaf.depth += increment;
        }, n);
    }

    /**
     * Wrapper for query from root
     */
    public Leaf query(double[] point) {
        return query(point, root);
    }

    /**
     * Finds the closest leaf to a point under a specified node
     */
    private Leaf query(double[] point, Node n) {
        while (!(n instanceof Leaf)) {
            Branch b = (Branch) n;
            if (point[b.cut.dim] <= b.cut.value) {
                n = b.left;
            } else {
                n = b.right;
            }
        }
        return (Leaf) n;
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
    public int getDisplacement(Leaf leaf) {
        if (leaf.equals(root)) {
            return 0;
        }
        Branch parent = leaf.parent;
        Node sibling = getSibling(leaf);
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
    public int getCollusiveDisplacement(Leaf leaf) {
        if (leaf.equals(root)) {
            return 0;
        }

        Node node = leaf;
        int maxResult = -1;
        while (node.parent != null) {
            Node sibling = getSibling(node);
            int deleted = node.num;
            int displacement = sibling.num;
            maxResult = Math.max(maxResult, displacement / deleted);
            node = node.parent;
        }
        return maxResult;
    }

    /**
     * Returns a leaf containing a point if it exists
     */
    public Leaf findLeaf(double[] point) {
        Leaf nearest = query(point);
        if (Arrays.equals(nearest.point[0], point)) {
            return nearest;
        }
        return null;
    }

    private double[][] getBoxFromPoints(double[][] points) {
        double[][] box = new double[2][points[0].length];
        for (int i = 0; i < points[0].length; i++) {
            box[0][i] = Double.MAX_VALUE;
            box[box.length - 1][i] = -Double.MAX_VALUE;
        }

        // For all dimensions
        for (int j = 0; j < points[0].length; j++) {
            // For all points, set box to min and max
            for (int i = 0; i < points.length; i++) {
                box[0][j] = Math.min(box[0][j], points[i][j]);
                box[box.length - 1][j] = Math.max(box[box.length - 1][j], points[i][j]);
            }
        }
        return box;
    }

    /**
     * Generates a random cut from the span of a bounding box
     */
    private Cut insertCut(double[][] bbox) {
        double[] span = new double[bbox[0].length];
        // Cumulative sum of span
        double[] spanSum = new double[bbox[0].length];

        for (int i = 0; i < bbox[0].length; i++) {
            span[i] =  bbox[bbox.length - 1][i] - bbox[0][i];
            if (i > 0) {
                spanSum[i] = spanSum[i - 1] + span[i];
            } else {
                spanSum[i] = span[0];
            }
        }

        // Weighted random with each dimension's span
        double range = spanSum[spanSum.length - 1];
        double r = random.nextDouble() * range;
        int dimension = -1;
        for (int i = 0; i < bbox[0].length; i++) {
            // Finds first value greater than chosen
            if (spanSum[i] > r) {
                dimension = i;
                break;
            }
        }
        assert dimension > -1;
        double value = bbox[0][dimension] + spanSum[dimension] - r;
        return new Cut(dimension, value);
    }

    /**
     * Generates a random cut from the span of a point and bounding box
     */
    private Cut insertPointCut(double[] point, double[][] bbox) {
        double[][] newBox = new double[2][bbox[0].length];
        for (int i = 0; i < ndim; i++) {
            newBox[0][i] = Math.min(bbox[0][i], point[i]);
            newBox[newBox.length - 1][i] = Math.max(bbox[bbox.length - 1][i], point[i]);
        }
        return insertCut(newBox);
    }

    /** 
     * Java doesn't have tuples :(
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
