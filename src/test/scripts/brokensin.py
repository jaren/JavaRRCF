import math, random

# Generates an anomalous noisy sine wave for use with the test scripts 

for i in range(800):
    k = 50 * (math.sin(2 * math.pi / 100 * i) + random.uniform(-0.2, 0.2)) + 100
    if i > 300 and i < 320:
        print(1.35 * k)
    else:
        print(k)