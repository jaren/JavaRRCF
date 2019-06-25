# JavaRRCF: Java implementation of a Robust Random Cut Forest
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

## Usage
### General package:
 * Includes generalized versions of Random Cut Trees and Random Cut Forests
 * Essentially the same as kLabUM/rrcf
 * Supports multidimensional data

### Memory package (deprecated):
 * Theoretically decreases minimum memory usage by about 40%, unclear how this actually affects the numbers in practice though
 * Attempted memory-optimized version of RRCF (storing only delta bounding boxes and using a shared buffer for shingled points)
 * Less time efficient
 * Intended for use with shingled single-dimensional data

## Sample run command (with Numenta anomaly benchmark taxi data):
```mvn package -DskipTests && curl https://raw.githubusercontent.com/numenta/NAB/master/data/realKnownCause/nyc_taxi.csv | tail -n +2 | awk -F',' '{print $2}' | time bash -c "java -cp target/rrcf-1.0.jar rrcf.ShingleCsv false false 48 200 1000 1234 > ~/Downloads/output.csv"```
