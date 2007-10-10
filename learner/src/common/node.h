#ifndef BAYWAY_NODE_H
#define BAYWAY_NODE_H

#include <stdio.h>
#include "typedef_node.h"
#include "bane.h"

struct node {
 
  int id;
  int valcount;
  
  int parentcount;
  int* parent;
  int first_parent;

  int childcount;
  int* child;
  int first_child;

  int pcc;
  int N;
  int* ss;
  int* ssp;

  int* path_to_me_count;
  int ancestorcount;
  int offspringcount;
};

extern int TRACK_OFFSPRING_COUNT;

#define SS(N,P,C) ((N)->ss[(P) * (N)->valcount + (C)])

/* C is supposed to be counter containing current child id */

#define FIRST_CHILD(BN,PND,C) \
  ((BN)->nodes + ((C) = (PND)->first_child))
                           
#define NEXT_CHILD(BN,PND,C) \
  ((BN)->nodes + ((C) = (PND)->child[C]))

#define ADD_CHILD(BN,PND,CH) \
{\
  (PND)->child[(CH)->id] = (PND)->first_child;\
  (PND)->first_child = (CH)->id;\
  ++ (PND)->childcount;\
}

#define DEL_CHILD(BN,PND,CH) \
{\
  int c;\
  node* dummy;\
  dummy = FIRST_CHILD((BN), (PND), c);\
  if(c == (CH)->id) {\
    (PND)->first_child = (PND)->child[(CH)->id];\
  } else { \
    while((PND)->child[c] != (CH)->id) {\
      dummy = NEXT_CHILD((BN), PND, c);\
    }\
    (PND)->child[c] = (PND)->child[(CH)->id];\
  }\
 (PND)->child[(CH)->id] = -1;\
  -- (PND)->childcount;\
  dummy = NULL;\
}

#define FIRST_PARENT(BN,CND,P) \
  ((BN)->nodes + ((P) = (CND)->first_parent))
                           
#define NEXT_PARENT(BN,CND,P) \
  ((BN)->nodes + ((P) = (CND)->parent[P]))

#define ADD_PARENT(BN,CND,PR) \
{\
  (CND)->parent[(PR)->id] = (CND)->first_parent;\
  (CND)->first_parent = (PR)->id;\
  ++ (CND)->parentcount;\
  (CND)->pcc *= (PR)->valcount;\
}
                           
#define DEL_PARENT(BN,CND,PR) \
{\
  int p;\
  node* dummy;\
  dummy = FIRST_PARENT((BN), (CND), p);\
  if(p == (PR)->id) {\
    (CND)->first_parent = (CND)->parent[(PR)->id];\
  } else { \
    while((CND)->parent[p] != (PR)->id) {\
      dummy = NEXT_PARENT((BN), CND, p);\
    }\
    (CND)->parent[p] = (CND)->parent[(PR)->id];\
  }\
 (CND)->parent[(PR)->id] = -1;\
  -- (CND)->parentcount;\
  (CND)->pcc /= (PR)->valcount;\
  dummy = NULL;\
}


#define IS_ANCESTOR_OF(NODE,AID) ((NODE)->path_to_me_count[AID]>0)

extern void 
node_assign(node* dst, node* src, int nodecount);

extern void 
node_init(node* nodi, int i, int nodecount);

extern void 
node_write(node* nd, int nodecount, FILE* fp);

extern void 
node_write_prob(node* nd, FILE* fp);

extern int
node_param_count(node *nd);

#endif

