
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import ucsdh.SMBUtilities;

public class runner {
	public static void main(String[] args) throws Exception {
		String password = null;
		//get password from the arg:
		//	-Dservice_account_password=xxxxxxxx
		for(String arg : args) {
			if(arg.substring(0, 27).equals("-Dservice_account_password=")) {
				password = arg.substring(27);
			}
		}
		
		if(password == null) {
			throw new Exception("Expecting application argument service_account_password");
		}
		
		String index = SMBUtilities.Connect("your_hostname", "your_username", "your_password", "your_domain", "your_share_name");
		
		Object stream = SMBUtilities.ReadFile(index, "deleteme.txt", "your\\folder\\path");
		
		Object writeResult = SMBUtilities.WriteFile(index, "deletemetemptest.txt", "your\\\\folder\\\\path", false, stream, "UTF-8");
		
		Object listing = SMBUtilities.ListDirectory(index, "your\\\\folder\\\\path", null);
		
		Object createResult = SMBUtilities.CreateDirectory(index, "your\\\\folder\\\\path");
		
		Object deleteResult = SMBUtilities.DeleteDirectory(index, "your\\\\folder\\\\path", false);
		
		SMBUtilities.Disconnect(index);
	}
}
