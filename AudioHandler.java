/**
 * Created by ros_fechan on 6/7/2017.
 */
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;
import com.sun.javafx.application.PlatformImpl;

/*This class allows the program to play music in a ba
ckground thread*/
public class AudioHandler implements Runnable{
    private MediaPlayer songPlayer;

    public AudioHandler(String audioFileName){
        PlatformImpl.startup(this);
        Media song = new Media(new File(audioFileName).toURI().toString());
        this.songPlayer = new MediaPlayer(song);
    }

    //Starts itself in a new background thread so music will play according.
    public void startMusic(){
        Thread t = new Thread(this);
        t.start();
    }

    public void stopMusic(){
        this.songPlayer.stop();
    }

    public void setVolume(double volume){
        this.songPlayer.setVolume(volume);
    }

    //This part actually plays the music by using MediaPlayer.play()
    @Override
    public void run(){
        try{
            this.songPlayer.play();
        } catch (NullPointerException e){
            //Apparently this will happen even if the music plays successfully.
        }
    }
}
