import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHAManager {

	private MessageDigest md;
	
	public SHAManager() {
		
		try {
			md = MessageDigest.getInstance("SHA-1");
			
		} catch (NoSuchAlgorithmException e) {e.printStackTrace();}
	}
	
	public String getSHA(String s) {
		
		md.update(s.getBytes());
		return byteToHex(md.digest());
	}

	private String byteToHex(byte[] b) {

		// One byte can hold 2 Hexadecimal digits (Hexadecimal digits are 16 = 2^4
		// i.e. 4 bits are enough; since 1 byte is 8 bits 1 byte can hold 2 Hex. digits
		char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7',
				 '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

		StringBuffer buf = new StringBuffer();
		for (int j=0; j<b.length; j++) {
			buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
			buf.append(hexDigit[b[j] & 0x0f]);
		}
		
		return buf.toString();
	}

}
