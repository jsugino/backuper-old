package mylib.tools.misc;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class RemoteFile implements VirFile, Comparable<RemoteFile>
{
  /**
  * 前後には '/' を含まない。
  */
  public String relpath;
  public String name;
  public FTPFile ftpfile;
  public FTPConnection ftpconnection;

  public RemoteFile( String path, FTPConnection ftpconnection )
  {
    while ( path.length() > 0 && path.charAt(0) == '/' ) {
      path = path.substring(1);
    }
    while ( path.length() > 0 && path.charAt(path.length()-1) == '/' ) {
      path = path.substring(0,path.length()-1);
    }

    if ( path.length() == 0 ) {
      this.name = this.relpath = ".";
    } else {
      this.relpath = path;
      int idx = path.lastIndexOf('/');
      if ( idx < 0 ) {
	this.name = path;
      } else {
	this.name = path.substring(idx+1);
      }
    }
    this.ftpfile = null;
    this.ftpconnection = ftpconnection;
  }

  public RemoteFile( RemoteFile parent, FTPFile ftpfile )
  {
    if ( parent.relpath.equals(".") ) {
      this.name = this.relpath = ftpfile.getName();
    } else {
      this.name = ftpfile.getName();
      this.relpath = parent.relpath+'/'+ftpfile.getName();
    }
    this.ftpfile = ftpfile;
    this.ftpconnection = parent.ftpconnection;
  }

  public boolean isDirectory()
  throws IOException
  {
    return getFTPFile().isDirectory();
  }

  public boolean isFile()
  throws IOException
  {
    return getFTPFile().isFile();
  }

  public boolean exists()
  {
    // TODO: 本当は全部 true では、いけないかも。
    return true;
  }

  public boolean delete()
  throws IOException
  {
    return ftpconnection.getFTPClient().deleteFile(ftpconnection.topdir+relpath);
  }

  public boolean mkdir()
  {
    return false;
  }

  public long lastModified()
  throws IOException
  {
    return getFTPFile().getTimestamp().getTimeInMillis();
  }

  public boolean setLastModified( long modify )
  {
    return false;
  }

  public boolean renameTo( VirFile tofile )
  {
    return false;
  }

  public String getName()
  {
    return name;
  }

  public long length()
  throws IOException
  {
    return getFTPFile().getSize();
  }

  public VirFile getParentFile()
  {
    int idx = relpath.lastIndexOf('/');
    if ( idx < 0 ) return null;
    return new RemoteFile( relpath.substring(0,idx), ftpconnection );
  }

  public String[] list()
  {
    return null;
  }

  public VirFile[] listFiles( final Set<Pattern> rejects )
  throws IOException
  {
    final String fullpath = toString()+"/";
    FTPFile list[] = ftpconnection.getFTPClient().listFiles(ftpconnection.topdir+relpath,new FTPFileFilter(){
      public boolean accept( FTPFile file ) {
	String target = fullpath+file.getName();
	for ( Pattern pat : rejects ) {
	  if ( pat.matcher(target).matches() ) return false;
	}
	return true;
      }
    });
    RemoteFile ret[] = new RemoteFile[list.length];
    for ( int i = 0; i < list.length; ++i ) {
      ret[i] = new RemoteFile(this,list[i]);
    }
    return ret;
  }

  public FTPFile getFTPFile()
  throws IOException
  {
    if ( ftpfile == null ) {
      FTPFile ftparr[] = ftpconnection.getFTPClient().listFiles(ftpconnection.topdir+relpath);
      ftpfile = ftparr[0];
    }
    return ftpfile;
  }

  public InputStream openAsInputStream()
  throws IOException
  {
    InputStream ins = ftpconnection.getFTPClient().retrieveFileStream(ftpconnection.topdir+relpath);
    //return ins;
    return ftpconnection.new FinalizeInputStream(ins);
  }

  public OutputStream openAsOutputStream()
  throws IOException
  {
    return null;
  }

  public VirFile makeSubFile( String name )
  {
    return new RemoteFile(this.relpath+"/"+name,ftpconnection);
  }

  public boolean equals( Object obj )
  {
    if ( obj == null || !(obj instanceof RemoteFile) ) return false;
    RemoteFile target = (RemoteFile)obj;
    return
      ftpconnection.equals(target.ftpconnection) &&
      relpath.equals(target.relpath);
  }

  public int hashCode()
  {
    return ftpconnection.hashCode()+relpath.hashCode();
  }

  public String toString()
  {
    return ftpconnection.toString()+relpath;
  }

  public int compareTo( RemoteFile pathname )
  {
    return relpath.compareTo(pathname.relpath);
  }

  public int compareTo( VirFile pathname )
  {
    if ( pathname instanceof RemoteFile ) {
      return relpath.compareTo(((RemoteFile)pathname).relpath);
    }
    return relpath.compareTo(pathname.toString());
  }

  public static class FTPConnection
  {
    public String server;
    public String userid;
    public String password;

    /**
    * 先頭も終わりにも '/' が含まれる。
    */
    public String topdir;

    public FTPClient ftpclient = null;

    /**
    * Creates a new <code>FTPConnection</code> instance.
    *
    * @param setting ftp server data. [userid[:password]@]server/top/dir )
    */
    public FTPConnection( String server, String topdir )
    {
      this(server,"anonymous","anonymous",topdir);
    }

    public FTPConnection( String server, String userid, String password, String topdir )
    {
      this.server = server;
      this.userid = userid;
      this.password = password;
      if ( topdir.length() == 0 ) {
	topdir = "/";
      } else {
	if ( topdir.charAt(0) != '/' ) {
	  topdir = "/"+topdir;
	}
	if ( topdir.charAt(topdir.length()-1) != '/' ) {
	  topdir = topdir+"/";
	}
      }
      this.topdir = topdir;
    }

    public FTPClient getFTPClient()
    throws IOException
    {
      if ( ftpclient == null ) {
	FTPClient ftp = ftpclient = new FTPClient();
	try {
	  ftpclient.connect(server);
	  if ( !ftpclient.login(userid,password) ) {
	    ftpclient.disconnect();
	    throw new IOException("Cannot connect by "+userid);
	  }
	} finally {
	  ftpclient = null;
	}
	ftpclient = ftp;
      }
      return ftpclient;
    }

    public void disconnect()
    throws IOException
    {
      if ( ftpclient != null ) {
	try {
	  ftpclient.disconnect();
	} finally {
	  ftpclient = null;
	}
      }
    }

    public String toString()
    {
      return server+topdir;
    }

    public boolean equals( Object obj )
    {
      if ( obj == null || !(obj instanceof FTPConnection) ) return false;
      FTPConnection target = (FTPConnection)obj;
      return
	server.equals(target.server) &&
	userid.equals(target.userid) &&
	password.equals(target.password) &&
	topdir.equals(target.topdir);
    }

    public int hashCode()
    {
      return server.hashCode() + userid.hashCode() + password.hashCode() + topdir.hashCode();
    }

    public class FinalizeInputStream extends FilterInputStream
    {
      public FinalizeInputStream( InputStream in )
      {
	super(in);
      }

      public void close()
      throws IOException
      {
	in.close();
	getFTPClient().completePendingCommand();
      }
    }
  }
}
