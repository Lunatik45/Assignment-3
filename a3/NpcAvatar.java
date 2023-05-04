package a3;

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

	public NpcAvatar(ObjShape s, TextureImage t, Vector3f p, Sound sound, IAudioManager audioMgr)
	{
		super(GameObject.root(), s, t);
		this.audioMgr = audioMgr;
		engineSound = sound;
		engineSound.initialize(audioMgr);
		engineSound.setMaxDistance(50.0f);
		engineSound.setMinDistance(3.0f);
		engineSound.setRollOff(2.0f);
		engineSound.play(80, true);
		setPosition(p);
		wantsAccel = false;
		wantsDecel = false;
		wantsTurnLeft = false;
		wantsTurnRight = false;
		speed = 0;
	}
	
	public void setPosition(Vector3f m)
	{
		setLocalLocation(m);
		engineSound.setLocation(m);
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
}
