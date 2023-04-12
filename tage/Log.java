package tage;

/**
 * A simple logging class to easily debug the class during runtime.
 */
public class Log {

	private static int level = -1;

	/**
	 * Set the log level. 0 is None.
	 * 
	 * @param logLevel (None, Debug, Verbose, Trace)
	 */
	public static void setLogLevel(int logLevel)
	{
		if (level != logLevel)
		{
			level = logLevel;
			System.out.printf("Log level set to %d\n", level);
		}
	}	

	public static void debug(String s, Object... args)
	{
		if (level >= 1)
		{
			System.out.printf(s, args);
		}
	}

	public static void verbose(String s, Object... args)
	{
		if (level >= 2)
		{
			System.out.printf(s, args);
		}
	}

	public static void trace(String s, Object... args)
	{
		if (level >= 3)
		{
			System.out.printf(s, args);
		}
	}

}
