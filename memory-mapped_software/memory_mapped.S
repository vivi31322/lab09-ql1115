## Lab 9-3-4 Software Design
## Testing assembly code to check whether CPU works correctly in Lab9-3

.data
## Martix A, B, C data as usual
A:
.byte 0 1 2 3 4 5

B:
.byte 0 1 2 3 4 5

C:
.byte 0 0 0 0 0 0 0 0 0

## define parameter for memory space which accelerator can access
ACCEL_REG_BASE_ADDR:
.word 0x100000

ACCEL_BUF_BASE_ADDR:
.word 0x200000

REG_ROFFSET_ENABLE:
.word 0x0

REG_ROFFSET_STATUS:
.word 0x4

REG_OFFSET_MATA_MEM_ADDR:
.word 0x10

REG_OFFSET_MATB_MEM_ADDR:
.word 0x14

REG_OFFSET_MATC_MEM_ADDR:
.word 0x1c

REG_OFFSET_MAT_MEM_STRIDE:
.word 0x20


## inst start
.text
la s0,A  ## s0 -> base address of A
la s1,B  ## s1 -> base address of B
la s2,C  ## s2 -> base address of C

li s3,3  ## s3 -> dimM
li s4,2  ## s4 -> dimK
li s5,3  ## s5 -> dimN

######################
##                  ##
##      Step 1      ##
##                  ##
######################
## t0 for addrA which accelerator can access in SRAM
la t6,ACCEL_BUF_BASE_ADDR       ## addrA = ACCEL_BUF_BASE_ADDR + 0
lw t0,0(t6)

## t1 for addrB which accelerator can access in SRAM
mul t3,s3,s4 					## calculate the size of matrix A
add t1,t0,t3 					## addrB = ACCEL_BUF_BASE_ADDR + M*K

## t2 for addrC which accelerator can access in SRAM
mul t3,s4,s5 					## calculate the size of matrix B
add t2,t1,t3					## addrC = ACCEL_BUF_BASE_ADDR + M*K + K*N

## copy Matrix A to addrA 
mul t3,s3,s4                    ## t3 for size of Matrix A = 3*2
li t4,0                         ## t4 for offset
loop_A:
add t5,s0,t4                    
lb  t6,0(t5)
add t5,t0,t4
sb  t6,0(t5)
addi t4,t4,1
blt t4,t3,loop_A

## copy Matrix B to addrB 
mul t3,s4,s5                    ## t3 for size of Matrix B = 2*3
li t4,0                         ## t4 for offset
loop_B:
add t5,s1,t4                    
lb  t6,0(t5)
add t5,t1,t4
sb  t6,0(t5)
addi t4,t4,1
blt t4,t3,loop_B

######################
##                  ##
##      Step 2      ##
##                  ##
######################
## After doing this, we move the original data in SRAM which "accelerator cannot access" to the memory space "accelerator can access".
## And the next thing we need to do is to write the information into ACCEL_REG to make sure that accelerator can work with correct spec.

## t3 for REG_BASE_ADDR
la t6,ACCEL_REG_BASE_ADDR
lw t3,0(t6)

la t6,REG_OFFSET_MATA_MEM_ADDR       ## t4 for REGADDR_MATA_MEM_ADDR (the register for accelerator)
lw t4,0(t6)
add t4,t3,t4
sw t0,0(t4)                          ## put addrA into REGADDR_MATA_MEM_ADDR

la t6,REG_OFFSET_MATB_MEM_ADDR       ## t4 for REGADDR_MATB_MEM_ADDR (the register for accelerator)
lw t4,0(t6)
add t4,t3,t4
sw t1,0(t4)                          ## put addrB into REGADDR_MATB_MEM_ADDR

la t6,REG_OFFSET_MATC_MEM_ADDR       ## t4 for REGADDR_MATC_MEM_ADDR (the register for accelerator)
lw t4,0(t6)
add t4,t3,t4
sw t2,0(t4)                          ## put addrC into REGADDR_MATC_MEM_ADDR

######################
##                  ##
##      Step 3      ##
##                  ##
######################
## information ready, pull up the ENABLE signal 
la t6,REG_ROFFSET_ENABLE             ## t4 for REG_ROFFSET_ENABLE
lw t4,0(t6)   
add t4,t3,t4
li t5,1
sw t5,0(t4)                          ## set ENABLE

## ... accelerator processing ...

## CPU hanging til the accelerator pull up the DONE signal
hang:
la t6,REG_ROFFSET_STATUS             ## t4 for REG_ROFFSET_STATUS
lw t4,0(t6)  
add t4,t3,t4
lw t5,0(t4)                          ## load STATUS, check "DONE" signal 
li t6,1


######################
##                  ##
##      Step 4      ##
##                  ##
######################
## CPU waiting for STATUS.Done signal.
## In software simulation, we will use emulator.cpp to write STATUS.Done signal automatically after detecting ENABLE signal.
## Notice that 

beq t5,t6,FINISH                     ## If (t5 == t6) means it's done!
j hang


######################
##                  ##
##      Step 5      ##
##                  ##
######################
FINISH:
la t4,ACCEL_BUF_BASE_ADDR
lw t4,0(t4)
vle8_v v1,0(t4)                     ## load data from ACCEL_BUF_BASE_ADDR to chekout whether the data transfer is correct
vle8_v v2,8(t4)						## ..
vle8_v v3,0(t3)                     ## load data from ACCEL_REG_BASE_ADDR to checkout the control signal is correct
hcf



