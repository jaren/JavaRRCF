# JavaRRCF
### Java implementation of a Robust Random Cut Forest:
### Inspired by https://github.com/kLabUM/rrcf
### Original paper: http://proceedings.mlr.press/v48/guha16.pdf
* An unsupervised framework for detecting and classifying anomalies in streaming data
* Features:
 * Is well-suited to streaming data
 * Handles high-dimensional data
 * Avoids influence from irrelevant dimensions
 * Handles duplicates which would mask outliers

#### General package:
* Includes generalized versions of Random Cut Trees and Random Cut Forests
* Supports multidimensional data

#### Memory package:
* Less time efficient
* Potentially decreases memory usage by orders of magnitude
* Intended for use with single-dimensional data
* Automatically handles data shingling

### Sample usage:
`mvn package -DskipTests && curl https://raw.githubusercontent.com/numenta/NAB/master/data/realKnownCause/nyc_taxi.csv | tail -n +2 | awk -F',' '{print $2}' | time bash -c "java -cp target/rrcf-1.0.jar rrcf.ShingleCsv false true 48 200 1000 1234 > ~/Downloads/output.csv"`