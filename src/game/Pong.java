package game;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.UnknownHostException;

import network.NetworkConnection;
import network.Paquet;


public class Pong extends PongBase {
	private static final long serialVersionUID = 7657998555042629676L;


	/**
	 * Programme principal
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		Pong jp = new Pong();

		String host = "localhost";

		if(args.length == 2) {
			host = args[0];

			try {
				jp.setDistantPort(Integer.parseInt(args[1]));
			} catch(NumberFormatException e) {
				System.err.println("Port incorrect : utilisation du port 6000");
				// on ne modifie pas le port par défaut
			}
		}

		try {
			jp.setDistantHost(host);
		} catch (UnknownHostException e) {
			System.err.println("Impossible de contacter le serveur : " + e);
			System.exit(1);
		}

		jp.start();
	}


	/**
	 * Connexion serveur avant l'initialisation de la partie graphique
	 * et du jeu en lui même.
     *
     * @throws IllegalStateException Si la connexion au serveur est impossible
     */
	@Override
	public void start() throws IllegalStateException {
		// connexion au serveur
		try{
			sock = new NetworkConnection();
		} catch (IOException e) {
			throw new IllegalStateException("Erreur à la connexion : " + e.getMessage());
		}

		initGUI("Pong");

		// un espèce de handshake
		while(true) {
			try {
				sock.sendAndWaitConfirm(getDistantHost(), getDistantPort(), "HELLO", 2000);
				break;
			} catch (IOException e) {
				showAlert("Erreur à l'envoi de la demande de connexion au serveur : " + e.getMessage());
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
		while (currentState() != State.FINISHED) {
			try {
				p = sock.tryReceive(5);
			} catch (IOException e) {
				p = null;
			}

			if(p != null && p.getMessage() != null)
				executeCmd(p.getMessage());

			repaint();

            wait(5);
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

		sendToDistantPlayer(String.format("%s P2 %s", MSG_MOVE, joueur2.y));
	}

    @Override
    protected void onGamePause() {
        super.onGamePause();

        sendToDistantPlayer(String.format("%s %s", MSG_STATE_CHANGED, currentState()));
    }

    @Override
    protected void onGameResume() {
        super.onGameResume();

        sendToDistantPlayer(String.format("%s %s", MSG_STATE_CHANGED, currentState()));
    }

	@Override
	protected void onGameOver(String winner) {
		changeState(State.FINISHED);

		repaint();

		showAlert(winner.equals("P2")
				  ? "Vous avez gagné \\o/" : "Vous avez perdu [-_-]\"");
	}
}