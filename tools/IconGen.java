import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Usage (JDK 11+): java -Djava.awt.headless=true tools/IconGen.java <output dir>
 * Then copy the mipmap-DENSITY/ic_launcher*.png files into app/src/main/res and play-listing/* into docs/release.
 *
 * Renders the Flute Assistant launcher icon to PNG at every density, plus the Play Store
 * listing assets. The geometry mirrors the adaptive-icon vector drawables (108x108 canvas,
 * launchers show the inner 72x72), so raster and vector icons look identical.
 */
public class IconGen {
    static final Color BG = new Color(0x3949AB);
    static final Color FG = Color.WHITE;

    static Path2D body() {
        Path2D p = new Path2D.Double();
        p.moveTo(46, 28); p.lineTo(62, 28); p.lineTo(73, 80); p.lineTo(35, 80); p.closePath();
        return p;
    }
    static Shape rod() {
        return new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            .createStrokedShape(new Line2D.Double(54, 76, 62, 37));
    }
    static Shape weight() { return new Ellipse2D.Double(59 - 4.5, 53 - 4.5, 9, 9); }

    /** Draws the foreground in adaptive-canvas coordinates. */
    static void foreground(Graphics2D g) {
        g.setColor(FG); g.fill(body());
        g.setColor(BG); g.fill(rod()); g.fill(weight());
    }

    static Graphics2D graphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    /** Legacy launcher icon: the inner 72dp of the adaptive canvas, masked to a shape. */
    static BufferedImage legacy(int size, boolean round) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);
        double margin = round ? 0 : size * 2.0 / 48; // 2dp of breathing room on the square icon
        double inner = size - 2 * margin;
        Shape mask = round
            ? new Ellipse2D.Double(margin, margin, inner, inner)
            : new RoundRectangle2D.Double(margin, margin, inner, inner, inner * 0.3, inner * 0.3);
        g.setColor(BG); g.fill(mask);
        g.setClip(mask);
        g.translate(margin, margin); g.scale(inner / 72.0, inner / 72.0); g.translate(-18, -18);
        foreground(g);
        g.dispose();
        return img;
    }

    /** Play Store icon: full-bleed square, Play applies its own rounding. */
    static BufferedImage playIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(img);
        g.setColor(BG); g.fillRect(0, 0, size, size);
        g.scale(size / 72.0, size / 72.0); g.translate(-18, -18);
        foreground(g);
        g.dispose();
        return img;
    }

    static BufferedImage featureGraphic() {
        int w = 1024, h = 500;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(img);
        g.setPaint(new GradientPaint(0, 0, new Color(0x283593), w, h, BG));
        g.fillRect(0, 0, w, h);
        // Icon on the left, in a rounded tile.
        int tile = 300, tx = 90, ty = (h - tile) / 2;
        g.setColor(new Color(255, 255, 255, 28));
        g.fill(new RoundRectangle2D.Double(tx, ty, tile, tile, 70, 70));
        Graphics2D gi = (Graphics2D) g.create();
        gi.translate(tx, ty); gi.scale(tile / 72.0, tile / 72.0); gi.translate(-18, -18);
        foreground(gi); gi.dispose();
        int textX = 440, maxWidth = w - textX - 60;
        g.setColor(FG);
        g.setFont(fit(g, "Flute Assistant", Font.BOLD, 86, maxWidth));
        g.drawString("Flute Assistant", textX, 232);
        g.setFont(fit(g, "Metronome, tuner, notes and flute", Font.PLAIN, 38, maxWidth));
        g.setColor(new Color(255, 255, 255, 220));
        g.drawString("Metronome, tuner, notes and flute", textX + 2, 296);
        g.dispose();
        return img;
    }

    /** Largest font size (stepping down from maxSize) at which the text fits in maxWidth. */
    static Font fit(Graphics2D g, String text, int style, int maxSize, int maxWidth) {
        for (int size = maxSize; size > 12; size -= 2) {
            Font f = new Font(Font.SANS_SERIF, style, size);
            if (g.getFontMetrics(f).stringWidth(text) <= maxWidth) return f;
        }
        return new Font(Font.SANS_SERIF, style, 12);
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        int[][] densities = {{48, 0}, {72, 1}, {96, 2}, {144, 3}, {192, 4}};
        String[] names = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        for (int[] d : densities) {
            File dir = new File(out, "mipmap-" + names[d[1]]); dir.mkdirs();
            ImageIO.write(legacy(d[0], false), "png", new File(dir, "ic_launcher.png"));
            ImageIO.write(legacy(d[0], true), "png", new File(dir, "ic_launcher_round.png"));
        }
        File listing = new File(out, "play-listing"); listing.mkdirs();
        ImageIO.write(playIcon(512), "png", new File(listing, "icon-512.png"));
        ImageIO.write(featureGraphic(), "png", new File(listing, "feature-graphic-1024x500.png"));
        ImageIO.write(legacy(512, false), "png", new File(out, "preview-square.png"));
        ImageIO.write(legacy(512, true), "png", new File(out, "preview-round.png"));
        System.out.println("icons written to " + out);
    }
}
