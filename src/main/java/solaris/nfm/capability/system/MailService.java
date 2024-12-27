package solaris.nfm.capability.system;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
}
