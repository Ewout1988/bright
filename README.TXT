===== Release notes for B-Right 0.1 =====

0. Contents
-----------

  I.   Introduction

  II.  Build and installation instructions

      A) Windows (win32)
      B) Unix (linux, MacOS X)

  III. Usage notes

      A) Inference
      B) MCMC

I. Introduction
---------------

This file contains installation instructions, and usage notes for
B-Right 0.1.

B-Right is a software package for Bayesian network learning and
Bayesian network inference.

It was designed for structure learning on large data sets (up to 100
variables) and includes both a simulated annealing learner for
searching for the maximum a posteriori (MAP) structure, as well as an
equi-energy Metropolis Hastings MCMC sampler to sample from the
posterior distribution.

Currently, it is limited to nominal data, and multinomial CPDs.

The GUI has integrated support only for the simulated annealing algorithm,
since the MCMC sampler requires further improvements to make its setup easier.

B-Right includes code borrowed from the following projects:
 - GraphViz
 - Apache batik
 - Wts

II. Build and installation instructions
---------------------------------------

B-Right requires that a Java 1.5 JVM is installed.

A) Windows (win32)

 a) Unpack the zip file. 
 b) Double-click the bright-xx.jar file.

B) Unix (linux, MacOS X)

Requirements:
 - GraphViz (for the 'dot' tool)
 - Java build environment: JDK, ant
 - GNU C build environment: autotools, gcc, make

Currently, only a source distribution is available.

The instructions to build

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

java -Xmx128M -jar client/dist/bright-xx.jar

You will need to edit the Settings to change the path to your dot binary.
If 'dot' is in your path, then you can simply put 'dot'.

III. Usage notes
---------------

A) Inference

By default, B-Right uses remote web services for pre-processing that
is needed for the inference tool. If this is an acceptible solution
for you, but it doesn't seem to work altough you are connected to the
Internet, then perhaps you should configure proxy settings. Note that
B-Right will prompt you to confirm this transmission over the internet
each time.

On unix(-alikes), this pre-processing may be configured to run
locally, and only requires perl to be installed. In that case, simply
configure the path to your perl binary, and B-Right will no longer use
the remote services.

Another alternative is to install a wts server locally on a linux
server on your intranet, and deploy the wts service (which you may
find in SVN), and configure B-Right to use that one instead of the one
on the Internet.

B) MCMC (preliminary)

The MCMC sampler requires proper setup and diagnostics to verify that
it is sampling ("mixing") properly. The following contains a step-wise
cookbook to achieve a good sampler (which we are planning to implement
in an automatic calibration utility):

1) Set H0 to be a bit lower than (-Score) of your best simulated annealing
   solution.

   If you choose this value too high, bnmcmc will quit when finding a
   "better" solution. If you set it too low, the sampler will work less
   efficiently.

2) Set TK so that the sampler has a metropolis hastings acceptance ratio
   over 0.75.

3) Set HK to be around -score of the 10% percentile lower limit of the scores
   at TK.

4) Set Tc so that you achieve a equi-energy jump acceptance
   ratio of around 0.4 in the highest rings

5) Compute K from TK and Tc.

6) Compute Hc from K, H0 and HK.