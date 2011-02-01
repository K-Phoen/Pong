package game;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.IOException;

import network.NetworkConnection;
import network.Paquet;


public class MirrorPong extends PongBase {
	/**
	 * ID de sérialisation
	 */
	private static final long serialVersionUID = 7224334478468671910L;

	private static int server_port = 6000;
	
	/**
	 * Nombre de points à atteindre pour remporter le match
	 */
	private int max_points = 0;


	/**
	 * Programme principal
	 * 
	 * @param arg
	 */
	public static void main(String[] args) {
		MirrorPong jp = new MirrorPong();
		
		if(args.length == 1) {
			try {
				server_port = Integer.parseInt(args[0]);
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
			sock = new NetworkConnection(server_port);
		} catch (IOException e) {
			showAlert("Erreur au lancement du serveur : " + e);
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
		
		state = State.READY;

		setDistantHost(p.getDatagram().getAddress());
		setDistantPort(p.getDatagram().getPort());
		
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
		while (state != State.FINISHED) {
			wait(8);
			
			if(state == State.PAUSED)
				continue;
			
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

			// envoi de la position de la balle
			sendToDistantPlayer(String.format("%s %d %d", MSG_BALL, ballPoint.x, ballPoint.y));

			repaint();
		}
		
		// partie terminée
		
		String winner = (joueur1_score == max_points) ? "P1" : "P2";
		
		sendToDistantPlayer(String.format("%s %s", MSG_GAME_OVER, winner));
		onGameOver(winner);
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
		
		// envoi de la position du joueur 1
		sendToDistantPlayer(String.format("%s P1 %d", MSG_MOVE, joueur1.y));
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
		if (state != State.READY)
			return;
		
		ballSpeed.x = 4;
		ballSpeed.y = 2;
		
		state = State.STARTED; // demarre le jeu
		sendToDistantPlayer(MSG_GAME_STARTED);
	}

	/**
	 * Déplace la balle selon sa vitesse actuelle.
	 */
	protected void moveBall() {
		ballPoint.x += ballSpeed.x;
		ballPoint.y += ballSpeed.y;
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
		
		// ici, soit le jeu est terminé, soit on est en attente de la relance
		state = (joueur1_score == max_points || joueur2_score == max_points)
				? State.FINISHED
				: State.READY;

		// envoi des scores
		sendToDistantPlayer(MSG_SCORE + " P1 " + joueur1_score);
		sendToDistantPlayer(MSG_SCORE + " P2 " + joueur2_score);

		// envoi de l'info "mur touché"
		sendToDistantPlayer(MSG_WALL_TOUCHED);

		super.onWallTouched();

		resetBall();
	}
	
	@Override
	protected void onGameOver(String winner) {
		showAlert(winner.equals("P1")
				  ? "Vous avez gagné \\o/" : "Vous avez perdu [-_-]\"");
	}
}