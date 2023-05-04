package server;

import java.io.IOException;
import java.util.Scanner;

import tage.Log;

public class NetworkingServer {

	private GameServerUDP thisUDPServer;
	private NPCController npcController;

	public NetworkingServer(int serverPort, String protocol, int logLevel)
	{
		try
		{
			Log.setLogLevel(logLevel);
			if (protocol.toUpperCase().compareTo("UDP") != 0)
			{
				System.out.println("Only UDP is supported at this time.");
				throw new IllegalArgumentException();
			}
			npcController = new NPCController();
			thisUDPServer = new GameServerUDP(serverPort, npcController);
			Log.print("UDP server ready.\n");
			npcController.start();
		} catch (IOException | IllegalArgumentException e)
		{
			e.printStackTrace();
			System.exit(-1);
		}

		Scanner scanner = new Scanner(System.in);
		while (true)
		{
			String scan = scanner.nextLine();

			if (scan.toLowerCase().contains("quit") || scan.toLowerCase().equals("q"))
			{
				System.out.println("Shutting down.");
				try
				{
					thisUDPServer.shutdown();
					scanner.close();
					System.exit(0);
				} catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args)
	{
		if (args.length == 1)
		{
			new NetworkingServer(Integer.parseInt(args[0]), "UDP", 0);
		} else if (args.length == 2)
		{
			new NetworkingServer(Integer.parseInt(args[0]), args[1], 0);
		} else if (args.length == 3)
		{
			new NetworkingServer(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]));
		} else
		{
			new NetworkingServer(6010, "UDP", 0);
		}
	}

}
