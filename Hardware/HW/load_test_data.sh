if [ -n "$1" ]
then
    if [ "$1" = "Test1" ]
    then
        cp ./src/main/resource/Test1_inst.asm ./src/main/resource/inst.asm
        cp ./src/main/resource/Test1_m_code.hex ./src/main/resource/m_code.hex
	    cp ./src/main/resource/data_empty.hex ./src/main/resource/data.hex
    elif [ "$1" = "Test2" ]
    then
        cp ./src/main/resource/Test2_inst.asm ./src/main/resource/inst.asm
        cp ./src/main/resource/Test2_m_code.hex ./src/main/resource/m_code.hex
	    cp ./src/main/resource/data_empty.hex ./src/main/resource/data.hex
    elif [ "$1" = "Test3" ]
    then
        cp ./src/main/resource/Test3_inst.asm ./src/main/resource/inst.asm
        cp ./src/main/resource/Test3_m_code.hex ./src/main/resource/m_code.hex
	    cp ./src/main/resource/data_empty.hex ./src/main/resource/data.hex
    elif [ "$1" = "Vector" ]
    then
        cp ./src/main/resource/Vector_inst.asm ./src/main/resource/inst.asm
        cp ./src/main/resource/Vector_m_code.hex ./src/main/resource/m_code.hex
	    cp ./src/main/resource/Vector_data.hex ./src/main/resource/data.hex
    else
        cp ../../riscv-test/out/asm/rv32ui-$1.asm ./src/main/resource/inst.asm
        cp ../../riscv-test/out/hex/text/rv32ui-$1.hex ./src/main/resource/m_code.hex
	    cp ../../riscv-test/out/hex/data/rv32ui-$1.hex ./src/main/resource/data.hex
    fi
else
    echo "[Error] usage should be: ./test_data.sh <which Test program> (Test1/Test2/Test3/Vector/[inst code])"
fi