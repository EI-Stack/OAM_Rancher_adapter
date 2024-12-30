package solaris.nfm.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.rest.RestServiceBase;
import solaris.nfm.exception.base.ExceptionBase;

@Service
@Slf4j
public class RicService extends RestServiceBase
{
	@Value("${solaris.server.ric.http.url}")
	private String			url;
	@Autowired
	private ObjectMapper	objectMapper;
	@Autowired
	private FgcService fgcService;

	@PostConstruct
	public void init()
	{
		super.setNetworkUrl(this.url);

		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		httpHeaders.setOrigin(this.url);
		// httpHeaders.setBasicAuth(this.configBean.getUsername(), this.configBean.getPassword());

		super.setHttpHeaders(httpHeaders);
	}

	@Cacheable(cacheNames = "ricFields")
	public ArrayNode getFields() throws ExceptionBase
	{
		log.debug("no cache");
		return (ArrayNode) super.doMethodGet("/fields");
	}

	@Cacheable(cacheNames = "ricGnbs")
	public ArrayNode getGnbs(final String fieldId) throws ExceptionBase
	{
		log.debug("no cache");
		return (ArrayNode) super.doMethodGet("/fields/" + fieldId + "/gnbs");
	}

	public void checkNci(final ArrayNode cellArray) throws StreamReadException, DatabindException, IOException, ExceptionBase
	{
		final Set<Integer> ncis = getNcis();
		final Set<Integer> invalidNcis = new HashSet<>();

		for (final JsonNode cell : cellArray)
		{
			final Integer nci = cell.path("NCI").asInt();
			if (!ncis.contains(nci)) invalidNcis.add(nci);
		}

		if (invalidNcis.size() > 0) throw new ExceptionBase("Cell NCI " + invalidNcis + " is invalid.");
	}

	public Set<Integer> getNcis() throws StreamReadException, DatabindException, IOException, ExceptionBase
	{
		final JsonNode sourceRoot = super.doMethodGet("/v1/privateAPI/getCellList");
		return this.objectMapper.readValue(sourceRoot.path("NCI").traverse(), new TypeReference<LinkedHashSet<Integer>>()
		{});
	}

	public void checkRicSliceId(final Integer ricSliceId) throws StreamReadException, DatabindException, IOException, ExceptionBase
	{
		final Set<Integer> ricSliceIds = getRicSliceIds();
		if (!ricSliceIds.contains(ricSliceId)) throw new ExceptionBase("ricSliceId (" + ricSliceId + ") is invalid.");
	}

	public Set<Integer> getRicSliceIds() throws StreamReadException, DatabindException, IOException, ExceptionBase
	{
		final JsonNode sourceRoot = super.doMethodGet("/v1/networkSliceInstances/ricSlices");
		return this.objectMapper.readValue(sourceRoot.path("networkSliceInstanceUid").traverse(), new TypeReference<LinkedHashSet<Integer>>()
		{});
	}

	public JsonNode getRicSlice(final Integer ricSliceId) throws ExceptionBase, StreamReadException, DatabindException, IOException
	{
		final ObjectNode ricSlice = (ObjectNode) super.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/checkRicSlice", null);
		final ObjectNode sliceContent = (ObjectNode) ricSlice.path("SliceContent");

		ricSlice.put("ricSliceId", ricSliceId);
		final JsonNode dLThptPerSlice = sliceContent.path("dLThptPerSlice");
		sliceContent.replace("dLThptPerSlice", this.objectMapper.readTree(dLThptPerSlice.asText().replace("'", "\"")));
		final JsonNode dLThptPerUE = sliceContent.path("dLThptPerUE");
		sliceContent.replace("dLThptPerUE", this.objectMapper.readTree(dLThptPerUE.asText().replace("'", "\"")));
		final JsonNode uLThptPerSlice = sliceContent.path("uLThptPerSlice");
		sliceContent.replace("uLThptPerSlice", this.objectMapper.readTree(uLThptPerSlice.asText().replace("'", "\"")));
		final JsonNode uLThptPerUE = sliceContent.path("uLThptPerUE");
		sliceContent.replace("uLThptPerUE", this.objectMapper.readTree(uLThptPerUE.asText().replace("'", "\"")));

		return ricSlice;
	}

	public ArrayNode getRicSlices() throws ExceptionBase, StreamReadException, DatabindException, IOException
	{
		final Set<Integer> ricSliceIds = getRicSliceIds();
		final ArrayNode ricSlices = JsonNodeFactory.instance.arrayNode();

		for (final Integer ricSliceId : ricSliceIds)
		{
			ricSlices.add(getRicSlice(ricSliceId));
		}
		return ricSlices;
	}

	public ArrayNode getRicSlicesOld() throws ExceptionBase
	{
		final ArrayNode ricSlices = (ArrayNode) super.doMethodGet("/v1/networkSliceInstances/ricSlices");
		for (final JsonNode tmpRicSlice : ricSlices)
		{
			final ObjectNode ricSlice = (ObjectNode) tmpRicSlice;
			ricSlice.remove("serviceProfileList");
			final String ricSliceId = ricSlice.path("networkSliceInstanceUid").asText();
			final JsonNode ricSliceInfo = super.doMethodPost("/v1/networkSliceInstances/" + ricSliceId + "/checkRicSlice", null);
			final JsonNode sliceProfile = ricSliceInfo.path("SliceContent").path("serviceProfileList").path(0);

			final ObjectNode sliceProfileT = ricSlice.putObject("sliceProfile");
			ricSlice.put("sst", sliceProfile.path("sNSSAIList").path(0).path("sst").asInt());
			ricSlice.put("sd", sliceProfile.path("sNSSAIList").path(0).path("sd").asText());
			sliceProfileT.put("maxNumberofUEs", sliceProfile.path("maxNumberofUEs").asText());
			sliceProfileT.put("latency", sliceProfile.path("latency").asText());
			final ObjectNode dLThptPerSlice = sliceProfileT.putObject("dLThptPerSlice");
			dLThptPerSlice.setAll((ObjectNode) sliceProfile.path("dLThptPerSlice"));
			dLThptPerSlice.remove("servAttrCom");
			final ObjectNode uLThptPerSlice = sliceProfileT.putObject("uLThptPerSlice");
			uLThptPerSlice.setAll((ObjectNode) sliceProfile.path("uLThptPerSlice"));
			uLThptPerSlice.remove("servAttrCom");
		}
		return ricSlices;
	}


	public JsonNode getCombineSlices() throws Exception{
		//fgc slice
		JsonNode fgcSlice = fgcService.doMethodGet("/v1/snssais");
		//smo slice
		ArrayNode smoSlice = this.getRicSlices();
		ArrayNode combinedSlicesList = objectMapper.createArrayNode();

		for (JsonNode fgcNode : fgcSlice) {
			String sstsd = "";
			if (fgcNode.path("sliceServiceType").asText().equals("eMBB") ||
					fgcNode.path("sliceServiceType").asText().equals("EMBB")) {
				sstsd = "1";
			} else if (fgcNode.path("sliceServiceType").asText().equals("uRLLC") ||
					fgcNode.path("sliceServiceType").asText().equals("URLLC")){
				sstsd = "2";
			}
			for (JsonNode smoNode : smoSlice) {
				if ((sstsd + fgcNode.path("sliceDifferentiator").asText()).equals(smoNode.path("ricSliceId").asText())){
					ObjectNode combinedSlices = objectMapper.createObjectNode();
					combinedSlices.setAll((ObjectNode) fgcNode);
					combinedSlices.setAll((ObjectNode) smoNode);
					combinedSlicesList.add(combinedSlices);
				}
			}
		}
		return combinedSlicesList;
	}
}