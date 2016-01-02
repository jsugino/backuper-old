package mylib.tools.misc;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mylib.tools.misc.Backuper.FilePair;
import mylib.tools.misc.Backuper.VirFile;
import mylib.util.MylibTestCase;
import mylib.util.DirMaker;

public class BackuperTest extends MylibTestCase
{
  public BackuperTest( String name )
  {
    super(name);
  }

  // ----------------------------------------------------------------------
  /**
  * relate に関するテスト
  */
  public void testRelate()
  {
    File dir = prepareTestDirectory();
    System.out.println("dir = "+dir);
    assertEquals(new RealFile(new File(dir,"b/1")),calcRelate(dir,"a/1","a","b"));

    assertEquals(new RealFile(new File(dir,"b/1/2/3")),calcRelate(dir,"a/1/2/3","a","b"));
    assertEquals(new RealFile(new File(dir,"b/3")),calcRelate(dir,"a/1/2/3","a/1/2","b"));
    assertEquals(new RealFile(new File(dir,"b")),calcRelate(dir,"a/1/2/3","a/1/2/3","b"));

    assertEquals(new RealFile(new File(dir,"b/x/1/2/3")),calcRelate(dir,"a/1/2/3","a","b/x"));
    assertEquals(new RealFile(new File(dir,"b/x/y/3")),calcRelate(dir,"a/1/2/3","a/1/2","b/x/y"));
    assertEquals(new RealFile(new File(dir,"b/x/y/z")),calcRelate(dir,"a/1/2/3","a/1/2/3","b/x/y/z"));
  }

  public VirFile calcRelate( File dir, String target, String base, String rel )
  {
    return Backuper.relate(
      new RealFile(new File(dir,target)),
      new RealFile(new File(dir,base)),
      new RealFile(new File(dir,rel)));
  }

  // ----------------------------------------------------------------------
  /**
  * ディレクトリ内にファイルのみがある場合。
  */
  public void testSimple()
  throws Exception
  {
    File dir = prepareTestDirectory();

    long current = System.currentTimeMillis() - 10000L;

    File frd = new File(dir,"a"); frd.mkdir();
    File fr1 = new File(frd,"1"); touch(fr1,"1",current);
    File fr2 = new File(frd,"2"); touch(fr2,"22",current+4000L);

    File tod = new File(dir,"b"); tod.mkdir();
    File to2 = new File(tod,"2"); touch(to2,"22",current+4000L);
    File to3 = new File(tod,"3"); touch(to3,"333",current+8000L);

    Backuper target = new Backuper(frd,tod);

    target.doCompare(System.out);

    assertEquals(makeMyList(new File[]{fr1}),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{fr2,to2}),target.sameList);
    assertEquals(makeMyList(new File[]{to3}),target.toOnlyList);
  }

  // ----------------------------------------------------------------------
  /**
  * 長さが異なる場合。
  */
  public void testLength()
  throws Exception
  {
    File dir = prepareTestDirectory();

    long current = System.currentTimeMillis();

    File frd = new File(dir,"a"); frd.mkdir();
    File fr1 = new File(frd,"1"); touch(fr1,"1",current);
    File fr2 = new File(frd,"2"); touch(fr2,"22",current);
    File fr3 = new File(frd,"3"); touch(fr3,"333",current);

    File tod = new File(dir,"b"); tod.mkdir();
    File to1 = new File(tod,"1"); touch(to1,"1",current);
    File to2 = new File(tod,"2"); touch(to2,"222",current);
    File to3 = new File(tod,"3"); touch(to3,"333",current);

    Backuper target = new Backuper(frd,tod);

    target.doCompare(System.out);

    assertEquals(makeFilePairList(new File[]{fr1,to1,fr3,to3}),target.sameList);
    assertEquals(makeMyList(new File[]{fr2}),target.fromOnlyList);
    assertEquals(makeMyList(new File[]{to2}),target.toOnlyList);
    assertEquals(0,target.touchList.size());
    assertEquals(0,target.moveList.size());
  }

  // ----------------------------------------------------------------------
  /**
  * 同じ名前のファイルとディレクトリがあった場合のテストの準備。
  */
  public static Backuper prepareSimple( File dir )
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
    File a1 = new File(a,"1"); touch(a1,current);
    File a2 = new File(a,"2"); touch(a2,current);
    File a4 = new File(a,"4"); a4.mkdir();
    File a41 = new File(a4,"1"); touch(a41,current);
    File a5 = new File(a,"5"); touch(a5,current);
    File a6 = new File(a,"6"); a6.mkdir();
    File a61 = new File(a6,"1"); touch(a61,current);
    File a62 = new File(a6,"2"); touch(a62,"abc",current);

    current += 10000L;
    File b = new File(dir,"b"); b.mkdir();
    File b2 = new File(b,"2"); touch(b2,a2.lastModified());
    File b3 = new File(b,"3"); touch(b3,current);
    File b4 = new File(b,"4"); touch(b4,current);
    File b5 = new File(b,"5"); b5.mkdir();
    File b51 = new File(b5,"1"); touch(b51,current);
    File b6 = new File(b,"6"); b6.mkdir();
    File b62 = new File(b6,"2"); touch(b62,"def",current);
    File b63 = new File(b6,"3"); touch(b63,current);

    Backuper target = new Backuper(a,b);

    target.doCompare(System.out);

    assertEquals(makeMyList(new File[]{a1,a4,a41,a5,a61}),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{a2,b2}),target.sameList);
    assertEquals(makeMyList(new File[]{b3,b4,b5,b51,b63}),target.toOnlyList);
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
    Backuper target = prepareSimple(dir);

    assertDirectory(new File(dir,"b"),new String[][]{
      { "2" },
      { "3" },
      { "4" },
      { "5/1" },
      { "6/2", "def" },
      { "6/3" },
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
    Backuper target = prepareSimple(dir);

    long origmod = new File(dir,"a/6/2").lastModified();
    assertEquals(origmod+10000L,new File(dir,"b/6/2").lastModified());

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertEquals(origmod,new File(dir,"b/6/2").lastModified());

    assertDirectory(new File(dir,"b"),new String[][]{
      { "1" },
      { "2" },
      { "4/1" },
      { "5" },
      { "6/1" },
      { "6/2", "def" },
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
    Backuper target = prepareSimple(dir);

    target.compareTouchList(System.out);

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertDirectory(new File(dir,"b"),new String[][]{
      { "1" },
      { "2" },
      { "4/1" },
      { "5" },
      { "6/1" },
      { "6/2", "abc" },
    });
  }

  // ----------------------------------------------------------------------
  /**
  * ディレクトリ内のファイル移動があった場合のテストの準備。
  */
  public static Backuper prepareMove( File dir )
  throws Exception
  {
    long current = System.currentTimeMillis() - 10000L;
    DirMaker mk = new DirMaker(dir);

    File a, a1, a11, a12, a13;

    mk.top().c(
      a = mk.d("a").c(
	a1 = mk.d("1").c(
	  a11 = mk.f("1","data 11",current),
	  a12 = mk.f("2","data 2222",current),
	  a13 = mk.f("3","data 333333",current))));

    File b, b2, b21, b22, b23, b3, b30;
    current += 10000L;
    mk.top().c(
      b = mk.d("b").c(
	b2 = mk.d("2").c(
	  b21 = mk.f("1","data 11",a11.lastModified()),
	  b22 = mk.f("2","data 1212",a12.lastModified()),
	  b23 = mk.f("3","data 333333",current)),
	b3 = mk.d("3").c(
	  b30 = mk.f("0","",current))));

    Backuper target = new Backuper(a,b);

    target.doCompare(System.out);

    assertEquals(makeMyList(new File[]{a1,a13}),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(makeMyList(new File[]{b2,b23,b3,b30}),target.toOnlyList);
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

    Backuper target = prepareMove(dir);

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

    Backuper target = prepareMove(dir);
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

    Backuper target = new Backuper(a,b);

    target.doCompare(System.out);

    assertEquals(new ArrayList<File>(),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{a1,b1,a2,b2,a3,b3}),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    target.compareSameList(System.out);

    assertEquals(makeMyList(new File[]{a2,a3}),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{a1,b1}),target.sameList);
    assertEquals(makeMyList(new File[]{b2,b3}),target.toOnlyList);
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
  * RejectFile のテスト。
  * ソースディレクトリの不要ディレクトリの指定。
  */
  public void testReject()
  throws Exception
  {
    File dir = prepareTestDirectory();
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;

    File a, ax, ax1, ax2, axy, axy1, axy2, ay, ay1, ay2, az, az1, az2, azx, azx1, azx2, azxx, azxx1, azxx2, azxxy, azxxy1, azxxy2, b;
    DirMaker mk = new DirMaker(dir);
    mk.top().c(
      a = mk.d("dira").c(
	ax = mk.d("dirx").c(
	  ax1 = mk.f("fileax1","data ax1",current),
	  ax2 = mk.f("fileax2","data ax2",current),
	  axy = mk.d("diry").c(
	    axy1 = mk.f("fileaxy1","data axy1",current),
	    axy2 = mk.f("fileaxy2","data axy2",current))),
	ay = mk.d("diry").c(
	  ay1 = mk.f("fileay1","data ay1",current),
	  ay2 = mk.f("fileay2","data ay2",current)),
	az = mk.d("dirz").c(
	  az1 = mk.f("fileaz1","data az1",current),
	  az2 = mk.f("fileaz2","data az2",current),
	  azx = mk.d("dirx").c(
	    azx1 = mk.f("fileazx1","data azx1",current),
	    azx2 = mk.f("fileazx2","data azx2",current),
	    azxx = mk.d("dirx").c(
	      azxx1 = mk.f("fileazxx1","data azxx1",current),
	      azxx2 = mk.f("fileazxx2","data azxx2",current),
	      azxxy = mk.d("diry").c(
		azxxy1 = mk.f("fileazxxy1","data azxxy1",current),
		azxxy2 = mk.f("fileazxxy2","data azxxy2",current)))))),
      b = mk.d("dirb").c());

    Backuper target = new Backuper(a,b);

    // ------------------------------
    target.clearAll();
    target.doCompare(System.out);

    assertCollectionEquals(
      makeMyArray(
	ax,ax1,ax2,axy,axy1,axy2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
      ), target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("diry");
    target.doCompare(System.out);

    assertCollectionEquals(
      makeMyArray(
	ax,ax1,ax2,axy,axy1,axy2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
      ), target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("dirx/diry");
    target.doCompare(System.out);

    assertCollectionEquals(
      makeMyArray(
	ax,ax1,ax2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
      ), target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("dirx\\diry");
    target.doCompare(System.out);

    assertCollectionEquals(
      makeMyArray(
	ax,ax1,ax2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
      ), target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/diry");
    target.doCompare(System.out);

    assertCollectionEquals(
      makeMyArray(
	ax,ax1,ax2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2
      ), target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/diry");
    target.addRejectFile("diry");
    target.doCompare(System.out);

    assertCollectionEquals(
      makeMyArray(
	ax,ax1,ax2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2
      ), target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);
  }

  /**
  * RejectFile のテスト。
  * ファイル名が大文字小文字を無視するか。
  */
  public void testReject2()
  throws Exception
  {
    File dir = prepareTestDirectory();
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;

    File a, ax, ax1, ax2, ay, ay1, ay2, az, az1, az2, b;
    DirMaker mk = new DirMaker(dir);
    mk.top().c(
      a = mk.d("dira").c(
	ax = mk.d("dirx").c(
	  ax1 = mk.f("file1","data ax1",current),
	  ax2 = mk.f("File2","data ax2",current)),
	ay = mk.d("diry").c(
	  ay1 = mk.f("File1","data ay1",current),
	  ay2 = mk.f("FILE2","data ay2",current)),
	az = mk.d("dirz").c(
	  az1 = mk.f("FILE1","data az1",current),
	  az2 = mk.f("file2","data az2",current))),
      b = mk.d("dirb").c());

    Backuper target = new Backuper(a,b);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/file1");
    target.doCompare(System.out);

    assertCollectionEquals(makeMyArray(ax,ax2,ay,ay2,az,az2),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/FILE2");
    target.doCompare(System.out);

    assertCollectionEquals(makeMyArray(ax,ax1,ay,ay1,az,az1),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);
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

    target = newFilePair(fileA,fileB);
    assertTrue(target.equals(newFilePair(fileA,fileB)));
    assertFalse(target.equals(null));
    assertFalse(target.equals(newFilePair(fileA,fileC)));
    assertFalse(target.equals(newFilePair(fileA,null)));
    assertFalse(target.equals(newFilePair(null,fileB)));
    assertFalse(target.equals(newFilePair((File)null,null)));
    assertFalse(target.equals(new Object()));

    target = newFilePair(fileA,null);
    assertTrue(target.equals(newFilePair(fileA,null)));
    assertFalse(target.equals(null));
    assertFalse(target.equals(newFilePair(fileA,fileC)));
    assertFalse(target.equals(newFilePair(fileA,fileB)));
    assertFalse(target.equals(newFilePair(null,fileB)));
    assertFalse(target.equals(newFilePair((File)null,null)));
    assertFalse(target.equals(new Object()));

    target = newFilePair(null,fileB);
    assertTrue(target.equals(newFilePair(null,fileB)));
    assertFalse(target.equals(null));
    assertFalse(target.equals(newFilePair(fileC,fileB)));
    assertFalse(target.equals(newFilePair(fileA,fileB)));
    assertFalse(target.equals(newFilePair(fileA,null)));
    assertFalse(target.equals(newFilePair((File)null,null)));
    assertFalse(target.equals(new Object()));

    target = newFilePair((File)null,null);
    assertTrue(target.equals(newFilePair((File)null,null)));
    assertFalse(target.equals(null));
    assertFalse(target.equals(newFilePair(fileC,fileB)));
    assertFalse(target.equals(newFilePair(fileA,fileB)));
    assertFalse(target.equals(newFilePair(fileA,null)));
    assertFalse(target.equals(newFilePair(null,fileB)));
    assertFalse(target.equals(new Object()));
  }

  // ----------------------------------------------------------------------
  // 以下は、ユーティリティ。
  public static VirFile[] makeMyArray( File ... files )
  {
    VirFile myfiles[] = new VirFile[files.length];
    for ( int i = 0; i < files.length; ++i ) {
      myfiles[i] = new RealFile(files[i]);
    }
    return myfiles;
  }

  public static List<VirFile> makeMyList( File ... files )
  {
    return makeList(makeMyArray(files));
  }

  public static Set<VirFile> makeMySet( File ... files )
  {
    return makeSet(makeMyArray(files));
  }

  /**
  * FilePair のリストを生成する。
  */
  public static List<FilePair> makeFilePairList( File pairs[] )
  {
    FilePair pair[] = new FilePair[pairs.length/2];
    for ( int i = 0; i < pairs.length; i += 2 ) {
      pair[i/2] = newFilePair(pairs[i],pairs[i+1]);
    }
    return makeList(pair);
  }

  /**
  * FilePair を生成する。
  */
  public static FilePair newFilePair( File a, File b )
  {
    return new FilePair(new RealFile(a), new RealFile(b));
  }
}
