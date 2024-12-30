package solaris.nfm.model.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumNamePatternValidator implements ConstraintValidator<EnumNamePattern, Enum<?>>
{
	private Pattern pattern;

	@Override
	public void initialize(final EnumNamePattern annotation)
	{
		try
		{
			pattern = Pattern.compile(annotation.regexp());
		} catch (final PatternSyntaxException e)
		{
			throw new IllegalArgumentException("Given regex is invalid", e);
		}
	}

	@Override
	public boolean isValid(final Enum<?> value, final ConstraintValidatorContext context)
	{
		if (value == null)
		{
			return true;
		}

		final Matcher m = pattern.matcher(value.name());
		return m.matches();
	}
}