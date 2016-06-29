import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;


public class FileListMD5erThread extends Thread {

	private List<File> fileList;
	private File logFilePart;
	
	public FileListMD5erThread(List<File> fileList, File logFilePart) {
		this.fileList = fileList;
		this.logFilePart = logFilePart;
	}
	
	public void run() {
		System.out.println("Writing MD5 sums to log file: " + logFilePart + " : " + new Date());
		try {
			TestUtils.writeMd5sumsToFile(fileList, MD5CheckerThreaded.TOP_LEVEL_DIR, logFilePart);
		} catch (IOException e) {
			System.err.println("Error writing MD5 sums to file: " + logFilePart.getAbsolutePath() + " : " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("Finished writing MD5 sums to log file: " + logFilePart + " : " + new Date());
	}
	
	
}
