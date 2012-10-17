PROJECT(bright)
cmake_minimum_required(VERSION 2.8)

SET(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")

SET(COMMON
  src/common/bane.c
  src/common/bane_nb.c
  src/common/banesearch.c
  src/common/data.c
  src/common/fdata.c
  src/common/forest.c
  src/common/format.c
  src/common/nbmodel.c
  src/common/nb_selector.c
  src/common/node.c
  src/common/parent_matrix.c
  src/common/score_hashtable.c 
)

SET(BINARIES
  arcweights
  bflearner
  bnlearner
  bn_contd_learner
  bnmcmc
  bnpredict
  modeleval
  remove_edge
  str_n_dat2sst
  trim
)

INCLUDE_DIRECTORIES(
 src/common
)

ADD_LIBRARY(b-learner SHARED ${COMMON})

FOREACH(BIN ${BINARIES})
 ADD_EXECUTABLE(${BIN} src/${BIN}.c)
 TARGET_LINK_LIBRARIES(${BIN} b-learner m)
ENDFOREACH(BIN)

FUNCTION(copy_script dir source-file-in source-file)
  FILE(COPY
    ${CMAKE_CURRENT_SOURCE_DIR}/bin/${dir}/${source-file-in}
    DESTINATION ${CMAKE_RUNTIME_OUTPUT_DIRECTORY}/${dir})
  FILE(RENAME 
    ${CMAKE_RUNTIME_OUTPUT_DIRECTORY}/${dir}/${source-file-in}
    ${CMAKE_RUNTIME_OUTPUT_DIRECTORY}/${dir}/${source-file})
ENDFUNCTION(copy_script)

FILE(MAKE_DIRECTORY  
  ${CMAKE_RUNTIME_OUTPUT_DIRECTORY}/perl)
copy_script(perl joincliques.pl.in joincliques.pl)
copy_script(perl moralize.pl.in moralize.pl)
copy_script(perl quantify.pl.in quantify.pl)
copy_script(perl sst2dpa.pl.in sst2dpa.pl)
copy_script(perl sst2tht.pl.in sst2tht.pl)
copy_script(perl str2pla.pl.in str2pla.pl)
copy_script(perl str2ps.pl.in str2ps.pl)
copy_script(perl str_n_arcw2ps.pl.in str_n_arcw2ps.pl)
copy_script(perl triangulate.pl.in triangulate.pl)

FILE(MAKE_DIRECTORY  
  ${CMAKE_RUNTIME_OUTPUT_DIRECTORY}/python)
copy_script(python hugo.py.in hugo.py)

FILE(MAKE_DIRECTORY  
  ${CMAKE_RUNTIME_OUTPUT_DIRECTORY}/sh)
copy_script(sh prepobb.sh.in prepobb.sh)