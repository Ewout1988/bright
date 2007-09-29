#include <stdio.h>
#include <math.h>
#include <signal.h>
#include <errno.h>
#include <unistd.h>
#include "err.h"
#include "format.h"
#include "data.h"
#include "parent_matrix.h"
#include "score_hashtable.h"
#include "banesearch.h"
#include "bane.h"
#include "node.h"

int usr1_set;
int usr2_set;
int alrm_set;

double param_cost = 0;

void usr2_handler(int signum) {
  signal(12,usr2_handler);
  signum = 0;
  usr2_set = 1;
}

void usr1_handler(int signum) {
  signal(10,usr1_handler);
  signum = 0;
  usr1_set = 1;
}

void alrm_handler(int signum) {
  signal(14,alrm_handler);
  signum = 0;
  alrm_set = 1;
}

void set_signal_handlers(){
  usr1_set = 0;
  usr2_set = 0;
  alrm_set = 0;
  signal(10,usr1_handler);
  signal(12,usr2_handler);
  signal(14,alrm_handler);
}

double bane_get_score_param_costs(bane* bn, double ess, double* scoreboard)
{
  double score = bane_get_score(bn, ess, scoreboard);

  score -= bane_param_count(bn) * param_cost;

  return score;
}

typedef struct search_stats search_stats;

struct search_stats {
  unsigned long t;
  unsigned long tn;
  double best_score;
  unsigned long tn_when_best;
  double hit_rate_rwa;
  bane* beba;
};

search_stats* search_stats_create(bane* bn) {
  search_stats* me;
  MECALL(me,1,search_stats);
  me->beba = bane_copy(bn);
  return me;
}

void search_stats_free(search_stats* me) {
  if(me->beba != NULL) {
    bane_free(me->beba);
  }
  free(me);
}

void search_stats_assign(search_stats* dst, search_stats* src) {
  dst->t = src->t;
  dst->tn = src->tn;
  dst->best_score = src->best_score;
  dst->tn_when_best = src->tn_when_best;
  dst->hit_rate_rwa = src->hit_rate_rwa;
  bane_assign(dst->beba, src->beba);
}

void write_report_n_structure(search_stats* st, search_stats* stp,
			      char* reportfilename, char* structfilename,
			      int Titerations){
  FILE* fp;
  double better = 0;

  /* Structure first since it is bigger */

  OPENFILE_OR_DIE(fp,structfilename,"w");
  bane_write_structure(st->beba,fp);
  CLOSEFILE_OR_DIE(fp,structfilename);

  /* Report second */

  OPENFILE_OR_DIE(fp,reportfilename,"w");
  

  better = (stp->t == 0) ? 
    0 : exp(st->best_score - stp->best_score);

  fprintf(fp,"%lu %lu %lu %g %f %u\n", 
	  st->tn, 
	  st->tn - stp->tn, 
	  st->tn - st->tn_when_best, 
	  better, 
	  st->best_score,
	  Titerations);

  CLOSEFILE_OR_DIE(fp,reportfilename);

  search_stats_assign(stp,st);

}



int better_to_restart(data* dt, search_stats* st){
  int res = 0;
  long vain = st->tn - st->tn_when_best;

  if( (st->tn ==  0) 
      || ((1.0 * vain / st->tn > 0.5)  && (st->t > (unsigned) 10*dt->m))){
    /* fprintf(stderr,"rs1:%lu %ld\n", st->t, vain); */
    res = 1;
  }

  if(((long) st->t > 100 * dt->m) && (st->hit_rate_rwa > 0.95)){
    /* fprintf(stderr,"rs2: %f\n", st->hit_rate_rwa); */
    res = 1;
  }
  return res;
}


typedef int (*greedy_change)(bane*, arc*, int);
typedef void (*greedy_unchange)(bane*, arc*);

static
void remove_bad_arcs(data* dt, double ess, 
		     arc* ar, double* scoreboard, score_hashtable* sht,
		     search_stats* stg){

  int all_removed = 0; 
  bane* bn = stg->beba;

  bane_gather_full_ss(bn, dt);
  bane_get_score_param_costs(bn, ess, scoreboard);

  while(!all_removed){
    all_removed = 1;
    
    for(ar->to=0; ar->to<bn->nodecount; ++ar->to){
      node* nodi = bn->nodes + ar->to;
      for(ar->from = 0; ar->from<bn->nodecount; ++ar->from){
	if (IS_PARENT(ar->to,ar->from,bn->pmx)){
	  double old_score;
	  double gain = 0;

	  int to_params = node_param_count(bn->nodes + ar->to);
	  bane_del_arc(bn, ar);
	  int to_params_diff = node_param_count(bn->nodes + ar->to) - to_params;
	  if(!score_hashtable_get(sht, bn->pmx->mx, &old_score)) {
	    bane_gather_ss_for_i(bn, dt, ar->to);
	    gain = bane_get_score_for_i(nodi,ess) - scoreboard[ar->to]
	      - to_params_diff * param_cost;
	    if(gain >= 0) {
	      bane_del_arc_complete(bn, ar);
	      scoreboard[ar->to] += gain;
	      stg->tn_when_best = stg->tn;
	      stg->best_score += gain;
	      all_removed = 0;	      
	    } else {
	      bane_add_arc(bn, ar);
	    }
	    ++ stg->tn;
	  } else {
	    bane_add_arc(bn, ar);	  
	  }
	}
      }
    }
  }
}

static
void t_times_greedy_step(data* dt, double ess, int t_limit, int maxtblsize,
			 arc* ar, double* scoreboard, score_hashtable* sht,
			 search_stats* stl, search_stats* stg){

  int t; 
  bane* bn = stl->beba;

  greedy_change grch[3];
  greedy_unchange gruch[3];
  greedy_unchange grcmpl[3];

  grch[0] = bane_add_random_arc;
  grch[1] = bane_del_random_arc;
  grch[2] = bane_rev_random_arc;

  grcmpl[0] = bane_add_arc_complete;
  grcmpl[1] = bane_del_arc_complete;
  grcmpl[2] = bane_rev_arc_complete;

  gruch[0] = bane_del_arc;
  gruch[1] = bane_add_arc;
  gruch[2] = bane_rev_arc;

  for(t = 0; (t<t_limit) && (!usr1_set) && (!usr2_set) &&(!alrm_set); ++t){
    int ch = rand()%3;
    double new_score;
    double from_score = 0;
    double to_score = 0;

    //fprintf(stderr, "%d", ch);

    int hthit;

    if(!grch[ch](bn,ar,maxtblsize)) continue; /* if CHANGE not succeeded */

    hthit = 0;
    if(score_hashtable_get(sht, bn->pmx->mx, &new_score)) {
      hthit = 1;
    } else {
      bane_gather_ss_for_i(bn, dt, ar->to); /* to needs always work */
      to_score = bane_get_score_for_i(bn->nodes + ar->to, ess);	
      from_score = scoreboard[ar->from]; /* cheap default for from */
      if(ch==2) { /* rev - from needs to be calculated */
	bane_gather_ss_for_i(bn, dt, ar->from);
	from_score = bane_get_score_for_i(bn->nodes + ar->from, ess);
      }
      new_score = stl->best_score +  
	from_score + to_score 
      - scoreboard[ar->from] - scoreboard[ar->to];
      score_hashtable_put(sht, bn->pmx->mx, new_score);
      
      ++ stl->tn;
      ++ stg->tn;
    }

    stl->hit_rate_rwa += 0.01 * (hthit - stl->hit_rate_rwa);

    ++ stl->t;
    ++ stg->t;

    if(new_score > stl->best_score) { /* commit */
      grcmpl[ch](bn, ar);
      //fprintf(stderr, ".");
      //double check;

      /* fprintf(stderr,"%d %f\n",ch,new_score); */
      stl->best_score = new_score;
      stl->tn_when_best = stl->tn;
      if(hthit){ /* We need to update score board */ 
	bane_gather_ss_for_i(bn, dt, ar->to); /* to needs always work */
	to_score = bane_get_score_for_i(bn->nodes + ar->to, ess);	
	from_score = scoreboard[ar->from]; /* cheap default for from */
	if(ch==2) { /* rev - from needs to be calculated */
	  bane_gather_ss_for_i(bn, dt, ar->from);
	  from_score = bane_get_score_for_i(bn->nodes + ar->from, ess);
	}
      }
      scoreboard[ar->from] = from_score;
      scoreboard[ar->to] = to_score;

      //bane_gather_full_ss(bn, dt);
      //check = bane_get_score(bn, ess, scoreboard);
      //if (fabsl(stl->best_score - check) > 1E-5) {
      //  fprintf(stderr, "WRONG (%g)!", stl->best_score - check);
      //}

      if(new_score > stg->best_score) {
	stg->best_score = new_score;
	stg->tn_when_best = stg->tn;
	bane_assign(stg->beba, bn);
      }
    } else { /* retract */
      gruch[ch](bn, ar);
    }
  }
}

static int bane_repl_random_to_arc(bane* bn, arc* del_ar, arc* add_ar,
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

static int bane_repl_random_from_arc(bane* bn, arc* del_ar, arc* add_ar,
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

static int bane_repl_arc(bane *bn, arc *del_ar, arc *add_ar) {
  bane_del_arc(bn, del_ar);
  bane_add_arc(bn, add_ar);
}

static double compute_score(bane *bn, int ch, arc *ar, arc *del_ar,
			    arc *add_ar, data *dt, double *scoreboard,
			    score_hashtable *sht, double *from_score,
			    double *to_score, double ess,
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

    bane_gather_ss_for_i(bn, dt, del_ar->to); /* to needs always work */
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

void search(format *fmt, data *dt, double ess, int maxtblsize,
	    char *reportfilename, char *structfilename, int salearner,
	    double T0, double mu_T, int iterations) {
  search_stats* stg = NULL; /* global search stats */
  search_stats* stl; /* search stats for current round - local*/
  search_stats* stp; /* search stats at previous report */ 

  double* scoreboard;
  score_hashtable* sht;

  int keysize;
  int items_per_pos;
  arc* ar;

  bane* bnf;         /* forest to start */
  bane* bn = NULL;

  int* roots;
  int rootpos;

  MECALL(ar, 1, arc);

  /* Create initial forest and find root candidates */

  bnf = bane_create_forest(fmt,ess,dt);
  {
    int r;
    MEMALL(roots, bnf->nodecount, int);
    rootpos = 0; /* first postition that deserves to be root */
    for(r=0; r<bnf->nodecount; ++r){
      if((bnf->nodes[r].childcount > 0) || (bnf->nodes[r].parentcount > 0)) {
	roots[r] = r;
      } else {
	roots[r] = roots[rootpos];
	roots[rootpos] = r;
	++ rootpos;
      }
    }
  }

  /*
    Open struct file if it already exists and take that struct as
    the current best.
   */
  {
    struct stat *buf;

    FILE *fp = fopen(structfilename, "r");

    if (fp) {
      /* fprintf(stderr, "Reading existing best struct from %s\n",
      	      structfilename); */
      bn = bane_create_from_format(fmt);
      bane_read_structure(bn,fp);
      fclose(fp);
      stg = search_stats_create(bn);
      bane_gather_full_ss(stg->beba, dt);
      stg->best_score = 
	bane_get_score_param_costs(stg->beba, ess, NULL);
    }
  }

  /* Take first forest candidate */
  if (!bn) {
    int x;
    bn = bane_copy(bnf);
    if(rootpos < bnf->nodecount) {
      for(x=roots[rootpos]; bn->nodes[x].parentcount > 0; x=ar->from) {
	ar->from = bn->nodes[x].first_parent;
	ar->to = x;
	bane_rev_arc(bn,ar);
	bane_rev_arc_complete(bn,ar);
      }
      /* fprintf(stderr,"Trying root number %d\n",roots[rootpos]); */
      ++rootpos;
    }
  }


  if (!stg) {
    stg = search_stats_create(bn);
    stg->best_score = 0;
  }
  stl = search_stats_create(bn);
  stp = search_stats_create(bn);

  bane_free(bn);

  MECALL(scoreboard, stl->beba->nodecount, double);

  keysize = stl->beba->pmx->m * stl->beba->pmx->one_dim_size;
  items_per_pos = score_hashtable_items_for_mem(500000, keysize);
  if(items_per_pos < 1) items_per_pos = 1;
  if(items_per_pos > 10) items_per_pos = 10;
  sht = score_hashtable_create(keysize, items_per_pos);

  bane_gather_full_ss(stl->beba, dt);
  stl->best_score = 
    bane_get_score_param_costs(stl->beba, ess, scoreboard);

  if (stg->best_score == 0)
    stg->best_score = stl->best_score;
  score_hashtable_put(sht,stl->beba->pmx->mx, stg->best_score); 
  stl->hit_rate_rwa = 0;

  ++stl->t;
  ++stg->t;
  ++stl->tn;
  ++stg->tn;

  stp->t = -1;
  stp->tn = -1;
  stp->best_score = stg->best_score;

  if (salearner) {
    double T = T0;
    int Titerations = 0;

    arc* del_ar;
    arc* add_ar;

    greedy_change sach[3];
    greedy_unchange sauch[3];
    greedy_unchange sacmpl[3];

    sach[0] = bane_add_random_arc;
    sach[1] = bane_del_random_arc;
    sach[2] = bane_rev_random_arc;

    sacmpl[0] = bane_add_arc_complete;
    sacmpl[1] = bane_del_arc_complete;
    sacmpl[2] = bane_rev_arc_complete;

    sauch[0] = bane_del_arc;
    sauch[1] = bane_add_arc;
    sauch[2] = bane_rev_arc;

    MECALL(del_ar, 1, arc);
    MECALL(add_ar, 1, arc);

    int calibrating = 0;

    if (T == 0) {
      calibrating = 1;
      fprintf(stderr, "Calibrating T0\n");
      T = 1; // start from 1, and go up until accept ratio > 0.8
    }
   
    while (!usr2_set) {
      int n_accepts = 0;
      int n_rejects = 0;
      int n_eless = 0;
  
      int i;

      double new_score;

      for (i = 0; i < (calibrating ? 1000 : iterations); ++i) {
	/*
	 * take a step
	 */
	//fprintf(stderr, ".");
	bane *bn = stl->beba;

	int ch;
	int hthit;
	int accept;
	double from_score = 0;
	double to_score = 0;

	for (;;) {
	  ch = rand() % 5;

	  //fprintf(stderr, "%d", ch);

	  if (ch < 3) {
	    if (sach[ch](bn, ar, maxtblsize))
	      break;
	  } else
	    if (ch == 3) {
	      if (bane_repl_random_to_arc(bn, del_ar, add_ar, maxtblsize))
		break;
	    } else {
	      if (bane_repl_random_from_arc(bn, del_ar, add_ar, maxtblsize))
		break;
	    }
	}

	//fprintf(stderr, "!");

	hthit = 0;

	if (score_hashtable_get(sht, bn->pmx->mx, &new_score)) {
	  hthit = 1;
	} else {
	  new_score = compute_score(bn, ch, ar, del_ar,
				    add_ar, dt, scoreboard,
				    sht, &from_score, &to_score,
				    ess, stl->best_score);
	  //fprintf(stderr, "N");
	  if (ch < 3) {
	    ++ stl->tn;
	    ++ stg->tn;
	  } else {
	    stl->tn += 2;
	    stg->tn += 2;
	  }
	}

	/*
	 * new_E: new_score; best_E: stg->score
	 */
	accept = new_score > stl->best_score;

	if (accept) {
	  //if (ch == 4) fprintf(stderr, "f");
	  ++n_eless;
	} else
	  if (drand48()
	      < exp (-(stl->best_score - new_score) / T)) {
	    accept = 1;
	    ++n_accepts;
	    //if (ch == 4) fprintf(stderr, "g");
	  } else {
	    ++n_rejects;
	    //fprintf(stderr, "r");
	  }

	if (accept) {
	  double score_check = stl->best_score;
	  double check;
	  stl->best_score = new_score;
	  stl->beba = bn;

	  //fprintf(stderr, "a");

	  if (hthit) {
	    compute_score(bn, ch, ar, del_ar, add_ar, dt, scoreboard, NULL,
			  &from_score, &to_score, ess, score_check);
	  }

	  if (ch < 3)
	    sacmpl[ch](bn, ar);
	  else {
	    bane_del_arc(bn, add_ar);
	    bane_del_arc_complete(bn, del_ar);
	    bane_add_arc(bn, add_ar);
	    bane_add_arc_complete(bn, add_ar);
	  }

	  scoreboard[ar->from] = from_score;
	  scoreboard[ar->to] = to_score;

	  /*
	    bane_gather_full_ss(stl->beba, dt);
	    check = bane_get_score_param_costs(stl->beba, ess, scoreboard);
	    if (fabsl(stl->best_score - check) > 1E-5) {
	    fprintf(stderr, "WRONG! (%g)", stl->best_score - check);
	    stl->best_score = check;
	    }
	  */

	  if (new_score > stg->best_score) {
	    stg->best_score = new_score;
	    stg->tn_when_best = stg->tn;
	    bane_assign(stg->beba, bn);
	    fprintf(stderr, "N");
	  } else if (new_score == stg->best_score) {
	    fprintf(stderr, "h");
	  }
	} else {
	  if (ch < 3)
	    sauch[ch](bn, ar);
	  else
	    bane_repl_arc(bn, add_ar, del_ar);
	}

	if(usr1_set || alrm_set) {
	  usr1_set = 0;
	  if(alrm_set) {
	    remove_bad_arcs(dt, ess, ar, scoreboard, sht, stg);
	  }
	  write_report_n_structure(stg, stp, reportfilename, structfilename,
				   Titerations);
	  if(alrm_set){
	    usr2_set = 1;
	  }
	}
      }

      double acceptratio
	= (double)(n_accepts + n_eless)/(n_accepts + n_eless + n_rejects);
      double acceptratio2
	= (double)(n_eless)/(n_accepts + n_eless + n_rejects);

      if (calibrating) {
	if (acceptratio >= 0.6) {
	  // between 40 and 90: http://dx.doi.org/10.1016/S0045-7949(03)00214-1
	  calibrating = 0;
	  T0 = T;
	  fprintf(stderr, "Calibration done\n");
	} else {
	  T *= 1.1;
	}
      } else {
	T /= mu_T;

	if ((acceptratio < 0.10) && (acceptratio2 < 0.005)) {
	  ++Titerations;
	  T = T0;
	  write_report_n_structure(stg, stp, reportfilename, structfilename,
				   Titerations);

	  if (Titerations == 3)
	    exit(0);
	}
      }

      fprintf(stderr, "T: %g score: %g (accept-ratio: %g, %g)\n", T,
	      new_score, acceptratio, acceptratio2);
    }
  } else {
    while(!usr2_set){
      t_times_greedy_step(dt, ess, 100, maxtblsize, ar, scoreboard, sht, stl, stg);
      if(usr1_set || alrm_set) {
	usr1_set = 0;
	if(alrm_set) {
	  remove_bad_arcs(dt, ess, ar, scoreboard, sht, stg);
	}
	write_report_n_structure(stg, stp, reportfilename, structfilename, 0);
	if(alrm_set){
	  usr2_set = 1;
	}
      }

      if (better_to_restart(dt,stl)){
	if (rand() % fmt->dim < (fmt->dim - rootpos)) {
	  fprintf(stderr,"Trying root number %d\n",roots[rootpos]);
	  int x;
	  bn = bane_copy(bnf);
	  for(x=roots[rootpos]; bn->nodes[x].parentcount > 0; x=ar->from) {
	    ar->from = bn->nodes[x].first_parent;
	    ar->to = x;
	    bane_rev_arc(bn,ar);
	    bane_rev_arc_complete(bn,ar);
	  }
	  ++rootpos;
	} else {
	  int t;
	  if ((rand() % 3) == 0) {
	    /* take best one and mutilate it */
	    fprintf(stderr, "Mutilating best one\n");
	    bn = bane_copy(stg->beba);
	  } else {
	    int n;
	    parent_matrix* smx;
	    /* Take some good one out of hashtable and mutilate it */
	    
	    n = 1 + rand() % (1 + (int) (sht->keycount * 0.01));
	    fprintf(stderr, "Mutilating %d\n", n);
	    smx = parent_matrix_create_wrap(stl->beba->nodecount,
					    score_hashtable_get_nth_key(sht,n));
	    bn = bane_create_from_pmx(fmt,smx);
	    parent_matrix_free(smx);
	  }
	  
	  for(t=0; t<bn->nodecount / 3; ++t) {
	    bane_del_random_arc(bn,ar,maxtblsize);
	    bane_del_arc_complete(bn, ar);
	  } 
	}
	

	bane_assign(stl->beba, bn);
	bane_free(bn);

	bane_gather_full_ss(stl->beba, dt);
	stl->t = 0;
	stl->tn = 0;
	stl->best_score = bane_get_score(stl->beba, ess, scoreboard);
	stl->tn_when_best = 0;
	stl->hit_rate_rwa = 0;
      }
    }
  }

  search_stats_free(stl);
  search_stats_free(stg);
  search_stats_free(stp);
  free(scoreboard);
  free(ar);
  score_hashtable_free(sht);
  free(roots);
}

int main(int argc, char* argv[]){
  int n;
  data* dt;
  format* fmt;
  double ess;
  int searchtime;
  FILE* fp;
  double T0;
  double mu_T;
  int iterations;
  int salearner = strstr(argv[0], "bnsalearner") != NULL;
  int saautolearner = strstr(argv[0], "bnsaautolearner") != NULL;

  if (salearner) {
    if (argc != 13) {
      fprintf(stderr,
	      "Usage: %s vdfile datafile datacount ess "
	      "reportfile structfile searchtime(<0 forever) "
	      "T0 mu_T iterations param_cost pidfile\n", 
	      argv[0]);
      exit(-1);
    }
  } else if (saautolearner) {
    if (argc != 11) {
      fprintf(stderr,
	      "Usage: %s vdfile datafile datacount ess "
	      "reportfile structfile searchtime(<0 forever) "
	      "iterations param_cost pidfile\n", 
	      argv[0]);
      exit(-1);
    }
  } else {
    if (argc != 9) {
      fprintf(stderr,
	      "Usage: %s vdfile datafile datacount ess "
	      "reportfile structfile searchtime(<0 forever) pidfile\n", 
	      argv[0]);
      exit(-1);
    }
  }

  set_signal_handlers();

  fclose(stdin);
  fclose(stdout);
  /* fclose(stderr); */

  OPENFILE_OR_DIE(fp, argv[argc-1], "w");
  fprintf(fp,"%d\n",getpid());
  CLOSEFILE_OR_DIE(fp, argv[argc-1]);

  fmt = format_cread(argv[1]);

  n = atoi(argv[3]);
  dt = data_create(n,fmt->dim);
  data_read(argv[2],dt);

  ess = atof(argv[4]);

  searchtime = atoi(argv[7]);
  if (searchtime >= 0) { /* HEY, 0 WILL NOT DO ANYTHING */
    alarm(searchtime);
  }

  if (salearner) {
    T0 = atof(argv[8]);
    mu_T = atof(argv[9]);
    iterations = atoi(argv[10]);
    if (argc == 13) {
      param_cost = atof(argv[11]);
      fprintf(stderr, "Using param cost: %g\n", param_cost);
    }
  } else if (saautolearner) {
    T0 = 0;
    mu_T = 1.01;
    iterations = atoi(argv[8]);
    param_cost = atof(argv[9]);
    fprintf(stderr, "Using param cost: %g\n", param_cost);
  }

  search(fmt, dt, ess, 10000, argv[5], argv[6],
	 salearner || saautolearner, T0, mu_T, iterations);

  format_free(fmt);
  data_free(dt);
  
  return 0;
}
