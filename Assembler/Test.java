import java.io.IOException;

public class Test
{
  public static void main(String[] args) throws IOException
  {
    BitInputStream test = new BitInputStream("boob.bmb");
    int bits;
    while((bits = test.readBits(32)) != - 1)
    {
      String b = Integer.toBinaryString(bits);
      String save = String.format("%32s", b).replace(' ', '0');
      String h = String.format("0x%8x", bits).replace(' ', '0');
      System.out.println(save + "\t" + h);
    }
  }
}