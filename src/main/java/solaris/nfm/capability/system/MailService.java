package solaris.nfm.capability.system;

import java.io.UnsupportedEncodingException;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MailService
{
	@Autowired
	private JavaMailSender	mailSender;

	@Value("${spring.mail.send-from}")
	private String			sendFrom;

	private String			pmMailSubject	= "OAM Performance Measurement Alarm";
	private String			fmMailSubject	= "OAM Fault Alarm !!!";

	public void sendPm(final Set<String> sendTo, final String message)
	{
		send(pmMailSubject, sendTo, message);
	}

	public void sendFm(final Set<String> sendTo, final String message)
	{
		send(fmMailSubject, sendTo, message);
	}

	@Async
	private void send(final String subject, final Set<String> sendTo, final String message)
	{
		if (sendTo == null || sendTo.size() == 0) return;

		final SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
		simpleMailMessage.setFrom(sendFrom);
		simpleMailMessage.setTo(sendTo.toArray(new String[0]));
		simpleMailMessage.setSubject(subject);
		simpleMailMessage.setText(message);
		try
		{
			mailSender.send(simpleMailMessage);
		} catch (final MailException ex)
		{
			log.error("\t[Mail] Sending mail is failed. message: \n{}", ex.getLocalizedMessage());
		}
	}

	public void sendMimeMessageWithAttachments(final String subject, final String from, final String to, final String bcc, final InputStreamSource attachedFileContent)
			throws MessagingException, UnsupportedEncodingException
	{
		final MimeMessage message = this.mailSender.createMimeMessage();
		final MimeMessageHelper helper = new MimeMessageHelper(message, true);
		helper.setSubject(subject);
		helper.setFrom(from, "OAM Service");
		helper.setTo(to);
		helper.setBcc(bcc);
		helper.setReplyTo(from);
		helper.setText("stub", false);
		helper.addAttachment("OAM Report.pdf", attachedFileContent);
		this.mailSender.send(message);
		log.debug("\t Mail send.");
	}
}
