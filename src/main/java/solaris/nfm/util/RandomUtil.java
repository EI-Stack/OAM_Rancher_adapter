package solaris.nfm.util;

public class RandomUtil
{
	public static String getRandomString(final int length)
	{
		String randomString = "";
		String[] randomElements =
			{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
					"m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
		for (int i = 0; i < length; i++)
			randomString += randomElements[(int) (Math.random() * randomElements.length)];
		return randomString;
	}
}