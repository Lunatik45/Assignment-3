package a3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.UUID;

import org.joml.Vector2f;
import org.joml.Vector3f;

import tage.Log;
import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient {

	private MyGame game;
	private GhostManager ghostManager;
	private NpcManager npcManager;
	private UUID id;

	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game)
			throws IOException
	{
		super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
		npcManager = game.getNpcManager();
	}

	public UUID getID()
	{
		return id;
	}

	@Override
	protected void processPacket(Object message)
	{
		String strMessage = (String) message;
		Log.trace("Message recieved: %s\n", strMessage);

		String[] messageTokens;
		try
		{
			messageTokens = strMessage.split(",");
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.print("Could not process message %s\n", strMessage);
			return;
		}

		if (messageTokens.length > 0)
		{
			// Handle JOIN message
			if (messageTokens[0].compareTo("join") == 0)
			{
				if (messageTokens[1].compareTo("success") == 0)
				{
					System.out.println("join success confirmed");
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition(), game.getPlayerLookAt(), game.getAvatarSelection());
					sendGetNpcMessage();
				}
				if (messageTokens[1].compareTo("failure") == 0)
				{
					System.out.println("join failure confirmed");
					game.setIsConnected(false);
				}
			}

			// Handle BYE message
			if (messageTokens[0].compareTo("bye") == 0)
			{ 
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}

			// Handle CREATE message and DETAILS_FOR message
			if (messageTokens[0].compareTo("create") == 0 || (messageTokens[0].compareTo("dsfr") == 0))
			{ 
				UUID ghostID = UUID.fromString(messageTokens[1]);

				Vector3f ghostPosition = new Vector3f(Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]), Float.parseFloat(messageTokens[4]));
				Vector3f lookat = new Vector3f(Float.parseFloat(messageTokens[5]), Float.parseFloat(messageTokens[6]),
						Float.parseFloat(messageTokens[7]));
				
				String textureSelection = messageTokens[8];

				try
				{
					ghostManager.createGhostAvatar(ghostID, ghostPosition, lookat, textureSelection);
				} catch (IOException e)
				{
					System.out.println("error creating ghost avatar");
				}
			}

			// Handle WANTS_DETAILS message
			if (messageTokens[0].compareTo("wsds") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition(), game.getPlayerLookAt(), game.getAvatarSelection());
			}

			// Handle MOVE message
			if (messageTokens[0].compareTo("move") == 0)
			{
				UUID ghostID = UUID.fromString(messageTokens[1]);

				Vector3f ghostPosition = new Vector3f(Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]), Float.parseFloat(messageTokens[4]));
				Vector3f lookat = new Vector3f(Float.parseFloat(messageTokens[5]),
						Float.parseFloat(messageTokens[6]), Float.parseFloat(messageTokens[7]));

				ghostManager.updateGhostAvatar(ghostID, ghostPosition, lookat, Float.parseFloat(messageTokens[8]));
			}

			// Handle CREATE_NPC message
			if (messageTokens[0].compareTo("createnpc") == 0)
			{
				Log.debug("creating npc\n");
				Vector3f npcPosition = new Vector3f(Float.parseFloat(messageTokens[1]),
						Float.parseFloat(messageTokens[2]), Float.parseFloat(messageTokens[3]));
				Vector3f lookat = new Vector3f(Float.parseFloat(messageTokens[4]),
						Float.parseFloat(messageTokens[5]), Float.parseFloat(messageTokens[6]));

				try
				{
					npcManager.createGhostAvatar(npcPosition, lookat);
				} catch (IOException e)
				{
					System.out.println("error creating ghost avatar");
				}
			}

			// Handle GET_NPC_TARGETS message
			if (messageTokens[0].compareTo("getnpctargets") == 0)
			{
				Log.debug("getting npc targets\n");
				ArrayList<Vector2f> targets = game.getNpcTargets();
				sendNpcTargetsMessage(targets);
				game.setPrimaryNpcHandler();
			}

			// Handle NPC_STATUS message
			if (messageTokens[0].compareTo("npcstatus") == 0)
			{
				// Vector3f position = new Vector3f(Float.parseFloat(messageTokens[1]), Float.parseFloat(messageTokens[2]),
				// 		Float.parseFloat(messageTokens[3]));
				// Vector3f lookat = new Vector3f(Float.parseFloat(messageTokens[4]), Float.parseFloat(messageTokens[5]),
				// 		Float.parseFloat(messageTokens[6]));
				// boolean wantsAccel = Boolean.parseBoolean(messageTokens[7]);
				// boolean wantsDecel = Boolean.parseBoolean(messageTokens[8]);
				// boolean wantsTurnLeft = Boolean.parseBoolean(messageTokens[9]);
				// boolean wantsTurnRight = Boolean.parseBoolean(messageTokens[10]);
				boolean wantsAccel = messageTokens[7].equals("1");
				boolean wantsDecel = messageTokens[8].equals("1");
				boolean wantsTurnLeft = messageTokens[9].equals("1");
				boolean wantsTurnRight = messageTokens[10].equals("1");
				
				npcManager.updateNpcStatus(wantsAccel, wantsDecel, wantsTurnLeft, wantsTurnRight);
			}

			if (messageTokens[0].compareTo("npcmove") == 0)
			{
				Log.debug("handling npc move\n");
				Vector3f position = new Vector3f(Float.parseFloat(messageTokens[1]),
						Float.parseFloat(messageTokens[2]), Float.parseFloat(messageTokens[3]));
				Vector3f lookat = new Vector3f(Float.parseFloat(messageTokens[4]), Float.parseFloat(messageTokens[5]),
						Float.parseFloat(messageTokens[6]));
				
				npcManager.updateNpcAvatar(position, lookat);
			}
		}
	}

	// The initial message from the game client requesting to join the
	// server. localId is a unique identifier for the client. Recommend
	// a random UUID.
	// Message Format: (join,localId)
	public void sendJoinMessage()
	{
		try
		{
			sendPacket(new String("join," + id.toString()));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	// Informs the server that the client is leaving the server.
	// Message Format: (bye,localId)

	public void sendByeMessage()
	{
		try
		{
			sendPacket(new String("bye," + id.toString()));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	// Informs the server of the client’s Avatar’s position. The server
	// takes this message and forwards it to all other clients registered
	// with the server.
	// Message Format: (create,localId,x,y,z) where x, y, and z represent the
	// position

	public void sendCreateMessage(Vector3f position, Vector3f lookat, String textureSelection)
	{
		try
		{
			String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + lookat.x();
			message += "," + lookat.y();
			message += "," + lookat.z();

			message += "," + textureSelection;

			sendPacket(message);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	// Informs the server of the local avatar's position. The server then
	// forwards this message to the client with the ID value matching remoteId.
	// This message is generated in response to receiving a WANTS_DETAILS message
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z) where x, y, and z represent the
	// position.

	public void sendDetailsForMessage(UUID remoteId, Vector3f position, Vector3f lookat, String textureSelection)
	{
		try
		{
			String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + lookat.x();
			message += "," + lookat.y();
			message += "," + lookat.z();
			message += "," + textureSelection;

			sendPacket(message);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	// Informs the server that the local avatar has changed position.
	// Message Format: (move,localId,x,y,z) where x, y, and z represent the
	// position.

	public void sendMoveMessage(Vector3f position, Vector3f lookat, float pitch)
	{
		try
		{
			String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			message += "," + lookat.x();
			message += "," + lookat.y();
			message += "," + lookat.z();
			message += "," + pitch;

			sendPacket(message);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	
	private void sendNpcTargetsMessage(ArrayList<Vector2f> targets)
	{
		try
		{
			String message = new String("npctargets");
			
			for (Vector2f target : targets)
			{
				
				message += "," + String.format("%.2f", target.x());
				message += "," + String.format("%.2f", target.y());
			}

			sendPacket(message);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void sendNpcMoveMessage(Vector3f position, Vector3f lookat, float pitch)
	{
		try
		{
			String message = new String("npcmove," + id.toString());
			message += String.format(",%.2f", position.x());
			message += String.format(",%.2f", position.y());
			message += String.format(",%.2f", position.z());
			message += String.format(",%.2f", lookat.x());
			message += String.format(",%.2f", lookat.y());
			message += String.format(",%.2f", lookat.z());
			message += "," + pitch;

			sendPacket(message);

			// Log.debug("Sent NPC move message\n");
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void sendGetNpcMessage()
	{
		try
		{
			sendPacket(new String("getnpc," + id.toString()));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
