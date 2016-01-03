package mylib.tools.misc;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.io.InputStream;
import java.io.OutputStream;
import mylib.tools.misc.RemoteFile.FTPConnection;
import java.util.regex.Pattern;

/**
* パックアップを実行するツールクラス。
* <BR>
* コマンドラインとしては次のように使用する。
* <pre>
* java mylib.tools.misc.Backuper [-stme] [-R rejectlist.txt] [-r rejectdir]... tools.misc.Backuper fromdir todir
* </pre>
* ファイルの同一性は、次のように検証される。
* <UL>
* <LI>sameList
* : ファイルパスが同じ(大文字小文字無視) ＆ ファイル長が同じ ＆ 最終更新日時が同じ
* <LI>touchList
* : ファイルパスが同じ(大文字小文字無視) ＆ ファイル長が同じ ＆ 最終更新日時が違う
* <LI>moveList
* : ファイル長が同じ ＆ 最終更新日時が同じ
* </UL>
* 個別に各メソッドを利用する場合は、次のように呼び出す。
* <pre>
* Backuper backuper = new Backuper(fromdir,todir);
* backuper.addRejectFile(rej);           // (必要に応じ)バックアップ対象外の指定
* backuper.doCompare(System.out);        // 比較の実施
* backuper.compareSameList(System.out);  // (必要に応じ)同一と判定されたものを、厳密に比較
* backuper.compareTouchList(System.out); // (必要に応じ)更新日時違いと判定されたものを、厳密に比較
* backuper.compareMoveList(System.out);  // (必要に応じ)移動と判定されたものを、厳密に比較
* backuper.printResult(System.out);      // 結果の表示
* backuper.doExecute(System.out);        // バックアップの実行
* </pre>
*/
public class Backuper
{
  public static void main( String argv[] )
  {
    try {
      boolean compareTouchList = false;
      boolean compareMoveList = false;
      boolean compareSameList = false;
      boolean doExecute = false;
      boolean isDebug = false;
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
	     case 'd':
	      isDebug = true;
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
      Backuper backuper = new Backuper(argv[cnt],argv[cnt+1]);
      for ( String rej : rejList ) {
	backuper.addRejectFile(rej);
      }
      backuper.doCompare(System.out);
      if ( compareSameList ) backuper.compareSameList(System.out);
      if ( compareTouchList ) backuper.compareTouchList(System.out);
      if ( compareMoveList ) backuper.compareMoveList(System.out);
      backuper.printResult(System.out);
      if ( isDebug ) backuper.printList(System.out);
      if ( doExecute ) backuper.doExecute(System.out);
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
    System.err.println("usage : java [-stme] [-R rejectlist.txt] [-r rejectdir]... tools.misc.Backuper fromdir todir");
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
  public VirFile fromDirectory;

  /**
  * 比較先ディレクトリルート
  */
  public VirFile toDirectory;

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
  public List<VirFile> fromOnlyList;

  /**
  * 比較先にしか存在しないもの。
  * なお、このリストにはディレクトリも含まれることに注意すること。
  */
  public List<VirFile> toOnlyList;

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
  public Set<Pattern> rejectFileSet;

  /**
  * バックアップツールオブジェクトを生成する。
  *
  * @param fromdir コピー元
  * @param todir コピー先
  * @exception IOException コピー元、コピー先のディレクトリが無い場合
  */
  public Backuper( String fromdir, String todir )
  throws IOException
  {
    this(new File(fromdir),new File(todir));
  }

  /**
  * バックアップツールオブジェクトを生成する。
  *
  * @param fromdir コピー元
  * @param todir コピー先
  * @param ftpsettings FTP設定
  * @exception IOException コピー元、コピー先のディレクトリが無い場合
  */
  public Backuper( String fromdir, String todir, Map<String,FTPConnection> ftpsettings )
  throws IOException
  {
    fromDirectory = createFile(fromdir,ftpsettings);
    toDirectory = createFile(todir,ftpsettings);
    setupFields();
  }

  /**
  * バックアップツールオブジェクトを生成する。
  *
  * @param fromdir コピー元
  * @param todir コピー先
  * @param ftpsettings FTP設定
  * @exception IOException コピー元、コピー先のディレクトリが無い場合
  */
  public Backuper( File fromdir, File todir )
  throws IOException
  {
    fromDirectory = new RealFile(fromdir);
    if ( !fromDirectory.isDirectory() ) {
      throw new IOException(fromdir+" is not directory.");
    }
    toDirectory = new RealFile(todir);
    if ( !toDirectory.isDirectory() ) {
      throw new IOException(todir+" is not directory.");
    }
    setupFields();
  }

  public void setupFields()
  {
    this.sameList = new ArrayList<FilePair>();
    this.fromOnlyList = new ArrayList<VirFile>();
    this.toOnlyList = new ArrayList<VirFile>();
    this.touchList = new ArrayList<FilePair>();
    this.moveList = new ArrayList<FilePair>();
    this.rejectFileSet = new HashSet<Pattern>();
  }

  public static VirFile createFile( String path, Map<String,FTPConnection> ftpsettings )
  throws IOException
  {
    if ( path.length() > 1 && path.charAt(1) == ':' ) {
      String drive = (""+path.charAt(0)).toUpperCase();
      FTPConnection ftpcon = ftpsettings.get(drive);
      if ( ftpcon != null ) {
	return new RemoteFile(path.substring(2),ftpcon);
      }
    }
    RealFile file = new RealFile(path);
    if ( !file.isDirectory() ) {
      throw new IOException(path+" is not directory.");
    }
    return file;
  }

  /**
  * コピー対象外のファイル / ディレクトリを追加する。
  * Backuper オブジェクト生成時の from ディレクトリからの相対パスを指定する。
  *
  * @param reject コピー対象外のファイル / ディレクトリ
  */
  public void addRejectFile( String reject )
  {
    // TODO: 本来は fromDirectory だけでなく、toDirectory についても、考慮すべき。
    // TODO: Windows と ftp で CASE_INSENSITIVE の考え方が異なる。

    String pat;
    reject = reject.toLowerCase().replace('\\','/');
    if ( reject.startsWith("**/") ) {
      pat = ".*/"+Pattern.quote(reject.substring(3));
    } else {
      pat = Pattern.quote(reject);
    }
    pat = Pattern.quote(fromDirectory.toString().replace('\\','/'))+"/"+pat;
    rejectFileSet.add(Pattern.compile(pat,Pattern.CASE_INSENSITIVE));
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
  * Reject ファイルを含め、すべてクリアする。
  */
  public void clearAll()
  {
    clearList();
    rejectFileSet.clear();
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
  throws IOException
  {
    clearList();
    compareFiles(out,fromDirectory,toDirectory);
    checkMove();
  }

  private void compareFiles( PrintStream out, VirFile frdir, VirFile todir )
  throws IOException
  {
    VirFile frlist[] = frdir.listFiles(rejectFileSet);
    VirFile tolist[] = todir.listFiles(rejectFileSet);

    Arrays.sort(frlist);
    Arrays.sort(tolist);
    int i = 0, j = 0;
    while ( i < frlist.length && j < tolist.length ) {
      VirFile fr = frlist[i];
      VirFile to = tolist[j];
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
	    } else {
	      addToList(fromOnlyList,fr);
	      addToList(toOnlyList,to);
	    }
	  } else {
	    addToList(fromOnlyList,fr);
	    addToList(toOnlyList,to);
	  }
	} else {
	  if ( to.isDirectory() ) {
	    //sameList.add(new FilePair(fr,to));
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

  private void addToList( List<VirFile> list, VirFile dirfile )
  throws IOException
  {
    list.add(dirfile);
    if ( dirfile.isDirectory() ) {
      VirFile files[] = dirfile.listFiles(rejectFileSet);
      Arrays.sort(files);
      for ( int i = 0; i < files.length; ++i ) {
	addToList(list,files[i]);
      }
    }
  }

  public void compareSameList( PrintStream out )
  throws IOException
  {
    compareFilePairList(out,sameList,false);
  }

  public void compareTouchList( PrintStream out )
  throws IOException
  {
    compareFilePairList(out,touchList,true);
  }

  public void compareMoveList( PrintStream out )
  throws IOException
  {
    compareFilePairList(out,moveList,true);
  }

  private void compareFilePairList( PrintStream out, List<FilePair> list, boolean outsame )
  throws IOException
  {
    Iterator<FilePair> itr = list.iterator();
    while ( itr.hasNext() ) {
      FilePair fpair = itr.next();
      if ( compFile(fpair.from,fpair.to) ) {
	if ( outsame ) {
	  out.println("compsame "+fpair.from+" "+fpair.to);
	}
      } else {
	out.println("compdiff "+fpair.from+" "+fpair.to);
	itr.remove();
	fromOnlyList.add(fpair.from);
	toOnlyList.add(fpair.to);
      }
    }
  }

  private void checkMove()
  throws IOException
  {
    class FileData implements Comparable<FileData>
    {
      private VirFile file;
      private long len;
      private long mod;
      private int idx;

      public FileData( VirFile file, int idx )
      throws IOException
      {
	this.file = file;
	this.idx = idx;
	this.len = file.isDirectory() ? -1 : file.length();
	this.mod = file.lastModified();
      }

      public int compareTo( FileData o )
      {
	// TODO: ファイル長と更新日時が同一なら「同一」としているが、乱暴すぎる！

	if ( len != o.len ) return len < o.len ? -1 : 1;
	if ( mod != o.mod ) return mod < o.mod ? -1 : 1;
	return 0;
      }
    }

    FileData frarr[] = new FileData[fromOnlyList.size()];
    for ( int i = 0; i < frarr.length; ++i ) {
      frarr[i] = new FileData(fromOnlyList.get(i),i);
    }
    Arrays.sort(frarr);

    FileData toarr[] = new FileData[toOnlyList.size()];
    for ( int j = 0; j < toarr.length; ++j ) {
      toarr[j] = new FileData(toOnlyList.get(j),j);
    }
    Arrays.sort(toarr);

    int i = 0, j = 0;
    while ( i < frarr.length && j < toarr.length ) {
      if ( frarr[i].file.isDirectory() ) { ++i; continue; }
      if ( toarr[j].file.isDirectory() ) { ++j; continue; }
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

    Iterator<VirFile> itr;

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
  throws IOException
  {
    for ( VirFile file : toOnlyList ) {
      if ( file.isFile() ) {
	out.println("del \""+file+"\"");
      } else {
	out.println("rmdir \""+file+"\"");
      }
    }
    for ( VirFile file : fromOnlyList ) {
      VirFile tofile = relate(file,fromDirectory,toDirectory);
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
      VirFile tofile = relate(fpair.from,fromDirectory,toDirectory);
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
	Iterator<VirFile> itr = toOnlyList.iterator();
	while ( itr.hasNext() ) {
	  VirFile file = itr.next();
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
	Iterator<VirFile> itr = fromOnlyList.iterator();
	while ( itr.hasNext() ) {
	  VirFile file = itr.next();
	  VirFile tofile = relate(file,fromDirectory,toDirectory);
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
	  VirFile tofile = relate(fpair.from,fromDirectory,toDirectory);
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

  public static VirFile relate( VirFile target, VirFile base, VirFile rel )
  {
    return target.equals(base) ? rel : relate(target.getParentFile(),base,rel).makeSubFile(target.getName());
  }

  public static boolean compFile( VirFile from, VirFile to )
  throws IOException
  {
    BufferedInputStream infr = new BufferedInputStream(from.openAsInputStream());
    BufferedInputStream into = new BufferedInputStream(to.openAsInputStream());
    int chfr, chto;
    while ( (chfr = infr.read()) >= 0 & (chto = into.read()) >= 0 ) {
      if ( chfr != chto ) break;
    }
    infr.close();
    into.close();
    return chfr == chto;
  }

  public static void copyFile( VirFile from, VirFile to )
  throws IOException
  {
    InputStream infr = from.openAsInputStream();
    OutputStream outto = to.openAsOutputStream();
    byte buf[] = new byte[1024];
    int nn;
    while ( (nn = infr.read(buf)) > 0 ) {
      outto.write(buf,0,nn);
    }
    infr.close();
    outto.close();
    setModified(from,to);
  }

  public static void setModified( VirFile from, VirFile to )
  throws IOException
  {
    if ( !to.setLastModified(from.lastModified()) ) {
      throw new IOException("Cannot set lastModified: "+to);
    }
  }

  public static class FilePair
  {
    public VirFile from;
    public VirFile to;

    public FilePair( VirFile from, VirFile to )
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
