package solaris.nfm.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RandomPW {
	private static int length = 8;

	public static String getRandomPath() {
		length = 4;
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		Date current = new Date();
		String randomPath = sdFormat.format(current) + getRandom();
		return randomPath;
	}

	public static String getRandom(final int strLength) {
		length = strLength;
		return getRandom();
	}

	public static String getRandom() {
		java.util.Random r = new java.util.Random();
		int rnd = 0;
		String str = "";
		for (int i = 0; i < length; i++) {
			if (RandomPW.getNumChar()) {
				rnd = r.nextInt(9);
				str += rnd;
			} else {
				rnd = r.nextInt(52);
				if (rnd < 26) {
					str += ((char) (rnd + 65));
				} else {
					str += ((char) (rnd - 26 + 97));
				}
			}
		}
		return str;
	}

	public static boolean getNumChar() {// 8 number 2 char
		java.util.Random r = new java.util.Random();
		if (r.nextInt(100) > 20)
			return true;
		else
			return false;
	}
}
