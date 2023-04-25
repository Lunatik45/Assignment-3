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
		game.decelerate(time);
	}
}