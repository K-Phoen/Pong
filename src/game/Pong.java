package game;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import network.NetworkConnection;
import network.Paquet;


public class Pong extends PongBase {
	private static final long serialVersionUID = 7657998555042629676L;
	
	private NetworkConnection sock;
	private static String host = "localhost";
	private InetAddress host_address = null;
	private static int port = 6000;
	
	
	/**
	 * Programme principal
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Pong jp = new Pong();
		
		if(args.length == 2) {
			try {
				port = Integer.parseInt(args[0]);
			} catch(NumberFormatException e) {
				System.err.println("Port incorrect : utilisation du port 6000");
				// on ne modifie pas le port par défaut
			}
			
			host = args[1];
		}
		
		jp.start();
	}
	
	
	/**
	 * Connexion serveur avant l'initialisation de la partie graphique
	 * et du jeu en lui même.
	 */
	@Override
	public void start() {
		try {
			host_address = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			System.err.println("Impossible de contacter le serveur : " + e);
			System.exit(1);
		}
		
		// connexion au serveur
		try{
			sock = new NetworkConnection();
		} catch (IOException e) {
			System.err.println("Erreur à la connexion : " + e.getMessage());
			System.exit(1);
		}
		
		initGUI("Pong");
		
		// un espèce de handshake
		while(true) {
			try {
				sock.sendAndWaitConfirm(host_address, port, "HELLO", 1000);
				break;
			} catch (IOException e) {
				System.err.println("Erreur à l'envoi de la demande de connexion au serveur : " + e.getMessage());
			}
		}
		
		super.start();
	}
	
	/**
	 * On met à jour le jeu selon les infos transmises par le serveur
	 * 
	 * @note Sera appelée par le thread.
	 */
	@Override
	public void run() {
		
		Paquet p;
		while (true) {
			try {
				p = sock.tryReceive(5);
			} catch (IOException e) {
				p = null;
			}
			
			if(p != null && p.getMessage() != null)
				executeCmd(p.getMessage());

			repaint();

			try {
				Thread.sleep(5); //pause pour ralentir le jeu
			} catch (InterruptedException e) {
				// rien
			}
		}
	}
	
	/**
	 * Met à jour la position du pavé du joueur 2 par rapport
	 * aux mouvements de la souris
	 * 
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 * 
	 * @param e Event lié à la souris
	 */
	@Override
	public void mouseMoved(MouseEvent e) {
		joueur2.y = e.getY() - 25;
		
		try {
			sock.send(host_address, port, MSG_MOVE + " P2 " + joueur2.y);
		} catch (IOException ex) {
			System.err.println("Erreur à l'envoi des coordonnées du pavé vers le serveur : "+ ex);
		}
	}
}