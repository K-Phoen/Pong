package game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import network.NetworkConnection;


public abstract class PongBase extends JFrame implements KeyListener, Runnable, MouseListener, MouseMotionListener {

    /**
     * Représente un état du jeu
     */
    protected enum State {
		WAITING, READY, STARTED, PAUSED, FINISHED
	}

	/**
	 * ID de sérialisation
	 */
	private static final long serialVersionUID = -8330079307530116835L;

	/**
	 * Thread qui sera chargé de la gestion du jeu
	 */
	private Thread runner;

	/**
	 * Connexion au second joueur ou socket serveur
	 */
	protected NetworkConnection sock;

	/**
	 * Adresse de l'hôte distant
	 */
	private InetAddress distant_player_host;

	/**
	 * Port de l'hôte distant
	 */
	private int distant_player_port = 6000;

	/**
	 * Etat actuel du jeu (lancé, en pause, etc.)
	 */
	private State state = State.WAITING;

    protected Wall wall = new Wall(20, 75);
    protected Rectangle effects_zone;

	private Image offscreeni;
	private Graphics offscreeng;
	protected Rectangle plane;
    private BufferedImage img_ball, img_raquette, img_raquette2;
    
	protected Point ballPoint, joueur1, joueur2, ballSpeed;

	final protected int racket_width = 13, racket_height = 75;
	final protected int ball_width = 32, ball_height = 32;
    final protected int effects_zone_padding = 150;

	/**
	 * Utilisée pour faire clignoter le jeu
	 */
	private boolean death_mode = false;

	/**
	 * Scores des deux joueurs
	 */
	protected int joueur1_score = 0, joueur2_score = 0;

	/**
	 * Liste des instructions reconnues par notre "protocole"
	 */
	protected static final String MSG_MOVE 			= "move";
	protected static final String MSG_POS 			= "positions";
	protected static final String MSG_BALL 			= "ball";
	protected static final String MSG_SCORE 		= "score";
	protected static final String MSG_CONTACT		= "contact";
	protected static final String MSG_WALL_TOUCHED 	= "wall";
	protected static final String MSG_GAME_OVER 	= "game_over";
	protected static final String MSG_GAME_STARTED 	= "game_started";
    protected static final String MSG_STATE_CHANGED	= "state";

	/**
	 * Localisation des ressources sur le disque dur
	 */
	protected static final String SOUND_CONTACT = "./data/pong.wav";
    protected static final String SOUND_START   = "./data/baseball.wav";
	protected static final String IMG_BALL 		= "./data/ball.png";
	protected static final String IMG_RACKET_P1	= "./data/raquette.png";
	protected static final String IMG_RACKET_P2	= "./data/raquette2.png";


	/**
	 * Lance le jeu
	 */
	public void start() {
		// démarrage du thread de gestion du jeu
		startGame();
	}

	/**
	 * Initialise la partie graphique.
     *
     * @param window_title Titre de la fenêtre
	 */
	protected void initGUI(String window_title) {
		// caractéristiques de la fenêtre
		setTitle(window_title);
		setVisible(true);
		setBounds(100, 100, 640, 480);
		//setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);

		// ajout des listener
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);

		// création du plateau de jeu
		offscreeni = createImage(getWidth(), getHeight());
		offscreeng = offscreeni.getGraphics();
		setBackground(Color.black);

        effects_zone = new Rectangle(effects_zone_padding, effects_zone_padding,
                                     getWidth() - 2 * effects_zone_padding,
                                     getHeight() - 2 * effects_zone_padding);

		// on place les centres des raquettes
		joueur2 = new Point(getWidth() - 35, getHeight() / 2 - 25);
		joueur1 = new Point(35, getHeight() / 2 - 25);


		// chargement de l'image de la balle et des raquettes
		try {
			img_ball = ImageIO.read(new File(IMG_BALL));
			img_raquette = ImageIO.read(new File(IMG_RACKET_P1));
			img_raquette2 = ImageIO.read(new File(IMG_RACKET_P2));
		} catch (IOException e) {
			showAlert("Impossible de charger une ressource : "+e.getMessage());
			System.exit(1);
		}

		resetBall();

		// zone de jeu
		plane = new Rectangle(15, 15, (getWidth()), (getHeight() - 30));

		// actualisation de l'affichage
		repaint();

        // chargement des sons
        loadSounds();
	}

    /**
     * Pré-charge les sons de manières à ce qu'ils puissent être joués
     * immédiatement lors de l'appel à Sound.play()
     */
    private void loadSounds()
    {
        try {
            Sound.load(SOUND_CONTACT);
            Sound.load(SOUND_START);
        } catch (Exception ex) {
            showAlert("Impossible de charger les sons : "+ex.getLocalizedMessage());
        }
    }

	public void setDistantHost(String host) throws UnknownHostException {
		distant_player_host = InetAddress.getByName(host);
	}

	public void setDistantHost(InetAddress host) {
		distant_player_host = host;
	}

	public void setDistantPort(int port) {
		distant_player_port = port;
	}

	public InetAddress getDistantHost() {
		return distant_player_host;
	}

	public int getDistantPort() {
		return distant_player_port;
	}

	/**
	 * Position la balle au centre du terrain, avec une vitesse nulle.
	 */
	protected void resetBall() {
		ballPoint = new Point(getWidth() / 2, getHeight() / 2);
		ballSpeed = new Point(0, 0);
	}

	/**
	 * Crée un thread avec la classe courante.
	 * Ce thread sera chargé de mettre à jour l'affichage en fonction
	 * des échanges entre le client et le serveur.
	 */
	protected void startGame() {
		if (runner != null)
			return;

		runner = new Thread(this);

		try {
			runner.start();
		} catch (IllegalStateException e) {
			showAlert("Impossible de créer le thread du jeu : "+e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Envoie un message au joueur distant
	 *
	 * @param msg Le message à envoyer
	 */
	protected void sendToDistantPlayer(String msg) {
		if(distant_player_host == null)
			return;

		try {
			sock.send(distant_player_host, distant_player_port, msg);
		} catch (IOException e) {
			showAlert("Erreur à l'envoi de données vers le client : "+ e);
		}
	}


    /**
	 * Sera appelée lors du début du mode pause
	 */
	protected void onGamePause() {
		if(state != State.STARTED)
			return;

		changeState(State.PAUSED);

		repaint();
	}

    /**
	 * Sera appelée lors de la sortie du mode pause
	 */
	protected void onGameResume() {
		if(state != State.PAUSED)
			return;

		changeState(State.STARTED);

		repaint();
	}

    /**
     * Change l'état actuel du jeu
     *
     * @param new_state Nouvel état
     */
    protected void changeState(State new_state) {
        state = new_state;

        System.out.println(state);

        // lancement du jeu
        if(new_state == State.STARTED && joueur1_score == 0 && joueur2_score == 0)
            Sound.play(SOUND_START);
    }

    /**
     * Retourne l'état actuel du jeu
     *
     * @return L'état courant du jeu
     */
    protected State currentState() {
        return state;
    }

	/**
	 * Servira à mettre le jeu en pause lors de l'appui sur les touches P ou p
     *
     * @param e Event lié au clavier
     */
	@Override
	public void keyPressed(KeyEvent e) {
		char c = e.getKeyChar();

		if(c == 'p' || c == 'P') {
			if(state == State.PAUSED)
				onGameResume();
			else
				onGamePause();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) { }

	/**
	 * Sera appelée lors de la fin d'une partie
     *
     * @param winner Identifiant du vainqueur (P1 ou P2)
     */
	protected abstract void onGameOver(String winner);

	/**
	 * Effectue un sleep
	 *
	 * @param delay Nombre de millisecondes à attendre
	 */
	protected void wait(int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			// rien
		}
	}

	/**
	 * Analyse un message transmis par le réseau pour
	 * exécuter la méthode qui va bien.
	 *
	 * @param cmd Message à analyser
	 */
	protected void executeCmd(String cmd) {
		String[] args = cmd.split(" ");

		/* commandes à un seul argument */

		if(args[0].equals(MSG_WALL_TOUCHED)) {
			onWallTouched();
			return;
		} else if (args[0].equals(MSG_CONTACT)) {
			Sound.play(SOUND_CONTACT);
			return;
		}


		/* commandes à deux arguments */

		if(args.length == 2) {
			if (args[0].equals(MSG_GAME_OVER)) {
				onGameOver(args[1]);
				return;
            } else if (args[0].equals(MSG_STATE_CHANGED)) {
                changeState(State.valueOf(args[1]));
			}
		}

		if(args.length != 3)
			return;

		/* commandes à trois arguments */

		if(args[0].equals(MSG_MOVE)) { // changement de la position des joueurs
			if(args[1].equals("P1"))
				joueur1.y = Integer.parseInt(args[2]); // changement de la position du joueur 1
			else
				joueur2.y = Integer.parseInt(args[2]); // changement de la position du joueur 2
		}
		else if(args[0].equals(MSG_BALL)) { // changement de la position de la balle
			ballPoint.x = Integer.parseInt(args[1]);
			ballPoint.y = Integer.parseInt(args[2]);
		}
		else if(args[0].equals(MSG_SCORE)) { // mise à jour des scores
			if(args[1].equals("P1"))
				joueur1_score = Integer.parseInt(args[2]);
			else
				joueur2_score = Integer.parseInt(args[2]);
		}
	}

	/**
	 * Demande de re-dessiner l'interface
     *
     * @param g Element dans lequel on dessine
	 */
	@Override
	public void update(Graphics g) {
		paint(g);
	}

	/**
	 * Dessine l'interface
     *
     * @param g Element dans lequel on dessine
     */
	@Override
	public void paint(Graphics g) {
		if (offscreeng == null)
			return;

		offscreeng.setColor(new Color(244, 122, 0)); // orange foncé
		offscreeng.fillRect(0, 0, getWidth(), getHeight());
		offscreeng.setColor(!death_mode ? Color.white : Color.red);

		displayScores();

		if (plane != null) {
			offscreeng.clipRect(plane.x, plane.y, plane.width - 28,
								plane.height + 1);
			offscreeng.drawRect(plane.x, plane.y, plane.width - 30, plane.height);

			// affichage des raquettes
			offscreeng.drawImage(img_raquette2, joueur2.x, joueur2.y, null);
			offscreeng.drawImage(img_raquette, joueur1.x,joueur1.y, null);

			// affichage d'un message si besoin
			if(!drawStateMessage()) {
				drawGroundLines();

				// affichage de la balle
				offscreeng.drawImage(img_ball, ballPoint.x - ball_width / 2,
									 ballPoint.y - ball_height / 2, null);
			}

            // affichage du mur
            if(wall.isVisible()) {
                offscreeng.setColor(Color.BLACK);
                offscreeng.fillRect(wall.x, wall.y, (int) wall.getWidth(),
                                    (int) wall.getHeight());
            }
		}

		g.drawImage(offscreeni, 0, 10, this);
	}

	/**
	 * Affiche le message correspondant à l'état du jeu (s'il y en
	 * a un).
	 *
	 * @return true si un message a été affiché, false sinon
	 */
	private boolean drawStateMessage() {
		switch (state) {
			case WAITING:
				offscreeng.setFont(new Font("Dialog", Font.BOLD, 40));
				offscreeng.drawString("En attente ...",
									  getWidth() / 2 - 90, getHeight() / 2);
				break;
			case READY:
				offscreeng.setFont(new Font("Dialog", Font.BOLD, 40));
				offscreeng.drawString("Prêt ?",
									  getWidth() / 2 - 40, getHeight() / 2);
				break;
			case PAUSED:
                System.out.println("Paused");
				offscreeng.setFont(new Font("Dialog", Font.BOLD, 40));
				offscreeng.drawString("Pause", getWidth() / 2 - 50, getHeight() / 2 );
				break;
			case FINISHED:
				offscreeng.setFont(new Font("Dialog", Font.BOLD, 40));
				offscreeng.drawString("Game Over !", getWidth() / 2 - 110,
									  getHeight() / 2);
				break;
			default:
				return false;
		}

		return true;
	}

	/**
	 * Dessine les lignes du terrain
	 */
	private void drawGroundLines() {
		int circle_radius = 75;
		int circle_origin_y = getHeight() / 2;
		int circle_origin_x = getWidth() / 2;
		int thickness = 4;

		drawCircle(offscreeng, circle_origin_x, circle_origin_y, circle_radius, thickness);

		//creation de la ligne de fond verticale ( drawLine(x1,y1,x2,y2) ) du point (x1,y1) au point (x2,y2)
		// on en fait plusieurs pour gérer l'épaisseur du trait
		offscreeng.drawLine(getWidth()/2,getHeight(),getWidth()/2, -getHeight());
		offscreeng.drawLine(getWidth()/2+1,getHeight(),getWidth()/2+1, -getHeight());
		offscreeng.drawLine(getWidth()/2-1,getHeight(),getWidth()/2-1, -getHeight());
	}

	/**
	 * 	Calls the drawOval method of java.awt.Graphics
	 *  with a square bounding box centered at specified
	 *  location with width/height of 2r.
	 *
	 * @param g The Graphics object.
	 * @param x The x-coordinate of the center of the
	 *          circle.
	 * @param y The y-coordinate of the center of the
	 *          circle.
	 * @param r The radius of the circle.
	 */
	private static void drawCircle(Graphics g, int x, int y, int r) {
		g.drawOval(x-r, y-r, 2*r, 2*r);
	}

	/**
	 * 	Draws a circle of radius r at location (x,y) with
	 *  the specified line width. Note that the radius r
	 *  is to the <B>center</B> of the doughnut drawn.
	 *  The outside radius will be r+lineWidth/2 (rounded
	 *  down). Inside radius will be r-lineWidth/2
	 *  (rounded down).
	 *
	 * @param g The Graphics object.
	 * @param x The x-coordinate of the center of the
	 *          circle.
	 * @param y The y-coordinate of the center of the
	 *          circle.
	 * @param r The radius of the circle.
	 * @param thickness Pen thickness of circle drawn.
	 */
	private static void drawCircle(Graphics g, int x, int y, int r, int thickness) {
		// correction du rayon pour prendre en compte l'�paisseur du trait
		r += thickness / 2;

		for(int i=0; i < thickness; i++) {
			drawCircle(g, x, y, r);

			if (i+1 < thickness) {
				drawCircle(g, x+1, y, r-1);
				drawCircle(g, x-1, y, r-1);
				drawCircle(g, x, y+1, r-1);
				drawCircle(g, x, y-1, r-1);

				r--;
			}
		}
	}

	/**
	 * Fait clignoter l'interface (par exemple lorsqu'un point
	 * a été marqué).
	 */
	protected void blink() {
		for (int i = 3; i > 0; i--) {
			death_mode = true;
			repaint();

			wait(300);

			death_mode = false;
			repaint();

			wait(300);
		}
	}

	/**
	 * Affiche l'état des scores
	 */
	private void displayScores() {
		offscreeng.setFont(new Font("Dialog", Font.BOLD, 14));

		offscreeng.drawString(String.format("Joueur 1 : %d", joueur1_score),
							  getWidth() / 10, 35);
		offscreeng.drawString(String.format("Joueur 2 : %d", joueur2_score),
							  4 * getWidth() / 5, 35);
	}

	/**
	 * Appelée lorsqu'un mur a été touché.
	 */
	protected void onWallTouched() {
		displayScores();

		blink();
	}

	/**
	 * Affiche une boite de dialogue contenant le message
	 * passé en paramètre. Seul le bouton "OK" est proposé
	 * par la fenêtre affichée.
	 *
	 * @param msg Message à afficher dans la fenêtre.
	 */
	protected void showAlert(String msg) {
		JOptionPane.showMessageDialog(this, msg);
	}

	/*
	 * Les méthodes suivantes sont requises par des interfaces
	 * mais ne nous sont pas utiles ...
	 */
	@Override
	public void mouseEntered(MouseEvent e) { }

	@Override
	public void mouseExited(MouseEvent e) {	}

	@Override
	public void mousePressed(MouseEvent e) { }

	@Override
	public void mouseReleased(MouseEvent e) { }

	@Override
	public void mouseDragged(MouseEvent e) { }

	@Override
	public void keyReleased(KeyEvent e) { }

	@Override
	public void keyTyped(KeyEvent e) { }
}
