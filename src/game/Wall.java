/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package game;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;


public class Wall extends Rectangle {
    private static final long serialVersionUID = 1L;
    private boolean is_visible = false;
    private BufferedImage img;


    public Wall(int w, int h) throws IOException {
        setSize(w, h);

        img = ImageIO.read(new File(Constants.IMG_WALL));
    }

    public void setVisible(boolean visible) {
        is_visible = visible;
    }

    public boolean isVisible() {
        return is_visible;
    }

    public void toggleVisibility() {
        is_visible = !is_visible;
    }

    void drawOn(Graphics g) {
        g.drawImage(img, x, y, null);
    }
}
