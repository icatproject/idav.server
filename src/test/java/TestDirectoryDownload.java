import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;


public class TestDirectoryDownload {

	private static String facilityName;
	private static String invName;
	private static String datasetName;

	private static File referenceLogFile;
	private static String drive;
	private static String existingDownloadDirString;
	private static int numRepetitions;

	private static final String NUM_REPETITIONS_PROPERTY = "directory.download.num.repetitions";
	
	@BeforeClass
	public static void classSetUp() throws Exception {
		referenceLogFile = new File(TestUtils.testProperties.getProperty("reference.log.file")); 
		existingDownloadDirString = TestUtils.testProperties.getProperty("directory.to.download"); 
		drive = TestUtils.testProperties.getProperty("webdav.drive.mapping");
		String numRepetitionsString = TestUtils.testProperties.getProperty(NUM_REPETITIONS_PROPERTY);
		try {
			numRepetitions = Integer.parseInt(numRepetitionsString);
		} catch (NumberFormatException e) {
			throw new Exception("Property '" + NUM_REPETITIONS_PROPERTY + "' must be an integer - found '" + numRepetitionsString + "'", e);
		}
		facilityName = TestUtils.testProperties.getProperty("facility.name");
		invName = TestUtils.testProperties.getProperty("investigation.name");
		datasetName = TestUtils.testProperties.getProperty("dataset.name");
	}
	
	@Test
	public void test() {
		try {
			File tempDownloadDir = null;
			File downloadDir = null;
			if ( existingDownloadDirString != null ) {
				// use an existing folder specified by setting the property 'directory.to.download'
				downloadDir = new File(drive + ":\\" + existingDownloadDirString);
				System.out.println("Using existing folder for download test '" + downloadDir + "'");
			} else {
				// create a folder structure down to dataset level on the webdav server
				// and upload a test directory structure to it
				downloadDir = TestUtils.createFolders(facilityName, invName, datasetName);
				tempDownloadDir = downloadDir;
				System.out.println("Uploading test directory structure to webdav server ...");
				TestUtils.unzipFile(TestUtils.UPLOAD_ZIP_FILE, downloadDir);
			}
			List<File> filesList = TestUtils.createOrderedListOfFiles(downloadDir);
			double folderSizeMB = TestUtils.getFolderSizeMB(filesList);
			System.out.println("folderSizeMB = " + folderSizeMB);
			
			System.out.println("Creating " + referenceLogFile.getName());
			TestUtils.writeMd5sumsToFile(filesList, downloadDir, referenceLogFile);
			String refMd5sum = TestUtils.getMd5sumForFile(referenceLogFile);
	
			double cumulativeTransferSpeed = 0;
			for (int i=1; i<=numRepetitions; i++) {
				System.out.println("Starting repetition " + i + " of " + numRepetitions);
				String dateTimeString = TestUtils.getCurrentDateTimeString();
				File workingDir = new File(dateTimeString);
				boolean workingDirSuccess = workingDir.mkdir();
				if (workingDirSuccess) {
					System.out.print(i + ") Working dir: '" + dateTimeString + "' ");
				} else {
					System.err.println("Error creating working dir: '" + workingDir + "'");
				}
				long startMs = System.currentTimeMillis();
				// note that without specifying preserveFileDate=false in the FileUtils.copyDirectory
				// call below the copy can fail with an IllegalArgumentException (Negative time)
				// this is possibly because some directories (virtual ones at the datafile level, I think)
				// are returned with a timestamp of now and if the clock on the webdav server is ahead of
				// the clock on the client then this may appear to be in the future
				try {
					FileUtils.copyDirectory(downloadDir, workingDir, false);
				} catch (Exception e) {
					fail(e.getClass().getSimpleName() + " copying directory '" + downloadDir + "' to '" + workingDir + "' : " + e.getMessage());
				}
				long endMs = System.currentTimeMillis();
				double secsTaken = (endMs-startMs)/1000.0;
				double MBperSec = folderSizeMB/secsTaken;
				cumulativeTransferSpeed += MBperSec;
				System.out.println("Download took " + secsTaken + " secs (average " + MBperSec + " MB/sec)");
				
				List<File> copiedFilesList = TestUtils.createOrderedListOfFiles(workingDir);
				
				File logFile = new File(dateTimeString + ".log");
				TestUtils.writeMd5sumsToFile(copiedFilesList, workingDir, logFile);
				String logMd5sum = TestUtils.getMd5sumForFile(logFile);
				
				if (logMd5sum.equals(refMd5sum)) {
	//				System.out.println("Log file MD5 sums match");
					TestUtils.deleteDirectory(workingDir);
					logFile.delete();
				} else {
					fail("Log file MD5 sums do not match");
				}
	//			System.out.println();
			}
			referenceLogFile.delete();
			if (tempDownloadDir != null) {
				// delete the whole facility on webdav
				// specifically this only deletes a folder that has been temporarily uploaded 
				// for testing and not existing directories selected by setting 'directory.to.download'
				TestUtils.deleteFolderRecursive(facilityName);
			}
			double averageTransferSpeed = cumulativeTransferSpeed/numRepetitions;
			System.out.println("Average transfer speed was " + averageTransferSpeed + " MB/sec");
			System.out.println("Test passed");
		} catch (Exception e) {
			fail(e.getClass().getSimpleName() + " : " + e.getMessage());
		}
	}

}
