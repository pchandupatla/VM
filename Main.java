import java.util.*;
import java.io.*;

public class Main
{
  // private static HashMap<String, String> registerMap = new HashMap<>();
  // NOT OPCODE, Doesn't start with a dot
  //TODO: fix OPCODE_ARRAY opcodes
  private static final List<String> OPCODE_ARRAY = Arrays.asList(new String[]{"ADD", "SUB", "MUL", "AND", "NOT", "XOR", "LOADB", "LOADDB", 
                                                                            "LOADQB", "STOREB", "STOREDB", "STOREQB", "MOV", "LEAP", "TRAP", "RSCOOTA", 
                                                                            "RSCOOTL", "LSCOOT", "POP", "PUSH", "HARKBACK", "DSR", "CLEAP"});
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
      // System.out.println(args[0]);
      // System.out.println(Arrays.toString(args[0].split("\\.")));
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
          // System.out.println(beginningAddress);
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
    // System.out.println(file.nextLine());
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

      // System.out.printf("%s - %d\n", currLine, beginningAddress + offset);
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
    
    if(!OPCODE_ARRAY.contains(tokens[0]) && !tokens[0].startsWith("."))
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
    if(!isOpcode(tokens[0]))
    {
      tokens = Arrays.copyOfRange(tokens, 1, tokens.length);
    }
    
    AssemblyLine.opcodeCheck(tokens);
    
    return offset + ADDRESS_OFFSET;
  }

  private static boolean isHex(String line)
  {
    String hexIdentifier = "0x";
    if(line.length() < 3)
    {
      return false;
    }

    return line.substring(0, 2).equalsIgnoreCase(hexIdentifier);
  }

  private static boolean isOpcode(String opcode)
  {
    return OPCODE_ARRAY.contains(opcode);
  }

  private static int registerIndex(String register)
  {
    return REGISTER_ARRAY.indexOf(register);
  }

  static class AssemblyLine
  {
    private static boolean initialize = false;
    private static BitOutputStream outputStream;

    private static void opcodeCheck(String[] tokens) throws IOException
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
        throw new AssemblerException("Opcode: " + tokens[0] + "not recognized");
      }

      switch(opcode)
      {
        case "ADD":
          add(tokens);
          break;
        case "SUB":
          sub(tokens);
          break;
        case "MUL":
          mul(tokens);
          break;
        case "AND":
          and(tokens);
          break;
        case "NOT":
          not(tokens);
          break;
        case "XOR":
          xor(tokens);
          break;
        case "LOADB":
          break;
        case "LOADDB":
          break;
        case "LOADQB":
          break;
        case "STOREB":
          break;
        case "STOREDB":
          break;
        case "STOREQB":
          break;
        case "MOV":
          break;
        case "LEAP":
        case "CLEAP":
          break;
        case "DSR":
          break;
        case "HARKBACK":
          break;
        case "PUSH":
          break;
        case "POP":
          break;
        case "LSCOOT":
        case "RSCOOTL":
        case "RSCOOTA":
          break;
        case "TRAP":
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


    private static void arithmetic(String[] tokens, int opcode)
    {
      if(tokens.length != 4)
      {
        throw new AssemblerException("Incorrect number of arguments for " + OPCODE_ARRAY.get(opcode) +" instruction.");
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
        String hex = tokens[3].substring(2);
        int imm = Integer.parseInt(hex, 16);

        if(imm > 0XFFFF || imm < -0X8000)
        {
          throw new AssemblerException("Immediate: " + hex + " is not a valid immediate");  
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


