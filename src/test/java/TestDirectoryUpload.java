import static org.junit.Assert.*;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;


public class TestDirectoryUpload {

	private static final String UNZIP_TEMP_DIR_STEM = "unzip_temp_";

	private static File referenceLogFile;

	private static String facilityName;
	private static String invName;

	private static int numRepetitions;
 
	private static final String NUM_REPETITIONS_PROPERTY = "directory.upload.num.repetitions";

	@BeforeClass
	public static void classSetUp() throws Exception {
		facilityName = TestUtils.testProperties.getProperty("facility.name");
		invName = TestUtils.testProperties.getProperty("investigation.name");
		referenceLogFile = new File(TestUtils.testProperties.getProperty("reference.log.file")); 
		String numRepetitionsString = TestUtils.testProperties.getProperty(NUM_REPETITIONS_PROPERTY);
		try {
			numRepetitions = Integer.parseInt(numRepetitionsString);
		} catch (NumberFormatException e) {
			throw new Exception("Property '" + NUM_REPETITIONS_PROPERTY + "' must be an integer - found '" + numRepetitionsString + "'", e);
		}
	}

	@Test
	public void test() {
		try {
			File unzipTempDir = new File(UNZIP_TEMP_DIR_STEM + TestUtils.getCurrentDateTimeString());
			TestUtils.unzipFile(TestUtils.UPLOAD_ZIP_FILE, unzipTempDir);

			List<File> filesList = TestUtils.createOrderedListOfFiles(unzipTempDir);
			double folderSizeMB = TestUtils.getFolderSizeMB(filesList);
			System.out.println("folderSizeMB = " + folderSizeMB);

			TestUtils.writeMd5sumsToFile(filesList, unzipTempDir, referenceLogFile);
			String refMd5sum = TestUtils.getMd5sumForFile(referenceLogFile);

			double cumulativeTransferSpeed = 0;
			for (int i=1; i<=numRepetitions; i++) {
				System.out.println("Starting repetition " + i + " of " + numRepetitions);
				File uploadDir = TestUtils.createFolders(facilityName, invName);
				System.out.println("uploadDir = '" + uploadDir + "'");
	
				long startMs = System.currentTimeMillis();
				// note that without specifying preserveFileDate=false in the FileUtils.copyDirectory
				// call below the copy can fail with an IllegalArgumentException (Negative time)
				// this is possibly because some directories (virtual ones at the datafile level, I think)
				// are returned with a timestamp of now and if the clock on the webdav server is ahead of
				// the clock on the client then this may appear to be in the future
				try {
					System.out.println("Uploading directory to webdav ...");
					FileUtils.copyDirectory(unzipTempDir, uploadDir, false);
				} catch (Exception e) {
					fail(e.getClass().getSimpleName() + " copying directory '" + unzipTempDir + "' to '" + uploadDir + "' : " + e.getMessage());
				}
				long endMs = System.currentTimeMillis();
				double secsTaken = (endMs-startMs)/1000.0;
				double MBperSec = folderSizeMB/secsTaken;
				cumulativeTransferSpeed += MBperSec;
				System.out.println("Upload took " + secsTaken + " secs (average " + MBperSec + " MB/sec)");
	
				// check all the files got there correctly
				List<File> uploadedFilesList = TestUtils.createOrderedListOfFiles(uploadDir);
				String dateTimeString = TestUtils.getCurrentDateTimeString();
				File logFile = new File(dateTimeString + ".log");
				TestUtils.writeMd5sumsToFile(uploadedFilesList, uploadDir, logFile);
				String logMd5sum = TestUtils.getMd5sumForFile(logFile);
	
				if (logMd5sum.equals(refMd5sum)) {
					// delete the whole facility on webdav
					TestUtils.deleteFolderRecursive(facilityName);  
					// delete the log file use to check all the MD5 sums
					logFile.delete();								
				} else {
					fail("Log file MD5 sums do not match: refMd5sum=" + refMd5sum + " logMd5sum=" + logMd5sum);
				}
			}
			// delete the locally unzipped dir that was used for the upload(s)
			TestUtils.deleteDirectory(unzipTempDir);  
			referenceLogFile.delete();
			double averageTransferSpeed = cumulativeTransferSpeed/numRepetitions;
			System.out.println("Average transfer speed was " + averageTransferSpeed + " MB/sec");
			System.out.println("Test passed");
		} catch (Exception e) {
			fail(e.getClass().getSimpleName() + " : " + e.getMessage());
		}
	}

}
