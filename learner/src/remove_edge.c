#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "err.h"
#include "format.h"
#include "data.h"
#include "bane.h"
#include "node.h"

int main(int argc, char* argv[]){

  format* fmt;
  bane* bn;
  double ess;
  FILE* fp;
  arc ar;
  int i;
  
  if(argc != 4){
    fprintf(stderr, 
	    "Usage: %s vdfile structfile outstructfile\n", 
	    argv[0]);
    exit(-1);
  }

  fmt = format_cread(argv[1]);
  bn = bane_create_from_format(fmt);
  OPENFILE_OR_DIE(fp,argv[2],"r"); 
  bane_read_structure(bn,fp);
  CLOSEFILE_OR_DIE(fp,argv[2]);

  for(i=0; i<bn->nodecount; ++i){
    int p;
    node* nodi = bn->nodes + i;
    ar.to = i;
    for(p=0; p<bn->nodecount; ++p){
      if(IS_PARENT(i,p,bn->pmx)){
	double arcscore;
	ar.from = p;
	bane_del_arc(bn, &ar);

	char buf[256];
	sprintf(buf, "%s.%d-%d.str", argv[3], ar.from, ar.to);

	OPENFILE_OR_DIE(fp,buf,"w");
	bane_write_structure(bn,fp);
	CLOSEFILE_OR_DIE(fp,buf);

	bane_add_arc(bn, &ar);
      }
    }
  }

  format_free(fmt);
  bane_free(bn);

  return 0;
}
