import java.util.Map;

public class Main {

	public static void main(String[] args) {
		
		if(args.length < 2)
			throw new RuntimeException("Too few input arguments: -b -n -times=1");
		
		int b = Integer.parseInt(args[0]);
		int n = Integer.parseInt(args[1]);
		int queries = 1;
		
		if(args.length >= 3) 
			queries = Integer.parseInt(args[2]);
		
		ChordCoordinator chord = ChordCoordinator.getInstance(b);
		String logId = b + "b_" + n + "n";

		
		System.out.println("Generating Chord-like network with an identifiers space spanned by " + b + " bits and involving " + n + " peers");
		chord.genNetwork(n, logId);
		
		System.out.println("Starting simulation");
		SimulationResults resultBundle = chord.simulate(logId, queries);
		
		System.out.println(resultBundle.getAvgHops());
		
		
		// Logging Distribtions
		log(resultBundle.getHopDistribution(), b, n, queries, "Queries");
		log(resultBundle.getQueryDistribution(), b, n, queries, "Hops");

		System.out.println("Done");
	}
	
	
	/*
	 * Helper method to log the distributions in a csv file
	 * */
	public static void log(Map<Integer, Integer> distributionMap, int b, int n, int queryPerNode, String measure) {

		CSVLogger queryOcc = new CSVLogger("Occ/b"+b+"_n"+n+"_q"+ +queryPerNode+ "_" + measure + "_queries_distribution");
		String[] headerOcc = {measure, "Occ"};
		
		queryOcc.writeLine(headerOcc);
		
		for(Map.Entry<Integer, Integer> pair : distributionMap.entrySet()) {
			// line: query - occurrence
			String[] l = {""+pair.getKey(), ""+pair.getValue()};
			queryOcc.writeLine(l);
		}
		queryOcc.close();
	}
}
