package mylib.tools.misc;

import static org.junit.Assert.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import mylib.tools.misc.Backuper.FilePair;
import mylib.tools.misc.Backuper.VirFile;
import mylib.tools.misc.RemoteFile.FTPConnection;
import mylib.tools.misc.RemoteFile;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static mylib.tools.misc.BackuperTest.*;


/**
* このテストプログラムを実行するための条件：
* <ul>
* <item>
* ・マイコンピュータ→(右クリック)→管理　を選ぶ
* ・サービスとアプリケーション→インターネットインフォメーションサービス→FTPサイト
*   →既定のFTPサイト→(右クリック)→プロパティ　を選ぶ
* ・ホームディレクトリタブ　を選ぶ。
* 　・ローカルパスにこの開発ディレクトリ(pom.xlsがあるディレクトリ D:\MyWorks\mylib\trunk)を設定する。
* 　・読み取り/書き取り/ログアクセス　すべて、チェックを入れる。
* 　・ディレクトリの表示スタイルを UNIX とする。
* ・セキュリティアカウントタブ　を選ぶ。
* 　・匿名アクセスを許可する　をはずす。(anonymous ログインできないようにする。)
* ・c:/TEMP/work ファイルにテスト用の UserID と Password を設定する。
* </ul>
*/
public class BackuperFtpTest // extends MylibTestCase
{
  public String testUID = null;
  public String testPass = null;

  public BackuperFtpTest( /* String name */ )
  {
    //super(name);
  }

  @Rule
  public TemporaryFolder tempdir = new TemporaryFolder(new File("target"));

  protected void setUp()
  throws Exception
  {
    BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("c:/TEMP/work")));
    testUID = in.readLine();
    testPass = in.readLine();
    in.close();
  }

  // ----------------------------------------------------------------------
  /**
  * FTPClient クラスライブラリのテスト
  */
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testSampleFTP()
  throws Exception
  {
    FileOutputStream os = null;
    FTPClient fp = new FTPClient();
    FileInputStream is = null;

    fp.connect("localhost");
    if (!FTPReply.isPositiveCompletion(fp.getReplyCode())) { // コネクトできたか？
      throw new Exception("connection failed");
    }

    if (fp.login(testUID,testPass) == false) { // ログインできたか？
      throw new Exception("login failed");
    }

    File ftpdir = tempdir.newFolder("ftp");
    File localdir = tempdir.newFolder("local");

    // ファイル受信
    touch(new File(ftpdir,"a"),"data a");
    os = new FileOutputStream(new File(localdir,"a"));// クライアント側
    System.out.println("toFtpFile = "+toFtpFile(ftpdir,"a"));
    fp.retrieveFile(toFtpFile(ftpdir,"a"), os);// サーバー側
    os.close();
    assertFile(localdir,"a",new String[]{"data a"},0);

    // ファイル送信
    touch(new File(localdir,"b"),"data b");
    is = new FileInputStream(new File(localdir,"b"));// クライアント側
    System.out.println("toFtpFile = "+toFtpFile(ftpdir,"b"));
    fp.storeFile(toFtpFile(ftpdir,"b"), is);// サーバー側
    is.close();
    assertFile(ftpdir,"b",new String[]{"data b"},0);

    fp.disconnect();
  }

  @Test
  public void testDriveName()
  {
    File file = new File("V:/top/sub/file");
    File prev = null;
    while ( (file = file.getParentFile()) != null ) {
      prev = file;
    }
    assertEquals("V:\\",prev.getPath().toString());
  }

  // ----------------------------------------------------------------------
  /**
  * relate に関するテスト
  */
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testRelate()
  throws Exception
  {
    File dir = tempdir.getRoot();
    System.out.println("dir = "+dir);
    FTPConnection ftpcon = new FTPConnection("localhost",testUID,testPass,toFtpFile(dir));

    assertEquals(new RemoteFile("b/1",ftpcon),calcRelate(ftpcon,"a/1","a","b"));

    assertEquals(new RemoteFile("b/1/2/3",ftpcon),calcRelate(ftpcon,"a/1/2/3","a","b"));
    assertEquals(new RemoteFile("b/3",ftpcon),calcRelate(ftpcon,"a/1/2/3","a/1/2","b"));
    assertEquals(new RemoteFile("b",ftpcon),calcRelate(ftpcon,"a/1/2/3","a/1/2/3","b"));

    assertEquals(new RemoteFile("b/x/1/2/3",ftpcon),calcRelate(ftpcon,"a/1/2/3","a","b/x"));
    assertEquals(new RemoteFile("b/x/y/3",ftpcon),calcRelate(ftpcon,"a/1/2/3","a/1/2","b/x/y"));
    assertEquals(new RemoteFile("b/x/y/z",ftpcon),calcRelate(ftpcon,"a/1/2/3","a/1/2/3","b/x/y/z"));

    // ------------------------------
    ftpcon.disconnect();
  }

  public VirFile calcRelate( FTPConnection ftpcon, String target, String base, String rel )
  {
    return Backuper.relate(
      new RemoteFile(target,ftpcon),
      new RemoteFile(base,ftpcon),
      new RemoteFile(rel,ftpcon));
  }

  // ----------------------------------------------------------------------
  /**
  * VirFile のコンストラクタのテスト
  */
  @Test
  public void testVirFileConstructor()
  throws Exception
  {
    FTPConnection ftpcon = new FTPConnection("server","user","pass","/top/dir");
    Map<String,FTPConnection> ftpsettings = new HashMap();
    ftpsettings.put("W",ftpcon);

    assertEquals(".",((RemoteFile)Backuper.createFile("W:",ftpsettings)).relpath);
    assertEquals(".",((RemoteFile)Backuper.createFile("W:/",ftpsettings)).relpath);
    assertEquals("top",((RemoteFile)Backuper.createFile("W:/top",ftpsettings)).relpath);
    assertEquals("top",((RemoteFile)Backuper.createFile("W:/top/",ftpsettings)).relpath);
    assertEquals("top/sub",((RemoteFile)Backuper.createFile("W:/top/sub",ftpsettings)).relpath);
    assertEquals("top/sub",((RemoteFile)Backuper.createFile("W:/top/sub/",ftpsettings)).relpath);

    // ------------------------------
    ftpcon.disconnect();
  }

  // ----------------------------------------------------------------------
  /**
  * ファイルのテスト
  */
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testSingleFile()
  throws Exception
  {
    File ftpdir = tempdir.getRoot();
    FTPConnection ftpcon = new FTPConnection("localhost",testUID,testPass,toFtpFile(ftpdir));
    long current = System.currentTimeMillis()/60000L*60000L;
    File filea = new File(ftpdir,"a"); touch(filea,"data a",current);
    RemoteFile rema = new RemoteFile("a",ftpcon);
    assertFile(rema.openAsInputStream(),rema.relpath,new String[]{"data a"},0);
    assertEquals("a",rema.getName());
    assertEquals(6L,rema.length());
    assertEquals(current,filea.lastModified());
    assertEquals(current,rema.lastModified());
    assertTrue(rema.isFile());
    assertFalse(rema.isDirectory());
    assertTrue(rema.exists());
    assertTrue(rema.delete());
    assertFalse(filea.exists());

    // ------------------------------
    ftpcon.disconnect();
  }

  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testSingleDir()
  throws Exception
  {
    File ftpdir = tempdir.getRoot();
    FTPConnection ftpcon = new FTPConnection("localhost",testUID,testPass,toFtpFile(ftpdir));
    File filea = new File(ftpdir,"a"); touch(filea,"data a");
    File fileb = new File(ftpdir,"b"); touch(fileb,"data b");
    File filec = new File(ftpdir,"c"); touch(filec,"data c");
    RemoteFile top = new RemoteFile("/",ftpcon);
    RemoteFile list[] = (RemoteFile[])top.listFiles(new HashSet<Pattern>());
    assertEquals(
      new HashSet(Arrays.asList(new RemoteFile[]{new RemoteFile("a",ftpcon),new RemoteFile("b",ftpcon),new RemoteFile("c",ftpcon)})),
      new HashSet(Arrays.asList(list)));
    assertTrue(list[0].isFile());
    assertTrue(list[1].isFile());
    assertTrue(list[2].isFile());
    assertEquals("a",list[0].getName());
    assertEquals("b",list[1].getName());
    assertEquals("c",list[2].getName());

    // ------------------------------
    ftpcon.disconnect();
  }

  // ----------------------------------------------------------------------
  /**
  * ディレクトリ内にファイルのみがある場合。
  */
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testSimple()
  throws Exception
  {
    File dirloc = tempdir.newFolder("loc");
    File dirftp = tempdir.newFolder("ftp");

    long current = System.currentTimeMillis()/60000L*60000L;

    File frd = new File(dirftp,"a"); frd.mkdir();
    File fr1 = new File(frd,"1"); touch(fr1,"1",current);
    File fr2 = new File(frd,"2"); touch(fr2,"22",current-60000L);

    File tod = new File(dirloc,"b"); tod.mkdir();
    File to2 = new File(tod,"2"); touch(to2,"22",current-60000L);
    File to3 = new File(tod,"3"); touch(to3,"333",current-120000L);

    FTPConnection ftpcon = new FTPConnection("localhost",testUID,testPass,toFtpFile(dirftp));
    Map<String,FTPConnection> ftpsettings = new HashMap();
    ftpsettings.put("W",ftpcon);
    System.out.println("from = "+frd);
    Backuper target = new Backuper("W:/a",tod.toString(),ftpsettings);

    target.doCompare(System.out);

    assertEquals(makeMyList(ftpcon,"a/1"),target.fromOnlyList);
    assertEquals(makeFilePairList(ftpcon,"a/2",to2),target.sameList);
    assertEquals(makeMyList(to3),target.toOnlyList);

    // ------------------------------
    ftpcon.disconnect();
  }

  // ----------------------------------------------------------------------
  /**
  * 長さが異なる場合。
  */
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testLength()
  throws Exception
  {
    File dir = tempdir.getRoot();

    long current = System.currentTimeMillis()/60000L*60000L;

    File frd = new File(dir,"a"); frd.mkdir();
    File fr1 = new File(frd,"1"); touch(fr1,"1",current);
    File fr2 = new File(frd,"2"); touch(fr2,"22",current);
    File fr3 = new File(frd,"3"); touch(fr3,"333",current);

    File tod = new File(dir,"b"); tod.mkdir();
    File to1 = new File(tod,"1"); touch(to1,"1",current);
    File to2 = new File(tod,"2"); touch(to2,"222",current);
    File to3 = new File(tod,"3"); touch(to3,"333",current);

    FTPConnection ftpcon = new FTPConnection("localhost",testUID,testPass,toFtpFile(dir));
    Map<String,FTPConnection> ftpsettings = new HashMap();
    ftpsettings.put("W",ftpcon);
    Backuper target = new Backuper("W:/a",tod.toString(),ftpsettings);

    target.doCompare(System.out);

    assertEquals(makeFilePairList(ftpcon,"a/1",to1,"a/3",to3),target.sameList);
    assertEquals(makeMyList(ftpcon,"a/2"),target.fromOnlyList);
    assertEquals(makeMyList(to2),target.toOnlyList);
    assertEquals(0,target.touchList.size());
    assertEquals(0,target.moveList.size());

    // ------------------------------
    ftpcon.disconnect();
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
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;
    File a = new File(dir,"a"); a.mkdir();
    File a1 = new File(a,"1"); touch(a1,"",current);
    File a2 = new File(a,"2"); touch(a2,"",current);
    File a4 = new File(a,"4"); a4.mkdir();
    File a41 = new File(a4,"1"); touch(a41,"",current);
    File a5 = new File(a,"5"); touch(a5,"",current);
    File a6 = new File(a,"6"); a6.mkdir();
    File a61 = new File(a6,"1"); touch(a61,"",current);
    File a62 = new File(a6,"2"); touch(a62,"abc",current);

    current += 60000L;
    File b = new File(dir,"b"); b.mkdir();
    File b2 = new File(b,"2"); touch(b2,"",a2.lastModified());
    File b3 = new File(b,"3"); touch(b3,"",current);
    File b4 = new File(b,"4"); touch(b4,"",current);
    File b5 = new File(b,"5"); b5.mkdir();
    File b51 = new File(b5,"1"); touch(b51,"",current);
    File b6 = new File(b,"6"); b6.mkdir();
    File b62 = new File(b6,"2"); touch(b62,"def",current);
    File b63 = new File(b6,"3"); touch(b63,"",current);

    FTPConnection ftpcon = new FTPConnection("localhost",testUID,testPass,toFtpFile(dir));
    Map<String,FTPConnection> ftpsettings = new HashMap();
    ftpsettings.put("W",ftpcon);
    Backuper target = new Backuper("W:/a",b.toString(),ftpsettings);

    target.doCompare(System.out);

    assertEquals(makeMyList(ftpcon,"a/1","a/4","a/4/1","a/5","a/6/1"),target.fromOnlyList);
    assertEquals(makeFilePairList(ftpcon,"a/2",b2),target.sameList);
    assertEquals(makeMyList(b3,b4,b5,b51,b63),target.toOnlyList);
    assertEquals(makeFilePairList(ftpcon,"a/6/2",b62),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    return target;
  }

  /**
  * 同じ名前のファイルとディレクトリがあった場合のテスト：比較のみ。
  * doCompare のみ
  */
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testSimpleDir0()
  throws Exception
  {
    File dir = tempdir.getRoot();
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
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testSimpleDir1()
  throws Exception
  {
    File dir = tempdir.getRoot();
    Backuper target = prepareSimple(dir);

    long origmod = new File(dir,"a/6/2").lastModified();
    assertEquals(origmod+60000L,new File(dir,"b/6/2").lastModified());

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
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testSimpleDir2()
  throws Exception
  {
    File dir = tempdir.getRoot();
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
  public Backuper prepareMove( File dir )
  throws Exception
  {
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;
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
    File b30 = new File(b3,"0"); touch(b30,"",current);

    FTPConnection ftpcon = new FTPConnection("localhost",testUID,testPass,toFtpFile(dir));
    Map<String,FTPConnection> ftpsettings = new HashMap();
    ftpsettings.put("W",ftpcon);
    Backuper target = new Backuper("W:/a",b.toString(),ftpsettings);

    target.doCompare(System.out);

    assertEquals(makeMyList(ftpcon,"a/1","a/1/3"),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(makeMyList(b2,b23,b3,b30),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(makeFilePairList(ftpcon,"a/1/1",b21,"a/1/2",b22),target.moveList);

    return target;
  }

  /**
  * ディレクトリ内のファイル移動があった場合のテスト：単純コピー。
  * doCompare(), doExecute();
  */
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testMove1()
  throws Exception
  {
    File dir = tempdir.getRoot();

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
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testMove2()
  throws Exception
  {
    File dir = tempdir.getRoot();

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
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testSame()
  throws Exception
  {
    File dir = tempdir.getRoot();

    long current = System.currentTimeMillis()/60000L*60000L - 120000L;
    File a = new File(dir,"a"); a.mkdir();
    File a1 = new File(a,"1"); touch(a1,"data 11",current);
    File a2 = new File(a,"2"); touch(a2,"data 2222",current);
    File a3 = new File(a,"3"); touch(a3,"data 333333",current);

    current += 10000L;
    File b = new File(dir,"b"); b.mkdir();
    File b1 = new File(b,"1"); touch(b1,"data 11",a1.lastModified());
    File b2 = new File(b,"2"); touch(b2,"data 1212",a2.lastModified());
    File b3 = new File(b,"3"); touch(b3,"data 131313",a3.lastModified());

    FTPConnection ftpcon = new FTPConnection("localhost",testUID,testPass,toFtpFile(dir));
    Map<String,FTPConnection> ftpsettings = new HashMap();
    ftpsettings.put("W",ftpcon);
    Backuper target = new Backuper("W:/a",b.toString(),ftpsettings);

    target.doCompare(System.out);

    assertEquals(new ArrayList<File>(),target.fromOnlyList);
    assertEquals(makeFilePairList(ftpcon,"a/1",b1,"a/2",b2,"a/3",b3),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    target.compareSameList(System.out);

    assertEquals(makeMyList(ftpcon,"a/2","a/3"),target.fromOnlyList);
    assertEquals(makeFilePairList(ftpcon,"a/1",b1),target.sameList);
    assertEquals(makeMyList(b2,b3),target.toOnlyList);
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
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testReject()
  throws Exception
  {
    File dir = tempdir.getRoot();
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;

    File a      = tempdir.newFolder("dira");
    File ax     = tempdir.newFolder("dira","dirx");
    File ax1    = tempdir.newFile  ("dira/dirx/fileax1"); touch(ax1,"data ax1",current);
    File ax2    = tempdir.newFile  ("dira/dirx/fileax2"); touch(ax2,"data ax2",current);
    File axy    = tempdir.newFolder("dira/dirx/diry");
    File axy1   = tempdir.newFile  ("dira/dirx/diry/fileaxy1"); touch(axy1,"data axy1",current);
    File axy2   = tempdir.newFile  ("dira/dirx/diry/fileaxy2"); touch(axy2,"data axy2",current);
    File ay     = tempdir.newFolder("dira/diry");
    File ay1    = tempdir.newFile  ("dira/diry/fileay1"); touch(ay1,"data ay1",current);
    File ay2    = tempdir.newFile  ("dira/diry/fileay2"); touch(ay2,"data ay2",current);
    File az     = tempdir.newFolder("dira/dirz");
    File az1    = tempdir.newFile  ("dira/dirz/fileaz1"); touch(az1,"data az1",current);
    File az2    = tempdir.newFile  ("dira/dirz/fileaz2"); touch(az2,"data az2",current);
    File azx    = tempdir.newFolder("dira/dirz/dirx");
    File azx1   = tempdir.newFile  ("dira/dirz/dirx/fileazx1"); touch(azx1,"data azx1",current);
    File azx2   = tempdir.newFile  ("dira/dirz/dirx/fileazx2"); touch(azx2,"data azx2",current);
    File azxx   = tempdir.newFolder("dira/dirz/dirx/dirx");
    File azxx1  = tempdir.newFile  ("dira/dirz/dirx/dirx/fileazxx1"); touch(azxx1,"data azxx1",current);
    File azxx2  = tempdir.newFile  ("dira/dirz/dirx/dirx/fileazxx2"); touch(azxx2,"data azxx2",current);
    File azxxy  = tempdir.newFolder("dira/dirz/dirx/dirx/diry");
    File azxxy1 = tempdir.newFile  ("dira/dirz/dirx/dirx/diry/fileazxxy1"); touch(azxxy1,"data azxxy1",current);
    File azxxy2 = tempdir.newFile  ("dira/dirz/dirx/dirx/diry/fileazxxy2"); touch(azxxy2,"data azxxy2",current);
    File b      = tempdir.newFolder("dirb");

    FTPConnection ftpcon = new FTPConnection("localhost",testUID,testPass,toFtpFile(dir));
    Map<String,FTPConnection> ftpsettings = new HashMap();
    ftpsettings.put("W",ftpcon);
    Backuper target = new Backuper("W:/dira",b.toString(),ftpsettings);

    // ------------------------------
    target.clearAll();
    target.doCompare(System.out);

    assertEquals(
      makeMyList(ftpcon,dir,
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

    assertEquals(
      makeMyList(ftpcon,dir,
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

    assertEquals(
      makeMyList(ftpcon,dir,
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

    assertEquals(
      makeMyList(ftpcon,dir,
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

    assertEquals(
      makeMyList(ftpcon,dir,
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

    assertEquals(
      makeMyList(ftpcon,dir,
	ax,ax1,ax2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2
      ), target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    ftpcon.disconnect();
  }

  /**
  * RejectFile のテスト。
  * ファイル名が大文字小文字を無視するか。
  */
  @Ignore("Need to use Jetty as a ftp server")
  @Test
  public void testReject2()
  throws Exception
  {
    File dir = tempdir.getRoot();
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;

    File a   = tempdir.newFolder("dira");
    File ax  = tempdir.newFolder("dira/dirx");
    File ax1 = tempdir.newFile  ("dira/dirx/file1"); touch(ax1,"data ax1",current);
    File ax2 = tempdir.newFile  ("dira/dirx/File2"); touch(ax2,"data ax2",current);
    File ay  = tempdir.newFolder("dira/diry");
    File ay1 = tempdir.newFile  ("dira/diry/File1"); touch(ay1,"data ay1",current);
    File ay2 = tempdir.newFile  ("dira/diry/FILE2"); touch(ay2,"data ay2",current);
    File az  = tempdir.newFolder("dira/dirz");
    File az1 = tempdir.newFile  ("dira/dirz/FILE1"); touch(az1,"data az1",current);
    File az2 = tempdir.newFile  ("dira/dirz/file2"); touch(az2,"data az2",current);
    File b   = tempdir.newFolder("dirb");

    FTPConnection ftpcon = new FTPConnection("localhost",testUID,testPass,toFtpFile(dir));
    Map<String,FTPConnection> ftpsettings = new HashMap();
    ftpsettings.put("W",ftpcon);
    Backuper target = new Backuper("W:/dira",b.toString(),ftpsettings);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/file1");
    target.doCompare(System.out);

    assertEquals(makeMyList(ftpcon,dir,ax,ax2,ay,ay2,az,az2),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/FILE2");
    target.doCompare(System.out);

    assertEquals(makeMyList(ftpcon,dir,ax,ax1,ay,ay1,az,az1),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    ftpcon.disconnect();
  }

  // ----------------------------------------------------------------------
  // 以下は、ユーティリティ。

  public static String toFtpFile( File dir, String name )
  {
    return toFtpFileSub(null,dir).append(name).toString();
  }

  public static String toFtpFile( File file )
  {
    return toFtpFile((File)null,file);
  }

  private static String toFtpFile( File base, File file )
  {
    return toFtpFileSub(base,file.getParentFile()).append(file.getName()).toString();
  }

  private static StringBuffer toFtpFileSub( File base, File dir )
  {
    if ( base == null ? (dir == null) : (base.equals(dir)) ) {
      return new StringBuffer("");
    } else {
      return toFtpFileSub(base,dir.getParentFile()).append(dir.getName()).append("/");
    }
  }

  public static List<VirFile> makeMyList( File ... files )
  {
    VirFile myfiles[] = new VirFile[files.length];
    for ( int i = 0; i < files.length; ++i ) {
      myfiles[i] = new RealFile(files[i]);
    }
    return Arrays.asList(myfiles);
  }

  public static List<VirFile> makeMyList( FTPConnection ftpcon, String ... files )
  {
    VirFile myfiles[] = new VirFile[files.length];
    for ( int i = 0; i < files.length; ++i ) {
      myfiles[i] = new RemoteFile(files[i],ftpcon);
    }
    return Arrays.asList(myfiles);
  }

  public static List<VirFile> makeMyList( FTPConnection ftpcon, File base, File ... files )
  {
    VirFile myfiles[] = new VirFile[files.length];
    for ( int i = 0; i < files.length; ++i ) {
      myfiles[i] = new RemoteFile(toFtpFile(base,files[i]),ftpcon);
    }
    return Arrays.asList(myfiles);
  }

  /**
  * FilePair のリストを生成する。
  */
  public static List<FilePair> makeFilePairList( FTPConnection ftpcon, Object ... pairs )
  {
    FilePair pair[] = new FilePair[pairs.length/2];
    for ( int i = 0; i < pairs.length; i += 2 ) {
      pair[i/2] = new FilePair(new RemoteFile((String)pairs[i],ftpcon),new RealFile((File)pairs[i+1]));
    }
    return Arrays.asList(pair);
  }
}
