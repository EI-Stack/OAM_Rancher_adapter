package solaris.nfm.service;

import java.io.IOException;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import solaris.nfm.capability.annotation.jsonvalid.JsonService;
import solaris.nfm.exception.base.ExceptionBase;

@Service
public class DataFormationCheckService
{
	@Autowired
	private JsonService jsonService;

	public void checkSupi(final String supi) throws ExceptionBase, IOException
	{
		final JsonNode supiNode = jsonService.getJsonNodeFromClasspathForYaml("json-schema/R16/TS29571_CommonData.yaml").path("components").path("schemas").path("Supi");
		Pattern pattern = Pattern.compile(supiNode.path("pattern").asText());
		final String patternStr = "^(imsi-[0-9]{5,15}|nai-.+|gci-.+|gli-.+)$";
		pattern = Pattern.compile(patternStr);

		if (!pattern.matcher(supi).matches())
		{
			throw new ExceptionBase("Invalid SUPI format: " + supi + ", it does not meet regexp \"" + patternStr + "\"");
		}
	}
}
