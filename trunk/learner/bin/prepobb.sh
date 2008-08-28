#!/bin/sh

bin=$1
exebin=$2

vd_file=$3
idt_file=$4
str_file=$5

ess=$6
dcount=`cat $idt_file | wc -l`

pla_file=$7
qjt_file=$8
dpa_file=$9

tmpdir=$10
rm -rf $tmpdir
mkdir $tmpdir

sst_file="$tmpdir/data.sst"
tht_file="$tmpdir/data.tht"
moral_file="$tmpdir/data.moral"
trg_file="$tmpdir/data.trg"
clq_file="$tmpdir/data.clq"
jtree_file="$tmpdir/data.jtree"

logfile=$tmpdir/logfile

$bin/str2pla.pl $vd_file $str_file $pla_file 2>> $logfile
$exebin/str_n_dat2sst $str_file $vd_file $idt_file $dcount $sst_file 2>> $logfile
$bin/sst2tht.pl $sst_file $ess $tht_file 2>> $logfile
$bin/moralize.pl $str_file $moral_file 2>> $logfile
$bin/triangulate.pl $vd_file $moral_file $trg_file $clq_file 2>> $logfile
$bin/joincliques.pl $vd_file $clq_file $jtree_file 2>> $logfile
$bin/quantify.pl $str_file $tht_file $jtree_file $clq_file $qjt_file 2>> $logfile
$bin/sst2dpa.pl $sst_file $ess $dpa_file 2>> $logfile
