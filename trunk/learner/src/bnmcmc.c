#include <assert.h>
#include <stdio.h>
#include <math.h>
#include <errno.h>
#include <unistd.h>
#include <string.h>
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

static double calc_score(bane *bn, data *dt, double current_score,
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

static void complete_step(bane *bn, data *dt, double current_score,
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

static double calc_m(double current_score, double new_score,
		     double T, double H0)
{
  if (-new_score < H0)
    new_score = -H0;
  if (-current_score < H0)
    current_score = -H0;

  return exp((new_score - current_score)/T);
}

static double calc_mh(bane *bn, double current_score, double new_score,
		      step_aux_info *sinfo, unsigned *nb_size, double T,
		      double H0)
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

  if (-new_score < H0)
    new_score = -H0;
  if (-current_score < H0)
    current_score = -H0;

  return hastings_ratio * calc_m(current_score, new_score, T, H0);
}

typedef struct energy_ring {
  unsigned  num_samples;
  int       struct_size;

  unsigned *sample_mx;         /* array of network structures */
  double   *sample_score;      /* array of scores, score = -h(x) */
} energy_ring;

void energy_ring_init(energy_ring *ring, unsigned ring_size, bane *bn)
{
  ring->struct_size = bn->pmx->m * bn->pmx->one_dim_size;
  ring->num_samples = 0;

  MECALL(ring->sample_mx, ring->struct_size * ring_size, unsigned);
  MECALL(ring->sample_score, ring_size, double);
}

typedef struct mcmc_chain {
  double             T;

  energy_ring       *energy_rings; /* array of rings */

  struct mcmc_chain *prev;         /* ring at higher T */
  struct mcmc_chain *next;         /* ring at lower T */

  /* info on current state */
  bane     *current;
  double    current_score;
  double   *scoreboard;
  unsigned *nb_size;

  /* diagnostics */
  int mh_accepts;
  int mh_rejects;
  int mh_eless;

  int ee_accepts;
  int ee_rejects;
  int ee_eless;

} mcmc_chain;

static mcmc_chain *chain_create(mcmc_chain *prev, double T, int K,
				unsigned sample_length,
				unsigned sample_interval,
				bane *bn, data *dt, double ess,
				double param_cost)
{
  mcmc_chain *chain;
  MECALL(chain, 1, mcmc_chain);

  chain->T = T;

  if (prev)
    prev->next = chain;
  chain->prev = prev;

  MECALL(chain->scoreboard, bn->nodecount, double);
  MECALL(chain->nb_size, 5, unsigned);

  chain->current = bane_copy(bn);
  bane_gather_full_ss(chain->current, dt);
  chain->current_score
    = bane_get_score_param_costs(chain->current, ess, chain->scoreboard);
  bane_calc_neighbourhood_sizes(chain->current, chain->nb_size); 

  if (T > 1.) { // an auxiliary chain, create energy rings
    int r;
    MECALL(chain->energy_rings, K+1, energy_ring);
    for (r = 0; r < K+1; ++r)
      energy_ring_init(chain->energy_rings + r, sample_length/sample_interval,
		       chain->current);
  } else
    chain->energy_rings = 0;

  chain->mh_accepts = chain->mh_rejects = chain->mh_eless = 0;
  chain->ee_accepts = chain->ee_rejects = chain->ee_eless = 0;

  return chain;
}

static int get_ring_index(double score, double *H, int K)
{
  double h = -score;
  int i;

  if (h < H[0]) {
    fprintf(stderr, "Oops: %g lower than H0 (%g): ", h, H[0]);
    exit(-1);
  }

  for (i = 1; i <= K; ++i)
    if (h < H[i])
      return i-1;

  return K;
}

static void init_ladder(double *T, double *H, int K,
			double H0, double HK, double c)
{
  int i;

  T[0] = 1;
  H[0] = H0;

  if (K > 0) {
    double logHStep = log(HK/H0)/K;
    for (i = 1; i <= K; ++i) {
      H[i] = exp(log(H0) + i*logHStep);
      T[i] = T[i-1] * c;
    }
  }

  fprintf(stderr, "T: ");
  for (i = 0; i <= K; ++i)
    fprintf(stderr, " %g", T[i]);
  fprintf(stderr, "\n");

  fprintf(stderr, "H: ");
  for (i = 0; i <= K; ++i)
    fprintf(stderr, " %g", H[i]);
  fprintf(stderr, "\n");
}

static void sample(format *fmt, data *dt, double ess, int maxtblsize,
		   int chain_length, int sample_interval,
		   int K, int B, int N,
		   double H0, double HK, double c, double pee)
{
  score_hashtable* sht;

  parent_matrix* tmp_pmx;

  double max_score = -HUGE;
  bane *proposal;
  bane *empty;
  struct step_aux_info sinfo;

  int iteration;
  int last_iteration;

  double *T;
  double *H;
  mcmc_chain *chainK = NULL;

  MECALL(sinfo.new_nb_size, 5, unsigned);
  MECALL(T, K+1, double);
  MECALL(H, K+1, double);

  init_ladder(T, H, K, H0, HK, c);

  proposal = bane_create_from_format(fmt);
  empty = bane_create_from_format(fmt);
  
  bane_gather_full_ss(proposal, dt);

  tmp_pmx = parent_matrix_create(proposal->nodecount);

  sht = create_hashtable(500000, proposal);

  last_iteration = chain_length + (N+B)*K;

  printf("state\tlog posterior\n");

  for (iteration = 0; iteration < last_iteration; ++iteration) {
    int k;
    mcmc_chain *chain = NULL;

    for (k = K; k >= 0; --k) {
      if (iteration == (K-k)*(B+N)) {
	mcmc_chain *next
	  = chain_create(chain, T[k], K, chain_length-B, sample_interval,
			 proposal, dt, ess, param_cost);
	if (chainK == NULL)
	  chainK = next;
      }

      if (K == k)
	chain = chainK;
      else
	chain = chain->next;

      if (!chain)
	break;

      if (iteration >= last_iteration - k*(B+N))
	continue;

      int doEEStep = 0;
      energy_ring *ring;

      if (k != K) {
	int I = get_ring_index(chain->current_score, H, K);
	ring = chain->prev->energy_rings + I;
	if (ring->num_samples)
	  doEEStep = drand48() < pee;
      }

      if (doEEStep) {
	int accept;
	int sampleI = rand() % ring->num_samples;
	double new_score = ring->sample_score[sampleI];

	double d
	  = calc_m(chain->current_score, new_score, chain->T, H[k])
	  / calc_m(chain->current_score, new_score, chain->prev->T, H[k+1]);

	accept = d >= 1;

	if (accept) {
	  ++chain->ee_eless;
	} else
	  if (drand48() < d) {
	    accept = 1;
	    ++chain->ee_accepts;
	  } else {
	    ++chain->ee_rejects;
	  }

	if (accept) {
	  memcpy(tmp_pmx->mx, ring->sample_mx + sampleI * ring->struct_size, 
		 sizeof(unsigned) * ring->struct_size);
	  bane_assign(chain->current, empty);
	  bane_assign_from_pmx(chain->current, tmp_pmx);
	  bane_gather_full_ss(chain->current, dt);
	  chain->current_score
	    = bane_get_score_param_costs(chain->current, ess,
					 chain->scoreboard);
	  bane_calc_neighbourhood_sizes(chain->current, chain->nb_size);
	}
      } else {
	int accept;
	double new_score;
	double mh;

	propose_step(chain->current, proposal, &sinfo, chain->nb_size,
		     maxtblsize);

	new_score = calc_score(proposal, dt, chain->current_score, sht, &sinfo,
			       chain->scoreboard, ess, param_cost);

	mh = calc_mh(proposal, chain->current_score, new_score, &sinfo,
		     chain->nb_size, chain->T, H[k]);
    
	accept = mh >= 1;

	if (accept) {
	  ++chain->mh_eless;
	} else
	  if (drand48() < mh) {
	    accept = 1;
	    ++chain->mh_accepts;
	  } else {
	    ++chain->mh_rejects;
	  }

	if (accept) {
	  bane *sw;
	  unsigned *nbsw;

	  complete_step(proposal, dt, chain->current_score, chain->scoreboard,
			&sinfo, ess, param_cost);

	  /* swap current - proposal */
	  sw = chain->current;
	  chain->current = proposal;
	  proposal = sw;

	  nbsw = chain->nb_size;
	  chain->nb_size = sinfo.new_nb_size;
	  sinfo.new_nb_size = nbsw;

	  chain->current_score = new_score;

	  if (new_score > max_score) {
	    max_score = new_score;
	    if (max_score > -H0) {
	      fprintf(stderr, "max_score %g > -H0 %g", max_score, -H0);
	      exit(1);
	    }
	  }
	}
      }

      if (iteration % sample_interval == 0) {
	double mh_ar, ee_ar;

	if ((iteration >= (K-k)*(B+N) + B)) {
	  if (chain->energy_rings) {
	    int I = get_ring_index(chain->current_score, H, K);
	    ring = chain->energy_rings + I;

	    ring->sample_score[ring->num_samples] = chain->current_score;
	    memcpy(ring->sample_mx + ring->num_samples * ring->struct_size,
		   chain->current->pmx->mx,
		   sizeof(unsigned) * ring->struct_size);

	    ++ring->num_samples;
	  }
	}

	mh_ar = (double)(chain->mh_accepts + chain->mh_eless)
	  /(chain->mh_accepts + chain->mh_eless + chain->mh_rejects);

	ee_ar = (double)(chain->ee_accepts + chain->ee_eless)
	  /(chain->ee_accepts + chain->ee_eless + chain->ee_rejects);

	fprintf(stderr, "[%d,%d]: %g (mh: %g",
		iteration, k, chain->current_score,
		mh_ar);
	if (chain->ee_accepts + chain->ee_eless + chain->ee_rejects)
	  fprintf(stderr, ", ar: %g", ee_ar);
	fprintf(stderr, ")\n");

	if (chain->T == 1) {
	  fprintf(stdout, "%d\t%g\n", iteration - K*(B+N),
		  chain->current_score);
	}
      }
    }
  }

  {
    int i, k;
    mcmc_chain *chain;

    chain = chainK;
    fprintf(stderr, "Chain");

    for (i = 0; i < K; ++i) {
      fprintf(stderr, "\t[%g-%g(", H[i], H[i+1]);
      chain = chain->next;
    }

    fprintf(stderr, "\t>=%g", H[K]);
    fprintf(stderr, "\n");

    for (k = 0; k <= K; ++k) {
      fprintf(stderr, "%d, T%d = %g", k, k, chain->T);

      if (chain->energy_rings) {
	for (i = 0; i <= K; ++i) {
	  fprintf(stderr, "\t%d", chain->energy_rings[i].num_samples);
	}
      }
      fprintf(stderr, "\n");
      chain = chain->prev;
    }
  }

  score_hashtable_free(sht);
}

int main(int argc, char* argv[]){
  int n;
  data* dt;
  format* fmt;
  double ess;
  FILE* fp;
  int chain_length, sample_interval;
  int K, B, N;
  double H0, HK, c, pee;

  srand48(getpid());

  if (argc != 16) {
    fprintf(stderr,
	    "Usage: %s vdfile datafile datacount ess "
	    "param_cost chain_length samples K pburnin min_ring_samples "
	    "H0 HK c pee pid_file\n", 
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
  sample_interval = chain_length / atoi(argv[7]);

  K = atoi(argv[8]);
  B = atof(argv[9]) * chain_length;
  N = atoi(argv[10]) * sample_interval;

  H0 = atof(argv[11]);
  HK = atof(argv[12]);
  c = atof(argv[13]);
  pee = atof(argv[14]);

  TRACK_OFFSPRING_COUNT = 1;

  sample(fmt, dt, ess, 10000, chain_length, sample_interval,
	 K, B, N, H0, HK, c, pee);

  format_free(fmt);
  data_free(dt);
  
  return 0;
}
