#include <stdio.h>
#include <math.h>
#include <signal.h>
#include <errno.h>
#include <unistd.h>
#include "err.h"
#include "format.h"
#include "data.h"
#include "parent_matrix.h"
#include "bane.h"
#include "node.h"


int main(int argc, char* argv[]){
  int n;
  data* dt;
  format* fmt;
  bane* bn;
  FILE* fp;

  if(argc !=6){
    fprintf(stderr, 
	    "Usage: %s structfile vdfile datafile datacount outfile\n",
	    argv[0]);
    exit(-1);
  }


  fmt = format_cread(argv[2]);
  n = atoi(argv[4]);
  dt = data_create(n,fmt->dim);
  data_read(argv[3],dt);

  bn = bane_create_from_format(fmt);

  OPENFILE_OR_DIE(fp,argv[1],"r");
  bane_read_structure(bn,fp);
  CLOSEFILE_OR_DIE(fp,argv[1]);
  
  bane_gather_full_ss_in_order(bn,dt);

  /*
   * format:
   *  one line with pcc
   *  for every pcc:
   *    all SS for every value followed by new line
   */

  { /* Write the ss */
    int i;

    OPENFILE_OR_DIE(fp,argv[5],"w");
    
    for(i=0; i<bn->nodecount; ++i){
      node* nodi = bn->nodes + i;
      int pci;
      fprintf(fp,"%d\n",nodi->pcc);
      for(pci=0; pci<nodi->pcc; ++pci){
	int l;
	for(l=0; l<nodi->valcount; ++l){
	  fprintf(fp, "%d%c", 
		  SS(nodi, pci, l), (l<nodi->valcount-1) ? ' ' : '\n');
	}
      }
    }

    CLOSEFILE_OR_DIE(fp,argv[5]);

  }

  bane_free(bn);
  format_free(fmt);
  data_free(dt);

  return 0;
}










