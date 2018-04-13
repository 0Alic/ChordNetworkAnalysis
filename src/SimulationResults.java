import java.util.Map;

public class SimulationResults {

	private double avgHops;
	// Map to help the construction of a bar graph
		// Occurrence of each hop 
	private Map<Integer, Integer> hopsPerformed;
		// Occurrence of received queries
	private Map<Integer, Integer> queriesReceived;

	public SimulationResults(double avgHops, Map<Integer, Integer> hopsPerformed, Map<Integer, Integer> queriesPerformed) {
		
		this.avgHops = avgHops;
		this.hopsPerformed = hopsPerformed;
		this.queriesReceived = queriesPerformed;
	}
	
	public double getAvgHops() {
		return this.avgHops;
	}
	
	public Map<Integer, Integer> getHopDistribution() {
		return this.hopsPerformed;
	}
	
	public Map<Integer, Integer> getQueryDistribution() {
		return this.queriesReceived;
	}
}
