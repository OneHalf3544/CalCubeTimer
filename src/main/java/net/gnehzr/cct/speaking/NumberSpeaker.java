package net.gnehzr.cct.speaking;

import com.google.inject.Inject;
import javazoom.jl.decoder.JavaLayerException;
import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.statistics.SolveTime;
import net.gnehzr.cct.statistics.SolveType;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NumberSpeaker implements Comparable<NumberSpeaker> {

	private static final Logger LOG = Logger.getLogger(NumberSpeaker.class);
	private final Configuration configuration;

	public enum TalkerType {
		TIMER_OFF("timer_off"), TIMER_RUNNING("timer_running"), TIMER_RESET("timer_reset");
		private String desc;
		private TalkerType(String desc) {
			this.desc = desc;
		}
		public String toString() {
			return desc;
		}
	}
	private final String ZIP_EXTENSION = ".zip";
	private Map<String, NumberSpeaker> numberSpeakers;

	private Map<String, NumberSpeaker> getNumberSpeakers() {
		if (numberSpeakers != null) {
			return numberSpeakers;
		}
		numberSpeakers = new HashMap<>();
		for(File f : configuration.getVoicesFolder().listFiles()) {
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
	private NumberSpeaker[] alphabetized;
	public NumberSpeaker[] getSpeakers() {
		if(alphabetized == null) {
			alphabetized = new ArrayList<>(getNumberSpeakers().values()).toArray(new NumberSpeaker[0]);
			Arrays.sort(alphabetized);
		}
		return alphabetized.clone();
	}
	//returns null if the specified voice was not found
	private NumberSpeaker getSpeaker(String name) {
		return getNumberSpeakers().get(name);
	}
	public NumberSpeaker getCurrentSpeaker() {
		NumberSpeaker c = getSpeaker(configuration.getString(VariableKey.VOICE, false));
		if(c == null) {
			NumberSpeaker[] speakers = getSpeakers();
			if(speakers.length > 0)
				c = speakers[0];
			else
				c = new NumberSpeaker(configuration);
		}
		return c;
	}

	@Inject
	public NumberSpeaker(Configuration configuration) {
		this.configuration = configuration;
	}
	
	private ZipFile clips;
	private String name;
	private NumberSpeaker(String name, File zip, Configuration configuration) throws IOException {
		this.configuration = configuration;
		clips = new ZipFile(zip);
		this.name = name;
	}
    
	public String toString() {
		return name;
	}
	
    //appends .mp3 to name
    private MP3 getMP3FromName(String name) throws Exception {
    	try {
    		return new MP3(clips.getInputStream(new ZipEntry(name + ".mp3")));
    	} catch(Exception e) {
    		throw new Exception("Error opening file: " + name + ".mp3 in " + this.name + ".zip");
    	}
    }
    
    public void speak(TalkerType type) {
    	if(clips == null) return;
		try {
			MP3 temp = getMP3FromName(type.toString());
			temp.play();
		} catch (IOException e) {
			LOG.info("unexpected exception", e);
		} catch (JavaLayerException e) {
			LOG.info("unexpected exception", e);
		} catch (Exception e) {
			LOG.info("unexpected exception", e);
		}
    }
    
    public void speak(SolveTime time) throws Exception {
    	if(time.isType(SolveType.DNF))
    		getMP3FromName("dnf").play();
    	else
    		speak(false, (int)Math.round(time.secondsValue() * 100));
    }
    
    //"Your time is " + lastin.toSolveTime(null, null).value() / 100. + " seconds"
    //Speaks something of the form "xyz.ab seconds"
    public void speak(boolean yourTime, long hundredths) throws Exception {
    	if(clips == null)
    		throw new Exception("Failed to open " + name + ".zip!");
    	if(yourTime) {
    		getMP3FromName("your_time_is").play();
    	}
    	Boolean clockFormat;
    	try {
    		clockFormat = configuration.getBoolean(VariableKey.CLOCK_FORMAT, false);
    	} catch(Exception e) {
    		clockFormat = true;
    	}
    	LinkedList<String> time = breakItDown(hundredths, clockFormat);
    	for(String file : time) {
    		getMP3FromName(file).play();
    	}
    }
    
    private LinkedList<String> breakItDown(long hundredths, boolean clockFormat) {
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

    // test client
	// todo
    public void main(String[] args) {
    	NumberSpeaker carrie = getSpeaker("carrie");
		for(int ch = 12000; ch < 13000; ch+=10) {
			LOG.info("TIME: " + ch / 100.);
			try {
				carrie.speak(false, ch);
			} catch (Exception e) {
				LOG.info("unexpected exception", e);
			}
		}
    }
    public int hashCode() {
    	return this.name.hashCode();
    }
    public boolean equals(Object obj) {
    	if (obj instanceof NumberSpeaker) {
			NumberSpeaker o = (NumberSpeaker) obj;
			return this.name.equals(o.name);
		}
    	return false;
    }
	@Override
	public int compareTo(NumberSpeaker o) { //this is needed for sorting
		return name.compareTo(o.name);
	}
}

