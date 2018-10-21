package mylib.tools.misc;

import org.junit.Test;

public class TerminalTest
{
  public static void main( String argv[] )
  {
    try {
      System.out.println("ABC");
      System.out.println("012");
      System.out.print("\u001b[A\u001b[C");
      System.out.println("XYZ");
    } catch ( Exception ex ) {
      ex.printStackTrace();
    }
  }
}
