#include <stdio.h>
#include <stdlib.h>
#include "err.h"
#include "data.h"

data* data_create(int datacount, int attcount){
  data* dt;
  int i,j;

  MEMALL(dt, 1, data);
  dt->N = datacount;
  dt->om = attcount;
  dt->m = attcount;
  MEMALL(dt->d, dt->N*dt->m, char);
  MEMALL(dt->rowmap, dt->N, int);
  MEMALL(dt->colmap, dt->m, int);

  for(j=0; j<dt->N; ++j) dt->rowmap[j] = j;
  for(i=0; i<dt->m; ++i) dt->colmap[i] = i;

  return dt;
}

data* data_new(data* old_dt){
  int i,j;
  data* dt;
  MEMALL(dt, 1, data)
  dt->N = old_dt->N;
  dt->om = old_dt->om;
  dt->m = old_dt->m;
  dt->d = old_dt->d;
  MEMALL(dt->rowmap, dt->N, int)
  for(j=0; j<dt->N; ++j){
    dt->rowmap[j] = old_dt->rowmap[j];
  }
  MEMALL(dt->colmap, dt->m, int)
  for(i=0; i<dt->m; ++i){
    dt->colmap[i] = old_dt->colmap[i];
  }

  return dt;
}

data* data_new_cols(data* old_dt, int* sel){
  int i,j,c;
  data* dt;
  MEMALL(dt, 1, data)
  dt->N = old_dt->N;  
  dt->om = old_dt->om;
  dt->m = old_dt->m;
  dt->d = old_dt->d;
  MEMALL(dt->rowmap, dt->N, int)
  for(j=0; j<dt->N; ++j){
    dt->rowmap[j] = old_dt->rowmap[j];
  }
  MEMALL(dt->colmap, dt->m, int)
  c=0;
  for(i=0; i<dt->m; ++i){
    if(sel[i]) dt->colmap[c++] = old_dt->colmap[i];
  }
  dt->m = c;

  return dt;
}

data* data_new_rows_by_vals(data* old_dt, int* sel){
  int i,j,r;
  data* dt;
  MEMALL(dt, 1, data)
  dt->N = old_dt->N;  
  dt->om = old_dt->om;
  dt->m = old_dt->m;
  dt->d = old_dt->d;

  MEMALL(dt->colmap, dt->m, int);

  for(i=0; i<dt->m; ++i){
    dt->colmap[i] = old_dt->colmap[i];
  }

  MEMALL(dt->rowmap, dt->N, int);

  r = 0;
  for(j=0; j<dt->N; ++j){
    int match = 1;
    for(i=0; i<dt->m; ++i){
      match &= ((sel[i] == -1) || (D(old_dt,j,i) == sel[i]));
    }
    if(match) dt->rowmap[r++] = old_dt->rowmap[j];
  }
  
  dt->N = r;

  return dt;
}

void data_read(char* filename, data* dt){
  FILE* fp;  
  int j;

  OPENFILE_OR_DIE(fp,filename,"r");

  for(j=0; j<dt->N; ++j){
    int i;
    dt->rowmap[j] = j;
    for(i=0; i<dt->m; ++i){
      int tmp;
      int e = fscanf(fp, "%d", &tmp);
      if(e != 1){
	fprintf(stderr, "error while reading %s, %d, %d, %d\n", filename, e, i, j);
	exit(ERROR_IN_DATAREAD);	
      }
      dt->colmap[i] = i;
      D(dt,j,i) = (char) tmp;
    }
  }

  CLOSEFILE_OR_DIE(fp,filename);
}

void data_write(char* filename, data* dt){
  FILE* fp;  
  int j;

  OPENFILE_OR_DIE(fp,filename,"w");

  for(j=0; j<dt->N; ++j){
    int i;
    for(i=0; i<dt->m; ++i){
      fprintf(fp,"%d%c", D(dt,j,i), (i==dt->m-1) ? '\n' : '\t');
    }
  }

  CLOSEFILE_OR_DIE(fp,filename);
}

void data_old(data* dt){
  free (dt->rowmap);
  free (dt->colmap);
  free (dt);
}

void data_free(data* dt){
  free (dt->d);
  free (dt->rowmap);
  free (dt->colmap);
  free (dt);
}












