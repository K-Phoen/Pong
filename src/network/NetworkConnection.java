package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;


public class NetworkConnection {
	private DatagramSocket sock;
	
	private int to_confirm = 0;
	
	/**
	 * Crée une connexion "client"
	 * 
	 * @throws SocketException En cas d'erreur
	 */
	public NetworkConnection() throws SocketException {
		sock = new DatagramSocket();
	}
	
	/**
	 * Crée une connexion "serveur"
	 * 
	 * @param port Port du serveur
	 * 
	 * @throws SocketException En cas d'erreur
	 */
	public NetworkConnection(int port) throws SocketException {
		sock = new DatagramSocket(port);
	}
	
	/**
	 * Reçoit des données en mode bloquant.
	 * 
	 * @return Paquet Les données reçues.
	 * 
	 * @throws IOException Si une erreur survient
	 */
	public Paquet receive() throws IOException {
		return tryReceive(0);
	}
	
	/**
	 * Reçoit des données en mode NON bloquant.
	 * 
	 * @param timeout Le timeout au-delà duquel on stoppe l'attente
	 * 
	 * @return Paquet Les données reçues.
	 * 
	 * @throws IOException Si une erreur survient
	 */
	public Paquet tryReceive(int timeout) throws IOException {
		Paquet paquet = receiveRaw(timeout);

		if(paquet == null)
			return null;
		
		String[] data = paquet.getMessage().split(" ");
			
		// on regarde si le message nécessite une confirmation
		if(data.length > 1) {
			try {
				confirm(paquet.getDatagram(), Integer.parseInt(data[0]));
				
				// reconstruction d'un paquet sans le numéro du paquet
				String new_msg = paquet.getMessage().substring(data[0].length() + 1);
				
				paquet.setMessage(new_msg);
				//return new Paquet(datagram);
			} catch (NumberFormatException e) {
				// si pas un nombre : pas une demande de confirmation
			}
		}

		return paquet;
	}
	
	private Paquet receiveRaw(int timeout) throws IOException {
		try {
			sock.setSoTimeout(timeout);
		} catch (SocketException e) {
			throw new IOException("Impossible de définir le timeout pour la réception");
		}
		
		byte[] buffer = new byte[1024];
		DatagramPacket p = new DatagramPacket(buffer, buffer.length);
		
		try {
			sock.receive(p);
			
			return new Paquet(p);
		} catch (SocketTimeoutException e) {
			// on ignore : null sera retourné
		}
		
		return null;
	}
	
	public void sendAndWaitConfirm(InetAddress addr, int port, String msg) throws IOException {
		sendAndWaitConfirm(addr, port, msg, 0);
	}
	
	public void sendAndWaitConfirm(InetAddress addr, int port, String msg, int timeout) throws IOException {
		to_confirm++;
		
		Paquet reply;
		for(int nb_essais = 3; nb_essais != 0; nb_essais--)
		{
			try {
				send(addr, port, String.format("%d %s", to_confirm, msg));
			} catch (IOException e) {
				continue; // on retente
			}
			
			try {
				reply = receiveRaw(timeout);
			} catch (IOException e) {
				continue;
			}
			
			if(reply != null && reply.getMessage().equals(String.format("%d OK", to_confirm)))
				return;
		}
		
		throw new IOException("La confirmation du message \""+msg+"\" n'est pas arrivée");
	}
	
	/**
	 * Envoie un message sans confirmation.
	 * 
	 * @param addr Adresse de l'hôte à contacter
	 * @param port Port de l'hôte à contacter
	 * @param msg Message à envoyer
	 * 
	 * @throws IOException Si on ne parvient pas ï¿½ envoyer le message 
	 */
	public void send(InetAddress addr, int port, String msg) throws IOException {
		sock.send(new DatagramPacket(msg.getBytes(), msg.length(), addr, port));
	}
	
	/**
	 * Envoie la confirmation pour accuser réception d'un message
	 *  
	 * @param p DatagramPacket dont on confirme la réception
	 * @param msg_no Identifiant du message contenu dans le paquet
	 * 
	 * @throws IOException Si l'envoi échoue
	 */
	private void confirm(DatagramPacket p, int msg_no) throws IOException {
		String msg = String.format("%d OK", msg_no);
		
		sock.send(new DatagramPacket(msg.getBytes(), msg.length(), p.getAddress(), p.getPort()));
	}
	
	/**
	 * On ferme proprement la socket dès que l'objet est détruit
     *
     * @throws Throwable Bouh
     */
	@Override
	public void finalize() throws Throwable
    {
         sock.close();
         super.finalize();
    }
}
