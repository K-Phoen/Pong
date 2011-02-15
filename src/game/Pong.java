/*
 *  Pong.java
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

import game.objects.Player;
import game.Constants.State;
import java.io.IOException;
import java.net.UnknownHostException;

import network.Connection;
import network.Paquet;


public final class Pong extends PongBase {
    private static final long serialVersionUID = 7657998555042629676L;


    /**
     * Programme principal
     *
     * @param args
     */
    public static void main(String[] args) {
        Pong jp = new Pong();

        String host = "localhost";

        if(args.length == 2) {
            host = args[0];

            try {
                jp.setDistantPort(Integer.parseInt(args[1]));
            } catch(NumberFormatException e) {
                System.err.println("Port incorrect : utilisation du port 6000");
                // on ne modifie pas le port par défaut
            }
        }

        try {
            jp.setDistantHost(host);
        } catch (UnknownHostException e) {
            System.err.println("Impossible de contacter le serveur : " + e);
            System.exit(1);
        }

        jp.start();
    }


    /**
     * Connexion serveur avant l'initialisation de la partie graphique
     * et du jeu en lui même.
     *
     * @throws IllegalStateException Si la connexion au serveur est impossible
     */
    @Override
    public void start() {
        // connexion au serveur
        try{
            sock = new Connection();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur à la connexion : " + e.getMessage());
        }

        initGUI("Pong");

        waitServer();

        super.start();
    }

    /**
     * On met à jour le jeu selon les infos transmises par le serveur
     *
     * @note Sera appelée par le thread.
     */
    @Override
    public void run() {
        Paquet p;
        while (currentState() != State.FINISHED) {
            try {
                p = sock.tryReceive(5);
            } catch (IOException e) {
                p = null;
            }

            if(p != null && p.getMessage() != null)
                executeCmd(p.getMessage());

            repaint();

            //wait(5);
        }

        repaint();

        onGameOver();
    }

    @Override
    protected Player getMyPlayer() {
        return player2;
    }

    @Override
    protected void onGamePause() {
        super.onGamePause();

        sendToDistantPlayer(String.format("%s %s", Constants.MSG_STATE_CHANGED,
                                                   currentState()));
    }

    @Override
    protected void onGameResume() {
        super.onGameResume();

        sendToDistantPlayer(String.format("%s %s", Constants.MSG_STATE_CHANGED,
                                                   currentState()));
    }

    @Override
    protected void onWallMoved(int x, int y, boolean visible) {
        wall.x = x;
        wall.y = y;
        wall.setVisible(visible);
    }

    private void waitServer() {
        // un espèce de handshake
        while(true) {
            try {
                sock.sendAndWaitConfirm(getDistantHost(), getDistantPort(), "HELLO", 2000);
                break;
            } catch (IOException e) {
                showAlert("Erreur à l'envoi de la demande de connexion au serveur : " + e.getMessage());
            }
        }
    }
}