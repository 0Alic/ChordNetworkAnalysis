import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/*
 * Class for a centralized Chord simulation
 * 
 * This class has the complete view of all the peers in the network and so it manages the network creation,
 * the finger tables creation and performs all the queries.
 * 
 * */
public class ChordCoordinator {

	private static ChordCoordinator instance = null;
	
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
	private boolean networkInitialized;

	
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
			System.out.println("WARNING: b should be a multiple of 4; " + b + " changed to " + bb);
			b = bb;
		}
		
		
		bits = b;
		this.networkInitialized = false;
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
		if(new BigInteger(""+n).compareTo(maxId) > 0) {
			throw new RuntimeException("Too many nodes inserted");
		}

		System.out.println("Generating Chord-like network with an identifiers space spanned by " + bits + " bits and involving " + n + " peers");

		// Helper variables
			// Counter
		int i = 0;
		
		// Reset current data
		nodes.clear();
		topologyLogger = new Logger("topology"+logId);
		cytoCsv = new CSVLogger(logId);

		//
		// Generate the peers
		//
		System.out.println("Creating nodes...");

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
		
		for(BigInteger peerId : nodes.keySet()) {
			// Build finger tables			
			Node node = nodes.get(peerId);
			BigInteger maxId = new BigInteger("2").pow(bits);

			// A set is used in order to have no duplicates while preserving insertion order
			Set<BigInteger> fingerTable = new LinkedHashSet<BigInteger>(); 
			
			for(int k=0; k<bits; k++) {
				// Build finger table for node node
				BigInteger target = new BigInteger("2").pow(k).add(peerId).mod(maxId);
				BigInteger finger = findFinger(target);
				
				boolean inserted = fingerTable.add(finger);
				
				// Write peer-finger in file for CytoScape
				if(inserted) {
					String[] line = {""+peerId, ""+finger};
					cytoCsv.writeLine(line);
				}
			}
			node.setFingerTable(new ArrayList<BigInteger>(fingerTable));
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
	 * Simulate a query for a random key for each node in the network
	 * 
	 * logId: the log file suffix
	 * nQueries: the number of queries issued by each node
	 * 
	 * return: a bundle of simulation results
	 * */
	public SimulationResults simulate(String logId, int nQueries) {

		// Check whether the network is initialized or not
		if(!networkInitialized) {
			throw new RuntimeException("Network not initialized. Initialize the network with genNetwork(n, logId)");
		}


		// Helper variables
			// Simulation results
		int totalHops = 0;
		Map<Integer, Integer> hopsMap = new HashMap<Integer, Integer>();
		Map<Integer, Integer> queryMap = new HashMap<Integer, Integer>();	

		simulationLogger = new Logger("simulation"+ nQueries + "q_"+logId);

		
		//
		// Perform simulation
		// For each peer generate a key and query it
		//
			// Helper variables to store hop count and hop history
		int hops = 0;
		ArrayList<String> hopNodes = new ArrayList<String>();

		for(int i=0; i<nQueries; i++) {
			// each node performs nQueries queries
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
		double avgHops = ((double) totalHops) / ((double) nodes.size()*nQueries);

		// Return the result bundle
		return new SimulationResults(avgHops, hopsMap, queryMap);
	}

		
	/********************************************/
	/*				Helper Methods				*/
	/********************************************/

	/*
	 * Generate the code for a peer in the network
	 * Truncate the SHA1 hexadecimal code in case bits are < 160
	 * 
	 * return: a random peer identifier
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
	 * 
	 * return: the finger of that target
	 * */
	private BigInteger findFinger(BigInteger target) {
		
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
