package net.gnehzr.cct.main;

import net.gnehzr.cct.configuration.Configuration;
import net.gnehzr.cct.configuration.VariableKey;
import net.gnehzr.cct.i18n.StringAccessor;
import net.gnehzr.cct.misc.JTextAreaWithHistory;
import net.gnehzr.cct.misc.SendMailUsingAuthentication;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class EmailDialog extends JDialog implements ActionListener, CaretListener {

	private static final Logger LOG = Logger.getLogger(EmailDialog.class);

	private JTextField toAddress;
	JTextField subject = null;
	JTextAreaWithHistory body = null;
	private JButton sendButton, doneButton = null;
	private final Configuration configuration;

	public EmailDialog(JDialog owner, String bodyText, Configuration configuration) {
		super(owner, true);
		this.configuration = configuration;
		Container pane = getContentPane();
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(2, 2, 2, 2);
		pane.add(new JLabel(StringAccessor.getString("EmailDialog.destination")), c); 

		toAddress = new JTextField();
		toAddress.addCaretListener(this);
		caretUpdate(null);
		toAddress.setToolTipText(StringAccessor.getString("EmailDialog.separate")); 
		c.weightx = 1;
		c.weighty = 0;
		c.gridx = 1;
		c.gridy = 0;
		pane.add(toAddress, c);

		c.weightx = 0;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 1;
		pane.add(new JLabel(StringAccessor.getString("EmailDialog.subject")), c); 

		c.weightx = 1;
		c.weighty = 0;
		c.gridx = 1;
		c.gridy = 1;
		subject = new JTextField();
		pane.add(subject, c);

		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = GridBagConstraints.REMAINDER;
		body = new JTextAreaWithHistory();
		body.setText(bodyText);
		pane.add(new JScrollPane(body), c);

		JPanel horiz = new JPanel();
		sendButton = new JButton(StringAccessor.getString("EmailDialog.send")); 
		sendButton.addActionListener(this);
		horiz.add(sendButton);
		doneButton = new JButton(StringAccessor.getString("EmailDialog.done")); 
		doneButton.addActionListener(this);
		horiz.add(doneButton);
		c.weightx = 0;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 3;
		pane.add(horiz, c);

		setPreferredSize(new Dimension(550, 350));
		pack();
		setLocationRelativeTo(null);
	}

	static String toPrettyString(String[] recievers) {
		String result = ""; 
		for(String reciever : recievers) {
			result += ", " + reciever; 
		}
		return result.substring(2);
	}

	private class EmailWorker extends SwingWorker<Void, Void> {
		private SendMailUsingAuthentication smtpMailSender;
		private String[] receivers;
		WaitingDialog waiting;
		private Exception error;
		public EmailWorker(JDialog owner, char[] pass, String[] receivers) {
			smtpMailSender = new SendMailUsingAuthentication(pass, configuration);
			this.receivers = receivers;

			waiting = new WaitingDialog(owner, true, this);
			waiting.setResizable(false);
			waiting.setTitle(StringAccessor.getString("EmailDialog.working")); 
			waiting.setText(StringAccessor.getString("EmailDialog.sending")); 
			waiting.setButtonText(StringAccessor.getString("EmailDialog.cancel")); 
		}
		@Override
		protected Void doInBackground() {
			SwingUtilities.invokeLater(() -> {
                waiting.setVisible(true);
            });
			try {
//				System.out.print("Sending...");
				smtpMailSender.postMail(receivers, subject.getText(), body.getText());
//				LOG.info("done!");
			} catch (MessagingException e) {
				error = e;
				LOG.info("unexpected exception", e);
			}
			return null;
		}
		@Override
		protected void done() {
			waiting.setButtonText(StringAccessor.getString("EmailDialog.ok")); 
			if(error == null) {
				waiting.setTitle(StringAccessor.getString("EmailDialog.sucess")); 
				waiting.setText(StringAccessor.getString("EmailDialog.successmessage") + "\n" + StringAccessor.getString("EmailDialog.recipients") + toPrettyString(receivers));   
			} else {
				waiting.setTitle(StringAccessor.getString("EmailDialog.error")); 
				waiting.setText(StringAccessor.getString("EmailDialog.failmessage") + "\n" + StringAccessor.getString("EmailDialog.error") + ": " + error.getLocalizedMessage());    
			}
		}
	}

	private static class WaitingDialog extends JDialog {
		private JTextArea message;
		private JButton button;
		public WaitingDialog(JDialog owner, boolean modal, final SwingWorker<?, ?> worker) {
			super(owner, modal);
			message = new JTextArea();
			message.setEditable(false);
			message.setLineWrap(true);
			message.setWrapStyleWord(true);
			button = new JButton();
			button.addActionListener(arg0 -> {
                setVisible(false);
                //TODO - this is apparently not interrupting the call to postMail, I have
                //no idea how to fix it though.
                worker.cancel(true);
            });
			getContentPane().add(message, BorderLayout.CENTER);
			getContentPane().add(button, BorderLayout.PAGE_END);
			setPreferredSize(new Dimension(200, 150));
			pack();
			setLocationRelativeTo(owner);
		}
		public void setText(String text) {
			message.setText(text);
		}
		public void setButtonText(String text) {
			button.setText(text);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == sendButton) {
			if(configuration.getBoolean(VariableKey.SMTP_ENABLED, false)) {
				char[] pass = null;
				if(configuration.getBoolean(VariableKey.SMTP_ENABLED, false) &&
						configuration.getString(VariableKey.SMTP_PASSWORD, false).isEmpty()) {
					PasswordPrompt prompt = new PasswordPrompt(this);
					prompt.setVisible(true);
					if(prompt.isCanceled()) {
						return;
					}
					pass = prompt.getPassword();
				}

				SwingWorker<Void, Void> sendEmail = new EmailWorker(this, pass, toAddress.getText().split("[,;]")); 
				sendEmail.execute();
			} else {
				try {
					URI mailTo = new URI("mailto", 
							toAddress.getText() + "?" + 
							"subject=" + subject.getText() + "&body=" + body.getText(),  
							null);
					Desktop.getDesktop().mail(mailTo);
				} catch (URISyntaxException | IOException e1) {
					LOG.info("unexpected exception", e1);
				}
			}
		} else if (source == doneButton) {
			setVisible(false);
		}
	}
	private static class PasswordPrompt extends JDialog implements ActionListener {
		private boolean canceled = true;
		private JPasswordField pass = null;
		private JButton ok, cancel = null;
		public PasswordPrompt(JDialog parent) {
			super(parent, StringAccessor.getString("EmailDialog.passwordprompt"), true); 
			Container pane = getContentPane();

			pass = new JPasswordField(20);
			ok = new JButton(StringAccessor.getString("EmailDialog.ok")); 
			ok.addActionListener(this);
			getRootPane().setDefaultButton(ok);
			cancel = new JButton(StringAccessor.getString("EmailDialog.cancel")); 
			cancel.addActionListener(this);
			JPanel okCancel = new JPanel();
			okCancel.add(ok);
			okCancel.add(cancel);

			pane.add(pass, BorderLayout.CENTER);
			pane.add(okCancel, BorderLayout.PAGE_END);
			pack();
			setResizable(false);
			setLocationRelativeTo(parent);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if(source == ok) {
				canceled = false;
				setVisible(false);
			} else if(source == cancel) {
				canceled = true;
				setVisible(false);
			}
		}
		public boolean isCanceled() {
			return canceled;
		}
		public char[] getPassword() {
			return pass.getPassword();
		}
	}
	@Override
	public void caretUpdate(CaretEvent e) {
		setTitle(StringAccessor.getString("EmailDialog.emailto") + toAddress.getText()); 
	}
}
