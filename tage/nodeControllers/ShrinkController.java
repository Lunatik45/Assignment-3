package tage.nodeControllers;

import tage.GameObject;
import tage.NodeController;

/**
 * This Srhink Controller is a node controller that slowly rotates and shrinks an object.
 * @author Eric Rodriguez
 */
public class ShrinkController extends NodeController {

	public ShrinkController()
	{
		super();
	}

	@Override
	public void apply(GameObject t)
	{
		t.yaw(0.03f);
		t.roll(0.02f);
		t.pitch(0.02f);
		t.worldYaw(-0.01f);
		t.setLocalScale(t.getLocalScale().scale(0.99f, 0.99f, 0.99f));
	}
}