package tage.input.action;

import net.java.games.input.Event;
import tage.*;

import org.joml.*;

import a3.MyGame;

public class TurnLeftAction extends AbstractInputAction {
	private MyGame game;
	private GameObject av;
	
	private float turnConst, turnCoef;

	public TurnLeftAction(MyGame g, float turnConst, float turnCoef)
	{
		game = g;
		this.turnCoef = turnCoef;
		this.turnConst = turnConst;
	}

	@Override
	public void performAction(float time, Event e)
	{
		float keyValue = e.getValue();
		if (keyValue > -.2 && keyValue < .2)
			return; // deadzone

		av = game.getAvatar();

		if (game.getSpeed() > 0 && !game.getIsFalling())
		{
			float speed = (float) game.getSpeed();
			float max = (float) game.getMaxSpeed();
			float yaw = time * turnCoef * (speed / max) + turnConst;
			
			av.worldYaw(yaw);
		}
	}
}

