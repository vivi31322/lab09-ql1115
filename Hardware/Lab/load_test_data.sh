if [ -n "$1" ]
then
    if [ "$1" = "Test1" ]
    then
        cp ./src/main/resource/Test1_inst.asm ./src/main/resource/inst.asm
        cp ./src/main/resource/Test1_inst.hex ./src/main/resource/inst.hex
	cp ./src/main/resource/data_empty.hex ./src/main/resource/data.hex
    elif [ "$1" = "Test2" ]
    then
        cp ./src/main/resource/Test2_inst.asm ./src/main/resource/inst.asm
        cp ./src/main/resource/Test2_inst.hex ./src/main/resource/inst.hex
	cp ./src/main/resource/data_empty.hex ./src/main/resource/data.hex
    elif [ "$1" = "Test3" ]
    then
        cp ./src/main/resource/Test3_inst.asm ./src/main/resource/inst.asm
        cp ./src/main/resource/Test3_inst.hex ./src/main/resource/inst.hex
	cp ./src/main/resource/data_empty.hex ./src/main/resource/data.hex
    elif [ "$1" = "Emulator" ]
    then
        cp ../../Emulator/inst.asm ./src/main/resource/inst.asm
        cp ../../Emulator/inst.hex ./src/main/resource/inst.hex
	    cp ../../Emulator/data.hex ./src/main/resource/data.hex
    elif [ "$1" = "-s" ]
    then
        cp ../../riscv-test/out/asm/rv32ui_SingleTest-$2.asm ./src/main/resource/inst.asm
        cp ../../riscv-test/out/hex/text/rv32ui_SingleTest-$2.hex ./src/main/resource/inst.hex
        cp ../../riscv-test/out/hex/data/rv32ui_SingleTest-$2.hex ./src/main/resource/data.hex
    else
        cp ../../riscv-test/out/asm/rv32ui_FullTest-$1.asm ./src/main/resource/inst.asm
        cp ../../riscv-test/out/hex/text/rv32ui_FullTest-$1.hex ./src/main/resource/inst.hex
        cp ../../riscv-test/out/hex/data/rv32ui_FullTest-$1.hex ./src/main/resource/data.hex
    fi
else
    echo "[Error] usage should be: ./test_data.sh <which Test program> (Test1/Test2/Test3/Emulator/[inst code])"
fi
