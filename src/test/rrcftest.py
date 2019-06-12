import sys
import numpy as np
import rrcf

# Reads y values from stdin and outputs (index, y, codisp)
# Args: shingle_size num_trees tree_size

shingle_size = int(sys.argv[1])
num_trees = int(sys.argv[2])
tree_size = int(sys.argv[3])

forest = []
for _ in range(num_trees):
    tree = rrcf.RCTree()
    forest.append(tree)
    
generator = (float(x) for x in sys.stdin)
points = rrcf.shingle(generator, size=shingle_size)

print('"x","y","value"')
for index, point in enumerate(points):
    avg = 0
    for tree in forest:
        if len(tree.leaves) > tree_size:
            tree.forget_point(index - tree_size)
        tree.insert_point(point, index=index)
        avg += tree.codisp(index)
    print(f"{index},{point[0]},{avg / num_trees}")