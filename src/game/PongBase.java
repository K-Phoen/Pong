package game;

import java.awt.Color;
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

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import network.NetworkConnection;


public abstract class PongBase extends JFrame implements KeyListener, Runnable, MouseListener, MouseMotionListener {
	/**
	 * ID de sérialisation 
	 */
	private static final long serialVersionUID = -8330079307530116835L;
	
	protected static final String SOUND_CONTACT = "./data/pong.wav";
	
	protected Thread runner;
	protected NetworkConnection sock;
	
	protected Image offscreeni;
	protected Graphics offscreeng;
	protected Rectangle plane;
	protected Point ballPoint, joueur1, joueur2, ballSpeed;
	
	BufferedImage img_ball;
	final int ball_width = 32, ball_height = 32;
	
	BufferedImage img_raquette;
	BufferedImage img_raquette2;
	final int racket_width = 13, racket_height = 75;
	
	protected boolean is_paused = false;
	
	/**
	 * Indique si le jeu est démarré
	 */
	protected boolean is_game_started = false;
	
	/**
	 * Utilisée pour faire clignoter le jeu
	 */
	protected boolean death_mode = false;
	
	/**
	 * Score du joueur 1
	 */
	protected int joueur1_score = 0;
	
	/**
	 * Score du joueur 2
	 */
	protected int joueur2_score = 0;
	
	
	protected static final String MSG_MOVE 			= "move";
	protected static final String MSG_POS 			= "positions";
	protected static final String MSG_BALL 			= "ball";
	protected static final String MSG_SCORE 		= "score";
	protected static final String MSG_CONTACT		= "contact";
	protected static final String MSG_WALL_TOUCHED 	= "wall";
	protected static final String MSG_GAME_OVER 	= "game_over";
	protected static final String MSG_PAUSE		 	= "set_pause";
	
	
	/**
	 * Lance le jeu
	 */
	public void start() {
		// démarrage du thread de gestion du jeu
		startGame();
	}
	
	/**
	 * Initialise la partie graphique.
	 */
	protected void initGUI(String window_title) {
		// caractéristiques de la fenêtre
		setTitle(window_title);
		setVisible(true);
		setBounds(100, 100, 640, 480);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);

		// ajout des listener
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);

		// création du plateau de jeu
		offscreeni = createImage(getWidth(), getHeight());

		offscreeng = offscreeni.getGraphics();
		setBackground(Color.black);
		
		// on place les pavés
		joueur2 = new Point((getWidth() - 35), ((getHeight() / 2) - 25));
		joueur1 = new Point(35, ((getHeight() / 2) - 25));
		
		
		// chargement de l'image de la balle et des raquettes
		try {
			img_ball = ImageIO.read(new File("./data/ball.png"));
			img_raquette = ImageIO.read(new File("./data/raquette.png"));
			img_raquette2 = ImageIO.read(new File("./data/raquette2.png"));
		} catch (IOException e) {
			System.err.println("Impossible de charger l'image : "+e.getMessage());
			System.exit(1);
		}
		
		resetBall();

		// zone de jeu
		plane = new Rectangle(15, 15, (getWidth()), (getHeight() - 30));

		// actualisation de l'affichage
		repaint();
	}
	
	/**
	 * Initialise la position et la vitesse de la balle.
	 */
	protected void resetBall()
	{
		ballPoint = new Point((getWidth() / 2), (getHeight() / 2));
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
		runner.setPriority(Thread.MAX_PRIORITY);
		
		try {
			runner.start();
		} catch (IllegalStateException e) {
			System.err.println("Impossible de créer le thread du jeu : "+e.getMessage());
			System.exit(1);
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent e) { }
	
	protected abstract void onGameOver(String winner);
	
	/**
	 * Analyse un message transmis par le réseau pour
	 * exécuter la méthode qui va bien.
	 * 
	 * @param cmd Message à analyser
	 */
	protected void executeCmd(String cmd)
	{
		String[] args = cmd.split(" ");
		
		/* commandes à un seul argument */
		
		if(args[0].equals(MSG_WALL_TOUCHED)) {
			onWallTouched();
			return;
		} else if (args[0].equals(MSG_CONTACT)) {
			playSound(SOUND_CONTACT);
			return;
		}
		
		
		/* commandes à deux arguments */
		
		if(args.length == 2) {
			if (args[0].equals(MSG_GAME_OVER)) {
				onGameOver(args[1]);
				return;
			} else if (args[0].equals(MSG_PAUSE)) {
				if(args[1].equals("on"))
					onGamePause();
				else
					onGameResume();
				
				return;
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
	 */
	@Override
	public void update(Graphics g) {
		paint(g);
	}

	/**
	 * Dessine l'interface
	 */
	@Override
	public void paint(Graphics g) {
		if (offscreeng == null)
			return;
		
		offscreeng.setColor(new Color(244, 122, 0)); // orange foncé
		offscreeng.fillRect(0, 0, getWidth(), getHeight());
		offscreeng.setColor(!death_mode ? Color.white : Color.red);
		
		displayScores();
		
		drawGroundLines();
		
		if (plane != null) {
			offscreeng.clipRect(plane.x, plane.y, plane.width - 28,
								plane.height + 1);
			offscreeng.drawRect(plane.x, plane.y, plane.width - 30, plane.height);
			
//			offscreeng.fillRect(joueur1.x, joueur1.y, racket_width, racket_height);
//			offscreeng.fillRect(joueur2.x, joueur2.y, racket_width, racket_height);
			
			offscreeng.drawImage(img_raquette2, joueur2.x, joueur2.y, null);
			offscreeng.drawImage(img_raquette, joueur1.x,joueur1.y, null);
			
			offscreeng.drawImage(img_ball, ballPoint.x - ball_width / 2,
								 ballPoint.y - ball_height / 2, null);
		}
		
		g.drawImage(offscreeni, 0, 10, this);
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
		// correction du rayon pour prendre en compte l'épaisseur du trait
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
	protected void blink()
	{
		for (int i = 3; i > 0; i--) {
			death_mode = true;
			repaint();

			try {
				Thread.sleep(300); 
			} catch (InterruptedException e) {}
			
			death_mode = false;
			repaint();
			
			try {
				Thread.sleep(300); 
			} catch (InterruptedException e) {}
		}
	}
	
	/**
	 * Joue un son.
	 * 
	 * @param sound Fichier contenant le son à jouer
	 */
	protected void playSound(String sound) {
		Thread t = new Sound(sound);
		t.setPriority(Thread.MIN_PRIORITY);
		
		t.start();
	}
	
	/**
	 * Affiche l'état des scores
	 */
	private void displayScores() {
		offscreeng.drawString(String.format("Joueur 1 : %d", joueur1_score), (getWidth() / 10), 35);
		offscreeng.drawString(String.format("Joueur 2 : %d", joueur2_score), (4 * getWidth() / 5), 35);
	}
	
	/**
	 * Appelée lorsqu'un mur a été touché.
	 */
	protected void onWallTouched() {
		displayScores();
		
		blink();
	}
	
	/*
	 * Les méthodes suivantes sont requises par l'interface MouseListener
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
	public void keyPressed(KeyEvent e) {
		char c =e.getKeyChar();
		
		if(c == 'p' | c == 'P') {
			if(is_paused)
				onGameResume();
			else
				onGamePause();
		}
	}


	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	protected void onGamePause() {
		is_paused = true;
	}

	protected void onGameResume() {
		is_paused = false;
	}
	
	protected void wait(int delay) {
		try {
			Thread.sleep(delay); 
		} catch (InterruptedException e) {
			// rien
		}
	}
}
