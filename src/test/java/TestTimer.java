
public class TestTimer {

	private long startTimeMs;
	
	public TestTimer() {
		startTimeMs = System.currentTimeMillis();
	}
	
	public void reset() {
		startTimeMs = System.currentTimeMillis();
	}

	public double getElapsedTimeSecs() {
		long currentTimeMs = System.currentTimeMillis();
		return (currentTimeMs - startTimeMs) / 1000.0; 
	}
	
	public static void main(String[] args) throws Exception {
		TestTimer timer = new TestTimer();
		Thread.sleep(1234);
		System.out.println( timer.getElapsedTimeSecs() );
//		timer.reset();
		Thread.sleep(2345);
		System.out.println( timer.getElapsedTimeSecs() );
	}
}
