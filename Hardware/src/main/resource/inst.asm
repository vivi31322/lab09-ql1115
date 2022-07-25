j <reset_vector>
li ra,0
li sp,0
li gp,0
li tp,0
li t0,0
li t1,0
li t2,0
li s0,0
li s1,0
li a0,0
li a1,0
li a2,0
li a3,0
li a4,0
li a5,0
li a6,0
li a7,0
li s2,0
li s3,0
li s4,0
li s5,0
li s6,0
li s7,0
li s8,0
li s9,0
li s10,0
li s11,0
li t3,0
li t4,0
li t5,0
li t6,0
li gp,0
li a0,1
slli a0,a0,0x1f
bltz a0,<test2>
nop 
li gp,1
li a7,93
li a0,0
li gp,2
li t1,2
nop 
nop 
nop 
nop 
nop 
li t2,2
add t3,t2,t1
addi t3,t3,-4
bnez t3,<fail>
li gp,3
li t1,3
nop 
nop 
nop 
nop 
nop 
li t2,3
add t3,t1,t2
addi t3,t3,-6
bnez t3,<fail>
li gp,4
li t1,4
nop 
nop 
nop 
nop 
nop 
li t2,4
nop 
add t3,t2,t1
addi t3,t3,-8
bnez t3,<fail>
li gp,5
li t1,5
nop 
nop 
nop 
nop 
nop 
li t2,5
nop 
add t3,t1,t2
addi t3,t3,-10
bnez t3,<fail>
li gp,6
li t1,6
nop 
nop 
nop 
nop 
nop 
li t2,6
nop 
nop 
add t3,t2,t1
addi t3,t3,-12
bnez t3,<fail>
li gp,7
li t1,7
nop 
nop 
nop 
nop 
nop 
li t2,7
nop 
nop 
add t3,t1,t2
addi t3,t3,-14
bnez t3,<fail>
li gp,8
auipc t1,0x8
addi t1,t1,-452
li t2,255
nop 
nop 
nop 
nop 
nop 
lbu t3,0(t1)
sub t3,t3,t2
bnez t3,<fail>
li gp,9
auipc t1,0x8
addi t1,t1,-498
li t2,15
nop 
nop 
nop 
nop 
nop 
lbu t3,0(t1)
sub t3,t2,t3
bnez t3,<fail>
li gp,10
li t3,1
auipc t1,0x8
addi t1,t1,-552
lb t2,0(t1)
add t3,t2,t3
add t3,t3,t2
sub t3,t2,t3
bnez t3,<fail>
li gp,11
auipc t3,0x8
addi t3,t3,-580
auipc t1,0x8
addi t1,t1,-592
sw t1,0(t3)
add t2,zero,t3
lw t2,0(t2)
lb t2,0(t2)
lb t1,0(t1)
bne t2,t1,<fail>
bne zero,gp,<pass>
nop 
beqz gp,<fail+0x4>
slli gp,gp,0x1
ori gp,gp,1
li a7,93
mv a0,gp
j <pass_fail_end>
nop 
li gp,1
li a7,93
li a0,0
nop 
nop 
nop 
nop 
nop 
hcf
