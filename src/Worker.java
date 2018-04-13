import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Class describing a thread building finger tables of a set of fingers.
 * */
public class Worker extends Thread {

	private int bits;

	// Reference to the Chord's coordinator
	private ChordCoordinator coordinator;
	
	// Reference to the coordinator's peer structure
	private Map<BigInteger, Node> nodes;
	
	// Portion of peers to build finger tables
	private List<BigInteger> portion;

	// List of csv lines
	private ArrayList<String[]> csvLines;
	
	public Worker(	Map<BigInteger, Node> 	nodes, 
					List<BigInteger> 		peerPortion, 
					ChordCoordinator 		coordinator, 
					int 					bits) {
		
		this.bits = bits;
		this.nodes = nodes;
		this.portion = peerPortion;
		this.coordinator = coordinator;
		
		this.csvLines = new ArrayList<String[]>();
	}
	
	@Override
	public void run() {
		
		for(BigInteger peerId : portion) {
			// Build finger tables			
			Node node = nodes.get(peerId);
			BigInteger maxId = new BigInteger("2").pow(bits);

			// A set is used in order to have no duplicates while preserving insertion order
			Set<BigInteger> fingerTable = new LinkedHashSet<BigInteger>(); 
			
			for(int k=0; k<bits; k++) {
				// Build finger table for node node
				BigInteger target = new BigInteger("2").pow(k).add(peerId).mod(maxId);
				BigInteger finger = coordinator.findFinger(target);
				
				boolean inserted = fingerTable.add(finger);
				
				// Write peer-finger in file for CytoScape
				if(inserted) {
					String[] line = {""+peerId, ""+finger};
					csvLines.add(line);
				}
			}
			node.setFingerTable(fingerTable);
		}
	}

	public ArrayList<String[]> getFingerLines(){
		
		return csvLines;
	}
}
