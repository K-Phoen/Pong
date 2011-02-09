package game;


public interface Constants {
    /**
	 * Liste des instructions reconnues par notre "protocole"
	 */
	static final String MSG_MOVE 			= "move";
	static final String MSG_BALL 			= "ball";
	static final String MSG_SCORE           = "score";
	static final String MSG_CONTACT         = "contact";
	static final String MSG_WALL_TOUCHED 	= "wall";
    static final String MSG_STATE_CHANGED	= "state";
    static final String MSG_WALL_POS       	= "wall_pos";

	/**
	 * Localisation des ressources sur le disque dur
	 */
	static final String SOUND_CONTACT   = "./data/pong.wav";
    static final String SOUND_START     = "./data/baseball.wav";

	static final String IMG_BALL 		= "./data/ball.png";
    static final String IMG_WALL 		= "./data/mur.png";
	static final String IMG_RACKET_P1	= "./data/raquette.png";
	static final String IMG_RACKET_P2	= "./data/raquette2.png";

    static final int RACKET_WIDTH = 13, RACKET_HEIGHT = 75;
	static final int BALL_WIDTH = 32, BALL_HEIGHT = 32;

    /**
     * Représente un état du jeu
     */
    enum State {
		WAITING, READY, STARTED, PAUSED, FINISHED
	}
}
