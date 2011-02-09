/*
 *  Paquet.java
 *
 *  Copyright 2011 Kévin Gomez Pinto <contact@kevingomez.fr>
 *                 Jonathan Da Silva <Jonathan.Da_Silva1@etudiant.u-clermont1.fr>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301, USA.
 */

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
