SET(COMMON
  bane.c
  bane_nb.c
  banesearch.c
  data.c
  fdata.c
  forest.c
  format.c
  nbmodel.c
  nb_selector.c
  node.c
  parent_matrix.c
  score_hashtable.c 
)

INCLUDE_DIRECTORIES(
 common
)

ADD_LIBRARY(bayway ${COMMON})

INSTALL_FILES(/include/common .*\\.h)
INSTALL (TARGETS bayway DESTINATION lib)
