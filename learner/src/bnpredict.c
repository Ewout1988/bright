#include <stdio.h>
#include <math.h>
#include "err.h"
#include "format.h"
#include "data.h"
#include "bane.h"

int main(int argc, char* argv[]){

  format* fmt;
  data* trndt;
  data* tstdt;
  bane* bn;
  double ess;
  
  FILE* fp;
  int j;
  double log_score;

  if(argc < 8){
    fprintf(stderr, 
	    "Usage: %s formatfile strfile trnidt trndc ess tstidt tstdc\n", 
	    argv[0]);
    exit(-1);
  }

  fmt = format_cread(argv[1]);
  trndt = data_create(atoi(argv[4]), fmt->dim);
  data_read(argv[3], trndt);

  tstdt = data_create(atoi(argv[7]), fmt->dim);
  data_read(argv[6], tstdt);

  bn = bane_create_from_format(fmt);
  OPENFILE_OR_DIE(fp,argv[2],"r");
  bane_read_structure(bn,fp);
  CLOSEFILE_OR_DIE(fp,argv[4]);                                                 
  ess = atof(argv[5]);

  log_score = 0;
  bane_gather_full_ss(bn,trndt);

  for(j=0; j<tstdt->N;++j){
    double lp = bane_logprob(bn,tstdt,j,ess);
    log_score += lp;
    fprintf(stdout,"%g\n",lp);
  }

  fprintf(stdout, "%f\n", log_score/tstdt->N);

  data_free(trndt);
  data_free(tstdt);
  bane_free(bn);
  format_free(fmt);

  return 0;
}
