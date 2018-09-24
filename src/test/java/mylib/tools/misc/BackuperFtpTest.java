package mylib.tools.misc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import mylib.tools.misc.Backuper.FilePair;
import mylib.tools.misc.RemoteFile.FTPConnection;
import mylib.tools.misc.RemoteFile;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import static org.junit.Assert.*;
import static mylib.tools.misc.BackuperTest.*;


/**
* FTPのテスト
*/
public class BackuperFtpTest // extends MylibTestCase
{
  public static final String UID = "user";
  public static final String PASS = "password";
  public static final String HOME = "/home/user";

  private static FakeFtpServer fakeFtpServer;

  @BeforeClass
  public static void startFtpSerer()
  throws Exception
  {
    fakeFtpServer = new FakeFtpServer();
    fakeFtpServer.addUserAccount(new org.mockftpserver.fake.UserAccount(UID,PASS,HOME));
    fakeFtpServer.start();
  }

  @AfterClass
  public static void stopFtpSerer()
  throws Exception
  {
    fakeFtpServer.stop();
  }

  /**
  * BackuperFtpTest
  */
  public BackuperFtpTest( /* String name */ )
  {
    //super(name);
  }

  @Rule
  public TemporaryFolder tempdir = new TemporaryFolder(new File("target"));

  // ----------------------------------------------------------------------
  /**
  * FakeFtpServer では、常に、ファイルの日付しか返さない。
  * このクラスは、実際の FTP Server と同様に、時刻を返すようにするための定義。
  */
  public static class UnixFakeFileSystemEx
    extends org.mockftpserver.fake.filesystem.UnixFakeFileSystem
  {
    public String homedir;

    public UnixFakeFileSystemEx( String home )
    {
      super();
      setDirectoryListingFormatter(new UnixDirectoryListingFormatterEx());
      add(new DirectoryEntry(home));
      homedir = home+'/';
    }

    public DirectoryEntry newFolder( String dir )
    {
      DirectoryEntry entry = new DirectoryEntry(homedir+dir);
      add(entry);
      return entry;
    }

    public FileEntry newFile( String file )
    {
      FileEntry entry = new FileEntry(homedir+file);
      add(entry);
      return entry;
    }

    public FileEntry newFile( String file, String contents )
    {
      FileEntry entry = new FileEntry(homedir+file,contents);
      add(entry);
      return entry;
    }

    public FileEntry newFile( String file, String contents, long timestamp )
    {
      FileEntry entry = new FileEntry(homedir+file,contents);
      add(entry);
      entry.setLastModified(new Date(timestamp));
      return entry;
    }
  }

  public static class UnixDirectoryListingFormatterEx 
    extends org.mockftpserver.fake.filesystem.UnixDirectoryListingFormatter
  {
    public static final java.text.SimpleDateFormat DATEFORMAT = new java.text.SimpleDateFormat("HH:mm");
    @Override
    public String format( org.mockftpserver.fake.filesystem.FileSystemEntry fileSystemEntry )
    {
      String text = super.format(fileSystemEntry);
      Date date = fileSystemEntry.getLastModified();
      if ( System.currentTimeMillis() - date.getTime() < (6L * 30L * 24L * 3600000L) ) {
	int idx2 = text.lastIndexOf(' ');
	int idx1 = text.lastIndexOf(' ',idx2-1);
	text = text.substring(0,idx1)+DATEFORMAT.format(date)+text.substring(idx2);
      }
      return text;
    }
  }

  // ----------------------------------------------------------------------
  /**
  * FTPClient クラスライブラリのテスト
  */
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testSampleFTP()
  throws Exception
  {
    FileOutputStream os = null;
    FTPClient fp = new FTPClient();
    FileInputStream is = null;

    fp.connect("localhost");
    if ( !FTPReply.isPositiveCompletion(fp.getReplyCode()) ) { // コネクトできたか？
      fail("connection failed");
    }

    UnixFakeFileSystemEx fileSystem = new UnixFakeFileSystemEx(HOME);
    fakeFtpServer.setFileSystem(fileSystem);

    if ( !fp.login(UID,PASS) ) { // ログインできたか？
      fail("login failed");
    }

    File localdir = tempdir.newFolder("local");

    // ファイル受信
    fileSystem.newFile("a","data a");
    os = new FileOutputStream(new File(localdir,"a"));// クライアント側
    if ( !fp.retrieveFile(HOME+"/a",os) ) {// サーバー側
      fail("retrieveFile "+HOME+"/a");
    }
    os.close();
    assertFile(new File(localdir,"a"),"a",new String[]{"data a"},0);

    // ファイル送信
    touch(new File(localdir,"b"),"data b");
    is = new FileInputStream(new File(localdir,"b"));// クライアント側
    if ( !fp.storeFile(HOME+"/b",is) ) {// サーバー側
      fail("storeFile "+HOME+"/b");
    }
    is.close();
    assertFile(((FileEntry)fileSystem.getEntry(HOME+"/b")).createInputStream(),HOME+"/b",new String[]{"data b"},0);

    fp.disconnect();
  }

  //@Test
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
  @Test
  public void testRelate()
  throws Exception
  {
    FTPConnection ftpcon = new FTPConnection("localhost",UID,PASS,HOME);

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
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testSingleFile()
  throws Exception
  {
    FTPConnection ftpcon = new FTPConnection("localhost",UID,PASS,HOME);
    long current = System.currentTimeMillis()/60000L*60000L;

    FileEntry filea = new FileEntry(HOME+"/a","data a");
    filea.setLastModified(new Date(current));

    UnixFakeFileSystemEx fileSystem = new UnixFakeFileSystemEx(HOME);
    fileSystem.add(filea);
    fakeFtpServer.setFileSystem(fileSystem);

    RemoteFile rema = new RemoteFile("a",ftpcon);
    assertFile(rema.openAsInputStream(),rema.relpath,new String[]{"data a"},0);
    assertEquals("a",rema.getName());
    assertEquals(6L,rema.length());
    assertEquals(current,filea.getLastModified().getTime());
    assertEquals(current,rema.lastModified());
    assertTrue(rema.isFile());
    assertFalse(rema.isDirectory());
    assertTrue(rema.exists());
    assertTrue(rema.delete());
    assertFalse(filea.isDirectory());

    // ------------------------------
    ftpcon.disconnect();
  }

  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testSingleDir()
  throws Exception
  {
    UnixFakeFileSystemEx fileSystem = new UnixFakeFileSystemEx(HOME);
    fileSystem.newFile("a","data a");
    fileSystem.newFile("b","data b");
    fileSystem.newFile("c","data c");
    fakeFtpServer.setFileSystem(fileSystem);

    FTPConnection ftpcon = new FTPConnection("localhost",UID,PASS,HOME);
    RemoteFile top = new RemoteFile("/",ftpcon);
    RemoteFile list[] = (RemoteFile[])top.listFiles(new HashSet<Pattern>());
    assertEquals(
      new HashSet(Arrays.asList(new RemoteFile[]{
	    new RemoteFile("a",ftpcon),
	    new RemoteFile("b",ftpcon),
	    new RemoteFile("c",ftpcon),
	  })),
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
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testSimple()
  throws Exception
  {
    long current = System.currentTimeMillis()/60000L*60000L;

    UnixFakeFileSystemEx fileSystem = new UnixFakeFileSystemEx(HOME);
    fileSystem.newFolder("a");
    fileSystem.newFile  ("a/1","1",current);
    fileSystem.newFile  ("a/2","22",current-60000L);
    fakeFtpServer.setFileSystem(fileSystem);

    tempdir.newFolder("loc");
    File tod = tempdir.newFolder("loc","b");
    File to2 = tempdir.newFile("loc/b/2"); touch(to2,"22",current-60000L);
    File to3 = tempdir.newFile("loc/b/3"); touch(to3,"333",current-120000L);

    FTPConnection ftpcon = new FTPConnection("localhost",UID,PASS,HOME);
    Map<String,FTPConnection> ftpsettings = new HashMap();
    ftpsettings.put("W",ftpcon);
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
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testLength()
  throws Exception
  {
    long current = System.currentTimeMillis()/60000L*60000L;

    UnixFakeFileSystemEx fileSystem = new UnixFakeFileSystemEx(HOME);
    fileSystem.newFolder("a");
    fileSystem.newFile  ("a/1","1",current);
    fileSystem.newFile  ("a/2","22",current);
    fileSystem.newFile  ("a/3","333",current);
    fakeFtpServer.setFileSystem(fileSystem);

    File tod = tempdir.newFolder("b"); tod.mkdir();
    File to1 = new File(tod,"1"); touch(to1,"1",current);
    File to2 = new File(tod,"2"); touch(to2,"222",current);
    File to3 = new File(tod,"3"); touch(to3,"333",current);

    FTPConnection ftpcon = new FTPConnection("localhost",UID,PASS,HOME);
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
  public Backuper prepareSimple()
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

    UnixFakeFileSystemEx fileSystem = new UnixFakeFileSystemEx(HOME);
    fileSystem.newFolder("a");
    fileSystem.newFile("a/1","",current);
    fileSystem.newFile("a/2","",current);
    fileSystem.newFolder("a/4");
    fileSystem.newFile("a/4/1","",current);
    fileSystem.newFile("a/5","",current);
    fileSystem.newFolder("a/6");
    fileSystem.newFile("a/6/1","",current);
    fileSystem.newFile("a/6/2","abc",current);
    fakeFtpServer.setFileSystem(fileSystem);

    current += 60000L;
    File b = tempdir.newFolder("b");
    File b2 = tempdir.newFile("b/2"); touch(b2,"",current-60000L);
    File b3 = tempdir.newFile("b/3"); touch(b3,"",current);
    File b4 = tempdir.newFile("b/4"); touch(b4,"",current);
    File b5 = tempdir.newFolder("b","5");
    File b51 = tempdir.newFile("b/5/1"); touch(b51,"",current);
    File b6 = tempdir.newFolder("b","6");
    File b62 = tempdir.newFile("b/6/2"); touch(b62,"def",current);
    File b63 = tempdir.newFile("b/6/3"); touch(b63,"",current);

    FTPConnection ftpcon = new FTPConnection("localhost",UID,PASS,HOME);
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
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testSimpleDir0()
  throws Exception
  {
    Backuper target = prepareSimple();

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
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testSimpleDir1()
  throws Exception
  {
    File dir = tempdir.getRoot();
    Backuper target = prepareSimple();

    long origmod = fakeFtpServer.getFileSystem().getEntry(HOME+"/a/6/2").getLastModified().getTime();
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
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testSimpleDir2()
  throws Exception
  {
    Backuper target = prepareSimple();

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
  public Backuper prepareMove()
  throws Exception
  {
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;

    UnixFakeFileSystemEx fileSystem = new UnixFakeFileSystemEx(HOME);
    fileSystem.newFolder("a");
    fileSystem.newFolder("a/1");
    fileSystem.newFile("a/1/1","data 11",current);
    fileSystem.newFile("a/1/2","data 2222",current);
    fileSystem.newFile("a/1/3","data 333333",current);
    fakeFtpServer.setFileSystem(fileSystem);

    File b = tempdir.newFolder("b");
    File b2 = tempdir.newFolder("b","2");
    File b21 = tempdir.newFile("b/2/1"); touch(b21,"data 11",current);
    File b22 = tempdir.newFile("b/2/2"); touch(b22,"data 1212",current);
    File b23 = tempdir.newFile("b/2/3"); touch(b23,"data 333333",current+60000L);
    File b3 = tempdir.newFolder("b","3");
    File b30 = tempdir.newFile("b/3/0"); touch(b30,"",current+120000L);

    FTPConnection ftpcon = new FTPConnection("localhost",UID,PASS,HOME);
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
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testMove1()
  throws Exception
  {
    Backuper target = prepareMove();

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
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testMove2()
  throws Exception
  {
    Backuper target = prepareMove();
    target.compareMoveList(System.out);

    System.out.println("----------------------------------------");
    target.doExecute(System.out);

    assertDirectory(new File(tempdir.getRoot(),"b"),new String[][]{
      { "1/1", "data 11" },
      { "1/2", "data 2222" },
      { "1/3", "data 333333" },
    });
  }

  // ----------------------------------------------------------------------
  /**
  * 同一ファイル名、同一時刻でも、完全に比較する場合のテスト。
  */
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testSame()
  throws Exception
  {
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;

    UnixFakeFileSystemEx fileSystem = new UnixFakeFileSystemEx(HOME);
    fileSystem.newFolder("a");
    fileSystem.newFile("a/1","data 11",current);
    fileSystem.newFile("a/2","data 2222",current);
    fileSystem.newFile("a/3","data 333333",current);
    fakeFtpServer.setFileSystem(fileSystem);

    File b = new File(tempdir.getRoot(),"b"); b.mkdir();
    File b1 = new File(b,"1"); touch(b1,"data 11",current);
    File b2 = new File(b,"2"); touch(b2,"data 1212",current);
    File b3 = new File(b,"3"); touch(b3,"data 131313",current);

    FTPConnection ftpcon = new FTPConnection("localhost",UID,PASS,HOME);
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
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testReject()
  throws Exception
  {
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;

    String a, ax, ax1, ax2, axy, axy1, axy2, ay, ay1, ay2, az, az1, az2;
    String azx, azx1, azx2, azxx, azxx1, azxx2, azxxy, azxxy1, azxxy2;

    UnixFakeFileSystemEx fileSystem = new UnixFakeFileSystemEx(HOME);
    fileSystem.newFolder(a      = "dira");
    fileSystem.newFolder(ax     = "dira/dirx");
    fileSystem.newFile  (ax1    = "dira/dirx/fileax1","data ax1",current);
    fileSystem.newFile  (ax2    = "dira/dirx/fileax2","data ax2",current);
    fileSystem.newFolder(axy    = "dira/dirx/diry");
    fileSystem.newFile  (axy1   = "dira/dirx/diry/fileaxy1","data axy1",current);
    fileSystem.newFile  (axy2   = "dira/dirx/diry/fileaxy2","data axy2",current);
    fileSystem.newFolder(ay     = "dira/diry");
    fileSystem.newFile  (ay1    = "dira/diry/fileay1","data ay1",current);
    fileSystem.newFile  (ay2    = "dira/diry/fileay2","data ay2",current);
    fileSystem.newFolder(az     = "dira/dirz");
    fileSystem.newFile  (az1    = "dira/dirz/fileaz1","data az1",current);
    fileSystem.newFile  (az2    = "dira/dirz/fileaz2","data az2",current);
    fileSystem.newFolder(azx    = "dira/dirz/dirx");
    fileSystem.newFile  (azx1   = "dira/dirz/dirx/fileazx1","data azx1",current);
    fileSystem.newFile  (azx2   = "dira/dirz/dirx/fileazx2","data azx2",current);
    fileSystem.newFolder(azxx   = "dira/dirz/dirx/dirx");
    fileSystem.newFile  (azxx1  = "dira/dirz/dirx/dirx/fileazxx1","data azxx1",current);
    fileSystem.newFile  (azxx2  = "dira/dirz/dirx/dirx/fileazxx2","data azxx2",current);
    fileSystem.newFolder(azxxy  = "dira/dirz/dirx/dirx/diry");
    fileSystem.newFile  (azxxy1 = "dira/dirz/dirx/dirx/diry/fileazxxy1","data azxxy1",current);
    fileSystem.newFile  (azxxy2 = "dira/dirz/dirx/dirx/diry/fileazxxy2","data azxxy2",current);
    fakeFtpServer.setFileSystem(fileSystem);

    File b      = tempdir.newFolder("dirb");

    FTPConnection ftpcon = new FTPConnection("localhost",UID,PASS,HOME);
    Map<String,FTPConnection> ftpsettings = new HashMap();
    ftpsettings.put("W",ftpcon);
    Backuper target = new Backuper("W:/dira",b.toString(),ftpsettings);

    // ------------------------------
    target.clearAll();
    target.doCompare(System.out);

    assertEquals(
      new HashSet(makeMyList(ftpcon,
	  ax,ax1,ax2,axy,axy1,axy2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
	)), new HashSet(target.fromOnlyList));
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("diry");
    target.doCompare(System.out);

    assertEquals(
      new HashSet(makeMyList(ftpcon,
	  ax,ax1,ax2,axy,axy1,axy2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
	)), new HashSet(target.fromOnlyList));
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("dirx/diry");
    target.doCompare(System.out);

    assertEquals(
      new HashSet(makeMyList(ftpcon,
	  ax,ax1,ax2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
	)), new HashSet(target.fromOnlyList));
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("dirx\\diry");
    target.doCompare(System.out);

    assertEquals(
      new HashSet(makeMyList(ftpcon,
	  ax,ax1,ax2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2,azxxy,azxxy1,azxxy2
	)), new HashSet(target.fromOnlyList));
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/diry");
    target.doCompare(System.out);

    assertEquals(
      new HashSet(makeMyList(ftpcon,
	  ax,ax1,ax2,ay,ay1,ay2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2
	)), new HashSet(target.fromOnlyList));
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
      new HashSet(makeMyList(ftpcon,
	  ax,ax1,ax2,az,az1,az2,azx,azx1,azx2,azxx,azxx1,azxx2
	)), new HashSet(target.fromOnlyList));
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
  // ToDo : Linux環境でも動作するようにする。
  //@Test
  public void testReject2()
  throws Exception
  {
    long current = System.currentTimeMillis()/60000L*60000L - 120000L;

    String a, ax, ax1, ax2, ay, ay1, ay2, az, az1, az2;

    UnixFakeFileSystemEx fileSystem = new UnixFakeFileSystemEx(HOME);
    fileSystem.newFolder(a   = "dira");
    fileSystem.newFolder(ax  = "dira/dirx");
    fileSystem.newFile  (ax1 = "dira/dirx/file1","data ax1",current);
    fileSystem.newFile  (ax2 = "dira/dirx/File2","data ax2",current);
    fileSystem.newFolder(ay  = "dira/diry");
    fileSystem.newFile  (ay1 = "dira/diry/File1","data ay1",current);
    fileSystem.newFile  (ay2 = "dira/diry/FILE2","data ay2",current);
    fileSystem.newFolder(az  = "dira/dirz");
    fileSystem.newFile  (az1 = "dira/dirz/FILE1","data az1",current);
    fileSystem.newFile  (az2 = "dira/dirz/file2","data az2",current);
    fakeFtpServer.setFileSystem(fileSystem);

    File b   = tempdir.newFolder("dirb");

    FTPConnection ftpcon = new FTPConnection("localhost",UID,PASS,HOME);
    Map<String,FTPConnection> ftpsettings = new HashMap();
    ftpsettings.put("W",ftpcon);
    Backuper target = new Backuper("W:/dira",b.toString(),ftpsettings);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/file1");
    target.doCompare(System.out);

    assertEquals(makeMyList(ftpcon,ax,ax2,ay,ay2,az,az2),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    target.clearAll();
    target.addRejectFile("**/FILE2");
    target.doCompare(System.out);

    assertEquals(makeMyList(ftpcon,ax,ax1,ay,ay1,az,az1),target.fromOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.sameList);
    assertEquals(new ArrayList<File>(),target.toOnlyList);
    assertEquals(new ArrayList<FilePair>(),target.touchList);
    assertEquals(new ArrayList<File>(),target.moveList);

    // ------------------------------
    ftpcon.disconnect();
  }

  // ----------------------------------------------------------------------
  // 以下は、ユーティリティ。

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
