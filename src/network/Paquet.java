package network;

import java.net.DatagramPacket;

public class Paquet {
	private DatagramPacket p;
	private String msg;
	
	
	public Paquet(DatagramPacket p) {
		this.p = p;
		this.msg = new String(p.getData(), 0, p.getLength());
	}
	
	public DatagramPacket getDatagram() {
		return p;
	}
	
	public String getMessage() {
		return msg;
	}
	
	public void setMessage(String message) {
		DatagramPacket datagram = new DatagramPacket(message.getBytes(), message.length());
		datagram.setAddress(p.getAddress());
		datagram.setPort(p.getPort());
		
		p = datagram;
		msg = message;
	}
}
