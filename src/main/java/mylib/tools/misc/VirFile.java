package mylib.tools.misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.regex.Pattern;

public interface VirFile // extends Comparable<VirFile>
{
  public boolean isDirectory() throws IOException;
  public boolean isFile() throws IOException;
  public boolean exists();
  public boolean delete() throws IOException;
  public boolean mkdir();
  public long lastModified() throws IOException;
  public boolean setLastModified( long modify );
  public boolean renameTo( VirFile tofile );
  public String getName();
  public long length() throws IOException;
  public VirFile getParentFile();
  public String[] list() throws IOException;
  public VirFile[] listFiles( Set<Pattern> rejects ) throws IOException;
  public InputStream openAsInputStream() throws IOException;
  public OutputStream openAsOutputStream() throws IOException;
  public boolean equals( Object obj );
  public String toString();
  public VirFile makeSubFile( String name );
}
