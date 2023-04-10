package tage.input;

import net.java.games.input.Event;
import tage.*;
import tage.input.action.AbstractInputAction;

import org.joml.*;

import a3.MyGame;

public class TurnRightAction extends AbstractInputAction {
	
	private MyGame game;
	private GameObject av;

	private float turnConst, turnCoef;

	public TurnRightAction(MyGame g, float turnConst, float turnCoef)
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

			av.worldYaw(-yaw);
		}
	}
}

