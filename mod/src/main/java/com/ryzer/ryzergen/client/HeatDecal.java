package com.ryzer.ryzergen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Lays a decal over any block's own model, as the game lays its breaking cracks
 * (SheetedDecalTextureGenerator): each face gets the decal's texture whole, mapped from the vertex's
 * place in its block, so a block of any shape from any mod takes it. Unlike the cracks it keeps a
 * colour of our choosing (the heat's tint or glow), and turns or flips the decal per block (one of
 * eight ways) so a whole layer of them does not repeat. Feed it the pose the block is drawn with.
 */
final class HeatDecal implements VertexConsumer {
    private final VertexConsumer delegate;
    private final Matrix4f inversePose;
    private final Matrix3f inverseNormal;
    private final int colour;
    private final int variant;
    private final Vector3f local = new Vector3f();
    private final Vector3f normal = new Vector3f();
    private float x;
    private float y;
    private float z;

    HeatDecal(VertexConsumer delegate, PoseStack.Pose pose, int colour, int variant) {
        this.delegate = delegate;
        this.inversePose = new Matrix4f(pose.pose()).invert();
        this.inverseNormal = new Matrix3f(pose.normal()).invert();
        this.colour = colour;
        this.variant = variant;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        delegate.setColor(colour);
        return this;
    }

    /** Set in setNormal, once the face's direction is known. */
    @Override
    public VertexConsumer setUv(float u, float v) {
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        delegate.setNormal(normalX, normalY, normalZ);
        Vector3f n = inverseNormal.transform(normalX, normalY, normalZ, normal);
        Direction direction = Direction.getNearest(n.x(), n.y(), n.z());
        Vector3f p = inversePose.transformPosition(x, y, z, local);
        p.rotateY((float) Math.PI);
        p.rotateX((float) (-Math.PI / 2));
        p.rotate(direction.getRotation());
        float u = -p.x();
        float v = -p.y();
        if ((variant & 4) != 0) {
            float t = u;
            u = v;
            v = t;
        }
        // The heat textures repeat, so a flip stays within them.
        delegate.setUv((variant & 1) != 0 ? 1 - u : u, (variant & 2) != 0 ? 1 - v : v);
        return this;
    }
}
