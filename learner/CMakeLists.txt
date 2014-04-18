PROJECT(BLEARNER)
CMAKE_MINIMUM_REQUIRED(VERSION 2.6)

SUBDIRS(src)

INSTALL (
  FILES bin/perl/joincliques.pl.in 
  DESTINATION bin/perl 
  RENAME joincliques.pl
)
INSTALL (
  FILES bin/perl/moralize.pl.in 
  DESTINATION bin/perl 
  RENAME moralize.pl
)
INSTALL (
  FILES bin/perl/quantify.pl.in 
  DESTINATION bin/perl 
  RENAME quantify.pl
)
INSTALL (
  FILES bin/perl/sst2dpa.pl.in 
  DESTINATION bin/perl 
  RENAME sst2dpa.pl
)
INSTALL (
  FILES bin/perl/sst2tht.pl.in 
  DESTINATION bin/perl 
  RENAME sst2tht.pl
)
INSTALL (
  FILES bin/perl/str2pla.pl.in 
  DESTINATION bin/perl 
  RENAME str2pla.pl
)
INSTALL (
  FILES bin/perl/str2ps.pl.in 
  DESTINATION bin/perl 
  RENAME str2ps.pl
)
INSTALL (
  FILES bin/perl/str_n_arcw2ps.pl.in 
  DESTINATION bin/perl 
  RENAME str_n_arcw2ps.pl
)
INSTALL (
  FILES bin/perl/triangulate.pl.in 
  DESTINATION bin/perl 
  RENAME triangulate.pl
)
INSTALL (
  FILES bin/perl/wlabel.pl.in
  DESTINATION bin/perl
  RENAME wlabel.pl
)

INSTALL (
  FILES bin/sh/prepobb.sh.in
  DESTINATION bin/sh
  RENAME prepobb.sh
)

INSTALL (
  FILES bin/python/hugo.py.in
  DESTINATION bin/python/
  RENAME hugo.py
)
