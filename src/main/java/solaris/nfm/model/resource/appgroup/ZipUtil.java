package solaris.nfm.model.resource.appgroup;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZipUtil
{

	public static List<File> unzipForYaml(final MultipartFile file) throws Exception
	{
		// 解zip
		final BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(file.getBytes()));
		final ZipInputStream zip = new ZipInputStream(bis, Charset.forName("UTF-8"));
		ZipEntry ze;
		final List<File> files = new ArrayList<>();
		while ((ze = zip.getNextEntry()) != null)
		{
			if (!ze.isDirectory())
			{
				String zeName = "";
				// 篩選檔名有yaml的檔名，去除資料夾.
				if (ze.getName().contains("/"))
				{
					final String[] stringArray = ze.getName().split("/");
					for (final String items : stringArray)
					{
						if (items.contains("yaml") || items.contains("yml"))
						{
							zeName = items;
						}
					}
				}

				if (!zeName.endsWith(".yaml") && !zeName.contains("yml"))
				{
					continue;
				}
				// mac壓縮zip檔時會自動生成.
				if (zeName.contains("__MACOSX"))
				{
					continue;
				}

				final File f = new File(zeName);
				log.info("Unzipping to  {}", f.getAbsolutePath());
				final FileOutputStream fos = new FileOutputStream(f);
				IOUtils.copy(zip, fos);
				files.add(f);
				fos.close();
			}
		}
		return files;
	}
}