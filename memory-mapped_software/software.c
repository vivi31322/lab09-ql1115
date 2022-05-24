// Memory-Mapped Software Design for Lab 9
// 
// *************************************************************
// **														  **
// **     This c code write 								  **
// **     1. memory-mapped registers of the accelerator		  **
// **     2. memory-mapped buffers of the accelerator		  **
// **     to a .txt file as memory used in Chisel simulation. **
// **														  **
// *************************************************************

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <stdint.h>

// Parameter Setting
#define BYTE 8
#define DEFAULT_STRIDE 1

// total memory 64 KB
#define MEM_BYTES   0xFFFFFFFF
#define TEXT_OFFSET 0x0
#define DATA_OFFSET 0x8000

// memory address space for accelerator
#define ACCEL_REG_BASE_ADDR 0x100000
#define ACCEL_BUF_BASE_ADDR 0x200000

// Parameter setting for accelerator reg 
// (according to Lab9 document)
#define ENABLE               0x0
#define STATUS			     0x4
#define MATA_SIZE            0x8
#define MATB_SIZE            0xC
#define MATC_SIZE            0x10
#define MATA_MEM_ADDR	     0x14
#define MATB_MEM_ADDR        0x18
#define MATC_MEM_ADDR    	 0x1C
#define MAT_MEM_STRIDE	     0x20
#define MATC_GOLDEN_MEM_ADDR 0x24
#define MAT_BUF              0x20000

// set base address for data (Matrix A, B & C)
#define BASE_A        0x8000
#define BASE_B        0x8040
#define BASE_C        0x8080
#define Base_C_Golden 0x80C0


void print_matrix (uint8_t **mat, int row, int column, char* name) {
	if (name) {
		printf("%s = [\n",name);
	}
	for (int i=0; i < row; i++) {
	   for (int j=0; j < column; j++) {
	        printf("%d ",mat[i][j]);
	   }
	   printf("\n");
	}
	if (name)
     printf("];\n");
}

uint8_t **init_matrix(int dimX, int dimY, char *name){
  uint8_t **mat= malloc(sizeof(uint8_t *)*dimX);
  for (int i=0; i <dimX; i++){
     mat[i] = malloc(sizeof(uint8_t)*dimY);
     for (int j=0; j <dimY; j++){
        if (name) {
         mat[i][j] = (uint8_t)(rand() & 0xFF);
        }
        else
         mat[i][j] = (uint8_t)(0 & 0xFF);
     }
  }
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

char* toBinary(int n, int len) {
    char* binary = (char*)malloc(sizeof(char) * len);
    int k = 0;
    for (unsigned i = (1 << len - 1); i > 0; i = i / 2) {
        binary[k++] = (n & i) ? '1' : '0';
    }
    binary[k] = '\0';
    return binary;
}

void mat_write(uint8_t **A,
            uint8_t **B,
            uint8_t **C,
            int dimM,
            int dimK,
            int dimN
            ){

	FILE *f = fopen("data_mem.txt", "wb");
	if ( f == NULL ) {
		printf("Error Opening File!\n");
		exit(1);
	}

	int pointer;

	for (pointer=0; pointer < MEM_BYTES/4;) {
		//fprintf(f, "0x%04x :", pointer*4);
		switch (pointer) {
			case BASE_A/4 : 
				// write matrix A into .txt file
				for (int i=0; i< dimM; i++){
					for (int k=0; k< dimK; k++) {
						fprintf(f, "%s\n", toBinary( A[i][k], 4*BYTE));
			      	pointer++; break;
			      }
			      
				}

			case BASE_B/4 :
				// write matrix B into .txt file
				for (int k=0; k< dimK; k++){
					for (int j=0; j< dimN; j++) {
						fprintf(f, "%s\n",  toBinary( B[k][j], 4*BYTE));
			      	pointer++; break;
			      }
				}


		   // Base_C for HW design to write its output.
			// 
			// case BASE_C/4 : 
			// 	// write matrix A into .txt file
			// 	for (int i=0; i< dimM; i++){
			// 		for (int j=0; j< dimN; j++) {
			// 			fprintf(f, "%s\n", toBinary( C[i][j], 4*BYTE));
			//       }
			// 	}
			// 	pointer+=dimM*dimN; break;

			case Base_C_Golden/4 :
				// write matrix C into .txt file
				for (int i=0; i< dimM; i++){
					for (int j=0; j< dimN; j++) {
						fprintf(f, "%s\n", toBinary( C[i][j], 4*BYTE));
			      	pointer++; break;
			      }
				}

			case ACCEL_REG_BASE_ADDR + MATA_SIZE/4 :
				fprintf(f, "%s\n", toBinary( dimM*dimK, 4*BYTE));
				pointer++; break;

			case ACCEL_REG_BASE_ADDR + MATB_SIZE/4 :
				fprintf(f, "%s\n", toBinary( dimN*dimK, 4*BYTE));
				pointer++; break;

			case ACCEL_REG_BASE_ADDR + MATC_SIZE/4 :
				fprintf(f, "%s\n", toBinary( dimM*dimN, 4*BYTE));
				pointer++; break;

			case ACCEL_REG_BASE_ADDR + MATA_MEM_ADDR/4 :
				fprintf(f, "%s\n", toBinary( BASE_A, 4*BYTE));
				pointer++; break;

			case ACCEL_REG_BASE_ADDR + MATB_MEM_ADDR/4 :
				fprintf(f, "%s\n", toBinary( BASE_B, 4*BYTE));
				pointer++; break;

			case ACCEL_REG_BASE_ADDR + MATC_MEM_ADDR/4 :
				fprintf(f, "%s\n", toBinary( BASE_C, 4*BYTE));
				pointer++; break;

			case ACCEL_REG_BASE_ADDR + MAT_MEM_STRIDE/4 :
				fprintf(f, "%s%s%s%s\n", toBinary( DEFAULT_STRIDE, BYTE), toBinary( DEFAULT_STRIDE, BYTE), toBinary( DEFAULT_STRIDE, BYTE), toBinary( 0, BYTE));
				pointer++; break;

			case ACCEL_REG_BASE_ADDR + MATC_GOLDEN_MEM_ADDR/4 :
				fprintf(f, "%s\n", toBinary( Base_C_Golden, 4*BYTE));

			default :
				fprintf(f, "%s\n", toBinary( 0, 4*BYTE));
		   	pointer++;break;
		}
	}
}


int main(int argc, char *argv[]){

	int M = 3;
	int K = 2;
	int N = 3;
	uint8_t **A;
	uint8_t **B;
	uint8_t **C;

	srand(time(NULL));   // Initialization, should only be called once.

	A = init_matrix(M, K, "A");
	B = init_matrix(K, N, "B");
	C = init_matrix(M, N, NULL);

	mat_mul(A, B, C, M, K, N);

	print_matrix(A, M, K, "A") ;
	print_matrix(B, K, N, "B") ;
	print_matrix(C, M, N, "C") ;

	mat_write(A, B, C, M, K, N);

	return 0;
}