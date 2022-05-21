// Memory-Mapped Software Design

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <stdint.h>


// TODO-
// haven't define control signal yet

#define DATA_MEM 0x0100
#define BASE_A 0x0010
#define BASE_B 0x0040
#define BASE_C 0x0080


uint8_t **init_matrix(int dimX, int dimY, char *name){
  uint8_t **mat= malloc(sizeof(uint8_t *)*dimX);
  if (name)
     printf("%s = [\n",name);
  for (int i=0; i <dimX; i++){
     mat[i] = malloc(sizeof(uint8_t)*dimY);
     for (int j=0; j <dimY; j++){
        if (name) {
         mat[i][j] = (uint8_t)(rand() & 0xf);
         printf("%d ",mat[i][j]);
        }
        else
         mat[i][j] = (uint8_t)(0 & 0xFF);
     }
     if (name)
        printf("\n");
  }
  if (name)
     printf("];\n");
  return mat;
}

void mat_mul(uint8_t **A,
            uint8_t **B,
            uint8_t **C,
            int dimM,
            int dimK,
            int dimN
            ){
    for (int i=0; i< dimM; i++) {
        for (int j=0; j< dimN; j++) {
            for (int k=0; k< dimK; k++){
                C[i][j] += A[i][k]*B[k][j];
            }
        }
    }
}

void mat_write(uint8_t **A,
            uint8_t **B,
            uint8_t **C,
            int dimM,
            int dimK,
            int dimN
            ){

	FILE *f = fopen("data_mem.txt", "w");
	if ( f == NULL ) {
		printf("Error Opening File!\n");
		exit(1);
	}

	int pointer;

	for (pointer=0; pointer < DATA_MEM/4;) {
		// printf("%d\n", pointer);

		if (pointer == BASE_A/4) {
			// write matrix A into .txt file
			for (int i=0; i< dimM; i++){
				for (int k=0; k< dimK; k++) {
					fprintf(f, "%d\n", A[i][k]);
		        }
			}
			pointer+=dimM*dimK;
		}
		else if (pointer == BASE_B/4) {
			// write matrix B into .txt file
			for (int k=0; k< dimK; k++){
				for (int j=0; j< dimN; j++) {
					fprintf(f, "%d\n", B[k][j]);
		        }
			}
			pointer+=dimN*dimK;
		}
		else if (pointer == BASE_C/4) {
			// write matrix A into .txt file
			for (int i=0; i< dimM; i++){
				for (int j=0; j< dimN; j++) {
					fprintf(f, "%d\n", C[i][j]);
		        }
			}
			pointer+=dimM*dimN;
		}
		else {
			fprintf(f, "%d\n", 0);
			pointer++;
		}
	}
}


int main(int argc, char *argv[]){

	int M = 4;
	int K = 4;
	int N = 4;
	uint8_t **A;
	uint8_t **B;
	uint8_t **C;

	srand(time(NULL));   // Initialization, should only be called once.

	A = init_matrix(M, K, "A");
	B = init_matrix(K, N, "B");
	C = init_matrix(M, N, NULL);

	mat_mul(A,B,C, M, K, N);

	// print matrix C
	printf("C = [\n");
	for (int i=0; i <M; i++) {
	   for (int j=0; j <N; j++) {
	        printf("%d ",C[i][j]);
	   }
	   printf("\n");
	}
	printf("];\n");

	mat_write(A, B, C, M, K, N);
	return 0;
}