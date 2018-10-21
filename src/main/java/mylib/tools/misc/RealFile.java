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

  @Override
  public boolean isDirectory()
  throws IOException
  {
    checkExists();
    return file.isDirectory();
  }

  @Override
  public boolean isFile()
  throws IOException
  {
    checkExists();
    return file.isFile();
  }

  @Override
  public boolean exists()
  {
    return file.exists();
  }

  @Override
  public boolean delete()
  {
    return file.delete();
  }

  @Override
  public boolean mkdir()
  {
    return file.mkdir();
  }

  @Override
  public boolean mkdirs()
  {
    return file.mkdirs();
  }

  @Override
  public long lastModified()
  throws IOException
  {
    checkExists();
    return file.lastModified();
  }

  @Override
  public boolean setLastModified( long modify )
  {
    return file.setLastModified(modify);
  }

  @Override
  public boolean renameTo( VirFile tofile )
  {
    // TODO: RealFile への強制キャストは誤り。
    return file.renameTo(((RealFile)tofile).file);
  }

  @Override
  public String getName()
  {
    return file.getName();
  }

  @Override
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

  @Override
  public VirFile getParentFile()
  {
    return new RealFile(file.getParentFile());
  }

  @Override
  public String[] list()
  {
    return file.list();
  }

  @Override
  public VirFile[] listFiles( final Set<Pattern> rejects )
  {
    File filelist[] = file.listFiles(new FileFilter(){
      public boolean accept( File pathname ) {
	String target = pathname.toString();
	try {
	  if ( Backuper.isSymlink(target) ) return false;
	} catch ( IOException ex ) {
	  System.out.println("IOException ("+ex.getMessage()+")occured, but ignored");
	}
	target = target.replace('\\','/');
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

  @Override
  public InputStream openAsInputStream()
  throws IOException
  {
    return new FileInputStream(file);
  }

  @Override
  public OutputStream openAsOutputStream()
  throws IOException
  {
    return new FileOutputStream(file);
  }

  @Override
  public VirFile makeSubFile( String name )
  {
    return new RealFile(new File(file,name));
  }

  public int hashCode()
  {
    return file.hashCode();
  }

  @Override
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

  @Override
  public String toString()
  {
    return file == null ? "null" : file.toString();
  }
}
