#include <math.h>
#include "banesearch.h"
#include "err.h"
#include "forest.h"
#include "parent_matrix.h"
#include "node.h"
#include "bane.h"

int bane_suggest_ranadd_from(bane* bn, node* from, arc* ar, int maxtblsize) {
  int to_id = -1;
  node* to = NULL;
  int t,i;

  int target_count = /* number of possible targets */
    bn->nodecount - from->ancestorcount - from->childcount - 1;
  if(target_count == 0) return 0;

  t = rand() % target_count;    
  i = 0; /* count upto t */
  for(to_id=0; ; ++to_id){
    if((to_id == from->id)                     /* self loop */
       || (IS_CHILD(from->id, to_id, bn->pmx)) /* arc to child */ 
       || (IS_ANCESTOR_OF(from, to_id)))       /* arc to ancestor */
      continue;
    if(i == t) break;
    ++i;
  }

  to = bn->nodes + to_id;

  if(to->pcc * from->valcount * to->valcount <= maxtblsize){  
    /* Complexity constraint satisfied */
    ar->to = to_id;
    ar->from = from->id;
    return 1;
  }

  return 0;
}

int bane_add_random_arc_from(bane* bn, node* from, arc* ar, int maxtblsize) {
  if (bane_suggest_ranadd_from(bn,from,ar,maxtblsize)) {
    bane_add_arc(bn, ar);
    return 1;
  }
  return 0;
}

int bane_suggest_ranadd_to(bane *bn, node *to, arc *ar, int maxtblsize) {
  int from_id = -1;
  node *from = NULL;
  int t, i;

  /*
    fixme: s/childcount/offspringcount/
    this overestimates sourcecount, this is caught in the next for loop
    causing to loop twice.
  */
  int source_count = bn->nodecount - to->parentcount - to->childcount - 1;

  for (;;) {
    if (source_count == 0) return 0;

    t = rand() % source_count;
    i = 0; /* count upto t */

    for (from_id = 0; from_id < bn->nodecount; ++from_id) {
      if ((from_id == to->id)			  /* self loop */
	  || (IS_CHILD(from_id, to->id, bn->pmx)) /* arc from parent */
	  || (IS_ANCESTOR_OF(bn->nodes + from_id, to->id)))
						  /* arc from descendent */
	continue;

      if (i == t)
	break;

      ++i;
    }

    if (from_id == bn->nodecount) /* reached end of loop before hitting t */
      source_count = i;
    else
      break;
  }

  from = bn->nodes + from_id;

  if (to->pcc * from->valcount * to->valcount <= maxtblsize) {
    /* Complexity constraint satisfied */
    ar->to = to->id;
    ar->from = from_id;
    return 1;
  }

  return 0;
}

int bane_add_random_arc_to(bane* bn, node* to, arc* ar, int maxtblsize) {
  if (bane_suggest_ranadd_to(bn,to,ar,maxtblsize)) {
    bane_add_arc(bn, ar);
    return 1;
  }
  return 0;
}

int bane_suggest_ranadd(bane* bn, arc* ar, int maxtblsize) {
  int r;
  for(r=0; r<10; ++r) { /* Try at most 10 times to find candidate arc */
    int from_id = rand()%bn->nodecount;
    node* from = bn->nodes + from_id; /* Random pick from */
    if(bane_suggest_ranadd_from(bn, from, ar, maxtblsize)) return 1;
  }
  return 0;
}

int bane_add_random_arc(bane* bn, arc* ar, int maxtblsize){
  if(bane_suggest_ranadd(bn,ar,maxtblsize)){
    bane_add_arc(bn, ar);
    return 1;
  }
  return 0;
}

int bane_suggest_randel_from(bane* bn, node* from, arc* ar){
  int t, c, i;
  
  node* to;

  if (from->childcount == 0) return 0;

  /* get to_id */
  t = rand() % (from->childcount);
  
  to = FIRST_CHILD(bn,from,c);
  for(i=0; i<t; ++i){
    to = NEXT_CHILD(bn,from,c);
  }
  
  ar->to = to->id;
  ar->from = from->id;

  return 1;
}

int bane_del_random_arc_from(bane* bn, node* from, arc* ar){
  if(bane_suggest_randel_from(bn,from,ar)){
    bane_del_arc(bn, ar);
    return 1;
  }
  return 0;
}

int bane_suggest_randel(bane* bn, arc* ar){
  node* from = NULL;
  int r;

  /* get from_id */
  for(r=0; r<10; ++r){ /* Try at most ten times to find a node with child */
    from = bn->nodes + rand()%bn->nodecount;
    if(from->childcount > 0) break;
  }

  if(r == 10) return 0; /* Not found */

  return bane_suggest_randel_from(bn, from, ar);

}

int bane_del_random_arc(bane* bn, arc* ar, int maxtblsize){
  maxtblsize=maxtblsize; /* to suppress warnings */
  if(bane_suggest_randel(bn,ar)){
    bane_del_arc(bn, ar);
    return 1;
  }
  return 0;
}


int bane_suggest_ranrev(bane* bn, arc* ar, int maxtblsize){
  int r;

  for(r=0; r<10; ++r){
    node* from;
    node* to;

    int all_from_parents_common = 1;
    int all_to_parents_common = 1;

    int p = -1;
    node* pn;

    if(!bane_suggest_randel(bn,ar)) continue;

    from = bn->nodes + ar->from;
    to = bn->nodes + ar->to;

    /* NB! Following logic checks only the guards that do not lead
       to the cycles if arc is reversed */

    /* Check if all of the from's parents are also to's parents */
    pn = FIRST_PARENT(bn,from,p);
    while((p!=-1) && all_from_parents_common){
      all_from_parents_common &= IS_PARENT(to->id, p, bn->pmx); 
      pn = NEXT_PARENT(bn,from,p);
    }

    /* Check if all of the to's parents are also from's parents */
    pn = FIRST_PARENT(bn,to,p);
    while((p!=-1) && all_to_parents_common ){
      if(p != from->id) { 
	all_to_parents_common &= IS_PARENT(from->id, p, bn->pmx); 
      }
      pn = NEXT_PARENT(bn,to,p);
    }
    
    /* retry if the change would not change V-structures */
    if(all_from_parents_common && all_to_parents_common) continue;

    /* retry if the node would have too many parent configurations */
    if(from->pcc * to->valcount * from->valcount > maxtblsize) continue;

    /* retry if the rev would introduce cycle */
    if(to->path_to_me_count[from->id] > 1) continue;
    
    ar->to = to->id;
    ar->from = from->id;
    return 1;
  }

  return 0;
}

int bane_rev_random_arc(bane* bn, arc* ar, int maxtblsize){
  if(bane_suggest_ranrev(bn,ar, maxtblsize)){
    bane_rev_arc(bn, ar);  /* NB. reverses ar */
    return 1;
  }
  return 0;
}

int bane_repl_random_to_arc(bane* bn, arc* del_ar, arc* add_ar,
			    int maxtblsize) {
  int i;

  for (i = 0; i < 10; ++i) {
    if (bane_del_random_arc(bn, del_ar, maxtblsize))
      if (bane_suggest_ranadd_from(bn, bn->nodes + del_ar->from,
				   add_ar, maxtblsize)) {
	bane_add_arc(bn, add_ar);
	return 1;
      } else
	bane_add_arc(bn, del_ar);
    else
      return 0;
  }

  return 0;
}

int bane_repl_random_from_arc(bane* bn, arc* del_ar, arc* add_ar,
			      int maxtblsize) {
  int i;

  for (i = 0; i < 10; ++i) {
    if (bane_del_random_arc(bn, del_ar, maxtblsize))
      if (bane_suggest_ranadd_to(bn, bn->nodes + del_ar->to,
				 add_ar, maxtblsize)) {
	bane_add_arc(bn, add_ar);
	return 1;
      } else
	bane_add_arc(bn, del_ar);
    else
      return 0;
  }

  return 0;
}

void bane_repl_arc(bane *bn, arc *del_ar, arc *add_ar) {
  bane_del_arc(bn, del_ar);
  bane_add_arc(bn, add_ar);
}

double update_score(bane *bn, enum Operation ch, arc *ar, arc *del_ar,
		    arc *add_ar, data *dt, double *scoreboard,
		    score_hashtable *sht, double *from_score,
		    double *to_score, double ess,
		    double param_cost,
		    double current_best_score) {
  double new_score = current_best_score;

  if (ch < 3) {
    bane_gather_ss_for_i(bn, dt, ar->to); /* to needs always work */
    *to_score = bane_get_score_for_i(bn->nodes + ar->to, ess);	
    *from_score = scoreboard[ar->from]; /* cheap default for from */
    if (ch == 2) { /* rev - from needs to be calculated */
      bane_gather_ss_for_i(bn, dt, ar->from);
      *from_score = bane_get_score_for_i(bn->nodes + ar->from, ess);
    }

    if (sht) {
      new_score = current_best_score +  
	*from_score + *to_score 
	- scoreboard[ar->from] - scoreboard[ar->to];


      /*
       * param penalty
       */
      int fromvalcount = (bn->nodes + ar->from)->valcount;
      int tovalcount = (bn->nodes + ar->to)->valcount;

      if (ch < 3) {
	int curto = node_param_count(bn->nodes + ar->to);
	int prevto = 0;

	int curfrom = 0;
	int prevfrom = 0;

	switch (ch) {
	case 0:
	  prevto = curto / fromvalcount;
	  break;
	case 1:
	  prevto = curto * fromvalcount;
	  break;
	case 2:
	  prevto = curto / fromvalcount;

	  curfrom = node_param_count(bn->nodes + ar->from);
	  prevfrom = curfrom * tovalcount;
	default:
	  ;/* unreachable */	  
	}

	new_score -= (curto - prevto + curfrom - prevfrom) * param_cost;
      }

      score_hashtable_put(sht, bn->pmx->mx, new_score);
    }
  } else {
    double del_to_score, add_to_score;

    /*
     * a del arc followed by an add arc, we must undo the add_ar to see
     * what happened.
     */
    bane_del_arc(bn, add_ar);

    bane_gather_ss_for_i(bn, dt, del_ar->to);
    del_to_score = bane_get_score_for_i(bn->nodes + del_ar->to, ess);

    if (sht) {
      int fromvalcount = (bn->nodes + del_ar->from)->valcount;      
      int curto = node_param_count(bn->nodes + del_ar->to);
      int prevto = curto * fromvalcount;

      new_score = current_best_score +  
	del_to_score - scoreboard[del_ar->to]
	- (curto - prevto) * param_cost;
      score_hashtable_put(sht, bn->pmx->mx, new_score);
    }

    bane_add_arc(bn, add_ar);

    bane_gather_ss_for_i(bn, dt, add_ar->to);
    add_to_score = bane_get_score_for_i(bn->nodes + add_ar->to, ess);

    if (sht) {
      int fromvalcount = (bn->nodes + add_ar->from)->valcount;      
      int curto = node_param_count(bn->nodes + add_ar->to);
      int prevto = curto / fromvalcount;

      new_score = new_score
	+ add_to_score
	- (add_ar->to == del_ar->to ?
	   del_to_score : scoreboard[add_ar->to])
	- (curto - prevto) * param_cost;
      score_hashtable_put(sht, bn->pmx->mx, new_score);
    }

    ar->from = del_ar->to;
    *from_score = del_to_score;
    ar->to = add_ar->to;
    *to_score = add_to_score;
  }

  return new_score;
}

void bane_calc_neighbourhood_sizes(bane *bn, unsigned *nbv)
{
  int i;

  for (i = 0; i < 5; ++i)
    nbv[i] = 0;

  for (i = 0; i < bn->nodecount; ++i) {
    node* nodi = bn->nodes + i;

    /* add_arc */
    nbv[Add] += bn->nodecount - nodi->ancestorcount - nodi->childcount - 1;
    /* del_arc */
    nbv[Del] += nodi->childcount;

    /* change_to */
    nbv[ChangeTo] += nodi->childcount *
      (bn->nodecount - nodi->ancestorcount - nodi->childcount - 1);
    /* change_from */
    nbv[ChangeFrom] += nodi->parentcount *
      (bn->nodecount - nodi->offspringcount - nodi->parentcount - 1);
  }
}

int bane_add_arc_i_from(bane *bn, node *from, arc *ar, int t, int maxtblsize)
{
  int to_id = -1;
  node* to = NULL;
  int i;

  i = 0; /* count upto t */
  for (to_id = 0; ; ++to_id) {
    if ((to_id == from->id)                    /* self loop */
       || (IS_CHILD(from->id, to_id, bn->pmx)) /* arc to child */ 
       || (IS_ANCESTOR_OF(from, to_id)))      /* arc to ancestor */
      continue;
    if (i == t) break;
    ++i;
  }

  to = bn->nodes + to_id;

  if (to->pcc * from->valcount * to->valcount <= maxtblsize) {
    /* Complexity constraint satisfied */
    ar->to = to_id;
    ar->from = from->id;

    bane_add_arc(bn, ar);
    bane_add_arc_complete(bn, ar);

    return 1;
  } else
    return 0;
}

int bane_add_arc_i_to(bane *bn, node *to, arc *ar, int t, int maxtblsize)
{
  int from_id = -1;
  node* from = NULL;
  int i;

  i = 0; /* count upto t */
  for (from_id = 0; ; ++from_id) {
    if ((from_id == to->id)                    /* self loop */
       || (IS_CHILD(from_id, to->id, bn->pmx)) /* arc from parent */ 
       || (IS_ANCESTOR_OF(bn->nodes + from_id, to->id))) /* arc from ancestor */
      continue;
    if (i == t) break;
    ++i;
  }

  from = bn->nodes + from_id;

  if (to->pcc * from->valcount * to->valcount <= maxtblsize) {
    /* Complexity constraint satisfied */
    ar->to = to->id;
    ar->from = from->id;

    bane_add_arc(bn, ar);
    bane_add_arc_complete(bn, ar);

    return 1;
  } else
    return 0;
}

int bane_del_arc_i_from(bane *bn, node *from, arc *ar, int t)
{
  int c, i;  
  node* to;

  to = FIRST_CHILD(bn,from,c);
  for (i = 0; i < t; ++i){
    to = NEXT_CHILD(bn, from, c);
  }

  ar->to = to->id;
  ar->from = from->id;

  bane_del_arc(bn, ar);
  bane_del_arc_complete(bn, ar);

  return 1;  
}

int bane_del_arc_i_to(bane *bn, node *to, arc *ar, int t)
{
  int c, i;
  node* from;

  from = FIRST_PARENT(bn, to, c);
  for (i = 0; i < t; ++i){
    from = NEXT_PARENT(bn, to, c);
  }

  ar->to = to->id;
  ar->from = from->id;

  bane_del_arc(bn, ar);
  bane_del_arc_complete(bn, ar);

  return 1;  
}

int bane_change_arc_dst(bane *bn, node *from, arc *del_ar, arc *add_ar,
			int arc_i, int dst_i, int maxtblsize)
{
  bane_del_arc_i_from(bn, from, del_ar, arc_i);

  return bane_add_arc_i_from(bn, from, add_ar, dst_i, maxtblsize);
}

int bane_change_arc_src(bane *bn, node *to, arc *del_ar, arc *add_ar,
			int arc_i, int src_i, int maxtblsize)
{
  bane_del_arc_i_to(bn, to, del_ar, arc_i);

  return bane_add_arc_i_to(bn, to, add_ar, src_i, maxtblsize);
}
