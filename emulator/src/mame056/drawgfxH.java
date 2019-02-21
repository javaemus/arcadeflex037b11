/**
 * Ported to 0.56
 */
package mame056;

import java.util.Arrays;

public class drawgfxH {

    /*TODO*///
/*TODO*///#define MAX_GFX_PLANES 8
/*TODO*///#define MAX_GFX_SIZE 64
/*TODO*///
/*TODO*///#define RGN_FRAC(num,den) (0x80000000 | (((num) & 0x0f) << 27) | (((den) & 0x0f) << 23))
/*TODO*///#define IS_FRAC(offset) ((offset) & 0x80000000)
/*TODO*///#define FRAC_NUM(offset) (((offset) >> 27) & 0x0f)
/*TODO*///#define FRAC_DEN(offset) (((offset) >> 23) & 0x0f)
/*TODO*///#define FRAC_OFFSET(offset) ((offset) & 0x007fffff)
/*TODO*///
/*TODO*///#define STEP4(START,STEP)  (START),(START)+1*(STEP),(START)+2*(STEP),(START)+3*(STEP)
/*TODO*///#define STEP8(START,STEP)  STEP4(START,STEP),STEP4((START)+4*(STEP),STEP)
/*TODO*///#define STEP16(START,STEP) STEP8(START,STEP),STEP8((START)+8*(STEP),STEP)
/*TODO*///
/*TODO*///
    public static class GfxLayout {

        public GfxLayout() {
        }

        public GfxLayout(int width, int height, int total, int planes, int planeoffset[], int xoffset[], int yoffset[], int charincrement) {
            this.width = width;
            this.height = height;
            this.total = total;
            this.planes = planes;
            this.planeoffset = planeoffset;
            this.xoffset = xoffset;
            this.yoffset = yoffset;
            this.charincrement = charincrement;
        }

        public GfxLayout(GfxLayout c) {
            width = c.width;
            height = c.height;
            total = c.total;
            planes = c.planes;
            planeoffset = Arrays.copyOf(c.planeoffset, c.planeoffset.length);
            xoffset = Arrays.copyOf(c.xoffset, c.xoffset.length);
            yoffset = Arrays.copyOf(c.yoffset, c.yoffset.length);
            charincrement = c.charincrement;
        }

        public /*UNINT16*/ int width, height;/* width and height of chars/sprites */
        public /*UNINT32*/ int total;/* total numer of chars/sprites in the rom */
        public /*UNINT16*/ int planes;/* number of bitplanes */
        public /*UNINT32*/ int planeoffset[];/* start of every bitplane */
        public /*UNINT32*/ int xoffset[];/* coordinates of the bit corresponding to the pixel */
        public /*UNINT32*/ int yoffset[];/* of the given coordinates */
        public /*UNINT16*/ int charincrement;/* distance between two consecutive characters/sprites */
    }

    /*TODO*///
/*TODO*///#define GFX_RAW 0x12345678
/*TODO*////* When planeoffset[0] is set to GFX_RAW, the gfx data is left as-is, with no conversion.
/*TODO*///   No buffer is allocated for the decoded data, and gfxdata is set to point to the source
/*TODO*///   data; therefore, you must not use ROMREGION_DISPOSE.
/*TODO*///   xoffset[0] is an optional displacement (*8) from the beginning of the source data, while
/*TODO*///   yoffset[0] is the line modulo (*8) and charincrement the char modulo (*8). They are *8
/*TODO*///   for consistency with the usual behaviour, but the bottom 3 bits are not used.
/*TODO*///   GFX_PACKED is automatically set if planes is <= 4.
/*TODO*///
/*TODO*///   This special mode can be used to save memory in games that require several different
/*TODO*///   handlings of the same ROM data (e.g. metro.c can use both 4bpp and 8bpp tiles, and both
/*TODO*///   8x8 and 16x16; cps.c has 8x8, 16x16 and 32x32 tiles all fetched from the same ROMs).
/*TODO*///   Note, however, that performance will suffer in rotated games, since the gfx data will
/*TODO*///   not be prerotated and will rely on GFX_SWAPXY.
/*TODO*///*/
/*TODO*///
/*TODO*///struct GfxElement
/*TODO*///{
/*TODO*///	int width,height;
/*TODO*///
/*TODO*///	unsigned int total_elements;	/* total number of characters/sprites */
/*TODO*///	int color_granularity;	/* number of colors for each color code */
/*TODO*///							/* (for example, 4 for 2 bitplanes gfx) */
/*TODO*///	pen_t *colortable;	/* map color codes to screen pens */
/*TODO*///	int total_colors;
/*TODO*///	UINT32 *pen_usage;	/* an array of total_elements entries. */
/*TODO*///						/* It is a table of the pens each character uses */
/*TODO*///						/* (bit 0 = pen 0, and so on). This is used by */
/*TODO*///						/* drawgfgx() to do optimizations like skipping */
/*TODO*///						/* drawing of a totally transparent character */
/*TODO*///	UINT8 *gfxdata;		/* pixel data */
/*TODO*///	int line_modulo;	/* amount to add to get to the next line (usually = width) */
/*TODO*///	int char_modulo;	/* = line_modulo * height */
/*TODO*///	int flags;
/*TODO*///};
/*TODO*///
/*TODO*///#define GFX_PACKED				1	/* two 4bpp pixels are packed in one byte of gfxdata */
/*TODO*///#define GFX_SWAPXY				2	/* characters are mirrored along the top-left/bottom-right diagonal */
/*TODO*///#define GFX_DONT_FREE_GFXDATA	4	/* gfxdata was not malloc()ed, so don't free it on exit */
/*TODO*///
/*TODO*///
    public static class GfxDecodeInfo {

        public GfxDecodeInfo(int mr, int s, GfxLayout g, int ccs, int tcc) {
            memory_region = mr;
            start = s;
            if (g != null) {
                gfxlayout = new GfxLayout(g);
            } else {
                gfxlayout = null;
            }
            color_codes_start = ccs;
            total_color_codes = tcc;
        }

        public GfxDecodeInfo(int s, GfxLayout g, int ccs, int tcc) {
            start = s;
            if (g != null) {
                gfxlayout = new GfxLayout(g);
            } else {
                gfxlayout = null;
            }
            color_codes_start = ccs;
            total_color_codes = tcc;
        }

        public GfxDecodeInfo(int s) {
            this(s, s, null, 0, 0);
        }

        public int memory_region;/* memory region where the data resides (usually 1)  -1 marks the end of the array */
        public int start;/* beginning of data data to decode (offset in RAM[]) */
        public GfxLayout gfxlayout;
        public int color_codes_start;/* offset in the color lookup table where color codes start */
        public int total_color_codes;/* total number of color codes */
    }

    public static class rectangle {

        public rectangle() {
        }

        public rectangle(int min_x, int max_x, int min_y, int max_y) {
            this.min_x = min_x;
            this.max_x = max_x;
            this.min_y = min_y;
            this.max_y = max_y;
        }

        public rectangle(rectangle rec) {
            min_x = rec.min_x;
            max_x = rec.max_x;
            min_y = rec.min_y;
            max_y = rec.max_y;
        }

        public int min_x, max_x;
        public int min_y, max_y;
    }
    /*TODO*///
/*TODO*///struct _alpha_cache {
/*TODO*///	const UINT8 *alphas;
/*TODO*///	const UINT8 *alphad;
/*TODO*///	UINT8 alpha[0x101][0x100];
/*TODO*///};
/*TODO*///
/*TODO*///extern struct _alpha_cache alpha_cache;
/*TODO*///
/*TODO*///enum
/*TODO*///{
/*TODO*///	TRANSPARENCY_NONE,			/* opaque with remapping */
/*TODO*///	TRANSPARENCY_NONE_RAW,		/* opaque with no remapping */
/*TODO*///	TRANSPARENCY_PEN,			/* single pen transparency with remapping */
/*TODO*///	TRANSPARENCY_PEN_RAW,		/* single pen transparency with no remapping */
/*TODO*///	TRANSPARENCY_PENS,			/* multiple pen transparency with remapping */
/*TODO*///	TRANSPARENCY_PENS_RAW,		/* multiple pen transparency with no remapping */
/*TODO*///	TRANSPARENCY_COLOR,			/* single remapped pen transparency with remapping */
/*TODO*///	TRANSPARENCY_PEN_TABLE,		/* special pen remapping modes (see DRAWMODE_xxx below) with remapping */
/*TODO*///	TRANSPARENCY_PEN_TABLE_RAW,	/* special pen remapping modes (see DRAWMODE_xxx below) with no remapping */
/*TODO*///	TRANSPARENCY_BLEND,			/* blend two bitmaps, shifting the source and ORing to the dest with remapping */
/*TODO*///	TRANSPARENCY_BLEND_RAW,		/* blend two bitmaps, shifting the source and ORing to the dest with no remapping */
/*TODO*///	TRANSPARENCY_ALPHAONE,		/* single pen transparency, single pen alpha */
/*TODO*///	TRANSPARENCY_ALPHA,			/* single pen transparency, other pens alpha */
/*TODO*///
/*TODO*///	TRANSPARENCY_MODES			/* total number of modes; must be last */
/*TODO*///};
/*TODO*///
/*TODO*////* drawing mode case TRANSPARENCY_PEN_TABLE */
/*TODO*///extern UINT8 gfx_drawmode_table[256];
/*TODO*///enum
/*TODO*///{
/*TODO*///	DRAWMODE_NONE,
/*TODO*///	DRAWMODE_SOURCE,
/*TODO*///	DRAWMODE_SHADOW
/*TODO*///};
/*TODO*///typedef void (*plot_pixel_proc)(struct mame_bitmap *bitmap,int x,int y,UINT32 pen);
/*TODO*///typedef int  (*read_pixel_proc)(struct mame_bitmap *bitmap,int x,int y);
/*TODO*///typedef void (*plot_box_proc)(struct mame_bitmap *bitmap,int x,int y,int width,int height,UINT32 pen);
/*TODO*///typedef void (*mark_dirty_proc)(int sx,int sy,int ex,int ey);
/*TODO*///
/*TODO*///
/*TODO*////* pointers to pixel functions.  They're set based on orientation, depthness and whether
/*TODO*///   dirty rectangle handling is enabled */
/*TODO*///extern plot_pixel_proc plot_pixel;
/*TODO*///extern read_pixel_proc read_pixel;
/*TODO*///extern plot_box_proc plot_box;
/*TODO*///extern mark_dirty_proc mark_dirty;
/*TODO*///
/*TODO*////* Alpha blending functions */
/*TODO*///extern int alpha_active;
/*TODO*///void alpha_init(void);
/*TODO*///INLINE void alpha_set_level(int level) {
/*TODO*///	if(level == 0)
/*TODO*///		level = -1;
/*TODO*///	alpha_cache.alphas = alpha_cache.alpha[level+1];
/*TODO*///	alpha_cache.alphad = alpha_cache.alpha[255-level];
/*TODO*///}
/*TODO*///
/*TODO*///INLINE UINT32 alpha_blend16( UINT32 d, UINT32 s )
/*TODO*///{
/*TODO*///	const UINT8 *alphas = alpha_cache.alphas;
/*TODO*///	const UINT8 *alphad = alpha_cache.alphad;
/*TODO*///	return (alphas[s & 0x1f] | (alphas[(s>>5) & 0x1f] << 5) | (alphas[(s>>10) & 0x1f] << 10))
/*TODO*///		+ (alphad[d & 0x1f] | (alphad[(d>>5) & 0x1f] << 5) | (alphad[(d>>10) & 0x1f] << 10));
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///INLINE UINT32 alpha_blend32( UINT32 d, UINT32 s )
/*TODO*///{
/*TODO*///	const UINT8 *alphas = alpha_cache.alphas;
/*TODO*///	const UINT8 *alphad = alpha_cache.alphad;
/*TODO*///	return (alphas[s & 0xff] | (alphas[(s>>8) & 0xff] << 8) | (alphas[(s>>16) & 0xff] << 16))
/*TODO*///		+ (alphad[d & 0xff] | (alphad[(d>>8) & 0xff] << 8) | (alphad[(d>>16) & 0xff] << 16));
/*TODO*///}

}
