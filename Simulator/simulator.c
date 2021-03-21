#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include "register.h"

#define INSTR_LEN 4
#define MEM_SIZE 65536

void init(char *argv[]);
void error(const char *);

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
  for(int i = 0; i < 24; i++)
  {
    printf("%.2x", MEMORY[orig_address++]);
    if(orig_address % 4 == 0)
    {
      printf("\n");
    }
  }
}

void error(const char * error_string)
{
  printf("ERROR: %s\n", error_string);
  exit(-1);
}