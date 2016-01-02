package mylib.tools.misc;

import static org.junit.Assert.*;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import mylib.tools.misc.BackuperEx.FilePair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static mylib.tools.misc.BackuperTest.*;

public class BackuperExTest // extends MylibTestCase
{
  public BackuperExTest( /* String name */ )
  {
    // super(name);
  }

  @Rule
  public TemporaryFolder tempdir = new TemporaryFolder(new File("target"));

  // ----------------------------------------------------------------------
  /**
  * ディレクトリ内にファイルのみがある場合。
  */
  @Test
  public void testSimple()
  throws Exception
  {
    long cur = System.currentTimeMillis();

    // a/1
    // a/2
    File frd = tempdir.newFolder("a");
    File fr1 = tempdir.newFile  ("a/1"); touch(fr1,"1",cur);
    File fr2 = tempdir.newFile  ("a/2"); touch(fr2,"22",cur);

    // b/2
    // b/3
    File tod = tempdir.newFolder("b");
    File to2 = tempdir.newFile  ("b/2"); touch(to2,"22",cur);
    File to3 = tempdir.newFile  ("b/3"); touch(to3,"333",cur);

    BackuperEx target = new BackuperEx(frd,tod);

    target.doCompare(System.out);

    assertEquals(Arrays.asList(new File[]{fr1}),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{fr2,to2}),target.sameList);
    assertEquals(Arrays.asList(new File[]{to3}),target.toOnlyList);
  }

  // ----------------------------------------------------------------------
  /**
  * 同じ名前のファイルとディレクトリがあった場合のテストの準備。
  */
  public BackuperEx prepareSimple()
  throws Exception
  {
    /**
    + a/1
    = a/2
    + a/4
    + a/4/1
    + a/5
    = a/6
    + a/6/1
    = a/6/2

    = b/2
    + b/3
    + b/4
    + b/5
    + b/5/1
    = b/6
    = b/6/2
    + b/6/3
    **/
    long current = System.currentTimeMillis() - 10000L;
    File a   = tempdir.newFolder("a");
    File a1  = tempdir.newFile  ("a/1"); touch(a1,"1",current);
    File a2  = tempdir.newFile  ("a/2"); touch(a2,"2",current);
    File a4  = tempdir.newFolder("a","4");
    File a41 = tempdir.newFile  ("a/4/1"); touch(a41,"4/1",current);
    File a5  = tempdir.newFile  ("a/5"); touch(a5,"5",current);
    File a6  = tempdir.newFolder("a","6");
    File a61 = tempdir.newFile  ("a/6/1"); touch(a61,"6/1",current);
    File a62 = tempdir.newFile  ("a/6/2"); touch(a62,"6/2",current);

    current += 10000L;
    File b   = tempdir.newFolder("b");
    File b2  = tempdir.newFile  ("b/2"); touch(b2,"2",a2.lastModified());
    File b3  = tempdir.newFile  ("b/3"); touch(b3,"3",current);
    File b4  = tempdir.newFile  ("b/4"); touch(b4,"4",current);
    File b5  = tempdir.newFolder("b","5");
    File b51 = tempdir.newFile  ("b/5/1"); touch(b51,"5/1",current);
    File b6  = tempdir.newFolder("b","6");
    File b62 = tempdir.newFile  ("b/6/2"); touch(b62,"6/2",current);
    File b63 = tempdir.newFile  ("b/6/3"); touch(b63,"6/3",current);

    BackuperEx target = new BackuperEx(a,b);

    target.doCompare(System.out);

    assertEquals(Arrays.asList(new File[]{a1,a4,a41,a5,a61}),target.fromOnlyList);
    assertEquals(new HashSet(makeFilePairList(new File[]{a2,b2,a6,b6})),new HashSet(target.sameList));
    assertEquals(Arrays.asList(new File[]{b3,b4,b5,b51,b63}),target.toOnlyList);
    assertEquals(makeFilePairList(new File[]{a62,b62}),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    return target;
  }

  /**
  * 同じ名前のファイルとディレクトリがあった場合のテスト：比較のみ。
  * doCompare のみ
  */
  @Test
  public void testSimpleDir0()
  throws Exception
  {
    BackuperEx target = prepareSimple();

    assertDirectory(new File(tempdir.getRoot(),"b"),new String[][]{
      { "2", "2" },
      { "3", "3" },
      { "4", "4" },
      { "5/1", "5/1" },
      { "6/2", "6/2" },
      { "6/3", "6/3" },
    });
  }

  /**
  * 同じ名前のファイルとディレクトリがあった場合のテスト：単純コピー。
  * doCompare(), doExecute()
  */
  @Test
  public void testSimpleDir1()
  throws Exception
  {
    BackuperEx target = prepareSimple();

    long origmod = new File(tempdir.getRoot(),"a/6/2").lastModified();
    assertEquals(origmod+10000L,new File(tempdir.getRoot(),"b/6/2").lastModified());

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertEquals(origmod,new File(tempdir.getRoot(),"b/6/2").lastModified());

    assertDirectory(new File(tempdir.getRoot(),"b"),new String[][]{
      { "1", "1" },
      { "2", "2" },
      { "4/1", "4/1" },
      { "5", "5" },
      { "6/1", "6/1" },
      { "6/2", "6/2" },
    });
  }

  /**
  * 同じ名前のファイルとディレクトリがあった場合のテスト：時刻違いのファイル比較付き。
  * doCompare(), compareTouchList(), doExecute()
  */
  @Test
  public void testSimpleDir2()
  throws Exception
  {
    BackuperEx target = prepareSimple();

    target.compareTouchList(System.out);

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertDirectory(new File(tempdir.getRoot(),"b"),new String[][]{
      { "1", "1" },
      { "2", "2" },
      { "4/1", "4/1" },
      { "5", "5" },
      { "6/1", "6/1" },
      { "6/2", "6/2" },
    });
  }

  // ----------------------------------------------------------------------
  /**
  * ディレクトリ内のファイル移動があった場合のテストの準備。
  */
  public BackuperEx prepareMove( File dir )
  throws Exception
  {
    long current = System.currentTimeMillis() - 10000L;
    File a   = tempdir.newFolder("a");
    File a1  = tempdir.newFolder("a","1");
    File a11 = tempdir.newFile  ("a/1/1"); touch(a11,"data 11",current);
    File a12 = tempdir.newFile  ("a/1/2"); touch(a12,"data 2222",current);
    File a13 = tempdir.newFile  ("a/1/3"); touch(a13,"data 333333",current);

    current += 10000L;
    File b   = tempdir.newFolder("b");
    File b2  = tempdir.newFolder("b","2");
    File b21 = tempdir.newFile	("b/2/1"); touch(b21,"data 11",a11.lastModified());
    File b22 = tempdir.newFile	("b/2/2"); touch(b22,"data 1212",a12.lastModified());
    File b23 = tempdir.newFile	("b/2/3"); touch(b23,"data 333333",current);
    File b3  = tempdir.newFolder("b","3");
    File b30 = tempdir.newFile  ("b/3/0"); b30.setLastModified(current);

    BackuperEx target = new BackuperEx(a,b);

    target.doCompare(System.out);

    assertEquals(Arrays.asList(new File[]{a1,a13}),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(Arrays.asList(new File[]{b2,b23,b3,b30}),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(makeFilePairList(new File[]{a11,b21,a12,b22}),target.moveList);

    return target;
  }

  /**
  * ディレクトリ内のファイル移動があった場合のテスト：単純コピー。
  * doCompare(), doExecute();
  */
  @Test
  public void testMove1()
  throws Exception
  {
    File dir = tempdir.getRoot();
    BackuperEx target = prepareMove(dir);

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertDirectory(new File(dir,"b"),new String[][]{
      { "1/1", "data 11" },
      { "1/2", "data 1212" },
      { "1/3", "data 333333" },
    });
  }

  /**
  * ディレクトリ内のファイル移動があった場合のテスト：ディレクトリ違いのファイルの比較付き。
  * doCompare(), compareMoveList(), doExecute();
  */
  @Test
  public void testMove2()
  throws Exception
  {
    File dir = tempdir.getRoot();
    BackuperEx target = prepareMove(dir);
    target.compareMoveList(System.out);

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertDirectory(new File(dir,"b"),new String[][]{
      { "1/1", "data 11" },
      { "1/2", "data 2222" },
      { "1/3", "data 333333" },
    });
  }

  // ----------------------------------------------------------------------
  /**
  * 同一ファイル名、同一時刻でも、完全に比較する場合のテスト。
  */
  @Test
  public void testSame()
  throws Exception
  {
    long current = System.currentTimeMillis() - 10000L;
    File a = tempdir.newFolder("a");
    File a1 = tempdir.newFile("a/1"); touch(a1,"data 11",current);
    File a2 = tempdir.newFile("a/2"); touch(a2,"data 2222",current);
    File a3 = tempdir.newFile("a/3"); touch(a3,"data 333333",current);

    current += 10000L;
    File b = tempdir.newFolder("b");
    File b1 = tempdir.newFile("b/1"); touch(b1,"data 11",a1.lastModified());
    File b2 = tempdir.newFile("b/2"); touch(b2,"data 1212",a2.lastModified());
    File b3 = tempdir.newFile("b/3"); touch(b3,"data 131313",a3.lastModified());

    BackuperEx target = new BackuperEx(a,b);

    target.doCompare(System.out);

    assertEquals(new ArrayList<File>(),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{a1,b1,a2,b2,a3,b3}),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    target.compareSameList(System.out);

    assertEquals(Arrays.asList(new File[]{a2,a3}),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{a1,b1}),target.sameList);
    assertEquals(Arrays.asList(new File[]{b2,b3}),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    target.doExecute(System.out);

    assertDirectory(b,new String[][]{
      { "1", "data 11" },
      { "2", "data 2222" },
      { "3", "data 333333" },
    });
  }

  // ----------------------------------------------------------------------
  /**
  * FilePair のテスト。
  */
  @Test
  public void testFilePair()
  throws Exception
  {
    File dir = tempdir.getRoot();
    File fileA = tempdir.newFile("A");
    File fileB = tempdir.newFile("B");
    File fileC = tempdir.newFile("C");
    FilePair target;

    target = new FilePair(fileA,fileB);
    assertTrue(target.equals(new FilePair(fileA,fileB)));
    assertFalse(target.equals(null));
    assertFalse(target.equals(new FilePair(fileA,fileC)));
    assertFalse(target.equals(new FilePair(fileA,null)));
    assertFalse(target.equals(new FilePair(null,fileB)));
    assertFalse(target.equals(new FilePair(null,null)));
    assertFalse(target.equals(new Object()));

    target = new FilePair(fileA,null);
    assertTrue(target.equals(new FilePair(fileA,null)));
    assertFalse(target.equals(null));
    assertFalse(target.equals(new FilePair(fileA,fileC)));
    assertFalse(target.equals(new FilePair(fileA,fileB)));
    assertFalse(target.equals(new FilePair(null,fileB)));
    assertFalse(target.equals(new FilePair(null,null)));
    assertFalse(target.equals(new Object()));

    target = new FilePair(null,fileB);
    assertTrue(target.equals(new FilePair(null,fileB)));
    assertFalse(target.equals(null));
    assertFalse(target.equals(new FilePair(fileC,fileB)));
    assertFalse(target.equals(new FilePair(fileA,fileB)));
    assertFalse(target.equals(new FilePair(fileA,null)));
    assertFalse(target.equals(new FilePair(null,null)));
    assertFalse(target.equals(new Object()));

    target = new FilePair(null,null);
    assertTrue(target.equals(new FilePair(null,null)));
    assertFalse(target.equals(null));
    assertFalse(target.equals(new FilePair(fileC,fileB)));
    assertFalse(target.equals(new FilePair(fileA,fileB)));
    assertFalse(target.equals(new FilePair(fileA,null)));
    assertFalse(target.equals(new FilePair(null,fileB)));
    assertFalse(target.equals(new Object()));
  }

  /**
  * FilePair のリストを生成する。
  */
  public static List<FilePair> makeFilePairList( File pairs[] )
  {
    FilePair pair[] = new FilePair[pairs.length/2];
    for ( int i = 0; i < pairs.length; i += 2 ) {
      pair[i/2] = new FilePair(pairs[i],pairs[i+1]);
    }
    return Arrays.asList(pair);
  }
}
