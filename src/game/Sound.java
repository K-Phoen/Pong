package game;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Sound extends Thread {

    private static HashMap<String, InputStream> streams = new HashMap<String, InputStream>();
    private static HashMap<String, AudioFormat> formats = new HashMap<String, AudioFormat>();
    private String sound_to_play;


    /**
     * Crée une instance de la classe Sound. Elle sera chargée de lire un son
     * dans un thread
     *
     * @param sound Son à lire
     */
    private Sound(String sound) {
        sound_to_play = sound;
    }

    /**
     * Lance le thread de lecture du son : ne pas appeler directement !
     */
    @Override
    public void run() {
        try {
            doPlay(sound_to_play);
        } catch (Exception e) {
            //System.err.println("Erreur à la lecture du son "+sound_to_play+" : "+e.getMessage());
        }
    }

    /**
     * Charge un fichier son en mémoire
     *
     * @param filename Adresse du fichier à charger
     *
     * @throws UnsupportedAudioFileException Si le fichier ne contient pas du
     *                                       son ou n'est pas reconnu par le système
     * @throws IOException Si une erreur survient à la lecture du fichier
     */
    public static void load(String filename) throws UnsupportedAudioFileException, IOException {
        if (streams.containsKey(filename))
            return;

        AudioInputStream stream = AudioSystem.getAudioInputStream(new File(filename));
        AudioFormat format = stream.getFormat();

        formats.put(filename, format);
        streams.put(filename, new ByteArrayInputStream(getSamples(stream, format)));
    }

    public static byte[] getSamples(AudioInputStream stream, AudioFormat format) throws IOException {
        int length = (int) (stream.getFrameLength() * format.getFrameSize());
        DataInputStream in = new DataInputStream(stream);
        byte[] samples = new byte[length];

        in.readFully(samples);

        return samples;
    }

    /**
     * Joue un son dans un thread
     *
     * @param sound Chemin du fichier contenant le son à jouer
     */
    public static void play(String sound) {
        Thread t = new Sound(sound);
		//t.setPriority(Thread.MIN_PRIORITY);

		t.start();
    }

    /**
     * Réalise la lecture du son
     *
     * @param filename Chemin du fichier contenant le son à jouer
     *
     * @throws UnsupportedAudioFileException Si le fichier ne contient pas du
     *                                       son ou n'est pas reconnu par le système
     * @throws IOException Si une erreur survient à la lecture du fichier
     */
    private static void doPlay(String filename) throws UnsupportedAudioFileException, IOException {
        if (!streams.containsKey(filename))
            load(filename);

        AudioFormat format = formats.get(filename);
        InputStream source = streams.get(filename);

        // 100 ms buffer for real time change to the sound stream
        int bufferSize = format.getFrameSize() * Math.round(format.getSampleRate() / 10);
        byte[] buffer = new byte[bufferSize];
        SourceDataLine line;

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, bufferSize);
        } catch (LineUnavailableException e) {
            throw new IOException(e);
        }

        line.start();
        
        int numBytesRead = 0;
        while (numBytesRead != -1) {
            numBytesRead = source.read(buffer, 0, buffer.length);
            
            if (numBytesRead != -1)
                line.write(buffer, 0, numBytesRead);
        }

        line.drain();
        line.close();

        source.reset();
    }

    /**
     * Petit test de lecture d'un son sans pré-chargement
     * @param args
     */
    public static void main(String[] args) {
        try {
            Sound.play("./data/pong_1.wav");
        } catch (Exception e) {
            System.err.println("Erreur à la lecture du son : " + e);
        }
    }
}