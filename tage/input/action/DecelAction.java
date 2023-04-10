package tage.input.action;

import tage.*;
import net.java.games.input.Event;
import org.joml.*;

import a3.MyGame;
import a3.ProtocolClient;

public class DecelAction extends AbstractInputAction {
	private MyGame game;
	private GameObject av;
	private Vector3f oldPosition, newPosition;
	private Vector4f fwdDirection;
	private ProtocolClient protClient;

	public DecelAction(MyGame g, ProtocolClient p)
	{
		game = g;
		protClient = p;
	}

	@Override
	public void performAction(float time, Event e)
	{
		// av = game.getAvatar();
		// oldPosition = av.getWorldLocation();
		// fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
		// fwdDirection.mul(av.getWorldRotation());
		// fwdDirection.mul(0.01f);
		// newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(),
		// fwdDirection.z());
		// av.setLocalLocation(newPosition);
		// protClient.sendMoveMessage(av.getWorldLocation());

		game.decelerate(time);
	}
}