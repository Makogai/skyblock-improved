package me.sbi.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * Renders wireframe and filled boxes in 3D world space. Based on OdinFabric's PrimitiveRenderer.
 */
public final class PrimitiveRenderer {

    private static final int[] EDGES = {
        0, 1,  1, 5,  5, 4,  4, 0,
        3, 2,  2, 6,  6, 7,  7, 3,
        0, 3,  1, 2,  5, 6,  4, 7
    };

    private PrimitiveRenderer() {}

    public static void renderLineBox(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            AABB aabb,
            float r, float g, float b, float a) {
        float x0 = (float) aabb.minX;
        float y0 = (float) aabb.minY;
        float z0 = (float) aabb.minZ;
        float x1 = (float) aabb.maxX;
        float y1 = (float) aabb.maxY;
        float z1 = (float) aabb.maxZ;

        float[] corners = {
            x0, y0, z0,
            x1, y0, z0,
            x1, y1, z0,
            x0, y1, z0,
            x0, y0, z1,
            x1, y0, z1,
            x1, y1, z1,
            x0, y1, z1
        };

        for (int i = 0; i < EDGES.length; i += 2) {
            int i0 = EDGES[i] * 3;
            int i1 = EDGES[i + 1] * 3;

            float cx0 = corners[i0];
            float cy0 = corners[i0 + 1];
            float cz0 = corners[i0 + 2];
            float cx1 = corners[i1];
            float cy1 = corners[i1 + 1];
            float cz1 = corners[i1 + 2];

            float dx = cx1 - cx0;
            float dy = cy1 - cy0;
            float dz = cz1 - cz0;

            buffer.addVertex(pose, cx0, cy0, cz0).setColor(r, g, b, a).setNormal(pose, dx, dy, dz);
            buffer.addVertex(pose, cx1, cy1, cz1).setColor(r, g, b, a).setNormal(pose, dx, dy, dz);
        }
    }

    public static void addChainedFilledBoxVertices(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float r, float g, float b, float a) {
        Matrix4f matrix = pose.pose();
        vertex(matrix, buffer, minX, minY, minZ, r, g, b, a);
        vertex(matrix, buffer, minX, minY, minZ, r, g, b, a);
        vertex(matrix, buffer, minX, minY, minZ, r, g, b, a);
        vertex(matrix, buffer, minX, minY, maxZ, r, g, b, a);
        vertex(matrix, buffer, minX, maxY, minZ, r, g, b, a);
        vertex(matrix, buffer, minX, maxY, maxZ, r, g, b, a);
        vertex(matrix, buffer, minX, maxY, maxZ, r, g, b, a);
        vertex(matrix, buffer, minX, minY, maxZ, r, g, b, a);
        vertex(matrix, buffer, maxX, maxY, maxZ, r, g, b, a);
        vertex(matrix, buffer, maxX, minY, maxZ, r, g, b, a);
        vertex(matrix, buffer, maxX, minY, maxZ, r, g, b, a);
        vertex(matrix, buffer, maxX, minY, minZ, r, g, b, a);
        vertex(matrix, buffer, maxX, maxY, maxZ, r, g, b, a);
        vertex(matrix, buffer, maxX, maxY, minZ, r, g, b, a);
        vertex(matrix, buffer, maxX, maxY, minZ, r, g, b, a);
        vertex(matrix, buffer, maxX, minY, minZ, r, g, b, a);
        vertex(matrix, buffer, minX, maxY, minZ, r, g, b, a);
        vertex(matrix, buffer, minX, minY, minZ, r, g, b, a);
        vertex(matrix, buffer, minX, minY, minZ, r, g, b, a);
        vertex(matrix, buffer, maxX, minY, minZ, r, g, b, a);
        vertex(matrix, buffer, minX, minY, maxZ, r, g, b, a);
        vertex(matrix, buffer, maxX, minY, maxZ, r, g, b, a);
        vertex(matrix, buffer, maxX, minY, maxZ, r, g, b, a);
        vertex(matrix, buffer, minX, maxY, minZ, r, g, b, a);
        vertex(matrix, buffer, minX, maxY, minZ, r, g, b, a);
        vertex(matrix, buffer, minX, maxY, maxZ, r, g, b, a);
        vertex(matrix, buffer, maxX, maxY, minZ, r, g, b, a);
        vertex(matrix, buffer, maxX, maxY, maxZ, r, g, b, a);
        vertex(matrix, buffer, maxX, maxY, maxZ, r, g, b, a);
        vertex(matrix, buffer, maxX, maxY, maxZ, r, g, b, a);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer buffer, float x, float y, float z, float r, float g, float b, float a) {
        buffer.addVertex(matrix, x, y, z).setColor(r, g, b, a);
    }
}
