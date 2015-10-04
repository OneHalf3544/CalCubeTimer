package net.gnehzr.cct.speaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javazoom.jl.decoder.JavaLayerException;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.statistics.SolveTime;
import net.gnehzr.cct.statistics.SolveType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Singleton
public class NumberSpeaker implements Comparable<NumberSpeaker> {

	private static final Logger LOG = LogManager.getLogger(NumberSpeaker.class);

	private final ExecutorService threadPool = Executors.newFixedThreadPool(5);

	private static final String ZIP_EXTENSION = ".zip";

	private final Configuration configuration;
	private Map<String, NumberSpeaker> numberSpeakers;
	private NumberSpeaker[] alphabetized;
	private ZipFile clips;
	private String name;

	public enum TalkerType {
		TIMER_OFF("timer_off"),
		TIMER_RUNNING("timer_running"),
		TIMER_RESET("timer_reset");

		private String desc;

		TalkerType(String desc) {
			this.desc = desc;
		}
		public String toString() {
			return desc;
		}
	}

	@Inject
	public NumberSpeaker(Configuration configuration) {
		this.configuration = configuration;
	}

	private Map<String, NumberSpeaker> getNumberSpeakers() {
		if (numberSpeakers != null) {
			return numberSpeakers;
		}
		numberSpeakers = new HashMap<>();
		for(File f : Objects.requireNonNull(configuration.getVoicesFolder().listFiles())) {
            String fileName = f.getName();
            if(fileName.endsWith(ZIP_EXTENSION) && f.isFile()) {
                try {
					String name = f.getName();
                    name = name.substring(0, name.length() - ZIP_EXTENSION.length());
					numberSpeakers.put(name, new NumberSpeaker(name, f, configuration));
                } catch (IOException e) {
                    LOG.warn("ignored exception", e);
                }
            }
        }
		return numberSpeakers;
	}
	public NumberSpeaker[] getSpeakers() {
		if(alphabetized == null) {
			alphabetized = new ArrayList<>(getNumberSpeakers().values()).toArray(new NumberSpeaker[0]);
			Arrays.sort(alphabetized);
		}
		return alphabetized.clone();
	}

	//returns null if the specified voice was not found
	@Nullable
	NumberSpeaker getSpeaker(String name) {
		return getNumberSpeakers().get(name);
	}

	public NumberSpeaker getCurrentSpeaker() {
		NumberSpeaker c = getSpeaker(configuration.getString(VariableKey.VOICE, false));
		if(c == null) {
			NumberSpeaker[] speakers = getSpeakers();
			if(speakers.length > 0) {
				c = speakers[0];
			}
			else {
				c = new NumberSpeaker(configuration);
			}
		}
		return c;
	}

	private NumberSpeaker(String name, File zip, Configuration configuration) throws IOException {
		this.configuration = configuration;
		clips = new ZipFile(zip);
		this.name = name;
	}


	public void sayInspectionWarning(Duration seconds) {
		threadPool.submit(() -> {
			try {
				getCurrentSpeaker().speak(false, new SolveTime(seconds));
			} catch (Exception e) {
				LOG.error("error during speak warning time", e);
			}
		});
	}


	public void speakTime(SolveTime latestTime) {
		if (!configuration.getBoolean(VariableKey.SPEAK_TIMES)) {
			return;
		}
		threadPool.submit(() -> {
			try {
				getCurrentSpeaker().speak(latestTime);
			} catch (JavaLayerException e) {
				LOG.error("unexpected exception", e);
			}
		});
	}

	public String toString() {
		return name;
	}
	
    //appends .mp3 to name
    private MP3 getMP3FromName(String name) {
    	try {
    		return new MP3(clips.getInputStream(new ZipEntry(name + ".mp3")));
    	} catch(IOException e) {
    		throw new RuntimeException("Error opening file: " + name + ".mp3 in " + this.name + ".zip");
    	}
    }
    
    public void speak(TalkerType type) throws JavaLayerException {
    	if(clips == null) {
			return;
		}
		MP3 temp = getMP3FromName(type.toString());
		temp.play();
	}
    
    public void speak(SolveTime time) throws JavaLayerException {
    	if(time.isType(SolveType.DNF)) {
			LOG.debug("speak dnf");
			getMP3FromName("dnf").play();
		}
    	else {
			speak(false, time);
		}
    }
    
    //"Your time is " + lastin.toSolveTime(null, null).value() / 100. + " seconds"
    //Speaks something of the form "xyz.ab seconds"
    void speak(boolean yourTime, SolveTime time) throws JavaLayerException {
		Objects.requireNonNull(clips, "Failed to open " + name + ".zip!");
		LOG.debug("speak time: {}", time);

		if(yourTime) {
    		getMP3FromName("your_time_is").play();
    	}
		Boolean clockFormat = configuration.useClockFormat();

		List<String> wordFileNamesList = breakItDown(time.getTime(), clockFormat);
    	for(String file : wordFileNamesList) {
    		getMP3FromName(file).play();
    	}
    }
    
    private LinkedList<String> breakItDown(Duration duration, boolean clockFormat) {
		long hundredths = duration.toMillis() / 10;
    	long largest; //either the number of minutes or hundreds of seconds
    	if(clockFormat) {
    		largest = hundredths / (60*100);
    		hundredths %= (60*100);
    	} else {
    		largest = hundredths / 10000;
    		hundredths %= 10000;
    	}
    	long tens = hundredths / 1000;
    	hundredths %= 1000;
    	long ones = hundredths / 100;
    	hundredths %= 100;
    	long tenths = hundredths / 10;
    	hundredths %= 10;
    	LinkedList<String> temp = new LinkedList<>();
    	if(largest != 0) {
    		if(clockFormat) {
    			long minTens = largest / 10;
    			long minSecs = largest % 10;
    			dealWithTens(temp, minTens, minSecs);
    			if(largest == 1)
    				temp.add("minute");
    			else if(largest > 1)
    				temp.add("minutes");
    		} else
    			temp.add(100*largest+"");
    	}
		if(tens+ones != 0 || tens+ones == 0 && (largest == 0 || tenths + hundredths != 0)) {
			dealWithTens(temp, tens, ones);
		}
		if(tenths + hundredths != 0) {
	    	temp.add("point");
	    	temp.add(tenths+"");
	    	if(hundredths != 0) {
	    		temp.add(hundredths+"");
	    	}
		}
		boolean oneSecond = tens==0 && ones==1 && tenths==0 && hundredths==0;
		if(tens + ones + tenths + hundredths != 0 || !clockFormat)
			temp.add("second" + (oneSecond ? "" : "s"));
		
    	return temp;
    }
    
    private void dealWithTens(LinkedList<String> temp, long tens, long ones) {
		if(tens == 1)
			temp.add((10*tens + ones) + "");
		else if(tens != 0) {
			temp.add(10*tens + "");
			if(ones != 0)
				temp.add(ones + "");
		} else {
			temp.add(ones + "");
		}
    }

    public int hashCode() {
    	return this.name.hashCode();
    }

	public boolean equals(Object obj) {
		if (!(obj instanceof NumberSpeaker)) {
			return false;
		}
		NumberSpeaker o = (NumberSpeaker) obj;
		return this.name.equals(o.name);
	}

	@Override
	public int compareTo(NumberSpeaker o) { //this is needed for sorting
		return name.compareTo(o.name);
	}
}

