#include <stdio.h>
#include <string.h>
#include "err.h"
#include "node.h"

void 
node_assign(node* dst, node* src, int nodecount){
  dst->id = src->id;
  dst->valcount = src->valcount;
  dst->parentcount = src->parentcount;
  memcpy(dst->parent, src->parent, nodecount*sizeof(int));
  dst->first_parent = src->first_parent;
  dst->childcount = src->childcount;
  memcpy(dst->child, src->child, nodecount*sizeof(int));
  dst->first_child = src->first_child;
  dst->pcc = src->pcc;
  dst->N = src->N;
  dst->ancestorcount = src->ancestorcount;
  memcpy(dst->path_to_me_count, src->path_to_me_count, nodecount*sizeof(int));
}

void 
node_init(node* nodi, int i, int nodecount){
  int j;
  nodi->id = i;
  nodi->valcount = 1;
  nodi->parentcount = 0;
  for(j=0; j<nodecount; ++j) nodi->parent[j] = -1;
  nodi->first_parent = -1;
  nodi->childcount = 0;
  for(j=0; j<nodecount; ++j) nodi->child[j] = -1;
  nodi->first_child = -1;
  nodi->pcc = 1;
  nodi->N = 0;
  nodi->ss = NULL;
  nodi->ssp = NULL;
  nodi->ancestorcount = 0;
  for(j=0; j<nodecount; ++j) nodi->path_to_me_count[j] = 0;
}


void node_write(node* nd, int nodecount, FILE* fp){
  int i;
  fprintf(fp, "id=%d vals=%d parents=%d children=%d ancestors=%d\n",
	  nd->id, nd->valcount, nd->parentcount, nd->childcount, 
	  nd->ancestorcount);
  
  fprintf(fp, "parents: %d ",nd->first_parent);
  for(i=0; i<nodecount;++i){
    fprintf(fp, " %d",nd->parent[i]);
  } 
  fprintf(fp, "\n");

  fprintf(fp, "children: %d ",nd->first_child);
  for(i=0; i<nodecount;++i){
    fprintf(fp, " %d",nd->child[i]);
  } 
  fprintf(fp, "\n");

  fprintf(fp, "paths from ancestors: ");
  for(i=0; i<nodecount;++i){
    fprintf(fp, " %d",nd->path_to_me_count[i]);
  } 
  fprintf(fp, "\n");
  fprintf(fp, "pcc=%d\n",nd->pcc);
  
}

int node_param_count(node *n) {
  return n->pcc * n->valcount;
}


