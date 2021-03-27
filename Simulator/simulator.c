#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include "register.h"


#define INSTR_LEN 4
#define MEM_SIZE 65536
static size_t MAX_CHARS = 16;
static char prompt[] = "bmb> ";
static const char * registers[REG_NUM] = {"ANSHL", "PRNV", "MARIE", "ALVIN", "SPNCR", "LOWE", "ERIN", "ETHAN", "ABERY", "JON", "PAJAK",
                                          "ERALP", "BAMB", "RET", "SP", "PC"};

void init(char *argv[]);
void run_command(void);
void error(const char *);
void ldump(void);
void mdump(char * cmd);
void exec_instr(void);
void run(char * cmd);
void go(void);
int get_bits(int start, int end, uint32_t data);
void add(int instr);
void sub(int instr);
void mul(int instr);

int start_found = 0;

uint8_t MEMORY[MEM_SIZE];

typedef struct latches{
  int32_t IR;
  uint8_t N;
  uint8_t Z;
  uint8_t P;
  int32_t REGS[REG_NUM];
}latches_t;

latches_t CURRENT_LATCHES, NEXT_LATCHES;

int main(int argc, char *argv[])
{
  if(argc < 2)
  {
    error("Must supply a file to simulate");
  }
  init(argv);
}

void init(char *argv[])
{
  CURRENT_LATCHES.Z = 1;
  
  uint32_t orig_address = 0;
  uint8_t buffer[INSTR_LEN];
  FILE *ptr;
  ptr = fopen(argv[1], "rb");
  if(ptr == NULL)
  {
    error("Cannot find specified file");
  }

  fread(buffer, sizeof(buffer), 1, ptr);
  for(int i = 0; i < INSTR_LEN; i++)
  {
    orig_address = orig_address << 8;
    orig_address += buffer[i];
  }
  // printf("%x\n", orig_address);
  CURRENT_LATCHES.REGS[PC_NUM] = orig_address;

  while(fread(buffer, sizeof(buffer), 1, ptr) == 1)
  {
    //printf("0x");
    for(int i = 0; i < INSTR_LEN; i++)
    {
      //printf("%.2x", buffer[i]);
      MEMORY[orig_address++] = buffer[i];
    }
    //printf("\n");
  }

  orig_address = CURRENT_LATCHES.REGS[PC_NUM];
  CURRENT_LATCHES.IR = (MEMORY[orig_address] << 24) + (MEMORY[orig_address + 1] << 16) + (MEMORY[orig_address + 2] << 8) + MEMORY[orig_address + 3];
  CURRENT_LATCHES.REGS[SP_NUM] = MEM_SIZE - 1;

  //debug code
  for(int i = 0; i < 28; i++)
  {
    printf("%.2x", MEMORY[orig_address++]);
    if(orig_address % 4 == 0)
    {
      printf("\n");
    }
  }

  run_command();
}

void run_command(void)
{
  char * cmd = NULL;
  printf("%s", prompt);
  if(getline(&cmd, &MAX_CHARS, stdin) == -1)
  {
    free(cmd);
    error("Unable to read from stdin\n");
  }

  //ldump -> look at current state
  //mdump <A> <B> -> look at memory from A to B
  //run <#> -> run # of instructions
  //go -> run until halt
  switch (cmd[0]) 
  {
    case 'l':
      ldump();
      break;
    case 'm':
      mdump(cmd);
      break;
    case 'r':
      run(cmd);
      break;
    case 'g':
      go();
      break;
    default:
      printf("No command by that name (or even starting letter), dumbass\n");
      break;
  }
  
  free(cmd);
}

void ldump(void)
{  
  printf("IR: 0x%x\n", CURRENT_LATCHES.IR);
  printf("N: 0x%x\n", CURRENT_LATCHES.N);
  printf("Z: 0x%x\n", CURRENT_LATCHES.Z);
  printf("P: 0x%x\n", CURRENT_LATCHES.P);

  for (int i = ANSHL; i < REG_NUM; i++)
  {
    printf("Register <%s>: 0x%x\n", registers[i], CURRENT_LATCHES.REGS[i]);
  }
}

void mdump(char *cmd)
{
  strtok(cmd, " ");
  char *start = strtok(NULL, " ");
  if(start == NULL)
  {
    error("Must specify beginning and end memory address\n");
  }
  char * ptr;
  int start_addr = (int)strtol(start, &ptr, 16);

  char *end = strtok(NULL, " ");
  if(end == NULL)
  {
    error("Must specify beginning and end memory address\n");
  }
  char * temp;
  int end_addr = (int)strtol(end, &temp, 16);

  //printf("%x %x\n", start_addr, end_addr);
  for(int i = start_addr; i <= end_addr; i++)
  {
    printf("Address <0x%x>: 0x%.2x\n", i, MEMORY[i]);
  }
}

void run(char *cmd)
{
  strtok(cmd, " ");
  char *num_s = strtok(NULL, " ");

  if(num_s == NULL){
    error("Must specify number of runs\n");
  }

  int num = atoi(num_s);

  for(int i = 0; i < num; i++)
  {
    // do stuff 
    NEXT_LATCHES = CURRENT_LATCHES;
    exec_instr();
    CURRENT_LATCHES = NEXT_LATCHES;

    // debug:
    ldump();
  }
}

void exec_instr(void)
{
  int pc = CURRENT_LATCHES.REGS[PC_NUM];
  CURRENT_LATCHES.IR = (MEMORY[pc] << 24) + (MEMORY[pc + 1] << 16) + (MEMORY[pc + 2] << 8) + MEMORY[pc + 3];
  NEXT_LATCHES.REGS[PC_NUM] = pc + 4;
  uint32_t instruction = CURRENT_LATCHES.IR;
  uint32_t opcode = get_bits(27, 31, instruction);
  //printf("INSTR: 0x%x\n", instruction);
  //printf("OPCODE: 0x%x\n", opcode);

  switch(opcode)
  {
    case 0:
      add(instruction);
      break;
    case 1:
      sub(instruction);
      break;
    case 2:
      mul(instruction);
      break;
  }
}

void setcc(int value){
  NEXT_LATCHES.N = 0;
  NEXT_LATCHES.Z = 0;
  NEXT_LATCHES.P = 0;
  
  if(value > 0)
  {
    NEXT_LATCHES.P = 1;
  }
  else if(value == 0)
  {
    NEXT_LATCHES.Z = 1;
  }
  else
  {
    NEXT_LATCHES.N = 1;
  }
}

void add(int instr)
{
  int regA = get_bits(23, 26, instr);
  int regB = get_bits(19, 22, instr);

  int imm;
  if(get_bits(18, 18, instr) == 1)
  {
    imm = get_bits(0, 15, instr);
  }
  else
  {
    imm = CURRENT_LATCHES.REGS[get_bits(0, 3, instr)]; 
  }

  NEXT_LATCHES.REGS[regA] = CURRENT_LATCHES.REGS[regB] + imm;
  setcc(NEXT_LATCHES.REGS[regA]);
}


void sub(int instr)
{
  int regA = get_bits(23, 26, instr);
  int regB = get_bits(19, 22, instr);

  int imm;
  if(get_bits(18, 18, instr) == 1)
  {
    imm = get_bits(0, 15, instr);
  }
  else
  {
    imm = CURRENT_LATCHES.REGS[get_bits(0, 3, instr)]; 
  }

  NEXT_LATCHES.REGS[regA] = CURRENT_LATCHES.REGS[regB] - imm;
  setcc(NEXT_LATCHES.REGS[regA]);
}

void mul(int instr)
{
  int regA = get_bits(23, 26, instr);
  int regB = get_bits(19, 22, instr);

  int imm;
  if(get_bits(18, 18, instr) == 1)
  {
    imm = get_bits(0, 15, instr);
  }
  else
  {
    imm = CURRENT_LATCHES.REGS[get_bits(0, 3, instr)]; 
  }

  NEXT_LATCHES.REGS[regA] = CURRENT_LATCHES.REGS[regB] * imm;
  setcc(NEXT_LATCHES.REGS[regA]);
}

int get_bits(int start, int end, uint32_t data)
{
  int value = 0;
  int mask = 0x01;

  for(int i = 0; i <= end; i++)
  {
    if(i >= start)
    {
      value += (data & mask);
    }

    mask = mask << 1;
  }

  return value >> start;
}

void go(void)
{
  while(CURRENT_LATCHES.REGS[PC_NUM] != 0)
  {
    run("r 1");
  }
}

void error(const char * error_string)
{
  printf("ERROR: %s\n", error_string);
  exit(-1);
}
