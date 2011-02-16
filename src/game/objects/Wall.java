/*
 *  Wall.java
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
import java.io.IOException;
import java.util.Random;


public class Wall extends GraphicObject {
    private boolean isVisible = false;
    private Dimension zone;
    private int margin;


    public Wall(String img, Dimension zone, int margin) throws IOException {
        super(img);

        this.zone = zone;
        this.margin = margin;
    }

    /**
     * Déplace le mur de manière aléatoire dans la zone définie lors de sa
     * création
     */
    public void move() {
        Random r = new Random();

        x = r.nextInt((int) zone.getWidth()) + margin;
        y = r.nextInt((int) zone.getHeight()) + margin;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void toggleVisibility() {
        isVisible = !isVisible;
    }
}
