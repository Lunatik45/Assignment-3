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
			printf("Log level set to %d\n", level);
		}
	}

	/**
	 * Log with printf. This will always output.
	 * 
	 * @param s    Formatted string
	 * @param args Input for the formatted string
	 */
	public static void print(String s, Object... args)
	{
		printf(s, args);
	}

	public static void debug(String s, Object... args)
	{
		if (level >= 1)
		{
			printf(s, args);
		}
	}

	public static void verbose(String s, Object... args)
	{
		if (level >= 2)
		{
			printf(s, args);
		}
	}

	public static void trace(String s, Object... args)
	{
		if (level >= 3)
		{
			printf(s, args);
		}
	}

	private static void printf(String s, Object... args)
	{
		try
		{
			System.out.printf(s, args);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
