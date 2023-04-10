package tage.input.action;

import net.java.games.input.Event;
import tage.*;

import org.joml.*;

import a3.MyGame;
import a3.ProtocolClient;

public class BwdAction extends AbstractInputAction {
	private MyGame game;
	private GameObject av;
	private Vector3f oldPosition, newPosition;
	private Vector4f bwdDirection;
	private ProtocolClient protClient;

	public BwdAction(MyGame g, ProtocolClient p)
	{
		game = g;
		protClient = p;
	}

	@Override
	public void performAction(float time, Event e)
	{
		av = game.getAvatar();
		oldPosition = av.getWorldLocation();
		bwdDirection = new Vector4f(0f, 0f, 1f, 1f);
		bwdDirection.mul(av.getWorldRotation());
		bwdDirection.mul(-0.01f);
		newPosition = oldPosition.add(bwdDirection.x(), bwdDirection.y(), bwdDirection.z());
		av.setLocalLocation(newPosition);
		protClient.sendMoveMessage(av.getWorldLocation());
	}
}

