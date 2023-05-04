package a3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.Log;
import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient {

	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;

	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame game)
			throws IOException
	{
		super(remoteAddr, remotePort, protocolType);
		this.game = game;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
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
			return;
		}

		// Game specific protocol to handle the message
		if (messageTokens.length > 0)
		{
			// Handle JOIN message
			// Format: (join,success) or (join,failure)
			if (messageTokens[0].compareTo("join") == 0)
			{
				if (messageTokens[1].compareTo("success") == 0)
				{
					System.out.println("join success confirmed");
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition(), game.getPlayerRotation(), game.getAvatarSelection());
				}
				if (messageTokens[1].compareTo("failure") == 0)
				{
					System.out.println("join failure confirmed");
					game.setIsConnected(false);
				}
			}

			// Handle BYE message
			// Format: (bye,remoteId)
			if (messageTokens[0].compareTo("bye") == 0)
			{ // remove ghost avatar with id = remoteId
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}

			// Handle CREATE message
			// Format: (create,remoteId,x,y,z)
			// AND
			// Handle DETAILS_FOR message
			// Format: (dsfr,remoteId,x,y,z)
			if (messageTokens[0].compareTo("create") == 0 || (messageTokens[0].compareTo("dsfr") == 0))
			{ // create a new ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);

				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]), Float.parseFloat(messageTokens[4]));

				Matrix4f rotation = new Matrix4f();
				int i = 5;
				for (int j = 0; j < 4; j++)
				{
					for (int k = 0; k < 4; k++)
					{
						rotation.set(j, k, Float.parseFloat(messageTokens[i++]));
					}
				}

				String textureSelection = messageTokens[i];

				try
				{
					ghostManager.createGhostAvatar(ghostID, ghostPosition, rotation, textureSelection);
				} catch (IOException e)
				{
					System.out.println("error creating ghost avatar");
				}
			}

			// Handle WANTS_DETAILS message
			// Format: (wsds,remoteId)
			if (messageTokens[0].compareTo("wsds") == 0)
			{
				// Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition(), game.getPlayerRotation(),
						game.getAvatarSelection());
			}

			// Handle MOVE message
			// Format: (move,remoteId,x,y,z)
			if (messageTokens[0].compareTo("move") == 0)
			{
				// move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);

				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(Float.parseFloat(messageTokens[2]),
						Float.parseFloat(messageTokens[3]), Float.parseFloat(messageTokens[4]));

				Matrix4f rotation = new Matrix4f();
				int i = 5;
				for (int j = 0; j < 4; j++)
				{
					for (int k = 0; k < 4; k++)
					{
						rotation.set(j, k, Float.parseFloat(messageTokens[i++]));
					}
				}
				
				ghostManager.updateGhostAvatar(ghostID, ghostPosition, rotation);
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

	public void sendCreateMessage(Vector3f position, Matrix4f rotation, String textureSelection)
	{
		try
		{
			String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			for (int i = 0; i < 4; i++)
			{
				for (int j = 0; j < 4; j++)
				{
					message += "," + rotation.get(i, j);
				}
			}

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

	public void sendDetailsForMessage(UUID remoteId, Vector3f position, Matrix4f rotation, String textureSelection)
	{
		try
		{
			String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			for (int i = 0; i < 4; i++)
			{
				for (int j = 0; j < 4; j++)
				{
					message += "," + rotation.get(i, j);
				}
			}

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

	public void sendMoveMessage(Vector3f position, Matrix4f rotation)
	{
		try
		{
			String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			for (int i = 0; i < 4; i++)
			{
				for (int j = 0; j < 4; j++)
				{
					message += "," + rotation.get(i, j);
				}
			}

			sendPacket(message);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
