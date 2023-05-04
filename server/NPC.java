package server;

import org.joml.Vector3f;

import tage.Log;

public class NPC {
	private Vector3f position, lookat;
	private boolean wantsAccel, wantsDecel, wantsTurnLeft, wantsTurnRight;

	public NPC()
	{
		position = new Vector3f(0, 0, 0);
		lookat = new Vector3f(0, 0, -1);
		resetDriveStatus();
	}

	public Vector3f getPosition()
	{
		return new Vector3f(position);
	}

	public Vector3f getLookat()
	{
		return new Vector3f(lookat);
	}

	public void brake()
	{
		wantsDecel = true;
	}

	public void turn(boolean left)
	{
		if (left)
		{
			wantsTurnLeft = true;
		} else
		{
			wantsTurnRight = true;
		}
	}

	public void accelerate()
	{
		wantsAccel = true;
	}

	public void updateInfo(Vector3f location, Vector3f lookat)
	{
		this.position = new Vector3f(location);
		this.lookat = new Vector3f(lookat);
	}

	public void resetDriveStatus()
	{
		wantsAccel = false;
		wantsDecel = false;
		wantsTurnLeft = false;
		wantsTurnRight = false;
	}

	public String getStatusMsg()
	{
		String p = position.x + "," + position.y + "," + position.z;
		String l = lookat.x + "," + lookat.y + "," + lookat.z;
		String a = wantsAccel ? "1" : "0";
		String b = wantsDecel ? "1" : "0";
		String c = wantsTurnLeft ? "1" : "0";
		String d = wantsTurnRight ? "1" : "0";

		return new String(p + "," + l + "," + a + "," + b + "," + c + "," + d);
	}

	public void goStraight()
	{
		resetDriveStatus();
		wantsAccel = true;
	}
}
