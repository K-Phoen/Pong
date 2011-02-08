/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package game;

import java.awt.Rectangle;


public class Wall extends Rectangle {
    private static final long serialVersionUID = 1L;
    private boolean is_visible = false;


    public Wall(int w, int h) {
        setSize(w, h);
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
}
