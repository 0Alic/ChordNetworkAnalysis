import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/*
 * Class for a centralized Chord simulation
 * 
 * This class has the complete view of all the peers in the network and so it manages the network creation,
 * the finger tables creation and performs all the queries.
 * 
 * Since the number of peers in the network may reach an high number I used a TreeMap in order to store them.
 * TreeMap class stores its values sorted by the -key- of the map: this simplifies the creation of the 
 * finger tables, even though the insertion and lookup of an element will take O(log(n)) instead of the
 * usual O(1) of HashMaps. 
 * But since I need the keys sorted TreeMap avoids me to make a full copy of the identifiers into a different
 * data structure, sort it and use the copy to create the finger tables. As I said, since the number of peers may
 * be high, make a copy of all the peers may exceed the memory.
 * 
 * 
 * */
public class ChordCoordinator {

	private static ChordCoordinator instance = null;
	private static final int nW = 4;
	
	// Chord's
	private static int bits;
	private BigInteger maxId;
	private Map<BigInteger, Node> nodes;
	private SHAManager shaManager;
	
	// Loggers
	private Logger topologyLogger;
	private Logger simulationLogger;
	private CSVLogger cytoCsv;

	// Helpers
	private Random rnd;
	private boolean networkInitialized = false;

	
	/********************************************/
	/*				Constructor					*/
	/********************************************/

	public ChordCoordinator(int b) {
		
		// Input error handling
		if(b <= 0) {
			throw new RuntimeException("Negative or zero identifier input");
		}
		if(b > 160) {
			b = 160;
		}
		if((b % 4) != 0) {
			int bb = b + (4 - (b % 4));
			b = bb;
		}
		
		
		bits = b;
		this.nodes = new TreeMap<BigInteger, Node>();
		this.rnd = new Random();
		this.maxId = new BigInteger("2").pow(bits);
		this.shaManager = new SHAManager();
	}

	/* Get Singleton instance */
	public static ChordCoordinator getInstance(int b) {
		
		if(instance == null || bits != b)
			instance = new ChordCoordinator(b);
		
		return instance;
	}

	
	/********************************************/
	/*			Chord Network Methods			*/
	/********************************************/

	/*
	 * Generate a Chord-like network
	 * 
	 * n: number of nodes
	 * logId: the log file suffix
	 * */
	public void genNetwork(int n, String logId) {

		// Input error handling
		if(n <= 0) {
			throw new RuntimeException("Negative number of peers input");
		}
		if(new BigInteger(""+n).compareTo(maxId) > 0) {
			throw new RuntimeException("Too many nodes inserted");
		}

		// Helper variables
			// Counter
		int i = 0;
		
		// Reset current data
		nodes.clear();	
		topologyLogger = new Logger("topology"+logId);
		cytoCsv = new CSVLogger(logId);

		System.out.println("Creating nodes...");
		//
		// Generate the peers
		//
		
		while(i < n) {
			// Generate node
			BigInteger peerCode = genPeerCode();

			if(nodes.get(peerCode) == null) {
				// if that id is not taken, insert				
				nodes.put(peerCode, new Node(peerCode, maxId));
				i++;
			}
		}

		//
		// Build finger tables
		//
		System.out.println("Creating fingers...");
		
		// Multi-threaded finger table creation
		ArrayList<Worker> workers = new ArrayList<Worker>();
		ArrayList<BigInteger> peerList = new ArrayList<BigInteger>(nodes.keySet());
		int offset = (int) Math.ceil((double) peerList.size() / (double) nW);
		
		for(int t=0; t<nW; t++) {
			// Split the key set for each worker
			int first = t*offset;
			int last = (t+1)*offset;
			if(last > peerList.size())
				last = peerList.size();
			
			List<BigInteger> portion = peerList.subList(first, last);
			workers.add(new Worker(nodes, portion, this, bits));
			workers.get(t).start(); // start thread
			
			first = last;
		}
		
		for(int t=0; t<nW; t++) {
			// Wait for the threads and get the finger tables in the csv file
			try { 
				workers.get(t).join();
				cytoCsv.writeAll(workers.get(t).getFingerLines());
				}
			catch (InterruptedException e) { e.printStackTrace(); }
		}
		
		
		//
		// Set all the node's predecessors
		//
		System.out.println("Setting predecessor of each node");
		for(BigInteger peer : nodes.keySet()) {
			// I am the predecessor of my successor
			Node node = nodes.get(peer);
			Node nodeSucc = nodes.get(node.getSuccessor());
			nodeSucc.setPredecessor(peer);
		}

		
		//
		// Done, write information in files
		//
		for(BigInteger peer : nodes.keySet()) {
			topologyLogger.write(nodes.get(peer).getDescriptor());
		}

		topologyLogger.close();
		cytoCsv.close();
		
		networkInitialized = true;
	}
	

	/*
	 * Simulate a query for a random key by each node in the network
	 * 
	 * logId: the log file suffix
	 * simTimes: the number of queries issued by each node
	 * */
	public SimulationResults simulate(String logId, int simTimes) {

		// Check whether the network is initialized or not
		if(!networkInitialized) {
			throw new RuntimeException("Network not initialized. Initialize the network with genNetwork(n, logId)");
		}


		// Helper variables
			// Simulation results
		int totalHops = 0;
		Map<Integer, Integer> hopsMap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> queryMap = new HashMap<Integer, Integer>();	

		simulationLogger = new Logger("simulation"+logId);

		
		//
		// Perform simulation
		// For each peer generate a key and query it
		//
			// Helper variables to store hop count and history
		int hops = 0;
		ArrayList<String> hopNodes = new ArrayList<String>();

		for(int i=0; i<simTimes; i++) {
			for(BigInteger peer : nodes.keySet()) {
				// peer performs the query
				
				hops = 0;
				hopNodes.clear();
				
				// generate the key to query and its holder
				BigInteger key = genPeerCode();
				BigInteger keyHolder = findFinger(key);
				BigInteger current = peer;
				
				nodes.get(keyHolder).addQuery(); // remember that the keyHolder peer receives one more query
	
				while(!current.equals(keyHolder)) {
					// while the query hasn't reached the peer storing the key
					Node nd = nodes.get(current);
					current = nd.findSuccessor(key);
					
					hops++;
					hopNodes.add(""+current);
				}

				// Write log line for the simulation log file
				simulationLogger.write("Peer\t" + peer + 
										"\tquery\t" + key + 
										"\tholder\t" + keyHolder + 
										"\tnodes\t" + hopNodes + 
										"\thops\t" + hops);
				
				addToDistributionMap(hopsMap, hops);
				totalHops += hops;
			}
		}
		
		//
		// End phase
		//
		simulationLogger.close();
		
		// Build the query distribution map
		for(BigInteger peer : nodes.keySet()) {
			Node node = nodes.get(peer);
			addToDistributionMap(queryMap, node.getQueries());
			node.resetQueries();
		}
		
		// Compute average hops
		double avgHops = ((double) totalHops) / ((double) nodes.size()*simTimes);

		// Return the result bundle
		return new SimulationResults(avgHops, hopsMap, queryMap);
	}

		
	/********************************************/
	/*				Helper Methods				*/
	/********************************************/

	/*
	 * Generate the code for a peer in the network
	 * Truncate the SHA1 hexadecimal code in case bits are < 160
	 * This still ensure uniformity
	 * */
	private BigInteger genPeerCode() {
		
		int len = bits / 4;
		String fullHex = shaManager.getSHA("" + rnd.nextLong());
		String hex = fullHex.substring(fullHex.length() - len);

		return new BigInteger(hex, 16);
	}

	
	/*
	 * Given a target find the first clockwise finger node
	 * 
	 * target: the target identifier
	 * */
	public BigInteger findFinger(BigInteger target) {
		
		BigInteger first = null;
		boolean isFirst = false;
		
		for(BigInteger peerId : nodes.keySet()) {
			// bt is the candidate finger
		
			if(!isFirst) {
				first = peerId;
				isFirst = true;
			}
			
			if(peerId.compareTo(target) >= 0)
				// bt >= target
				return peerId;
		}

		if(first == null) {
			throw new RuntimeException("No Finger found for " + target);
		}
		
		return first;
	}
	
	/*
	 * Add a key to a distribution map: if the key is already present, increment its value by one
	 * 
	 * map: the distribution map
	 * key: the key map
	 * */
	private void addToDistributionMap(Map<Integer, Integer> map, int key) {
		
		if(map.get(key) == null)
			map.put(key, 1);
		else
			map.put(key, map.get(key)+1);

	}
}
