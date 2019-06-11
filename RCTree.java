//package rrcf;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Robust random cut tree data structure Used for anomaly detection on streaming
 * data
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

public class RCTree {
    // TODO: Generic type for object
    // TODO: getSibling function
    // TODO: Add tolerance
    // TODO: Support existing constructor
    private Node root;
    private int ndim;
    private Map<Object, Leaf> leavesMap;
    private Random random;

    public RCTree() {
        random = new Random();
        leavesMap = new HashMap<>();
    }

    @Override
    public String toString() {
        String[] depthAndTreeString = { "", "" };
        printNodeToString(root, depthAndTreeString);
        return depthAndTreeString[1];
    }

    private void printNodeToString(Node node, String[] depthAndTreeString) {
        Consumer<Character> ppush = (c) -> {
            String branch = String.format(" %c  ", c);
            depthAndTreeString[0] += branch;
        };
        Runnable ppop = () -> {
            depthAndTreeString[0] = depthAndTreeString[0].substring(0, depthAndTreeString[0].length() - 4);
        };
        if (node instanceof Leaf) {
            depthAndTreeString[1] += String.format("(%s)\n", ((Leaf) node).index);
        } else if (node instanceof Branch) {
            depthAndTreeString[1] += String.format("%c+\n", 9472);
            depthAndTreeString[1] += String.format("%s %c%c%c", depthAndTreeString[0], 9500, 9472, 9472);
            ppush.accept((char) 9474);
            printNodeToString(((Branch) node).left, depthAndTreeString);
            ppop.run();
            depthAndTreeString[1] += String.format("%s %c%c%c", depthAndTreeString[0], 9492, 9472, 9472);
            ppush.accept(' ');
            printNodeToString(((Branch) node).right, depthAndTreeString);
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

    // Delete a leaf from the tree and return deleted node
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
        Node sibling;
        if (leaf.equals(parent.left)) {
            sibling = parent.right;
        } else {
            sibling = parent.left;
        }

        // If parent is root, set sibling to root and update depths
        if (root.equals(parent)) {
            // TODO: { del parent }
            sibling.parent = null;
            root = sibling;

            if (sibling instanceof Leaf) {
                ((Leaf) sibling).depth = 0;
            } else {
                mapLeaves(genIncDepth(-1), sibling);
            }
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
        mapLeaves(genIncDepth(-1), sibling);
        // Update leaf counts for each branch
        updateLeafCountUpwards(parent, -1);
        // Update bounding boxes
        tightenBoxUpwards(parent, leaf.point[0]);
        return leavesMap.remove(index);
    }

    // Insert a point into the tree and create a new leaf
    public Leaf insertPoint(double[] point, Object index) {
        // If no points, set necessary variables
        if (root == null) {
            Leaf leaf = new Leaf(point, index, 0);
            root = leaf;
            ndim = point.length;
            return leavesMap.put(index, leaf);
        }

        // Check that dimensions are consistent and index doesn't exist
        assert point.length == ndim;
        assert !leavesMap.containsKey(index);

        // Check for duplicates and only update counts if it exists
        Leaf duplicate = findDuplicate(point);
        if (duplicate != null) {
            updateLeafCountUpwards(duplicate, 1);
            leavesMap.put(index, duplicate);
            return duplicate;
        }

        // No duplicates found, continue
        Node node = root;
        Branch parent = node.parent;
        Leaf leaf = null;
        Branch branch = null;
        int depth = 0;
        int maxDepth = -1;
        boolean useLeftSide = false;
        // TODO: O(n) operation, maybe cache?
        for (Leaf l : leavesMap.values()) {
            if (l.depth > maxDepth)
                maxDepth = l.depth;
        }
        // Traverse tree until insertion spot found
        for (int i = 0; i < maxDepth + 1; i++) {
            double[][] bbox = node.point;
            Cut c = insertPointCut(point, bbox);
            if (c.value < bbox[0][c.dim] || c.value >= bbox[bbox.length - 1][c.dim]) {
                leaf = new Leaf(point, index, depth);
                branch = new Branch(c.dim, c.value, node, leaf, leaf.num + node.num);
                break;
            } else {
                depth += 1;
                Branch b = (Branch) node;
                parent = b;
                if (point[b.cutDimension] <= b.cutValue) {
                    node = b.left;
                    useLeftSide = true;
                } else {
                    node = b.right;
                    useLeftSide = false;
                }
            }
            ;
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

        mapLeaves(genIncDepth(1), branch);
        updateLeafCountUpwards(parent, 1);
        relaxBoxUpwards(branch);
        leavesMap.put(index, leaf);
        return leaf;
    }

    private void updateLeafCountUpwards(Node leaf, int increment) {
        while (leaf != null) {
            leaf.num += increment;
            leaf = leaf.parent;
        }
    }

    // When a point is deleted, contract bounding box of nodes above point if
    // possible
    private void tightenBoxUpwards(Branch node, double[] point) {
        while (node != null) {
            double[][] bbox = lrBranchBox(node);
            for (int i = 0; i < ndim; i++) {
                if (bbox[0][i] == point[i] || bbox[bbox.length - 1][i] == point[i]) {
                    return;
                }
            }
            // TODO: Reference copy is okay?
            node.point = bbox;
            node = node.parent;
        }
    }

    // TODO: Make sure these two are not flipped ^v

    // When a point is inserted, expand bounding box of nodes above new point
    private void relaxBoxUpwards(Branch node) {
        double[][] bbox = lrBranchBox(node);
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

    private double[][] lrBranchBox(Branch node) {
        double[][] box = new double[2][ndim];
        for (int i = 0; i < ndim; i++) {
            box[0][i] = Math.min(node.left.point[0][i], node.right.point[0][i]);
            box[1][i] = Math.max(node.left.point[node.left.point.length - 1][i],
                    node.right.point[node.right.point.length - 1][i]);
        }
        return box;
    }

    private Consumer<Leaf> genIncDepth(int increment) {
        return (leaf) -> {
            leaf.depth += increment;
        };
    }

    public Leaf query(double[] point) {
        return query(point, root);
    }

    private Leaf query(double[] point, Node n) {
        if (n instanceof Leaf) {
            return (Leaf) n;
        }
        Branch b = (Branch) n;
        if (point[b.cutDimension] <= b.cutValue) {
            return query(point, b.left);
        }
        return query(point, b.right);
    }

    public int getDisplacement(Object key) {
        return getDisplacement(leavesMap.get(key));
    }

    public int getDisplacement(Leaf leaf) {
        if (leaf.equals(root)) {
            return 0;
        }
        Branch parent = leaf.parent;
        Node sibling;
        if (leaf.equals(parent.left)) {
            sibling = parent.right;
        } else {
            sibling = parent.left;
        }
        return sibling.num;
    }

    public int getCollusiveDisplacement(Object key) {
        return getCollusiveDisplacement(leavesMap.get(key));
    }

    public int getCollusiveDisplacement(Leaf leaf) {
        if (leaf.equals(root)) {
            return 0;
        }

        Node node = leaf;
        int maxResult = -1;
        for (int i = 0; i < leaf.depth; i++) {
            Branch parent = node.parent;
            if (parent == null)
                break;
            Node sibling;
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

    public double[][] getBBox() {
        return getBBox((Branch) root);
    }

    public double[][] getBBox(Branch n) {
        double[][] box = new double[2][ndim];
        for (int i = 0; i < ndim; i++) {
            box[0][i] = Double.MIN_VALUE;
            box[1][i] = Double.MAX_VALUE;
        }
        mapLeaves((leaf) -> {
            for (int i = 0; i < leaf.point.length; i++) {
                if (leaf.point[0][i] < box[0][i]) {
                    box[0][i] = leaf.point[0][i];
                } else if (leaf.point[0][i] > box[1][i]) {
                    box[1][i] = leaf.point[0][i];
                }
            }
        });
        return box;
    }

    // Returns a leaf containing a point if it exists
    public Leaf findDuplicate(double[] point) {
        Leaf nearest = query(point);
        if (nearest.point[0].equals(point)) {
            return nearest;
        }
        return null;
    }

    private Cut insertPointCut(double[] point, double[][] bbox) {
        double[][] newBox = new double[bbox.length][bbox[0].length];
        double[] span = new double[bbox[0].length];
        double[] spanSum = new double[bbox[0].length];
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
        // Random cut
        double range = spanSum[spanSum.length - 1];
        double r = random.nextDouble() * range;
        int dimension = -1;
        for (int i = 0; i < ndim; i++) {
            if (spanSum[i] >= r) {
                dimension = i;
                break;
            }
        }
        double value = newBox[0][dimension] + spanSum[dimension] - r;
        return new Cut(dimension, value);
    }

    public static class Cut {
        public int dim;
        public double value;

        public Cut(int d, double v) {
            dim = d;
            value = v;
        }
    }

    public static class Node {
        public Branch parent;
        public int num;

        public double[][] point;
    }

    public static class Branch extends Node {
        public int cutDimension;
        public double cutValue;
        public Node left;
        public Node right;

        public Branch(int dim, double cut, Node l, Node r, int n) {
            cutDimension = dim;
            cutValue = cut;
            left = l;
            right = r;
            num = n;
        }
    }

    public static class Leaf extends Node {
        public Object index;
        public int depth;

        public Leaf(double[] p, Object i, int d) {
            point = new double[1][p.length];
            point[0] = p;
            index = i;
            depth = d;
            num = 1;
        }
    }
}