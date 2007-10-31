B-Right 0.1
-----------

This is the README file for B-Right 0.1.

B-Right is a software package for Bayesian network learning, and inference.

It was designed for structure learning on large data sets (up to 100
variables) and includes both a simulated annealing learner for searching
for the maximum a posteriori (MAP) structure, as well as an MCMC algorithm
for sampling from the posterior distribution.

The GUI has integrated support only for the simulated annealing algorithm,
since the MCMC sampler requires further improvements to make its setup easier.

B-Right includes code borrowed from the following projects:
 - GraphViz
 - Apache batik

Installation instructions
-------------------------

The installation assumes that a Java 1.5 JVM is installed.

A. Windows (WIN32)
------------------

a) Unpack the zip file.
b) Double-click the bright-xx.jar file.

B-Right uses remote web services for pre-processing that is needed for the
inference tool. In the GUI, you may want to edit the settings to configure
proxy settings, if needed.

B. Unix (linux, MacOS X)
------------------------

Requirements:
 - GraphViz (for the 'dot' tool)
 - Java build environment: JDK, ant

Currently, only a source distribution is available.

Build instructions:

----
# unpacking the source
tar -xzvf B-Right-xx.tar.gz
cd B-Right-xx

# building the GUI
cd client
ant
cd ..

# building the learner
mkdir build
cd build
../learner/configure --prefix=$PWD/../client/dist/
make
make install
cd ..
----

To run:

java -jar client/dist/bright-xx.jar

You will need to edit the Settings to change the path to your dot binary.
If 'dot' is in your path, then you can simply put 'dot'.

B-Right uses remote web services for pre-processing that is needed for the
inference tool. In the GUI, you may want to edit the settings to configure
proxy settings, if needed.
