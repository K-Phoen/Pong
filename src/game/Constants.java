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
	String MSG_MOVE 			= "move";
	String MSG_BALL 			= "ball";
	String MSG_SCORE            = "score";
	String MSG_CONTACT          = "contact";
	String MSG_WALL_TOUCHED 	= "wall";
    String MSG_STATE_CHANGED	= "state";
    String MSG_WALL_POS       	= "wall_pos";

	/**
	 * Localisation des ressources sur le disque dur
	 */
	String SOUND_CONTACT   = "./data/pong.wav";
    String SOUND_START     = "./data/baseball.wav";

	String IMG_BALL 		= "./data/ball.png";
    String IMG_WALL 		= "./data/mur.png";
	String IMG_RACKET_P1	= "./data/raquette.png";
	String IMG_RACKET_P2	= "./data/raquette2.png";
    
    int EFFECTS_ZONE_MARGIN = 150;

    /**
     * Représente un état du jeu
     */
    enum State {
		WAITING, READY, STARTED, PAUSED, FINISHED
	}
}
