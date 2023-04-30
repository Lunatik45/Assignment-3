package tage.input.action;

import tage.*;
import net.java.games.input.Event;
import org.joml.*;

import com.bulletphysics.dynamics.vehicle.RaycastVehicle;

import a3.MyGame;
import a3.ProtocolClient;

public class AccelAction extends AbstractInputAction {
	private MyGame game;
	private GameObject av;
	private Vector3f oldPosition, newPosition;
	private Vector4f fwdDirection;
	private ProtocolClient protClient;
	private RaycastVehicle vehicle;

	public AccelAction(MyGame g, RaycastVehicle v, ProtocolClient p)
	{
		game = g;
		protClient = p;
		vehicle = v;
	}

	@Override
	public void performAction(float time, Event e)
	{
		// av = game.getAvatar();
		// oldPosition = av.getWorldLocation();
		// fwdDirection = new Vector4f(0f, 0f, 1f, 1f);
		// fwdDirection.mul(av.getWorldRotation());
		// fwdDirection.mul(0.01f);
		// newPosition = oldPosition.add(fwdDirection.x(), fwdDirection.y(), fwdDirection.z());
		// av.setLocalLocation(newPosition);
		// protClient.sendMoveMessage(av.getWorldLocation());
		System.out.println("Accelerating");
		this.vehicle.applyEngineForce(5000, 2);
		this.vehicle.applyEngineForce(5000, 3);
		// game.accelerate(time);
	}
}

