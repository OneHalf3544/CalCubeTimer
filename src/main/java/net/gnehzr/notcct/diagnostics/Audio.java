package net.gnehzr.notcct.diagnostics;
import org.apache.log4j.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

public class Audio{

	private static final Logger LOG = Logger.getLogger(Audio.class);

	public static void main(String[] args){
		Mixer.Info[]    aInfos = AudioSystem.getMixerInfo();
		LOG.info("Available Mixers: " + aInfos.length);
		for (int i = 0; i < aInfos.length; i++) {
			Mixer mixer = AudioSystem.getMixer(aInfos[i]);
			Line.Info lineInfo = new Line.Info(TargetDataLine.class);
			LOG.info("Mixer " + i + ": " +
					aInfos[i].getName() + " desc: " + aInfos[i].getDescription()
					+ " vend: " + aInfos[i].getVendor()
					+ " ver: " + aInfos[i].getVersion());
			if (mixer.isLineSupported(lineInfo)) {
				Line.Info[] info = mixer.getTargetLineInfo(lineInfo);

				for ( int j = 0;  j < info.length; ++j ) {
					AudioFormat af;
					AudioFormat[] forms = ((DataLine.Info)
							info[j]).getFormats();
					for ( int n = 0;  n < forms.length;  ++n ) {
						af = forms[n];
						LOG.info("    " + af);
					}

				}

			}
		}

	}
}
