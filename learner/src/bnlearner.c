#include <stdio.h>
#include <math.h>
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

#ifdef WIN32
#include "drand48.c"
#endif

double param_cost = 0;

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

bane *open_or_create(char *structfilename, format *fmt, data *dt, double ess)
{
  /*
    Open struct file if it already exists and take that struct as
    the current best.
   */
  FILE *fp = fopen(structfilename, "r");
  arc ar;
  bane *bn;

  if (fp) {
    bn = bane_create_from_format(fmt);
    bane_read_structure(bn,fp);
    fclose(fp);
  } else  {
    int r;
    bane* bnf = bane_create_forest(fmt,ess,dt);
    int* roots;
    int  rootpos;

    MEMALL(roots, bnf->nodecount, int);
    rootpos = 0; /* first postition that deserves to be root */
    for (r=0; r<bnf->nodecount; ++r){
      if ((bnf->nodes[r].childcount > 0) || (bnf->nodes[r].parentcount > 0)) {
	roots[r] = r;
      } else {
	roots[r] = roots[rootpos];
	roots[rootpos] = r;
	++ rootpos;
      }
    }

    bn = bane_copy(bnf);
    if (rootpos < bnf->nodecount) {
      for (r=roots[rootpos]; bn->nodes[r].parentcount > 0; r=ar.from) {
	ar.from = bn->nodes[r].first_parent;
	ar.to = r;
	bane_rev_arc(bn,&ar);
	bane_rev_arc_complete(bn,&ar);
      }
    }

    free(roots);
    bane_free(bnf);
  }

  return bn;
}

typedef int (*greedy_change)(bane*, arc*, int);
typedef void (*greedy_unchange)(bane*, arc*);

void search(format *fmt, data *dt, double ess, int maxtblsize,
	    char *reportfilename, char *structfilename,
	    int iterations, int coolings) {
  search_stats* stg; /* global search stats */
  search_stats* stl; /* search stats for current round - local*/
  search_stats* stp; /* search stats at previous report */ 

  double* scoreboard;
  score_hashtable* sht;

  arc* ar;
  arc* del_ar;
  arc* add_ar;

  greedy_change sach[3];
  greedy_unchange sauch[3];
  greedy_unchange sacmpl[3];

  double T0, T;
  double mu_T = 1.01;
  int Titerations = 0;

  bane* bn = open_or_create(structfilename, fmt, dt, ess);

  stg = search_stats_create(bn);
  stl = search_stats_create(bn);
  stp = search_stats_create(bn);

  sht = create_hashtable(500000, bn);

  bane_free(bn);

  /*
   * set as global best
   */
  bane_gather_full_ss(stg->beba, dt);
  stg->best_score = bane_get_score_param_costs(stg->beba, ess, NULL);

  /*
   * set as local best, with score board
   */
  MECALL(scoreboard, stl->beba->nodecount, double);

  bane_gather_full_ss(stl->beba, dt);
  stl->best_score = bane_get_score_param_costs(stl->beba, ess, scoreboard);
  score_hashtable_put(sht,stl->beba->pmx->mx, stl->best_score); 
  stl->hit_rate_rwa = 0;

  ++stl->t;
  ++stg->t;
  ++stl->tn;
  ++stg->tn;

  stp->t = -1;
  stp->tn = -1;
  stp->best_score = stg->best_score;

  sach[0] = bane_add_random_arc;
  sach[1] = bane_del_random_arc;
  sach[2] = bane_rev_random_arc;

  sacmpl[0] = bane_add_arc_complete;
  sacmpl[1] = bane_del_arc_complete;
  sacmpl[2] = bane_rev_arc_complete;

  sauch[0] = bane_del_arc;
  sauch[1] = bane_add_arc;
  sauch[2] = bane_rev_arc;

  MECALL(ar, 1, arc);
  MECALL(del_ar, 1, arc);
  MECALL(add_ar, 1, arc);

  int improved = 1;
  int calibrating = 1;
  double max_a = 0, max_b = 0;

  T = 1; // start from 1, and go up until accept ratio > 0.6

  for(;;) {
    int n_accepts = 0;
    int n_rejects = 0;
    int n_eless = 0;
  
    int i;

    double new_score;

    for (i = 0; i < (calibrating ? 1000 : iterations); ++i) {
      /*
       * take a step
       */
      bane *bn = stl->beba;

      int ch;
      int hthit;
      int accept;
      double from_score = 0;
      double to_score = 0;

      for (;;) {
	ch = rand() % 5;

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

      hthit = 0;

      if (score_hashtable_get(sht, bn->pmx->mx, &new_score)) {
	hthit = 1;
      } else {
	new_score = update_score(bn, ch, ar, del_ar,
				 add_ar, dt, scoreboard,
				 sht, &from_score, &to_score,
				 ess, param_cost, stl->best_score);
	if (ch < 3) {
	  ++ stl->tn;
	  ++ stg->tn;
	} else {
	  stl->tn += 2;
	  stg->tn += 2;
	}
      }

      accept = new_score > stl->best_score;

      if (accept) {
	++n_eless;
      } else
	if (drand48()
	    < exp (-(stl->best_score - new_score) / T)) {
	  accept = 1;
	  ++n_accepts;
	} else {
	  ++n_rejects;
	}

      if (accept) {
	double score_check = stl->best_score;
	stl->best_score = new_score;
	stl->beba = bn;

	if (hthit) {
	  /*
	   * although we know the score of the entire network, we also
	   * need to update the scoreboard, using from_score and
	   * to_score
	   */
	  update_score(bn, ch, ar, del_ar, add_ar, dt, scoreboard, NULL,
		       &from_score, &to_score, ess, param_cost, score_check);
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
	  improved = 1;
	} else if (new_score == stg->best_score) {
	}
      } else {
	if (ch < 3)
	  sauch[ch](bn, ar);
	else
	  bane_repl_arc(bn, add_ar, del_ar);
      }
    }

    double acceptratio_a
      = (double)(n_accepts + n_eless)/(n_accepts + n_eless + n_rejects);
    double acceptratio_b
      = (double)(n_eless)/(n_accepts + n_eless + n_rejects);

    if (acceptratio_a > max_a)
      max_a = acceptratio_a;
    if (acceptratio_b > max_b)
      max_b = acceptratio_b;

    if (calibrating) {
      if (acceptratio_a >= 0.6) {
	// between 40 and 90: http://dx.doi.org/10.1016/S0045-7949(03)00214-1
	calibrating = 0;
	T0 = T;
      } else {
	T *= 1.1;
      }
    } else {
      T /= mu_T;

      if (improved) {
	write_report_n_structure(stg, stp, reportfilename, structfilename,
				 Titerations);
	improved = 0;
      }

#define AR_A_CUTOFF 0.20
#define AR_B_CUTOFF 0.005

      if ((acceptratio_a <= AR_A_CUTOFF) && (acceptratio_b <= AR_B_CUTOFF)) {
	++Titerations;
	T = T0;
	write_report_n_structure(stg, stp, reportfilename, structfilename,
				 Titerations);

	if (Titerations == coolings)
	  exit(0);
	max_a = 0;
	max_b = 0;
      } else {
      }

      double p_a = 1 - (acceptratio_a - AR_A_CUTOFF)/(max_a - AR_A_CUTOFF);
      double p_b = 1 - (acceptratio_b - AR_B_CUTOFF)/(max_b - AR_B_CUTOFF);

      double p = p_a < p_b ? p_a : p_b;
      double p_total = (double)Titerations/coolings + p/coolings;

      fprintf(stderr, "%g\n", p_total);
    }
  }

  search_stats_free(stl);
  search_stats_free(stg);
  search_stats_free(stp);
  free(scoreboard);
  free(ar);
  score_hashtable_free(sht);
}

int main(int argc, char* argv[]){
  int n;
  data* dt;
  format* fmt;
  double ess;
  FILE* fp;
  int iterations, coolings;

  if (argc != 11) {
    fprintf(stderr,
	    "Usage: %s vdfile datafile datacount ess "
	    "reportfile structfile iterations coolings param_cost pidfile\n", 
	    argv[0]);
    exit(-1);
  }

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

  iterations = atoi(argv[7]);
  coolings = atoi(argv[8]);
  param_cost = atof(argv[9]);

  search(fmt, dt, ess, 10000, argv[5], argv[6], iterations, coolings);

  format_free(fmt);
  data_free(dt);
  
  return 0;
}
