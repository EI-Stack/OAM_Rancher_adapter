package solaris.nfm.controller;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.capability.annotation.jsonvalid.JsonService;
import solaris.nfm.capability.annotation.jsonvalid.JsonValidationService;
import solaris.nfm.capability.rest.EncryptService;
import solaris.nfm.capability.system.MailService;
import solaris.nfm.service.FgcService;
import solaris.nfm.service.SliceService;
import solaris.nfm.service.SliceService.PlmnId;
import solaris.nfm.util.RsaUtil;
import solaris.nfm.util.SerializationUtil;

@RestController
@RequestMapping("")
@Slf4j
public class SystemCtr
{
	// @Autowired
	// private DeviceService deviceService;
	@Autowired
	private ObjectMapper			objectMapper;
	@Autowired
	private MailService				mailService;
	@Autowired
	private SliceService			sliceSrv;
	@Autowired
	private FgcService				fgcSrv;
	@Autowired
	private JsonValidationService	jsonSchemaService;
	@Autowired
	private JsonService				jsonService;
	@Autowired
	private EncryptService			encryptService;

	@PostMapping(value = "/hooks/Elastalert")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void ReceiveWebhook(@RequestBody final JsonNode json) throws Exception
	{
		log.debug("\t Receive webhook from Affirmed ElasticSearch: \n{}", json.toPrettyString());
	}

	@GetMapping(value = "/v1/test3")
	@ResponseStatus(HttpStatus.OK)
	public void test3(@RequestBody final JsonNode json) throws Exception
	{
		String plaintext = "Postman Tester";
		log.debug("[{}] Decrypted data (HEX String): {}", plaintext, encryptService.encrypt(plaintext));
		plaintext = "9";
		log.debug("[{}] Decrypted data (HEX String): {}", plaintext, encryptService.encrypt(plaintext));
		plaintext = "140.92.10.99";
		log.debug("[{}] Decrypted data (HEX String): {}", plaintext, encryptService.encrypt(plaintext));
	}

	@GetMapping(value = "/v1/test4")
	@ResponseStatus(HttpStatus.OK)
	public void test4(@RequestBody final JsonNode json) throws Exception
	{
		String encryptedDataHexString = "08f9efede18cf28268e06bd86eedf249";
		log.debug("[{}] Plain Text: {}", encryptedDataHexString, encryptService.decrypt(encryptedDataHexString));
		encryptedDataHexString = "a0113cee08dba2d3919052b2faa1236d";
		log.debug("[{}] Plain Text: {}", encryptedDataHexString, encryptService.decrypt(encryptedDataHexString));
		encryptedDataHexString = "f9b21abc9b3cfab1a4bae2c2705d1529";
		log.debug("[{}] Plain Text: {}", encryptedDataHexString, encryptService.decrypt(encryptedDataHexString));
	}

	@GetMapping(value = "/v1/test5")
	@ResponseStatus(HttpStatus.OK)
	public void test5() throws Exception
	{
		log.debug("RSA 非對稱加密測試");
		log.debug("情境測試 - 典型測試");
		KeyPair keyPair = RsaUtil.createKeyPair();
		String publicKeyWithBase64 = RsaUtil.getPublicKeyWithBase64(keyPair);
		log.debug("Public Key (Base64)=[{}]", publicKeyWithBase64);
		String privateKeyWithBase64 = RsaUtil.getPrivateKeyWithBase64(keyPair);
		log.debug("Private Key (Base64)=[{}]", privateKeyWithBase64);
		String plaintext = "1234567890abcdefgh";
		// 公鑰加密
		String encryptedString = RsaUtil.encryptString(keyPair.getPublic(), plaintext);
		log.debug("公鑰加密 Encrypted String (Base64)=[{}]", encryptedString);
		// 私鑰解密
		String decryptedString = RsaUtil.decryptString(keyPair.getPrivate(), encryptedString);
		log.debug("私鑰解密 Decrypted String=[{}]", decryptedString);

		log.debug("情境測試 - 字串加解密");
		PublicKey importedPublicKey = RsaUtil.importPublicKey(publicKeyWithBase64);
		// 私鑰加密
		encryptedString = RsaUtil.encryptString(keyPair.getPrivate(), plaintext);
		log.debug("私鑰加密 Encrypted String (Base64)=[{}]", encryptedString);
		// 公鑰解密
		decryptedString = RsaUtil.decryptString(importedPublicKey, encryptedString);
		log.debug("公鑰解密 Decrypted String=[{}]", decryptedString);

		log.debug("情境測試 - JsonNode 加解密");
		final ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.put("regionId", "a");
		result.put("setId", "b");
		result.put("pointer", "c");
		log.debug("JSON=[{}]", result.toPrettyString());
		byte[] plainBytes = SerializationUtil.serializeObjectToByteArray(result);
		// 私鑰加密
		byte[] encryptedBytes = RsaUtil.encryptByteArray(keyPair.getPrivate(), plainBytes);
		log.debug("私鑰加密 Encrypted String (Base64)=[{}]", Base64.getEncoder().encodeToString(encryptedBytes));
		// 公鑰解密
		byte[] decryptedBytes = RsaUtil.decryptByteArray(importedPublicKey, encryptedBytes);
		log.debug("公鑰解密 Decrypted String=[{}]", ((JsonNode) SerializationUtil.deserializeObject(decryptedBytes)).toPrettyString());
	}

	@PostMapping(value = "/v1/test2")
	@ResponseStatus(HttpStatus.OK)
	public void test2(@RequestBody final JsonNode json) throws Exception
	{
		final JsonNode fight = jsonService.getJsonNodeFromClasspathForYaml("json-schema/R16/TS29571_CommonData.yaml").path("components").path("schemas").path("Supi");
		log.debug("Input Json={}", fight.toPrettyString());

		// log.debug("Input Json={}", json.toPrettyString());
		// jsonSchemaService.validate("schema.yaml", json);

		// json valid檢查格式
		// final JsonNode schemaNode = baseJsonSchemaValidator.getJsonNodeFromClasspath("json-schema/schema.yaml");
		// final JsonSchema schema = baseJsonSchemaValidator.getJsonSchemaFromJsonNodeAutomaticVersion(schemaNode);

		// final ObjectMapper yamlObjMapper = new ObjectMapper(new YAMLFactory());
		// final JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)).objectMapper(yamlObjMapper)
		// .build(); /* Using draft-07. You can choose anyother draft. */
		// final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("json-schema/schema.yaml");
		// final JsonSchema schema = factory.getSchema(is);

		// final String emailContent = MessageFormat.format("Network: {0}\nSeverity: {1}\nError Code: {2}\nDescription: {3}\nEvent Time: {4}\nSOP: {5}", "00", "11", "22", "33", "44", "55");
		// log.debug("emailContent={}", emailContent);
		// mailService.sendPm(Set.of("holisun@gmail.com"), emailContent);
	}

	@GetMapping(value = "/v1/test")
	@ResponseStatus(HttpStatus.OK)
	public void test() throws Exception
	{
		// final ObjectNode json = fgcSrv.createTest();
		// final AuthenticationSubscription dto = objectMapper.convertValue(json.path("AuthenticationSubscription"), AuthenticationSubscription.class);
		// log.debug("123={}", dto.getAuthenticationMethod());

		final PlmnId plmnId = new PlmnId("001", "35");
		final String imsi = "001010000000001";
		final String supi = "imsi-" + imsi;
		log.debug("json12=\n{}", this.fgcSrv.createUeProfileForAffirmed(plmnId, supi).toPrettyString());

		// log.debug("={}", this.deviceService.getPmMeasureFileDirPath(bean, 0L, 1L));
		// log.debug("={}", this.sliceSrv.createServiceProfile("id").toPrettyString());

		// final ObjectNode json = JsonNodeFactory.instance.objectNode();
		// // fault
		// final ObjectNode fault = json.putObject("fault");
		// fault.put("isSubscribe", true);
		// fault.putArray("filters").add("CRITICAL").add("MAJOR").add("MINOR").add("WARNING");
		//
		// final ObjectNode performance = json.putObject("performance");
		// performance.put("isSubscribe", true);
		// performance.putArray("filters").add("transciver").add("rx-window").add("tx");
		//
		// final ObjectNode file = json.putObject("file");
		// final ObjectNode fileUpload = file.putObject("upload");
		// fileUpload.put("isSubscribe", true);
		// fileUpload.putArray("events").add(JsonNodeFactory.instance.objectNode().put("local-logical-file-path", "local").put("remote-file-path", "remote").put("time", "2021-06-28T10:15:00"));
		// final ObjectNode download = file.putObject("download");
		// download.put("isSubscribe", true);
		// download.putArray("events").add(JsonNodeFactory.instance.objectNode().put("local-logical-file-path", "local").put("remote-file-path", "remote").put("time", "2021-06-28T10:15:00"));
		//
		// final ObjectNode software = json.putObject("software");
		// software.putObject("build");
		// final ObjectNode smDownload = software.putObject("download");
		// smDownload.put("isSubscribe", true);
		// smDownload.putArray("events").add(JsonNodeFactory.instance.objectNode().put("remote-file-path", "remote").put("time", "2021-06-28T10:15:00"));
		// final ObjectNode smInstall = software.putObject("install");
		// smInstall.put("isSubscribe", true);
		// smInstall.putArray("events").add(JsonNodeFactory.instance.objectNode().put("slot-name", "slot #1").put("time", "2021-06-28T10:15:00"));
		// final ObjectNode smActivation = software.putObject("activation");
		// smActivation.put("isSubscribe", true);
		// smActivation.putArray("events").add(JsonNodeFactory.instance.objectNode().put("slot-name", "slot #1").put("time", "2021-06-28T10:15:00"));
		//
		// log.debug("json={}", json.toPrettyString());
	}
}