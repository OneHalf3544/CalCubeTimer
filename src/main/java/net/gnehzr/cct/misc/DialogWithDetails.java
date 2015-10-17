package net.gnehzr.cct.misc;

import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.main.CALCubeTimerFrame;
import org.pushingpixels.lafwidget.LafWidget;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

public class DialogWithDetails extends JDialog implements ActionListener {
	private JButton copy, ok;
	private JScrollPane detailsPane;
	private String details;

	public DialogWithDetails(Window w, String title, String message, String details) {
		super(w, title, ModalityType.DOCUMENT_MODAL);
		initializeGUI(message, details);
	}

	private void initializeGUI(String message, String details) {
		this.details = details;
		JPanel pane = new JPanel(new BorderLayout());
		setContentPane(pane);
		
		JTextArea detailsArea = new JTextArea("CCT " + CALCubeTimerFrame.CCT_VERSION + " " + new Date() + "\n" + details, 15, 30);
		detailsArea.putClientProperty(LafWidget.TEXT_SELECT_ON_FOCUS, Boolean.FALSE);
		detailsPane = new JScrollPane(detailsArea);
		detailsArea.setEditable(false);
		
		String cpy, okay;
		if(StringAccessor.keyExists("Utils.copy"))
			cpy = StringAccessor.getString("Utils.copy");
		else
			cpy = "Copy";
		if(StringAccessor.keyExists("Utils.ok"))
			okay = StringAccessor.getString("Utils.ok");
		else
			okay = "Ok";
		copy = new JButton(cpy);
		copy.addActionListener(this);
		ok = new JButton(okay);
		ok.addActionListener(this);
		JPanel copy_ok = new JPanel();
		copy_ok.add(copy);
		copy_ok.add(ok);
		
		pane.add(new JLabel("<html>" + message.replaceAll("\n", "<br>") + "</html>"), BorderLayout.PAGE_START);
		pane.add(detailsPane, BorderLayout.CENTER);
		pane.add(copy_ok, BorderLayout.PAGE_END);
		
		pack();
		setLocationRelativeTo(getOwner());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if(source == copy) {
			StringSelection ss = new StringSelection(details);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
		} else if(source == ok)
			setVisible(false);
	}
}
