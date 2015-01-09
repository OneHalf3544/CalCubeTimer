package net.gnehzr.cct.configuration;

import net.gnehzr.cct.i18n.StringAccessor;

import javax.swing.*;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.event.ChangeEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TickerSlider extends JPanel {

	final Timer tickTock;

	JSlider slider;

	private JSpinner spinner;

	public TickerSlider(Timer ticker) {
		this.tickTock = ticker;

        slider = new JSlider(SwingConstants.HORIZONTAL);
		spinner = new JSpinner();
		spinner.setToolTipText(StringAccessor.getString("TickerSlider.Delaymillis")); 
		add(slider);
		add(spinner);
		slider.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if(slider.isEnabled())
					tickTock.start();
			}
			@Override
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
		SpinnerModel model = new SpinnerNumberModel(delay, min, max, 1);
		spinner.setModel(model);
		((NumberEditor)spinner.getEditor()).getTextField().setColumns(4);

		slider.addChangeListener(this::stateChanged);
		spinner.addChangeListener(this::stateChanged);
	}
	@Override
	public void setEnabled(boolean enabled) {
		spinner.setEnabled(enabled);
		slider.setEnabled(enabled);
	}

	private boolean stateChanging = false;

	private void stateChanged(ChangeEvent e) {
		if (stateChanging) {
			return;
		}

		stateChanging = true;
		if(e.getSource() == spinner) {
			slider.setValue((Integer) spinner.getValue());
		}
        else {
			spinner.setValue(slider.getValue());
		}
		tickTock.setDelay(slider.getValue());
		stateChanging = false;
	}
}
