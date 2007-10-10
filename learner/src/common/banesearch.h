#ifndef BANESEARCH_H
#define BANESEARCH_H

#include <stdio.h>
#include "typedef_node.h"
#include "parent_matrix.h"
#include "score_hashtable.h"
#include "format.h"
#include "data.h"
#include "node.h"
#include "bane.h"

extern int 
bane_add_random_arc_from(bane* bn, node* from, arc* ar, int maxtblsize);

extern int
bane_del_random_arc_from(bane* bn, node* from, arc* ar);

extern int 
bane_add_random_arc(bane* bn, arc* ar, int maxtblsize);

extern int
bane_del_random_arc(bane* bn, arc* ar, int maxtblsize);

extern int
bane_rev_random_arc(bane* bn, arc* ar, int maxtblsize);

extern int
bane_repl_random_to_arc(bane* bn, arc* del_ar, arc* add_ar, int maxtblsize);

extern int
bane_repl_random_from_arc(bane* bn, arc* del_ar, arc* add_ar, int maxtblsize);

extern int
bane_repl_arc(bane *bn, arc *del_ar, arc *add_ar);

enum Operation { Add = 0, Del = 1, Rev = 2, ChangeTo = 3, ChangeFrom = 4 };

/*
 * inputs:
 *
 * [1] when ch = Add, Del, Rev:
 *   ar: arc that was added, deleted or removed
 * [2] when ch = ChangeTo, ChangeFrom:
 *   del_ar, add_ar: arcs that were removed and added
 * scoreboard: current score contributions at each node
 * sht, ess, current_best_score, param_cost
 *
 * outputs:
 *
 * ar: ar->from to correspond to from_score
 *     ar->to to correspond to to_score
 * from_score (when sht != NULL):
 *   [1] updated score contribution at ar->from
 *   [2] updated score at del_ar->to (copied to ar->from)
 * to_score (when sht != NULL):
 *   [1] updated score contribution at ar->to
 *   [2] updated score at add_ar->to (copied to ar->to)
 *
 * returns updated total score (when sht != NULL)
 */
extern double
update_score(bane *bn, enum Operation ch, arc *ar, arc *del_ar,
	     arc *add_ar, data *dt, double *scoreboard,
	     score_hashtable *sht, double *from_score,
	     double *to_score, double ess,
	     double param_cost,
	     double current_best_score);
  
#endif




