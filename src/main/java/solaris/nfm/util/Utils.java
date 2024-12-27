package solaris.nfm.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Utils
{
	private static Logger logger = LoggerFactory.getLogger(Utils.class.getName());

	public static String getClassPath() throws Exception
	{
		final Pattern pattern = Pattern.compile("\\/(\\S+)");
		final Matcher matcher = pattern.matcher(Thread.currentThread().getContextClassLoader().getResource("").getPath().toString());
		if (matcher.find())
		{
			return matcher.group(1);
		}
		throw new Exception("\t [Find class path] ...... Failed !!");
	}

	public static String executeCommandLine(final String command)
	{
		final StringBuffer commandLineResponeSB = new StringBuffer();
		try
		{
			String lineResponeMessage;
			final Process process = Runtime.getRuntime().exec(command);
			final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "MS950"));
			while ((lineResponeMessage = bufferedReader.readLine()) != null)
			{
				commandLineResponeSB.append(lineResponeMessage + "\n");
			}
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
		logger.debug("\t Command Line Respone =[" + commandLineResponeSB + "]");
		return commandLineResponeSB.toString();
	}

	public static void backupDB(final Map argumentMap)
	{
		final List<String> commandList = new ArrayList<>();
		final String backupCMD = "\"C:\\Program Files\\PostgreSQL\\8.4\\bin\\pg_dump.exe\"";
		commandList.add(backupCMD);
		commandList.add("--host");
		commandList.add("140.92.13.220");
		commandList.add("--port");
		commandList.add("5432");
		commandList.add("--username");
		commandList.add("postgres");
		commandList.add("--format");
		commandList.add("custom");
		commandList.add("--blobs");
		commandList.add("--ignore-version");
		commandList.add("--verbose");
		commandList.add("--file");
		commandList.add("\"C:\\ArcMatrix.backup\"");
		commandList.add("funambol");
		final ProcessBuilder processBuilder = new ProcessBuilder(commandList);
		final Map<String, String> Envm = processBuilder.environment();
		Envm.put("PGPASSWORD", "helloworld");
		try
		{
			final Process process = processBuilder.start();
		} catch (final IOException e)
		{
			e.printStackTrace();
		}
		final String backupFileName = "C:\\ArcMatrix.backup";
		final File backupFile = new File(backupFileName);
		final long size = backupFile.length();
		if (size == 0)
		{
			System.out.println("Backup database failures!");
		} else
		{
			System.out.println("Backup database succeeds!");
		}
	}

	public static File getFile(final String filePath, final String fileName) throws Exception
	{
		final String emsFileRootPath = Utils.getFileRootPath();
		final String fileFullPath = emsFileRootPath + filePath;
		return new File(fileFullPath + fileName);
	}

	public static boolean createDirectory(final String dirName) throws Exception
	{
		final String emsFileRootPath = Utils.getFileRootPath();
		final String dirFullName = emsFileRootPath + dirName;
		return new File(dirFullName).mkdir();
	}

	public static boolean isExist(final String dirName) throws Exception
	{
		final String emsFileRootPath = Utils.getFileRootPath();
		final String dirFullName = emsFileRootPath + dirName;
		return new File(dirFullName).exists();
	}

	public static String getFileRootPath() throws Exception
	{
		String fileRootPath = null;
		final Matcher matcher = Pattern.compile("^(\\S+/)EMS/[^/]+/classes/$|^(\\S+/)EMS/src/$").matcher(Utils.getClassPath());
		if (!matcher.find())
		{
			throw new Exception("Can not find EMSFileRootPath.");
		}
		if (matcher.group(1) != null)
		{
			fileRootPath = matcher.group(1) + "EMSFileRoot/";
		} else
		{
			fileRootPath = matcher.group(2) + "EMSFileRoot/";
		}
		logger.trace("\t emsFileRootPath=" + fileRootPath);
		return fileRootPath;
	}

	public static void createFolder(final String folderPath)
	{
		/**
		 * 新建目录
		 *
		 * @param folderPath
		 *        String ex: c:/fqf
		 * @return void
		 */
		try
		{
			String filePath = folderPath;
			filePath = filePath.toString();
			final File myFilePath = new File(filePath);
			if (!myFilePath.exists())
			{
				myFilePath.mkdirs();
			}
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	public static boolean isValidWindowsFileName(final String text)
	{
		final Pattern pattern = Pattern.compile("# Match a valid Windows filename (unspecified file system).          \n" + "^                                # Anchor to start of string.        \n"
				+ "(?!                              # Assert filename is not: CON, PRN, \n" + "  (?:                            # AUX, NUL, COM1, COM2, COM3, COM4, \n"
				+ "    CON|PRN|AUX|NUL|             # COM5, COM6, COM7, COM8, COM9,     \n" + "    COM[1-9]|LPT[1-9]            # LPT1, LPT2, LPT3, LPT4, LPT5,     \n"
				+ "  )                              # LPT6, LPT7, LPT8, and LPT9...     \n" + "  (?:\\.[^.]*)?                  # followed by optional extension    \n"
				+ "  $                              # and end of string                 \n" + ")                                # End negative lookahead assertion. \n"
				+ "[^<>:\"/\\\\|?*\\x00-\\x1F]*     # Zero or more valid filename chars.\n" + "[^<>:\"/\\\\|?*\\x00-\\x1F\\ .]  # Last char is not a space or dot.  \n"
				+ "$                                # Anchor to end of string.            ", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.COMMENTS);
		final Matcher matcher = pattern.matcher(text);
		final boolean isMatch = matcher.matches();
		return isMatch;
	}

	/**
	 * 將 tag-name 轉換成 tagName
	 */
	public static JsonNode xmlTagNameCastToCamelCase(final JsonNode input)
	{
		final Matcher matchKey = Pattern.compile("\"(.+?)\" ?: ?").matcher(input.toPrettyString());
		final String result = matchKey.replaceAll(s -> {
			final Matcher matchDash = Pattern.compile("-[a-zA-Z]").matcher(s.group().toLowerCase());
			return matchDash.replaceAll(d -> d.group().substring(1).toUpperCase());
		});
		final ObjectMapper objectMapper = new ObjectMapper();
		try
		{
			return objectMapper.readTree(result);
		} catch (final JsonProcessingException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}