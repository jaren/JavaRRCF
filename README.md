# JavaRRCF
### Java implementation of Robust Random Cut Forest
### Inspired by https://github.com/kLabUM/rrcf
### Original paper: http://proceedings.mlr.press/v48/guha16.pdf

#### General package:
* Includes generalized versions of Random Cut Trees and Random Cut Forests
* Supports multidimensional data

#### Optimized package:
* Potentially decreases memory usage by orders of magnitude
* Intended for use with single-dimensional data
* Automatically handles data shingling in a space-efficient manner