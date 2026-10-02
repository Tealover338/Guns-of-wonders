package com.danila.gunsofwonders.client;

import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Exported Blockbench geometry; arbitrary cube and bone rotations are preserved. */
public final class GunMesh {
    public float[] camera, grip, supportGrip;
    public Arm rightArm, leftArm;
    public static final class Arm { public float[] wrist; public float[][] axes; public float width; }
    public float[][] bounds;
    public Quad[] quads, arms;
    public Frame[] idle;
    public float idleLength;
    public static final class Quad { public String texture; public float[][] vertices, uv; public float[] normal; }
    public static final class Frame { public float time; public float[] position; }
    private Map<String, List<Quad>> batches;
    private String id;
    public static GunMesh load(String id) {
        String path = "/assets/gunsofwonders/guns/" + id + ".json";
        try (var stream = GunMesh.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing " + path);
            var mesh = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), GunMesh.class);
            mesh.id = id; mesh.batches = new LinkedHashMap<>();
            for (Quad q : mesh.quads) mesh.batches.computeIfAbsent(q.texture, key -> new ArrayList<>()).add(q);
            return mesh;
        } catch (Exception ex) { throw new IllegalStateException("Unable to load gun " + id, ex); }
    }
    public void idle(PoseStack pose, float seconds) {
        float t = seconds % idleLength;
        for (int i = 1; i < idle.length; i++) if (t <= idle[i].time) {
            Frame a = idle[i-1], b = idle[i]; float f = (t-a.time)/(b.time-a.time);
            pose.translate((a.position[0]+f*(b.position[0]-a.position[0]))/16f,
                (a.position[1]+f*(b.position[1]-a.position[1]))/16f,
                (a.position[2]+f*(b.position[2]-a.position[2]))/16f); return;
        }
    }
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, Identifier skin) {
        batches.forEach((texture, faces) -> submitFaces(pose, collector, light,
            Identifier.fromNamespaceAndPath("gunsofwonders", "textures/guns/"+id+"/"+texture+".png"), faces));
        if (skin != null) submitFaces(pose, collector, light, skin, Arrays.asList(arms));
    }
    private static void submitFaces(PoseStack pose, SubmitNodeCollector collector, int light, Identifier texture, List<Quad> faces) {
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, buffer) -> {
            for (Quad q : faces) for (int i=0;i<4;i++) {
                float[] p=q.vertices[i], uv=q.uv[i], n=q.normal;
                buffer.addVertex(matrix,p[0]/16f,p[1]/16f,p[2]/16f).setColor(-1).setUv(uv[0],uv[1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(matrix,n[0],n[1],n[2]);
            }
        });
    }
}
