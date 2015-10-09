import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;


public class TestLargeFileUpload {

	private static int uploadFileSizeMB = -1;
	private static final String A_64_BYTE_STRING = "A string that is 64 bytes with a Windows carriage return added\r\n";

	private static String testUploadFilename;
	private static String facilityName;
	private static String invName;
	private static String datasetName;

	private static int numRepetitions;

	private static final String UPLOAD_FILE_SIZE_PROPERTY = "upload.file.size.MB";
	private static final String NUM_REPETITIONS_PROPERTY = "large.file.upload.num.repetitions";

	@BeforeClass
	public static void classSetUp() throws Exception {
		String uploadFileSizeMBString = TestUtils.testProperties.getProperty(UPLOAD_FILE_SIZE_PROPERTY);
		try {
			uploadFileSizeMB = Integer.parseInt(uploadFileSizeMBString);
		} catch (NumberFormatException e) {
			throw new Exception("Property '" + UPLOAD_FILE_SIZE_PROPERTY + "' must be an integer - found '" + uploadFileSizeMBString + "'", e);
		}
		String numRepetitionsString = TestUtils.testProperties.getProperty(NUM_REPETITIONS_PROPERTY);
		try {
			numRepetitions = Integer.parseInt(numRepetitionsString);
		} catch (NumberFormatException e) {
			throw new Exception("Property '" + NUM_REPETITIONS_PROPERTY + "' must be an integer - found '" + numRepetitionsString + "'", e);
		}
		testUploadFilename = TestUtils.testProperties.getProperty("test.upload.filename");
		facilityName = TestUtils.testProperties.getProperty("facility.name");
		invName = TestUtils.testProperties.getProperty("investigation.name");
		datasetName = TestUtils.testProperties.getProperty("dataset.name");
	}

	@Test
	public void test() {
		try {
			TestTimer timer = new TestTimer();
			File fileToUpload = new File(testUploadFilename);
			System.out.println("uploadFileSizeMB = " + uploadFileSizeMB);
			int numLineRepetitions = 16 * 1024 * uploadFileSizeMB;
			FileWriter fileWriter = new FileWriter(fileToUpload);
			for (int i=0; i<numLineRepetitions; i++) {
				fileWriter.write(A_64_BYTE_STRING);
			}
			fileWriter.close();
			String localMD5sum = TestUtils.getMd5sumForFile(fileToUpload);
			System.out.println("localMD5sum = " + localMD5sum);
			
			double cumulativeUploadSpeed = 0;
			double cumulativeDownloadSpeed = 0;
			for (int i=1; i<=numRepetitions; i++) {
				System.out.println("Starting repetition " + i + " of " + numRepetitions);
				File uploadDir = TestUtils.createFolders(facilityName, invName, datasetName);
				File destinationFile = new File(uploadDir, testUploadFilename);
				System.out.println("destinationFile = '" + destinationFile + "'");
				
				timer.reset();
				try {
					FileUtils.copyFile(fileToUpload, destinationFile);
				} catch (Exception e) {
					fail(e.getClass().getSimpleName() + " copying file '" + fileToUpload + "' to '" + destinationFile + "' : " + e.getMessage());
				}
				double secsTakenUpload = timer.getElapsedTimeSecs();
				double uploadMBperSec = uploadFileSizeMB/secsTakenUpload;
				cumulativeUploadSpeed += uploadMBperSec;
				System.out.println("Upload took " + secsTakenUpload + " secs (average " + uploadMBperSec + " MB/sec)");
	
				timer.reset();
				String remoteMD5sum = TestUtils.getMd5sumForFile(destinationFile);
				double secsTakenDownload = timer.getElapsedTimeSecs();
				double downloadMBperSec = uploadFileSizeMB/secsTakenDownload;
				cumulativeDownloadSpeed += downloadMBperSec;
				System.out.println("Download (and MD5 sum) took " + secsTakenDownload + " secs (average " + downloadMBperSec + " MB/sec)");
				System.out.println("remoteMD5sum = " + remoteMD5sum);
				
				if ( localMD5sum.equals(remoteMD5sum) ) {
					System.out.println("MD5 sums match");
				} else {
					fail("local and remote MD5 sums do not match: localMD5sum=" + localMD5sum + " remoteMD5sum=" + remoteMD5sum);
				}
				// delete the whole facility on webdav
				TestUtils.deleteFolderRecursive(facilityName);
			}
			// clean up
			if ( !fileToUpload.delete() ) {
				fail("Problems deleting local file: '" + fileToUpload + "'");
			}
			double averageUploadSpeed = cumulativeUploadSpeed/numRepetitions;
			System.out.println("Average upload speed was " + averageUploadSpeed + " MB/sec");
			double averageDownloadSpeed = cumulativeDownloadSpeed/numRepetitions;
			System.out.println("Average download speed was " + averageDownloadSpeed + " MB/sec");

			System.out.println("Test passed");
			
		} catch (Exception e) {
			fail(e.getClass().getSimpleName() + " : " + e.getMessage());
		}
		
	}

}
