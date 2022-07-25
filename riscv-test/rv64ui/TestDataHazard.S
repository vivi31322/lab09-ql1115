# See LICENSE for license details.

#*****************************************************************************
# TestDataHazard.S
#-----------------------------------------------------------------------------
#
# Test Data Hazard situation.
#

#include "riscv_test.h"
#include "test_macros.h"

RVTEST_RV64U
RVTEST_CODE_BEGIN

  #-------------------------------------------------------------
  # EXE rs - MEM rd Data Hazard tests - Data forwarding
  #-------------------------------------------------------------

## rs1 - rd
test2:
  li TESTNUM, 2
  li t1, 2
  TEST_INSERT_NOPS_5
  li t2, 2
  add t3,t2,t1
  addi t3,t3,-4
  bne  t3,x0, fail

## rs2 - rd
test3:
  li TESTNUM, 3
  li t1, 3
  TEST_INSERT_NOPS_5
  li t2, 3
  add t3,t1,t2
  addi t3,t3,-6
  bne  t3,x0, fail

  #-------------------------------------------------------------
  # EXE rs - WB rd Data Hazard tests - Data forwarding
  #-------------------------------------------------------------
## rs1 - rd
test4:
  li TESTNUM, 4
  li t1, 4
  TEST_INSERT_NOPS_5
  li t2, 4
  TEST_INSERT_NOPS_1
  add t3,t2,t1
  addi t3,t3,-8
  bne  t3,x0, fail

## rs2 - rd
test5:
  li TESTNUM, 5
  li t1, 5
  TEST_INSERT_NOPS_5
  li t2, 5
  TEST_INSERT_NOPS_1
  add t3,t1,t2
  addi t3,t3,-10
  bne  t3,x0, fail
  
  #-------------------------------------------------------------
  # ID rs - WB rd Data Hazard tests - Data forwarding
  #-------------------------------------------------------------
## rs1 - rd
test6:
  li TESTNUM, 6
  li t1, 6
  TEST_INSERT_NOPS_5
  li t2, 6
  TEST_INSERT_NOPS_2
  add t3,t2,t1
  addi t3,t3,-12
  bne  t3,x0, fail

## rs2 - rd
test7:
  li TESTNUM, 7
  li t1, 7
  TEST_INSERT_NOPS_5
  li t2, 7
  TEST_INSERT_NOPS_2
  add t3,t1,t2
  addi t3,t3,-14
  bne  t3,x0, fail

  #-------------------------------------------------------------
  # EXE rs - MEM rd (Memory Access) Data Hazard tests - Stall
  #-------------------------------------------------------------
## rs1 - rd
test8:
  li TESTNUM, 8
  la t1, tdata1
  li t2, 255
  TEST_INSERT_NOPS_5
  lbu t3, 0(t1)
  sub t3,t3,t2
  bne t3,x0, fail

## rs2 - rd
test9:
  li TESTNUM, 9
  la t1, tdata3
  li t2, 15
  TEST_INSERT_NOPS_5
  lbu t3, 0(t1)
  sub t3,t2,t3
  bne  t3,x0, fail

  #-------------------------------------------------------------
  # All Test
  #-------------------------------------------------------------

test10:
  li TESTNUM, 10
  li t3 ,1
  la t1, tdata1
  lb t2, 0(t1)
  add t3,t2,t3
  add t3,t3,t2
  sub t3,t2,t3
  bne  t3,x0, fail

test11:
  li TESTNUM, 11
  la t3, tdata5
  la t1, tdata1
  sw t1, 0(t3)
  add t2,x0,t3
  lw t2, 0(t2)
  lb t2,0(t2)
  lb t1,0(t1)
  bne t2,t1,fail

  TEST_PASSFAIL

RVTEST_CODE_END

  .data
RVTEST_DATA_BEGIN

  TEST_DATA

tdata1: .byte 0xff
tdata2: .byte 0xf0
tdata3: .byte 0x0f
tdata4: .byte 0x00
tdata5: .word 0x00000000

RVTEST_DATA_END
