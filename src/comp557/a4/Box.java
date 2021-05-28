package comp557.a4;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * A simple box class. A box is defined by it's lower (@see min) and upper (@see
 * max) corner.
 */
public class Box extends Intersectable {

	public Point3d max;
	public Point3d min;

	/**
	 * Default constructor. Creates a 2x2x2 box centered at (0,0,0)
	 */
	public Box() {
		super();
		this.max = new Point3d(1, 1, 1);
		this.min = new Point3d(-1, -1, -1);
	}

	@Override
	public void intersect(Ray ray, IntersectResult result) {
		// TODO: Objective 6: intersection of Ray with axis aligned box
		// f(x)=p+td
		Point3d p = new Point3d(ray.eyePoint);
		Vector3d d = new Vector3d(ray.viewDirection);
		double txmin = (min.x - p.x) / d.x;
		double txmax = (max.x - p.x) / d.x;
		double tymin = (min.y - p.y) / d.y;
		double tymax = (max.y - p.y) / d.y;
		double tzmin = (min.z - p.z) / d.z;
		double tzmax = (max.z - p.z) / d.z;

		double txlow = Math.min(txmin, txmax);
		double txhigh = Math.max(txmin, txmax);
		double tylow = Math.min(tymin, tymax);
		double tyhigh = Math.max(tymax, tymin);
		double tzlow = Math.min(tzmin, tzmax);
		double tzhigh = Math.max(tzmin, tzmax);

		double tmin = Math.max(txlow, Math.max(tylow, tzlow));
		double tmax = Math.min(txhigh, Math.min(tyhigh, tzhigh));
		if (tmax < tmin || tmin < 1e-9 || tmin >= result.t)
			return;
		else {
			result.material = material;
			result.t = tmin;
			result.p.scaleAdd(result.t, d, p);
			if (txlow > tylow && txlow > tzlow) {
				result.n.set(1, 0, 0);
				if (p.x < min.x)
					result.n.negate();
			} else if (tylow > txlow && tylow > tzlow) {
				result.n.set(0, 1, 0);
				if (p.y < min.y)
					result.n.negate();
			} else if (tzlow > txlow && tzlow > tylow) {
				result.n.set(0, 0, 1);
				if (p.z < min.z)
					result.n.negate();
			}
		}
	}

}
