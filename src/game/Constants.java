/*
 *  Constants.java
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
