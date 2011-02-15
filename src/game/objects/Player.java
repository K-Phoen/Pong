/*
 *  Player.java
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

package game.objects;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;


public class Player {
    private int id;
    private int score = 0;
    private Point pos;
    private BufferedImage img;

    public Player(int id, String img) throws IOException {
        this.id = id;
        pos = new Point(-1, -1);
        this.img = ImageIO.read(new File(img));
    }

    public void setPos(int x, int y) {
        pos.x = x;
        pos.y = y;
    }

    public void setY(int y) {
        pos.y = y;
    }

    /**
     * Retourne le point en haut à gauche de la zone du joueur
     * 
     * @return Coordonnées du point
     */
    public Point getPos() {
        return pos;
    }

    public Rectangle getZone() {
        return new Rectangle(getPos(), new Dimension(img.getWidth(),
                                                     img.getHeight()));
    }

    public void incScore() {
        ++score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public int getId() {
        return id;
    }

    public void drawOn(Graphics g) {
        g.drawImage(img, pos.x, pos.y, null);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Player))
            return false;

        Player p = (Player) obj;

        return p.getId() == getId();
    }

    @Override
    public int hashCode() {
        return 97 * 3 + this.id;
    }

    @Override
    public String toString() {
        return String.format("P%d", id);
    }
}
