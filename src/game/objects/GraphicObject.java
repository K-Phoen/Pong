/*
 *  GraphicObject.java
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


public abstract class GraphicObject extends Rectangle {
    private static final long serialVersionUID = 1L;
    
    protected BufferedImage img;


    public GraphicObject(String img) throws IOException {
        this.img = ImageIO.read(new File(img));

        setSize(this.img.getWidth(), this.img.getHeight());
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void drawOn(Graphics g) {
        g.drawImage(img, x, y, null);
    }

    public Rectangle getZone() {
        return new Rectangle(x, y, (int) getWidth(), (int) getHeight());
    }
}
