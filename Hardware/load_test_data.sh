if [ -n "$1" ]
then
    if [ "$1" = "Lab0" ]
    then
        cp ./src/main/resource/Lab0_inst.asm ./src/main/resource/inst.asm
        cp ./src/main/resource/Lab0_m_code.hex ./src/main/resource/m_code.hex
	cp ./src/main/resource/data_empty.hex ./src/main/resource/data.hex
    elif [ "$1" = "Lab1" ]
    then
        cp ./src/main/resource/Lab1_inst.asm ./src/main/resource/inst.asm
        cp ./src/main/resource/Lab1_m_code.hex ./src/main/resource/m_code.hex
	cp ./src/main/resource/data_empty.hex ./src/main/resource/data.hex
    elif [ "$1" = "Hw1" ]
    then
        cp ./src/main/resource/Hw1_inst.asm ./src/main/resource/inst.asm
        cp ./src/main/resource/Hw1_m_code.hex ./src/main/resource/m_code.hex
	cp ./src/main/resource/data_empty.hex ./src/main/resource/data.hex
    else
        cp ../riscv-test/out/asm/rv32ui-$1.asm ./src/main/resource/inst.asm
        cp ../riscv-test/out/hex/text/rv32ui-$1.hex ./src/main/resource/m_code.hex
	cp ../riscv-test/out/hex/data/rv32ui-$1.hex ./src/main/resource/data.hex
    fi
else
    echo "[Error] usage should be: ./test_data.sh <which Lab>"
fi