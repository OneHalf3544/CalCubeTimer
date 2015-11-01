package net.gnehzr.cct.misc;

import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.main.CalCubeTimerGui;
import org.jetbrains.annotations.NotNull;
import org.pushingpixels.lafwidget.LafWidget;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Date;

public class DialogWithDetails extends JDialog {

	public DialogWithDetails(Window w, String title, @NotNull String message, String details) {
		super(w, title, ModalityType.DOCUMENT_MODAL);
		initializeGUI(message, details);
	}

	private void initializeGUI(@NotNull String message, String details) {
		JPanel pane = new JPanel(new BorderLayout());
		setContentPane(pane);
		
		JTextArea detailsArea = new JTextArea("CCT " + CalCubeTimerGui.CCT_VERSION + " " + new Date() + "\n" + details, 15, 30);
		detailsArea.putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
		JScrollPane detailsPane = new JScrollPane(detailsArea);
		detailsArea.setEditable(false);

		String copyText = StringAccessor.keyExists("Utils.copy") ? StringAccessor.getString("Utils.copy") : "Copy";
		String okay = StringAccessor.keyExists("Utils.ok") ? StringAccessor.getString("Utils.ok") : "Ok";

		JButton copyButton = new JButton(copyText);
		copyButton.addActionListener(e -> {
			StringSelection ss = new StringSelection(details);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
		});
		JButton ok = new JButton(okay);
		ok.addActionListener(E -> setVisible(false));

		JPanel copy_ok = new JPanel();
		copy_ok.add(copyButton);
		copy_ok.add(ok);
		
		pane.add(new JLabel("<html>" + message.replaceAll("\n", "<br>") + "</html>"), BorderLayout.PAGE_START);
		pane.add(detailsPane, BorderLayout.CENTER);
		pane.add(copy_ok, BorderLayout.PAGE_END);
		
		pack();
		setLocationRelativeTo(getOwner());
	}
}
