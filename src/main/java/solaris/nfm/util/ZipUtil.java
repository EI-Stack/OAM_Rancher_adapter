package solaris.nfm.util;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.exception.base.ExceptionBase;

@Slf4j
public class ZipUtil
{
	public static JsonNode unzipAndExtract(final MultipartFile multipartFile, final ObjectMapper objectMapper) throws IOException, ExceptionBase
	{
		final String propertyFileName = "file-property.txt";
		// final Map<String, String> resultMap = new HashMap<>(2);

		// ---[ 將 multipartFile 轉型別成 File ]----------------------------------------------------------------------------[S]
		//		final String originalFileName = multipartFile.getOriginalFilename();
		//		final File zipFile = File.createTempFile("temp-zipped-linux-app-", originalFileName);
		//		zipFile.deleteOnExit();
		//		FileCopyUtils.copy(multipartFile.getBytes(), zipFile);
		// ---[ 將 multipartFile 轉型別成 File ]----------------------------------------------------------------------------[E]

		// ---[ 從 multipartFile 取出單一檔案 ]-------------------------------------------------------------------------------[S]
		//		final ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
		final ZipInputStream zipInputStream = new ZipInputStream(multipartFile.getInputStream());
		byte[] contentByteArray = null;
		try (zipInputStream)
		{
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			boolean isFileFound = false;
			while (zipEntry != null)
			{
				if (zipEntry.getName().equals(propertyFileName) == false)
				{
					zipEntry = zipInputStream.getNextEntry();
					continue;
				} else
				{
					isFileFound = true;
					break;
				}
			}

			if (isFileFound == false) throw new ExceptionBase(propertyFileName + " is non-existed.");

			contentByteArray = zipInputStream.readAllBytes();
			if (contentByteArray == null) throw new ExceptionBase("Byte array in " + propertyFileName + " is null.");
		} catch (Exception e)
		{
			zipInputStream.closeEntry();
			zipInputStream.close();
			throw e;
		}
		// ---[ 從 multipartFile 取出單一檔案 ]-------------------------------------------------------------------------------[E]

		log.debug("str={}", new String(contentByteArray));
		JsonNode rootNode = objectMapper.readTree(new String(contentByteArray));
		log.debug("rootNode={}", rootNode);

		//final String[] fileProperties = new String(contentByteArray).split("\\r\\n");
		// 前兩行是 Server 端所需的資料，第 3 行是 Agent 所需的資料
		//if (fileProperties.length < 2) throw new ExceptionBase(propertyFileName + " format is wrong. The line number (" + fileProperties.length + ") must be large than 1.");

		//final String packageName = fileProperties[0];
		//if (StringUtils.hasText(packageName) == false) throw new ExceptionBase("Package name in " + propertyFileName + " must not be blank.");
		//final String packageVersion = fileProperties[1];
		//if (StringUtils.hasText(packageVersion) == false) throw new ExceptionBase("Package version in " + propertyFileName + " must not be blank.");

		//resultMap.put("packageName", packageName.trim());
		//resultMap.put("packageVersion", packageVersion.trim());

		return rootNode;
	}
}