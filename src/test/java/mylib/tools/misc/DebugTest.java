package mylib.tools.misc;

import static org.junit.Assert.*;
import org.junit.Test;

public class DebugTest
{
  //@Test
  public void debug1()
  throws Exception
  {
    Backuper.main(
      new String[]{
	"/mnt/C/BACKUP/BackupOld/OSBACKUP/User-back/junsei/AppData/Local/Invincea/Enterprise/U1_2/user/current/AppData/LocalLow/Microsoft/CryptnetUrlCache/Content/",
	"/mnt/D/BackupOld/OSBACKUP/User-back/junsei/AppData/Local/Invincea/Enterprise/U1_2/user/current/AppData/LocalLow/Microsoft/CryptnetUrlCache/Content",
      }
    );
  }
}
