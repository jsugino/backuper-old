package mylib.tools.misc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
* パックアップを実行するツールクラス。
* <BR>
* コマンドラインとしては次のように使用する。
* <pre>
* java mylib.tools.misc.BackuperEx [-stme] [-R rejectlist.txt] [-r rejectdir]... tools.misc.BackuperEx fromdir todir
* </pre>
* ファイルの同一性は、次のように検証される。
* <UL>
* <LI>sameList
* : ファイルパスが同じ(大文字小文字無視) ＆ ファイル長が同じ ＆ 最終更新日時が同じ
* <LI>touchList
* : ファイルパスが同じ(大文字小文字無視) ＆ ファイル長が同じ ＆ 最終更新日時が違う
* <LI>moveList
* : ファイル長が同じ ＆ 最終更新日時が同じ ＆ (ファイル長が 1MB 未満なら)ファイル名が同じ(大文字小文字無視)
* </UL>
* 個別に各メソッドを利用する場合は、次のように呼び出す。
* <pre>
* BackuperEx backuperEx = new BackuperEx(fromdir,todir);
* backuperEx.addRejectFile(rej);           // (必要に応じ)バックアップ対象外の指定
* backuperEx.doCompare(System.out);        // 比較の実施
* backuperEx.compareSameList(System.out);  // (必要に応じ)同一と判定されたものを、厳密に比較
* backuperEx.compareTouchList(System.out); // (必要に応じ)更新日時違いと判定されたものを、厳密に比較
* backuperEx.compareMoveList(System.out);  // (必要に応じ)移動と判定されたものを、厳密に比較
* backuperEx.printResult(System.out);      // 結果の表示
* backuperEx.doExecute(System.out);        // バックアップの実行
* </pre>
*/
public class BackuperEx
{
  public static void main( String argv[] )
  {
    try {
      boolean compareTouchList = false;
      boolean compareMoveList = false;
      boolean compareSameList = false;
      boolean doExecute = false;
      int cnt;
      List<String> rejList = new ArrayList<String>();
      for ( cnt = 0; cnt < argv.length; ++cnt ) {
	if ( argv[cnt].charAt(0) == '-' ) {
	  String optstr = argv[cnt];
	  for ( int i = 1; i < optstr.length(); ++i ) {
	    switch ( optstr.charAt(i) ) {
	     case 't':
	      compareTouchList = true;
	      break;
	     case 's':
	      compareSameList = true;
	      break;
	     case 'm':
	      compareMoveList = true;
	      break;
	     case 'e':
	      doExecute = true;
	      break;
	     case 'r':
	      rejList.add(argv[++cnt]);
	      break;
	     case 'R':
	      addRejectList(argv[++cnt],rejList);
	      break;
	     default:
	      usage();
	      break;
	    }
	  }
	} else {
	  break;
	}
      }
      if ( argv.length != cnt+2 ) {
	usage();
      }
      BackuperEx backuperEx = new BackuperEx(argv[cnt],argv[cnt+1]);
      for ( String rej : rejList ) {
	backuperEx.addRejectFile(rej);
      }
      backuperEx.doCompare(System.out);
      if ( compareSameList ) backuperEx.compareSameList(System.out);
      if ( compareTouchList ) backuperEx.compareTouchList(System.out);
      if ( compareMoveList ) backuperEx.compareMoveList(System.out);
      backuperEx.printResult(System.out);
      if ( doExecute ) backuperEx.doExecute(System.out);
    } catch ( Exception ex ) {
      ex.printStackTrace();
      usage();
    }
  }

  public static void addRejectList( String fname, List<String> rejList )
  throws IOException
  {
    BufferedReader rin = new BufferedReader(new InputStreamReader(new FileInputStream(fname)));
    String line;
    while ( (line = rin.readLine()) != null ) {
      rejList.add(line.trim());
    }
    rin.close();
  }

  public static void usage()
  {
    System.err.println("usage : java [-stme] [-R rejectlist.txt] [-r rejectdir]... tools.misc.BackuperEx fromdir todir");
    System.err.println("-s : compare file fully for same");
    System.err.println("-t : compare file fully for touch");
    System.err.println("-m : compare file fully for move");
    System.err.println("-e : execute copy files");
    System.err.println("-R : reject directory file list");
    System.err.println("-r : reject directory(relative from fromdir)");
    System.exit(1);
  }

  // ----------------------------------------------------------------------

  /**
  * 比較元ディレクトリルート
  */
  public File fromDirectory;

  /**
  * 比較先ディレクトリルート
  */
  public File toDirectory;

  /**
  * 同一だと想定できるもの。つまり、次のものが同一なもの。
  * <UL>
  * <LI>相対ディレクトリ = 同じ
  * <LI>ファイル名 = 同じ
  * <LI>ファイル長 = 同じ
  * <LI>更新時刻 = 同じ
  * </UL>
  * 念のために次のコマンドで真に同一か確認したほうが良い。
  * <blockquote>
  * <code>cmp &lt;from&gt; &lt;to&gt;</code>
  * </blockquote>
  * なお、このリストにはディレクトリも含まれることに注意すること。
  */
  public List<FilePair> sameList;

  /**
  * 比較元にしか存在しないもの。
  * なお、このリストにはディレクトリも含まれることに注意すること。
  */
  public List<File> fromOnlyList;

  /**
  * 比較先にしか存在しないもの。
  * なお、このリストにはディレクトリも含まれることに注意すること。
  */
  public List<File> toOnlyList;

  /**
  * 更新時刻のみが変更になったと想定されるもの。つまり、次のものが同一なもの。
  * <UL>
  * <LI>相対ディレクトリ = 同じ
  * <LI>ファイル名 = 同じ
  * <LI>ファイル長 = 同じ
  * <LI>更新時刻 = 異なる
  * </UL>
  * なお、このリストにはディレクトリは含まれない。
  */
  public List<FilePair> touchList;

  /**
  * ファイルの位置のみが変更になったと想定されるもの。つまり、次のものが同一なもの。
  * <UL>
  * <LI>相対ディレクトリ = 異なる
  * <LI>ファイル名 = 同じ
  * <LI>ファイル長 = 同じ
  * <LI>更新時刻 = 同じ
  * </UL>
  * なお、このリストにはディレクトリは含まれない。
  */
  public List<FilePair> moveList;

  /**
  * コピー対象外のファイル / ディレクトリ
  */
  public Set<File> rejectFileSet;

  /**
  * バックアップツールオブジェクトを生成する。
  *
  * @param fromdir コピー元
  * @param todir コピー先
  * @exception IOException コピー元、コピー先のディレクトリが無い場合
  */
  public BackuperEx( String fromdir, String todir )
  throws IOException
  {
    this(new File(fromdir),new File(todir));
  }

  /**
  * バックアップツールオブジェクトを生成する。
  *
  * @param fromdir コピー元
  * @param todir コピー先
  * @exception IOException コピー元、コピー先のディレクトリが無い場合
  */
  public BackuperEx( File fromdir, File todir )
  throws IOException
  {
    fromDirectory = fromdir;
    if ( !fromDirectory.isDirectory() ) {
      throw new IOException(fromdir+" is not directory.");
    }
    toDirectory = todir;
    if ( !toDirectory.isDirectory() ) {
      throw new IOException(todir+" is not directory.");
    }

    this.sameList = new ArrayList<FilePair>();
    this.fromOnlyList = new ArrayList<File>();
    this.toOnlyList = new ArrayList<File>();
    this.touchList = new ArrayList<FilePair>();
    this.moveList = new ArrayList<FilePair>();
    this.rejectFileSet = new HashSet<File>();
  }

  /**
  * コピー対象外のファイル / ディレクトリを追加する。
  * BackuperEx オブジェクト生成時の from ディレクトリからの相対パスを指定する。
  *
  * @param reject コピー対象外のファイル / ディレクトリ
  */
  public void addRejectFile( String reject )
  {
    rejectFileSet.add(new File(fromDirectory,reject));
  }

  /**
  * 全てのファイル処理の結果が残っていないかチェックする。
  *
  * @return 全てのファイル処理の結果が残っていない場合 true
  */
  public boolean isListEmpty()
  {
    return (
      sameList.size() == 0 &&
      fromOnlyList.size() == 0 && 
      toOnlyList.size() == 0 &&
      touchList.size() == 0 &&
      moveList.size() == 0
    );
  }

  /**
  * 全てのファイル処理の結果をクリアする。
  */
  public void clearList()
  {
    sameList.clear();
    fromOnlyList.clear();
    toOnlyList.clear();
    touchList.clear();
    moveList.clear();
  }

  /**
  * 全てのファイル処理を出力する。
  *
  * @param out 出力先
  */
  public void printList( PrintStream out )
  {
    out.println("==============================");
    out.println("sameList = "+sameList);
    out.println("fromOnlyList = "+fromOnlyList);
    out.println("toOnlyList = "+toOnlyList);
    out.println("touchList = "+touchList);
    out.println("moveList = "+moveList);
  }

  /**
  * 比較処理を実行する。
  *
  * @param out 比較処理を実行した結果の出力先。
  */
  public void doCompare( PrintStream out )
  {
    clearList();
    compareFiles(out,fromDirectory,toDirectory);
    checkMove();
  }

  private void compareFiles( PrintStream out, File frdir, File todir )
  {
    File frlist[] = frdir.listFiles(new FileFilter(){
      public boolean accept( File pathname ) {
	return !rejectFileSet.contains(pathname);
      }
    });
    File tolist[] = todir.listFiles();

    Arrays.sort(frlist);
    Arrays.sort(tolist);
    int i = 0, j = 0;
    while ( i < frlist.length && j < tolist.length ) {
      File fr = frlist[i];
      File to = tolist[j];
      String frn = fr.getName();
      String ton = to.getName();
      int comp = frn.compareToIgnoreCase(ton);
      if ( comp == 0 ) {
	if ( fr.isFile() ) {
	  if ( to.isFile() ) {
	    long frt = fr.lastModified();
	    long tot = to.lastModified();
	    long frl = fr.length();
	    long tol = to.length();
	    if ( frl == tol ) {
	      if ( frt == tot ) {
		sameList.add(new FilePair(fr,to));
	      } else {
		touchList.add(new FilePair(fr,to));
	      }
	    }
	  } else {
	    addToList(fromOnlyList,fr);
	    addToList(toOnlyList,to);
	  }
	} else {
	  if ( to.isDirectory() ) {
	    sameList.add(new FilePair(fr,to));
	    compareFiles(out,fr,to);
	  } else {
	    addToList(fromOnlyList,fr);
	    addToList(toOnlyList,to);
	  }
	}
	++i; ++j;
      } else if ( comp < 0 ) {
	addToList(fromOnlyList,fr);
	++i;
      } else {
	addToList(toOnlyList,to);
	++j;
      }
    }
    while ( i < frlist.length ) {
      addToList(fromOnlyList,frlist[i]);
      ++i;
    }
    while ( j < tolist.length ) {
      addToList(toOnlyList,tolist[j]);
      ++j;
    }
  }

  private void addToList( List<File> list, File dirfile )
  {
    list.add(dirfile);
    if ( dirfile.isDirectory() ) {
      File files[] = dirfile.listFiles();
      Arrays.sort(files);
      for ( int i = 0; i < files.length; ++i ) {
	addToList(list,files[i]);
      }
    }
  }

  public void compareSameList( PrintStream out )
  throws IOException
  {
    compareFilePairList(out,sameList);
  }

  public void compareTouchList( PrintStream out )
  throws IOException
  {
    compareFilePairList(out,touchList);
  }

  public void compareMoveList( PrintStream out )
  throws IOException
  {
    compareFilePairList(out,moveList);
  }

  private void compareFilePairList( PrintStream out, List<FilePair> list )
  throws IOException
  {
    Iterator<FilePair> itr = list.iterator();
    while ( itr.hasNext() ) {
      FilePair fpair = itr.next();
      if ( compFile(fpair.from,fpair.to) ) {
	out.println("compsame "+fpair.from+" "+fpair.to);
      } else {
	out.println("compdiff "+fpair.from+" "+fpair.to);
	itr.remove();
	fromOnlyList.add(fpair.from);
	toOnlyList.add(fpair.to);
      }
    }
  }

  static class MD5Comparator
  {
    public MD5Comparator()
    {
    }

    class FileData implements Comparable<FileData>
    {
      private File file;
      private String name;
      private long len;
      private long mod;
      private int idx;

      public FileData( File file, int idx )
      {
	this.file = file;
	this.idx = idx;
	this.name = file.getName();
	this.len = file.isDirectory() ? -1 : file.length();
	this.mod = file.lastModified();
      }

      /**
      * ファイル長が同じなら、MD5を用いた同一判断に入る。
      */
      public int compareTo( FileData o )
      {
	if ( len != o.len ) return len < o.len ? -1 : 1;
	if ( len < 1024L * 1024L ) {
	  int cmp = name.compareToIgnoreCase(o.name);
	  if ( cmp != 0 ) return cmp;
	}
	if ( mod != o.mod ) return mod < o.mod ? -1 : 1;
	return 0;
      }

      public int compareWithMD5( FileData o )
      {
	return compareTo(o);
      }
    }

    public FileData[] prepareList( List<File> flist )
    {
      int cnt = 0;
      for ( File file : flist ) {
	if ( file.isFile() ) ++cnt;
      }
      FileData arr[] = new FileData[cnt];
      cnt = 0;
      int i = 0;
      for ( File file : flist ) {
	if ( file.isFile() ) {
	  arr[cnt++] = new FileData(file,i);
	}
	++i;
      }
      Arrays.sort(arr);
      return arr;
    }
  }

  private void checkMove()
  {
    MD5Comparator md5comp = new MD5Comparator();
    MD5Comparator.FileData frarr[] = md5comp.prepareList(fromOnlyList);
    MD5Comparator.FileData toarr[] = md5comp.prepareList(toOnlyList);

    int i = 0, j = 0;
    while ( i < frarr.length && j < toarr.length ) {
      int comp = frarr[i].compareTo(toarr[j]);
      if ( comp == 0 ) {
	moveList.add(new FilePair(frarr[i].file,toarr[j].file));
	fromOnlyList.set(frarr[i].idx,null);
	toOnlyList.set(toarr[j].idx,null);
	++i; ++j;
      } else if ( comp < 0 ) {
	++i;
      } else {
	++j;
      }
    }

    Iterator<File> itr;

    itr = fromOnlyList.iterator();
    while ( itr.hasNext() ) {
      if ( itr.next() == null ) itr.remove();
    }

    itr = toOnlyList.iterator();
    while ( itr.hasNext() ) {
      if ( itr.next() == null ) itr.remove();
    }
  }

  public void printResult( PrintStream out )
  {
    for ( File file : toOnlyList ) {
      if ( file.isFile() ) {
	out.println("del \""+file+"\"");
      } else {
	out.println("rmdir \""+file+"\"");
      }
    }
    for ( File file : fromOnlyList ) {
      File tofile = relate(file,fromDirectory,toDirectory);
      if ( file.isFile() ) {
	out.println("copy \""+file+"\" \""+tofile+"\"");
      } else {
	out.println("mkdir \""+tofile+"\"");
      }
    }
    for ( FilePair fpair : touchList ) {
      out.println("touch \""+fpair.from+"\" \""+fpair.to+"\"");
    }
    for ( FilePair fpair : moveList ) {
      File tofile = relate(fpair.from,fromDirectory,toDirectory);
      out.println("move \""+fpair.to+"\" \""+tofile+"\"");
    }
  }

  public void doExecute( PrintStream out )
  throws IOException
  {
    boolean doit = true;
    while ( doit && !isListEmpty() ) {
      doit = false;
      {
	Iterator<FilePair> itr = sameList.iterator();
	while ( itr.hasNext() ) {
	  FilePair fpair = itr.next();
	  //out.println("same \""+fpair.from+"\" \""+fpair.to+"\"");
	  itr.remove(); doit = true;
	}
      }
      {
	Iterator<File> itr = toOnlyList.iterator();
	while ( itr.hasNext() ) {
	  File file = itr.next();
	  if ( file.isFile() ) {
	    out.println("del \""+file+"\"");
	    if ( !file.delete() ) {
	      throw new IOException("Cannot delete file: "+file);
	    }
	    itr.remove(); doit = true;
	  } else if ( file.list().length == 0 ) {
	    out.println("rmdir \""+file+"\"");
	    if ( !file.delete() ) {
	      throw new IOException("Cannot delete directory: "+file);
	    }
	    itr.remove(); doit = true;
	  }
	}
      }
      {
	Iterator<File> itr = fromOnlyList.iterator();
	while ( itr.hasNext() ) {
	  File file = itr.next();
	  File tofile = relate(file,fromDirectory,toDirectory);
	  if ( !tofile.exists() && tofile.getParentFile().isDirectory() ) {
	    if ( file.isDirectory() ) {
	      out.println("mkdir \""+tofile+"\"");
	      if ( !tofile.mkdir() ) {
		throw new IOException("Cannot make directory: "+tofile);
	      }
	    } else {
	      out.println("copy \""+file+"\" \""+tofile+"\"");
	      copyFile(file,tofile);
	    }
	    itr.remove(); doit = true;
	  }
	}
      }
      {
	Iterator<FilePair> itr = touchList.iterator();
	while ( itr.hasNext() ) {
	  FilePair fpair = itr.next();
	  out.println("touch \""+fpair.from+"\" \""+fpair.to+"\"");
	  setModified(fpair.from,fpair.to);
	  itr.remove(); doit = true;
	}
      }
      {
	Iterator<FilePair> itr = moveList.iterator();
	while ( itr.hasNext() ) {
	  FilePair fpair = itr.next();
	  File tofile = relate(fpair.from,fromDirectory,toDirectory);
	  if ( tofile.getParentFile().isDirectory() ) {
	    out.println("move \""+fpair.to+"\" \""+tofile+"\"");
	    if ( !fpair.to.renameTo(tofile) ) {
	      throw new IOException("Cannot move: "+fpair.to+" "+tofile);
	    }
	    itr.remove(); doit = true;
	  }
	}
      }
    }
    if ( !isListEmpty() ) {
      printList(out);
      throw new IOException("remain datas");
    }
  }

  public static File relate( File target, File base, File rel )
  {
    return target.equals(base) ? rel :
      new File(relate(target.getParentFile(),base,rel),target.getName());
  }

  public static boolean compFile( File from, File to )
  throws IOException
  {
    BufferedInputStream infr = new BufferedInputStream(new FileInputStream(from));
    BufferedInputStream into = new BufferedInputStream(new FileInputStream(to));
    int chfr, chto;
    while ( (chfr = infr.read()) >= 0 & (chto = into.read()) >= 0 ) {
      if ( chfr != chto ) break;
    }
    infr.close();
    into.close();
    return chfr == chto;
  }

  public static void copyFile( File from, File to )
  throws IOException
  {
    FileInputStream infr = new FileInputStream(from);
    FileOutputStream outto = new FileOutputStream(to);
    byte buf[] = new byte[1024];
    int nn;
    while ( (nn = infr.read(buf)) > 0 ) {
      outto.write(buf,0,nn);
    }
    infr.close();
    outto.close();
    setModified(from,to);
  }

  public static void setModified( File from, File to )
  throws IOException
  {
    if ( !to.setLastModified(from.lastModified()) ) {
      throw new IOException("Cannot set lastModified: "+to);
    }
  }

  public static class FilePair
  {
    public File from;
    public File to;

    public FilePair( File from, File to )
    {
      this.from = from;
      this.to = to;
    }
    public boolean equals( Object obj )
    {
      if ( obj == null || !(obj instanceof FilePair) ) return false;
      FilePair target = (FilePair)obj;
      return (
	(from == null ? target.from == null : from.equals(target.from)) &&
	(to == null ? target.to == null : to.equals(target.to)));
    }
    public String toString()
    {
      return "["+from.toString()+","+to.toString()+"]";
    }

    @Override
    public int hashCode()
    {
      return from.hashCode() + to.hashCode();
    }
  }
}
