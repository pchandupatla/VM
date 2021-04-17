cd Assembler && javac Main.java && java Main data/orig.asm test && cd ../Simulator && gcc -o simulator simulator.c && simulator ../Assembler/test.bmb
