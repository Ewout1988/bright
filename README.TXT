B-Right 0.3
-----------
B-Right is a software package for Bayesian network learning, and inference.

It was designed for structure learning on large data sets (up to 100
variables) and includes both a simulated annealing learner for searching
for the maximum a posteriori (MAP) structure, as well as an MCMC algorithm
for sampling from the posterior distribution.

The GUI has only integrated support for the simulated annealing algorithm,
since the MCMC sampler requires further improvements to make its setup easier.

I. Installation instructions
****************************
A. Source
---------
This section contains build instructions for all Unix flavors (Linux, MacOS X, 
...).

Requirements:
 - Java Runtime Environment 1.5 or higher
 - C compiler, CMake, make
 - GraphViz (for the 'dot' tool)
 - Java build environment: JDK 1.5, ant

Build instructions:
# unpack the source
tar -xzvf B-Right-0.3.tar.gz
cd B-Right-0.3

# build the GUI
cd client
ant
cd ..

# build the learner
cd learner
mkdir build
cd build
cmake -DCMAKE_INSTALL_PREFIX=`pwd`/../../client/dist/ ../
make
make install
cd ..

To run:
java -jar client/dist/bright-0.3.jar

You will need to edit the Settings to change the path to your dot binary.
If 'dot' is in your path, then you can simply put 'dot'.

B-Right uses remote web services for pre-processing that is needed for the
inference tool. In the GUI, you may want to edit the settings to configure
proxy settings, if necessary.

B. Binary release 
-----------------
Currently there is only a binary release available for Windows.

Requirements:
 - Java Runtime Environment 1.5 or higher

a) Unpack the zip file.
b) Double-click the bright-0.3.jar file.

B-Right uses remote web services for pre-processing that is needed for the
inference tool. In the GUI, you may want to edit the settings to configure
proxy settings, if necessary.

The GraphViz program is included in the binary distribution.

C. Local preprocessing
----------------------
By default B-Right uses remote web services for pre-processing that is needed 
for the inference tool. It is possible to run this pre-processing step on your
local machine, which is convenient when there is no connection to the internet
available. In order to run the preprocessing step locally, a Perl installation 
is required and the location of the perl binary should be configured in the 
"Local perl binary" B-Right setting.

II. Usage notes
***************
A. Inference
------------
The inference tool can be used with the B-Right GUI.

B. MCMC (preliminary)
---------------------
The MCMC sampler requires proper setup and diagnostics to verify that it is 
sampling ("mixing") properly. The following contains a step-wise cookbook to 
achieve a good sampler (which we are planning to implement in an automatic 
calibration utility):

a) Set H0 to be a bit lower than (-Score) of your best simulated annealing
   solution.

   If you choose this value too high, bnmcmc will quit when finding a
   "better" solution. If you set it too low, the sampler will work less
   efficiently.

b) Set TK so that the sampler has a metropolis hastings acceptance ratio
   over 0.75.

c) Set HK to be around -score of the 10% percentile lower limit of the scores
   at TK.

d) Set Tc so that you achieve a equi-energy jump acceptance
   ratio of around 0.4 in the highest rings

e) Compute K from TK and Tc.

f) Compute Hc from K, H0 and HK.
