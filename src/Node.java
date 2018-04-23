import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/*
 * Class which describes the node of a Chord network
 * */
public class Node {

	private static BigInteger maxValue;

	// Chord's
	private BigInteger nodeId;

	private ArrayList<BigInteger> fingers;
	private BigInteger successor = null;
	private BigInteger predecessor = null;
	
	// Utility
	private int queriesReceived = 0;
	
	/* Constructor */
	public Node(BigInteger nodeId, BigInteger max) {

		maxValue = max;
		this.nodeId = nodeId;
	}

	
	/********************************************/
	/*			Getters and Setters				*/
	/********************************************/

	// Finger Table
	public List<BigInteger> getFingerTable() {
		return this.fingers;
	}

	public void setFingerTable(List<BigInteger> table) {		

		this.fingers = new ArrayList<BigInteger>(table);
		this.successor = fingers.get(0);
	}

	// Successor
	public BigInteger getSuccessor() {
		return this.successor;
	}
	
	// Predecessor
	public BigInteger getPredecessor() {
		return this.predecessor;
	}
	
	public void setPredecessor(BigInteger prec) {
		this.predecessor = prec;
	}

	// Queries received
	public int getQueries() {
		return this.queriesReceived;
	}

	public void addQuery() {
		queriesReceived++;
	}
	
	public void resetQueries() {
		queriesReceived = 0;
	}

	// Descriptor of the node
	public String getDescriptor() {
		
		String s = "";
		s += "Node\t" + nodeId;
		s += "\tPredecessor\t"+predecessor;
		s += "\tSuccessor\t"+successor;
		s += "\tFingerSize\t"+fingers.size();
		s += "\tFingers\t"+fingers;

		return s;
	}
		
	

	/********************************************/
	/*				Chord's Methods				*/
	/********************************************/

	
	/*
	 * Find the successor of a chord target
	 * 
	 * target: the target node
	 * 
	 * return: the successor of the target
	 * */
	public BigInteger findSuccessor(BigInteger target) {

		if(checkInterval(target, this.predecessor, this.nodeId))
			// target in (predecessor, myID]
			return this.nodeId;
		else if (checkInterval(target, this.nodeId, this.successor))
			// target in (myID, successorID]
			return this.successor;
		else
			// Forward query
			return closestPrecedingNode(target);
	}
	
	
	/*
	 * Find the closest preceding node of target
	 * 
	 * target: the target node
	 * 
	 * return: the successor of the target
	 * */
	private BigInteger closestPrecedingNode(BigInteger target) {
				
		for(int i=fingers.size()-1; i>=0; i--) {
			
			BigInteger f = fingers.get(i);
			
			
			// finger[i] in (myId, targetId) iff
			//
			// finger[i] in (myId, targetId)
			// or
			// finger[i] in (myId, targetId+maxId) if target > myId
			// or
			// finger[i]+maxId in (myId, targetId+maxId) if target > myId
			if(isIn(f, this.nodeId, target))
				return f;
			
			else if(this.nodeId.compareTo(target) > 0 &&
					isIn(f, this.nodeId, target.add(maxValue)))
				return f;

			else if(this.nodeId.compareTo(target) > 0 &&
					isIn(f.add(maxValue), this.nodeId, target.add(maxValue)))
				return f;

		}
		
		return nodeId;
	}
	
	
	
	/********************************************/
	/*				Helper methods				*/
	/********************************************/

	/*
	 * Strict inclusion
	 * 
	 * el in (first, last)
	 * */
	private boolean isIn(BigInteger el, BigInteger first, BigInteger last) {
		
		if(first.compareTo(last) < 0) {
			// first < last
			if(el.compareTo(first) > 0 && el.compareTo(last) < 0)
				// myId in (first, last)
				return true;
		}

		return false;
	}
	
	
	/*
	 * Check interval, deal with the Chord's ring property
	 * (interval open left, closed right)
	 * If el does not initially belong to the interval, try to "unfold" the ring by adding the maxId value and check again 
	 *
	 * el in (first, last] iff
	 * 
	 * el in (first, last] with first < last
	 * or
	 * el in (first, last+maxId] with first > last
	 * or
	 * el+maxId in (first, last+maxId] with first > last
	 * */
	private boolean checkInterval(BigInteger el, BigInteger first, BigInteger last) {
		
		if(isIn(el, first, last) || el.equals(last))
			return true;
		
		else if(first.compareTo(last) > 0 &&
				(isIn(el, first, last.add(maxValue)) || el.equals(last.add(maxValue))))
			return true;
		
		else if(first.compareTo(last) > 0 &&
				(isIn(el.add(maxValue), first, last.add(maxValue)) || el.add(maxValue).equals(last.add(maxValue))))
			return true;
		
		return false;
	}
}
