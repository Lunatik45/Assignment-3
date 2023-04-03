package tage.nodeControllers;

import java.util.HashMap;
import org.joml.Matrix4f;
import tage.GameObject;
import tage.NodeController;

/**
 * The Revolve Controller is a node controller that takes an object and makes it
 * slowly revolve around the parent.
 * @author Eric Rodriguez
 */
public class RevolveController extends NodeController {

	private HashMap<GameObject, Integer> map = new HashMap<>();
	private int i = 0;

	public RevolveController()
	{
		super();
	}

	@Override
	public void addTarget(GameObject go)
	{
		super.addTarget(go);
		map.put(go, ++i);
	}

	@Override
	public void removeTarget(GameObject go)
	{
		super.removeTarget(go);
		map.remove(go);
	}

	@Override
	public void apply(GameObject t)
	{
		float amt = super.getElapsedTimeTotal() / 2000f;
		amt -= map.get(t) * 1.3f;
		Matrix4f currentTranslation = t.getLocalTranslation();
		currentTranslation.translation((float) Math.sin(amt) * 0.5f, (float) Math.cos(amt) * 0.5f,
				(float) Math.sin(amt * 1.3) * 0.2f);
		t.setLocalTranslation(currentTranslation);
	}
}
