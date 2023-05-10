package a3;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import tage.GameObject;
import tage.ObjShape;
import tage.TextureImage;
import tage.audio.IAudioManager;
import tage.audio.Sound;

public class NpcAvatar extends GameObject{

	private Sound engineSound;
	private IAudioManager audioMgr;
	public boolean wantsAccel, wantsDecel, wantsTurnLeft, wantsTurnRight;
	public double speed;

	public NpcAvatar(ObjShape s, TextureImage t, Vector3f p, Vector3f l, Sound sound, IAudioManager audioMgr)
	{
		super(GameObject.root(), s, t);
		this.audioMgr = audioMgr;
		engineSound = sound;
		engineSound.initialize(audioMgr);
		engineSound.setMaxDistance(50.0f);
		engineSound.setMinDistance(3.0f);
		engineSound.setRollOff(2.0f);
		engineSound.play(80, true);
		setPosition(p, l);
		wantsAccel = false;
		wantsDecel = false;
		wantsTurnLeft = false;
		wantsTurnRight = false;
		speed = 0;
	}
	
	public void setPosition(Vector3f p, Vector3f l)
	{
		setLocalLocation(p);
		lookAt(l);
		float[] vals = new float[16];
		Matrix4f r = getLocalRotation();
		Matrix4f m = new Matrix4f().translation(p);
		m = m.mul(r);
		getPhysicsObject().setTransform(toDoubleArray(m.get(vals)));
		engineSound.setLocation(p);
	}

	public Vector3f getPosition()
	{
		return getWorldLocation();
	}

	public void setSoundPitch(float pitch)
	{
		engineSound.setPitch(pitch);
	}

	public void stopSounds()
	{
		engineSound.release(audioMgr);
	}

	public void setVolume(int volume)
	{
		engineSound.setVolume(volume);
	}

	public void updateSounds()
	{
		engineSound.setLocation(getWorldLocation());
	}

	private double[] toDoubleArray(float[] arr)
	{
		if (arr == null)
		{
			return null;
		}
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++)
		{
			ret[i] = (double) arr[i];
		}
		return ret;
	}
}
