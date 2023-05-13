package a3;

import java.util.UUID;

import org.joml.*;

import tage.*;
import tage.audio.IAudioManager;
import tage.audio.Sound;

// A ghost MUST be connected as a child of the root,
// so that it will be rendered, and for future removal.
// The ObjShape and TextureImage associated with the ghost
// must have already been created during loadShapes() and
// loadTextures(), before the game loop is started.

public class GhostAvatar extends GameObject {
	UUID uuid;
	Sound engineSound;
	IAudioManager audioMgr;

	public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p, Sound sound, IAudioManager audioMgr)
	{
		super(GameObject.root(), s, t);
		uuid = id;
		this.audioMgr = audioMgr;
		engineSound = sound;
		engineSound.initialize(audioMgr);
		engineSound.setMaxDistance(50.0f);
		engineSound.setMinDistance(3.0f);
		engineSound.setRollOff(2.0f);
		engineSound.play(80, true);
		setPosition(p);
	}

	public UUID getID()
	{
		return uuid;
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

	public void setVolume(int volume)
	{
		engineSound.setVolume(volume);
	}
}
