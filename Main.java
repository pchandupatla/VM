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
  private static final int ADDRESS_OFFSET = 4;
  private static int beginningAddress;
  public static void main(String[] args) throws IOException
  {
    if(args.length != 1)
    {
      throw new IllegalArgumentException("YOU DONE MESSED UP BOY/GIRL!");
    }
    
    Scanner file = new Scanner(new File(args[0]));
    boolean origFound = false;
    while(!origFound)
    {
      if(file.hasNextLine())
      {
        String origAddress = file.nextLine();
        String[] tokens = origAddress.split(" ");
        if(tokens[0].equals(".ORIG"))
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
          System.out.println(beginningAddress);
        }
      }
      else
      {
        throw new AssemblerException("Syntax error, no .ORIG op found");
      }
    }

    firstPass(file);
  }

  private static void firstPass(Scanner file)
  {

    int offset = 0; //points to the address of the next current instruction
    // System.out.println(OPCODE_ARRAY.length);
    while(file.hasNextLine())
    {
      String currLine = file.nextLine();
      int semiColIndex = currLine.indexOf(";");
      if(semiColIndex != -1)
      {
        currLine = currLine.substring(0, semiColIndex);
      }
      
      if(currLine.length() == 0)
      {
        continue;
      }

      System.out.printf("%s - %d\n", currLine, beginningAddress + offset);
      offset = firstPassParse(currLine, offset);
    }

    System.out.println(symbolTable.toString());
  }

  // commas, tabs, spaces
  //Returns 
  private static int firstPassParse(String line, int offset)
  {
    String[] tokens = line.split("[\\s,]+");

    if(tokens.length <= 0){
      return offset;
    }
    
    if(!OPCODE_ARRAY.contains(tokens[0]) && !tokens[0].startsWith(".")){
      int address = beginningAddress + offset;
      
      if(isHex(tokens[0])){
        throw new AssemblerException("Invalid label on line: " + line);
      }else{
        symbolTable.put(tokens[0], address);
      }
    }

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
}

class AssemblerException extends RuntimeException
{ 
  public AssemblerException(String s)
  {
    super(s);
  }
}
