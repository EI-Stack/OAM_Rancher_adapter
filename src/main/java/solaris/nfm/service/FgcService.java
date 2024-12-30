package solaris.nfm.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.rest.RestServiceBase;
import solaris.nfm.capability.system.json.util.JsonNodeUtil;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.service.SliceService.PlmnId;
import solaris.nfm.service.SliceService.Snssai;
import solaris.nfm.service.SliceService.Tai;

@Service
@Slf4j
public class FgcService extends RestServiceBase
{
	public static final String	AuthMethod_5G_AKA			= "5G_AKA";
	public static final String	AuthMethod_EAP_AKA_PRIME	= "EAP_AKA_PRIME";
	public static final String	AuthMethod_EAP_TLS			= "EAP_TLS";

	@Autowired
	private ObjectMapper		objectMapper;

	public FgcService(final RestTemplateBuilder restTemplateBuilder, final SslBundles sslBundles, final RestTemplate restTemplate, @Value("${solaris.server.adapter.core.http.url}") String url,
			@Value("${solaris.server.adapter.core.ssl.enabled:false}") Boolean sslEnabled, @Value("${solaris.server.adapter.core.ssl.bundle:}") String sslBundleName,
			@Value("${solaris.server.adapter.core.http.token:}") String token)
	{
		super.initService(restTemplateBuilder, sslBundles, restTemplate, url, token, sslEnabled, sslBundleName);
	}

	@Override
	@PostConstruct
	public void init()
	{}

	public ArrayNode getSlices() throws ExceptionBase
	{
		return (ArrayNode) doMethodGet("/v1/snssais");
	}

	public ArrayNode getSliceNames() throws ExceptionBase
	{
		final ArrayNode slices = getSlices();
		final ArrayNode sliceNames = JsonNodeFactory.instance.arrayNode();
		for (final JsonNode slice : slices)
		{
			sliceNames.add(slice.get("name").asText());
		}
		return sliceNames;
	}

	public ArrayNode getServiceAreaLists() throws ExceptionBase
	{
		return (ArrayNode) doMethodGet("/v1/serviceAreaLists");
	}

	public ArrayNode getServiceAreaListNames() throws ExceptionBase
	{
		final ArrayNode serviceAreaLists = getServiceAreaLists();
		final ArrayNode serviceAreaListNames = JsonNodeFactory.instance.arrayNode();
		for (final JsonNode serviceAreaList : serviceAreaLists)
		{
			serviceAreaListNames.add(serviceAreaList.get("name").asText());
		}
		return serviceAreaListNames;
	}

	public ArrayNode getUpfs() throws ExceptionBase
	{
		return (ArrayNode) doMethodGet("/v1/nrfProfiles");
	}

	public ArrayNode packageUpf() throws ExceptionBase
	{
		final ArrayNode upfs = JsonNodeFactory.instance.arrayNode();

		final ArrayNode locationInfos = getLocationInfo();
		final ArrayNode originalAllDnns = (ArrayNode) doMethodGet("/v1/dnns");
		final ArrayNode originUpfs = getUpfs();
		final Map<String, Set<String>> crossAndSupiMap = createCrossAndSupiMapping();
		for (final JsonNode originUpf : originUpfs)
		{
			// final ObjectNode upf = upfs.addObject();
			// upf.put("name", originUpf.get("nfInstanceId").asText());
			//
			final List<String> upfSliceNames = new ArrayList<>();
			// final ArrayNode upfSliceNames = upf.putArray("sliceNames");
			final ArrayNode sNssaiUpfinfos = (ArrayNode) originUpf.path("upfInfo").path("sNssaiUpfinfo");
			for (final JsonNode sNssaiUpfinfo : sNssaiUpfinfos)
			{
				upfSliceNames.add(sNssaiUpfinfo.path("sliceName").asText());
			}
			final ArrayNode upfSalNames = (ArrayNode) originUpf.path("upfInfo").path("smfServingAreaList");

			for (final String upfSliceName : upfSliceNames)
			{
				for (final JsonNode upfSalNameNode : upfSalNames)
				{
					final String upfSalName = upfSalNameNode.asText();
					// final ArrayNode link = links.addArray();
					// link.add(upfSliceName).add(upfSalName.asText());
					//
					final ObjectNode upf2 = upfs.addObject();
					// upf2.put("name", originUpf.get("nfInstanceId").asText());

					upf2.put("sliceName", upfSliceName);
					upf2.put("serviceAreaListName", upfSalName);

					upf2.set("ues", getSupis(crossAndSupiMap, upfSliceName, upfSalName));
					upf2.set("dnns", getDnns(sNssaiUpfinfos, originalAllDnns, upfSliceName));

					final JsonNode locationInfo = JsonNodeUtil.findArrayMember(locationInfos, "name", upfSalName);
					upf2.set("serviceAreas", locationInfo.path("serviceAreas"));
					upf2.set("trackingAreas", locationInfo.path("trackingAreas"));
					upf2.set("cells", locationInfo.path("cells"));
					upf2.set("gnbs", locationInfo.path("gnbs"));

					final ArrayNode upfs2 = upf2.putArray("upfs");
					upfs2.add(originUpf.get("nfInstanceId").asText());
				}
			}

			// upf.remove("sliceNames");
			// upf.remove("serviceAreaListNames");
		}
		return upfs;
	}

	private ArrayNode getSupis(final Map<String, Set<String>> crossAndSupiMap, final String sliceName, final String salName)
	{
		final String crossKey = sliceName + "#C#" + salName;
		final Set<String> supiSet = crossAndSupiMap.get(crossKey);
		// log.debug("crossKey={}, supiSet={}", crossKey, this.objectMapper.valueToTree(supiSet).toPrettyString());
		final ArrayNode supis = (supiSet == null) ? JsonNodeFactory.instance.arrayNode() : this.objectMapper.valueToTree(supiSet);
		return supis;
	}

	private ArrayNode getDnns(final ArrayNode sNssaiUpfinfos, final ArrayNode originalAllDnns, final String sliceName)
	{
		final JsonNode sNssaiUpfinfo = JsonNodeUtil.findArrayMember(sNssaiUpfinfos, "sliceName", sliceName);
		final ArrayNode originalDnns = (ArrayNode) sNssaiUpfinfo.path("dnnUpfinfoList");
		final Set<String> dnnNameSet = new LinkedHashSet<>();
		for (final JsonNode originalDnn : originalDnns)
		{
			final String dnnName = originalDnn.path("dnn").asText();
			dnnNameSet.add(dnnName);
		}
		final ArrayNode dnns = JsonNodeFactory.instance.arrayNode();
		for (final String dnnName : dnnNameSet)
		{
			final JsonNode dnnInfo = JsonNodeUtil.findArrayMember(originalAllDnns, "name", dnnName);
			dnns.add(dnnInfo);
		}

		return dnns;
	}

	/**
	 * Create a mapping for (sliceName + "#C#" + salName) -> supi
	 */
	public Map<String, Set<String>> createCrossAndSupiMapping() throws ExceptionBase
	{
		final Map<String, Set<String>> crossAndSupiMap = new LinkedHashMap<>();
		final JsonNode imsiLists = doMethodGet("/v1/5gc/ue/subscriptions");
		for (final JsonNode imsi : imsiLists)
		{
			final String supi = imsi.path("supi").asText();
			final JsonNode map = doMethodGet("/v1/5gc/layout/ues/" + supi);
			final ArrayNode sliceNames = (ArrayNode) map.path("sliceNameList");
			final ArrayNode salNames = (ArrayNode) map.path("serviceAreaListNameList");

			for (final JsonNode sliceNameNode : sliceNames)
			{
				final String sliceName = sliceNameNode.asText(); // 將 JsonNode 轉換成字串
				for (final JsonNode salNameNode : salNames)
				{
					final String salName = salNameNode.asText();

					final String crossKey = sliceName + "#C#" + salName;
					Set<String> supiSet = crossAndSupiMap.get(crossKey);
					if (supiSet == null)
					{
						supiSet = new HashSet<String>();
					}
					supiSet.add(supi);
					crossAndSupiMap.put(crossKey, supiSet);
				}
			}
		}
		return crossAndSupiMap;
	}

	public ArrayNode getLocationInfo() throws ExceptionBase
	{
		final ArrayNode result = JsonNodeFactory.instance.arrayNode();

		final ArrayNode serviceAreaLists = (ArrayNode) doMethodGet("/v1/serviceAreaLists");
		// log.debug("serviceAreaLists=\n{}", serviceAreaLists);

		for (final JsonNode serviceAreaList : serviceAreaLists)
		{
			final ObjectNode rSal = JsonNodeFactory.instance.objectNode();
			rSal.put("name", serviceAreaList.path("name").asText());
			final ArrayNode rSas = rSal.putArray("serviceAreas");
			final ArrayNode rTas = rSal.putArray("trackingAreas");
			final ArrayNode rCells = rSal.putArray("cells");
			final ArrayNode rGnbs = rSal.putArray("gnbs");

			final ArrayNode serviceAreaNames = (ArrayNode) serviceAreaList.path("serviceArea");
			for (final JsonNode serviceAreaName : serviceAreaNames)
			{
				rSas.add(serviceAreaName.path("name").asText());
				final JsonNode serviceArea = doMethodGet("/v1/serviceAreas/" + serviceAreaName.path("name").asText());

				final ArrayNode taiListNames = (ArrayNode) serviceArea.path("taiList");

				for (final JsonNode taiListName : taiListNames)
				{
					final JsonNode taiList = doMethodGet("/v1/taiLists/" + taiListName.path("name").asText());

					final ArrayNode taiNames = (ArrayNode) taiList.path("tai");

					for (final JsonNode taiName : taiNames)
					{
						final String taiNameString = taiName.path("name").asText();
						rTas.add(taiName.path("name").asText());
						final JsonNode tai = doMethodGet("/v1/tais/" + taiNameString);
						rCells.addAll((ArrayNode) tai.path("cells"));
						rGnbs.addAll((ArrayNode) tai.path("gnbs"));
					}
				}
			}
			result.add(rSal);
		}

		log.debug("\t LocationInfo=\n{}", result.toPrettyString());

		return result;
	}

	public ObjectNode createUeProfileForAffirmed(final PlmnId plmnId, final String supi)
	{
		final AuthenticationSubscription authenticationSubscription = new AuthenticationSubscription(AuthMethod._5G_AKA);
		authenticationSubscription.setEncPermanentKey("000102030405060708090A0B0C0D0E0F");
		authenticationSubscription.setProtectionParameterId("dummy");
		authenticationSubscription.setAuthenticationManagementField("8000");
		authenticationSubscription.setAlgorithmId("MILENAGE_PEGATRON");
		authenticationSubscription.setEncOpcKey("000102030405060708090A0B0C0D0E0F");
		final SequenceNumber sequenceNumber = new SequenceNumber();
		sequenceNumber.setSqnScheme(SqnScheme.NON_TIME_BASED);
		sequenceNumber.setSqn("000000006d40");
		sequenceNumber.setLastIndexes(Map.of("ausf", 10));
		sequenceNumber.setIndLength(5);
		sequenceNumber.setDifSign(Sign.POSITIVE);
		authenticationSubscription.setSequenceNumber(sequenceNumber);

		// ------------------------------------------------------------------------------------------------------------
		final AccessAndMobilitySubscriptionData accessAndMobilitySubscriptionData = new AccessAndMobilitySubscriptionData();
		accessAndMobilitySubscriptionData.setSupportedFeatures("00000001");
		accessAndMobilitySubscriptionData.setGpsis(Set.of("msisdn-0000000001", "msisdn-0000000002"));
		accessAndMobilitySubscriptionData.setInternalGroupIds(Set.of("0001777888123450020", "0001777888123451021"));
		accessAndMobilitySubscriptionData.setSubscribedUeAmbr(new Ambr("4200 Mbps", "4200 Mbps"));
		final Nssai nssai = new Nssai();
		nssai.setDefaultSingleNssais(Set.of(new Snssai(1, "000002")));
		nssai.setSingleNssais(Set.of(new Snssai(1, "000002"), new Snssai(1, "000001")));
		accessAndMobilitySubscriptionData.setNssai(nssai);
		accessAndMobilitySubscriptionData.setRatRestrictions(Set.of());
		accessAndMobilitySubscriptionData.setForbiddenAreas(Set.of());
		final ServiceAreaRestriction serviceAreaRestriction = new ServiceAreaRestriction();
		serviceAreaRestriction.setAreas(Set.of());
		accessAndMobilitySubscriptionData.setServiceAreaRestriction(serviceAreaRestriction);
		accessAndMobilitySubscriptionData.setCoreNetworkTypeRestrictions(Set.of());
		accessAndMobilitySubscriptionData.setRfspIndex(200);
		accessAndMobilitySubscriptionData.setSubsRegTimer(50);
		accessAndMobilitySubscriptionData.setUeUsageType(5);
		accessAndMobilitySubscriptionData.setMpsPriority(false);
		accessAndMobilitySubscriptionData.setMcsPriority(false);
		accessAndMobilitySubscriptionData.setActiveTime(3);
		final SorInfo sorInfo = new SorInfo(true, "1970-01-01T00:00:00Z");
		sorInfo.setSorMacIausf("0ABC0ABC0ABC0ABC0ABC0ABC0ABC0ABC");
		sorInfo.setCountersor("ABCD");
		accessAndMobilitySubscriptionData.setSorInfo(sorInfo);
		accessAndMobilitySubscriptionData.setMicoAllowed(false);
		accessAndMobilitySubscriptionData.setOdbPacketServices(OdbPacketServices.ALL_PACKET_SERVICES);
		accessAndMobilitySubscriptionData.setSubscribedDnnList(Set.of("pegapn2-ld3", "pegapn-ld3", "dstest.net"));

		// ------------------------------------------------------------------------------------------------------------
		final SmfSelectionSubscriptionData smfSelectionSubscriptionData = new SmfSelectionSubscriptionData();
		smfSelectionSubscriptionData.setSupportedFeatures("31345DCDC");
		Set<DnnInfo> dnnInfos = new LinkedHashSet<>();
		DnnInfo dnnInfo = new DnnInfo("pegapn2-ld3");
		dnnInfo.setDefaultDnnIndicator(true);
		dnnInfo.setLboRoamingAllowed(true);
		dnnInfo.setIwkEpsInd(false);
		dnnInfos.add(dnnInfo);
		dnnInfo = new DnnInfo("default");
		dnnInfo.setDefaultDnnIndicator(true);
		dnnInfo.setLboRoamingAllowed(true);
		dnnInfo.setIwkEpsInd(false);
		dnnInfos.add(dnnInfo);
		SnssaiInfo snssaiInfo = new SnssaiInfo();
		snssaiInfo.setDnnInfos(dnnInfos);
		final Map<String, SnssaiInfo> subscribedSnssaiInfos = new LinkedHashMap<>();
		subscribedSnssaiInfos.put("1", snssaiInfo);

		dnnInfos = new LinkedHashSet<>();
		dnnInfo = new DnnInfo("pegapn-ld3");
		dnnInfo.setDefaultDnnIndicator(true);
		dnnInfo.setLboRoamingAllowed(true);
		dnnInfo.setIwkEpsInd(false);
		dnnInfos.add(dnnInfo);
		snssaiInfo = new SnssaiInfo();
		snssaiInfo.setDnnInfos(dnnInfos);
		subscribedSnssaiInfos.put("1-000001", snssaiInfo);

		dnnInfos = new LinkedHashSet<>();
		dnnInfo = new DnnInfo("pegapn2-ld3");
		dnnInfo.setDefaultDnnIndicator(true);
		dnnInfo.setLboRoamingAllowed(true);
		dnnInfo.setIwkEpsInd(false);
		dnnInfos.add(dnnInfo);
		snssaiInfo = new SnssaiInfo();
		snssaiInfo.setDnnInfos(dnnInfos);
		subscribedSnssaiInfos.put("1-000002", snssaiInfo);

		smfSelectionSubscriptionData.setSubscribedSnssaiInfos(subscribedSnssaiInfos);

		// ------------------------------------------------------------------------------------------------------------
		// final SessionManagementSubscriptionData sessionManagementSubscriptionData = new SessionManagementSubscriptionData();

		// ------------------------------------------------------------------------------------------------------------
		final SmsManagementSubscriptionData smsManagementSubscriptionData = new SmsManagementSubscriptionData();
		smsManagementSubscriptionData.setSupportedFeatures("31345DCDC");

		// ------------------------------------------------------------------------------------------------------------
		final SmsSubscriptionData smsSubscriptionData = new SmsSubscriptionData();
		smsSubscriptionData.setSmsSubscribed(false);

		// ------------------------------------------------------------------------------------------------------------
		final TraceData traceData = new TraceData("55522-AAAAAA", TraceDepth.MINIMUM_WO_VENDOR_EXTENSION, "abc123", "def456");

		// ------------------------------------------------------------------------------------------------------------
		final OdbData odbData = new OdbData();
		odbData.setRoamingOdb(RoamingOdb.OUTSIDE_HOME_PLMN);

		// ------------------------------------------------------------------------------------------------------------
		final Map<String, OperatorSpecificDataContainer> operatorSpecificDataContainers = Map.of("data1", new OperatorSpecificDataContainer(DataType.string, "elem1"), "data2",
				new OperatorSpecificDataContainer(DataType.integer, 3254), "data3", new OperatorSpecificDataContainer(DataType.number, 3.1415), "data4",
				new OperatorSpecificDataContainer(DataType._boolean, true));

		// ------------------------------------------------------------------------------------------------------------
		final EeProfileData eeProfileData = new EeProfileData();
		eeProfileData.setRestrictedEventTypes(Set.of(EventType.LOSS_OF_CONNECTIVITY, EventType.UE_REACHABILITY_FOR_DATA, EventType.UE_REACHABILITY_FOR_SMS, EventType.LOCATION_REPORTING,
				EventType.CHANGE_OF_SUPI_PEI_ASSOCIATION, EventType.ROAMING_STATUS, EventType.COMMUNICATION_FAILURE, EventType.AVAILABILITY_AFTER_DDN_FAILURE));
		eeProfileData.setSupportedFeatures("31345DCDC");
		// ------------------------------------------------------------------------------------------------------------
		final ObjectNode json = JsonNodeFactory.instance.objectNode();
		// json.set("dnnConfiguration", objectMapper.valueToTree(createDnnConfigurationForAffirmed()));
		json.set("plmnId", objectMapper.valueToTree(plmnId));
		json.put("supi", supi);
		json.set("authenticationSubscription", objectMapper.valueToTree(authenticationSubscription));

		final ObjectNode profile = json.putObject("profile");
		final ObjectNode subscriptionDataSets = profile.putObject("subscriptionDataSets");
		subscriptionDataSets.set("amData", objectMapper.valueToTree(accessAndMobilitySubscriptionData));
		subscriptionDataSets.set("smfSelData", objectMapper.valueToTree(smfSelectionSubscriptionData));
		subscriptionDataSets.set("smData", objectMapper.valueToTree(Set.of(createSessionManagementSubscriptionDataForAffirmed(1, "000001", "pegapn-ld3", Set.of("172.17.0.1")),
				createSessionManagementSubscriptionDataForAffirmed(1, "000002", "pegapn2-ld3", Set.of("172.17.0.1")))));
		subscriptionDataSets.set("smsMngData", objectMapper.valueToTree(smsManagementSubscriptionData));
		subscriptionDataSets.set("smsSubsData", objectMapper.valueToTree(smsSubscriptionData));
		subscriptionDataSets.set("traceData", objectMapper.valueToTree(traceData));
		// json.set("odbData", objectMapper.valueToTree(odbData));
		profile.set("operatorSpecificData", objectMapper.valueToTree(operatorSpecificDataContainers));
		// json.set("eeProfileData", objectMapper.valueToTree(eeProfileData));

		log.debug("json=\n{}", json.toPrettyString());

		return json;
	}

	public DnnConfiguration createDnnConfigurationForAffirmed()
	{
		final PduSessionTypes pduSessionTypes = new PduSessionTypes(PduSessionType.IPV4);
		pduSessionTypes.setAllowedSessionTypes(Set.of(PduSessionType.IPV4V6, PduSessionType.IPV4, PduSessionType.ETHERNET));
		final SscModes sscModes = new SscModes(SscMode.SSC_MODE_3);
		sscModes.setAllowedSscModes(new LinkedHashSet<>(List.of(SscMode.SSC_MODE_2, SscMode.SSC_MODE_3)));
		final DnnConfiguration dnnConfiguration = new DnnConfiguration(pduSessionTypes, sscModes);

		dnnConfiguration.setIwkEpsInd(false);
		dnnConfiguration.setSessionAmbr(new Ambr("4200 Mbps", "4200 Mbps"));
		final UpSecurity upSecurity = new UpSecurity();
		upSecurity.setUpIntegr("REQUIRED");
		upSecurity.setUpConfid("NOT_NEEDED");
		dnnConfiguration.setUpSecurity(upSecurity);

		final Arp arp = new Arp(5, PreemptionCapability.MAY_PREEMPT, PreemptionVulnerability.NOT_PREEMPTABLE);
		final SubscribedDefaultQos fivegQosProfile = new SubscribedDefaultQos(2, arp);
		fivegQosProfile.setPriorityLevel(3);
		dnnConfiguration.setFivegQosProfile(fivegQosProfile);

		dnnConfiguration.setTgppChargingCharacteristics("3gppCharac");

		return dnnConfiguration;
	}

	public SessionManagementSubscriptionData createSessionManagementSubscriptionDataForAffirmed(final Integer sst, final String sd, final String dnnConfigurationKey, final Set<String> StaticIpAddress)
	{
		final SessionManagementSubscriptionData sessionManagementSubscriptionData = new SessionManagementSubscriptionData(new Snssai(sst, sd));
		final Map<String, DnnConfiguration> dnnConfigurations = new LinkedHashMap<>();

		final PduSessionTypes pduSessionTypes = new PduSessionTypes(PduSessionType.IPV4);
		// pduSessionTypes.setAllowedSessionTypes(Set.of(PduSessionType.IPV4));
		final SscModes sscModes = new SscModes(SscMode.SSC_MODE_1);
		// sscModes.setAllowedSscModes(new LinkedHashSet<>(List.of(SscMode.SSC_MODE_2, SscMode.SSC_MODE_3)));

		final DnnConfiguration dnnConfiguration = new DnnConfiguration(pduSessionTypes, sscModes);
		dnnConfiguration.setStaticIpAddress(StaticIpAddress);
		dnnConfigurations.put(dnnConfigurationKey, dnnConfiguration);

		sessionManagementSubscriptionData.setDnnConfigurations(dnnConfigurations);

		return sessionManagementSubscriptionData;
	}

	public ObjectNode createTest()
	{
		final AuthenticationSubscription authenticationSubscription = new AuthenticationSubscription(AuthMethod._5G_AKA);

		final ObjectNode json = JsonNodeFactory.instance.objectNode();
		json.set("AuthenticationSubscription", objectMapper.valueToTree(authenticationSubscription));
		log.debug("json=\n{}", json.toPrettyString());

		return json;
	}

	public ObjectNode createUeSubscription(final String imsi, final String k, final String op)
	{
		final AuthenticationSubscription authenticationSubscription = new AuthenticationSubscription(AuthMethod._5G_AKA);
		authenticationSubscription.setAuthenticationManagementField("8000");
		authenticationSubscription.setEncPermanentKey(k);
		authenticationSubscription.setEncOpcKey(op);
		final SequenceNumber sequenceNumber = new SequenceNumber();
		sequenceNumber.setSqn("16f3b3f70fc2");
		sequenceNumber.setSqnScheme(SqnScheme.NON_TIME_BASED);
		authenticationSubscription.setSequenceNumber(sequenceNumber);
		// log.debug("json=\n{}", objectMapper.valueToTree(authenticationSubscription).toPrettyString());
		// -----------------------------------------------------------------------------------------------------------------------

		final AccessAndMobilitySubscriptionData accessAndMobilitySubscriptionData = new AccessAndMobilitySubscriptionData();
		final Set<String> gpsis = new LinkedHashSet<>();
		gpsis.add("msisdn-0900000000");
		accessAndMobilitySubscriptionData.setGpsis(gpsis);
		final Nssai nssai = new Nssai();
		final Set<Snssai> defaultSingleNssais = new LinkedHashSet<>();
		defaultSingleNssais.add(new Snssai(1, "010203", true));
		defaultSingleNssais.add(new Snssai(1, "112233", true));
		nssai.setDefaultSingleNssais(defaultSingleNssais);
		nssai.setSingleNssais(new LinkedHashSet<Snssai>());
		accessAndMobilitySubscriptionData.setNssai(nssai);
		accessAndMobilitySubscriptionData.setSubscribedUeAmbr(new Ambr("1 Gbps", "2 Gbps"));

		// -----------------------------------------------------------------------------------------------------------------------

		final SmfSelectionSubscriptionData smfSelectionSubscriptionData = new SmfSelectionSubscriptionData();
		Set<DnnInfo> dnnInfos = new LinkedHashSet<>();
		dnnInfos.add(new DnnInfo("internet"));
		dnnInfos.add(new DnnInfo("internet2"));
		SnssaiInfo snssaiInfo = new SnssaiInfo();
		snssaiInfo.setDnnInfos(dnnInfos);
		final Map<String, SnssaiInfo> subscribedSnssaiInfos = new LinkedHashMap<>();
		subscribedSnssaiInfos.put("01010203", snssaiInfo);
		smfSelectionSubscriptionData.setSubscribedSnssaiInfos(subscribedSnssaiInfos);

		dnnInfos = new LinkedHashSet<>();
		dnnInfos.add(new DnnInfo("internet"));
		dnnInfos.add(new DnnInfo("internet2"));
		snssaiInfo = new SnssaiInfo();
		snssaiInfo.setDnnInfos(dnnInfos);
		subscribedSnssaiInfos.put("01112233", snssaiInfo);
		smfSelectionSubscriptionData.setSubscribedSnssaiInfos(subscribedSnssaiInfos);

		// -----------------------------------------------------------------------------------------------------------------------

		final AmPolicyData amPolicyData = new AmPolicyData(null, Set.of("saviah"));

		// -----------------------------------------------------------------------------------------------------------------------
		final Map<String, SmPolicySnssaiData> SmPolicySnssaiDataMap = new HashMap<>();
		final SmPolicyData smPolicyData = new SmPolicyData(SmPolicySnssaiDataMap);
		final Map<String, SmPolicySnssaiData> smPolicySnssaiDataMap = new LinkedHashMap<>();

		final Map<String, SmPolicyDnnData> smPolicyDnnDataMap = new LinkedHashMap<>();
		smPolicyDnnDataMap.put("internet", new SmPolicyDnnData("internet"));
		smPolicyDnnDataMap.put("internet2", new SmPolicyDnnData("internet2"));

		Snssai snssai = new Snssai(1);
		snssai.setSd("010203");
		SmPolicySnssaiData smPolicySnssaiData = new SmPolicySnssaiData(snssai, smPolicyDnnDataMap);
		smPolicySnssaiDataMap.put("01010203", smPolicySnssaiData);

		snssai = new Snssai(1);
		snssai.setSd("112233");
		smPolicySnssaiData = new SmPolicySnssaiData(snssai, smPolicyDnnDataMap);
		smPolicySnssaiDataMap.put("01112233", smPolicySnssaiData);

		smPolicyData.setSmPolicySnssaiData(smPolicySnssaiDataMap);

		final ObjectNode json = JsonNodeFactory.instance.objectNode();
		json.put("plmnID", "20893").put("ueId", imsi);
		json.set("AuthenticationSubscription", objectMapper.valueToTree(authenticationSubscription));
		json.set("AccessAndMobilitySubscriptionData", objectMapper.valueToTree(accessAndMobilitySubscriptionData));
		json.putArray("SessionManagementSubscriptionData").addPOJO(createSessionManagementSubscriptionData(1, "010203", "100 Mbps", "200 Mbps"))
		.addPOJO(createSessionManagementSubscriptionData(1, "112233", "100 Mbps", "200 Mbps"));

		json.set("SmfSelectionSubscriptionData", objectMapper.valueToTree(smfSelectionSubscriptionData));
		json.set("AmPolicyData", objectMapper.valueToTree(amPolicyData));
		json.set("SmPolicyData", objectMapper.valueToTree(smPolicyData));

		json.putArray("FlowRules");
		json.putArray("QosFlows");
		// log.debug("json=\n{}", json.toPrettyString());

		return json;
	}

	public SessionManagementSubscriptionData createSessionManagementSubscriptionData(final Integer sst, final String sd, final String downlink, final String uplink)
	{
		final Snssai snssai = new Snssai(sst);
		snssai.setSd(sd);
		final SessionManagementSubscriptionData sessionManagementSubscriptionData = new SessionManagementSubscriptionData(snssai);
		final Map<String, DnnConfiguration> dnnConfigurations = new LinkedHashMap<>();

		final PduSessionTypes pduSessionTypes = new PduSessionTypes(PduSessionType.IPV4);
		pduSessionTypes.setAllowedSessionTypes(Set.of(PduSessionType.IPV4));
		final SscModes sscModes = new SscModes(SscMode.SSC_MODE_1);
		sscModes.setAllowedSscModes(new LinkedHashSet<>(List.of(SscMode.SSC_MODE_2, SscMode.SSC_MODE_3)));
		final DnnConfiguration dnnConfiguration = new DnnConfiguration(pduSessionTypes, sscModes);
		dnnConfiguration.setSessionAmbr(new Ambr(uplink, downlink));
		final Arp arp = new Arp(5, PreemptionCapability.MAY_PREEMPT, PreemptionVulnerability.NOT_PREEMPTABLE);
		final SubscribedDefaultQos fivegQosProfile = new SubscribedDefaultQos(9, arp);
		fivegQosProfile.setPriorityLevel(8);
		dnnConfiguration.setFivegQosProfile(fivegQosProfile);
		dnnConfigurations.put("internet", dnnConfiguration);
		dnnConfigurations.put("internet2", createDnnConfiguration());

		sessionManagementSubscriptionData.setDnnConfigurations(dnnConfigurations);

		return sessionManagementSubscriptionData;
	}

	public DnnConfiguration createDnnConfiguration()
	{
		final PduSessionTypes pduSessionTypes = new PduSessionTypes(PduSessionType.IPV4);
		pduSessionTypes.setAllowedSessionTypes(Set.of(PduSessionType.IPV4));
		final SscModes sscModes = new SscModes(SscMode.SSC_MODE_1);
		sscModes.setAllowedSscModes(new LinkedHashSet<>(List.of(SscMode.SSC_MODE_2, SscMode.SSC_MODE_3)));
		final DnnConfiguration dnnConfiguration = new DnnConfiguration(pduSessionTypes, sscModes);
		dnnConfiguration.setSessionAmbr(new Ambr("200 Mbps", "100 Mbps"));
		final Arp arp = new Arp(5, PreemptionCapability.MAY_PREEMPT, PreemptionVulnerability.NOT_PREEMPTABLE);
		final SubscribedDefaultQos fivegQosProfile = new SubscribedDefaultQos(9, arp);
		fivegQosProfile.setPriorityLevel(8);
		dnnConfiguration.setFivegQosProfile(fivegQosProfile);

		return dnnConfiguration;
	}
	
	public ArrayNode getUpfsInfo() throws ExceptionBase {
		final ArrayNode associatedUpfs = (ArrayNode) doMethodGet("/v1/5gc/associatedUpfs");
		final ArrayNode registeredUpfs = (ArrayNode) doMethodGet("/v1/5gc/registeredUpfs");
		ArrayNode upfs = objectMapper.createArrayNode();
		HashSet<String> associatedUpfIdMap = new HashSet<>();
		if(associatedUpfs != null) {
			for(JsonNode assUpf : associatedUpfs) {
				associatedUpfIdMap.add(assUpf.get("NodeID").asText());
			}
		}
		if(registeredUpfs != null) {
			for(JsonNode regUpf : registeredUpfs) {
				String regUpfId = regUpf.get("NodeID").asText();
				if(associatedUpfIdMap.contains(regUpfId)) {
					upfs.add(((ObjectNode)regUpf).put("status", "Yes"));
				}else {
					upfs.add(((ObjectNode)regUpf).put("status", "No"));
				}
			}
		}
		return upfs;
	}
	
	@Cacheable(value = "SaviahCache", key = "'subscriberList'")
	public JsonNode getSubscriptionList() throws ExceptionBase
	{
		return doMethodGet("/v2/5gc/ue/subscriptions");
	}

	@Cacheable(value = "SaviahCache", key = "'ueList'")
	public JsonNode getRegisteredUeList() throws Exception
	{
		return doMethodGet("/v1/5gc/ue/registeredUeList");
	}

	@CacheEvict(value = "SaviahCache", allEntries = true)
	public void evictAllCache()
	{
		// This method will remove all entries from the cache
		// associated with the "yourCacheName" cache.
		log.info("Clear all cache.");
	}

	@Data
	@RequiredArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class AuthenticationSubscription  // TS29505
	{
		@NonNull
		private AuthMethod		authenticationMethod;          // TS29505 AuthMethod.enum
		private String			encPermanentKey;
		private String			protectionParameterId;
		private SequenceNumber	sequenceNumber;                // TS29505 SequenceNumber
		private String			authenticationManagementField; // String.pattern: '^[A-Fa-f0-9]{4}$'
		private String			algorithmId;
		private String			encOpcKey;					   // OPC
		private String			encTopcKey;					   // OP
		private Boolean			vectorGenerationInHss;	       // default=false
		private AuthMethod		n5gcAuthMethod;                // TS29505 AuthMethod
		private Boolean			rgAuthenticationInd;	       // default=false
		private String			supi;                          // TS29571 Supi=String.pattern: '^(imsi-[0-9]{5,15}|nai-.+|gci-.+|gli-.+|.+)$'
	}

	public enum AuthMethod
	{
		_5G_AKA,
		EAP_AKA_PRIME,
		EAP_TLS;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SequenceNumber  // TS29505
	{
		private SqnScheme				sqnScheme;    // enum:[GENERAL, NON_TIME_BASED, TIME_BASED] or any string
		private String					sqn;          // pattern: '^[A-Fa-f0-9]{12}$'
		private Map<String, Integer>	lastIndexes;  // minimum: 0
		private Integer					indLength;    // minimum: 0
		private Sign					difSign;
	}

	public enum SqnScheme
	{
		GENERAL,
		NON_TIME_BASED,
		TIME_BASED;
	}

	public enum Sign
	{
		POSITIVE,
		NEGATIVE
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class AccessAndMobilitySubscriptionData  // TS29503
	{
		private String							supportedFeatures;               // TS29571 SupportedFeatures=String.pattern: '^[A-Fa-f0-9]*$'
		private Set<String>						gpsis;                           // TS29571 Gpsi.pattern: '^(msisdn-[0-9]{5,15}|extid-[^@]+@[^@]+|.+)$'
		private Set<String>						internalGroupIds;                // TS29571 GroupId: pattern: '^[A-Fa-f0-9]{8}-[0-9]{3}-[0-9]{2,3}-([A-Fa-f0-9][A-Fa-f0-9]){1,10}$'
		private Map<String, String>				sharedVnGroupDataIds;            // TS29503 SharedDataId.pattern: '^[0-9]{5,6}-.+$'
		private Ambr							subscribedUeAmbr;	             // TS29571 AmbrRm
		private Nssai							nssai;				             // TS29503 Nssai
		private Set<RatType>					ratRestrictions;                 // TS29571 RatType
		private Set<Area>						forbiddenAreas;                  // TS29571 Area
		private ServiceAreaRestriction			serviceAreaRestriction;          // TS29571 ServiceAreaRestriction
		private Set<String>						coreNetworkTypeRestrictions;     // TS29571 CoreNetworkType=String.enum
		private Integer							rfspIndex;                       // TS29571 RfspIndexRm=Integer, 1~256 or null
		private Integer							subsRegTimer;                    // TS29571 DurationSecRm=Integer, null
		private Integer							ueUsageType;                     // TS29503 UeUsageType=Integer
		private Boolean							mpsPriority;                     // TS29503 MpsPriorityIndicator=Boolean
		private Boolean							mcsPriority;                     // TS29503 McsPriorityIndicator=Boolean
		private Integer							activeTime;                      // TS29571 DurationSecRm=Integer, null
		private SorInfo							sorInfo;                         // TS29503 SorInfo
		private Boolean							sorInfoExpectInd;
		private Boolean							sorafRetrieval;                  // default=false
		private Set<SorUpdateIndicator>			sorUpdateIndicatorList;          // TS29503 SorUpdateIndicator=String.enum, Array.minItems: 1
		private UpuInfo							upuInfo;                         // TS29503 UpuInfo
		private Boolean							micoAllowed;                     // TS29503 MicoAllowed=Boolean
		private Set<String>						sharedAmDataIds;                 // TS29503 SharedDataId=String.pattern: '^[0-9]{5,6}-.+$', Array.minItems: 1
		private OdbPacketServices				odbPacketServices;               // TS29571 OdbPacketServices=String.enum
		private Set<String>						subscribedDnnList;               // TS29571 Dnn=String or TS29571 WildcardDnn=String.pattern: '^[*]$', Array
		private Integer							serviceGapTime;                  // TS29571 DurationSec=Integer
		private MdtUserConsent					mdtUserConsent;                  // TS29503 MdtUserConsent=String.enum
		private MdtConfiguration				mdtConfiguration;                // TS29571 MdtConfiguration
		private TraceData						traceData;                       // TS29571 TraceData
		private CagData							cagData;                         // TS29503 CagData
		private String							stnSr;                           // TS29571 StnSr=String
		private String							cMsisdn;                         // TS29571 CMsisdn=String.pattern: '^[0-9]{5,15}$'
		private Integer							nbIoTUePriority;                 // TS29503 NbIoTUePriority=Integer 0~255
		private Boolean							nssaiInclusionAllowed;           // default=false
		private String							rgWirelineCharacteristics;       // TS29571 RgWirelineCharacteristics=TS29571 Bytes=String.format: byte
		private EcRestrictionDataWb				ecRestrictionDataWb;             // TS29503 EcRestrictionDataWb
		private Boolean							ecRestrictionDataNb;             // default=false
		private ExpectedUeBehaviourData			expectedUeBehaviourList;         // TS29503 ExpectedUeBehaviourData
		private Set<RatType>					primaryRatRestrictions;          // TS29571 RatType, Array
		private Set<RatType>					secondaryRatRestrictions;        // TS29571 RatType, Array
		private Set<EdrxParameters>				edrxParametersList;              // TS29503 EdrxParameters, Array.minItems: 1
		private Set<PtwParameters>				ptwParametersList;               // TS29503 PtwParameters, Array.minItems: 1
		private Boolean							iabOperationAllowed;             // default=false
		private Set<WirelineArea>				wirelineForbiddenAreas;          // TS29503 WirelineArea
		private WirelineServiceAreaRestriction	wirelineServiceAreaRestriction;  // TS29571 WirelineServiceAreaRestriction
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class WirelineServiceAreaRestriction  // TS29571
	{
		private RestrictionType		restrictionType;  // TS29571 RestrictionType
		private Set<WirelineArea>	areas;        // TS29571 WirelineArea, Array
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class WirelineArea  // TS29571
	{
		private Set<String>	globalLineIds;  // TS29571 Gli=TS29571 Bytes=String.format: byte, Array.minItems: 1
		private Set<String>	hfcNIds;        // TS29571 HfcNId=String.length=6, Array.minItems: 1
		private String		areaCodeB;      // TS29571 AreaCode=String
		private String		areaCodeC;      // TS29571 AreaCode=String
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class PtwParameters  // TS29503
	{
		@NonNull
		private OperationMode	operationMode;  // TS29503 OperationMode
		@NonNull
		private String			ptwValue;       // String.pattern: '^([0-1]{4})$'
	}

	public enum OperationMode  // TS29503
	{
		WB_S1,
		NB_S1,
		WB_N1,
		NB_N1
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class EdrxParameters  // TS29503
	{
		@NonNull
		private RatType	ratType;    // TS29571 RatType
		@NonNull
		private String	edrxValue;  // String.pattern: '^([0-1]{4})$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class EcRestrictionDataWb  // TS29503 ecModeARestricted 與 ecModeBRestricted 至少有一個有值
	{
		private Boolean	ecModeARestricted;
		private Boolean	ecModeBRestricted;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class CagData  // TS29503
	{
		@NonNull
		// A map (list of key-value pairs where PlmnId serves as key) of CagInfo
		private Map<String, CagInfo>	cagInfos;          // TS29503 CagInfo
		private String					provisioningTime;  // TS29571 DateTime=String
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class CagInfo  // TS29503
	{
		@NonNull
		private Set<String>	allowedCagList;    // TS29571 CagId=String.pattern: '^[A-Fa-f0-9]{8}$'
		private Boolean		cagOnlyIndicator;
	}

	@Data
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Ambr  // TS29571
	{
		private String	uplink;    // TS29571 BitRate=String.pattern: '^\d+(\.\d+)? (bps|Kbps|Mbps|Gbps|Tbps)$'
		private String	downlink;  // TS29571 BitRate=String.pattern: '^\d+(\.\d+)? (bps|Kbps|Mbps|Gbps|Tbps)$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Nssai  // TS29503
	{
		@NonNull
		private Set<Snssai>							defaultSingleNssais	= new LinkedHashSet<>();  // minItems: 1
		private Set<Snssai>							singleNssais;          // Array.minItems: 1
		private String								supportedFeatures;     // TS29571 SupportedFeatures=String.pattern: '^[A-Fa-f0-9]*$'
		private String								provisioningTime;      // TS29571 DateTime=String
		private Map<String, AdditionalSnssaiData>	additionalSnssaiData;  // TS29503 AdditionalSnssaiData, Map.minProperties: 1
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class AdditionalSnssaiData  // TS29503
	{
		private Boolean requiredAuthnAuthz;
	}

	public enum RatType  // TS29571
	{
		NR,
		EUTRA,
		WLAN,
		VIRTUAL,
		NBIOT,
		WIRELINE,
		WIRELINE_CABLE,
		WIRELINE_BBF,
		LTE_M,
		NR_U,
		EUTRA_U,
		TRUSTED_N3GA,
		TRUSTED_WLAN,
		UTRA,
		GERA;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceAll("_", "");
		}
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Area  // TS29571 限制：tacs 與 areaCode 兩者最少有一個不為空值
	{
		private Set<String>	tacs;      // TS29571 Tac=String.pattern: '(^[A-Fa-f0-9]{4}$)|(^[A-Fa-f0-9]{6}$)', Array.minItems: 1
		private String		areaCode;  // TS29571 AreaCode=String
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ServiceAreaRestriction  // TS29571
	{
		private String		restrictionType;                // TS29571 RestrictionType=String.enum
		private Set<Area>	areas;                          // TS29571 Area
		private Integer		maxNumOfTAs;                    // TS29571 Uinteger=Integer, minimum: 0
		private Integer		maxNumOfTAsForNotAllowedAreas;  // TS29571 Uinteger=Integer, minimum: 0
	}

	public enum RestrictionType  // TS29571
	{
		ALLOWED_AREAS,
		NOT_ALLOWED_AREAS;
	}

	public enum CoreNetworkType  // TS29571
	{
		_5GC,
		EPC;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	public enum SorUpdateIndicator  // TS29503
	{
		INITIAL_REGISTRATION,
		EMERGENCY_REGISTRATION;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SorInfo  // TS29503
	{
		private Set<SteeringInfo>	steeringContainer; // TS29503 SteeringContainer (TS29509 SteeringInfo.Array.minItems: 1 or TS29503 SecuredPacket=String.format=byte)
		@NonNull
		private Boolean				ackInd;            // TS29509 AckInd=Boolean
		private String				sorMacIausf;       // TS29509 SorMac=String.pattern: '^[A-Fa-f0-9]{32}$'
		private String				countersor;        // TS29509 CounterSor=String.pattern: '^[A-Fa-f0-9]{4}$'
		@NonNull
		private String				provisioningTime;  // TS29571 DateTime=String
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SteeringInfo  // TS29509
	{
		@NonNull
		private PlmnId			plmnId;          // TS29571 PlmnId
		private Set<AccessTech>	accessTechList;  // TS29509 AccessTech=String.enum, minItems: 1
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class UpuInfo  // TS29503
	{
		@NonNull
		private Set<UpuData>	upuDataList;       // TS29509 UpuData, Array.minItems: 1
		@NonNull
		private Boolean			upuRegInd;         // TS29503 UpuRegInd=Boolean
		@NonNull
		private Boolean			upuAckInd;         // TS29509 UpuAckInd=Boolean
		private String			upuMacIausf;       // TS29509 UpuMac=String.pattern: '^[A-Fa-f0-9]{32}$'
		private String			counterUpu;        // TS29509 CounterUpu=String.pattern: '^[A-Fa-f0-9]{4}$'
		@NonNull
		private String			provisioningTime;  // TS29571 DataTime=String
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class UpuData  // TS29509
	{
		private String		secPacket;        // TS29503 SecuredPacket=String.format=byte
		private Set<Snssai>	defaultConfNssai; // TS29571 Snssai, Array.minItems: 1
		private String		routingId;        // TS29544 RoutingId=String.pattern: '^[0-9]{1,4}$'
	}

	public enum AccessTech  // TS29509
	{
		NR,
		EUTRAN_IN_WBS1_MODE_AND_NBS1_MODE,
		EUTRAN_IN_NBS1_MODE_ONLY,
		EUTRAN_IN_WBS1_MODE_ONLY,
		UTRAN,
		GSM_AND_ECGSM_IoT,
		GSM_WITHOUT_ECGSM_IoT,
		ECGSM_IoT_ONLY,
		CDMA_1xRTT,
		CDMA_HRPD,
		GSM_COMPACT;
	}

	public enum OdbPacketServices  // TS29571
	{
		ALL_PACKET_SERVICES,
		ROAMER_ACCESS_HPLMN_AP,
		ROAMER_ACCESS_VPLMN_AP;
	}

	public enum MdtUserConsent  // TS29503
	{
		CONSENT_NOT_GIVEN,
		CONSENT_GIVEN;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class MdtConfiguration  // TS29571
	{
		@NonNull
		private JobType						jobType;                 // TS29571 JobType=String.enum
		private ReportTypeMdt				reportType;              // TS29571 ReportTypeMdt.Strng.eunm
		private AreaScope					areaScope;               // TS29571 AreaScope
		private Set<MeasurementLteForMdt>	measurementLteList;      // TS29571 MeasurementLteForMdt, Array
		private Set<MeasurementNrForMdt>	measurementNrList;       // TS29571 MeasurementNrForMdt, Array.minItems: 1
		private Set<SensorMeasurement>		sensorMeasurementList;   // TS29571 SensorMeasurement, Array.minItems: 1
		private Set<ReportingTrigger>		reportingTriggerList;    // TS29571 ReportingTrigger, Array.minItems: 1
		private ReportIntervalMdt			reportInterval;          // TS2571 ReportIntervalMdt.Strng.eunm
		private ReportIntervalNrMdt			reportIntervalNr;        // TS2571 ReportIntervalNrMdt.Strng.eunm
		private ReportAmountMdt				reportAmount;            // TS29571 ReportAmountMdt.Strng.eunm
		private Integer						eventThresholdRsrp;      // Integer.0~97
		private Integer						eventThresholdRsrpNr;    // Integer.0~127
		private Integer						eventThresholdRsrq;      // Integer.0~34
		private Integer						eventThresholdRsrqNr;    // Integer.0~127
		private Set<EventForMdt>			eventList;               // TS29571 EventForMdt, Array.minItems: 1
		private LoggingIntervalMdt			loggingInterval;         // TS29571 LoggingIntervalMdt.Strng.eunm
		private LoggingIntervalNrMdt		loggingIntervalNr;       // TS29571 LoggingIntervalNrMdt.Strng.eunm
		private LoggingDurationMdt			loggingDuration;         // TS29571 LoggingDurationMdt.Strng.eunm
		private LoggingDurationNrMdt		loggingDurationNr;       // TS29571 LoggingDurationNrMdt.Strng.eunm
		private PositioningMethodMdt		positioningMethod;       // TS29571 PositioningMethodMdt.Strng.eunm
		private CollectionPeriodRmmLteMdt	collectionPeriodRmmLte;  // TS29571 CollectionPeriodRmmLteMdt.Strng.eunm
		private CollectionPeriodRmmNrMdt	collectionPeriodRmmNr;   // TS29571 CollectionPeriodRmmNrMdt.Strng.eunm
		private MeasurementPeriodLteMdt		measurementPeriodLte;    // TS29571 MeasurementPeriodLteMdt.Strng.eunm
		private Set<PlmnId>					mdtAllowedPlmnIdList;    // TS29571 PlmnId, Array.Items: 1~16
		private Set<MbsfnArea>				mbsfnAreaList;           // TS29571 MbsfnArea, Array.Items: 1~8
		private Set<InterFreqTargetInfo>	interFreqTargetList;     // TS29571 InterFreqTargetInfo, Array.Items: 1~8
	}

	public enum JobType  // TS29571
	{
		IMMEDIATE_MDT_ONLY,
		LOGGED_MDT_ONLY,
		TRACE_ONLY,
		IMMEDIATE_MDT_AND_TRACE,
		RLF_REPORTS_ONLY,
		RCEF_REPORTS_ONLY,
		LOGGED_MBSFN_MDT;
	}

	public enum ReportTypeMdt  // TS29571
	{
		PERIODICAL,
		EVENT_TRIGGED;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class AreaScope  // TS29571
	{
		private Set<String>	eutraCellIdList;  // TS2971 EutraCellId=String.pattern: '^[A-Fa-f0-9]{7}$', Array.minItems: 1
		private Set<Snssai>	nrCellIdList;     // TS29571 NrCellId=String.pattern: '^[A-Fa-f0-9]{7}$', Array.minItems: 1
		private Set<String>	tacList;          // TS29571 Tac=String.pattern: '(^[A-Fa-f0-9]{4}$)|(^[A-Fa-f0-9]{6}$)', Array.minItems: 1
		private TacInfo		tacInfoPerPlmn;   // TS29571 TacInfo
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class TacInfo  // TS29571
	{
		@NonNull
		private Set<String> tacList;  // TS29571 Tac=String.pattern: '(^[A-Fa-f0-9]{4}$)|(^[A-Fa-f0-9]{6}$)', Array.minItems: 1
	}

	public enum MeasurementLteForMdt  // TS29571
	{
		M1,
		M2,
		M3,
		M4_DL,
		M4_UL,
		M5_DL,
		M5_UL,
		M6_DL,
		M6_UL,
		M7_DL,
		M7_UL,
		M8,
		M9;
	}

	public enum MeasurementNrForMdt  // TS29571
	{
		M1,
		M2,
		M3,
		M4_DL,
		M4_UL,
		M5_DL,
		M5_UL,
		M6_DL,
		M6_UL,
		M7_DL,
		M7_UL,
		M8,
		M9;
	}

	public enum SensorMeasurement  // TS29571
	{
		BAROMETRIC_PRESSURE,
		UE_SPEED,
		UE_ORIENTATION;
	}

	public enum ReportingTrigger  // TS29571
	{
		PERIODICAL,
		EVENT_A2,
		EVENT_A2_PERIODIC,
		ALL_RRM_EVENT_TRIGGERS;
	}

	public enum ReportIntervalMdt  // TS29571
	{
		_120,
		_240,
		_480,
		_640,
		_1024,
		_2048,
		_5120,
		_10240,
		_60000,
		_360000,
		_720000,
		_1800000,
		_3600000;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	public enum ReportIntervalNrMdt  // TS29571
	{
		_120,
		_240,
		_480,
		_640,
		_1024,
		_2048,
		_5120,
		_10240,
		_20480,
		_40960,
		_60000,
		_360000,
		_720000,
		_1800000,
		_3600000;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	public enum ReportAmountMdt  // TS29571
	{
		_1,
		_2,
		_4,
		_8,
		_16,
		_32,
		_64,
		infinity;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	public enum EventForMdt  // TS29571
	{
		OUT_OF_COVERAG,
		A2_EVENT;
	}

	public enum LoggingIntervalMdt  // TS29571
	{
		_128,
		_256,
		_512,
		_1024,
		_2048,
		_3072,
		_4096,
		_6144;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	public enum LoggingDurationMdt  // TS29571
	{
		_600,
		_1200,
		_2400,
		_3600,
		_5400,
		_7200;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	public enum LoggingIntervalNrMdt  // TS29571
	{
		_128,
		_256,
		_512,
		_1024,
		_2048,
		_3072,
		_4096,
		_6144,
		_320,
		_640,
		_infinity;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	public enum LoggingDurationNrMdt  // TS29571
	{
		_600,
		_1200,
		_2400,
		_3600,
		_5400,
		_7200;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	public enum PositioningMethodMdt  // TS29571
	{
		GNSS,
		E_CELL_ID;
	}

	public enum CollectionPeriodRmmLteMdt  // TS29571
	{
		_1024,
		_1280,
		_2048,
		_2560,
		_5120,
		_10240,
		_60000;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	public enum CollectionPeriodRmmNrMdt  // TS29571
	{
		_1024,
		_2048,
		_5120,
		_10240,
		_60000;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	public enum MeasurementPeriodLteMdt  // TS29571
	{
		_1024,
		_1280,
		_2048,
		_2560,
		_5120,
		_10240,
		_60000;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class MbsfnArea  // TS29571
	{
		private Integer	mbsfnAreaId;       // Integer 0~255
		private Integer	carrierFrequency;  // Integer 0~262143
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class InterFreqTargetInfo  // TS29571
	{
		private Integer			dlCarrierFreq;  // TS29571 ArfcnValueNR=Integer 0~3279165
		private Set<Integer>	cellIdList;     // TS29571 PhysCellId=Integer 0~1007, Array.Items: 1~32
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SessionManagementSubscriptionData  // TS29503
	{
		@NonNull
		private Snssai									singleNssai;
		private Map<String, DnnConfiguration>			dnnConfigurations;
		private Set<String>								internalGroupIds;             // GroupId: pattern: '^[A-Fa-f0-9]{8}-[0-9]{3}-[0-9]{2,3}-([A-Fa-f0-9][A-Fa-f0-9]){1,10}$'
		private Map<String, String>						sharedVnGroupDataIds;         // TS29503 SharedDataId=String.pattern: '^[0-9]{5,6}-.+$'
		private String									sharedDnnConfigurationsId;    // TS29503 SharedDataId=String.pattern: '^[0-9]{5,6}-.+$'
		private OdbPacketServices						odbPacketServices;            // TS29571 OdbPacketServices=String.enum[ALL_PACKET_SERVICES, ROAMER_ACCESS_HPLMN_AP, ROAMER_ACCESS_VPLMN_AP] or other
		private TraceData								traceData;                    // TS29571 TraceData
		private String									sharedTraceDataId;            // TS29503 SharedDataId=String.pattern: '^[0-9]{5,6}-.+$'
		private Map<String, ExpectedUeBehaviourData>	expectedUeBehavioursList;     // TS29503 ExpectedUeBehaviourData
		private Map<String, SuggestedPacketNumDl>		suggestedPacketNumDlList;     // TS29503 SuggestedPacketNumDl
		@JsonProperty("3gppChargingCharacteristics")
		private String									tgppChargingCharacteristics;  // TS29503 3GppChargingCharacteristics=String
	}

	@Data
	@RequiredArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class TraceData // TS29571
	{
		@NonNull
		private String		traceRef;                  // pattern: '^[0-9]{3}[0-9]{2,3}-[A-Fa-f0-9]{6}$'
		@NonNull
		private TraceDepth	traceDepth;                // TS29571 TraceDepth=String.enum[MINIMUM, MEDIUM, MAXIMUM, MINIMUM_WO_VENDOR_EXTENSION, MEDIUM_WO_VENDOR_EXTENSION, MAXIMUM_WO_VENDOR_EXTENSION] or other
		@NonNull
		private String		neTypeList;                // pattern: '^[A-Fa-f0-9]+$'
		@NonNull
		private String		eventList;                 // pattern: '^[A-Fa-f0-9]+$'
		private String		collectionEntityIpv4Addr;  // TS29571 Ipv4Addr
		private String		collectionEntityIpv6Addr;  // TS29571 Ipv6Addr
		private String		interfaceList;             // pattern: '^[A-Fa-f0-9]+$'
	}

	public enum TraceDepth
	{
		MINIMUM,
		MEDIUM,
		MAXIMUM,
		MINIMUM_WO_VENDOR_EXTENSION,
		MEDIUM_WO_VENDOR_EXTENSION,
		MAXIMUM_WO_VENDOR_EXTENSION;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ExpectedUeBehaviourData  // TS29503
	{
		private StationaryIndication		stationaryIndication;        // TS29571 StationaryIndication
		private Integer						communicationDurationTime;   // TS29571 DurationSec=Integer
		private Integer						periodicTime;  				 // TS29571 DurationSec=Integer
		private ScheduledCommunicationTime	scheduledCommunicationTime;  // TS29571 ScheduledCommunicationTime
		private ScheduledCommunicationType	scheduledCommunicationType;  // TS29571 ScheduledCommunicationType
		private Set<LocationArea>			expectedUmts;                // TS29503 LocationArea, minItems: 1
		private TrafficProfile				trafficProfile;              // TS29571 TrafficProfile
		private BatteryIndication			batteryIndication;           // TS29571 BatteryIndication
		private String						validityTime;                // TS29571 DateTime=String
	}

	enum StationaryIndication  // TS29571
	{
		STATIONARY,
		MOBILE
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ScheduledCommunicationTime  // TS29571
	{
		private Set<Integer>	daysOfWeek;      // TS29571 DayOfWeek=Integer, 1~7
		private String			timeOfDayStart;  // TS29571 TimeOfDay=String, example, 20:15:00
		private String			timeOfDayEnd;    // TS29571 TimeOfDay, example, 20:15:00
	}

	enum ScheduledCommunicationType  // TS29571
	{
		DOWNLINK_ONLY,
		UPLINK_ONLY,
		BIDIRECTIONAL
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class LocationArea // TS29503
	{
		private Set<GeographicArea>	geographicAreas	= new HashSet<>();  // TS29572 GeographicArea, minItems: 0
		private Set<CivicAddress>	civicAddresses	= new HashSet<>();  // TS29572 CivicAddress, minItems: 0
		private NetworkAreaInfo		nwAreaInfo;                         // TS29503 NetworkAreaInfo
	}

	enum TrafficProfile  // TS29571
	{
		SINGLE_TRANS_UL,
		SINGLE_TRANS_DL,
		DUAL_TRANS_UL_FIRST,
		DUAL_TRANS_DL_FIRST,
		MULTI_TRANS
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class BatteryIndication  // TS29571
	{
		private Boolean	batteryInd;
		private Boolean	replaceableInd;
		private Boolean	rechargeableInd;
	}

	public interface GeographicArea // TS29572
	{}

	public static class GADShape // TS29572
	{
		// 這裡有某種限定繼承的概念，換句話說，多型並不是開放的，而是有限定範圍，anyOf 與 discriminator-mapping 要相互搭配
		public SupportedGADShapes shape;
	}

	enum SupportedGADShapes // TS29572
	{
		POINT,
		POINT_UNCERTAINTY_CIRCLE,
		POINT_UNCERTAINTY_ELLIPSE,
		POLYGON,
		POINT_ALTITUDE,
		POINT_ALTITUDE_UNCERTAINTY,
		ELLIPSOID_ARC
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class GeographicalCoordinates  // TS29572
	{
		@NonNull
		private Double	lon;  // Double, -180~180
		@NonNull
		private Double	lat;  // Double, -90~90
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class UncertaintyEllipse  // TS29572
	{
		@NonNull
		private Float	semiMajor;         // TS29572 Uncertainty=Float.minimum: 0
		@NonNull
		private Float	semiMinor;         // TS29572 Uncertainty=Float.minimum: 0
		@NonNull
		private Integer	orientationMajor;  // TS29572 Orientation=Integer, 0~180
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Point extends GADShape implements GeographicArea   // TS29572
	{
		@NonNull
		private GeographicalCoordinates point;  // TS29572 GeographicalCoordinates
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class PointUncertaintyCircle extends GADShape implements GeographicArea  // TS29572
	{
		@NonNull
		private GeographicalCoordinates	point;        // TS29572 GeographicalCoordinates
		@NonNull
		private Float					uncertainty;  // TS29572 Uncertainty=Float.minimum: 0
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class PointUncertaintyEllipse extends GADShape implements GeographicArea  // TS29572
	{
		@NonNull
		private GeographicalCoordinates	point;               // TS29572 GeographicalCoordinates
		@NonNull
		private UncertaintyEllipse		uncertaintyEllipse;  // TS29572 UncertaintyEllipse
		@NonNull
		private Integer					confidence;          // TS29572 Confidence=Integer, 0~100
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Polygon extends GADShape implements GeographicArea // TS29572
	{
		@NonNull
		private Set<GeographicalCoordinates> pointList;  // TS29572 PointList.GeographicalCoordinates, 3~15
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class PointAltitude extends GADShape implements GeographicArea // TS29572
	{
		@NonNull
		private GeographicalCoordinates	point;     // TS29572 GeographicalCoordinates
		@NonNull
		private Double					altitude;  // TS29572 Altitude=Double, -32767~32767
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class PointAltitudeUncertainty extends GADShape implements GeographicArea // TS29572
	{
		@NonNull
		private GeographicalCoordinates	point;                // TS29572 GeographicalCoordinates
		@NonNull
		private Double					altitude;             // TS29572 Altitude=Double, -32767~32767
		@NonNull
		private UncertaintyEllipse		uncertaintyEllipse;   // TS29572 UncertaintyEllipse
		@NonNull
		private Float					uncertaintyAltitude;  // TS29572 Uncertainty=Float.minimum: 0
		@NonNull
		private Integer					confidence;           // TS29572 Confidence=Integer, 0~100
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class EllipsoidArc extends GADShape implements GeographicArea // TS29572
	{
		@NonNull
		private GeographicalCoordinates	point;              // TS29572 GeographicalCoordinates
		@NonNull
		private Integer					innerRadius;        // TS29572 InnerRadius=Integer, 0~327675
		@NonNull
		private Float					uncertaintyRadius;  // TS29572 Uncertainty=Float.minimum: 0
		@NonNull
		private Integer					offsetAngle;        // TS29572 Angle=Integer, 0~360
		@NonNull
		private Integer					includedAngle;      // TS29572 Angle=Integer, 0~360
		@NonNull
		private Integer					confidence;         // TS29572 Confidence=Integer, 0~100
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class CivicAddress // TS29572
	{
		private String country;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class NetworkAreaInfo // TS29503
	{
		private Set<Ecgi>				ecgis		= new HashSet<>();  // TS29571 Ecgi, minItems: 1
		private Set<Ecgi>				ncgis		= new HashSet<>();  // TS29571 Ecgi, minItems: 1
		private Set<GlobalRanNodeId>	gRanNodeIds	= new HashSet<>();  // TS29571 GlobalRanNodeId, minItems: 1
		private Set<Tai>				tais		= new HashSet<>();  // TS29571 Tai, minItems: 1
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Ecgi // TS29571
	{
		@NonNull
		private PlmnId	plmnId;       // TS29571 PlmnId
		@NonNull
		private String	eutraCellId;  // TS29571 EutraCellId=String.pattern: '^[A-Fa-f0-9]{7}$'
		private String	nid;          // TS29571 Nid=String.pattern: '^[A-Fa-f0-9]{11}$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Ncgi // TS29571
	{
		@NonNull
		private PlmnId	plmnId;     // TS29571 PlmnId
		@NonNull
		private String	nrCellId;  // TS29571 NrCellId=String.pattern: '^[A-Fa-f0-9]{9}$'
		private String	nid;       // TS29571 Nid=String.pattern: '^[A-Fa-f0-9]{11}$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class GlobalRanNodeId  // TS29571
	{
		@NonNull
		private PlmnId	plmnId;   // TS29571 PlmnId
		private String	n3IwfId;  // TS29571 N3IwfId=String.pattern: '^[A-Fa-f0-9]+$'
		private GNbId	gNbId;    // TS29571 GNbId
		private String	ngeNbId;  // TS29571 NgeNbId=String.pattern: '^(MacroNGeNB-[A-Fa-f0-9]{5}|LMacroNGeNB-[A-Fa-f0-9]{6}|SMacroNGeNB-[A-Fa-f0-9]{5})$'
		private String	wagfId;   // TS29571 WAgfId=String.pattern: '^[A-Fa-f0-9]+$'
		private String	tngfId;   // TS29571 TngfId=String.pattern: '^[A-Fa-f0-9]+$'
		private String	nid;      // TS29571 Nid=String.pattern: '^[A-Fa-f0-9]{11}$'
		private String	eNbId;    // TS29571 ENbId=String.pattern: '^(MacroeNB-[A-Fa-f0-9]{5}|LMacroeNB-[A-Fa-f0-9]{6}|SMacroeNB-[A-Fa-f0-9]{5}|HomeeNB-[A-Fa-f0-9]{7})$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class GNbId  // TS29571
	{
		@NonNull
		private Integer	bitLength;  // Integer 22~32
		@NonNull
		private String	gNBValue;   // String.pattern: '^[A-Fa-f0-9]{6,8}$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SuggestedPacketNumDl  // TS29503
	{
		@NonNull
		private Integer	suggestedPacketNumDl; // Integer.minimum: 1
		private String	validityTime;         // TS29571 DateTime=String
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SmfSelectionSubscriptionData  // TS29503
	{
		private String					supportedFeatures;      // TS29571 SupportedFeatures=String.pattern: '^[A-Fa-f0-9]*$'
		private Map<String, SnssaiInfo>	subscribedSnssaiInfos;  // TS29503 SnssaiInfo
		private String					sharedSnssaiInfosId;    // TS29503 SharedDataId=String.pattern: '^[0-9]{5,6}-.+$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SnssaiInfo  // TS29503
	{
		@NonNull
		private Set<DnnInfo> dnnInfos = new LinkedHashSet<>();  // TS29503 DnnInfo, minItems: 1
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class DnnInfo  // TS29503
	{
		@NonNull
		private String		dnn;                  // TS29571 Dnn=String
		private Boolean		defaultDnnIndicator;  // TS29503 DnnIndicator=Boolean
		private Boolean		lboRoamingAllowed;    // TS29503 LboRoamingAllowed=Boolean
		private Boolean		iwkEpsInd;  	      // TS29503 IwkEpsInd=Boolean
		private Boolean		dnnBarred;
		private Boolean		invokeNefInd;
		private Set<String>	smfList;              // TS29571 NfInstanceId=String.uuid, minItems: 1
		private Boolean		sameSmfInd;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class AmPolicyData  // TS29519: Contains the AM policy data for a given subscriber.
	{
		private Map<String, PresenceInfo>	praInfos;   // TS29571 PresenceInfo Map.minProperties: 1
		private Set<String>					subscCats;  // TS29519 String, Array.minItems: 1
	}

	@Data
	public static class PresenceInfo  // TS29571
	{
		private String					praId;
		private String					additionalPraId;
		private PresenceState			presenceState;        // TS29571 PresenceState
		private Set<Tai>				trackingAreaList;     // TS29571 Tai, minItems: 1
		private Set<Ecgi>				ecgiList;             // TS29571 Ecgi, minItems: 1
		private Set<Ncgi>				ncgiList;             // TS29571 Ncgi, minItems: 1
		private Set<GlobalRanNodeId>	globalRanNodeIdList;  // TS29571 GlobalRanNodeId, minItems: 1
		private Set<GlobalRanNodeId>	globaleNbIdList;      // TS29571 GlobalRanNodeId, minItems: 1
	}

	enum PresenceState  // TS29571
	{
		IN_AREA,
		OUT_OF_AREA,
		UNKNOWN,
		INACTIVE;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SmPolicyData  // TS29519: Contains the SM policy data for a given subscriber.
	{
		@NonNull
		private Map<String, SmPolicySnssaiData>	smPolicySnssaiData;  // TS29519 SmPolicySnssaiData, minProperties: 1
		private Map<String, UsageMonDataLimit>	umDataLimits;        // TS29519 UsageMonDataLimit
		private Map<String, UsageMonData>		umData;              // TS29519 UsageMonData
		private String							suppFeat;            // TS29571 SupportedFeatures=String.pattern: '^[A-Fa-f0-9]*$'
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SmPolicySnssaiData  // TS29519: Contains the SM policy data for a given subscriber and S-NSSAI.
	{
		@NonNull
		private Snssai							snssai;           // TS29571 Snssai
		private Map<String, SmPolicyDnnData>	smPolicyDnnData;  // TS29519 SmPolicyDnnData, Map.minProperties: 1
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SmPolicyDnnData // TS29519: Contains the SM policy data for a given DNN (and S-NSSAI).
	{
		@NonNull
		private String								dnn;                // TS29571 Dnn=String
		private Set<String>							allowedServices;    // minItems: 1
		private Set<String>							subscCats;          // minItems: 1
		private String								gbrUl;              // TS29571 BitRate.pattern: '^\d+(\.\d+)? (bps|Kbps|Mbps|Gbps|Tbps)$'
		private String								gbrDl;              // TS29571 BitRate.pattern: '^\d+(\.\d+)? (bps|Kbps|Mbps|Gbps|Tbps)$'
		private Boolean								adcSupport;
		private Boolean								subscSpendingLimits;
		private Integer								ipv4Index;          // TS29519 IpIndex=Integer
		private Integer								ipv6Index;          // TS29519 IpIndex=Integer
		private Boolean								offline;
		private Boolean								online;
		private ChargingInformation					chfInfo;            // TS29512 ChargingInformation
		private Map<String, LimitIdToMonitoringKey>	refUmDataLimitIds;  // TS29519 LimitIdToMonitoringKey, minProperties: 1
		private Boolean								mpsPriority;
		private Boolean								mcsPriority;
		private Boolean								imsSignallingPrio;
		private Integer								mpsPriorityLevel;
		private Integer								mcsPriorityLevel;
		private Map<String, PresenceInfo>			praInfos;           // TS29571 PresenceInfo, minProperties: 1
		private Map<String, String>					bdtRefIds;          // TS29122 BdtReferenceIdRm=String, minProperties: 1
		private Boolean								locRoutNotAllowed;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ChargingInformation  // TS29512
	{
		@NonNull
		private String	primaryChfAddress;       // TS29571 Uri=String
		@NonNull
		private String	secondaryChfAddress;     // TS29571 Uri=String
		private String	primaryChfSetId;         // TS29571 NfSetId=String
		private String	primaryChfInstanceId;    // TS29571 NfInstanceId=String.uuid
		private String	secondaryChfSetId;       // TS29571 NfSetId=String
		private String	secondaryChfInstanceId;  // TS29571 NfInstanceId=String.uuid
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class LimitIdToMonitoringKey  // TS29519: Contains the limit identifier and the corresponding monitoring key for a given SNSSAI and DNN.
	{
		@NonNull
		private String		limitId;
		private Set<String>	monkey;  // Array.minItems: 1
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class UsageMonDataLimit  // TS29519: Contains usage monitoring control data for a subscriber.
	{
		@NonNull
		private String							limitId;
		private Map<String, UsageMonDataScope>	scopes;      // TS29519 UsageMonDataScope, Map.minProperties: 1
		private UsageMonLevel					umLevel;     // TS29519 UsageMonLevel
		private String							startDate;   // TS29571 DateTime=String
		private String							endDate;     // TS29571 DateTime=String
		private UsageThreshold					usageLimit;  // TS29122 UsageThreshold
		private TimePeriod						resetPeriod; // TS29519 TimePeriod
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class UsageMonDataScope  // TS29519: Contains a SNSSAI and DNN combinations to which the UsageMonData instance belongs to.
	{
		@NonNull
		private Snssai		snssai;  // TS29571 Snssai
		private Set<String>	dnn;     // TS29571 Dnn=String, minItems: 1
	}

	enum UsageMonLevel  // TS29519
	{
		SESSION_LEVEL,
		SERVICE_LEVEL;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class UsageThreshold  // TS29122 UsageThreshold
	{
		private Integer	duration;     // TS29122 DurationSec=Integer, 0~, Unsigned integer identifying a period of time in units of seconds.
		private Long	totalVolume;     // TS29122 Volume=Long, 0~, Unsigned integer identifying a volume in units of bytes.
		private Long	downlinkVolume;  // TS29122 Volume=Long, 0~, Unsigned integer identifying a volume in units of bytes.
		private Long	uplinkVolume;    // TS29122 Volume=Long, 0~, Unsigned integer identifying a volume in units of bytes.
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class TimePeriod  // TS29519: Contains the periodicity for the defined usage monitoring data limits.
	{
		private Periodicity	period;        // TS29519 Periodicity
		private Integer		maxNumPeriod;  // TS29571 Uinteger=Integer, 0~
	}

	enum Periodicity  // TS29519 Periodicity
	{
		YEARLY,
		MONTHLY,
		WEEKLY,
		DAILY,
		HOURLY
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class UsageMonData // TS29519: Contains remain allowed usage data for a subscriber.
	{
		private String							limitId;
		private Map<String, UsageMonDataScope>	scopes;        // TS29519 UsageMonDataScope, minProperties: 1
		private UsageMonLevel					umLevel;       // TS29519 UsageMonLevel
		private UsageThreshold					allowedUsage;  // TS29122 UsageThreshold
		private String							resetTime;     // TS29571 DateTime=String
		private String							suppFeat;      // TS29571 SupportedFeatures=String.pattern: '^[A-Fa-f0-9]*$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class DnnConfiguration  // TS29503
	{
		@NonNull
		private PduSessionTypes			pduSessionTypes;              // TS29503 PduSessionTypes
		@NonNull
		private SscModes				sscModes;                     // TS29503 SscModes
		private Boolean					iwkEpsInd;                    // TS29503 IwkEpsInd=Boolean
		@JsonProperty("5gQosProfile")
		private SubscribedDefaultQos	fivegQosProfile;              // TS29571 SubscribedDefaultQos
		private Ambr					sessionAmbr;
		@JsonProperty("3gppChargingCharacteristics")
		private String					tgppChargingCharacteristics;  // TS29503 3GppChargingCharacteristics=String
		private Set<String>				staticIpAddress;              // TS29503 IpAddress, Array.Items: 1~2
		private UpSecurity				upSecurity;                   // TS29571 UpSecurity
		private String					pduSessionContinuityInd;      // TS29503 PduSessionContinuityInd=String.enum
		private String					niddNefId;                    // TS29510 NefId=String
		private NiddInformation			niddInfo;                     // TS29503 NiddInformation
		private Boolean					redundantSessionAllowed;
		private AcsInfo					acsInfo;                      // TS29571 AcsInfo,
		private Set<FrameRouteInfo>		ipv4FrameRouteList;           // TS29503 FrameRouteInfo, Array.minItems: 1
		private Set<FrameRouteInfo>		ipv6FrameRouteList;           // TS29503 FrameRouteInfo, Array.minItems: 1
		private Boolean					atsssAllowed;	              // default: false;
		private Boolean					secondaryAuth;
		private Boolean					dnAaaIpAddressAllocation;
		// Ipv4Addr.pattern: '^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])$'
		// Ipv6Addr.pattern: '^((:|(0?|([1-9a-f][0-9a-f]{0,3}))):)((0?|([1-9a-f][0-9af]{0,3})):){0,6}(:|(0?|([1-9a-f][0-9a-f]{0,3})))$'
		// or pattern: '^((([^:]+:){7}([^:]+))|((([^:]+:)*[^:]+)?::(([^:]+:)*[^:]+)?))$'
		// Ipv6Prefix.pattern: '^((:|(0?|([1-9a-f][0-9a-f]{0,3}))):)((0?|([1-9a-f][0-9af]{0,3})):){0,6}(:|(0?|([1-9a-f][0-9a-f]{0,3})))(\/(([0-9])|([0-9]{2})|(1[0-1][0-9])|(12[0-8])))$'
		// or pattern: '^((([^:]+:){7}([^:]+))|((([^:]+:)*[^:]+)?::(([^:]+:)*[^:]+)?))(\/.+)$'
		private String					dnAaaAddress;                 // TS29503 IpAddress
		private String					iptvAccCtrlInfo;
	}

	public enum PduSessionContinuityInd
	{
		MAINTAIN_PDUSESSION,
		RECONNECT_PDUSESSION,
		RELEASE_PDUSESSION;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class PduSessionTypes
	{
		@NonNull
		private PduSessionType		defaultSessionType;  // TS29571 PduSessionType=String.enum
		private Set<PduSessionType>	allowedSessionTypes; // TS29571 PduSessionType, Array.minItems: 1
	}

	public enum PduSessionType  // TS29571
	{
		IPV4,
		IPV6,
		IPV4V6,
		UNSTRUCTURED,
		ETHERNET;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SscModes
	{
		@NonNull
		private SscMode			defaultSscMode;   // TS29571 SscMode, enum=[SSC_MODE_1, SSC_MODE_2, SSC_MODE_3] or other
		private Set<SscMode>	allowedSscModes;  // TS29571 SscMode, minItems: 1, maxItems: 2
	}

	public enum SscMode  // TS29571
	{
		SSC_MODE_1,
		SSC_MODE_2,
		SSC_MODE_3;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class UpSecurity
	{
		private String	upIntegr;  // TS29571 UpIntegrity, enum=[REQUIRED, PREFERRED, NOT_NEEDED] or other string
		private String	upConfid;  // TS29571 UpConfidentiality, enum=[REQUIRED, PREFERRED, NOT_NEEDED] or other string
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SubscribedDefaultQos // TS29571
	{
		@NonNull
		@JsonProperty("5qi")
		private Integer	fiveqi;         // TS29571 5Qi=Integer, 0~255
		@NonNull
		private Arp		arp;            // TS29571 Arp
		private Integer	priorityLevel;  // TS29571 5QiPriorityLevel=Integer, 1~127
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Arp  // TS29571
	{
		@NonNull
		private Integer					priorityLevel;  // TS29571 ArpPriorityLevel=Integer, 1~15
		@NonNull
		private PreemptionCapability	preemptCap;     // TS29571 PreemptionCapability=enum[NOT_PREEMPT, MAY_PREEMPT] or other
		@NonNull
		private PreemptionVulnerability	preemptVuln;    // TS29571 PreemptionVulnerability=enum[NOT_PREEMPTABLE, PREEMPTABLE] or other
	}

	public enum PreemptionCapability
	{
		NOT_PREEMPT,
		MAY_PREEMPT;
	}

	public enum PreemptionVulnerability
	{
		NOT_PREEMPTABLE,
		PREEMPTABLE;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class NiddInformation  // TS29503
	{
		@NonNull
		private String	afId;
		private String	gpsi;        // TS29571 Gpsi.pattern: '^(msisdn-[0-9]{5,15}|extid-[^@]+@[^@]+|.+)$'
		private String	extGroupId;  // TS29571 ExternalGroupId=String.pattern: '^extgroupid-[^@]+@[^@]+$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class AcsInfo  // TS29571
	{
		private String	acsUrl;       // TS29571 Uri=String
		private String	acsIpv4Addr;  // TS29571 Ipv4Addr=String.pattern: '^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])$'
		private String	acsIpv6Addr;  // TS29571 Ipv6Addr=String.pattern: '^((:|(0?|([1-9a-f][0-9a-f]{0,3}))):)((0?|([1-9a-f][0-9af]{0,3})):){0,6}(:|(0?|([1-9a-f][0-9a-f]{0,3})))$' or pattern: '^((([^:]+:){7}([^:]+))|((([^:]+:)*[^:]+)?::(([^:]+:)*[^:]+)?))$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class FrameRouteInfo // TS29503
	{
		// Ipv4AddrMask.pattern: '^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\/([0-9]|[1-2][0-9]|3[0-2]))$'
		// example: '198.51.0.0/16'
		private String	ipv4Mask;    // TS29571 Ipv4AddrMask=String.pattern: '^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\/([0-9]|[1-2][0-9]|3[0-2]))$'
		private String	ipv6Prefix;  // TS29571 Ipv6Prefix=String.pattern: '^((:|(0?|([1-9a-f][0-9a-f]{0,3}))):)((0?|([1-9a-f][0-9af]{0,3})):){0,6}(:|(0?|([1-9a-f][0-9a-f]{0,3})))(\/(([0-9])|([0-9]{2})|(1[0-1][0-9])|(12[0-8])))$' or pattern:
		// '^((([^:]+:){7}([^:]+))|((([^:]+:)*[^:]+)?::(([^:]+:)*[^:]+)?))(\/.+)$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SmsSubscriptionData   // TS29503
	{
		private Boolean	smsSubscribed;        // TS29503 SmsSubscribed=Boolean
		private String	sharedSmsSubsDataId;  // TS29503 SharedDataId=String.pattern: '^[0-9]{5,6}-.+$'
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class SmsManagementSubscriptionData  // TS29503
	{
		private String		supportedFeatures;    // TS29503 SupportedFeatures=String.pattern: '^[A-Fa-f0-9]*$'
		private Boolean		mtSmsSubscribed;
		private Boolean		mtSmsBarringAll;
		private Boolean		mtSmsBarringRoaming;
		private Boolean		moSmsSubscribed;
		private Boolean		moSmsBarringAll;
		private Boolean		moSmsBarringRoaming;
		private Set<String>	sharedSmsMngDataIds;  // TS29503 SharedDataId=String.pattern: '^[0-9]{5,6}-.+$', minItems: 1
		private TraceData	traceData;            // TS29503 TraceData
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class OdbData  // TS29571
	{
		private RoamingOdb roamingOdb;  // TS29571 RoamingOdb
	}

	public enum RoamingOdb  // TS29571
	{
		OUTSIDE_HOME_PLMN,
		OUTSIDE_HOME_PLMN_COUNTRY;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class OperatorSpecificDataContainer  // TS29505
	{
		@NonNull
		private DataType	dataType;              // TS29571 DataType
		private String		dataTypeDefinition;
		@NonNull
		private Object		value;
		private String		supportedFeatures;     // TS29571 SupportedFeatures=String.pattern: '^[A-Fa-f0-9]*$'
	}

	public enum DataType  // TS29571
	{
		string,
		integer,
		number,
		_boolean,
		object;

		@Override
		@JsonValue
		public String toString()
		{
			return name().replaceFirst("_", "");
		}
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class EeProfileData  // TS29505
	{
		private Set<EventType>					restrictedEventTypes;  // TS29503 EventType=String.enum
		private String							supportedFeatures;     // TS29571 SupportedFeatures=String.pattern: '^[A-Fa-f0-9]*$'
		// A map (list of key-value pairs where EventType serves as key) of MTC provider lists. In addition to defined EventTypes, the key value "ALL" may be used to identify a map entry which contains a list of MtcProviders that are allowed monitoring all Event Types.
		private Map<String, Set<MtcProvider>>	allowedMtcProvider;    // TS29505 MtcProvider, Array.minItems: 1, Map.minProperties: 1
	}

	public enum EventType
	{
		LOSS_OF_CONNECTIVITY,
		UE_REACHABILITY_FOR_DATA,
		UE_REACHABILITY_FOR_SMS,
		LOCATION_REPORTING,
		CHANGE_OF_SUPI_PEI_ASSOCIATION,
		ROAMING_STATUS,
		COMMUNICATION_FAILURE,
		AVAILABILITY_AFTER_DDN_FAILURE,
		CN_TYPE_CHANGE,
		DL_DATA_DELIVERY_STATUS,
		PDN_CONNECTIVITY_STATUS,
		UE_CONNECTION_MANAGEMENT_STATE;
	}

	@Data
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class MtcProvider  // TS29505
	{
		private String	mtcProviderInformation;  // TS29571 MtcProviderInformation=String
		private String	afId;
	}
}