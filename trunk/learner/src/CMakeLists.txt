SET(COMMON
  common/bane.c
  common/bane_nb.c
  common/banesearch.c
  common/data.c
  common/fdata.c
  common/forest.c
  common/format.c
  common/nbmodel.c
  common/nb_selector.c
  common/node.c
  common/parent_matrix.c
  common/score_hashtable.c 
)

INCLUDE_DIRECTORIES(
 common
)

ADD_LIBRARY(b-learner SHARED ${COMMON})

ADD_EXECUTABLE(arcweights arcweights.c)
ADD_EXECUTABLE(bflearner bflearner.c)
ADD_EXECUTABLE(bnlearner bnlearner.c)
ADD_EXECUTABLE(bn_contd_learner bn_contd_learner.c)
ADD_EXECUTABLE(bnmcmc bnmcmc.c)
ADD_EXECUTABLE(bnpredict bnpredict.c)
ADD_EXECUTABLE(modeleval modeleval.c)
ADD_EXECUTABLE(remove_edge remove_edge.c)
ADD_EXECUTABLE(str_n_dat2sst str_n_dat2sst.c)
ADD_EXECUTABLE(trim trim.c)

TARGET_LINK_LIBRARIES(arcweights b-learner m)
TARGET_LINK_LIBRARIES(bflearner b-learner m)
TARGET_LINK_LIBRARIES(bnlearner b-learner m)
TARGET_LINK_LIBRARIES(bn_contd_learner b-learner m)
TARGET_LINK_LIBRARIES(bnmcmc b-learner m)
TARGET_LINK_LIBRARIES(bnpredict b-learner m)
TARGET_LINK_LIBRARIES(modeleval b-learner m)
TARGET_LINK_LIBRARIES(remove_edge b-learner m)
TARGET_LINK_LIBRARIES(str_n_dat2sst b-learner m)
TARGET_LINK_LIBRARIES(trim b-learner m)

INSTALL (TARGETS arcweights bflearner bnlearner bn_contd_learner bnmcmc bnpredict modeleval remove_edge str_n_dat2sst trim DESTINATION bin)