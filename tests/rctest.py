import math
import numpy as np
import rrcf

treenum = 40
forest = [rrcf.RCTree() for _ in range(treenum)]
print('"x","y","value"')
for i in np.arange(0, 30, 0.01):
	k = np.array([math.sin(i)])
	if i > 20 and i < 23:
		k[0] = 0.2
	accum = 0
	for t in forest:
		t.insert_point(k, index=i)
		accum += t.codisp(i)
	print(f"{i}, {k[0]}, {accum / treenum}")
