/*
 *  MirrorPong.java
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

package game;

import game.Constants.State;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Random;

import network.Connection;
import network.Paquet;


public class MirrorPong extends PongBase {
	/**
	 * ID de sérialisation
	 */
	private static final long serialVersionUID = 7224334478468671910L;

	private int server_port = 6000;

	/**
	 * Nombre de points à atteindre pour remporter le match
	 */
	private int max_points = 1;


	/**
	 * Programme principal
	 *
	 * @param args arguments du programme (port)
	 */
	public static void main(String[] args) {
		MirrorPong jp = new MirrorPong();
        
		if(args.length == 1) {
			try {
				jp.setPort(Integer.parseInt(args[0]));
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
     *
     * @throws IllegalStateException Si la création du serveur est impossible
     */
	@SuppressWarnings("null")
	@Override
	public void start() throws IllegalStateException {
		// lancement du serveur
		try {
			sock = new Connection(server_port);
		} catch (Exception e) {
			throw new IllegalStateException("Erreur au lancement du serveur : " + e.getLocalizedMessage());
		}

		// création de la GUI
		initGUI("MirrorPong");

        // attente d'un client
        waitClient();

		changeState(State.READY);

		super.start();
	}

    /**
     * Attent qu'un client se connecte pour enregistrer son adresse et son
     * port
     */
    private void waitClient() {
        // attente de la connexion du second joueur
		Paquet p = null;
		String msg = new String();
		while(!msg.equals("HELLO")) {
			try {
				p = sock.receive();
			} catch (IOException e) {
				continue;
			}

			if(p == null)
				continue;

			msg = p.getMessage();
		}

		setDistantHost(p.getDatagram().getAddress());
		setDistantPort(p.getDatagram().getPort());
    }

    /**
     * Fixe le nombre de points à atteindre pour gagner une partie
     *
     * @param max Nombre de points à atteintre pour gagner une partie
     *
     * @throws IllegalArgumentException Si le nombre de points est strictement
     *                                  inférieur à 1
     */
    public void setMaxPoints(int max) throws IllegalArgumentException
    {
        if(max < 1)
            throw new IllegalArgumentException("Nombre de points maximal incorrect : doit être supérieur à 0");

        max_points = max;
    }

    /**
     * Définit le port sur lequel devra fonctionner le serveur
     *
     * @param port Port d'écoute du serveur
     *
     * @throws IllegalArgumentException Si le port est inférieur ou égal à 0
     */
    public void setPort(int port) throws IllegalArgumentException
    {
        if(port <= 0)
            throw new IllegalArgumentException("Port incorrect");

        server_port = port;
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
        Random r = new Random();
		while (currentState() != State.FINISHED) {
            repaint();
            
			try {
				p = sock.tryReceive(5);
			} catch (IOException e) {
				p = null;
			}

            if(p != null && p.getMessage() != null)
				executeCmd(p.getMessage());

			if(currentState() == State.PAUSED)
				continue;

			checkPlayerCollision(joueur1);
			checkPlayerCollision(joueur2);
            checkWallCollision(wall);
			checkWalls();
            
			moveBall();

            // gestion du mur "amovible"
            if(r.nextInt(200) == 34)
            {
                moveWall();
                wall.toggleVisibility();

                // envoi des infos du mur
                sendToDistantPlayer(String.format("%s %d %d %s", Constants.MSG_WALL_POS,
                                                  wall.x, wall.y, wall.isVisible() ? "on" : "off"));
            }


			// envoi de la position de la balle
			sendToDistantPlayer(String.format("%s %d %d", Constants.MSG_BALL,
                                                          ballPoint.x,
                                                          ballPoint.y));

            wait(5);
		}

        repaint();

		// partie terminée
		onGameOver();
	}

    private void moveWall() {
        Random r = new Random();

        wall.x = r.nextInt((int) effects_zone.getWidth()) + effects_zone_padding;
        wall.y = r.nextInt((int) effects_zone.getHeight()) + effects_zone_padding;
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
		sendToDistantPlayer(String.format("%s P1 %d", Constants.MSG_MOVE, joueur1.y));
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
		if (currentState() != State.READY)
			return;

		ballSpeed.x = 4;
		ballSpeed.y = 2;

        // comme ça la balle n'est pas tout le temps lancée du même côté
        if(System.currentTimeMillis() % 2 == 0)
            ballSpeed.x *= -1;

		changeState(State.STARTED); // demarre le jeu
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
	 * et lance les actions associées si elle est avérée.
	 *
	 * @param player Joueur dont on veut tester la collision avec la balle.
	 */
	private void checkPlayerCollision(Point player) {
		if(!checkCollision(player))
			return;

		int racketHit = ballPoint.y - (player.y + 25);

		ballSpeed.y += racketHit / 7;
		ballSpeed.x *= -1;

		sendToDistantPlayer(Constants.MSG_CONTACT);
		Sound.play(Constants.SOUND_CONTACT);
	}

    private void checkWallCollision(Wall wall) {
        Rectangle balle_zone = new Rectangle(ballPoint.x - Constants.BALL_HEIGHT / 2,
											 ballPoint.y - Constants.BALL_WIDTH / 2,
											 Constants.BALL_WIDTH, Constants.BALL_HEIGHT);

        if(!wall.isVisible() || !wall.intersects(balle_zone))
            return;

		int racketHit = ballPoint.y - (wall.y + 25);

		ballSpeed.y += racketHit / 7;
		ballSpeed.x = -ballSpeed.x;

		sendToDistantPlayer(Constants.MSG_CONTACT);
		Sound.play(Constants.SOUND_CONTACT);
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
											  Constants.RACKET_WIDTH,
                                              Constants.RACKET_HEIGHT);
		Rectangle balle_zone = new Rectangle(ballPoint.x - Constants.BALL_WIDTH / 2,
											 ballPoint.y - Constants.BALL_HEIGHT / 2,
											 Constants.BALL_WIDTH, Constants.BALL_HEIGHT);

		return joueur_zone.intersects(balle_zone);
	}

	/**
	 * Vérifie que la balle ne soit pas en collision avec un mur.
	 */
	protected void checkWalls() {
		int ball_left = ballPoint.x - Constants.BALL_WIDTH / 2;
		int ball_top = ballPoint.y - Constants.BALL_WIDTH / 2;
		int ball_right = ballPoint.x + Constants.BALL_WIDTH;
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
	 * On regarde de quel côté la balle touche le mur, ont met les scores à
     * jour et on les envoie au client
	 */
	@Override
	protected void onWallTouched() {
		if (ballSpeed.x >= 0)
			joueur1_score++;
		else
			joueur2_score++;

		// envoi des scores
		sendToDistantPlayer(Constants.MSG_SCORE + " P1 " + joueur1_score);
		sendToDistantPlayer(Constants.MSG_SCORE + " P2 " + joueur2_score);

		// envoi de l'info "mur touché"
		sendToDistantPlayer(Constants.MSG_WALL_TOUCHED);

		super.onWallTouched();

        // ici, soit le jeu est terminé, soit on est en attente de la relance
        changeState((joueur1_score == max_points || joueur2_score == max_points)
                    ? State.FINISHED
                    : State.READY);

		resetBall();
	}

    /**
     * Chaque fois que l'état du jeu change, on prévient le client
     *
     * @param new_sate Nouvel état du jeu
     */
    @Override
    protected void changeState(State new_sate) {
        super.changeState(new_sate);

        sendToDistantPlayer(String.format("%s %s", Constants.MSG_STATE_CHANGED, new_sate));
    }

    /**
     * Appelée dès que la partie est terminée
     */
	protected void onGameOver() {
        String winner = (joueur1_score == max_points) ? "P1" : "P2";
        
		showAlert(winner.equals("P1")
				  ? "Vous avez gagné \\o/" : "Vous avez perdu [-_-]\"");
	}
}