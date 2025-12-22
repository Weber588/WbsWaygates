package wbs.waygates.util;

import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class TransformationBuilder {
    private Vector3f translation = new Vector3f(0, 0, 0);
    private Vector3f scale = new Vector3f(1, 1, 1);
    private Quaternionf leftRotation = new Quaternionf();
    private Quaternionf rightRotation = new Quaternionf();

    public Vector3f translation() {
        return translation;
    }

    public TransformationBuilder translation(Vector3f translation) {
        this.translation = translation;
        return this;
    }

    public TransformationBuilder translation(Vector scale) {
        return translation(new Vector3f((float) scale.getX(), (float) scale.getY(), (float) scale.getZ()));
    }

    public TransformationBuilder translation(float x, float y, float z) {
        return translation(new Vector3f(x, y, z));
    }

    public Vector3f scale() {
        return scale;
    }

    public TransformationBuilder scale(Vector3f scale) {
        this.scale = scale;
        return this;
    }

    public TransformationBuilder scale(Vector scale) {
        return scale(new Vector3f((float) scale.getX(), (float) scale.getY(), (float) scale.getZ()));
    }

    public TransformationBuilder scale(float x, float y, float z) {
        return scale(new Vector3f(x, y, z));
    }

    public Quaternionf leftRotation() {
        return leftRotation;
    }

    public TransformationBuilder leftRotation(Quaternionf leftRotation) {
        this.leftRotation = leftRotation;
        return this;
    }

    public Quaternionf rightRotation() {
        return rightRotation;
    }

    public TransformationBuilder rightRotation(Quaternionf rightRotation) {
        this.rightRotation = rightRotation;
        return this;
    }

    public Transformation build() {
        return new Transformation(translation, leftRotation, scale, rightRotation);
    }
}
