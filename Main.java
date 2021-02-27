import java.util.*;
import java.io.*;
public class Main
{
  // private static HashMap<String, String> registerMap = new HashMap<>();
  // NOT OPCODE, Doesn't start with a dot
  private static final List<String> OPCODE_ARRAY = Arrays.asList(new String[]{"ADD", "SUB", "MUL", "AND", "NOT", "XOR", "LOADB", "LOADDB", 
                                                                            "LOADQB", "STOREB", "STOREDB", "STOREQB", "MOV", "LEAP", "TRAP", "RSCOOTA", 
                                                                            "RSCOOTL", "LSCOOT", "POP", "PUSH", "HARKBACK", "DSR", "CLEAP"});
  private static HashMap<String, Integer> symbolTable = new HashMap<>();
  private static HashMap<String, Integer> opCodeTable = new HashMap<>();
  private static final int ADDRESS_OFFSET = 4;
  private static int beginningAddress; 
  private static String ORIG_STRING = ".ORIG";
  private static String END_STRING = ".END";
  private static String COMMENT_STRING = ";";
  private static String FILE_EXTENSION = ".bmb";
  private static String binaryFile;
  
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
      System.out.println(args[0]);
      System.out.println(Arrays.toString(args[0].split("\\.")));
      binaryFile = args[0].split("\\.")[0] + FILE_EXTENSION;
    }

    try 
    {
      File outputFile = new File(binaryFile);
      if(!outputFile.createNewFile())
      {
        throw new AssemblerException("Could not create output file");
      }
    }
    catch (IOException e)
    {
      throw new AssemblerException("Could not create output file");
    }

    // throw new AssemblerException("TEST END");

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


  private static void secondPass(Scanner file)
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

  private static int secondPassParse(String line, int offset)
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

  static class AssemblyLine
  {
    private static void opcodeCheck(String[] tokens)
    {
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
          break;
        case "MUL":
          break;
        case "AND":
          break;
        case "NOT":
          break;
        case "XOR":
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


