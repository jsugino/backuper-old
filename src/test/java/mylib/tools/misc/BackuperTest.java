package mylib.tools.misc;

import static org.junit.Assert.*;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mylib.tools.misc.Backuper.FilePair;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
//import mylib.util.MylibTestCase;
//import mylib.util.DirMaker;

public class BackuperTest // extends MylibTestCase
{
  public BackuperTest( /* String name */ )
  {
    //super(name);
  }

  @Rule
  public TemporaryFolder tempdir = new TemporaryFolder(new File("target"));

  // ----------------------------------------------------------------------
  /**
  * relate に関するテスト
  */
  @Test
  public void testRelate()
  throws IOException
  {
    File dir = tempdir.newFolder("reltest");
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
  @Test
  public void testSimple()
  throws Exception
  {
    long current = System.currentTimeMillis() - 10000L;

    File frd = tempdir.newFolder("a");
    File fr1 = new File(frd,"1"); touch(fr1,"1",current);
    File fr2 = new File(frd,"2"); touch(fr2,"22",current+4000L);

    File tod = tempdir.newFolder("b");
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
  @Test
  public void testLength()
  throws Exception
  {
    long current = System.currentTimeMillis();

    File frd = tempdir.newFolder("a");
    File fr1 = new File(frd,"1"); touch(fr1,"1",current);
    File fr2 = new File(frd,"2"); touch(fr2,"22",current);
    File fr3 = new File(frd,"3"); touch(fr3,"333",current);

    File tod = tempdir.newFolder("b");
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
  public Backuper prepareSimple( File dir )
  throws Exception
  {
    /**
    + a/1
    = a/2
    + a/4/
    + a/4/1
    + a/5
    = a/6/
    + a/6/1
    = a/6/2

    = b/2
    + b/3
    + b/4
    + b/5/
    + b/5/1
    = b/6/
    = b/6/2
    + b/6/3
    **/
    final long MIN = 60000L;
    long current = System.currentTimeMillis()/MIN*MIN - 10*MIN;
    File a   = tempdir.newFolder("a");
    File a1  = tempdir.newFile  ("a/1"); a1.setLastModified(current);
    File a2  = tempdir.newFile  ("a/2"); a2.setLastModified(current);
    File a4  = tempdir.newFolder("a","4");
    File a41 = tempdir.newFile  ("a/4/1"); a41.setLastModified(current);
    File a5  = tempdir.newFile  ("a/5"); a5.setLastModified(current);
    File a6  = tempdir.newFolder("a","6");
    File a61 = tempdir.newFile  ("a/6/1"); a61.setLastModified(current);
    File a62 = tempdir.newFile  ("a/6/2"); touch(a62,"abc",current);

    File b   = tempdir.newFolder("b");
    File b2  = tempdir.newFile  ("b/2"); b2.setLastModified(a2.lastModified());
    File b3  = tempdir.newFile  ("b/3"); b3.setLastModified(current+1*MIN);
    File b4  = tempdir.newFile  ("b/4"); b4.setLastModified(current+2*MIN);
    File b5  = tempdir.newFolder("b","5");
    File b51 = tempdir.newFile  ("b/5/1"); b51.setLastModified(current+3*MIN);
    File b6  = tempdir.newFolder("b","6");
    File b62 = tempdir.newFile  ("b/6/2"); touch(b62,"def",current+4*MIN);
    File b63 = tempdir.newFile  ("b/6/3"); b63.setLastModified(current+5*MIN);

    Backuper target = new Backuper(a,b,dir);

    target.doCompare(System.out);

    assertEquals(makeMyList(new File[]{a1,a4,a41,a5,a61}),target.fromOnlyList);
    assertEquals(makeFilePairList(new File[]{a2,b2}),target.sameList);
    assertEquals(makeMyList(new File[]{b3,b4,b5,b51,b63}),target.toOnlyList);
    assertEquals(makeFilePairList(new File[]{a62,b62}),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    return target;
  }

  public static void assertDirectory( File dir, String files[][] )
  throws IOException
  {
    assertTrue("Must be a directory:"+dir,dir.isDirectory());

    // Assert all filenames
    /*
    String targets[] = dir.list();
    Set<String> targetset = makeSet(targets);
    */
    Set<String> targetset = listAllFiles(dir);
    Set<String> fileset = new HashSet<String>();
    for ( int i = 0; i < files.length; ++i ) {
      fileset.add(files[i][0]);
    }
    assertEquals(fileset,targetset);

    // Assert each file contens in files
    for ( int i = 0; i < files.length; ++i ) {
      if ( files[i].length == 2 && files[i][1] == null ) {
	// もし、{ "filename", null } と定義されていたら内容チェックはしない。
	continue;
      }
      assertFile(new File(dir,files[i][0]),files[i][0],files[i],1);
    }
  }

  public static void assertFile( File file, String name, String data[], int offset )
  throws IOException
  {
    assertFile(new FileInputStream(file),name,data,offset);
  }

  public static void assertFile( InputStream inst, String name, String data[], int offset )
  throws IOException
  {
    BufferedReader in = new BufferedReader(new InputStreamReader(inst));
    // Assert each line in each file
    String line;
    int cnt = 0;
    for ( int j = offset; (line = in.readLine()) != null; ++j ) {
      ++cnt;
      if ( j >= data.length ) continue;
      assertEquals(name+"("+cnt+"): ",data[j],line);
    }
    in.close();
    assertEquals("length of "+name,data.length-offset,cnt);
  }

  public static Set<String> listAllFiles( File dir )
  {
    Set<String> ans = new HashSet<String>();
    listAllFilesSub(dir,null,ans);
    return ans;
  }

  public static void listAllFilesSub( File dir, String context, Set<String>ans )
  {
    File files[] = dir.listFiles();
    for ( int i = 0; i < files.length; ++i ) {
      File file = files[i];
      String name = file.getName();
      if ( context != null ) name = context+'/'+name;
      if ( files[i].isDirectory() ) {
	listAllFilesSub(files[i],name,ans);
      } else {
	ans.add(name);
      }
    }
  }

  /**
  * 同じ名前のファイルとディレクトリがあった場合のテスト：比較のみ。
  * doCompare のみ
  */
  @Test
  public void testSimpleDir0()
  throws Exception
  {
    Backuper target = prepareSimple(null);

    assertDirectory(new File(tempdir.getRoot(),"b"),new String[][]{
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
  @Test
  public void testSimpleDir1()
  throws Exception
  {
    Backuper target = prepareSimple(null);

    long origmod = new File(tempdir.getRoot(),"a/6/2").lastModified();
    assertEquals(origmod+4L*60000L,new File(tempdir.getRoot(),"b/6/2").lastModified());

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertEquals(origmod,new File(tempdir.getRoot(),"b/6/2").lastModified());

    assertDirectory(new File(tempdir.getRoot(),"b"),new String[][]{
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
  @Test
  public void testSimpleDir2()
  throws Exception
  {
    Backuper target = prepareSimple(null);

    target.compareTouchList(System.out);

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertDirectory(new File(tempdir.getRoot(),"b"),new String[][]{
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
  public Backuper prepareMove( File dir )
  throws Exception
  {
    final long MIN = 60000L;
    long current = System.currentTimeMillis()/MIN*MIN - 3*MIN;

    File a   = tempdir.newFolder("a");
    File a1  = tempdir.newFolder("a","1");
    File a11 = tempdir.newFile("a/1/1"); touch(a11,"data 11",current);
    File a12 = tempdir.newFile("a/1/2"); touch(a12,"data 2222",current);
    File a13 = tempdir.newFile("a/1/3"); touch(a13,"data 333333",current);

    File b   = tempdir.newFolder("b");
    File b2  = tempdir.newFolder("b","2");
    File b21 = tempdir.newFile("b/2/1"); touch(b21,"data 11",a11.lastModified());
    File b22 = tempdir.newFile("b/2/2"); touch(b22,"data 1212",a12.lastModified());
    File b23 = tempdir.newFile("b/2/3"); touch(b23,"data 333333",current+1*MIN);
    File b3  = tempdir.newFolder("b","3");
    File b30 = tempdir.newFile("b/3/0"); touch(b30,"",current+2*MIN);

    Backuper target = new Backuper(a,b,dir);

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
  @Test
  public void testMove1()
  throws Exception
  {
    Backuper target = prepareMove(null);

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertDirectory(new File(tempdir.getRoot(),"b"),new String[][]{
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
    Backuper target = prepareMove(null);
    target.compareMoveList(System.out);

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertDirectory(new File(tempdir.getRoot(),"b"),new String[][]{
      { "1/1", "data 11" },
      { "1/2", "data 2222" },
      { "1/3", "data 333333" },
    });
  }

  @Test
  public void testBackup1()
  throws Exception
  {
    File dir = tempdir.newFolder("c");
    Backuper target = prepareSimple(dir);
    String tc3  = getTimestamp(new File(tempdir.getRoot(),"b/3*").toString());
    String tc4  = getTimestamp(new File(tempdir.getRoot(),"b/4*").toString());
    String tc51 = getTimestamp(new File(tempdir.getRoot(),"b/5/1*").toString());
    String tc62 = getTimestamp(new File(tempdir.getRoot(),"b/6/2*").toString());
    String tc63 = getTimestamp(new File(tempdir.getRoot(),"b/6/3*").toString());


    target.compareTouchList(System.out);

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertDirectory(new File(tempdir.getRoot(),"b"),new String[][]{
      { "1" },
      { "2" },
      { "4/1" },
      { "5" },
      { "6/1" },
      { "6/2", "abc" },
    });

    assertDirectory(new File(tempdir.getRoot(),"c"),new String[][]{
	{ "3-"+tc3 },
	{ "4-"+tc4 },
	{ "5/1-"+tc51 },
	{ "6/2-"+tc62, "def" },
	{ "6/3-"+tc63 },
      });
  }

  @Test
  public void testBackup2()
  throws Exception
  {
    File dir = tempdir.newFolder("c");
    Backuper target = prepareMove(dir);
    String tc22 = getTimestamp(new File(tempdir.getRoot(),"b/2/2*").toString());
    String tc23 = getTimestamp(new File(tempdir.getRoot(),"b/2/3*").toString());
    String tc30 = getTimestamp(new File(tempdir.getRoot(),"b/3/0*").toString());
    target.compareMoveList(System.out);

    System.out.println("----------------------------------------");
    target.doExecute(System.out);
    /*{
      byte buf[] = new byte[1024];
      Process proc = Runtime.getRuntime().exec("cmd /c dir /S "+tempdir.getRoot());
      InputStream in = proc.getInputStream();
      int len;
      while ( (len = in.read(buf)) > 0 ) {
	System.out.write(buf,0,len);
      }
      proc.waitFor();
    }*/

    assertDirectory(new File(tempdir.getRoot(),"b"),new String[][]{
	{ "1/1", "data 11" },
	{ "1/2", "data 2222" },
	{ "1/3", "data 333333" },
      });

    assertNotEquals(tc22,tc30);
    assertDirectory(new File(tempdir.getRoot(),"c"),new String[][]{
	{ "2/2-"+tc22, "data 1212" },
	{ "2/3-"+tc23, "data 333333" },
	{ "3/0-"+tc30 },
      });
  }

  @Test
  public void testBackup3()
  throws Exception
  {
    final long MIN = 60000L;
    long current = System.currentTimeMillis()/MIN*MIN - 10*MIN;

    File dira = tempdir.newFolder("a");
    File a1   = tempdir.newFile  ("a/1.ext"); touch(a1,"a/1.ext",current);
    File dirb = tempdir.newFolder("b");
    File b1   = tempdir.newFile  ("b/abc");        b1.setLastModified(current+1*MIN);
    File b2   = tempdir.newFile  ("b/def.ext");    b2.setLastModified(current+2*MIN);
    File b3   = tempdir.newFile  ("b/gh.ext.ext"); b3.setLastModified(current+3*MIN);
    File b4   = tempdir.newFile  ("b/.ijk");       b4.setLastModified(current+4*MIN);
    File b5   = tempdir.newFile  ("b/.lmn.ext");   b5.setLastModified(current+5*MIN);
    File dirc = tempdir.newFolder("c");
    String tb1 = getTimestamp(new File(tempdir.getRoot(),"b/abc*").toString());
    String tb2 = getTimestamp(new File(tempdir.getRoot(),"b/def.ext*").toString());
    String tb3 = getTimestamp(new File(tempdir.getRoot(),"b/gh.ext.ext*").toString());
    String tb4 = getTimestamp(new File(tempdir.getRoot(),"b/.ijk*").toString());
    String tb5 = getTimestamp(new File(tempdir.getRoot(),"b/.lmn.ext*").toString());

    System.out.println("----------------------------------------");
    Backuper target = new Backuper(dira,dirb,dirc);

    target.doCompare(System.out);
    target.compareMoveList(System.out);
    target.doExecute(System.out);

    assertDirectory(dirb,new String[][]{
	{ "1.ext", "a/1.ext" },
      });

    assertDirectory(dirc,new String[][]{
	{ "abc-"+tb1 },
	{ "def-"+tb2+".ext" },
	{ "gh.ext-"+tb3+".ext" },
	{ ".ijk-"+tb4 },
	{ ".lmn-"+tb5+".ext" },
      });
  }

  public static String getTimestamp( String file )
  throws IOException, InterruptedException
  {
    String cmd = "cmd /c dir "+file;
    Process proc = Runtime.getRuntime().exec(cmd);
    BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
    String line;
    StringBuffer sbuf = new StringBuffer();
    while ( (line = in.readLine()) != null ) {
      if ( line.length() > 0 && line.charAt(0) != ' ' ) {
	int idx = 0;
	sbuf.append(line,idx,idx+=4); idx += 1;
	sbuf.append(line,idx,idx+=2); idx += 1;
	sbuf.append(line,idx,idx+=2); idx += 2;
	sbuf.append(line,idx,idx+=2); idx += 1;
	sbuf.append(line,idx,idx+=2);
	sbuf.append("00");
      }
    }
    proc.waitFor();
    return sbuf.toString();
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
    File a1 = new File(a,"1"); touch(a1,"data 11",current);
    File a2 = new File(a,"2"); touch(a2,"data 2222",current);
    File a3 = new File(a,"3"); touch(a3,"data 333333",current);

    current += 10000L;
    File b = tempdir.newFolder("b");
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
  @Test
  public void testReject()
  throws Exception
  {
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;

    File a    	= tempdir.newFolder("dira");
    File ax   	= tempdir.newFolder("dira","dirx");
    File ax1  	= tempdir.newFile  ("dira/dirx/fileax1"); touch(ax1,"data ax1",current);
    File ax2  	= tempdir.newFile  ("dira/dirx/fileax2"); touch(ax2,"data ax2",current);
    File axy  	= tempdir.newFolder("dira","dirx","diry");
    File axy1 	= tempdir.newFile  ("dira/dirx/diry/fileaxy1"); touch(axy1,"data axy1",current);
    File axy2 	= tempdir.newFile  ("dira/dirx/diry/fileaxy2"); touch(axy2,"data axy2",current);
    File ay   	= tempdir.newFolder("dira","diry");
    File ay1  	= tempdir.newFile  ("dira/diry/fileay1"); touch(ay1,"data ay1",current);
    File ay2  	= tempdir.newFile  ("dira/diry/fileay2"); touch(ay2,"data ay2",current);
    File az   	= tempdir.newFolder("dira","dirz");
    File az1  	= tempdir.newFile  ("dira/dirz/fileaz1"); touch(az1,"data az1",current);
    File az2  	= tempdir.newFile  ("dira/dirz/fileaz2"); touch(az2,"data az2",current);
    File azx  	= tempdir.newFolder("dira","dirz","dirx");
    File azx1 	= tempdir.newFile  ("dira/dirz/dirx/fileazx1"); touch(azx1,"data azx1",current);
    File azx2 	= tempdir.newFile  ("dira/dirz/dirx/fileazx2"); touch(azx2,"data azx2",current);
    File azxx 	= tempdir.newFolder("dira","dirz","dirx","dirx");
    File azxx1	= tempdir.newFile  ("dira/dirz/dirx/dirx/fileazxx1"); touch(azxx1,"data azxx1",current);
    File azxx2	= tempdir.newFile  ("dira/dirz/dirx/dirx/fileazxx2"); touch(azxx2,"data azxx2",current);
    File azxxy	= tempdir.newFolder("dira","dirz","dirx","dirx","diry");
    File azxxy1 = tempdir.newFile  ("dira/dirz/dirx/dirx/diry/fileazxxy1"); touch(azxxy1,"data azxxy1",current);
    File azxxy2 = tempdir.newFile  ("dira/dirz/dirx/dirx/diry/fileazxxy2"); touch(azxxy2,"data azxxy2",current);
    File b = tempdir.newFolder("dirb");

    Backuper target = new Backuper(a,b);

    // ------------------------------
    target.clearAll();
    target.doCompare(System.out);

    assertEquals(
      new HashSet(makeMyList(
	  ax,ax1,ax2,axy,axy1,axy2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
	)),
      new HashSet(target.fromOnlyList));
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("diry");
    target.doCompare(System.out);

    assertEquals(
      new HashSet(makeMyList(
	  ax,ax1,ax2,axy,axy1,axy2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
	)),
      new HashSet(target.fromOnlyList));
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("dirx/diry");
    target.doCompare(System.out);

    assertEquals(
      new HashSet(makeMyList(
	  ax,ax1,ax2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
	)),
      new HashSet(target.fromOnlyList));
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("dirx\\diry");
    target.doCompare(System.out);

    assertEquals(
      new HashSet(makeMyList(
	  ax,ax1,ax2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
	)),
      new HashSet(target.fromOnlyList));
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/diry");
    target.doCompare(System.out);

    assertEquals(
      new HashSet(makeMyList(
	  ax,ax1,ax2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2
	)),
      new HashSet(target.fromOnlyList));
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/diry");
    target.addRejectFile("diry");
    target.doCompare(System.out);

    assertEquals(
      new HashSet(makeMyList(
	  ax,ax1,ax2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2
	)),
      new HashSet(target.fromOnlyList));
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);
  }

  /**
  * RejectFile のテスト。
  * ファイル名が大文字小文字を無視するか。
  */
  @Test
  public void testReject2()
  throws Exception
  {
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;

    File a   = tempdir.newFolder("dira");
    File ax  = tempdir.newFolder("dira","dirx");
    File ax1 = tempdir.newFile  ("dira/dirx/file1"); touch(ax1,"data ax1",current);
    File ax2 = tempdir.newFile  ("dira/dirx/File2"); touch(ax2,"data ax2",current);
    File ay  = tempdir.newFolder("dira","diry");
    File ay1 = tempdir.newFile  ("dira/diry/File1"); touch(ay1,"data ay1",current);
    File ay2 = tempdir.newFile  ("dira/diry/FILE2"); touch(ay2,"data ay2",current);
    File az  = tempdir.newFolder("dira","dirz");
    File az1 = tempdir.newFile  ("dira/dirz/FILE1"); touch(az1,"data az1",current);
    File az2 = tempdir.newFile  ("dira/dirz/file2"); touch(az2,"data az2",current);
    File b   = tempdir.newFolder("dirb");

    Backuper target = new Backuper(a,b);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/file1");
    target.doCompare(System.out);

    assertEquals(makeMyList(ax,ax2,ay,ay2,az,az2),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/FILE2");
    target.doCompare(System.out);

    assertEquals(makeMyList(ax,ax1,ay,ay1,az,az1),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);
  }

  // ----------------------------------------------------------------------
  /**
  * FilePair のテスト。
  */
  @Test
  public void testFilePair()
  throws Exception
  {
    File fileA = tempdir.newFile("A");
    File fileB = tempdir.newFile("B");
    File fileC = tempdir.newFile("C");
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
  public static File touch( File file, String contents )
  throws IOException
  {
    FileOutputStream out = new FileOutputStream(file);
    out.write(contents.getBytes());
    out.close();
    return file;
  }

  public static File touch( File file, String contents, long time )
  throws IOException
  {
    FileOutputStream out = new FileOutputStream(file);
    out.write(contents.getBytes());
    out.close();
    file.setLastModified(time);
    return file;
  }

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
    return Arrays.asList(makeMyArray(files));
  }

  public static Set<VirFile> makeMySet( File ... files )
  {
    return new HashSet(Arrays.asList(makeMyArray(files)));
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
    return Arrays.asList(pair);
  }

  /**
  * FilePair を生成する。
  */
  public static FilePair newFilePair( File a, File b )
  {
    return new FilePair(new RealFile(a), new RealFile(b));
  }
}
