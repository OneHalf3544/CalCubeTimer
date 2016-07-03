package net.gnehzr.cct.main.actions;

import net.gnehzr.cct.help.AboutScrollFrame;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.main.CALCubeTimerFrame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.awt.event.ActionEvent;

/**
 * <p>
 * <p>
 * Created: 09.01.2015 22:19
 * <p>
 *
 * @author OneHalf
 */
@Component
class AboutAction extends AbstractNamedAction {

	private static final Logger LOG = LogManager.getLogger(AboutAction.class);

	private AboutScrollFrame makeMeVisible;

	public AboutAction() {
		super("showabout");
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
