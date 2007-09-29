#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "err.h"
#include "format.h"
#include "data.h"
#include "bane.h"
#include "node.h"

typedef struct arcwstruct arcwstruct;

struct arcwstruct {
  int from;
  int to;
  double weight;
};

int arcsort(const void* a1, const void* a2){
  if(((arcwstruct*) a1)->weight > ((arcwstruct*) a2)->weight)
    return 1;
  else 
    return ((arcwstruct*) a1)->weight > ((arcwstruct*) a2)->weight;
}

int main(int argc, char* argv[]){

  format* fmt;
  data *dt;
  bane* bn;
  double ess;
  FILE* fp;
  double refscore;
  int arccount;
  arcwstruct* arws;
  arc* ar;
  double* scoreboard;
  int i, j;
  int numarcs;
  
  if(argc != 8){
    fprintf(stderr, 
	    "Usage: %s vdfile datafile datacount structfile ess numarcs outstructfile\n", 
	    argv[0]);
    exit(-1);
  }

  fmt = format_cread(argv[1]);
  dt = data_create(atoi(argv[3]), fmt->dim);
  data_read(argv[2], dt);
  bn = bane_create_from_format(fmt);
  OPENFILE_OR_DIE(fp,argv[4],"r"); 
  bane_read_structure(bn,fp);
  CLOSEFILE_OR_DIE(fp,argv[4]);
  ess = atof(argv[5]);
  numarcs = atoi(argv[6]);

  arccount = 0;
  for(i=0; i<bn->nodecount; ++i){
    int p;
    for(p=0; p<bn->nodecount; ++p){
      arccount += IS_PARENT(i,p,bn->pmx);
    }    
  }

  MEMALL(arws, arccount, arcwstruct);
  MECALL(scoreboard, bn->nodecount, double);
  MEMALL(ar, 1, arc);

  bane_gather_full_ss_in_order(bn, dt);
  refscore = bane_get_score(bn,ess,scoreboard);

  for (j = 0; j<numarcs; ++j) {
    arccount = 0;
    for(i=0; i<bn->nodecount; ++i){
      int p;
      node* nodi = bn->nodes + i;
      ar->to = i;
      for(p=0; p<bn->nodecount; ++p){
	if(IS_PARENT(i,p,bn->pmx)){
	  double arcscore;
	  ar->from = p;
	  bane_del_arc(bn, ar);
	  bane_gather_ss_for_i(bn, dt, i);
	  arws[arccount].from = p;
	  arws[arccount].to = i;
	  arws[arccount].weight = scoreboard[i] 
	    - bane_get_score_for_i(nodi,ess);
	  ++arccount;
	  bane_add_arc(bn, ar);
	}
      }
    }

    qsort(arws, arccount, sizeof(arcwstruct), arcsort);

    /*
      remove arc with lowest weight.
     */
    ar->from = arws[0].from;
    ar->to = arws[0].to;
    fprintf(stderr, "%d %d ", ar->from, ar->to);
    bane_del_arc(bn, ar);
    bane_del_arc_complete(bn, ar);
    bane_gather_ss_for_i(bn, dt, ar->to);
    scoreboard[ar->to] = bane_get_score_for_i(bn->nodes + ar->to, ess);
    refscore -= arws[0].weight;
    fprintf(stderr, "%g\n", refscore);
  }

  OPENFILE_OR_DIE(fp,argv[7],"w");
  bane_write_structure(bn,fp);
  CLOSEFILE_OR_DIE(fp,argv[7]);

  free(scoreboard);
  free(ar);

  free(arws);
  format_free(fmt);
  data_free(dt);
  bane_free(bn);

  return 0;
}
