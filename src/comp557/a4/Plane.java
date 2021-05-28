package comp557.a4;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Class for a plane at y=0.
 * 
 * This surface can have two materials. If both are defined, a 1x1 tile checker
 * board pattern should be generated on the plane using the two materials.
 */
public class Plane extends Intersectable {

	/**
	 * The second material, if non-null is used to produce a checker board pattern.
	 */
	Material material2;

	/** The plane normal is the y direction */
	public static final Vector3d n = new Vector3d(0, 1, 0);

	/**
	 * Default constructor
	 */
	public Plane() {
		super();
	}

	@Override
	public void intersect(Ray ray, IntersectResult result) {

		// TODO: Objective 4: intersection of ray with plane

		// r(t)=p+td
		Point3d p = new Point3d(ray.eyePoint);
		Vector3d d = new Vector3d(ray.viewDirection);
		Vector3d paVec = new Vector3d();
		paVec.sub(p);

		double t = paVec.dot(Plane.n) / d.dot(Plane.n);

		if (t > 1e-9 && t < result.t) {
			result.t = t;
			Point3d intersect = new Point3d();
			intersect.add(p);
			intersect.scaleAdd(t, d, intersect);

			result.p.set(intersect);

			result.n = new Vector3d(Plane.n);

			double x = ((result.p.x % 2) + 2) % 2;
			double z = ((result.p.z % 2) + 2) % 2;

			if (this.material2 != null) {
				if ((x >= 1 && z >= 1) || (x < 1 && z < 1)) {
					result.material = this.material;
				} else {
					result.material = this.material2;
				}
			} else {
				result.material = this.material;
			}
		}
	}

}
