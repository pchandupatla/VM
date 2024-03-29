import java.util.*;
import java.io.*;

public class Main
{
  // private static HashMap<String, String> registerMap = new HashMap<>();
  // NOT OPCODE, Doesn't start with a dot
  //TODO: fix OPCODE_ARRAY opcodes
  private static final List<String> OPCODE_ARRAY = Arrays.asList(new String[]{"ADD", "SUB", "MUL", "AND", "NOT", "XOR", "LOADB", "LOADDB", 
                                                                            "LOADQB", "STOREB", "STOREDB", "STOREQB", "MOV", "LEAP", "TRAP", "RSCOOTA", 
                                                                            "RSCOOTL", "LSCOOT", "POP", "PUSH", "HARKBACK", "DSR", "CLEAP", "CLEAPN", "CLEAPZ", "CLEAPP", "CLEAPNZ", "CLEAPNP", "CLEAPZP", "CLEAPNZP"});
  private static final List<String> REGISTER_ARRAY = Arrays.asList(new String[]{"ANSHL", "PRNV", "MARIE", "ALVIN", "SPNCR", "LOWE", "ERIN", "ETHAN", "ABERY", "JON", 
                                                                                "PAJAK", "ERALP", "BAMB", "RET", "SP", "PC"});                                                                            
  private static HashMap<String, Integer> symbolTable = new HashMap<>();
  private static HashMap<String, Integer> opCodeTable = new HashMap<>();
  private static final int ADDRESS_OFFSET = 4;
  private static int beginningAddress; 
  private static String ORIG_STRING = ".ORIG";
  private static String END_STRING = ".END";
  private static String COMMENT_STRING = ";";
  private static String FILE_EXTENSION = ".bmb";
  private static String binaryFile;
  private static final int BITS_PER_OPCODE = 5;
  private static final int BITS_PER_REG = 4;
  private static final int BITS_PER_IMM = 16;
  private static final int BITS_PER_MEM = 32;
  
  public static void main(String[] args) throws IOException
  {
    setup();
    if(args.length == 0)
    {
      throw new IllegalArgumentException("YOU DONE MESSED UP BOY/GIRL!");
    }
    
    if(args.length == 2)
    {
      binaryFile = args[1] + FILE_EXTENSION;
    }
    else
    {
      binaryFile = args[0].split("\\.")[0] + FILE_EXTENSION;
    }

    File dataFile = new File(args[0]);
    Scanner file = new Scanner(dataFile);
    boolean origFound = false;
    while(!origFound)
    {
      if(file.hasNextLine())
      {
        String origAddress = file.nextLine();
        String[] tokens = origAddress.split(" ");
        if(tokens[0].equals(ORIG_STRING))
        {
          origFound = true;
          if(isHex(tokens[1]))
          {
            try
            {
              beginningAddress = Integer.parseInt(tokens[1].substring(2), 16);
            }
            catch(NumberFormatException nfe)
            {
              throw new AssemblerException("Invalid .ORIG address");
            }
          }
          else
          {
            throw new AssemblerException("Invalid .ORIG address");
          }
        }
      }
      else
      {
        throw new AssemblerException("Syntax error, no .ORIG op found");
      }
    }

    firstPass(file);
    file.close();

    file = new Scanner(dataFile);
    secondPass(file);
    file.close();

    AssemblyLine.outputStream.flush();
    AssemblyLine.outputStream.close();
  }

  private static void setup() throws IOException
  {
    Scanner kb = new Scanner(new File("data/opcode.txt"));
    while(kb.hasNextLine())
    {
      String unparsed = kb.nextLine();
      String[] tokens = unparsed.split(" ");
      opCodeTable.put(tokens[0], Integer.parseInt(tokens[1]));
    }
  }

  private static void firstPass(Scanner file)
  {
    int offset = 0; //points to the address of the next current instruction
    // System.out.println(OPCODE_ARRAY.length);

    boolean end = false;
    while(file.hasNextLine() && !end)
    {
      String currLine = file.nextLine();
      if(currLine.toUpperCase().equals(END_STRING))
      {
        end = true;
      }

      int semiColIndex = currLine.indexOf(COMMENT_STRING);
      if(semiColIndex != -1)
      {
        currLine = currLine.substring(0, semiColIndex);
      }
      
      if(currLine.length() == 0)
      {
        continue;
      }

      // System.out.printf("%s - %x\n", currLine, beginningAddress + offset);
      offset = firstPassParse(currLine, offset);
    }

    // System.out.println(symbolTable.toString());
  }


  private static void secondPass(Scanner file) throws IOException
  {
    int offset = 0;
    boolean end = false;
    while(file.hasNextLine() && !end)
    {
      String currLine = file.nextLine();
      
      if(currLine.equalsIgnoreCase(END_STRING))
      {
        end = true;
      }

      int semiColIndex = currLine.indexOf(COMMENT_STRING);
      if(semiColIndex != -1)
      {
        currLine = currLine.substring(0, semiColIndex);
      }
      
      if(currLine.length() == 0)
      {
        continue;
      }

      offset = secondPassParse(currLine, offset);
    }
  }

  // commas, tabs, spaces
  //Returns 
  private static int firstPassParse(String line, int offset)
  {
    String[] tokens = line.split("[\\s,]+");

    if(tokens.length <= 0)
    {
      return offset;
    }
    
    if(!isOpcode(tokens[0]) && !tokens[0].startsWith("."))
    {
      int address = beginningAddress + offset;
      
      if(isHex(tokens[0]))
      {
        throw new AssemblerException("Invalid label on line: " + line);
      }
      else
      {
        symbolTable.put(tokens[0], address);
      }
    }

    return offset + ADDRESS_OFFSET;
  }

  private static int secondPassParse(String line, int offset) throws IOException
  {
    String[] tokens = line.split("[\\s,]+");

    if(tokens.length <= 0 || tokens[0].toUpperCase().equals(".END") || tokens[0].toUpperCase().equals(ORIG_STRING))
    {
      return offset;
    }

    //first token is label
    if(!isOpcode(tokens[0].toUpperCase()))
    {
      tokens = Arrays.copyOfRange(tokens, 1, tokens.length);
    }
    
    AssemblyLine.opcodeCheck(tokens, offset + ADDRESS_OFFSET);
    
    return offset + ADDRESS_OFFSET;
  }

  private static boolean isHex(String line)
  {
    String hexIdentifier = "0x";
    if(line.length() < 3)
    {
      return false;
    }

    if(line.charAt(0) == '-')
    {
      return line.substring(1, 3).equalsIgnoreCase(hexIdentifier);
    }

    return line.substring(0, 2).equalsIgnoreCase(hexIdentifier);
  }

  private static int hexToImm(String line)
  {
    if(line.charAt(0) == '-')
    {
      return -1 * Integer.parseInt(line.substring(3), 16);
    }
    return Integer.parseInt(line.substring(2), 16);
  }

  private static boolean isOpcode(String opcode)
  {
    return OPCODE_ARRAY.contains(opcode);
  }

  private static int registerIndex(String register)
  {
    return REGISTER_ARRAY.indexOf(register);
  }

  private static class AssemblyLine
  {
    private static boolean initialize = false;
    private static BitOutputStream outputStream;

    private static void opcodeCheck(String[] tokens, int offset) throws IOException
    {
      if(!initialize)
      {
        initialize = true;
        FileWriter writer = new FileWriter(binaryFile, false);
        writer.close();
        outputStream = new BitOutputStream(binaryFile);
        outputStream.writeBits(BITS_PER_MEM, beginningAddress);
      }

      // System.out.println(Arrays.toString(tokens));
      
      String opcode = tokens[0].toUpperCase();
      if(!isOpcode(opcode))
      {
        throw new AssemblerException("Opcode: " + tokens[0] + " not recognized");
      }

      switch(opcode)
      {
        case "ADD": // 0
          add(tokens);
          break;
        case "SUB": // 1
          sub(tokens);
          break;
        case "MUL": // 2
          mul(tokens);
          break;
        case "AND": // 3
          and(tokens);
          break;
        case "NOT": // 5
          not(tokens);
          break;
        case "XOR": // 5
          xor(tokens);
          break;
        case "LOADB": // 6
          loadb(tokens);
          break;
        case "LOADDB": // 7
          loaddb(tokens);
          break;
        case "LOADQB": // 8
          loadqb(tokens);
          break;
        case "STOREB": // 9
          storeb(tokens);
          break;
        case "STOREDB": // 10
          storedb(tokens);
          break;
        case "STOREQB": // 11
          storeqb(tokens);
          break;
        case "MOV": // 12
          mov(tokens, offset);
          break;
        case "LEAP": // 13
          leap(tokens);
          break;
        case "CLEAP": // 13
          cleap(tokens, 0);
          break;
        case "CLEAPP": // 13
          cleap(tokens, 1);
          break;
        case "CLEAPZ": // 13
          cleap(tokens, 2);
          break;
        case "CLEAPZP": // 13
          cleap(tokens, 3);
          break;
        case "CLEAPN": // 13
          cleap(tokens, 4);
          break;
        case "CLEAPNP": // 13
          cleap(tokens, 5);
          break;
        case "CLEAPNZ": // 13
          cleap(tokens, 6);
          break;
        case "CLEAPNZP": // 13
          cleap(tokens, 7);
          break;
        case "DSR": // 14
          dsr(tokens);
          break;
        case "HARKBACK":
          harkback(tokens); // 15
          break;
        case "PUSH":
          push(tokens); // 16
          break;
        case "POP":
          pop(tokens); // 17
          break;
        case "LSCOOT":
          scoot(tokens, 0); // 18
          break;
        case "RSCOOTL":
          scoot(tokens, 1); // 18
          break;
        case "RSCOOTA":
          scoot(tokens, 3); // 18
          break;
        case "TRAP":
          trap(tokens); // 19
          break;
      }
    }

    private static void add(String[] tokens)
    {
      arithmetic(tokens, 0);
    }

    private static void sub(String[] tokens)
    {
      arithmetic(tokens, 1);
    }

    private static void mul(String[] tokens)
    {
      arithmetic(tokens, 2);
    }

    private static void and(String[] tokens)
    {
      arithmetic(tokens, 3);
    }

    private static void not(String[] tokens)
    {
      String[] newTokens = new String[]{"XOR", tokens[1], tokens[1], "0xFFFF"};
      arithmetic(newTokens, 5);
    }

    private static void xor(String[] tokens)
    {
      arithmetic(tokens, 5);
    }

    private static void loadb(String[] tokens)
    {
      load(tokens, 6); 
    }

    private static void loaddb(String[] tokens)
    {
      load(tokens, 7);
    }

    private static void loadqb(String[] tokens)
    {
      load(tokens, 8);
    }

    private static void storeb(String[] tokens)
    {
      load(tokens, 9);
    }

    private static void storedb(String[] tokens)
    {
      load(tokens, 10);
    }

    private static void storeqb(String[] tokens)
    {
      load(tokens, 11);
    }

    private static void mov(String[] tokens, int offset)
    {
      int opcode = 12;
      if(tokens.length != 3)
      {
        throw new AssemblerException("Incorrect number of arguments for " + tokens[0] + " instruction.");
      }

      registerCheck(tokens[1]);
      outputStream.writeBits(BITS_PER_OPCODE, opcode);
      outputStream.writeBits(BITS_PER_REG, registerIndex(tokens[1]));
      if(isHex(tokens[2]))
      {
        int imm = hexToImm(tokens[2]);
        if(imm > 0X7FFFFF || imm < -0X7FFFFF)
        {
          throw new AssemblerException("Immediate: " + imm + " is not a valid immediate");
        }
        outputStream.writeBits(23, imm);
      }
      else
      {
        if(!symbolTable.containsKey(tokens[2]))
        {
          throw new AssemblerException("Symbol: "+ tokens[2]+" not found.");
        }

        int diff = (symbolTable.get(tokens[2]));

        outputStream.writeBits(23, diff);
      }
    }

    private static void leap(String[] tokens)
    {
      cleap(tokens, 7);
    }

    private static void cleap(String[] tokens, int condition)
    {
      int opcode = 13;
      if(tokens.length != 3)
      {
        throw new AssemblerException("Incorrect number of arguments for " + tokens[0] + " instruction.");
      }

      registerCheck(tokens[1]);
      outputStream.writeBits(BITS_PER_OPCODE, opcode);
      outputStream.writeBits(3, condition);
      outputStream.writeBits(BITS_PER_REG, registerIndex(tokens[1]));
      
      if(!isHex(tokens[2]))
      {
        throw new AssemblerException("Second LEAP argument must specify a hex offset");
      }
      int imm = hexToImm(tokens[2]);
      if(imm > 0XFFFFF || imm < -0XFFFFF)
      {
        throw new AssemblerException("Immediate: " + imm + " is not a valid immediate");
      }
      outputStream.writeBits(20, imm);
    }

    private static void dsr(String[] tokens)
    {
      oneRegister(tokens, 14);
    }

    private static void harkback(String[] tokens)
    {
      int opcode = 15;
      if (tokens.length != 1)
      {
        throw new AssemblerException("Incorrect number of arguments for " + tokens[0] + " instruction.");
      }

      outputStream.writeBits(BITS_PER_OPCODE, opcode);
      outputStream.writeBits(27, 0);
    }

    private static void push(String[] tokens)
    {
      oneRegister(tokens, 16);
    }

    private static void pop(String[] tokens)
    {
      oneRegister(tokens, 17);
    }

    private static void scoot(String[] tokens, int steering)
    {
      int opcode = 18;
      if(tokens.length != 3)
      {
        throw new AssemblerException("Incorrect number of arguments for " + tokens[0] + " instruction.");
      }

      registerCheck(tokens[1]);
      if(!isHex(tokens[2]))
      {
        throw new AssemblerException(tokens[2] + " is not a hex number.");
      }

      int imm = hexToImm(tokens[2]);
      if(imm > 0X1F || imm < 0)
      {
        throw new AssemblerException(imm + " is not a valid scoot number");
      }

      outputStream.writeBits(BITS_PER_OPCODE, opcode);
      outputStream.writeBits(BITS_PER_REG, registerIndex(tokens[1]));
      outputStream.writeBits(2, steering);
      outputStream.writeBits(16, 0);
      outputStream.writeBits(5, imm);
    }

    private static void trap(String[] tokens)
    {
      int opcode = 19;
      if(tokens.length != 2)
      {
        throw new AssemblerException("Incorrect number of arguments for " + tokens[0] + " instruction.");
      }

      if(!isHex(tokens[1]))
      {
        throw new AssemblerException(tokens[1] + " is not a hex number.");
      }

      int imm = hexToImm(tokens[1]);
      if(imm > 0X7FFFFFF || imm < 0)
      {
        throw new AssemblerException(imm + " is not a valid trap number");
      }

      outputStream.writeBits(BITS_PER_OPCODE, opcode);
      outputStream.writeBits(27, imm);
    }

    private static void oneRegister(String[] tokens, int opcode)
    {
      if(tokens.length != 2)
      {
        throw new AssemblerException("Incorrect number of arguments for " + tokens[0] + " instruction.");
      }

      registerCheck(tokens[1]);
      outputStream.writeBits(BITS_PER_OPCODE, opcode);
      outputStream.writeBits(23, 0);
      outputStream.writeBits(BITS_PER_REG, registerIndex(tokens[1]));
    }

    
    private static void arithmetic(String[] tokens, int opcode)
    {
      if(tokens.length != 4)
      {
        throw new AssemblerException("Incorrect number of arguments for " + tokens[0] + " instruction.");
      }
      // System.out.println("GOT HERE!");
      for(int i = 1; i < 3; i++)
      {
        registerCheck(tokens[i]);
      }

      outputStream.writeBits(BITS_PER_OPCODE, opcode);
      outputStream.writeBits(BITS_PER_REG, registerIndex(tokens[1]));
      outputStream.writeBits(BITS_PER_REG, registerIndex(tokens[2]));
      boolean immediate = isHex(tokens[3]);
      
      if(immediate)
      {
        int imm = hexToImm(tokens[3]);

        if(imm > 0XFFFF || imm < -0XFFFF)
        {
          throw new AssemblerException("Immediate: " + imm + " is not a valid immediate");  
        }

        outputStream.writeBits(3, 4);
        outputStream.writeBits(BITS_PER_IMM, imm);
      }
      else
      {
        outputStream.writeBits(15, 0);
        registerCheck(tokens[3]);
        outputStream.writeBits(BITS_PER_REG, registerIndex(tokens[3]));
      }
    }

    private static void load(String[] tokens, int opcode)
    {
      if(tokens.length != 3)
      {
        throw new AssemblerException("Incorrect number of arguments for " + tokens[0] + " instruction.");
      }

      for(int i = 1; i < 3; i++)
      {
        registerCheck(tokens[i]);
      }

      outputStream.writeBits(BITS_PER_OPCODE, opcode);
      outputStream.writeBits(BITS_PER_REG, registerIndex(tokens[1]));
      outputStream.writeBits(19, 0);
      outputStream.writeBits(BITS_PER_REG, registerIndex(tokens[2]));
    }

    private static void registerCheck(String register)
    {
      if(registerIndex(register) == -1)
      {
        throw new AssemblerException(register + " is not a valid register.");
      }
    }
  }
}

class AssemblerException extends RuntimeException
{ 
  public AssemblerException(String s)
  {
    super(s);
  }
}


