package server;

import java.io.IOException;
import java.util.Scanner;

import tage.networking.IGameConnection.ProtocolType;

public class NetworkingServer {

	private GameServerUDP thisUDPServer;

	public NetworkingServer(int serverPort, String protocol)
	{
		try
		{
			if (protocol.toUpperCase().compareTo("UDP") != 0)
			{
				System.out.println("Only UDP is supported at this time.");
				throw new IllegalArgumentException();
			}
			thisUDPServer = new GameServerUDP(serverPort);
			System.out.println("UDP server ready.");
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
		if (args.length > 1)
		{
			NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
		}
	}

}
