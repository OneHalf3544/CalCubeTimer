package net.gnehzr.cct.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

/**
 * <p>
 * <p>
 * Created: 03.10.2015 21:52
 * <p>
 *
 * @author OneHalf
 */
public class Metronome {

    private static final Logger log = LogManager.getLogger(Metronome.class);

    public static Metronome createTickTockTimer(Duration duration) {
        // todo configuration.getString(VariableKey.METRONOME_CLICK_FILE, false));
        Clip clip = loadSoundFile("406__TicTacShutUp__click_1_d.wav");

        Timer timer = new Timer((int) duration.toMillis(), event -> {
            clip.stop();
            clip.setFramePosition(0);
            clip.start();
        });
        timer.setInitialDelay(0);

        return new Metronome(timer);
    }

    private boolean enabled;
    private final Timer timer;

    private Metronome(Timer timer) {
        this.timer = timer;
    }

    @NotNull
    private static Clip loadSoundFile(String methronomeResourceName) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(Objects.requireNonNull(Metronome.class.getResourceAsStream(methronomeResourceName))));
            DataLine.Info info = new DataLine.Info(Clip.class, audioInputStream.getFormat());
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(audioInputStream);
            return clip;

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e1) {
            throw new IllegalStateException(e1);
        }
    }

    public void startMetronome() {
        if (enabled) {
            log.debug("start metronome");
            timer.start();
        }
    }

    public void stopMetronome() {
        if (enabled) {
            log.debug("stop metronome");
            timer.stop();
        }
    }

    public void setDelay(int deley) {
        timer.setDelay(deley);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
