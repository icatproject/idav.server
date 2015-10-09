import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;


public class TestDirectoryMove {

	private static final String UNZIP_TEMP_DIR_STEM = "unzip_temp_";

	private static File referenceLogFile;

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
		referenceLogFile = new File(TestUtils.testProperties.getProperty("reference.log.file"));
    	// TODO - modify the special chars string to add % chars once this is fixed in ICAT
		specialCharsString = TestUtils.testProperties.getProperty("special.chars.string");
	}

	@Test
	public void testNormalDirName() {
		System.out.println("Runnning TestDirecotoryMove with normal dir name");
		moveDirectoryWithName(UNZIP_TEMP_DIR_STEM + TestUtils.getCurrentDateTimeString());
	}
	
	@Test
	public void testSpecialCharsDirName() {
		System.out.println("Runnning TestDirecotoryMove with special chars dir name");
		moveDirectoryWithName(specialCharsString + TestUtils.getCurrentDateTimeString());
	}
	
	/**
	 * Unzip a file containing a typical directory structure into a local temp directory
	 * and then create an overall MD5 sum for it.
	 * Then copy the directory onto the webdav server at the Dataset level.
	 * Rename the top level folder here (quick operation).
	 * Move the directory down to the next level.
	 * Rename the directory at the new level.
	 * Repeat moving and renaming until we have gone down 3 Datafile levels.
	 * Create an overall MD5 sum for the directory to check that the contents has
	 * not changed whilst the folder has been moving around and being renamed. 
	 */
	public void moveDirectoryWithName(String dirName) {
		try {
			TestTimer timer = new TestTimer();

			File unzipTempDir = new File(dirName);
			TestUtils.unzipFile(TestUtils.UPLOAD_ZIP_FILE, unzipTempDir);

			List<File> startFilesList = TestUtils.createOrderedListOfFiles(unzipTempDir);
			TestUtils.writeMd5sumsToFile(startFilesList, unzipTempDir, referenceLogFile);
			String refMd5sum = TestUtils.getMd5sumForFile(referenceLogFile);
			
			String uploadDirName = unzipTempDir.getName();
			String uploadDirNameRenamed = uploadDirName + "_renamed";

			TestUtils.createFolders(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name, datafileDir3Name);
			File datasetDir = TestUtils.generateFileObject(facilityName, invName, datasetName);
			File datafileDir1 = TestUtils.generateFileObject(facilityName, invName, datasetName, datafileDir1Name);
			File datafileDir2 = TestUtils.generateFileObject(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name);
			File datafileDir3 = TestUtils.generateFileObject(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name, datafileDir3Name);
			
			// copy the directory to the "dataset" directory
			File datasetDirFile = new File(datasetDir, uploadDirName);
			System.out.println("Uploading directory to dataset dir");
			timer.reset();
			FileUtils.copyDirectory(unzipTempDir, datasetDirFile, false);
			System.out.println("Upload took " + timer.getElapsedTimeSecs() + " secs");
			// rename the directory in the "dataset" directory
			System.out.println("Renaming directory in datasetDir");
			File datasetDirRenamedFile = new File(datasetDir, uploadDirNameRenamed);
			timer.reset();
			FileUtils.moveDirectory(datasetDirFile, datasetDirRenamedFile);
			System.out.println("Rename took " + timer.getElapsedTimeSecs() + " secs");
			// move the directory to the "datafileDir1" directory
			System.out.println("Moving directory to datafileDir1");
			File datafileDir1RenamedFile = new File(datafileDir1, uploadDirNameRenamed);
			timer.reset();
			FileUtils.moveDirectory(datasetDirRenamedFile, datafileDir1RenamedFile);
			System.out.println("Move took " + timer.getElapsedTimeSecs() + " secs");
			// rename the directory in the "datafileDir1" directory
			System.out.println("Renaming directory in datafileDir1");
			File datafileDir1File = new File(datafileDir1, uploadDirName);
			timer.reset();
			FileUtils.moveDirectory(datafileDir1RenamedFile, datafileDir1File);
			System.out.println("Rename took " + timer.getElapsedTimeSecs() + " secs");
			// move the directory to the "datafileDir2" directory
			System.out.println("Moving directory to datafileDir2");
			File datafileDir2File = new File(datafileDir2, uploadDirName);
			timer.reset();
			FileUtils.moveDirectory(datafileDir1File, datafileDir2File);
			System.out.println("Move took " + timer.getElapsedTimeSecs() + " secs");
			// rename the directory in the "datafileDir2" directory
			System.out.println("Renaming directory in datafileDir2");
			File datafileDir2RenamedFile = new File(datafileDir2, uploadDirNameRenamed);
			timer.reset();
			FileUtils.moveDirectory(datafileDir2File, datafileDir2RenamedFile);
			System.out.println("Rename took " + timer.getElapsedTimeSecs() + " secs");
			// move the directory to the "datafileDir3" directory
			System.out.println("Moving directory to datafileDir3");
			File datafileDir3File = new File(datafileDir3, uploadDirNameRenamed);
			timer.reset();
			FileUtils.moveDirectory(datafileDir2RenamedFile, datafileDir3File);
			System.out.println("Move took " + timer.getElapsedTimeSecs() + " secs");
			// rename the directory in the "datafileDir3" directory
			System.out.println("Renaming directory in datafileDir3");
			File datafileDir3RenamedFile = new File(datafileDir3, uploadDirName);
			timer.reset();
			FileUtils.moveDirectory(datafileDir3File, datafileDir3RenamedFile);
			System.out.println("Rename took " + timer.getElapsedTimeSecs() + " secs");
			
			// TODO - check the directory after each move?
			// checking it here doesn't pin down which move caused it to change
			List<File> endFilesList = TestUtils.createOrderedListOfFiles(unzipTempDir);
			TestUtils.writeMd5sumsToFile(endFilesList, unzipTempDir, referenceLogFile);
			String endMd5sum = TestUtils.getMd5sumForFile(referenceLogFile);
			
			if (refMd5sum.equals(endMd5sum)) {
				System.out.println("MD5 sums match");
			} else {
				fail("MD5 sum check failed - refMd5sum: " + refMd5sum + ", endMd5sum: " + endMd5sum);
			}
			
			// tidy up by deleting the the whole test facility directory
			TestUtils.deleteFolderRecursive(facilityName);
			// delete the locally unzipped dir that was used for the upload(s)
			TestUtils.deleteDirectory(unzipTempDir);  
			referenceLogFile.delete();
			System.out.println("Test passed");
			
		} catch (Exception e) {
			fail(e.getClass().getSimpleName() + " : " + e.getMessage());
		}
	}

}
