import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import junit.framework.TestCase;
import ucsdh.SMBUtilities;

@RunWith(Suite.class)
@SuiteClasses({})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AllTests extends TestCase {
	
	
	private static class ConnectionManager {
		public static String accountPassword = null;
		public static String utilitiesIndex = "";
		public static String directoryListDirectoryPath = "path\\to\\folder\\to\\create";
		public static String allTestsDirectoryPath = "path\\to\test\folder";
		public static String writeFileName = "junit_test_file2.txt";
		public static String  writeFileContents = "This is the test file contents";
		
		public static void Setup() throws Exception {
			if(accountPassword == null) {
				//setup this password in your junit build configuration
				//if you use this, you will also need to put the accountPassword variable into the SMBUtilities.Connection call below (line 44)
				accountPassword = System.getProperty("service_account_password");
			}
			
			utilitiesIndex = SMBUtilities.Connect("your_hostname", "your_username", "your_password", "your_domain", "your_share_name");

		}
		
		public static void TearDown() {
			SMBUtilities.Disconnect(utilitiesIndex);
		}
	}
	
	//@BeforeAll
	@BeforeEach
	public void RunSetup() throws Exception {
		try {
		ConnectionManager.Setup();
		}
		catch (Exception e) {
			System.out.println(e.toString());
			throw e;
		}
	}
	
	//@AfterAll
	@AfterEach
	public void RunTeardown() {
		ConnectionManager.TearDown();
	}
		
	@Test
	@Order(1)
	public void TestBasic() {
		assertEquals(true, true);
	}
	
	@Test
	@Order(2)
	public void TestIndexCreatedSuccessfully() {
		Boolean testResult = ConnectionManager.utilitiesIndex != null && !ConnectionManager.utilitiesIndex.isEmpty();
		assertTrue(testResult);
	}
	
	@Test
	@Order(3)
	public void TestCreateDirectory() {
		Object createResult = SMBUtilities.CreateDirectory(ConnectionManager.utilitiesIndex, ConnectionManager.allTestsDirectoryPath + "testfoldertobedeleted");
		Boolean testResult = createResult.equals("Directory already exists") || createResult.equals("Directory was created");
		assertTrue(testResult);
	}
	
	@Test
	@Order(4)
	public void TestDeleteDirectory() {
		Object deleteResult = SMBUtilities.DeleteDirectory(ConnectionManager.utilitiesIndex,
				ConnectionManager.allTestsDirectoryPath + "testfoldertobedeleted", true);
		Boolean testResult = deleteResult.equals("Directory does not exist")
				|| deleteResult.equals("Directory was deleted");
		assertTrue(testResult);
	}
	 
	
	@Test
	@Order(5)
	public void TestListDirectory() {
		Object testResult = SMBUtilities.ListDirectory(ConnectionManager.utilitiesIndex, ConnectionManager.directoryListDirectoryPath, "*.*");
		
		Boolean isStringArray = testResult instanceof String[];
		
		assertNotNull(testResult);
		assertTrue(isStringArray);
	}
	
	@Test
	@Order(6)
	public void TestDeleteFile() throws IOException {
		Object deleteFileResult = SMBUtilities.DeleteFile(ConnectionManager.utilitiesIndex, ConnectionManager.writeFileName, ConnectionManager.allTestsDirectoryPath);
		Boolean testResult = deleteFileResult.equals("The file does not exist") || deleteFileResult.equals("File successfully deleted");
		assertTrue(testResult);
	}
	
	@Test
	@Order(7)
	public void TestWriteFile_String() throws Exception {
		//String utilitiesIndex, String fileName, String directoryName, Boolean appendToFile, Object fileContent, String encoding
		
		//delete the file if it already exists
		SMBUtilities.DeleteFile(ConnectionManager.utilitiesIndex, ConnectionManager.writeFileName, ConnectionManager.allTestsDirectoryPath);
		
		Object testResult = SMBUtilities.WriteFile(ConnectionManager.utilitiesIndex, ConnectionManager.writeFileName, ConnectionManager.allTestsDirectoryPath, false, ConnectionManager.writeFileContents, "utf-8");
		assertEquals("The file has been written.", testResult);
	}
	
	@Test
	@Order(8)
	public void TestReadFile() throws IOException {
		Object testResult = SMBUtilities.ReadFile(ConnectionManager.utilitiesIndex, ConnectionManager.writeFileName, ConnectionManager.allTestsDirectoryPath);
		InputStream resultStream = (InputStream)testResult;
		assertNotNull(resultStream);
		
		String readFileContentsResult = "";
		Scanner s = new Scanner(resultStream).useDelimiter("\\A");
		while(s.hasNext()) {
			readFileContentsResult += s.hasNext() ? s.next() : "";
		}		
		
		assertEquals(ConnectionManager.writeFileContents, readFileContentsResult);
	}
	
	@Test
	@Order(9)
	public void TestDeleteFile_Again() throws IOException {
		Object deleteFileResult = SMBUtilities.DeleteFile(ConnectionManager.utilitiesIndex, ConnectionManager.writeFileName, ConnectionManager.allTestsDirectoryPath);
		Boolean testResult = deleteFileResult.equals("The file does not exist") || deleteFileResult.equals("File successfully deleted");
		assertTrue(testResult);
	}
	
	@Test
	@Order(10)
	public void TestWriteFile_ByteArray() {
		//String utilitiesIndex, String fileName, String directoryName, Boolean appendToFile, Object fileContent, String encoding

		//delete the file if it already exists
		try {
			Object testResult = SMBUtilities.WriteFile(ConnectionManager.utilitiesIndex, ConnectionManager.writeFileName, ConnectionManager.allTestsDirectoryPath, false, ConnectionManager.writeFileContents.getBytes(), "utf-8");
			assertEquals("The file has been written.", testResult);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertTrue("An exception was thrown and logged to the console", false);
		}
	}
	
	@Test
	@Order(11)
	public void TestReadFile_Again() throws IOException {
		Object testResult = SMBUtilities.ReadFile(ConnectionManager.utilitiesIndex, ConnectionManager.writeFileName, ConnectionManager.allTestsDirectoryPath);
		InputStream resultStream = (InputStream)testResult;
		assertNotNull(resultStream);
		
		String readFileContentsResult = "";
		Scanner s = new Scanner(resultStream).useDelimiter("\\A");
		while(s.hasNext()) {
			readFileContentsResult += s.hasNext() ? s.next() : "";
		}		
		
		assertEquals(ConnectionManager.writeFileContents, readFileContentsResult);
	}
	
	@Test
	@Order(12)
	public void TestDeleteFile_Again_Again() throws IOException {
		Object deleteFileResult = SMBUtilities.DeleteFile(ConnectionManager.utilitiesIndex, ConnectionManager.writeFileName, ConnectionManager.allTestsDirectoryPath);
		Boolean testResult = deleteFileResult.equals("The file does not exist") || deleteFileResult.equals("File successfully deleted");
		assertTrue(testResult);
	}
	
	@Test
	@Order(13)
	public void TestWriteFile_InputStream() throws Exception {
		InputStream fileStream = new ByteArrayInputStream(ConnectionManager.writeFileContents.getBytes(StandardCharsets.UTF_8));
		Object testResult = SMBUtilities.WriteFile(ConnectionManager.utilitiesIndex, ConnectionManager.writeFileName, ConnectionManager.allTestsDirectoryPath, false, fileStream, "utf-8");
		assertEquals("The file has been written.", testResult);
	}
	
	@Test
	@Order(14)
	public void TestReadFile_Again_Again() throws IOException {
		Object testResult = SMBUtilities.ReadFile(ConnectionManager.utilitiesIndex, ConnectionManager.writeFileName, ConnectionManager.allTestsDirectoryPath);
		InputStream resultStream = (InputStream)testResult;
		assertNotNull(resultStream);
		
		String readFileContentsResult = "";
		Scanner s = new Scanner(resultStream).useDelimiter("\\A");
		while(s.hasNext()) {
			readFileContentsResult += s.hasNext() ? s.next() : "";
		}		
		
		Boolean areEqual = ConnectionManager.writeFileContents.equals(readFileContentsResult);
		
		assertEquals(ConnectionManager.writeFileContents, readFileContentsResult.trim());
	}
	
	@Test
	@Order(15)
	public void TestDeleteFile_Again_Again_Again() throws IOException {
		Object deleteFileResult = SMBUtilities.DeleteFile(ConnectionManager.utilitiesIndex, ConnectionManager.writeFileName, ConnectionManager.allTestsDirectoryPath);
		Boolean testResult = deleteFileResult.equals("The file does not exist") || deleteFileResult.equals("File successfully deleted");
		assertTrue(testResult);
	}
}
