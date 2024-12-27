package solaris.nfm.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import solaris.nfm.exception.base.ExceptionBase;

public class RegexUtil
{
	public static void checkMailAddress(final String mailAddress) throws Exception
	{
		final String regex = "^\\w+((-\\w+)|(\\.\\w+))*\\@[A-Za-z0-9]+((\\.|-)[A-Za-z0-9]+)*\\.[A-Za-z]+$";
		final Pattern pattern = Pattern.compile(regex);
		final Matcher matcher = pattern.matcher(mailAddress);
		if (matcher.find() == false) throw new ExceptionBase(400, "The format of mail address (" + mailAddress + ") is invalid.");
	}
}
