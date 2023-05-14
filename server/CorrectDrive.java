package server;

import org.joml.Vector2f;
import org.joml.Vector3f;

import tage.Log;
import tage.ai.behaviortrees.BTAction;
import tage.ai.behaviortrees.BTStatus;

public class CorrectDrive extends BTAction {

	private NPCController npcController;
	private GameServerUDP server;
	private NPC npc;
	private double m1 = 20, m2 = 30, m3 = 45;

	public CorrectDrive(NPCController npcController, GameServerUDP server, NPC npc)
	{
		super();

		this.npcController = npcController;
		this.server = server;
		this.npc = npc;
	}

	@Override
	protected BTStatus update(float elapsedTime)
	{
		Vector3f lookat = npc.getLookat();
		Vector2f look2f = new Vector2f(lookat.x, lookat.z);
		Vector3f position = npc.getPosition();
		Vector2f pos2f = new Vector2f(position.x, position.z);
		Vector2f nextTarget = npcController.getNextTarget();
		Vector2f l = new Vector2f(look2f).sub(pos2f).normalize();
		Vector2f t = new Vector2f(nextTarget).sub(pos2f).normalize();
		float angle = l.angle(t);
		double angleAbs = Math.abs(angle);
		boolean left = angle < 0;

		npc.resetDriveStatus();

		if (pos2f.distance(nextTarget) < 20)
		{
			m1 = 30;
			m2 = 60;
			m3 = 75;
		}
		else if (pos2f.distance(nextTarget) < 50)
		{
			m1 = 20;
			m2 = 40;
			m3 = 50;
		} 
		else
		{
			m1 = 10;
			m2 = 15;
			m3 = 20;
		}

		if (angleAbs > Math.toRadians(m3))
		{
			npc.brake();
			npc.turn(left);
		} else if (angleAbs > Math.toRadians(m2))
		{
			npc.turn(left);
		} else if (angleAbs > Math.toRadians(m1))
		{
			npc.accelerate();
			npc.turn(left);
		} else
		{
			npc.accelerate();
		}

		return BTStatus.BH_SUCCESS;
	}
}
