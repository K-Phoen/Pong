package game;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetAddress;

import network.NetworkConnection;
import network.Paquet;


public class MirrorPong extends PongBase {
	/**
	 * ID de sérialisation
	 */
	private static final long serialVersionUID = 7224334478468671910L;

	private NetworkConnection sock;
	private InetAddress distant_player_host;
	private int distant_player_port;
	private static int port = 6000;


	/**
	 * Programme principal
	 * 
	 * @param arg
	 */
	public static void main(String[] args) {
		MirrorPong jp = new MirrorPong();
		
		if(args.length == 1) {
			try {
				port = Integer.parseInt(args[0]);
			} catch(NumberFormatException e) {
				System.err.println("Port incorrect : utilisation du port 6000");
				// on ne modifie pas le port par défaut
			}
		}
		
		jp.start();
	}


	/**
	 * Création du serveur avant l'initialisation de la partie graphique
	 * et du jeu en lui même.
	 */
	@SuppressWarnings("null")
	@Override
	public void start() {
		// lancement du serveur
		try {
			sock = new NetworkConnection(port);
		} catch (IOException e) {
			System.err.println("Erreur au lancement du serveur : " + e);
			System.exit(1);
		}

		// création de la GUI
		initGUI("MirrorPong");

		// attente de la connexion du second joueur
		Paquet p = null;
		String msg = new String();
		while(!msg.equals("HELLO"))
		{
			try {
				p = sock.receive();
			} catch (IOException e) {
				continue;
			}
			
			if(p == null)
				continue;
			
			msg = p.getMessage();
		}

		distant_player_host = p.getDatagram().getAddress();
		distant_player_port = p.getDatagram().getPort();
		
		super.start();
	}

	/**
	 * On met à jour les mouvements des joueurs dans l'affichage après avoir
	 * effectué quelques vérifications sur l'état du jeu et déplacé la balle.
	 * 
	 * @note Sera appelée par le thread.
	 */
	@Override
	public void run() {

		Paquet p;
		while (true) { // en attendant d'avoir mieux
			try {
				p = sock.tryReceive(5);
			} catch (IOException e) {
				p = null;
			}
			
			if(p != null && p.getMessage() != null)
				executeCmd(p.getMessage());

			checkPlayerCollision(joueur1);
			checkPlayerCollision(joueur2);
			checkWalls();

			moveBall();

			sendPositions();

			repaint();

			try {
				Thread.sleep(10); 
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
		joueur1.y = e.getY() - 25;
	}
	
	/**
	 * (Re)démarre le jeu s'il est arrêté (pas commencé ou point marqué)
	 * 
	 * @param e Event lié à la souris
	 * 
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseClicked(MouseEvent e) {
		if (is_game_started)
			return;
		
		ballSpeed.x = 4;
		ballSpeed.y = 2;
		is_game_started = true; // demarre le jeu
	}

	/**
	 * Déplace la balle selon sa vitesse actuelle.
	 */
	protected void moveBall() {
		ballPoint.x = ballPoint.x + ballSpeed.x;
		ballPoint.y = ballPoint.y + ballSpeed.y;
	}

	/**
	 * Teste la collision entre la balle et un joueur,
	 * et lance les actions associées si elle est avérée
	 * 
	 * @param player Joueur dont on veut tester la collision avec la balle.
	 */
	private void checkPlayerCollision(Point player) {
		if(!checkCollision(player))
			return;
		
		int racketHit = ballPoint.y - (player.y + 25);
		
		ballSpeed.y += racketHit / 7;
		ballSpeed.x = -ballSpeed.x;
		
		sendToDistantPlayer(MSG_CONTACT);
		playSound(SOUND_CONTACT);
	}
	
	/**
	 * Teste la collision entre la balle et la raquette d'un joueur
	 * 
	 * @param joueur Position de la raquette du joueur
	 * 
	 * @return True s'il y a collision, false sinon
	 */
	private boolean checkCollision(Point joueur) {
		Rectangle joueur_zone = new Rectangle(joueur.x, joueur.y, 
											  racket_width, racket_height);
		Rectangle balle_zone = new Rectangle(ballPoint.x - ball_width / 2,
											 ballPoint.y - ball_height / 2,
											 ball_width, ball_height);
		
		return joueur_zone.intersects(balle_zone);
	}
	
	/**
	 * Vérifie que la balle ne soit pas en collision avec un mur.
	 */
	protected void checkWalls() {
		int ball_left = ballPoint.x - ball_width / 2;
		int ball_top = ballPoint.y - ball_height / 2;
		int ball_right = ballPoint.x + ball_width;
		int ball_bottom = ballPoint.y;

		// gauche ou droit
		if (ball_left <= plane.x || ball_right >= plane.width) {
			onWallTouched();
			return;
		}
		
		// haut ou bas : la balle rebondit
		if (ball_top <= plane.y || ball_bottom >= plane.height)
			ballSpeed.y = -ballSpeed.y;
	}

	/**
	 * La balle a heurté un mur (derrière un des deux pavés).
	 * On regarde de quel côté la balle touche le mur, ont met
	 * les scores à jour et on les envoie au client
	 */
	@Override
	protected void onWallTouched() {
		if (ballSpeed.x >= 0)
			joueur1_score++;
		else
			joueur2_score++;

		sendToDistantPlayer(MSG_SCORE + " P1 " + joueur1_score);
		sendToDistantPlayer(MSG_SCORE + " P2 " + joueur2_score);

		sendToDistantPlayer(MSG_WALL_TOUCHED);

		super.onWallTouched();

		resetBall();

		is_game_started = false;
	}
	
	private void sendPositions() {
		sendToDistantPlayer(String.format("%s %d %d", MSG_BALL, ballPoint.x, ballPoint.y));
		sendToDistantPlayer(String.format("%s P1 %d", MSG_MOVE, joueur1.y));
	}

	/**
	 * Envoie un message au joueur distant
	 * 
	 * @param msg Le message à envoyer
	 */
	private void sendToDistantPlayer(String msg) {
		if(distant_player_host == null)
			return;
		
		try {
			sock.send(distant_player_host, distant_player_port, msg);
		} catch (IOException e) {
			System.err.println("Erreur à l'envoi de données vers le client : "+ e);
			System.err.println("Message à envoyer : "+ msg);
		}
	}
}