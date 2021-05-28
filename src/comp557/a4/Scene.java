package comp557.a4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.vecmath.Color3f;
import javax.vecmath.Color4f;
import javax.vecmath.Vector3d;

/**
 * Simple scene loader based on XML file format.
 */
public class Scene {

	/** List of surfaces in the scene */
	public List<Intersectable> surfaceList = new ArrayList<Intersectable>();

	/** All scene lights */
	public Map<String, Light> lights = new HashMap<String, Light>();

	/** Contains information about how to render the scene */
	public Render render;

	public double index = 1;

	/** The ambient light colour */
	public Color3f ambient = new Color3f();

	/**
	 * Default constructor.
	 */
	public Scene() {
		this.render = new Render();
	}

	/**
	 * renders the scene
	 */
	public void render(boolean showPanel) {

		Camera cam = render.camera;
		int w = cam.imageSize.width;
		int h = cam.imageSize.height;

		render.init(w, h, showPanel);

		Color3f color = new Color3f();
		Color3f sumC = new Color3f();
		Ray ray = new Ray();
		IntersectResult result = new IntersectResult();
		double[] offset = new double[2];

		Random rand = new Random();

		SceneNode root = surfaceList.get(0).getClass().equals(SceneNode.class) ? (SceneNode) surfaceList.get(0) : null;

		int grid = (int) Math.sqrt(render.samples);

		for (int i = 0; i < h && !render.isDone(); i++) {
			for (int j = 0; j < w && !render.isDone(); j++) {
				color.set(0, 0, 0);
				for (int q = 0; q < grid; q++) {
					double jitter = render.jitter ? rand.nextDouble() * 0.5 : 0.5;
					offset[0] = (double) (q + jitter) / grid;
					for (int p = 0; p < grid; p++) {
						jitter = render.jitter ? rand.nextDouble() * 0.5 : 0.5;
						offset[1] = (double) (p + jitter) / grid;
						// TODO: Objective 1: generate a ray (use the generateRay method)
						generateRay(i, j, offset, cam, ray);
						// TODO: Objective 2: test for intersection with scene surfaces
						result = new IntersectResult();

						for (Intersectable surface : surfaceList) {
							surface.intersect(ray, result);
						}

						// TODO: Objective 3: compute the shaded result for the intersection point
						// (perhaps requiring shadow rays)
						sumC.set(render.bgcolor);

						if (result.material != null) {
							// ambient light
							Color3f La = new Color3f(result.material.diffuse.x * ambient.x,
									result.material.diffuse.y * ambient.y, result.material.diffuse.z * ambient.z);
							sumC.set(La);

							for (Light light : lights.values()) {

								Ray shadowRay = new Ray();
								Vector3d viewDirection = new Vector3d();
								viewDirection.sub(light.from, result.p);
								shadowRay.viewDirection.set(viewDirection);
								Vector3d eyePosition = new Vector3d(result.p);
								shadowRay.eyePoint.set(eyePosition);

								IntersectResult shadowResult = new IntersectResult();
								if (!inShadow(result, light, root, shadowResult, shadowRay)) {
									Vector3d v = new Vector3d();
									Vector3d hVec = new Vector3d();
									v.sub(cam.from, result.p);
									v.normalize();
									Vector3d l = new Vector3d();
									l.sub(light.from, result.p);
									l.normalize();
									hVec.add(v, l);
									hVec.normalize();

									// lambertian light
									Color3f tmp = new Color3f(result.material.diffuse.x * light.color.x,
											result.material.diffuse.y * light.color.y,
											result.material.diffuse.z * light.color.z);
									tmp.scale((float) Math.max(0, result.n.dot(l)));
									tmp.scale((float) light.power);
									sumC.add(tmp);

									// Bling-Phong
									tmp = new Color3f(result.material.specular.x * light.color.x,
											result.material.specular.y * light.color.y,
											result.material.specular.z * light.color.z);
									tmp.scale((float) Math.pow(Math.max(0, result.n.dot(hVec)),
											result.material.shinyness));
									tmp.scale((float) light.power);
									sumC.add(tmp);
								}
							}

							if (result.material != null && result.material.refract) {

								Ray refractRay = new Ray();
								Color3f refractSum = new Color3f();
								IntersectResult refractResult = new IntersectResult();
								Vector3d I = new Vector3d();
								I.set(ray.viewDirection);
								Vector3d N = new Vector3d();
								N = result.n;
								Vector3d dir = new Vector3d();
								dir = generateRefract(I, N, result.material.index);

								refractRay.set(result.p, dir);

								refractSum.set(render.bgcolor);
								for (Intersectable surface : surfaceList)
									surface.intersect(refractRay, refractResult);

								if (refractResult.material != null) {
									Color3f refractLa = new Color3f(refractResult.material.diffuse.x * ambient.x,
											refractResult.material.diffuse.y * ambient.y,
											refractResult.material.diffuse.z * ambient.z);
									refractSum.set(refractLa);

									for (Light light : lights.values()) {

										Ray shadowRay = new Ray();

										Vector3d d = new Vector3d(light.from);
										d.sub(refractResult.p);
										shadowRay.set(refractResult.p, d);

										IntersectResult shadowResult = new IntersectResult();
										if (!inShadow(refractResult, light, root, shadowResult, shadowRay)) {

											Vector3d refl = new Vector3d();
											Vector3d reflv = new Vector3d();
											Vector3d reflh = new Vector3d();
											reflv.sub(result.p, refractResult.p);
											reflv.normalize();
											refl.sub(light.from, refractResult.p);
											refl.normalize();

											reflh.add(reflv, refl);
											reflh.normalize();

											// reflect Lam
											Color3f tmp = new Color3f(refractResult.material.diffuse.x * light.color.x,
													refractResult.material.diffuse.y * light.color.y,
													refractResult.material.diffuse.z * light.color.z);
											tmp.scale((float) Math.max(0, refractResult.n.dot(refl)));
											tmp.scale((float) light.power);
											refractSum.add(tmp);

											// reflect B-P
											tmp = new Color3f(refractResult.material.specular.x * light.color.x,
													refractResult.material.specular.y * light.color.y,
													refractResult.material.specular.z * light.color.z);
											tmp.scale((float) Math.pow(Math.max(0, refractResult.n.dot(reflh)),
													refractResult.material.shinyness));
											tmp.scale((float) light.power);
											refractSum.add(tmp);
										}
									}
									refractSum.set(refractSum.x * result.material.specular.x,
											refractSum.y * result.material.specular.y,
											refractSum.z * result.material.specular.z);
								}
								sumC.add(refractSum);
								ray.set(refractRay.eyePoint, refractRay.viewDirection);
								result = new IntersectResult(refractResult);
							}

							if (result.material != null && render.reflect && hasSpecular(result.material.specular)) {

								Ray reflectRay = new Ray();
								Color3f reflectSum = new Color3f();
								IntersectResult reflectResult = new IntersectResult();
								Vector3d reflectDir = new Vector3d();
								Vector3d rayv = new Vector3d();
								rayv.set(ray.viewDirection);
								rayv.negate();

								double co = 2 * rayv.dot(result.n);
								reflectDir.scale(co, result.n);
								reflectDir.sub(rayv);

								reflectRay.set(result.p, reflectDir);

								reflectSum.set(render.bgcolor);
								for (Intersectable surface : surfaceList)
									surface.intersect(reflectRay, reflectResult);

								if (reflectResult.material != null) {
									Color3f reflectLa = new Color3f(reflectResult.material.diffuse.x * ambient.x,
											reflectResult.material.diffuse.y * ambient.y,
											reflectResult.material.diffuse.z * ambient.z);
									reflectSum.set(reflectLa);

									for (Light light : lights.values()) {

										Ray shadowRay = new Ray();

										Vector3d d = new Vector3d(light.from);
										d.sub(reflectResult.p);
										shadowRay.set(reflectResult.p, d);

										IntersectResult shadowResult = new IntersectResult();
										if (!inShadow(reflectResult, light, root, shadowResult, shadowRay)) {

											Vector3d refl = new Vector3d();
											Vector3d reflv = new Vector3d();
											Vector3d reflh = new Vector3d();
											reflv.sub(result.p, reflectResult.p);
											reflv.normalize();
											refl.sub(light.from, reflectResult.p);
											refl.normalize();

											reflh.add(reflv, refl);
											reflh.normalize();

											// reflect Lam
											Color3f tmp = new Color3f(reflectResult.material.diffuse.x * light.color.x,
													reflectResult.material.diffuse.y * light.color.y,
													reflectResult.material.diffuse.z * light.color.z);
											tmp.scale((float) Math.max(0, reflectResult.n.dot(refl)));
											tmp.scale((float) light.power);
											reflectSum.add(tmp);

											// reflect B-P
											tmp = new Color3f(reflectResult.material.specular.x * light.color.x,
													reflectResult.material.specular.y * light.color.y,
													reflectResult.material.specular.z * light.color.z);
											tmp.scale((float) Math.pow(Math.max(0, reflectResult.n.dot(reflh)),
													reflectResult.material.shinyness));
											tmp.scale((float) light.power);
											reflectSum.add(tmp);
										}
									}
									reflectSum.set(reflectSum.x * result.material.specular.x,
											reflectSum.y * result.material.specular.y,
											reflectSum.z * result.material.specular.z);
								}
								sumC.add(reflectSum);
								ray.set(reflectRay.eyePoint, reflectRay.viewDirection);
								result = new IntersectResult(reflectResult);
							}
						}
						color.add(sumC);
					}
				}
				color.scale((float) 1 / (grid * grid));
				color.clamp(0, 1);

				// Here is an example of how to calculate the pixel value.
				int r = (int) (255 * color.x);
				int g = (int) (255 * color.y);
				int b = (int) (255 * color.z);
				int a = 255;
				int argb = (a << 24 | r << 16 | g << 8 | b);

				// update the render image
				render.setPixel(j, i, argb);
			}
		}

		// save the final render image
		render.save();

		// wait for render viewer to close
		render.waitDone();

	}

	public Vector3d generateRefract(Vector3d I, Vector3d N, double index) {
		Vector3d T = new Vector3d(I);
		T.normalize();
		double cos1 = I.dot(N) / (I.length() * N.length());
		if (cos1 < 0) {
			cos1 = -cos1;
		}
		double sin1 = Math.sqrt(1 - cos1 * cos1);
		double sin2 = sin1 * this.index / index;
		double cos2 = Math.sqrt(1 - sin2 * sin2);
		Vector3d C = new Vector3d(N);
		C.normalize();
		C.scale(cos1);
		C.add(T);
		C.normalize();
		Vector3d A = new Vector3d(C);
		A.scale(sin2);
		Vector3d B = new Vector3d(N);
		B.normalize();
		B.negate();
		B.scale(cos2);
		T.add(A, B);
		T.normalize();
		return T;

	}

	private boolean hasSpecular(Color4f a) {
		Color4f black = new Color4f(0, 0, 0, 1);
		if (Math.max(Math.max((a.x - black.x), Math.max((a.y - black.y), (a.z - black.z))), (a.w - black.w)) < 1e-9) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Generate a ray through pixel (i,j).
	 * 
	 * @param i      The pixel row.
	 * @param j      The pixel column.
	 * @param offset The offset from the center of the pixel, in the range
	 *               [-0.5,+0.5] for each coordinate.
	 * @param cam    The camera.
	 * @param ray    Contains the generated ray.
	 */
	public static void generateRay(final int i, final int j, final double[] offset, final Camera cam, Ray ray) {
		// TODO: Objective 1: generate rays given the provided parmeters
		Vector3d pixw = new Vector3d();
		pixw.sub(cam.from, cam.to);
		pixw.normalize();

		Vector3d unitu = new Vector3d();
		unitu.cross(cam.up, pixw);
		unitu.normalize();

		Vector3d unitv = new Vector3d();
		unitv.cross(pixw, unitu);
		unitv.normalize();
		double w = cam.imageSize.width;
		double h = cam.imageSize.height;
		double distance = cam.from.distance(cam.to);

		// compute bottom, left, top - bottom, right - left
		double t = distance * Math.tan(Math.toRadians(cam.fovy / 2.0));
		double b = -t;
		double r = t * w / h;
		double l = -r;

		double u = l + (r - l) * (j + offset[0]) / w;
		double v = -(b + (t - b) * (i + offset[1]) / h);

		Vector3d view = new Vector3d();
		pixw.scale(distance);
		view.scaleAdd(-1, pixw, view);
		view.scaleAdd(u, unitu, view);
		view.scaleAdd(v, unitv, view);
		view.normalize();

		ray.set(cam.from, view);
	}

	/**
	 * Shoot a shadow ray in the scene and get the result.
	 * 
	 * @param result       Intersection result from raytracing.
	 * @param light        The light to check for visibility.
	 * @param root         The scene node.
	 * @param shadowResult Contains the result of a shadow ray test.
	 * @param shadowRay    Contains the shadow ray used to test for visibility.
	 * 
	 * @return True if a point is in shadow, false otherwise.
	 */
	public boolean inShadow(final IntersectResult result, final Light light, final SceneNode root,
			IntersectResult shadowResult, Ray shadowRay) {

		// TODO: Objective 5: check for shdows and use it in your lighting computation
		for (Intersectable surface : surfaceList) {
			surface.intersect(shadowRay, shadowResult);
		}
		if (shadowResult.material != null && shadowResult.t < 1)
			return true;

		return false;
	}
}