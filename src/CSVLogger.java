import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.opencsv.CSVWriter;

/*
 * Little CSVWriter wrapper
 * */
public class CSVLogger {

	private CSVWriter log;	
	private String logname;
	
	public CSVLogger(String filename) {
		
		logname = filename;
		try { log = new CSVWriter(new FileWriter("csvLog/"+logname+".csv"), ','); } 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeLine(String[] line) {
		
		try {
			log.writeNext(line, false);
			log.flush();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeAll(List<String[]> lines) {
		
		try {
			log.writeAll(lines, false);
			log.flush();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() {
		
		try { log.close(); }
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
