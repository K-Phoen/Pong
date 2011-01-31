package game;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;


public abstract class PongBase extends JFrame implements Runnable, MouseListener, MouseMotionListener {
	/**
	 * ID de sï¿½rialisation 
	 */
	private static final long serialVersionUID = -8330079307530116835L;
	
	protected static final String SOUND_CONTACT = "./data/pong.wav";
	
	protected Thread runner;
	
	protected Image offscreeni;
	protected Graphics offscreeng;
	protected Rectangle plane;
	protected Point ballPoint, joueur1, joueur2, ballSpeed;
	final int racket_width = 6, racket_height = 50;
	
	BufferedImage img_ball;
	final int ball_width = 32, ball_height = 32;
	
	/**
	 * Indique si le jeu est dï¿½marrï¿½
	 */
	protected boolean is_game_started = false;
	
	/**
	 * Utilisï¿½e pour faire clignoter le jeu
	 */
	protected boolean death_mode = false;
	
	/**
	 * Score du joueur 1
	 */
	protected int joueur1_score = 0;
	
	/**
	 * Score du joueur 1
	 */
	protected int joueur2_score = 0;
	
	
	protected static final String MSG_MOVE 			= "move";
	protected static final String MSG_POS 			= "positions";
	protected static final String MSG_BALL 			= "ball";
	protected static final String MSG_SCORE 		= "score";
	protected static final String MSG_CONTACT		= "contact";
	protected static final String MSG_WALL_TOUCHED 	= "wall";
	
	
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
		// caractï¿½ristiques de la fenêtre
		setTitle(window_title);
		setVisible(true);
		setBounds(100, 100, 640, 480);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);

		// ajout des listener
		addMouseListener(this);
		addMouseMotionListener(this);

		// crï¿½ation du plateau de jeu
		offscreeni = createImage(getWidth(), getHeight());

		offscreeng = offscreeni.getGraphics();
		setBackground(Color.black);
		
		// on place les pavés
		joueur2 = new Point((getWidth() - 35), ((getHeight() / 2) - 25));
		joueur1 = new Point(35, ((getHeight() / 2) - 25));
		
		
		// chargement de l'image de la balle
		try {
			img_ball = ImageIO.read(new File("./data/ball.png"));
		} catch (IOException e) {
			System.err.println("Impossible de charger l'image de la balle : "+e.getMessage());
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
	 * Crï¿½e un thread avec la classe courante.
	 * Ce thread sera chargï¿½ de mettre ï¿½ jour l'affichage en fonction
	 * des ï¿½changes entre le client et le serveur.
	 */
	protected void startGame() {
		if (runner != null)
			return;
		
		runner = new Thread(this);
		runner.setPriority(Thread.MAX_PRIORITY);
		
		try {
			runner.start();
		} catch (IllegalStateException e) {
			System.err.println("Impossible de crï¿½er le thread du jeu : "+e.getMessage());
			System.exit(1);
		}
	}
	
	@Override
	public void mouseClicked(MouseEvent e) { }
	
	/**
	 * Analyse un message transmis par le rï¿½seau pour
	 * exï¿½cuter la mï¿½thode qui va bien.
	 * 
	 * @param cmd Message ï¿½ analyser
	 */
	protected void executeCmd(String cmd)
	{
		String[] args = cmd.split(" ");
		
		if(args[0].equals(MSG_WALL_TOUCHED)) {
			onWallTouched();
			return;
		} else if (args[0].equals(MSG_CONTACT)) {
			playSound(SOUND_CONTACT);
			return;
		}
			
		if(args.length != 3)
			return;
		
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
		else if(args[0].equals(MSG_SCORE)) { // mise ï¿½ jour des scores
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
		
		offscreeng.setColor(Color.black);
		offscreeng.fillRect(0, 0, getWidth(), getHeight());
		offscreeng.setColor(!death_mode ? Color.white : Color.red);
		
		displayScores();
		
		if (plane != null) {
			offscreeng.clipRect(plane.x, plane.y, plane.width - 28,
								plane.height + 1);
			offscreeng.drawRect(plane.x, plane.y, plane.width - 30, plane.height);
			
			offscreeng.fillRect(joueur1.x, joueur1.y, racket_width, racket_height);
			offscreeng.fillRect(joueur2.x, joueur2.y, racket_width, racket_height);
			
			offscreeng.drawImage(img_ball, ballPoint.x - ball_width / 2,
								 ballPoint.y - ball_height / 2, null);
		}
		
		g.drawImage(offscreeni, 0, 10, this);
	}
	
	/**
	 * Fait clignoter l'interface (par exemple lorsqu'un point
	 * a ï¿½tï¿½ marquï¿½).
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
	 * @param sound Fichier contenant le son ï¿½ jouer
	 */
	protected void playSound(String sound) {
		Thread t = new Sound(sound);
		t.setPriority(Thread.MIN_PRIORITY);
		
		t.start();
	}
	
	/**
	 * Affiche l'ï¿½tat des scores
	 */
	private void displayScores() {
		offscreeng.drawString(String.format("Joueur 1 : %d", joueur1_score), (getWidth() / 10), 35);
		offscreeng.drawString(String.format("Joueur 2 : %d", joueur2_score), (4 * getWidth() / 5), 35);
	}
	
	/**
	 * Appelï¿½e lorsqu'un mur a ï¿½tï¿½ touchï¿½.
	 */
	protected void onWallTouched() {
		displayScores();
		
		blink();
	}
	
	/*
	 * Les mï¿½thodes suivantes sont requises par l'interface MouseListener
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
}
