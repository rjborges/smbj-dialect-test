package ucsdh;

import com.hierynomus.smbj.*;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.Set;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2ShareAccess;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import com.hierynomus.smbj.share.Share;

/**
 * This class is a wrapper for the smbj library. It contains static and non-static functionality 
 * in order to meet the needs of the Mulesoft connector we've developed.
 */
public class SMBUtilities {
	private SMBClient _smbClient = null;
	private Connection _smbConnection = null;
	private AuthenticationContext _smbAuthContext = null;
	private Session _smbSession = null;
	private DiskShare _smbShare = null;

	private String _smbHost = "";//put your hostname here
	private String _smbUsername = "";//username
	private String _smbPassword = "";//password
	private String _smbDomain = "";//active directory domain
	private String _smbShareName = "";//smb share name

	/**
	 * Constructor
	 * 
	 * @param host The server we're connecting to
	 * @param username SMB Username (most likely Active Directory account)
	 * @param password Password for the given user
	 * @param domain Active Directory Domain (ad)
	 * @param shareName The name of the SMB Share that we will be accessing
	 */
	public SMBUtilities(String host, String username, String password, String domain, String shareName) {
		this._smbHost = host;// 
		this._smbUsername = username;
		this._smbPassword = password;
		this._smbDomain = domain;
		this._smbShareName = shareName;
	}


	/**
	 * A collection containing all of the initialized SMB Connections (SMBUtilities instances)
	 */
	private static HashMap<String, SMBUtilities> _utilsCollection = new HashMap<>();

	/**
	 * A static way to initialize an SMB connection
	 * 
	 * @param host The server we're connecting to
	 * @param username SMB Username (most likely Active Directory account)
	 * @param password Password for the given user
	 * @param domain Active Directory Domain (ad)
	 * @param shareName The name of the SMB Share that we will be accessing
	 * @return A static location index where the non-static SMBUtilities objects are stored
	 * @throws java.io.IOException An index determining where the 
	 */
	public static String Connect(String host, String username, String password, String domain, String shareName)
			throws java.io.IOException {
		SMBUtilities utils = new SMBUtilities(host, username, password, domain, shareName);
		utils.Initialize();

		UUID objectIndex = UUID.randomUUID();
		String indexAsString = objectIndex.toString();
		_utilsCollection.put(indexAsString, utils);

		return indexAsString;
	}

	/**
	 * A static way to uninitialize an SMB connection for a specific class instance
	 * 
	 * @param utilitiesIndex utilitiesIndex index of the SMBUtilities object that the current call will be using
	 * @return A string indicating success or failure
	 */
	public static String Disconnect(String utilitiesIndex) {
		if (_utilsCollection.containsKey(utilitiesIndex) == true) {
			SMBUtilities instance = _utilsCollection.get(utilitiesIndex);
			String result = instance.Uninitialize();

			return result;
		}

		return "Could not find Utilites index in the hashmap.";
	}

	/**
	 * Sets up all of the necessary SMB object connections, so we don't have to do it in every function
	 * 
	 * @throws IOException
	 */
	public void Initialize() throws IOException {
		SMB2Dialect [] supportedSmdDialects = {
	            SMB2Dialect.SMB_2_1
	            //SMB2Dialect.SMB_3_1_1
	    };
	    SmbConfig cfg = SmbConfig.builder().
	            withDialects(supportedSmdDialects).
	            withMultiProtocolNegotiate(true).
	            //withSigningRequired(false).
	            build();
	            
		this._smbClient = new SMBClient(cfg);
		this._smbConnection = _smbClient.connect(_smbHost);
		this._smbAuthContext = new AuthenticationContext(_smbUsername, _smbPassword.toCharArray(), _smbDomain);
		this._smbSession = _smbConnection.authenticate(_smbAuthContext);
		
		
		/*
		  RPCTransport transport = SMBTransportFactories.SRVSVC.getTransport(this._smbSession); 
		  ServerService serverService = new ServerService(transport); 
		  final List<NetShareInfo0> shares =  serverService.getShares0();
		  for (final NetShareInfo0 share : shares) {
		      System.out.println(share);
		  }
		 */
		
		
		Share smbShare = _smbSession.connectShare(_smbShareName);
		this._smbShare = (DiskShare) smbShare;
	}

	/**
	 * Shuts down all SMB connections in the current class instance
	 * 
	 * @return A default success message
	 */
	public String Uninitialize() {
		try {
			this._smbSession.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			this._smbConnection.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			this._smbClient.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "Successfully disconnected";
	}

	/**
	 * Deletes a file
	 * 
	 * @param utilitiesIndex utilitiesIndex utilitiesIndex index of the SMBUtilities object that the current call will be using
	 * @param fileName The name of the file being deleted
	 * @param directoryName The folder where the file is stored
	 * @return A string indicating success or failure
	 * @throws IOException The most likely exceptions will be related to the file's or folder's existence
	 */
	public static String DeleteFile(String utilitiesIndex, String fileName, String directoryName) throws IOException {
		if (_utilsCollection.containsKey(utilitiesIndex)) {
			SMBUtilities instance = _utilsCollection.get(utilitiesIndex);
			String result = instance.DeleteFileInternal(fileName, directoryName);
			return result;
		}

		// Boolean returnValue = _utils.DeleteFileInternal(fileName, directoryName);
		// utils = null;
		return "Could not find Utilites index in the hashmap.";
	}

	 /**
	 * Deletes a file
	 * 
	 * @param fileName The name of the file being deleted
	 * @param directoryName The folder where the file is stored
	 * @return A string indicating success or failure
	 * @throws IOException The most likely exceptions will be related to the file's or folder's existence
	 */
	public String DeleteFileInternal(String fileName, String directoryName) throws IOException {
		
		String fullPath = directoryName + "\\" + fileName;

		Boolean fileExists = this._smbShare.fileExists(fullPath);
		
		if(fileExists) {
			this._smbShare.rm(fullPath);
			return "File successfully deleted";
		}
		else {
			return "The file does not exist";
		}		
	}

	/**
	 * Stores a file to the given destination
	 * 
	 * @param utilitiesIndex utilitiesIndex utilitiesIndex index of the SMBUtilities object that the current call will be using
	 * @param fileName The name of the file when stored
	 * @param directoryName The folder where the file will be stored
	 * @param appendToFile If the file exists, append or overwrite
	 * @param fileContent A byte[], String, or InputStream representing the contents of the file
	 * @param encoding Not used yet. UTF-8 is the default
	 * @return A string indicating whether or not the file storage was successful
	 * @throws Exception The most likely exceptions will be related to overwrite access, or the location not existing
	 */
	public static String WriteFile(String utilitiesIndex, String fileName, String directoryName, Boolean appendToFile,
		Object fileContent, String encoding) throws Exception {
		if (_utilsCollection.containsKey(utilitiesIndex)) {
			SMBUtilities instance = _utilsCollection.get(utilitiesIndex);
			String result = instance.WriteFileInternal(fileName, directoryName, appendToFile, fileContent, encoding);
			return result;
		}

		return "Could not find Utilites index in the hashmap.";
	}

	/**
	 * Stores a file to the given destination
	 * 
	 * @param fileName The name of the file when stored
	 * @param directoryName The folder where the file will be stored
	 * @param appendToFile If the file exists, append or overwrite
	 * @param fileContent A byte[], String, or InputStream representing the contents of the file
	 * @param encoding Not used yet. UTF-8 is the default
	 * @return A string indicating whether or not the file storage was successful
	 * @throws Exception The most likely exceptions will be related to overwrite access, or the location not existing
	 */
	public String WriteFileInternal(String fileName, String directoryName, Boolean appendToFile, Object fileContent,
		String encoding) throws Exception {
		
		Logger logger = Logger.getLogger(SMBUtilities.class.getName()); 

		//check if the file already exists
		Boolean fileExists = false;

		logger.log(Level.INFO, "Checking if the file");
		for (FileIdBothDirectoryInformation f : this._smbShare.list(directoryName, "*.*")) {
			System.out.println("File : " + f.getFileName());
			if(f.getFileName().equalsIgnoreCase(fileName)){
				fileExists = true;

				logger.log(Level.INFO, "The file exists");
			}
		}

		Set<FileAttributes> fileAttributes = new HashSet<>();
		fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_NORMAL);
		Set<SMB2CreateOptions> createOptions = new HashSet<>();
		createOptions.add(SMB2CreateOptions.FILE_RANDOM_ACCESS);

		logger.log(Level.INFO, "Setting up file write mode");
		//determine how the file will be written
		SMB2CreateDisposition writeMode = SMB2CreateDisposition.FILE_SUPERSEDE;
		if(fileExists == true && appendToFile == true) {
			//this mode will append to the existing file
			writeMode = SMB2CreateDisposition.FILE_OPEN_IF;
			logger.log(Level.INFO, "mode is: SMB2CreateDisposition.FILE_OPEN_IF");
		} 
		else if(fileExists == true && appendToFile == false) {
			//this mode overwrites the file if it exists
			writeMode = SMB2CreateDisposition.FILE_OVERWRITE;
			logger.log(Level.INFO, "mode is: SMB2CreateDisposition.FILE_OVERWRITE");
		}
		else {
			//file not exists case. we will create the file
			writeMode = SMB2CreateDisposition.FILE_CREATE;
			logger.log(Level.INFO, "mode is: SMB2CreateDisposition.FILE_CREATE");
		}
		
		byte[] fileContentFinal = null;
		if(fileContent instanceof String) {
			fileContentFinal = fileContent.toString().getBytes();
		}
		else if(fileContent instanceof byte[]) {
			fileContentFinal = (byte[])fileContent;
		}
		else if(fileContent instanceof InputStream) {
			InputStream fileContentStream = (InputStream)fileContent;
			
			/*byte[] buffer = new byte[1024]; 
			int length; 
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			while ((length = fileContentStream.read(buffer))> 0) { 
				output.write(buffer);
			}*/
			
			int nRead;
		    byte[] data = new byte[4];
		    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		    while ((nRead = fileContentStream.read(data, 0, data.length)) != -1) {
		        buffer.write(data, 0, nRead);
		    }
		    buffer.flush();
		  
			 fileContentFinal = buffer.toByteArray();
		}
		else {
			throw new Exception("Invalid file content detected. Use either byte[], String, or InputStream");
		}

		File f = this._smbShare.openFile(directoryName + "\\" + fileName,
			new HashSet(Arrays.asList(new AccessMask[] { AccessMask.GENERIC_ALL })), 
						fileAttributes,
						SMB2ShareAccess.ALL, 
						writeMode, 
						createOptions);
		
		OutputStream oStream = f.getOutputStream();
		oStream.write(fileContentFinal);
		oStream.flush();
		oStream.close();

		return "The file has been written.";
	}

	/**
	 * Gets a file's contents 
	 * 
	 * @param utilitiesIndex utilitiesIndex index of the SMBUtilities object that the current call will be using
	 * @param fileName The name of the file
	 * @param directoryName The folder where the file will be read
	 * @return An InputStream object
	 * @throws java.io.IOException The most likely error will be that the file does not exist
	 */
	public static Object ReadFile(String utilitiesIndex, String fileName, String directoryName ) throws java.io.IOException {
			if (_utilsCollection.containsKey(utilitiesIndex)) {
				SMBUtilities instance = _utilsCollection.get(utilitiesIndex);
				Object result = instance.ReadFileInternal(fileName, directoryName);
				return result;
			}
	
			return "Could not find Utilites index in the hashmap.";
	}

	/**
	 * Gets a file's contents
	 * 
	 * @param fileName The name of the file
	 * @param directoryName The folder where the file will be read
	 * @return An InputStream object
	 * @throws java.io.IOException The most likely error will be that the file does not exist
	 */
	public Object ReadFileInternal(String fileName, String directoryName ) throws java.io.IOException {
		String filePathAndName = directoryName + "\\" + fileName;
		Set<SMB2ShareAccess> shareAccess = new HashSet<>();
        shareAccess.add(SMB2ShareAccess.FILE_SHARE_READ); // this is to get READ only
        com.hierynomus.smbj.share.File remoteSmbjFile =  this._smbShare.openFile(filePathAndName, EnumSet.of(AccessMask.GENERIC_READ), EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL), EnumSet.of(SMB2ShareAccess.FILE_SHARE_READ), SMB2CreateDisposition.FILE_OPEN, null);
        InputStream is = remoteSmbjFile.getInputStream();

		return is;
	}
	
	/**
	 * Creates a folder
	 * 
	 * @param utilitiesIndex utilitiesIndex index of the SMBUtilities object that the current call will be using
	 * @param directoryPathToBeCreated Name of the folder that will be created
	 * @return A message indicating success or failure
	 */
	public static Object CreateDirectory(String utilitiesIndex, String directoryPathToBeCreated) {
		if (_utilsCollection.containsKey(utilitiesIndex)) {
			SMBUtilities instance = _utilsCollection.get(utilitiesIndex);
			Object result = instance.CreateDirectoryInternal(directoryPathToBeCreated);
			return result;
		}

		return "Could not find Utilites index in the hashmap.";
	}
	
	/**
	 * Creates a folder
	 * 
	 * @param directoryPathToBeCreated Name of the folder that will be created
	 * @return A message indicating success or failure
	 */
	public Object CreateDirectoryInternal(String directoryPathToBeCreated) {
		
		if(this._smbShare.folderExists(directoryPathToBeCreated)) {
			return "Directory already exists";
		}
		else {
			this._smbShare.mkdir(directoryPathToBeCreated);		
			return "Directory was created";
		}
	}
	
	/**
	 * Deletes a folder
	 * 
	 * @param utilitiesIndex utilitiesIndex index of the SMBUtilities object that the current call will be using
	 * @param directoryPathToBeDeleted Folder that will be deleted
	 * @param recursiveDelete whether to do a recursive delete
	 * @return A message indicating success or failure
	 */
	public static Object DeleteDirectory(String utilitiesIndex, String directoryPathToBeDeleted, Boolean recursiveDelete) {
		if (_utilsCollection.containsKey(utilitiesIndex)) {
			SMBUtilities instance = _utilsCollection.get(utilitiesIndex);
			Object result = instance.DeleteDirectoryInternal(directoryPathToBeDeleted, recursiveDelete);
			return result;
		}

		return "Could not find Utilites index in the hashmap.";
	}
	
	/***
	 * Deletes a folder
	 * 
	 * @param directoryPathToBeDeleted Folder that will be deleted
	 * @param recursiveDelete whether to do a recursive delete
	 * @return A message indicating success or failure
	 */
	public Object DeleteDirectoryInternal(String directoryPathToBeDeleted, Boolean recursiveDelete) {		
		if(this._smbShare.folderExists(directoryPathToBeDeleted)) {
			this._smbShare.rmdir(directoryPathToBeDeleted, recursiveDelete);
			return "Directory was deleted";
		}
		else {	
			return "Directory does not exist";
		}
	}
	

	/**
	 * Lists all items in the given folder, matching the search term (wildcard)
	 * 
	 * @param utilitiesIndex index of the SMBUtilities object that the current call will be using
	 * @param directoryPathToBeListed The folder to be created
	 * @param wildcard sear
	 * @return A List object representing the items found
	 */
	public static Object ListDirectory(String utilitiesIndex, String directoryPathToBeListed, String wildcard) {
		if (_utilsCollection.containsKey(utilitiesIndex)) {
			SMBUtilities instance = _utilsCollection.get(utilitiesIndex);
			Object result = instance.ListDirectoryInternal(directoryPathToBeListed, wildcard);
			return result;
		}

		return "Could not find Utilites index in the hashmap.";
	}
	
	/**
	 * Lists all items in the given folder, matching the search term (wildcard)
	 * 
	 * @param directoryPathToBeListed The folder to be created
	 * @param wildcard sear
	 * @return A List object representing the items found
	 */
	public Object ListDirectoryInternal(String directoryPathToBeListed, String wildcard) {
		if(this._smbShare.folderExists(directoryPathToBeListed)) {
			List<FileIdBothDirectoryInformation> dirResult = null;
			if(wildcard == null || wildcard.isEmpty()) {
				dirResult = this._smbShare.list(directoryPathToBeListed);
			}
			else {
				dirResult = this._smbShare.list(directoryPathToBeListed, wildcard);
			}
			
			String[] returnValue = new String[dirResult.size()];
			int i = 0;
			
			for (FileIdBothDirectoryInformation f : dirResult) {
				returnValue[i] = f.getFileName();
				i++;
			}
			
			return returnValue;
		}
		else {	
			return null;
		}
	}
}