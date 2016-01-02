package mylib.tools.misc;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import mylib.tools.misc.BackuperEx.FilePair;
import mylib.util.MylibTestCase;

public class BackuperExTest extends MylibTestCase
{
  public BackuperExTest( String name )
  {
    super(name);
  }

  // ----------------------------------------------------------------------
  /**
  * ディレクトリ内にファイルのみがある場合。
  */
  public void testSimple()
  throws Exception
  {
    File dir = prepareTestDirectory();

    long cur = System.currentTimeMillis();

    // a/1
    // a/2
    File frd = new File(dir,"a"); frd.mkdir();
    File fr1 = new File(frd,"1"); touch(fr1,"1",cur);
    File fr2 = new File(frd,"2"); touch(fr2,"22",cur);

    // b/2
    // b/3
    File tod = new File(dir,"b"); tod.mkdir();
    File to2 = new File(tod,"2"); touch(to2,"22",cur);
    File to3 = new File(tod,"3"); touch(to3,"333",cur);

    BackuperEx target = new BackuperEx(frd,tod);

    target.doCompare(System.out);

    assertEquals(makeList(new File[]{fr1}),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{fr2,to2}),target.sameList);
    assertEquals(makeList(new File[]{to3}),target.toOnlyList);
  }

  // ----------------------------------------------------------------------
  /**
  * 同じ名前のファイルとディレクトリがあった場合のテストの準備。
  */
  public static BackuperEx prepareSimple( File dir )
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
    File a = new File(dir,"a"); a.mkdir();
    File a1 = new File(a,"1"); touch(a1,"1",current);
    File a2 = new File(a,"2"); touch(a2,"2",current);
    File a4 = new File(a,"4"); a4.mkdir();
    File a41 = new File(a4,"1"); touch(a41,"4/1",current);
    File a5 = new File(a,"5"); touch(a5,"5",current);
    File a6 = new File(a,"6"); a6.mkdir();
    File a61 = new File(a6,"1"); touch(a61,"6/1",current);
    File a62 = new File(a6,"2"); touch(a62,"6/2",current);

    current += 10000L;
    File b = new File(dir,"b"); b.mkdir();
    File b2 = new File(b,"2"); touch(b2,"2",a2.lastModified());
    File b3 = new File(b,"3"); touch(b3,"3",current);
    File b4 = new File(b,"4"); touch(b4,"4",current);
    File b5 = new File(b,"5"); b5.mkdir();
    File b51 = new File(b5,"1"); touch(b51,"5/1",current);
    File b6 = new File(b,"6"); b6.mkdir();
    File b62 = new File(b6,"2"); touch(b62,"6/2",current);
    File b63 = new File(b6,"3"); touch(b63,"6/3",current);

    BackuperEx target = new BackuperEx(a,b);

    target.doCompare(System.out);

    assertEquals(makeList(new File[]{a1,a4,a41,a5,a61}),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{a2,b2,a6,b6}),target.sameList);
    assertEquals(makeList(new File[]{b3,b4,b5,b51,b63}),target.toOnlyList);
    assertEquals(makeFilePairList(new File[]{a62,b62}),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    return target;
  }

  /**
  * 同じ名前のファイルとディレクトリがあった場合のテスト：比較のみ。
  * doCompare のみ
  */
  public void testSimpleDir0()
  throws Exception
  {
    File dir = prepareTestDirectory();
    BackuperEx target = prepareSimple(dir);

    assertDirectory(new File(dir,"b"),new String[][]{
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
  public void testSimpleDir1()
  throws Exception
  {
    File dir = prepareTestDirectory();
    BackuperEx target = prepareSimple(dir);

    long origmod = new File(dir,"a/6/2").lastModified();
    assertEquals(origmod+10000L,new File(dir,"b/6/2").lastModified());

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertEquals(origmod,new File(dir,"b/6/2").lastModified());

    assertDirectory(new File(dir,"b"),new String[][]{
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
  public void testSimpleDir2()
  throws Exception
  {
    File dir = prepareTestDirectory();
    BackuperEx target = prepareSimple(dir);

    target.compareTouchList(System.out);

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertDirectory(new File(dir,"b"),new String[][]{
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
  public static BackuperEx prepareMove( File dir )
  throws Exception
  {
    long current = System.currentTimeMillis() - 10000L;
    File a = new File(dir,"a"); a.mkdir();
    File a1 = new File(a,"1"); a1.mkdir();
    File a11 = new File(a1,"1"); touch(a11,"data 11",current);
    File a12 = new File(a1,"2"); touch(a12,"data 2222",current);
    File a13 = new File(a1,"3"); touch(a13,"data 333333",current);

    current += 10000L;
    File b = new File(dir,"b"); b.mkdir();
    File b2 = new File(b,"2"); b2.mkdir();
    File b21 = new File(b2,"1"); touch(b21,"data 11",a11.lastModified());
    File b22 = new File(b2,"2"); touch(b22,"data 1212",a12.lastModified());
    File b23 = new File(b2,"3"); touch(b23,"data 333333",current);
    File b3 = new File(b,"3"); b3.mkdir();
    File b30 = new File(b3,"0"); touch(b30,current);

    BackuperEx target = new BackuperEx(a,b);

    target.doCompare(System.out);

    assertEquals(makeList(new File[]{a1,a13}),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(makeList(new File[]{b2,b23,b3,b30}),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(makeFilePairList(new File[]{a11,b21,a12,b22}),target.moveList);

    return target;
  }

  /**
  * ディレクトリ内のファイル移動があった場合のテスト：単純コピー。
  * doCompare(), doExecute();
  */
  public void testMove1()
  throws Exception
  {
    File dir = prepareTestDirectory();

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
  public void testMove2()
  throws Exception
  {
    File dir = prepareTestDirectory();

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
  public void testSame()
  throws Exception
  {
    File dir = prepareTestDirectory();

    long current = System.currentTimeMillis() - 10000L;
    File a = new File(dir,"a"); a.mkdir();
    File a1 = new File(a,"1"); touch(a1,"data 11",current);
    File a2 = new File(a,"2"); touch(a2,"data 2222",current);
    File a3 = new File(a,"3"); touch(a3,"data 333333",current);

    current += 10000L;
    File b = new File(dir,"b"); b.mkdir();
    File b1 = new File(b,"1"); touch(b1,"data 11",a1.lastModified());
    File b2 = new File(b,"2"); touch(b2,"data 1212",a2.lastModified());
    File b3 = new File(b,"3"); touch(b3,"data 131313",a3.lastModified());

    BackuperEx target = new BackuperEx(a,b);

    target.doCompare(System.out);

    assertEquals(new ArrayList<File>(),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{a1,b1,a2,b2,a3,b3}),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    target.compareSameList(System.out);

    assertEquals(makeList(new File[]{a2,a3}),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{a1,b1}),target.sameList);
    assertEquals(makeList(new File[]{b2,b3}),target.toOnlyList);
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
  public void testFilePair()
  throws Exception
  {
    File dir = prepareTestDirectory();
    File fileA = new File(dir,"A");
    File fileB = new File(dir,"B");
    File fileC = new File(dir,"C");
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

  // ----------------------------------------------------------------------
  // 以下は、ユーティリティ。
  /**
  * FilePair のリストを生成する。
  */
  public static List<FilePair> makeFilePairList( File pairs[] )
  {
    FilePair pair[] = new FilePair[pairs.length/2];
    for ( int i = 0; i < pairs.length; i += 2 ) {
      pair[i/2] = new FilePair(pairs[i],pairs[i+1]);
    }
    return makeList(pair);
  }
}
