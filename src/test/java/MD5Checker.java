import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;


/**
 * @author rqw38472
 *
 * Program to generate a file containing a list of all the MD5 sums for all
 * of the files below that point in the file system.
 * 
 * The list is ordered such that it can be easily compared to another file 
 * created from another directory structure that is supposed to be identical.
 * 
 * This is used for comparing entire directory structures uploaded and
 * downloaded from the webdav server.
 * 
 * As this is single threaded and checks one file at a time sequentially it 
 * takes around 3 hours to check the 13120000 Monney (2013) investigation
 * containing 31.4 GB in around 76,000 files.
 * 
 * The intention is to replace this with a multi-threaded checker once the
 * problem described in TestUtils getMd5sumForFile is resolved.
 *
 */
public class MD5Checker {
	
//	private static final String DIR_PATH = "V:\\13120000 Monney (2013)";
//	private static final String DIR_PATH = "Y:\\13120000 Monney (2013)";
//	private static final String DIR_PATH = "Y:\\13120000 Monney (2013)\\Lab Data";
//	private static final String DIR_PATH = "Y:\\13120000 Monney (2013)\\Lab Data\\Diagnostics";
//	private static final String DIR_PATH = "X:\\Kevin\\13120000 Monney (2013) - BitKinex10 copy 05_10_15";
	private static final String DIR_PATH = "C:\\Users\\rqw38472\\temp\\13120000 Monney (2013) - BitKinex10 copy 05_10_15";

//	private static final String EXPT_MD5SUMS_LOG = "artemis_expt_md5s.log"; 
	private static final String EXPT_MD5SUMS_LOG = "artemis_expt_md5s_webdav_BitKinex10_download_06_10_15"; 
	

	public static void main(String[] args) throws IOException {
		System.out.println("Started: " + new Date());
		File topLevelDir = new File(DIR_PATH);
		File logFile = new File(EXPT_MD5SUMS_LOG);
		System.out.println("Creating ordered list of files: " + new Date());
		List<File> filesList = TestUtils.createOrderedListOfFiles(topLevelDir);
		System.out.println("Writing MD5 sums to log file: " + new Date());
		TestUtils.writeMd5sumsToFile(filesList, topLevelDir, logFile);
		String refMd5sum = TestUtils.getMd5sumForFile(logFile);
		System.out.println("MD5 sum for log file is: " + refMd5sum);		
		System.out.println("Finished: " + new Date());
	}
	
}
