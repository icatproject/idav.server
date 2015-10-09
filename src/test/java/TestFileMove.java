import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;


public class TestFileMove {

	private static String facilityName;
	private static String invName;
	private static String datasetName;
	private static String datafileDir1Name;
	private static String datafileDir2Name;
	private static String datafileDir3Name;
	private static String specialCharsString;
		
	@BeforeClass
	public static void classSetUp() throws IOException {
		facilityName = TestUtils.testProperties.getProperty("facility.name");
		invName = TestUtils.testProperties.getProperty("investigation.name");
		datasetName = TestUtils.testProperties.getProperty("dataset.name");
		datafileDir1Name = TestUtils.testProperties.getProperty("datafile.dir1.name");
		datafileDir2Name = TestUtils.testProperties.getProperty("datafile.dir2.name");
		datafileDir3Name = TestUtils.testProperties.getProperty("datafile.dir3.name");
    	// TODO - modify the special chars string to add % chars once this is fixed in ICAT
		specialCharsString = TestUtils.testProperties.getProperty("special.chars.string");
	}

	@Test
	public void testNormalFilename() {
		System.out.println("Runnning TestFileMove with normal filename");
		File fileToUpload = new File(TestUtils.UPLOAD_ZIP_FILE);
		testMoveUsingFile(fileToUpload);
	}
	
	@Test
	public void testSpecialCharsFilename() {
		System.out.println("Runnning TestFileMove with special chars filename");
		File originalFile = new File(TestUtils.UPLOAD_ZIP_FILE);
		File specialCharsFile = new File(specialCharsString + ".zip");
		// make a copy of the upload zip file
		try {
			FileUtils.copyFile(originalFile, specialCharsFile);
		} catch (IOException e) {
			fail("IOException making a copy of the file to upload: " + e.getMessage());
		}
		testMoveUsingFile(specialCharsFile);
		specialCharsFile.delete();
	}

	public void testMoveUsingFile(File fileToUpload) {
		try {
			TestTimer timer = new TestTimer();

			String startMd5sum = TestUtils.getMd5sumForFile(fileToUpload);
			String uploadFilename = fileToUpload.getName();
			String uploadFilenameRenamed = uploadFilename.replace(".", "_renamed.");

			TestUtils.createFolders(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name, datafileDir3Name);
			File datasetDir = TestUtils.generateFileObject(facilityName, invName, datasetName);
			File datafileDir1 = TestUtils.generateFileObject(facilityName, invName, datasetName, datafileDir1Name);
			File datafileDir2 = TestUtils.generateFileObject(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name);
			File datafileDir3 = TestUtils.generateFileObject(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name, datafileDir3Name);
			
			// copy the file to the "dataset" directory
			File datasetDirFile = new File(datasetDir, uploadFilename);
			System.out.println("Uploading file to dataset dir");
			timer.reset();
			FileUtils.copyFile(fileToUpload, datasetDirFile);
			System.out.println("Upload took " + timer.getElapsedTimeSecs() + " secs");
			// rename the file in the "dataset" directory
			System.out.println("Renaming file in datasetDir");
			File datasetDirRenamedFile = new File(datasetDir, uploadFilenameRenamed);
			timer.reset();
			FileUtils.moveFile(datasetDirFile, datasetDirRenamedFile);
			System.out.println("Rename took " + timer.getElapsedTimeSecs() + " secs");
			// move the file to the "datafileDir1" directory
			System.out.println("Moving file to datafileDir1");
			File datafileDir1RenamedFile = new File(datafileDir1, uploadFilenameRenamed);
			timer.reset();
			FileUtils.moveFile(datasetDirRenamedFile, datafileDir1RenamedFile);
			System.out.println("Move took " + timer.getElapsedTimeSecs() + " secs");
			// rename the file in the "datafileDir1" directory
			System.out.println("Renaming file in datafileDir1");
			File datafileDir1File = new File(datafileDir1, uploadFilename);
			timer.reset();
			FileUtils.moveFile(datafileDir1RenamedFile, datafileDir1File);
			System.out.println("Rename took " + timer.getElapsedTimeSecs() + " secs");
			// move the file to the "datafileDir2" directory
			System.out.println("Moving file to datafileDir2");
			File datafileDir2File = new File(datafileDir2, uploadFilename);
			timer.reset();
			FileUtils.moveFile(datafileDir1File, datafileDir2File);
			System.out.println("Move took " + timer.getElapsedTimeSecs() + " secs");
			// rename the file in the "datafileDir2" directory
			System.out.println("Renaming file in datafileDir2");
			File datafileDir2RenamedFile = new File(datafileDir2, uploadFilenameRenamed);
			timer.reset();
			FileUtils.moveFile(datafileDir2File, datafileDir2RenamedFile);
			System.out.println("Rename took " + timer.getElapsedTimeSecs() + " secs");
			// move the file to the "datafileDir3" directory
			System.out.println("Moving file to datafileDir3");
			File datafileDir3File = new File(datafileDir3, uploadFilenameRenamed);
			timer.reset();
			FileUtils.moveFile(datafileDir2RenamedFile, datafileDir3File);
			System.out.println("Move took " + timer.getElapsedTimeSecs() + " secs");
			// rename the file in the "datafileDir3" directory
			System.out.println("Renaming file in datafileDir3");
			File datafileDir3RenamedFile = new File(datafileDir3, uploadFilename);
			timer.reset();
			FileUtils.moveFile(datafileDir3File, datafileDir3RenamedFile);
			System.out.println("Rename took " + timer.getElapsedTimeSecs() + " secs");
			
			// TODO - check the file after each move?
			// checking it here doesn't pin down which move caused it to change
			String endMd5sum = TestUtils.getMd5sumForFile(datafileDir3RenamedFile);
			
			if (startMd5sum.equals(endMd5sum)) {
				System.out.println("MD5 sums match");
			} else {
				fail("MD5 sum check failed - startMd5sum: " + startMd5sum + ", endMd5sum: " + endMd5sum);
			}
			
			// tidy up by deleting the the whole test facility directory
			TestUtils.deleteFolderRecursive(facilityName);
			System.out.println("Test passed");
			
		} catch (Exception e) {
			fail(e.getClass().getSimpleName() + " : " + e.getMessage());
		}
	}

}
