SUBDIRS = common
bin_PROGRAMS = \
arcweights \
bflearner \
bn_contd_learner \
bnlearner \
bnmcmc \
bnpredict \
modeleval \
str_n_dat2sst \
trim \
remove_edge

srcdir = @srcdir@
VPATH = @srcdir@

AM_CPPFLAGS = -I@srcdir@/common

arcweights_LDADD = ./common/libbaywaycommon.la
bflearner_LDADD = ./common/libbaywaycommon.la
bn_contd_learner_LDADD = ./common/libbaywaycommon.la
bnlearner_LDADD = ./common/libbaywaycommon.la
bnmcmc_LDADD = ./common/libbaywaycommon.la
bnpredict_LDADD = ./common/libbaywaycommon.la
modeleval_LDADD = ./common/libbaywaycommon.la
str_n_dat2sst_LDADD = ./common/libbaywaycommon.la
trim_LDADD = ./common/libbaywaycommon.la
remove_edge_LDADD = ./common/libbaywaycommon.la
