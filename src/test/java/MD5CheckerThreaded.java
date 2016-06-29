import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;


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
 * The multi-threaded checker should be able to reduce the time taken from the
 * current 3ish hours taken by the single-threaded checker to around 1 hour
 * for the 13120000 Monney (2013) investigation containing 31.4 GB in around 
 * 76,000 files.
 * 
 * The intention is that this will replace the single-threaded checker once the
 * problem described in TestUtils getMd5sumForFile is resolved.
 *
 */
public class MD5CheckerThreaded {
	
	static final File TOP_LEVEL_DIR = new File("X:\\Kevin\\13120000 Monney (2013) - Win Exp copy 29_09_15");
	static final String EXPT_MD5SUMS_LOG = "artemis_expt_md5s_multi_threaded_check_Win_Exp_copy_29_09_15.log";
	
//	static final File TOP_LEVEL_DIR = new File("C:\\Program Files\\SlikSvn");
//	static final String EXPT_MD5SUMS_LOG = "artemis_expt_md5s_SlikSvn_ref.log"; 

//	static final File TOP_LEVEL_DIR = new File("V:\\13120000 Monney (2013)\\Raw Data");
//	static final String EXPT_MD5SUMS_LOG = "artemis_expt_md5s_Monney_Raw_Data_ref.log"; 

//	static final File TOP_LEVEL_DIR = new File("V:\\13120000 Monney (2013)");
//	static final String EXPT_MD5SUMS_LOG = "artemis_expt_md5s_Monney_ref.log"; 

	static final int NUM_THREADS = 4;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println("Started: " + new Date());
		File logFile = new File(EXPT_MD5SUMS_LOG);
		System.out.println("Creating ordered list of files: " + new Date());
		List<File> filesList = TestUtils.createOrderedListOfFiles(TOP_LEVEL_DIR);
		List<List<File>> subListsOfFiles = createSublistsOfFiles(filesList);
		List<Thread> threadList = new ArrayList<Thread>();
		List<File> logFilePartsList = new ArrayList<File>();
		System.out.println("Splitting MD5ing into threads: " + new Date());
		for (int threadCount=0; threadCount<subListsOfFiles.size(); threadCount++) {
			File logFilePart = new File(MD5CheckerThreaded.EXPT_MD5SUMS_LOG + "." + threadCount);
			logFilePartsList.add(logFilePart);
			FileListMD5erThread md5erThread = new FileListMD5erThread(subListsOfFiles.get(threadCount), logFilePart);
			threadList.add(md5erThread);
			md5erThread.start();
		}
		while (true) {
			boolean allThreadsFinished = true;
			for (Thread md5erThread : threadList) {
				if (md5erThread.isAlive()) {
					allThreadsFinished = false;
					break;
				}
			}
			if (allThreadsFinished) {
				break;
			} else {
				// sleep for 1 sec then check the threads again
				Thread.sleep(1000);
			}
		}
		
		// combine the log files into one
		PrintWriter logFileWriter = new PrintWriter(logFile);
		for (File logFilePart : logFilePartsList) {
			Scanner scanner = new Scanner(logFilePart);
			scanner.useDelimiter(System.getProperty("line.separator"));
			while (scanner.hasNext()) {
				logFileWriter.println(scanner.next());
			}
			scanner.close();
		}
		logFileWriter.close();
		
		String refMd5sum = TestUtils.getMd5sumForFile(logFile);
		System.out.println("MD5 sum for log file is: " + refMd5sum);		
		System.out.println("Finished: " + new Date());
	}
	
	private static List<List<File>> createSublistsOfFiles(List<File> fileList) {
		int numFiles = fileList.size();
		int incrementVal = (fileList.size()/NUM_THREADS)+1;
		System.out.println("incrementVal: " + incrementVal);
		int fromIndex = 0;
		int toIndex = incrementVal;
		List<List<File>> listOfFileLists = new ArrayList<List<File>>();
		while (true) {
			System.out.println(fromIndex + " to " + toIndex);
			List<File> subList = fileList.subList(fromIndex, toIndex);
			listOfFileLists.add(subList);
			fromIndex += incrementVal;
			toIndex += incrementVal;
			if (toIndex > numFiles) {
				toIndex = numFiles;
				if (fromIndex != toIndex) {
					System.out.println(fromIndex + " to " + toIndex);
					List<File> lastSubList = fileList.subList(fromIndex, toIndex);
					listOfFileLists.add(lastSubList);
				}
				break;
			}
		}
		return listOfFileLists;
	}
	
}
