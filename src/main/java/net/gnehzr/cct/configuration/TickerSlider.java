package net.gnehzr.cct.configuration;

import net.gnehzr.cct.i18n.StringAccessor;

import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class TickerSlider extends JPanel implements ChangeListener {
	final Timer tickTock;
	Clip clip;
	JSlider slider;
	private JSpinner spinner;
	public TickerSlider(Timer ticker) {
		this.tickTock = ticker;

        slider = new JSlider(SwingConstants.HORIZONTAL);
		spinner = new JSpinner();
		spinner.setToolTipText(StringAccessor.getString("TickerSlider.Delaymillis")); 
		add(slider);
		add(spinner);
		slider.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {
				if(slider.isEnabled())
					tickTock.start();
			}
			public void mouseReleased(MouseEvent e) {
				tickTock.stop();
			}
		});
	}

	public int getMilliSecondsDelay() {
		return slider.getValue();
	}
	public void setDelayBounds(int min, int max, int delay) {
		slider.setMinimum(min);
		slider.setMaximum(max);
		slider.setValue(delay);
		SpinnerModel model = new SpinnerNumberModel(delay,
				min,
				max,
				1);
		spinner.setModel(model);
		((NumberEditor)spinner.getEditor()).getTextField().setColumns(4);
		slider.addChangeListener(this);
		spinner.addChangeListener(this);
	}
	public void setEnabled(boolean enabled) {
		spinner.setEnabled(enabled);
		slider.setEnabled(enabled);
	}

	public static void main(String... args) {
		JFrame test = new JFrame();
		test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		TickerSlider temp = new TickerSlider(new Timer(0, null));
		temp.setDelayBounds(1, 5000, 1000);
		test.add(temp);
		test.pack();
		test.setVisible(true);
	}

	private boolean stateChanging = false;
	public void stateChanged(ChangeEvent e) {
		if(!stateChanging) {
			stateChanging = true;
			if(e.getSource() == spinner)
				slider.setValue((Integer) spinner.getValue());
			else
				spinner.setValue(slider.getValue());
			tickTock.setDelay(slider.getValue());
			stateChanging = false;
		}
	}
}
