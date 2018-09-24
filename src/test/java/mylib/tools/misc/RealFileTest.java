package mylib.tools.misc;

import static org.junit.Assert.*;
import java.io.File;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.HashSet;

public class RealFileTest
{
  @Rule
  public TemporaryFolder tempdir = new TemporaryFolder(new File("target"));

  @Test
  public void testList1()
  throws Exception
  {
    File dir = tempdir.newFolder("a");
    tempdir.newFile("a/1");
    tempdir.newFile("a/2");
    tempdir.newFile("a/3");

    RealFile realfile = new RealFile(dir);

    assertArrayEquals(new String[]{"1", "2", "3"},realfile.list());

    assertArrayEquals(new RealFile[]{
	new RealFile(new File(dir,"1")),
	new RealFile(new File(dir,"2")),
	new RealFile(new File(dir,"3"))},
      realfile.listFiles(new HashSet<Pattern>()));
  }

  @Test
  public void testList2()
  throws Exception
  {
    File dir = tempdir.newFolder("a b c");
    tempdir.newFile("a b c/1");
    tempdir.newFile("a b c/2");
    tempdir.newFile("a b c/3");

    RealFile realfile = new RealFile(dir);

    assertArrayEquals(new String[]{"1", "2", "3"},realfile.list());

    assertArrayEquals(new RealFile[]{
	new RealFile(new File(dir,"1")),
	new RealFile(new File(dir,"2")),
	new RealFile(new File(dir,"3"))},
      realfile.listFiles(new HashSet<Pattern>()));

    Backuper backuper = new Backuper(dir,dir);
    backuper.addRejectFile("2");

    assertArrayEquals(new RealFile[]{
	new RealFile(new File(dir,"1")),
	new RealFile(new File(dir,"3"))},
      realfile.listFiles(backuper.rejectFileSet));
  }

  @Test
  public void testList3()
  throws Exception
  {
    File dir = tempdir.newFolder("a");
    tempdir.newFile("a/1 1 1");
    tempdir.newFile("a/2 2 2");
    tempdir.newFile("a/3 3 3");

    RealFile realfile = new RealFile(dir);

    assertArrayEquals(new String[]{"1 1 1", "2 2 2", "3 3 3"},realfile.list());

    assertArrayEquals(new RealFile[]{
	new RealFile(new File(dir,"1 1 1")),
	new RealFile(new File(dir,"2 2 2")),
	new RealFile(new File(dir,"3 3 3"))},
      realfile.listFiles(new HashSet<Pattern>()));

    Backuper backuper = new Backuper(dir,dir);
    backuper.addRejectFile("2 2 2");

    assertArrayEquals(new RealFile[]{
	new RealFile(new File(dir,"1 1 1")),
	new RealFile(new File(dir,"3 3 3"))},
      realfile.listFiles(backuper.rejectFileSet));
  }

  @Test
  public void testList4()
  throws Exception
  {
    File dir = tempdir.newFolder("a");
    tempdir.newFile  ("a/1 1 1");
    File subdir = tempdir.newFolder("a","2 2 2");
    tempdir.newFile  ("a/2 2 2/2 1");
    tempdir.newFile  ("a/2 2 2/2 2");
    tempdir.newFile  ("a/2 2 2/2 3");
    tempdir.newFile  ("a/3 3 3");

    RealFile realfile = new RealFile(dir);

    assertArrayEquals(new String[]{"1 1 1", "2 2 2", "3 3 3"},realfile.list());
    assertArrayEquals(new String[]{"2 1", "2 2", "2 3"},new RealFile(realfile,"2 2 2").list());

    assertArrayEquals(new RealFile[]{
	new RealFile(new File(dir,"1 1 1")),
	new RealFile(new File(dir,"2 2 2")),
	new RealFile(new File(dir,"3 3 3"))},
      realfile.listFiles(new HashSet<Pattern>()));

    VirFile subfile = realfile.listFiles(new HashSet<Pattern>())[1];
    assertArrayEquals(new RealFile[]{
	new RealFile(new File(subdir,"2 1")),
	new RealFile(new File(subdir,"2 2")),
	new RealFile(new File(subdir,"2 3"))},
      subfile.listFiles(new HashSet<Pattern>()));

    Backuper backuper = new Backuper(dir,dir);
    backuper.addRejectFile("1 1 1");

    assertArrayEquals(new RealFile[]{
	new RealFile(new File(dir,"2 2 2")),
	new RealFile(new File(dir,"3 3 3"))},
      realfile.listFiles(backuper.rejectFileSet));

    assertArrayEquals(new RealFile[]{
	new RealFile(new File(subdir,"2 1")),
	new RealFile(new File(subdir,"2 2")),
	new RealFile(new File(subdir,"2 3"))},
      subfile.listFiles(backuper.rejectFileSet));
  }
}
