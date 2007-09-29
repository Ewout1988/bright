#ifndef DATA_H
#define DATA_H

typedef struct data data;

struct data {
  int om;
  unsigned char* d;
  int refcount;
  int m;
  int N;
  int* rowmap;
  int* colmap;
};


extern data* 
data_create(int datacount, int attcount);

extern data* 
data_new(data* old_dt);

extern data* 
data_new_cols(data* old_dt, int* sel);

extern data* 
data_new_rows_by_vals(data* old_dt, int* sel);

extern void
data_read(char* filename, data* dt);

extern void 
data_write(char* filename, data* dt);

extern void 
data_old(data* dt);

extern void 
data_free(data* d);

#define D(DT,J,I) ((DT)->d[(DT)->rowmap[J] * (DT)->om + (DT)->colmap[I]])

#define MISSINGVALUE  ((unsigned char) 255)
#define MISSING(DT,J,I) (MISSINGVALUE == D(DT,J,I))

#define DATA_DEL_ALL(DT) ((DT)->N = 0)
#define DATA_ADD(DTD,DTS,J) ((DTD)->rowmap[((DTD)->N)++] = DTS->rowmap[J])

#endif

