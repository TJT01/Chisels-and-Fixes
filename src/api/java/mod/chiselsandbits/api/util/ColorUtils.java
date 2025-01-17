package mod.chiselsandbits.api.util;

import net.minecraft.util.math.vector.Vector3d;

public class ColorUtils
{

    public static final int FULL_CHANNEL = 255;
    public static final int EMPTY_CHANNEL = 0;

    public static final Vector3d SUCCESSFUL_PATTERN_PLACEMENT_COLOR = new Vector3d(48/255f, 120/255f, 201/255f);
    public static final Vector3d NOT_FITTING_PATTERN_PLACEMENT_COLOR = new Vector3d(183/255f, 65/255f, 14/255f);
    public static final Vector3d MISSING_BITS_OR_SPACE_PATTERN_PLACEMENT_COLOR = new Vector3d(255/255f, 219/255f, 88/255f);

    private ColorUtils()
    {
        throw new IllegalStateException("Can not instantiate an instance of: ColorUtils. This is a utility class");
    }

    public static int pack(final int c) {
        return pack(c, c, c);
    }

    public static int pack(final int r, final int g, final int b) {
        return pack(r, g, b, FULL_CHANNEL);
    }

    @SuppressWarnings("PointlessBitwiseExpression")
    public static int pack(final int r, final int g, final int b, final int a) {
        int color = 0;

        color |= (a & FULL_CHANNEL) << 24;
        color |= (r & FULL_CHANNEL) << 16;
        color |= (g & FULL_CHANNEL) << 8;
        color |= (b & FULL_CHANNEL) << 0;

        return color;
    }
}
