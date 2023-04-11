package server;

import java.io.IOException;
import java.util.Scanner;

import tage.networking.IGameConnection.ProtocolType;

public class NetworkingServer {
	private GameServerUDP thisUDPServer;
	private GameServerTCP thisTCPServer;
	private boolean serverIsUdp;

	public NetworkingServer(int serverPort, String protocol)
	{
		try
		{
			if (protocol.toUpperCase().compareTo("TCP") == 0)
			{
				thisTCPServer = new GameServerTCP(serverPort);
				serverIsUdp = false;
				System.out.println("TCP server ready.");
			} else
			{
				thisUDPServer = new GameServerUDP(serverPort);
				System.out.println("UDP server ready.");
				serverIsUdp = true;
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}

		Scanner scanner = new Scanner(System.in);
		while (true)
		{
			System.out.print("server> ");
			String scan = scanner.nextLine();

			if (scan.toLowerCase().contains("quit") || scan.toLowerCase().equals("q"))
			{
				System.out.println("Shutting down.");
				try
				{
					if (serverIsUdp)
					{
						thisUDPServer.shutdown();
					}
					else
					{
						thisTCPServer.shutdown();
					}
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
