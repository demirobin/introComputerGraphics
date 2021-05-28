package comp557.a4;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class Metaballs extends Intersectable {

	ArrayList<Sphere> spheres = new ArrayList<Sphere>();

	double thresh = 0.2;

	double eps = 0.01;

	int iterations = 100;

	@Override
	public void intersect(Ray ray, IntersectResult result) {
		Point3d eye = ray.eyePoint;
		ArrayList<Sphere> M = new ArrayList<Sphere>();
		for (int i = 0; i < spheres.size(); i++) {
			Point3d c = new Point3d(spheres.get(i).center);
			Point3d p = new Point3d(ray.eyePoint);
			Point3d p2 = new Point3d();
			ray.getPoint(0.1, p2);
			Vector3d pc = new Vector3d();
			pc.sub(c, p);
			Vector3d pp2 = new Vector3d();
			pp2.sub(p2, p);
			double cos = pc.dot(pp2) / (pc.length() * pp2.length());
			double dist = pc.length() * Math.sqrt(1 - cos * cos);
			double r = spheres.get(i).radius;
			if (dist <= r && eye.distance(c) >= r) {
				M.add(spheres.get(i));
			}
		}

		if (M.size() == 0) {
			return;
		}

		double exceed = Double.POSITIVE_INFINITY;
		for (int t = 0; t < iterations; t++) {
			Point3d point = new Point3d();
			ray.getPoint(t, point);
			if (t < exceed && influence(point, M) > thresh) {
				exceed = t;
			}
		}
		if (exceed == Double.POSITIVE_INFINITY) {
			return;
		}

		double preceed = Double.NEGATIVE_INFINITY;
		for (int t = (int) exceed; t > 0; t--) {
			Point3d point = new Point3d();
			ray.getPoint(t, point);
			if (t > preceed & t < exceed) {
				preceed = t;
			}
		}
		double intersec = findIntersect(ray, M, preceed, exceed);

		if (intersec > 1e-9 && intersec < result.t) {
			result.t = intersec;
			Point3d intersection = new Point3d();
			ray.getPoint(intersec, intersection);
			// result.p.scaleAdd(intersec, ray.viewDirection, ray.eyePoint);
			result.p.set(intersection);
			for (Sphere s : spheres) {
				Vector3d N = new Vector3d();
				N.sub(intersection, s.center);
//				N.normalize();
//				N.scale(singleInfl(intersection, s) / thresh);
				result.n.add(N);
			}
			result.n.normalize();
			result.material = material;
		}

	}

	public double influence(Point3d p, ArrayList<Sphere> m) {
		double sum = 0;
		for (Sphere s : m) {
			double r = p.distance(s.center);
			if (r > s.radius) {
				sum += 0;
			} else if (r > s.radius / 3 && r <= s.radius) {
				sum += 3 / 2 * (1 - r / s.radius) * (1 - r / s.radius);
			} else {
				sum += (1 - 3 * (r / s.radius) * (r / s.radius));
			}
		}
		return sum;
	}

	public double singleInfl(Point3d p, Sphere m) {
		double r = p.distance(m.center);
		if (r > m.radius) {
			return 0;
		} else if (r > m.radius / 3 && r <= m.radius) {
			return 3 / 2 * (1 - r / m.radius) * (1 - r / m.radius);
		} else {
			return (1 - 3 * (r / m.radius) * (r / m.radius));
		}
	}

	public double findIntersect(Ray ray, ArrayList<Sphere> m, double pre, double exc) {
		double middle = Double.POSITIVE_INFINITY;
		for (int i = 0; i < iterations; i++) {
			middle = (pre + exc) / 2;
			Point3d p1 = new Point3d();
			ray.getPoint(pre, p1);
			Point3d p2 = new Point3d();
			ray.getPoint(exc, p2);
			if (influence(p1, m) + influence(p2, m) <= thresh) {
				pre = middle;
			} else {
				exc = middle;
			}
		}
		return middle;
	}
}