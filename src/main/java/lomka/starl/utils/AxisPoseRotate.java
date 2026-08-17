package lomka.starl.utils;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

/**
 * Fast quaternion-based pose rotation helper used by the custom pose stack path.
 * The implementation keeps the single-axis branches lightweight while still
 * falling back to JOML's generic rotation for arbitrary quaternions.
 */
public final class AxisPoseRotate {

    private AxisPoseRotate() {}

    public static void mulPose(Matrix4f pose, Matrix3f normal, Quaternionfc q) {
        float x = q.x();
        float y = q.y();
        float z = q.z();
        float w = q.w();

        if (y == 0.0F && z == 0.0F) {
            float s = 2.0F * x * w;
            float c = w * w - x * x;
            rotateX(pose, normal, s, c);
        } else if (x == 0.0F && z == 0.0F) {
            float s = 2.0F * y * w;
            float c = w * w - y * y;
            rotateY(pose, normal, s, c);
        } else if (x == 0.0F && y == 0.0F) {
            float s = 2.0F * z * w;
            float c = w * w - z * z;
            rotateZ(pose, normal, s, c);
        } else {
            pose.rotate(q);
            normal.rotate(q);
        }
    }

    private static void rotateX(Matrix4f m, Matrix3f n, float s, float c) {
        float m10 = m.m10(), m11 = m.m11(), m12 = m.m12(), m13 = m.m13();
        float m20 = m.m20(), m21 = m.m21(), m22 = m.m22(), m23 = m.m23();

        m.m10(m10 * c + m20 * s);
        m.m11(m11 * c + m21 * s);
        m.m12(m12 * c + m22 * s);
        m.m13(m13 * c + m23 * s);
        m.m20(m10 * -s + m20 * c);
        m.m21(m11 * -s + m21 * c);
        m.m22(m12 * -s + m22 * c);
        m.m23(m13 * -s + m23 * c);

        float n10 = n.m10, n11 = n.m11, n12 = n.m12;
        float n20 = n.m20, n21 = n.m21, n22 = n.m22;
        n.m10 = n10 * c + n20 * s;
        n.m11 = n11 * c + n21 * s;
        n.m12 = n12 * c + n22 * s;
        n.m20 = n10 * -s + n20 * c;
        n.m21 = n11 * -s + n21 * c;
        n.m22 = n12 * -s + n22 * c;
    }

    private static void rotateY(Matrix4f m, Matrix3f n, float s, float c) {
        float m00 = m.m00(), m01 = m.m01(), m02 = m.m02(), m03 = m.m03();
        float m20 = m.m20(), m21 = m.m21(), m22 = m.m22(), m23 = m.m23();

        m.m00(m00 * c - m20 * s);
        m.m01(m01 * c - m21 * s);
        m.m02(m02 * c - m22 * s);
        m.m03(m03 * c - m23 * s);
        m.m20(m00 * s + m20 * c);
        m.m21(m01 * s + m21 * c);
        m.m22(m02 * s + m22 * c);
        m.m23(m03 * s + m23 * c);

        float n00 = n.m00, n01 = n.m01, n02 = n.m02;
        float n20 = n.m20, n21 = n.m21, n22 = n.m22;
        n.m00 = n00 * c - n20 * s;
        n.m01 = n01 * c - n21 * s;
        n.m02 = n02 * c - n22 * s;
        n.m20 = n00 * s + n20 * c;
        n.m21 = n01 * s + n21 * c;
        n.m22 = n02 * s + n22 * c;
    }

    private static void rotateZ(Matrix4f m, Matrix3f n, float s, float c) {
        float m00 = m.m00(), m01 = m.m01(), m02 = m.m02(), m03 = m.m03();
        float m10 = m.m10(), m11 = m.m11(), m12 = m.m12(), m13 = m.m13();

        m.m00(m00 * c + m10 * s);
        m.m01(m01 * c + m11 * s);
        m.m02(m02 * c + m12 * s);
        m.m03(m03 * c + m13 * s);
        m.m10(m00 * -s + m10 * c);
        m.m11(m01 * -s + m11 * c);
        m.m12(m02 * -s + m12 * c);
        m.m13(m03 * -s + m13 * c);

        float n00 = n.m00, n01 = n.m01, n02 = n.m02;
        float n10 = n.m10, n11 = n.m11, n12 = n.m12;
        n.m00 = n00 * c + n10 * s;
        n.m01 = n01 * c + n11 * s;
        n.m02 = n02 * c + n12 * s;
        n.m10 = n00 * -s + n10 * c;
        n.m11 = n01 * -s + n11 * c;
        n.m12 = n02 * -s + n12 * c;
    }
}