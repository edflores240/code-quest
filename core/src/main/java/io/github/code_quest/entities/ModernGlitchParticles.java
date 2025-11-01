package io.github.code_quest.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.math.MathUtils;

public class ModernGlitchParticles extends Actor {
    private static class GlitchBit {
        float x, y, vx, vy, alpha, size, rotation, rotationSpeed;
        Color color;
        float life, maxLife;
        boolean isGlitching;
    }

    private final Array<GlitchBit> particles = new Array<>();
    private final Texture pixel;
    private final float width, height;
    private final Color baseColor = new Color();
    private float time = 0f;

    public ModernGlitchParticles(float width, float height, int count, Color tint) {
        this.width = width; 
        this.height = height;
        this.baseColor.set(tint);
        setBounds(0, 0, width, height);

        // Create a small square for particles
        Pixmap pm = new Pixmap(2, 2, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        this.pixel = new Texture(pm);
        pm.dispose();

        for (int i = 0; i < count; i++) {
            particles.add(createParticle());
        }
    }

    private GlitchBit createParticle() {
        GlitchBit p = new GlitchBit();
        p.x = MathUtils.random(width);
        p.y = MathUtils.random(height);
        p.vx = MathUtils.random(-15f, 15f);
        p.vy = MathUtils.random(-5f, 25f);
        p.alpha = MathUtils.random(0.1f, 0.4f);
        p.size = MathUtils.random(1f, 4f);
        p.rotation = MathUtils.random(0f, 360f);
        p.rotationSpeed = MathUtils.random(-180f, 180f);
        p.life = MathUtils.random(2f, 5f);
        p.maxLife = p.life;
        p.isGlitching = MathUtils.randomBoolean(0.3f);
        
        // Modern glitch colors
        if (p.isGlitching) {
            p.color = new Color(
                MathUtils.random(0.8f, 1f),
                MathUtils.random(0.2f, 0.6f),
                MathUtils.random(0.2f, 0.8f),
                p.alpha
            );
        } else {
            p.color = new Color(baseColor.r, baseColor.g, baseColor.b, p.alpha);
        }
        
        return p;
    }

    @Override
    public void act(float delta) {
        time += delta;
        
        for (int i = particles.size - 1; i >= 0; i--) {
            GlitchBit p = particles.get(i);
            
            // Update position
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.rotation += p.rotationSpeed * delta;
            
            // Update life
            p.life -= delta;
            p.alpha = (p.life / p.maxLife) * 0.4f;
            
            // Glitch effect - random position jumps
            if (p.isGlitching && MathUtils.randomBoolean(0.05f)) {
                p.x += MathUtils.random(-20f, 20f);
                p.y += MathUtils.random(-10f, 10f);
            }
            
            // Reset or remove particle
            if (p.life <= 0 || p.y > height + 20 || p.x < -20 || p.x > width + 20) {
                particles.removeIndex(i);
                particles.add(createParticle());
            }
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color old = batch.getColor();
        
        for (GlitchBit p : particles) {
            batch.setColor(p.color.r, p.color.g, p.color.b, p.alpha * parentAlpha);
            
            // Draw with rotation
            batch.draw(pixel, 
                p.x - p.size/2, p.y - p.size/2, 
                p.size/2, p.size/2, 
                p.size, p.size, 
                1f, 1f, 
                p.rotation, 
                0, 0, 
                pixel.getWidth(), pixel.getHeight(), 
                false, false
            );
        }
        
        batch.setColor(old);
    }

    public void dispose() {
        pixel.dispose();
    }
}