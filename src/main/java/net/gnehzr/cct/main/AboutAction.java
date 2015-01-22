package net.gnehzr.cct.main;

import net.gnehzr.cct.help.AboutScrollFrame;
import net.gnehzr.cct.i18n.StringAccessor;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:19
 * <p>
 *
 * @author OneHalf
 */
class AboutAction extends AbstractAction {
	private static final Logger LOG = Logger.getLogger(AboutAction.class);
	private AboutScrollFrame makeMeVisible;
	public AboutAction() {
		try {
			makeMeVisible = new AboutScrollFrame(CALCubeTimerFrame.class.getResource("about.html"), CALCubeTimerFrame.CUBE_ICON.getImage());
			setEnabled(true);
		} catch (Exception e1) {
			LOG.info("unexpected exception", e1);
			setEnabled(false);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e){
		makeMeVisible.setTitle(StringAccessor.getString("CALCubeTimer.about") + CALCubeTimerFrame.CCT_VERSION);
		makeMeVisible.setVisible(true);
	}
}
