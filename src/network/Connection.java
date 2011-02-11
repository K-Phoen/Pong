/*
 *  NetworkConnection.java
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;


public class Connection {
	private DatagramSocket sock;

	private int toConfirm = 0;

    private static final int BUFFER_SIZE    = 1024;
    private static final int NB_TRIES       = 3;


	/**
	 * Crée une connexion "client"
	 *
	 * @throws SocketException En cas d'erreur
	 */
	public Connection() throws SocketException {
		sock = new DatagramSocket();
	}

	/**
	 * Crée une connexion "serveur"
	 *
	 * @param port Port du serveur
	 *
	 * @throws SocketException En cas d'erreur
	 */
	public Connection(int port) throws SocketException {
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
				String newMsg = paquet.getMessage().substring(data[0].length() + 1);

				paquet.setMessage(newMsg);
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

		byte[] buffer = new byte[BUFFER_SIZE];
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
		toConfirm++;

		Paquet reply;
		for(int nb_essais = NB_TRIES; nb_essais != 0; nb_essais--) {
			try {
				send(addr, port, String.format("%d %s", toConfirm, msg));
			} catch (IOException e) {
				continue; // on retente
			}

			try {
				reply = receiveRaw(timeout);
			} catch (IOException e) {
				continue;
			}

			if(reply != null && reply.getMessage().equals(String.format("%d OK", toConfirm)))
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
	 * @throws IOException Si on ne parvient pas à envoyer le message
	 */
	public void send(InetAddress addr, int port, String msg) throws IOException {
		sock.send(new DatagramPacket(msg.getBytes(), msg.length(), addr, port));
	}

	/**
	 * Envoie la confirmation pour accuser réception d'un message
	 *
	 * @param p DatagramPacket dont on confirme la réception
	 * @param msgNo Identifiant du message contenu dans le paquet
	 *
	 * @throws IOException Si l'envoi échoue
	 */
	private void confirm(DatagramPacket p, int msgNo) throws IOException {
		String msg = String.format("%d OK", msgNo);

		sock.send(new DatagramPacket(msg.getBytes(), msg.length(), p.getAddress(), p.getPort()));
	}

	/**
	 * On ferme proprement la socket dès que l'objet est détruit
     *
     * @throws Throwable Bouh.
     */
	@Override
	public void finalize() throws Throwable {
         sock.close();
         super.finalize();
    }
}
