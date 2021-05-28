package comp557.a4;

import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

public class Mesh extends Intersectable {

	/** Static map storing all meshes by name */
	public static Map<String, Mesh> meshMap = new HashMap<String, Mesh>();

	/** Name for this mesh, to allow re-use of a polygon soup across Mesh objects */
	public String name = "";

	/**
	 * The polygon soup.
	 */
	public PolygonSoup soup;

	public Mesh() {
		super();
		this.soup = null;
	}

	@Override
	public void intersect(Ray ray, IntersectResult result) {

		// TODO: Objective 7: ray triangle intersection for meshes
		for (int[] face : soup.faceList) {
			Point3d A = soup.vertexList.get(face[0]).p;
			Point3d B = soup.vertexList.get(face[1]).p;
			Point3d C = soup.vertexList.get(face[2]).p;

			Vector3d AB = new Vector3d();
			AB.sub(B, A);
			Vector3d BC = new Vector3d();
			BC.sub(C, B);
			Vector3d CA = new Vector3d();
			CA.sub(A, C);
			Vector3d AC = new Vector3d();
			AC.sub(C, A);
			Vector3d n = new Vector3d();
			n.cross(AB, AC);
			n.normalize();
			Vector3d l = new Vector3d();
			l.sub(A, ray.eyePoint);

			double t = l.dot(n) / ray.viewDirection.dot(n);

			if (t < result.t && t > 1e-9) {
				Point3d X = new Point3d();
				X.scaleAdd(t, ray.viewDirection, ray.eyePoint);
				Vector3d AX = new Vector3d();
				AX.sub(X, A);
				Vector3d BX = new Vector3d();
				BX.sub(X, B);
				Vector3d CX = new Vector3d();
				CX.sub(X, C);
				Vector3d a = new Vector3d();
				Vector3d b = new Vector3d();
				Vector3d c = new Vector3d();
				a.cross(AB, AX);
				b.cross(BC, BX);
				c.cross(CA, CX);
				if (a.dot(n) > 1e-9 && b.dot(n) > 1e-9 && c.dot(n) > 1e-9) {
					result.material = material;
					result.t = t;
					result.p.scaleAdd(t, ray.viewDirection, ray.eyePoint);
					result.n.set(n);
				}
			}
		}
	}

}
