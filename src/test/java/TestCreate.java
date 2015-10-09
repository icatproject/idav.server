import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;


public class TestCreate {

	private static String facilityName;
	private static String invName;
	private static String datasetName;
	private static String datafileDir1Name;
	private static String datafileDir2Name;
	private static String datafileDir3Name;
	
	@BeforeClass
	public static void classSetUp() throws IOException {
		facilityName = TestUtils.testProperties.getProperty("facility.name");
		invName = TestUtils.testProperties.getProperty("investigation.name");
		datasetName = TestUtils.testProperties.getProperty("dataset.name");
		datafileDir1Name = TestUtils.testProperties.getProperty("datafile.dir1.name");
		datafileDir2Name = TestUtils.testProperties.getProperty("datafile.dir2.name");
		datafileDir3Name = TestUtils.testProperties.getProperty("datafile.dir3.name");
	}
	
	@Test
	public void testCreateAndDeleteFolderSingle() {
		try {
			TestUtils.createSingleFolder(facilityName);
			TestUtils.createSingleFolder(facilityName, invName);
			TestUtils.createSingleFolder(facilityName, invName, datasetName);
			TestUtils.createSingleFolder(facilityName, invName, datasetName, datafileDir1Name);
			TestUtils.createSingleFolder(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name);
			TestUtils.createSingleFolder(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name, datafileDir3Name);
			TestUtils.deleteEmptyFolder(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name, datafileDir3Name);
			TestUtils.deleteEmptyFolder(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name);
			TestUtils.deleteEmptyFolder(facilityName, invName, datasetName, datafileDir1Name);
			TestUtils.deleteEmptyFolder(facilityName, invName, datasetName);
			TestUtils.deleteEmptyFolder(facilityName, invName);
			TestUtils.deleteEmptyFolder(facilityName);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCreateAndDeleteFolderRecursive() {
		try {
			TestUtils.createFolders(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name, datafileDir3Name);
			TestUtils.deleteFolderRecursive(facilityName);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

/*
	@Test
	public void testCreateFacility() {
		try {
			TestUtils.createFolder(facilityName);
			TestUtils.deleteFolder(facilityName);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCreateInvestigation() {
		try {
			TestUtils.createFolder(facilityName, invName);
			TestUtils.deleteFolder(facilityName, invName);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCreateDataset() {
		try {
			TestUtils.createFolder(facilityName, invName, datasetName);
			TestUtils.deleteFolder(facilityName, invName, datasetName);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCreateDatafileDir1() {
		try {
			TestUtils.createFolder(facilityName, invName, datasetName, datafileDir1Name);
			TestUtils.deleteFolder(facilityName, invName, datasetName, datafileDir1Name);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCreateDatafileDir2() {
		try {
			TestUtils.createFolder(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name);
			TestUtils.deleteFolder(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testCreateDatafileDir3() {
		try {
			TestUtils.createFolder(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name, datafileDir3Name);
			TestUtils.deleteFolder(facilityName, invName, datasetName, datafileDir1Name, datafileDir2Name, datafileDir3Name);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
*/

}
