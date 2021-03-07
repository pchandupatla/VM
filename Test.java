import java.io.IOException;

public class Test
{
  public static void main(String[] args) throws IOException
  {
    BitInputStream test = new BitInputStream("boob.bmb");
    int bits;
    while((bits = test.readBits(1)) != - 1)
    {
      System.out.println(bits);
    }
  }
}