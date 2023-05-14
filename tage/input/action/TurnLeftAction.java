package tage.input.action;

import net.java.games.input.Event;

import com.bulletphysics.dynamics.vehicle.RaycastVehicle;

public class TurnLeftAction extends AbstractInputAction {
	
	private float turnMax;
	private float steer;
	private float turnConst, turnCoef;
	private RaycastVehicle vehicle;

	public TurnLeftAction(float turnConst, float turnCoef, float turnMax, RaycastVehicle v)
	{
		this.turnCoef = turnCoef;
		this.turnConst = turnConst;
		this.vehicle = v;
		this.steer = 0.0f;
		this.turnMax = turnMax;
	}

	@Override
	public void performAction(float time, Event e)
	{
		steer = vehicle.getSteeringValue(0);
		steer += turnConst + (turnCoef * time);
		if (steer > turnMax)
		{
			steer = turnMax;
		}

		vehicle.setSteeringValue(steer, 0);
		vehicle.setSteeringValue(steer, 1);
		vehicle.setSteeringValue(-steer * 0.5f, 2);
		vehicle.setSteeringValue(-steer * 0.5f, 3);
	}
}

