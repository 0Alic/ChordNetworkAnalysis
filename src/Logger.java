import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {

	private BufferedWriter log;	
	private String logname;
	
	public Logger(String filename) {
		
		logname = filename;
		try {
		
			log = new BufferedWriter(new FileWriter("log/"+logname+".log"));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void write(String line) {
		
		try {

			log.write(line);
			log.newLine();
			log.flush();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		
		try {

			log.close();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
