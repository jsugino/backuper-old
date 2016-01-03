package mylib.tools.misc;

import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.util.regex.Pattern;

public class RealFile implements VirFile, Comparable<RealFile>
{
  /**
  * 実態のファイルの場合、そのファイル名を表す。
  * FTPファイルの場合、単に、そのパスの管理のみ。
  */
  public File file;

  /**
  * 実態としてのファイルを生成する。
  */
  public RealFile( File file )
  {
    this.file = file;
  }

  /**
  * FTPに対応するドライブの場合は、FTPファイルとして生成する。
  * そうでなければ、通常のファイルを生成する。
  */
  public RealFile( String path )
  {
    this.file = new File(path);
  }

  public RealFile( RealFile parent, String sub )
  {
    this.file = new File(parent.file,sub);
  }

  public boolean isDirectory()
  throws IOException
  {
    checkExists();
    return file.isDirectory();
  }

  public boolean isFile()
  throws IOException
  {
    checkExists();
    return file.isFile();
  }

  public boolean exists()
  {
    return file.exists();
  }

  public boolean delete()
  {
    return file.delete();
  }

  public boolean mkdir()
  {
    return file.mkdir();
  }

  public long lastModified()
  throws IOException
  {
    checkExists();
    return file.lastModified();
  }

  public boolean setLastModified( long modify )
  {
    return file.setLastModified(modify);
  }

  public boolean renameTo( VirFile tofile )
  {
    // TODO: RealFile への強制キャストは誤り。
    return file.renameTo(((RealFile)tofile).file);
  }

  public String getName()
  {
    return file.getName();
  }

  public long length()
  throws IOException
  {
    checkExists();
    return file.length();
  }

  public void checkExists()
  throws IOException
  {
    if ( !file.exists() ) {
      throw new IOException("not exist "+file);
    }
  }

  public VirFile getParentFile()
  {
    return new RealFile(file.getParentFile());
  }

  public String[] list()
  {
    return file.list();
  }

  public VirFile[] listFiles( final Set<Pattern> rejects )
  {
    File filelist[] = file.listFiles(new FileFilter(){
      public boolean accept( File pathname ) {
	String target = pathname.toString().replace('\\','/');
	for ( Pattern pat : rejects ) {
	  if ( pat.matcher(target).matches() ) return false;
	}
	return true;
      }
    });
    VirFile retlist[] = new VirFile[filelist.length];
    for ( int i = 0; i < filelist.length; ++i ) {
      retlist[i] = new RealFile(filelist[i]);
    }
    return retlist;
  }

  public InputStream openAsInputStream()
  throws IOException
  {
    return new FileInputStream(file);
  }

  public OutputStream openAsOutputStream()
  throws IOException
  {
    return new FileOutputStream(file);
  }

  public VirFile makeSubFile( String name )
  {
    return new RealFile(new File(file,name));
  }

  public int hashCode()
  {
    return file.hashCode();
  }

  public boolean equals( Object obj )
  {
    if ( obj == null || !(obj instanceof RealFile) ) return false;
    RealFile target = (RealFile)obj;
    return (file == null ? target.file == null : file.equals(target.file));
  }

  public int compareTo( RealFile pathname )
  {
    return file.compareTo(pathname.file);
  }

  public int compareTo( VirFile pathname )
  {
    if ( pathname instanceof RealFile ) {
      return file.compareTo(((RealFile)pathname).file);
    }
    return file.toString().compareTo(pathname.toString());
  }

  public String toString()
  {
    return file == null ? "null" : file.toString();
  }
}
