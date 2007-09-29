#!/bin/bash

# usage: str2net vdfile datafile datacount strfile ess netfile
vdfile=$1
datafile=$2
datacount=$3
strfile=$4
plafile=$strfile.pla
sstfile=$strfile.sst
thtfile=$strfile.tht
ess=$5
netfile=$6

# str2pla computes placement of the network (using dotty?)
str2pla.pl $vdfile $strfile $plafile
str_n_dat2sst $strfile $vdfile $datafile $datacount $sstfile
sst2tht.pl $sstfile $ess $thtfile
hugo.py $vdfile $plafile $thtfile $strfile $netfile
