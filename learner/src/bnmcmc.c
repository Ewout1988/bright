#include <assert.h>
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

static int bane_change_random_add(bane *bn, arc *ar, unsigned nb_size,
				  int maxtblsize)
{
  unsigned op = rand() % nb_size;
  unsigned j = 0;
  unsigned i;

  for (i=0; i < bn->nodecount; ++i) {
    node* nodi = bn->nodes + i;

    int add_count = bn->nodecount - nodi->ancestorcount - nodi->childcount - 1;

    if (op < j + add_count) {
      if (!bane_add_arc_i_from(bn, nodi, ar, op - j, maxtblsize))
	return -1;
      return Add;
    }

    j += add_count;
  }

  assert(0);
}

static int bane_change_random_del(bane *bn, arc *ar, unsigned nb_size,
				  int maxtblsize)
{
  unsigned op = rand() % nb_size;
  unsigned j = 0;
  unsigned i;

  for (i=0; i < bn->nodecount; ++i) {
    node* nodi = bn->nodes + i;

    int del_count = nodi->childcount;

    if (op < j + del_count) {
      if (!bane_del_arc_i_from(bn, nodi, ar, op - j))
	return -1;
      return Del;
    }

    j += del_count;
  }

  assert(0);
}

static int bane_change_random_to(bane *bn, arc *del_ar, arc *add_ar,
				 unsigned nb_size, int maxtblsize)
{
  unsigned op = rand() % nb_size;
  unsigned j = 0;
  unsigned i;

  for (i=0; i < bn->nodecount; ++i) {
    node* nodi = bn->nodes + i;

    /* change_to */
    int dst_count = bn->nodecount - nodi->ancestorcount - nodi->childcount - 1;
    int to_count = nodi->childcount * dst_count;

    if (op < j + to_count) {
      int k = op - j;

      if (!bane_change_arc_dst(bn, nodi, del_ar, add_ar,
			       k / dst_count, k % dst_count, maxtblsize))
	return -1;
      return ChangeTo;
    }

    j += to_count;
  }

  assert(0);
}

static int bane_change_random_from(bane *bn, arc *del_ar, arc *add_ar,
				   unsigned nb_size, int maxtblsize)
{
  unsigned op = rand() % nb_size;
  unsigned j = 0;
  unsigned i;

  for (i=0; i < bn->nodecount; ++i) {
    node* nodi = bn->nodes + i;

    /* change_from */
    int src_count = bn->nodecount - nodi->offspringcount - nodi->parentcount-1;
    int from_count = nodi->parentcount * src_count;

    if (op < j + from_count) {
      int k = op - j;

      if (!bane_change_arc_src(bn, nodi, del_ar, add_ar,
			       k / src_count, k % src_count, maxtblsize))
	return -1;
      return ChangeFrom;
    }

    j += from_count;
  }

  assert(0);
}

typedef struct step_aux_info
{
  int ch;
  arc ar, del_ar, add_ar;
  double from_score, to_score;
  unsigned *new_nb_size;
  int hthit;
} step_aux_info;

static void propose_step(bane *current, bane *bn, step_aux_info *sinfo,
			 unsigned *nb_size, int maxtblsize)
{
  int ch;

  bane_assign(bn, current);

  for (;;) {
    /*
     * propose and apply a step to bn
     */
    ch = rand() % 5;

    if (ch != Rev && nb_size[ch] == 0)
      continue;

    switch (ch) {
    case Add:
      ch = bane_change_random_add(bn, &sinfo->ar, nb_size[ch], maxtblsize);
      break;
    case Del:
      ch = bane_change_random_del(bn, &sinfo->ar, nb_size[ch], maxtblsize);
      break;
    case Rev:
      if (!bane_rev_random_arc(bn, &sinfo->ar, maxtblsize))
	continue; /* could not revert any edge, try another Op */
      else
	bane_rev_arc_complete(bn, &sinfo->ar);
      break;
    case ChangeTo:
      ch = bane_change_random_to(bn, &sinfo->del_ar, &sinfo->add_ar,
				 nb_size[ch],
				 maxtblsize);
      break;
    case ChangeFrom:
      ch = bane_change_random_from(bn, &sinfo->del_ar, &sinfo->add_ar,
				   nb_size[ch],
				   maxtblsize);
      break;
    }

    if (ch == -1)
      bane_assign(bn, current);
    else
      break;
  }

  sinfo->ch = ch;
}

double calc_score(bane *bn, data *dt, double current_score,
		  score_hashtable *sht,
		  step_aux_info *sinfo, double *scoreboard,
		  double ess, double param_cost)
{
  double new_score;

  sinfo->hthit = 0;

  if (score_hashtable_get(sht, bn->pmx->mx, &new_score))
    sinfo->hthit = 1;
  else
    new_score = update_score(bn, sinfo->ch, &sinfo->ar, &sinfo->del_ar,
			     &sinfo->add_ar, dt, scoreboard,
			     sht, &sinfo->from_score, &sinfo->to_score,
			     ess, param_cost, current_score);

  return new_score;
}

void complete_step(bane *bn, data *dt, double current_score,
		   double *scoreboard, step_aux_info *sinfo,
		   double ess, double param_cost)
{
  if (sinfo->hthit) {
    /*
     * although we know the score of the entire network, we also
     * need to update the scoreboard, and thus calculate from_score
     * and to_score
     */
    update_score(bn, sinfo->ch, &sinfo->ar, &sinfo->del_ar, &sinfo->add_ar,
		 dt, scoreboard, NULL, &sinfo->from_score, &sinfo->to_score,
		 ess, param_cost, current_score); 
  }

  scoreboard[sinfo->ar.from] = sinfo->from_score;
  scoreboard[sinfo->ar.to] = sinfo->to_score;
}

double calc_mh(bane *bn, double current_score, double new_score,
	       step_aux_info *sinfo, unsigned *nb_size)
{
  double hastings_ratio;

  bane_calc_neighbourhood_sizes(bn, sinfo->new_nb_size);

  switch (sinfo->ch) {
  case Add:
    hastings_ratio
      = (double)nb_size[Add] / sinfo->new_nb_size[Del];
    break;
  case Del:
    hastings_ratio
      = (double)nb_size[Del] / sinfo->new_nb_size[Add];
    break;
  case Rev:
    hastings_ratio = 1.;
    break;
  case ChangeTo:
    hastings_ratio
      = (double)nb_size[ChangeTo] / sinfo->new_nb_size[ChangeTo];
    break;
  case ChangeFrom:
    hastings_ratio
      = (double)nb_size[ChangeFrom] / sinfo->new_nb_size[ChangeFrom];
  }

  return hastings_ratio * exp(new_score - current_score);
}

void sample(format *fmt, data *dt, double ess, int maxtblsize,
	    int chain_length, int sample_interval)
{
  double* scoreboard;
  score_hashtable* sht;

  arc* ar;
  arc* del_ar;
  arc* add_ar;

  double current_score, max_score;
  bane *proposal, *current;

  int iteration;
  unsigned *nb_size;

  struct step_aux_info sinfo;

  int n_accepts = 0;
  int n_rejects = 0;
  int n_eless = 0;

  proposal = bane_create_from_format(fmt);
  current = bane_create_from_format(fmt);

  sht = create_hashtable(500000, current);

  MECALL(scoreboard, current->nodecount, double);
  MECALL(nb_size, 5, unsigned);
  MECALL(sinfo.new_nb_size, 5, unsigned);

  bane_gather_full_ss(current, dt);
  current_score = bane_get_score_param_costs(current, ess, scoreboard);
  score_hashtable_put(sht, current->pmx->mx, current_score); 

  bane_calc_neighbourhood_sizes(current, nb_size);

  max_score = current_score;

  for (iteration = 0; iteration < chain_length;) {
    int accept;
    double new_score;
    double mh;

    propose_step(current, proposal, &sinfo, nb_size, maxtblsize);

    new_score = calc_score(proposal, dt, current_score, sht, &sinfo,
			   scoreboard, ess, param_cost);

    mh = calc_mh(proposal, current_score, new_score, &sinfo, nb_size);
    
    accept = mh >= 1;

    if (accept) {
      ++n_eless;
    } else
      if (drand48() < mh) {
	accept = 1;
	++n_accepts;
      } else {
	++n_rejects;
      }

    if (accept) {
      bane *sw;
      unsigned *nbsw;

      complete_step(proposal, dt, current_score, scoreboard, &sinfo,
		    ess, param_cost);

      /* swap current - proposal */
      sw = current;
      current = proposal;
      proposal = sw;

      nbsw = nb_size;
      nb_size = sinfo.new_nb_size;
      sinfo.new_nb_size = nbsw;

      current_score = new_score;

      if (current_score > max_score) {
	FILE *fp;

	OPENFILE_OR_DIE(fp,"best.str","w");
	bane_write_structure(current,fp);
	CLOSEFILE_OR_DIE(fp,"best.str");

	max_score = current_score;
      }
    }

    if (iteration % sample_interval == 0) {
      FILE *fp;

      fprintf(stdout, "%d,%g,%g,%d\n", iteration, current_score, max_score,
	      bane_arc_count(current));

      OPENFILE_OR_DIE(fp,"sample.str","w");
      bane_write_structure(current,fp);
      CLOSEFILE_OR_DIE(fp,"sample.str");
    }

    ++iteration;
  }

  free(scoreboard);
  free(ar);
  free(add_ar);
  free(del_ar);
  score_hashtable_free(sht);
}

int main(int argc, char* argv[]){
  int n;
  data* dt;
  format* fmt;
  double ess;
  FILE* fp;
  int chain_length, sample_interval;

  srand48(getpid());

  if (argc != 9) {
    fprintf(stderr,
	    "Usage: %s vdfile datafile datacount ess "
	    "param_cost chain_length sample_interval pidfile\n", 
	    argv[0]);
    exit(-1);
  }

  fclose(stdin);
  /* fclose(stdout); */
  /* fclose(stderr); */

  OPENFILE_OR_DIE(fp, argv[argc-1], "w");
  fprintf(fp,"%d\n",getpid());
  CLOSEFILE_OR_DIE(fp, argv[argc-1]);

  fmt = format_cread(argv[1]);

  n = atoi(argv[3]);
  dt = data_create(n,fmt->dim);
  data_read(argv[2],dt);

  ess = atof(argv[4]);
  param_cost = atof(argv[5]);

  chain_length = atoi(argv[6]);
  sample_interval = atoi(argv[7]);

  TRACK_OFFSPRING_COUNT = 1;

  sample(fmt, dt, ess, 10000, chain_length, sample_interval);

  format_free(fmt);
  data_free(dt);
  
  return 0;
}
