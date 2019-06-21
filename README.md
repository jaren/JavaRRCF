# JavaRRCF: Memory-optimized Java implementation of a Robust Random Cut Forest
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## About
- Inspired by https://github.com/kLabUM/rrcf
- Original paper: http://proceedings.mlr.press/v48/guha16.pdf
- An unsupervised framework for detecting and classifying anomalies in streaming data
- Features:
  - Is well-suited to streaming data
  - Handles high-dimensional data
  - Avoids influence from irrelevant dimensions
  - Handles duplicates which would mask outliers
- Essentially the same core algorithm as kLabUM/rrcf, with memory optimizations
  - Leaves don't store points
  - Each branch only stores a bounding box delta for its children
  - Bounding boxes and leaf points can be calculated from the top-down with a root bounding box
  - Decreases memory usage by about 40%
- Slightly slower than a non-memory-optimized version (~15%)

## Sample run command (with Numenta anomaly benchmark taxi data):
```mvn package -DskipTests && curl https://raw.githubusercontent.com/numenta/NAB/master/data/realKnownCause/nyc_taxi.csv | tail -n +2 | awk -F',' '{print $2}' | time bash -c "java -cp target/rrcf-1.0.jar rrcf.ShingleCsv false 48 200 1000 1234 > ~/Downloads/output.csv"```
