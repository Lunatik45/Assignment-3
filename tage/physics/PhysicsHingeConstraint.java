package tage.physics;

public interface PhysicsHingeConstraint {
	/**
	 * Returns the current angle between the two bodies
	 * @return Angle as a float
	 */
	public float getAngle();
	
	/**
	 * Returns the Axis that defines the hinge
	 * @return The hinge axis as a float array
	 */
	public float[] getAxis();

	/**
	 * Sets the constraint limits for the hinge
	 * @param low The lower limit of the hinge angle
	 * @param high The upper limit of the hinge angle
	 */
	public void setLimits(float low, float high);
	
	/**
	 * Enables or disables the motor for the hinge
	 * @param enable Whether to enable or disable the motor
	 * @param targetVelocity The desired motor velocity (in radians per second)
	 * @param maxMotorImpulse The maximum force the motor can apply (in Newton-meters)
	 */
	public void enableMotor(boolean enable, float targetVelocity, float maxMotorImpulse);
}
