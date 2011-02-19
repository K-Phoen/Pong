/*
 *  Ball.java
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

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;


public class Ball extends GraphicObject {
    private static final long serialVersionUID = 1L;
    private Point speed;

    public Ball(String img) throws IOException {
        super(img);

        speed = new Point(0, 0);
    }

    public Point getSpeed() {
        return speed;
    }

    public void setSpeed(int x, int y) {
        speed.x = x;
        speed.y = y;
    }

    /**
	 * Déplace la balle selon sa vitesse actuelle.
	 */
    public void move() {
        x += speed.x;
		y += speed.y;
    }

    @Override
    public void drawOn(Graphics g) {
        g.drawImage(img, x - (int) getWidth() / 2, y - (int) getHeight() / 2, null);
    }

    @Override
    public Rectangle getZone() {
        return new Rectangle(x - (int) getWidth() / 2, y - (int) getHeight() / 2,
                             (int) getWidth(), (int) getHeight());
    }
}
