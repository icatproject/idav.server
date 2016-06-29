import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.PathFileComparator;
import org.apache.commons.io.filefilter.TrueFileFilter;


public class TestUtils {

	private static final String PROPERTIES_FILE = "src/test/resources/test.properties";
	static final String UPLOAD_ZIP_FILE = "src/test/resources/SlikSvn.zip";
	static final String JUNIT_SUFFIX = " (JUNIT)";
	private static final int MAX_DELETION_ATTEMPTS = 10;
	private static final int MAX_MD5SUM_ATTEMPTS = 10;
	private static String drive;
	
	static Properties testProperties = new Properties();
	
	static {
		try {
			InputStream inStream = new FileInputStream(PROPERTIES_FILE);
			testProperties.load(inStream);
			inStream.close();
			System.out.println("Loaded properties from file");
			drive = TestUtils.testProperties.getProperty("webdav.drive.mapping");
		} catch (IOException e) {
			System.err.println("IOException loading properties from file: " + e.getMessage());
		}
	}
	
	static String getCurrentDateTimeString() {
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy-HH.mm.ss");
        return format.format(now);
	}
	
	static void createSingleFolder(String... dirNames) throws Exception {
		File dir = generateFileObject(dirNames);
		System.out.println("Creating folder '" + dir + "'");
		try {
			Files.createDirectory(dir.toPath());
		} catch (IOException e) {
			throw new Exception(e.getClass().getSimpleName() + " creating folder '" + dir + "' : " + e.getMessage());
		}
	}

	static File createFolders(String... dirNames) throws Exception {
		File dir = generateFileObject(dirNames);
		System.out.println("Creating folder(s) '" + dir + "'");
		try {
			Files.createDirectories(dir.toPath());
		} catch (IOException e) {
			throw new Exception(e.getClass().getSimpleName() + " creating folder(s) '" + dir + "' : " + e.getMessage());
		}
		return dir;
	}
	
	static void deleteEmptyFolder(String... dirNames) throws Exception {
		File dir = generateFileObject(dirNames);
		System.out.println("Deleting folder '" + dir + "'");
		try {
			Files.delete(dir.toPath());
		} catch (IOException e) {
			throw new Exception(e.getClass().getSimpleName() + " deleting empty folder '" + dir + "' : " + e.getMessage());
		}
	}

	
	static void deleteFolderRecursive(String... dirNames) throws Exception {
		File dir = generateFileObject(dirNames);
		deleteDirectory(dir);
	}

	/**
	 * Method to wrap the call to FileUtils.deleteDirectory which seems to regularly fail
	 * on both local and webdav directory deletions but which then succeeds if you wait a
	 * bit and then try again. Experience shows that it should only fail a few times before
	 * succeeding so the number of attempts is limited to a sensible number so that it
	 * does not go on trying forever.
	 * 
	 * Currently I presume that the failures are due to the copy or the md5summer 
	 * not having released all the files yet.
	 * 
	 * @param dir  the directory to recursively delete
	 * @throws Exception  if the directory has not been deleted 
	 * 					  after a limited number of attempts
	 */
	static void deleteDirectory(File dir) throws Exception {
		boolean dirDeleted = false;
		int i=0;
		while (!dirDeleted && i<MAX_DELETION_ATTEMPTS) {
			try {
				i++;
				FileUtils.deleteDirectory(dir);  
				dirDeleted = true;
			} catch (IOException e) {
				System.err.println("Pausing one second before retrying directory deletion of '" + dir + "': " + e.getMessage());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					System.err.println("Caught InterruptedException: " + e1.getMessage());
				}
			}
		}
		if (i>=MAX_DELETION_ATTEMPTS) {
			throw new Exception("Failed to delete directory '" + dir + "' after " + MAX_DELETION_ATTEMPTS + " attempts");
		}
	}
	
    static double getFolderSizeMB(List<File> filesList) {
    	long folderSizeBytes = 0;
    	for (File file : filesList) {
    		folderSizeBytes += file.length();
    	}
    	return (double)folderSizeBytes/1024/1024;
    }

    static void writeMd5sumsToFile(List<File> filesList, File topLevelDir, File logFile) throws IOException {
		String newLine = System.getProperty("line.separator");
		FileWriter fileWriter = new FileWriter(logFile);

		for (File file : filesList) {
			String md5sum = getMd5sumForFile(file);
			String line = md5sum + " : " + file.getAbsolutePath().substring(topLevelDir.getAbsolutePath().length()+1);
//			System.out.println(line);
			fileWriter.write(line + newLine);
		}
		
		fileWriter.close();
    }

    // When calling this method to check files on a webdav drive using the multi-threaded checker
    // it occasionally reports that a file cannot be found. The loop to retry the file a given
    // number of times continues to fail, and according to the idav.log on the server the file
    // is only requested once (successfully!) from the server.
    // Currently I presume that this is a problem with the Windows Webdav implementation 
    // not being able to handle multiple files being accessed concurrently, as the problem 
    // does not happen when the files are fetched serially using the non-threaded checker.
    // A possible solution to this, and one which would potentially also allow for faster
    // checking would be to convert the file path to a URL for a webdav GET request and
    // get the file directly from the webdav server.
    static String getMd5sumForFile(File file) throws IOException {
		int i=1;
		IOException ioe = null;
		while (i<=MAX_MD5SUM_ATTEMPTS) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				String md5sum = DigestUtils.md5Hex(in);
				return md5sum;
			} catch (IOException e) {
				System.err.println("Error getting MD5 sum for '" + file + "' at attempt number " + i + " : " + e.getMessage());
				ioe = e;
				i++;
				// pause before trying again
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					System.err.println("Caught InterruptedException: " + e1.getMessage());
				}
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
					// prevent this being thrown from the method
				}
			}
		}
		// if we get this far then throw the last Exception we caught
		throw ioe;
    }
    
    static List<File> createOrderedListOfFiles(File topLevelDir) {
		Collection<File> files = FileUtils.listFiles(topLevelDir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		List<File> filesList = new ArrayList<File>(files);
		PathFileComparator pathFileComparator = new PathFileComparator();
		Collections.sort(filesList, pathFileComparator);
		return filesList;
    }
    
    static File generateFileObject(String... dirNames) {
		String dirPath = drive + ":";
		for (String dirName : dirNames) {
			dirPath += "\\" + dirName + JUNIT_SUFFIX;
		}
		return new File(dirPath);
	}
    
	/**
	 * Unzip a file into a specified output folder
	 */
	static void unzipFile(String zipFile, File outputFolder) throws IOException {

		byte[] buffer = new byte[1024];

		// create output directory is not exists
		if (!outputFolder.exists()) {
			outputFolder.mkdir();
		}

		// get the zip file content
		ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
		// get the zipped file list entry
		ZipEntry ze = zis.getNextEntry();

		while (ze != null) {

			String fileName = ze.getName();
			File newFile = new File(outputFolder + File.separator + fileName);

			System.out.println("Unzipping to: " + newFile.getAbsoluteFile());

			// create all non existing folders
			// otherwise you will get FileNotFoundException for compressed folder
			if (ze.isDirectory()) {
				new File(newFile.getParent()).mkdirs();
			} else {
				FileOutputStream fos = null;
				new File(newFile.getParent()).mkdirs();
				fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}
			ze = zis.getNextEntry();
		}

		zis.closeEntry();
		zis.close();

		System.out.println("Unzip complete");
	}

}
