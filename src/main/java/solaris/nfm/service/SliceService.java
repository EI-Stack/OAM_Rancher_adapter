package solaris.nfm.service;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import solaris.nfm.exception.base.ExceptionBase;
import solaris.nfm.model.resource.slice.instance.NetworkSliceInstance;
import solaris.nfm.model.resource.slice.instance.NetworkSliceInstanceDao;
import solaris.nfm.model.resource.slice.template.NetworkSliceDao;

@Service
@Slf4j
public class SliceService
{
	@Autowired
	protected ObjectMapper			objectMapper;
	@Autowired
	private NetworkSliceDao			networkSliceDao;
	@Autowired
	private NetworkSliceInstanceDao	networkSliceInstanceDao;
	@Autowired
	private RicService				ricSrv;

	/**
	 * 取得從屬於該 network slice 的可用 RAN slice 清單
	 * 依據 (sst, sd)，(1) 先找 RAN slice 相符者 (2) 檢查目前所有的 instance 是否有使用該 RAN slice
	 */
	public Set<String> getAvailableRanSliceSet(final Long networkSliceId) throws ExceptionBase, StreamReadException, DatabindException, IOException
	{
		// 先取得 slice type (snssai)
		final solaris.nfm.model.resource.slice.template.NetworkSlice networkSlice = this.networkSliceDao.getById(networkSliceId);
		final ArrayNode serviceProfiles = networkSlice.getServiceProfiles();
		final JsonNode snssai = serviceProfiles.path(0).path("pLMNInfoList").path(0).path("snssai");
		final Integer sst = snssai.path("sst").asInt();
		final String sd = snssai.path("sd").asText();

		// 取得相符的 RAN slice 清單，但不確定是否可用
		final Set<String> ranSliceSet = new HashSet<>();
		final ArrayNode ricSlices = this.ricSrv.getRicSlices();
		for (final JsonNode ricSlice : ricSlices)
		{
			final Integer tmpSst = ricSlice.path("sst").asInt();
			final String tmpSd = ricSlice.path("sd").asText();
			if (sst == tmpSst && sd.equals(tmpSd))
			{
				ranSliceSet.add(ricSlice.path("networkSliceInstanceUid").asText());
			}
		}
		log.debug("ranSliceSet={}", ranSliceSet);

		// 取得已配置的 RAN slice 清單
		final Set<String> appliedRanSliceSet = new HashSet<>();
		final List<NetworkSliceInstance> networkSliceInstanceList = this.networkSliceInstanceDao.findAll();
		for (final NetworkSliceInstance networkSliceInstance : networkSliceInstanceList)
		{
			appliedRanSliceSet.addAll(networkSliceInstance.getRicSliceList());
		}
		log.debug("appliedRanSliceSet={}", appliedRanSliceSet);

		ranSliceSet.removeAll(appliedRanSliceSet);  // 此時只剩下可用的 RAN slice
		return ranSliceSet;
	}

	public JsonNode getSnssai(final Long networkSliceId)
	{
		// 先取得 slice type (snssai)
		final solaris.nfm.model.resource.slice.template.NetworkSlice networkSlice = this.networkSliceDao.getById(networkSliceId);
		final ArrayNode serviceProfiles = networkSlice.getServiceProfiles();
		final JsonNode snssai = serviceProfiles.path(0).path("pLMNInfoList").path(0).path("snssai");
		return snssai;
	}

	/**
	 * 檢查 cell 集合中，是否有任何 cell 成員是實際不存在或是已經被綁定
	 */
	public void checkNci(final Set<String> targetNciSet, final String excepRanSliceId) throws ExceptionBase, StreamReadException, DatabindException, IOException
	{
		final Set<String> nciSet = getNciSet();
		final Set<String> noneExistNciSet = new LinkedHashSet<>(targetNciSet);
		noneExistNciSet.removeAll(nciSet);
		if (noneExistNciSet.size() > 0) throw new ExceptionBase(400, "Cell NCIs in set " + noneExistNciSet + " do not exist.");

		final Set<String> boundNciSet = new LinkedHashSet<>(targetNciSet);
		log.debug("targetNciSet={}", boundNciSet);
		final Set<String> allBoundNciSet = getBoundNciSet(excepRanSliceId);
		log.debug("allBoundNciSet={}", allBoundNciSet);
		boundNciSet.retainAll(allBoundNciSet);
		log.debug("boundNciSet={}", boundNciSet);
		if (boundNciSet.size() > 0) throw new ExceptionBase(400, "Cell NCIs in set " + boundNciSet + " are bound by other RAN slices.");
	}

	/**
	 * 取得所有的 cell 的集合
	 */
	public Set<String> getNciSet() throws ExceptionBase, StreamReadException, DatabindException, IOException
	{
		final JsonNode root = this.ricSrv.doMethodGet("/v1/privateAPI/getCellList");
		final Set<String> nciSet = this.objectMapper.readValue(root.path("NCI").traverse(), new TypeReference<TreeSet<String>>()
		{});

		return nciSet;
	}

	/**
	 * 取得所有已經被 RAN slice 綁定的 cell NCI 的集合
	 *
	 * @param excepRanSliceId
	 *        表示除外的 RAN slice。若不須考慮，則以 null 表示
	 */
	public Set<String> getBoundNciSet(final String excepRanSliceId) throws ExceptionBase, StreamReadException, DatabindException, IOException
	{
		final ArrayNode ricSlices = (ArrayNode) this.ricSrv.doMethodGet("/v1/networkSliceInstances/ricSlices");
		final ArrayNode boundCellNcis = JsonNodeFactory.instance.arrayNode();
		for (final JsonNode tmpRicSlice : ricSlices)
		{
			final ObjectNode ricSlice = (ObjectNode) tmpRicSlice;
			if (excepRanSliceId != null && ricSlice.path("networkSliceInstanceUid").asText().equals(excepRanSliceId)) continue;
			boundCellNcis.addAll((ArrayNode) ricSlice.path("activeList"));
			boundCellNcis.addAll((ArrayNode) ricSlice.path("terminateList"));
		}
		final Set<String> boundCellNciSet = this.objectMapper.readValue(boundCellNcis.traverse(), new TypeReference<LinkedHashSet<String>>()
		{});
		return boundCellNciSet;
	}

	/**
	 * 取得所有尚未被 RAN slice 綁定的 cell NCI 的集合
	 */
	public Set<String> getUnboundNciSet() throws ExceptionBase, StreamReadException, DatabindException, IOException
	{
		final Set<String> allNicSet = getNciSet();
		final Set<String> boundNciSet = getBoundNciSet(null);
		final Set<String> unboundNciSet = new LinkedHashSet<>(allNicSet);
		unboundNciSet.removeAll(boundNciSet);

		return unboundNciSet;
	}

	public ObjectNode createArrayNodeForNci(final Set<String> nciSet)
	{
		final ObjectNode json = JsonNodeFactory.instance.objectNode();
		final ArrayNode ncis = json.putArray("NCI");
		for (final String nci : nciSet)
		{
			ncis.add(nci);
		}

		return json;
	}

	/**
	 * According to TS 28.541 v16.10.00
	 */
	public JsonNode createSliceProfile(final String ricSliceId)
	{
		final ObjectNode sliceProfile = JsonNodeFactory.instance.objectNode();
		sliceProfile.put("sliceProfileId", "cn=" + ricSliceId);  // String, Must
		final ArrayNode snssaiList = sliceProfile.putArray("sNSSAIList");  // Must (?)
		final ArrayNode plmnInfoList = sliceProfile.putArray("pLMNInfoList"); // Must
		final ArrayNode perfReq = sliceProfile.putArray("perfReq"); // Must
		sliceProfile.put("maxNumberofUEs", 100L);  // Long
		final ArrayNode coverageAreaTAList = sliceProfile.putArray("coverageAreaTAList");
		sliceProfile.put("latency", 50);  // Integer
		sliceProfile.put("uEMobilityLevel", "stationary");  // Integer
		sliceProfile.put("resourceSharingLevel", "shared");  // Integer

		final ObjectNode snssai = JsonNodeFactory.instance.objectNode();
		snssai.put("idx", 1);
		snssai.put("sst", 1);
		snssai.put("sd", "FFFFFF");
		snssaiList.add(snssai);

		final ObjectNode plmnInfo = JsonNodeFactory.instance.objectNode();
		plmnInfo.put("mcc", "466");
		plmnInfo.put("mnc", "93");
		plmnInfoList.add(plmnInfo);

		return sliceProfile;
	}

	/**
	 * According to TS 28.541 v16.10.00
	 */
	public ObjectNode createServiceProfile(final Integer sst, final String sd, final String mcc, final String mnc)
	{
		final ObjectNode serviceProfile = JsonNodeFactory.instance.objectNode();
		serviceProfile.put("serviceProfileId", "1");  // String, Must
		serviceProfile.put("sST", sst);  // Must
		final ArrayNode plmnInfoList = serviceProfile.putArray("pLMNInfoList"); // Must
		serviceProfile.put("maxNumberofUEs", 0L);  // Long
		serviceProfile.put("coverageArea", "0");  // String
		serviceProfile.put("latency", 50);  // Integer
		serviceProfile.put("uEMobilityLevel", "stationary");  // Integer
		serviceProfile.put("resourceSharingLevel", "shared");  // Integer (?)
		serviceProfile.put("networkSliceSharingIndicator", "");  // Integer
		serviceProfile.put("availability", 60.1f);  // Float
		final ObjectNode delayTolerance = serviceProfile.putObject("delayTolerance");
		final ObjectNode deterministicComm = serviceProfile.putObject("deterministicComm");
		final ObjectNode dLThptPerSlice = serviceProfile.putObject("dLThptPerSlice");
		final ObjectNode dLThptPerUE = serviceProfile.putObject("dLThptPerUE");
		final ObjectNode uLThptPerSlice = serviceProfile.putObject("uLThptPerSlice");
		final ObjectNode uLThptPerUE = serviceProfile.putObject("uLThptPerUE");
		final ObjectNode maxPktSize = serviceProfile.putObject("maxPktSize");
		final ObjectNode maxNumberofConns = serviceProfile.putObject("maxNumberofConns");
		final ObjectNode kPIMonitoring = serviceProfile.putObject("kPIMonitoring");
		final ObjectNode userMgmtOpen = serviceProfile.putObject("userMgmtOpen");
		final ObjectNode v2XCommModels = serviceProfile.putObject("v2XCommModels");
		final ObjectNode termDensity = serviceProfile.putObject("termDensity");
		// serviceProfile.put("resourceSharingLevel", "shared");
		serviceProfile.put("activityFactor", 60.1f);  // Float
		serviceProfile.put("uESpeed", 3);  // Integer
		serviceProfile.put("jitter", 10);  // Integer
		serviceProfile.put("survivalTime", "2021-10-22T02:37:50.302Z");  // String
		serviceProfile.put("reliability", "string");  // String
		// serviceProfile.put("maxDLDataVolume", "string");
		// serviceProfile.put("maxULDataVolume", "string");

		final ObjectNode plmnInfo = JsonNodeFactory.instance.objectNode();
		final ObjectNode plmnId = plmnInfo.putObject("PlmnId");
		// 國家代碼，台灣 466, String, pattern: '^[0-9]{3}$'
		plmnId.put("mcc", "466");
		// 電信營運商代碼, 02 沒人用, String, pattern: '^[0-9]{2,3}$'
		plmnId.put("mnc", "93");
		final ObjectNode snssai = plmnInfo.putObject("snssai");
		snssai.put("sst", sst);
		snssai.put("sd", sd);
		plmnInfoList.add(plmnInfo);

		delayTolerance.setAll(createServAttrCom());
		delayTolerance.put("support", DelayToleranceSupport.NOT_SUPPORTED.name().replace("_", " "));

		deterministicComm.setAll(createServAttrCom());
		deterministicComm.put("availability", DeterministicCommAvailability.NOT_SUPPORTED.name().replace("_", " "));
		deterministicComm.putArray("periodicityList").add("string");

		dLThptPerSlice.setAll(createServAttrCom());
		dLThptPerSlice.put("guaThpt", 500);
		dLThptPerSlice.put("maxThpt", 1000);

		dLThptPerUE.setAll(createServAttrCom());
		dLThptPerUE.put("guaThpt", 20);
		dLThptPerUE.put("maxThpt", 50);

		uLThptPerSlice.setAll(createServAttrCom());
		uLThptPerSlice.put("guaThpt", 200);
		uLThptPerSlice.put("maxThpt", 500);

		uLThptPerUE.setAll(createServAttrCom());
		uLThptPerUE.put("guaThpt", 5);
		uLThptPerUE.put("maxThpt", 10);

		maxPktSize.setAll(createServAttrCom());
		maxPktSize.put("maxSize", 512);

		maxNumberofConns.setAll(createServAttrCom());
		maxNumberofConns.put("nOofConn", 100);

		kPIMonitoring.setAll(createServAttrCom());
		kPIMonitoring.putArray("kPIList").add("string");

		userMgmtOpen.setAll(createServAttrCom());
		userMgmtOpen.put("support", "NOT_SUPPORTED");

		v2XCommModels.setAll(createServAttrCom());
		v2XCommModels.put("v2XMode", "NOT_SUPPORTED");

		// 下列兩者擇一使用
		termDensity.setAll(createServAttrCom());
		termDensity.put("density", 0);  // Integer

		return serviceProfile;
	}

	public ObjectNode createServAttrCom()
	{
		final ObjectNode root = JsonNodeFactory.instance.objectNode();
		final ObjectNode servAttrCom = root.putObject("servAttrCom");
		// This attribute specifies the category of a service requirement/attribute of GST
		// allowedValues: character, scalability (Type: String)
		servAttrCom.put("category", Category.character.name());
		// This attribute specifies the tagging of a service requirement/attribute of GST in character category
		// allowedValues: performance, function, operation (Type: String)
		// Condition: It shall be supported if the category is character. Otherwise this attribute shall be absent.
		if (servAttrCom.path("category").isMissingNode() == false && servAttrCom.path("category").asText().equals(Category.character.name()))
		{
			servAttrCom.putArray("periodicityList").add(Tagging.performance.name());
		}
		// allowedValues: API, KPI
		servAttrCom.put("exposure", Exposure.KPI.name());
		return root;
	}

	public enum Category
	{
		character,
		scalability
	}
	public enum Tagging
	{
		performance,
		function,
		operation
	}
	public enum Exposure
	{
		API,
		KPI
	}

	public enum Support
	{
		NOT_SUPPORTED,
		SUPPORTED
	}

	public enum DelayToleranceSupport
	{
		NOT_SUPPORTED,
		SUPPORTED
	}

	public enum DeterministicCommAvailability
	{
		NOT_SUPPORTED,
		SUPPORTED
	}

	public enum UserMgmtOpenSupport
	{
		NOT_SUPPORTED,
		SUPPORTED
	}

	public enum V2XCommModelsV2XMode
	{
		NOT_SUPPORTED,
		SUPPORTED_BY_NR
	}

	public enum MobilityLevel
	{
		STATIONARY,
		NOMADIC,
		RESTRICTED_MOBILITY,
		FULLY_MOBILITY
	}
	public enum SharingLevel
	{
		SHARED,
		NON_SHARED
	}

	public enum NetworkSliceSharingIndicator
	{
		SHARED,
		NON_SHARED
	}

	public enum OperationalState
	{
		ENABLED,
		DISABLED
	}

	public enum AdministrativeState
	{
		LOCKED,
		UNLOCKED
	}

	@Data
	@RequiredArgsConstructor
	@AllArgsConstructor
	@NoArgsConstructor
	@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	public static class Snssai  // TS29571
	{
		@NonNull
		private Integer	sst;  // maximum 255
		private String	sd;   // String.pattern: '^[A-Fa-f0-9]{6}$'
		private Boolean	isDefault;

		public Snssai(final Integer sst, final String sd)
		{
			this.sst = sst;
			this.sd = sd;
		}
	}

	@Data
	@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	public static class PlmnId  // TS29571
	{
		@NonNull
		private String	mcc;  // TS29571 Mcc.pattern: '^\d{3}$'
		@NonNull
		private String	mnc;  // TS29571 Mnc.pattern: '^\d{2,3}$'
	}

	@Data
	@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	public class PlmnInfo
	{
		private PlmnId	plmnId;
		private Snssai	snssai;
	}

	@Data
	@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	public class Tai
	{
		private PlmnId	plmnId;
		private Integer	nrTac;  // maximum: 16777215
	}

	@Data
	@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	class PerfReq
	{

	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	class PerfReqEmbb extends PerfReq
	{
		private Long	expDataRateDL;
		private Long	expDataRateUL;
		private Long	areaTrafficCapDL;
		private Long	areaTrafficCapUL;
		private Long	userDensity;
		private Float	activityFactor;
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	class PerfReqUrllc extends PerfReq
	{
		private Long	cSAvailabilityTarget;
		private String	cSReliabilityMeanTime;
		private Long	expDataRate;
		private String	msgSizeByte;
		private String	transferIntervalTarget;
		private String	survivalTime;
	}

	@Data
	@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	class NetworkSlice
	{
		private OperationalState	operationalState;
		private AdministrativeState	administrativeState;
		private Set<ServiceProfile>	serviceProfileList;
		private String				networkSliceSubnetRef;
	}

	@Data
	class ServiceProfile
	{
		private String							serviceProfileId;
		private Set<PlmnInfo>					plmnInfoList;  // 全部最多 6 個，但最少要有一個是 primary，
		private Integer							maxNumberofUEs;
		private Integer							latency;
		private MobilityLevel					uEMobilityLevel;
		private Integer							sst;  // maximum 255
		private NetworkSliceSharingIndicator	networkSliceSharingIndicator;
		private Integer							availability;
		private DelayTolerance					delayTolerance;
		private DeterministicComm				deterministicComm;
		private ThptBase						dLThptPerSlice;
		private ThptBase						dLThptPerUE;
		private ThptBase						uLThptPerSlice;
		private ThptBase						uLThptPerUE;
		private MaxPktSize						maxPktSize;
		private MaxNumberofConns				maxNumberofConns;
		private KpiMonitoring					kPIMonitoring;
		private UserMgmtOpen					userMgmtOpen;
		private V2XCommModel					v2XCommModels;
		private String							coverageArea;
		private TermDensity						termDensity;
		private Float							activityFactor;
		private Integer							uESpeed;
		private Integer							jitter;
		private String							survivalTime;
		private String							reliability;
	}

	@Data
	class SliceProfile
	{
		private String			sliceProfileId;
		private Set<Snssai>		sNSSAIList;
		private Set<PlmnId>		pLMNIdList;
		private PerfReq			perfReq;
		private Long			maxNumberofUEs;
		private Set<Integer>	coverageAreaTAList;  // TACList or NrTACList
		private Integer			latency;
		private MobilityLevel	uEMobilityLevel;
		private SharingLevel	resourceSharingLevel;
	}

	@Data
	class ServAttrCom
	{
		private Category	category;
		private Tagging		tagging;
		private Exposure	exposure;
	}

	@Data
	class ServAttrComBase
	{
		private ServAttrCom servAttrCom;
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	class ThptBase extends ServAttrComBase
	{
		private Float	guaThpt;
		private Float	maxThpt;
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	class DelayTolerance extends ServAttrComBase
	{
		private Support support;
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	class DeterministicComm extends ServAttrComBase
	{
		private Support	availability;
		private String	periodicityList;
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	class MaxPktSize extends ServAttrComBase
	{
		private Integer maxsize;
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	class MaxNumberofConns extends ServAttrComBase
	{
		private Integer nOofConn;
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	class KpiMonitoring extends ServAttrComBase
	{
		private String kPIList;
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	class UserMgmtOpen extends ServAttrComBase
	{
		private Support support;
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	class V2XCommModel extends ServAttrComBase
	{
		private V2XCommModelsV2XMode v2XMode;
	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	class TermDensity extends ServAttrComBase
	{
		private Integer density;
	}

	public JsonNode createRanSliceProfile(final String ricSliceId, final Integer sst, final String sd)
	{
		final ObjectNode sliceProfile = JsonNodeFactory.instance.objectNode();
		sliceProfile.put("serviceProfileId", "cn=" + ricSliceId);
		sliceProfile.put("description", "");
		sliceProfile.put("maxNumberofUEs", 100);
		sliceProfile.put("coverageArea", 0);
		sliceProfile.put("latency", 50);
		sliceProfile.put("uEMobilityLevel", "stationary");
		sliceProfile.put("resourceSharingLevel", "shared");
		sliceProfile.put("sST", sst);
		sliceProfile.put("availability", 60.1);
		sliceProfile.put("activityFactor", 60.1);
		sliceProfile.put("uESpeed", 3);
		sliceProfile.put("jitter", 10);
		sliceProfile.put("survivalTime", "2021-10-22T02:37:50.302Z");
		sliceProfile.put("reliability", "string");
		sliceProfile.put("maxDLDataVolume", "string");
		sliceProfile.put("maxULDataVolume", "string");

		final ArrayNode snssaiList = sliceProfile.putArray("sNSSAIList");
		final ObjectNode snssai = JsonNodeFactory.instance.objectNode();
		snssai.put("idx", 1);
		snssai.put("sst", sst);
		snssai.put("sd", sd);
		snssaiList.add(snssai);

		final ArrayNode plmnIdList = sliceProfile.putArray("pLMNIdList");
		final ObjectNode plmnId = JsonNodeFactory.instance.objectNode();
		plmnId.put("mcc", "466");
		plmnId.put("mnc", "93");
		plmnIdList.add(plmnId);

		final ObjectNode delayTolerance = sliceProfile.putObject("delayTolerance");
		delayTolerance.put("support", "NOT_SUPPORTED");
		ObjectNode servAttrCom = delayTolerance.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");

		final ObjectNode deterministicComm = sliceProfile.putObject("deterministicComm");
		deterministicComm.put("availability", "NOT_SUPPORTED");
		servAttrCom = deterministicComm.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");
		final ArrayNode periodicityList = deterministicComm.putArray("periodicityList");
		periodicityList.add(0);

		final ObjectNode dLThptPerSlice = sliceProfile.putObject("dLThptPerSlice");
		dLThptPerSlice.put("guaThpt", 500);
		dLThptPerSlice.put("maxThpt", 1000);
		servAttrCom = dLThptPerSlice.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");

		final ObjectNode dLThptPerUE = sliceProfile.putObject("dLThptPerUE");
		dLThptPerUE.put("guaThpt", 20);
		dLThptPerUE.put("maxThpt", 50);
		servAttrCom = dLThptPerUE.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");

		final ObjectNode uLThptPerSlice = sliceProfile.putObject("uLThptPerSlice");
		uLThptPerSlice.put("guaThpt", 200);
		uLThptPerSlice.put("maxThpt", 500);
		servAttrCom = uLThptPerSlice.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");

		final ObjectNode uLThptPerUE = sliceProfile.putObject("uLThptPerUE");
		uLThptPerUE.put("guaThpt", 5);
		uLThptPerUE.put("maxThpt", 10);
		servAttrCom = uLThptPerUE.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");

		final ObjectNode maxPktSize = sliceProfile.putObject("maxPktSize");
		maxPktSize.put("maxSize", 512);
		servAttrCom = maxPktSize.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");

		final ObjectNode maxNumberofPDUSessions = sliceProfile.putObject("maxNumberofPDUSessions");
		maxNumberofPDUSessions.put("nOofPDUSessions", 512);
		servAttrCom = maxNumberofPDUSessions.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");

		final ObjectNode kPIMonitoring = sliceProfile.putObject("kPIMonitoring");
		servAttrCom = kPIMonitoring.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");
		final ArrayNode kPIList = kPIMonitoring.putArray("kPIList");
		kPIList.add("string");

		final ObjectNode userMgmtOpen = sliceProfile.putObject("userMgmtOpen");
		userMgmtOpen.put("support", "NOT_SUPPORTED");
		servAttrCom = userMgmtOpen.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");

		final ObjectNode v2XCommModels = sliceProfile.putObject("v2XCommModels");
		v2XCommModels.put("v2XMode", "NOT_SUPPORTED");
		servAttrCom = v2XCommModels.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");

		final ObjectNode termDensity = sliceProfile.putObject("termDensity");
		termDensity.put("density", 0);
		servAttrCom = termDensity.putObject("servAttrCom");
		servAttrCom.put("category", "character");
		servAttrCom.put("tagging", "performance");
		servAttrCom.put("exposure", "KPI");

		return sliceProfile;
	}
}
