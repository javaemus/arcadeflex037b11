/**
 * Ported to 0.56
 */
package mame056;

import static arcadeflex.libc.cstdio.sprintf;
import static arcadeflex.video.osd_save_snapshot;
import common.subArrays.IntArray;
import static mame.drawgfxH.TRANSPARENCY_NONE;
import static mame.drawgfxH.TRANSPARENCY_NONE_RAW;
import static mame.driverH.ORIENTATION_FLIP_X;
import static mame.driverH.ORIENTATION_FLIP_Y;
import static mame.driverH.ORIENTATION_SWAP_XY;
import static mame.driverH.VIDEO_TYPE_VECTOR;
import mame056.commonH.mame_bitmap;
import static mame056.inputH.*;
import static mame056.input.*;
import static old.mame.drawgfx.drawgfx;
import static old.mame.drawgfx.fillbitmap;
import static old.mame.drawgfx.plot_box;
import static old.mame.drawgfx.set_pixel_functions;
import static old.mame.inptportH.IPT_UI_CANCEL;
import static old.mame.inptportH.IPT_UI_DOWN;
import static old.mame.inptportH.IPT_UI_LEFT;
import static old.mame.inptportH.IPT_UI_RIGHT;
import static old.mame.inptportH.IPT_UI_SHOW_GFX;
import static old.mame.inptportH.IPT_UI_SNAPSHOT;
import static old.mame.inptportH.IPT_UI_UP;
import static old.mame.usrintrf.*;
import static old.mame.usrintrf.ui_text;
import static old2.mame.common.schedule_full_refresh;
import static old2.mame.mame.*;
import static old2.mame.mameH.MAX_GFX_ELEMENTS;
import static mame056.common.snapno;

public class usrintrf {

    /*TODO*///static int setup_selected;
/*TODO*///static int osd_selected;
    static int jukebox_selected;
    /*TODO*///static int single_step;
    static int trueorientation;
    static int orientation_count;

    public static void switch_ui_orientation() {
        if (orientation_count == 0) {
            trueorientation = Machine.orientation;
            Machine.orientation = Machine.ui_orientation;
            set_pixel_functions();
        }

        orientation_count++;
    }

    public static void switch_true_orientation() {
        orientation_count--;

        if (orientation_count == 0) {
            Machine.orientation = trueorientation;
            set_pixel_functions();
        }
    }

    public static void set_ui_visarea(int xmin, int ymin, int xmax, int ymax) {
        int temp, w, h;

        /* special case for vectors */
        if ((Machine.drv.video_attributes & VIDEO_TYPE_VECTOR) != 0) {
            if ((Machine.ui_orientation & ORIENTATION_SWAP_XY) != 0) {
                temp = xmin;
                xmin = ymin;
                ymin = temp;
                temp = xmax;
                xmax = ymax;
                ymax = temp;
            }
        } else {
            if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0) {
                w = Machine.drv.screen_height;
                h = Machine.drv.screen_width;
            } else {
                w = Machine.drv.screen_width;
                h = Machine.drv.screen_height;
            }

            if ((Machine.ui_orientation & ORIENTATION_FLIP_X) != 0) {
                temp = w - xmin - 1;
                xmin = w - xmax - 1;
                xmax = temp;
            }

            if ((Machine.ui_orientation & ORIENTATION_FLIP_Y) != 0) {
                temp = h - ymin - 1;
                ymin = h - ymax - 1;
                ymax = temp;
            }

            if ((Machine.ui_orientation & ORIENTATION_SWAP_XY) != 0) {
                temp = xmin;
                xmin = ymin;
                ymin = temp;
                temp = xmax;
                xmax = ymax;
                ymax = temp;
            }

        }
        Machine.uiwidth = xmax - xmin + 1;
        Machine.uiheight = ymax - ymin + 1;
        Machine.uixmin = xmin;
        Machine.uiymin = ymin;
    }

    /*TODO*///
/*TODO*///
/*TODO*///struct GfxElement *builduifont(void)
/*TODO*///{
/*TODO*///    static unsigned char fontdata6x8[] =
/*TODO*///	{
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
/*TODO*///		0x7c,0x80,0x98,0x90,0x80,0xbc,0x80,0x7c,0xf8,0x04,0x64,0x44,0x04,0xf4,0x04,0xf8,
/*TODO*///		0x7c,0x80,0x98,0x88,0x80,0xbc,0x80,0x7c,0xf8,0x04,0x64,0x24,0x04,0xf4,0x04,0xf8,
/*TODO*///		0x7c,0x80,0x88,0x98,0x80,0xbc,0x80,0x7c,0xf8,0x04,0x24,0x64,0x04,0xf4,0x04,0xf8,
/*TODO*///		0x7c,0x80,0x90,0x98,0x80,0xbc,0x80,0x7c,0xf8,0x04,0x44,0x64,0x04,0xf4,0x04,0xf8,
/*TODO*///		0x30,0x48,0x84,0xb4,0xb4,0x84,0x48,0x30,0x30,0x48,0x84,0x84,0x84,0x84,0x48,0x30,
/*TODO*///		0x00,0xfc,0x84,0x8c,0xd4,0xa4,0xfc,0x00,0x00,0xfc,0x84,0x84,0x84,0x84,0xfc,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x30,0x68,0x78,0x78,0x30,0x00,0x00,
/*TODO*///		0x80,0xc0,0xe0,0xf0,0xe0,0xc0,0x80,0x00,0x04,0x0c,0x1c,0x3c,0x1c,0x0c,0x04,0x00,
/*TODO*///		0x20,0x70,0xf8,0x20,0x20,0xf8,0x70,0x20,0x48,0x48,0x48,0x48,0x48,0x00,0x48,0x00,
/*TODO*///		0x00,0x00,0x30,0x68,0x78,0x30,0x00,0x00,0x00,0x30,0x68,0x78,0x78,0x30,0x00,0x00,
/*TODO*///		0x70,0xd8,0xe8,0xe8,0xf8,0xf8,0x70,0x00,0x1c,0x7c,0x74,0x44,0x44,0x4c,0xcc,0xc0,
/*TODO*///		0x20,0x70,0xf8,0x70,0x70,0x70,0x70,0x00,0x70,0x70,0x70,0x70,0xf8,0x70,0x20,0x00,
/*TODO*///		0x00,0x10,0xf8,0xfc,0xf8,0x10,0x00,0x00,0x00,0x20,0x7c,0xfc,0x7c,0x20,0x00,0x00,
/*TODO*///		0xb0,0x54,0xb8,0xb8,0x54,0xb0,0x00,0x00,0x00,0x28,0x6c,0xfc,0x6c,0x28,0x00,0x00,
/*TODO*///		0x00,0x30,0x30,0x78,0x78,0xfc,0x00,0x00,0xfc,0x78,0x78,0x30,0x30,0x00,0x00,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x20,0x20,0x20,0x20,0x20,0x00,0x20,0x00,
/*TODO*///		0x50,0x50,0x50,0x00,0x00,0x00,0x00,0x00,0x00,0x50,0xf8,0x50,0xf8,0x50,0x00,0x00,
/*TODO*///		0x20,0x70,0xc0,0x70,0x18,0xf0,0x20,0x00,0x40,0xa4,0x48,0x10,0x20,0x48,0x94,0x08,
/*TODO*///		0x60,0x90,0xa0,0x40,0xa8,0x90,0x68,0x00,0x10,0x20,0x40,0x00,0x00,0x00,0x00,0x00,
/*TODO*///		0x20,0x40,0x40,0x40,0x40,0x40,0x20,0x00,0x10,0x08,0x08,0x08,0x08,0x08,0x10,0x00,
/*TODO*///		0x20,0xa8,0x70,0xf8,0x70,0xa8,0x20,0x00,0x00,0x20,0x20,0xf8,0x20,0x20,0x00,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x30,0x30,0x60,0x00,0x00,0x00,0xf8,0x00,0x00,0x00,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x30,0x30,0x00,0x00,0x08,0x10,0x20,0x40,0x80,0x00,0x00,
/*TODO*///		0x70,0x88,0x88,0x88,0x88,0x88,0x70,0x00,0x10,0x30,0x10,0x10,0x10,0x10,0x10,0x00,
/*TODO*///		0x70,0x88,0x08,0x10,0x20,0x40,0xf8,0x00,0x70,0x88,0x08,0x30,0x08,0x88,0x70,0x00,
/*TODO*///		0x10,0x30,0x50,0x90,0xf8,0x10,0x10,0x00,0xf8,0x80,0xf0,0x08,0x08,0x88,0x70,0x00,
/*TODO*///		0x70,0x80,0xf0,0x88,0x88,0x88,0x70,0x00,0xf8,0x08,0x08,0x10,0x20,0x20,0x20,0x00,
/*TODO*///		0x70,0x88,0x88,0x70,0x88,0x88,0x70,0x00,0x70,0x88,0x88,0x88,0x78,0x08,0x70,0x00,
/*TODO*///		0x00,0x00,0x30,0x30,0x00,0x30,0x30,0x00,0x00,0x00,0x30,0x30,0x00,0x30,0x30,0x60,
/*TODO*///		0x10,0x20,0x40,0x80,0x40,0x20,0x10,0x00,0x00,0x00,0xf8,0x00,0xf8,0x00,0x00,0x00,
/*TODO*///		0x40,0x20,0x10,0x08,0x10,0x20,0x40,0x00,0x70,0x88,0x08,0x10,0x20,0x00,0x20,0x00,
/*TODO*///		0x30,0x48,0x94,0xa4,0xa4,0x94,0x48,0x30,0x70,0x88,0x88,0xf8,0x88,0x88,0x88,0x00,
/*TODO*///		0xf0,0x88,0x88,0xf0,0x88,0x88,0xf0,0x00,0x70,0x88,0x80,0x80,0x80,0x88,0x70,0x00,
/*TODO*///		0xf0,0x88,0x88,0x88,0x88,0x88,0xf0,0x00,0xf8,0x80,0x80,0xf0,0x80,0x80,0xf8,0x00,
/*TODO*///		0xf8,0x80,0x80,0xf0,0x80,0x80,0x80,0x00,0x70,0x88,0x80,0x98,0x88,0x88,0x70,0x00,
/*TODO*///		0x88,0x88,0x88,0xf8,0x88,0x88,0x88,0x00,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x00,
/*TODO*///		0x08,0x08,0x08,0x08,0x88,0x88,0x70,0x00,0x88,0x90,0xa0,0xc0,0xa0,0x90,0x88,0x00,
/*TODO*///		0x80,0x80,0x80,0x80,0x80,0x80,0xf8,0x00,0x88,0xd8,0xa8,0x88,0x88,0x88,0x88,0x00,
/*TODO*///		0x88,0xc8,0xa8,0x98,0x88,0x88,0x88,0x00,0x70,0x88,0x88,0x88,0x88,0x88,0x70,0x00,
/*TODO*///		0xf0,0x88,0x88,0xf0,0x80,0x80,0x80,0x00,0x70,0x88,0x88,0x88,0x88,0x88,0x70,0x08,
/*TODO*///		0xf0,0x88,0x88,0xf0,0x88,0x88,0x88,0x00,0x70,0x88,0x80,0x70,0x08,0x88,0x70,0x00,
/*TODO*///		0xf8,0x20,0x20,0x20,0x20,0x20,0x20,0x00,0x88,0x88,0x88,0x88,0x88,0x88,0x70,0x00,
/*TODO*///		0x88,0x88,0x88,0x88,0x88,0x50,0x20,0x00,0x88,0x88,0x88,0x88,0xa8,0xd8,0x88,0x00,
/*TODO*///		0x88,0x50,0x20,0x20,0x20,0x50,0x88,0x00,0x88,0x88,0x88,0x50,0x20,0x20,0x20,0x00,
/*TODO*///		0xf8,0x08,0x10,0x20,0x40,0x80,0xf8,0x00,0x30,0x20,0x20,0x20,0x20,0x20,0x30,0x00,
/*TODO*///		0x40,0x40,0x20,0x20,0x10,0x10,0x08,0x08,0x30,0x10,0x10,0x10,0x10,0x10,0x30,0x00,
/*TODO*///		0x20,0x50,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xfc,
/*TODO*///		0x40,0x20,0x10,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x70,0x08,0x78,0x88,0x78,0x00,
/*TODO*///		0x80,0x80,0xf0,0x88,0x88,0x88,0xf0,0x00,0x00,0x00,0x70,0x88,0x80,0x80,0x78,0x00,
/*TODO*///		0x08,0x08,0x78,0x88,0x88,0x88,0x78,0x00,0x00,0x00,0x70,0x88,0xf8,0x80,0x78,0x00,
/*TODO*///		0x18,0x20,0x70,0x20,0x20,0x20,0x20,0x00,0x00,0x00,0x78,0x88,0x88,0x78,0x08,0x70,
/*TODO*///		0x80,0x80,0xf0,0x88,0x88,0x88,0x88,0x00,0x20,0x00,0x20,0x20,0x20,0x20,0x20,0x00,
/*TODO*///		0x20,0x00,0x20,0x20,0x20,0x20,0x20,0xc0,0x80,0x80,0x90,0xa0,0xe0,0x90,0x88,0x00,
/*TODO*///		0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x00,0x00,0x00,0xf0,0xa8,0xa8,0xa8,0xa8,0x00,
/*TODO*///		0x00,0x00,0xb0,0xc8,0x88,0x88,0x88,0x00,0x00,0x00,0x70,0x88,0x88,0x88,0x70,0x00,
/*TODO*///		0x00,0x00,0xf0,0x88,0x88,0xf0,0x80,0x80,0x00,0x00,0x78,0x88,0x88,0x78,0x08,0x08,
/*TODO*///		0x00,0x00,0xb0,0xc8,0x80,0x80,0x80,0x00,0x00,0x00,0x78,0x80,0x70,0x08,0xf0,0x00,
/*TODO*///		0x20,0x20,0x70,0x20,0x20,0x20,0x18,0x00,0x00,0x00,0x88,0x88,0x88,0x98,0x68,0x00,
/*TODO*///		0x00,0x00,0x88,0x88,0x88,0x50,0x20,0x00,0x00,0x00,0xa8,0xa8,0xa8,0xa8,0x50,0x00,
/*TODO*///		0x00,0x00,0x88,0x50,0x20,0x50,0x88,0x00,0x00,0x00,0x88,0x88,0x88,0x78,0x08,0x70,
/*TODO*///		0x00,0x00,0xf8,0x10,0x20,0x40,0xf8,0x00,0x08,0x10,0x10,0x20,0x10,0x10,0x08,0x00,
/*TODO*///		0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x20,0x40,0x20,0x20,0x10,0x20,0x20,0x40,0x00,
/*TODO*///		0x00,0x68,0xb0,0x00,0x00,0x00,0x00,0x00,0x20,0x50,0x20,0x50,0xa8,0x50,0x00,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x20,0x20,0x40,0x0C,0x10,0x38,0x10,0x20,0x20,0xC0,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x28,0x28,0x50,0x00,0x00,0x00,0x00,0x00,0x00,0xA8,0x00,
/*TODO*///		0x70,0xA8,0xF8,0x20,0x20,0x20,0x20,0x00,0x70,0xA8,0xF8,0x20,0x20,0xF8,0xA8,0x70,
/*TODO*///		0x20,0x50,0x88,0x00,0x00,0x00,0x00,0x00,0x44,0xA8,0x50,0x20,0x68,0xD4,0x28,0x00,
/*TODO*///		0x88,0x70,0x88,0x60,0x30,0x88,0x70,0x00,0x00,0x10,0x20,0x40,0x20,0x10,0x00,0x00,
/*TODO*///		0x78,0xA0,0xA0,0xB0,0xA0,0xA0,0x78,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x10,0x20,0x20,0x00,0x00,0x00,0x00,0x00,
/*TODO*///		0x10,0x10,0x20,0x00,0x00,0x00,0x00,0x00,0x28,0x50,0x50,0x00,0x00,0x00,0x00,0x00,
/*TODO*///		0x28,0x28,0x50,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x30,0x78,0x78,0x30,0x00,0x00,
/*TODO*///		0x00,0x00,0x00,0x78,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xFC,0x00,0x00,0x00,0x00,
/*TODO*///		0x68,0xB0,0x00,0x00,0x00,0x00,0x00,0x00,0xF4,0x5C,0x54,0x54,0x00,0x00,0x00,0x00,
/*TODO*///		0x88,0x70,0x78,0x80,0x70,0x08,0xF0,0x00,0x00,0x40,0x20,0x10,0x20,0x40,0x00,0x00,
/*TODO*///		0x00,0x00,0x70,0xA8,0xB8,0xA0,0x78,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x50,0x88,0x88,0x50,0x20,0x20,0x20,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x20,0x00,0x20,0x20,0x20,0x20,0x20,0x00,
/*TODO*///		0x00,0x20,0x70,0xA8,0xA0,0xA8,0x70,0x20,0x30,0x48,0x40,0xE0,0x40,0x48,0xF0,0x00,
/*TODO*///		0x00,0x48,0x30,0x48,0x48,0x30,0x48,0x00,0x88,0x88,0x50,0xF8,0x20,0xF8,0x20,0x00,
/*TODO*///		0x20,0x20,0x20,0x00,0x20,0x20,0x20,0x00,0x78,0x80,0x70,0x88,0x70,0x08,0xF0,0x00,
/*TODO*///		0xD8,0xD8,0x00,0x00,0x00,0x00,0x00,0x00,0x30,0x48,0x94,0xA4,0xA4,0x94,0x48,0x30,
/*TODO*///		0x60,0x10,0x70,0x90,0x70,0x00,0x00,0x00,0x00,0x28,0x50,0xA0,0x50,0x28,0x00,0x00,
/*TODO*///		0x00,0x00,0x00,0xF8,0x08,0x00,0x00,0x00,0x00,0x00,0x00,0x78,0x00,0x00,0x00,0x00,
/*TODO*///		0x30,0x48,0xB4,0xB4,0xA4,0xB4,0x48,0x30,0x7C,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
/*TODO*///		0x60,0x90,0x90,0x60,0x00,0x00,0x00,0x00,0x20,0x20,0xF8,0x20,0x20,0x00,0xF8,0x00,
/*TODO*///		0x60,0x90,0x20,0x40,0xF0,0x00,0x00,0x00,0x60,0x90,0x20,0x90,0x60,0x00,0x00,0x00,
/*TODO*///		0x10,0x20,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x88,0x88,0x88,0xC8,0xB0,0x80,
/*TODO*///		0x78,0xD0,0xD0,0xD0,0x50,0x50,0x50,0x00,0x00,0x00,0x00,0x30,0x30,0x00,0x00,0x00,
/*TODO*///		0x00,0x00,0x00,0x00,0x00,0x10,0x20,0x00,0x20,0x60,0x20,0x20,0x70,0x00,0x00,0x00,
/*TODO*///		0x20,0x50,0x20,0x00,0x00,0x00,0x00,0x00,0x00,0xA0,0x50,0x28,0x50,0xA0,0x00,0x00,
/*TODO*///		0x40,0x48,0x50,0x28,0x58,0xA8,0x38,0x08,0x40,0x48,0x50,0x28,0x44,0x98,0x20,0x3C,
/*TODO*///		0xC0,0x28,0xD0,0x28,0xD8,0xA8,0x38,0x08,0x20,0x00,0x20,0x40,0x80,0x88,0x70,0x00,
/*TODO*///		0x40,0x20,0x70,0x88,0xF8,0x88,0x88,0x00,0x10,0x20,0x70,0x88,0xF8,0x88,0x88,0x00,
/*TODO*///		0x70,0x00,0x70,0x88,0xF8,0x88,0x88,0x00,0x68,0xB0,0x70,0x88,0xF8,0x88,0x88,0x00,
/*TODO*///		0x50,0x00,0x70,0x88,0xF8,0x88,0x88,0x00,0x20,0x50,0x70,0x88,0xF8,0x88,0x88,0x00,
/*TODO*///		0x78,0xA0,0xA0,0xF0,0xA0,0xA0,0xB8,0x00,0x70,0x88,0x80,0x80,0x88,0x70,0x08,0x70,
/*TODO*///		0x40,0x20,0xF8,0x80,0xF0,0x80,0xF8,0x00,0x10,0x20,0xF8,0x80,0xF0,0x80,0xF8,0x00,
/*TODO*///		0x70,0x00,0xF8,0x80,0xF0,0x80,0xF8,0x00,0x50,0x00,0xF8,0x80,0xF0,0x80,0xF8,0x00,
/*TODO*///		0x40,0x20,0x70,0x20,0x20,0x20,0x70,0x00,0x10,0x20,0x70,0x20,0x20,0x20,0x70,0x00,
/*TODO*///		0x70,0x00,0x70,0x20,0x20,0x20,0x70,0x00,0x50,0x00,0x70,0x20,0x20,0x20,0x70,0x00,
/*TODO*///		0x70,0x48,0x48,0xE8,0x48,0x48,0x70,0x00,0x68,0xB0,0x88,0xC8,0xA8,0x98,0x88,0x00,
/*TODO*///		0x40,0x20,0x70,0x88,0x88,0x88,0x70,0x00,0x10,0x20,0x70,0x88,0x88,0x88,0x70,0x00,
/*TODO*///		0x70,0x00,0x70,0x88,0x88,0x88,0x70,0x00,0x68,0xB0,0x70,0x88,0x88,0x88,0x70,0x00,
/*TODO*///		0x50,0x00,0x70,0x88,0x88,0x88,0x70,0x00,0x00,0x88,0x50,0x20,0x50,0x88,0x00,0x00,
/*TODO*///		0x00,0x74,0x88,0x90,0xA8,0x48,0xB0,0x00,0x40,0x20,0x88,0x88,0x88,0x88,0x70,0x00,
/*TODO*///		0x10,0x20,0x88,0x88,0x88,0x88,0x70,0x00,0x70,0x00,0x88,0x88,0x88,0x88,0x70,0x00,
/*TODO*///		0x50,0x00,0x88,0x88,0x88,0x88,0x70,0x00,0x10,0xA8,0x88,0x50,0x20,0x20,0x20,0x00,
/*TODO*///		0x00,0x80,0xF0,0x88,0x88,0xF0,0x80,0x80,0x60,0x90,0x90,0xB0,0x88,0x88,0xB0,0x00,
/*TODO*///		0x40,0x20,0x70,0x08,0x78,0x88,0x78,0x00,0x10,0x20,0x70,0x08,0x78,0x88,0x78,0x00,
/*TODO*///		0x70,0x00,0x70,0x08,0x78,0x88,0x78,0x00,0x68,0xB0,0x70,0x08,0x78,0x88,0x78,0x00,
/*TODO*///		0x50,0x00,0x70,0x08,0x78,0x88,0x78,0x00,0x20,0x50,0x70,0x08,0x78,0x88,0x78,0x00,
/*TODO*///		0x00,0x00,0xF0,0x28,0x78,0xA0,0x78,0x00,0x00,0x00,0x70,0x88,0x80,0x78,0x08,0x70,
/*TODO*///		0x40,0x20,0x70,0x88,0xF8,0x80,0x70,0x00,0x10,0x20,0x70,0x88,0xF8,0x80,0x70,0x00,
/*TODO*///		0x70,0x00,0x70,0x88,0xF8,0x80,0x70,0x00,0x50,0x00,0x70,0x88,0xF8,0x80,0x70,0x00,
/*TODO*///		0x40,0x20,0x00,0x60,0x20,0x20,0x70,0x00,0x10,0x20,0x00,0x60,0x20,0x20,0x70,0x00,
/*TODO*///		0x20,0x50,0x00,0x60,0x20,0x20,0x70,0x00,0x50,0x00,0x00,0x60,0x20,0x20,0x70,0x00,
/*TODO*///		0x50,0x60,0x10,0x78,0x88,0x88,0x70,0x00,0x68,0xB0,0x00,0xF0,0x88,0x88,0x88,0x00,
/*TODO*///		0x40,0x20,0x00,0x70,0x88,0x88,0x70,0x00,0x10,0x20,0x00,0x70,0x88,0x88,0x70,0x00,
/*TODO*///		0x20,0x50,0x00,0x70,0x88,0x88,0x70,0x00,0x68,0xB0,0x00,0x70,0x88,0x88,0x70,0x00,
/*TODO*///		0x00,0x50,0x00,0x70,0x88,0x88,0x70,0x00,0x00,0x20,0x00,0xF8,0x00,0x20,0x00,0x00,
/*TODO*///		0x00,0x00,0x68,0x90,0xA8,0x48,0xB0,0x00,0x40,0x20,0x88,0x88,0x88,0x98,0x68,0x00,
/*TODO*///		0x10,0x20,0x88,0x88,0x88,0x98,0x68,0x00,0x70,0x00,0x88,0x88,0x88,0x98,0x68,0x00,
/*TODO*///		0x50,0x00,0x88,0x88,0x88,0x98,0x68,0x00,0x10,0x20,0x88,0x88,0x88,0x78,0x08,0x70,
/*TODO*///		0x80,0xF0,0x88,0x88,0xF0,0x80,0x80,0x80,0x50,0x00,0x88,0x88,0x88,0x78,0x08,0x70
/*TODO*///    };
/*TODO*///
/*TODO*///	static struct GfxLayout fontlayout6x8 =
/*TODO*///	{
/*TODO*///		6,8,	/* 6*8 characters */
/*TODO*///		256,	/* 256 characters */
/*TODO*///		1,	/* 1 bit per pixel */
/*TODO*///		{ 0 },
/*TODO*///		{ 0, 1, 2, 3, 4, 5, 6, 7 }, /* straightforward layout */
/*TODO*///		{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
/*TODO*///		8*8 /* every char takes 8 consecutive bytes */
/*TODO*///	};
/*TODO*///	static struct GfxLayout fontlayout12x8 =
/*TODO*///	{
/*TODO*///		12,8,	/* 12*8 characters */
/*TODO*///		256,	/* 256 characters */
/*TODO*///		1,	/* 1 bit per pixel */
/*TODO*///		{ 0 },
/*TODO*///		{ 0,0, 1,1, 2,2, 3,3, 4,4, 5,5, 6,6, 7,7 }, /* straightforward layout */
/*TODO*///		{ 0*8, 1*8, 2*8, 3*8, 4*8, 5*8, 6*8, 7*8 },
/*TODO*///		8*8 /* every char takes 8 consecutive bytes */
/*TODO*///	};
/*TODO*///	static struct GfxLayout fontlayout6x16 =
/*TODO*///	{
/*TODO*///		6,16,	/* 6*8 characters */
/*TODO*///		256,	/* 256 characters */
/*TODO*///		1,	/* 1 bit per pixel */
/*TODO*///		{ 0 },
/*TODO*///		{ 0, 1, 2, 3, 4, 5, 6, 7 }, /* straightforward layout */
/*TODO*///		{ 0*8,0*8, 1*8,1*8, 2*8,2*8, 3*8,3*8, 4*8,4*8, 5*8,5*8, 6*8,6*8, 7*8,7*8 },
/*TODO*///		8*8 /* every char takes 8 consecutive bytes */
/*TODO*///	};
/*TODO*///	static struct GfxLayout fontlayout12x16 =
/*TODO*///	{
/*TODO*///		12,16,	/* 12*16 characters */
/*TODO*///		256,	/* 256 characters */
/*TODO*///		1,	/* 1 bit per pixel */
/*TODO*///		{ 0 },
/*TODO*///		{ 0,0, 1,1, 2,2, 3,3, 4,4, 5,5, 6,6, 7,7 }, /* straightforward layout */
/*TODO*///		{ 0*8,0*8, 1*8,1*8, 2*8,2*8, 3*8,3*8, 4*8,4*8, 5*8,5*8, 6*8,6*8, 7*8,7*8 },
/*TODO*///		8*8 /* every char takes 8 consecutive bytes */
/*TODO*///	};
/*TODO*///
/*TODO*///	struct GfxElement *font;
/*TODO*///	static pen_t colortable[2*2];	/* ASG 980209 */
/*TODO*///
/*TODO*///
/*TODO*///	switch_ui_orientation();
/*TODO*///
/*TODO*///	if ((Machine->drv->video_attributes & VIDEO_PIXEL_ASPECT_RATIO_MASK)
/*TODO*///			== VIDEO_PIXEL_ASPECT_RATIO_1_2)
/*TODO*///	{
/*TODO*///		if (Machine->gamedrv->flags & ORIENTATION_SWAP_XY)
/*TODO*///		{
/*TODO*///			font = decodegfx(fontdata6x8,&fontlayout6x16);
/*TODO*///			Machine->uifontwidth = 6;
/*TODO*///			Machine->uifontheight = 16;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			font = decodegfx(fontdata6x8,&fontlayout12x8);
/*TODO*///			Machine->uifontwidth = 12;
/*TODO*///			Machine->uifontheight = 8;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else if (Machine->uiwidth >= 420 && Machine->uiheight >= 420)
/*TODO*///	{
/*TODO*///		font = decodegfx(fontdata6x8,&fontlayout12x16);
/*TODO*///		Machine->uifontwidth = 12;
/*TODO*///		Machine->uifontheight = 16;
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		font = decodegfx(fontdata6x8,&fontlayout6x8);
/*TODO*///		Machine->uifontwidth = 6;
/*TODO*///		Machine->uifontheight = 8;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (font)
/*TODO*///	{
/*TODO*///		/* colortable will be set at run time */
/*TODO*///		memset(colortable,0,sizeof(colortable));
/*TODO*///		font->colortable = colortable;
/*TODO*///		font->total_colors = 2;
/*TODO*///	}
/*TODO*///
/*TODO*///	switch_true_orientation();
/*TODO*///
/*TODO*///	return font;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
    static void erase_screen(mame_bitmap bitmap) {
        fillbitmap(bitmap, Machine.uifont.colortable.read(0), null);
        schedule_full_refresh();
    }

    /*TODO*///
/*TODO*///
/*TODO*////***************************************************************************
/*TODO*///
/*TODO*///  Display text on the screen. If erase is 0, it superimposes the text on
/*TODO*///  the last frame displayed.
/*TODO*///
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*///void displaytext(struct mame_bitmap *bitmap,const struct DisplayText *dt)
/*TODO*///{
/*TODO*///	switch_ui_orientation();
/*TODO*///
/*TODO*///	osd_mark_dirty(Machine->uixmin,Machine->uiymin,Machine->uixmin+Machine->uiwidth-1,Machine->uiymin+Machine->uiheight-1);
/*TODO*///
/*TODO*///	while (dt->text)
/*TODO*///	{
/*TODO*///		int x,y;
/*TODO*///		const char *c;
/*TODO*///
/*TODO*///
/*TODO*///		x = dt->x;
/*TODO*///		y = dt->y;
/*TODO*///		c = dt->text;
/*TODO*///
/*TODO*///		while (*c)
/*TODO*///		{
/*TODO*///			int wrapped;
/*TODO*///
/*TODO*///
/*TODO*///			wrapped = 0;
/*TODO*///
/*TODO*///			if (*c == '\n')
/*TODO*///			{
/*TODO*///				x = dt->x;
/*TODO*///				y += Machine->uifontheight + 1;
/*TODO*///				wrapped = 1;
/*TODO*///			}
/*TODO*///			else if (*c == ' ')
/*TODO*///			{
/*TODO*///				/* don't try to word wrap at the beginning of a line (this would cause */
/*TODO*///				/* an endless loop if a word is longer than a line) */
/*TODO*///				if (x != dt->x)
/*TODO*///				{
/*TODO*///					int nextlen=0;
/*TODO*///					const char *nc;
/*TODO*///
/*TODO*///
/*TODO*///					nc = c+1;
/*TODO*///					while (*nc && *nc != ' ' && *nc != '\n')
/*TODO*///					{
/*TODO*///						nextlen += Machine->uifontwidth;
/*TODO*///						nc++;
/*TODO*///					}
/*TODO*///
/*TODO*///					/* word wrap */
/*TODO*///					if (x + Machine->uifontwidth + nextlen > Machine->uiwidth)
/*TODO*///					{
/*TODO*///						x = dt->x;
/*TODO*///						y += Machine->uifontheight + 1;
/*TODO*///						wrapped = 1;
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///
/*TODO*///			if (!wrapped)
/*TODO*///			{
/*TODO*///				drawgfx(bitmap,Machine->uifont,*c,dt->color,0,0,x+Machine->uixmin,y+Machine->uiymin,0,TRANSPARENCY_NONE,0);
/*TODO*///				x += Machine->uifontwidth;
/*TODO*///			}
/*TODO*///
/*TODO*///			c++;
/*TODO*///		}
/*TODO*///
/*TODO*///		dt++;
/*TODO*///	}
/*TODO*///
/*TODO*///	switch_true_orientation();
/*TODO*///}
/*TODO*///
/*TODO*////* Writes messages on the screen. */
/*TODO*///static void ui_text_ex(struct mame_bitmap *bitmap,const char* buf_begin, const char* buf_end, int x, int y, int color)
/*TODO*///{
/*TODO*///	switch_ui_orientation();
/*TODO*///
/*TODO*///	for (;buf_begin != buf_end; ++buf_begin)
/*TODO*///	{
/*TODO*///		drawgfx(bitmap,Machine->uifont,*buf_begin,color,0,0,
/*TODO*///				x + Machine->uixmin,
/*TODO*///				y + Machine->uiymin, 0,TRANSPARENCY_NONE,0);
/*TODO*///		x += Machine->uifontwidth;
/*TODO*///	}
/*TODO*///
/*TODO*///	switch_true_orientation();
/*TODO*///}
/*TODO*///
/*TODO*////* Writes messages on the screen. */
/*TODO*///void ui_text(struct mame_bitmap *bitmap,const char *buf,int x,int y)
/*TODO*///{
/*TODO*///	ui_text_ex(bitmap, buf, buf + strlen(buf), x, y, UI_COLOR_NORMAL);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///void ui_drawbox(struct mame_bitmap *bitmap,int leftx,int topy,int width,int height)
/*TODO*///{
/*TODO*///	UINT32 black,white;
/*TODO*///
/*TODO*///	switch_ui_orientation();
/*TODO*///
/*TODO*///	if (leftx < 0) leftx = 0;
/*TODO*///	if (topy < 0) topy = 0;
/*TODO*///	if (width > Machine->uiwidth) width = Machine->uiwidth;
/*TODO*///	if (height > Machine->uiheight) height = Machine->uiheight;
/*TODO*///
/*TODO*///	leftx += Machine->uixmin;
/*TODO*///	topy += Machine->uiymin;
/*TODO*///
/*TODO*///	black = Machine->uifont->colortable[0];
/*TODO*///	white = Machine->uifont->colortable[1];
/*TODO*///
/*TODO*///	plot_box(bitmap,leftx,        topy,         width,  1,       white);
/*TODO*///	plot_box(bitmap,leftx,        topy+height-1,width,  1,       white);
/*TODO*///	plot_box(bitmap,leftx,        topy,         1,      height,  white);
/*TODO*///	plot_box(bitmap,leftx+width-1,topy,         1,      height,  white);
/*TODO*///	plot_box(bitmap,leftx+1,      topy+1,       width-2,height-2,black);
/*TODO*///
/*TODO*///	switch_true_orientation();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///static void drawbar(struct mame_bitmap *bitmap,int leftx,int topy,int width,int height,int percentage,int default_percentage)
/*TODO*///{
/*TODO*///	UINT32 black,white;
/*TODO*///
/*TODO*///
/*TODO*///	switch_ui_orientation();
/*TODO*///
/*TODO*///	if (leftx < 0) leftx = 0;
/*TODO*///	if (topy < 0) topy = 0;
/*TODO*///	if (width > Machine->uiwidth) width = Machine->uiwidth;
/*TODO*///	if (height > Machine->uiheight) height = Machine->uiheight;
/*TODO*///
/*TODO*///	leftx += Machine->uixmin;
/*TODO*///	topy += Machine->uiymin;
/*TODO*///
/*TODO*///	black = Machine->uifont->colortable[0];
/*TODO*///	white = Machine->uifont->colortable[1];
/*TODO*///
/*TODO*///	plot_box(bitmap,leftx+(width-1)*default_percentage/100,topy,1,height/8,white);
/*TODO*///
/*TODO*///	plot_box(bitmap,leftx,topy+height/8,width,1,white);
/*TODO*///
/*TODO*///	plot_box(bitmap,leftx,topy+height/8,1+(width-1)*percentage/100,height-2*(height/8),white);
/*TODO*///
/*TODO*///	plot_box(bitmap,leftx,topy+height-height/8-1,width,1,white);
/*TODO*///
/*TODO*///	plot_box(bitmap,leftx+(width-1)*default_percentage/100,topy+height-height/8,1,height/8,white);
/*TODO*///
/*TODO*///	switch_true_orientation();
/*TODO*///}
/*TODO*///
/*TODO*////* Extract one line from a multiline buffer */
/*TODO*////* Return the characters number of the line, pbegin point to the start of the next line */
/*TODO*///static unsigned multiline_extract(const char** pbegin, const char* end, unsigned max)
/*TODO*///{
/*TODO*///	unsigned mac = 0;
/*TODO*///	const char* begin = *pbegin;
/*TODO*///	while (begin != end && mac < max)
/*TODO*///	{
/*TODO*///		if (*begin == '\n')
/*TODO*///		{
/*TODO*///			*pbegin = begin + 1; /* strip final space */
/*TODO*///			return mac;
/*TODO*///		}
/*TODO*///		else if (*begin == ' ')
/*TODO*///		{
/*TODO*///			const char* word_end = begin + 1;
/*TODO*///			while (word_end != end && *word_end != ' ' && *word_end != '\n')
/*TODO*///				++word_end;
/*TODO*///			if (mac + word_end - begin > max)
/*TODO*///			{
/*TODO*///				if (mac)
/*TODO*///				{
/*TODO*///					*pbegin = begin + 1;
/*TODO*///					return mac; /* strip final space */
/*TODO*///				} else {
/*TODO*///					*pbegin = begin + max;
/*TODO*///					return max;
/*TODO*///				}
/*TODO*///			}
/*TODO*///			mac += word_end - begin;
/*TODO*///			begin = word_end;
/*TODO*///		} else {
/*TODO*///			++mac;
/*TODO*///			++begin;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	if (begin != end && (*begin == '\n' || *begin == ' '))
/*TODO*///		++begin;
/*TODO*///	*pbegin = begin;
/*TODO*///	return mac;
/*TODO*///}
/*TODO*///
/*TODO*////* Compute the output size of a multiline string */
/*TODO*///static void multiline_size(int* dx, int* dy, const char* begin, const char* end, unsigned max)
/*TODO*///{
/*TODO*///	unsigned rows = 0;
/*TODO*///	unsigned cols = 0;
/*TODO*///	while (begin != end)
/*TODO*///	{
/*TODO*///		unsigned len;
/*TODO*///		len = multiline_extract(&begin,end,max);
/*TODO*///		if (len > cols)
/*TODO*///			cols = len;
/*TODO*///		++rows;
/*TODO*///	}
/*TODO*///	*dx = cols * Machine->uifontwidth;
/*TODO*///	*dy = (rows-1) * 3*Machine->uifontheight/2 + Machine->uifontheight;
/*TODO*///}
/*TODO*///
/*TODO*////* Compute the output size of a multiline string with box */
/*TODO*///static void multilinebox_size(int* dx, int* dy, const char* begin, const char* end, unsigned max)
/*TODO*///{
/*TODO*///	multiline_size(dx,dy,begin,end,max);
/*TODO*///	*dx += Machine->uifontwidth;
/*TODO*///	*dy += Machine->uifontheight;
/*TODO*///}
/*TODO*///
/*TODO*////* Display a multiline string */
/*TODO*///static void ui_multitext_ex(struct mame_bitmap *bitmap, const char* begin, const char* end, unsigned max, int x, int y, int color)
/*TODO*///{
/*TODO*///	while (begin != end)
/*TODO*///	{
/*TODO*///		const char* line_begin = begin;
/*TODO*///		unsigned len = multiline_extract(&begin,end,max);
/*TODO*///		ui_text_ex(bitmap, line_begin, line_begin + len,x,y,color);
/*TODO*///		y += 3*Machine->uifontheight/2;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*////* Display a multiline string with box */
/*TODO*///static void ui_multitextbox_ex(struct mame_bitmap *bitmap,const char* begin, const char* end, unsigned max, int x, int y, int dx, int dy, int color)
/*TODO*///{
/*TODO*///	ui_drawbox(bitmap,x,y,dx,dy);
/*TODO*///	x += Machine->uifontwidth/2;
/*TODO*///	y += Machine->uifontheight/2;
/*TODO*///	ui_multitext_ex(bitmap,begin,end,max,x,y,color);
/*TODO*///}
/*TODO*///
/*TODO*///void ui_displaymenu(struct mame_bitmap *bitmap,const char **items,const char **subitems,char *flag,int selected,int arrowize_subitem)
/*TODO*///{
/*TODO*///	struct DisplayText dt[256];
/*TODO*///	int curr_dt;
/*TODO*///	const char *lefthilight = ui_getstring (UI_lefthilight);
/*TODO*///	const char *righthilight = ui_getstring (UI_righthilight);
/*TODO*///	const char *uparrow = ui_getstring (UI_uparrow);
/*TODO*///	const char *downarrow = ui_getstring (UI_downarrow);
/*TODO*///	const char *leftarrow = ui_getstring (UI_leftarrow);
/*TODO*///	const char *rightarrow = ui_getstring (UI_rightarrow);
/*TODO*///	int i,count,len,maxlen,highlen;
/*TODO*///	int leftoffs,topoffs,visible,topitem;
/*TODO*///	int selected_long;
/*TODO*///
/*TODO*///
/*TODO*///	i = 0;
/*TODO*///	maxlen = 0;
/*TODO*///	highlen = Machine->uiwidth / Machine->uifontwidth;
/*TODO*///	while (items[i])
/*TODO*///	{
/*TODO*///		len = 3 + strlen(items[i]);
/*TODO*///		if (subitems && subitems[i])
/*TODO*///			len += 2 + strlen(subitems[i]);
/*TODO*///		if (len > maxlen && len <= highlen)
/*TODO*///			maxlen = len;
/*TODO*///		i++;
/*TODO*///	}
/*TODO*///	count = i;
/*TODO*///
/*TODO*///	visible = Machine->uiheight / (3 * Machine->uifontheight / 2) - 1;
/*TODO*///	topitem = 0;
/*TODO*///	if (visible > count) visible = count;
/*TODO*///	else
/*TODO*///	{
/*TODO*///		topitem = selected - visible / 2;
/*TODO*///		if (topitem < 0) topitem = 0;
/*TODO*///		if (topitem > count - visible) topitem = count - visible;
/*TODO*///	}
/*TODO*///
/*TODO*///	leftoffs = (Machine->uiwidth - maxlen * Machine->uifontwidth) / 2;
/*TODO*///	topoffs = (Machine->uiheight - (3 * visible + 1) * Machine->uifontheight / 2) / 2;
/*TODO*///
/*TODO*///	/* black background */
/*TODO*///	ui_drawbox(bitmap,leftoffs,topoffs,maxlen * Machine->uifontwidth,(3 * visible + 1) * Machine->uifontheight / 2);
/*TODO*///
/*TODO*///	selected_long = 0;
/*TODO*///	curr_dt = 0;
/*TODO*///	for (i = 0;i < visible;i++)
/*TODO*///	{
/*TODO*///		int item = i + topitem;
/*TODO*///
/*TODO*///		if (i == 0 && item > 0)
/*TODO*///		{
/*TODO*///			dt[curr_dt].text = uparrow;
/*TODO*///			dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///			dt[curr_dt].x = (Machine->uiwidth - Machine->uifontwidth * strlen(uparrow)) / 2;
/*TODO*///			dt[curr_dt].y = topoffs + (3*i+1)*Machine->uifontheight/2;
/*TODO*///			curr_dt++;
/*TODO*///		}
/*TODO*///		else if (i == visible - 1 && item < count - 1)
/*TODO*///		{
/*TODO*///			dt[curr_dt].text = downarrow;
/*TODO*///			dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///			dt[curr_dt].x = (Machine->uiwidth - Machine->uifontwidth * strlen(downarrow)) / 2;
/*TODO*///			dt[curr_dt].y = topoffs + (3*i+1)*Machine->uifontheight/2;
/*TODO*///			curr_dt++;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			if (subitems && subitems[item])
/*TODO*///			{
/*TODO*///				int sublen;
/*TODO*///				len = strlen(items[item]);
/*TODO*///				dt[curr_dt].text = items[item];
/*TODO*///				dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///				dt[curr_dt].x = leftoffs + 3*Machine->uifontwidth/2;
/*TODO*///				dt[curr_dt].y = topoffs + (3*i+1)*Machine->uifontheight/2;
/*TODO*///				curr_dt++;
/*TODO*///				sublen = strlen(subitems[item]);
/*TODO*///				if (sublen > maxlen-5-len)
/*TODO*///				{
/*TODO*///					dt[curr_dt].text = "...";
/*TODO*///					sublen = strlen(dt[curr_dt].text);
/*TODO*///					if (item == selected)
/*TODO*///						selected_long = 1;
/*TODO*///				} else {
/*TODO*///					dt[curr_dt].text = subitems[item];
/*TODO*///				}
/*TODO*///				/* If this item is flagged, draw it in inverse print */
/*TODO*///				dt[curr_dt].color = (flag && flag[item]) ? UI_COLOR_INVERSE : UI_COLOR_NORMAL;
/*TODO*///				dt[curr_dt].x = leftoffs + Machine->uifontwidth * (maxlen-1-sublen) - Machine->uifontwidth/2;
/*TODO*///				dt[curr_dt].y = topoffs + (3*i+1)*Machine->uifontheight/2;
/*TODO*///				curr_dt++;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				dt[curr_dt].text = items[item];
/*TODO*///				dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///				dt[curr_dt].x = (Machine->uiwidth - Machine->uifontwidth * strlen(items[item])) / 2;
/*TODO*///				dt[curr_dt].y = topoffs + (3*i+1)*Machine->uifontheight/2;
/*TODO*///				curr_dt++;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	i = selected - topitem;
/*TODO*///	if (subitems && subitems[selected] && arrowize_subitem)
/*TODO*///	{
/*TODO*///		if (arrowize_subitem & 1)
/*TODO*///		{
/*TODO*///			int sublen;
/*TODO*///
/*TODO*///			len = strlen(items[selected]);
/*TODO*///
/*TODO*///			dt[curr_dt].text = leftarrow;
/*TODO*///			dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///
/*TODO*///			sublen = strlen(subitems[selected]);
/*TODO*///			if (sublen > maxlen-5-len)
/*TODO*///				sublen = strlen("...");
/*TODO*///
/*TODO*///			dt[curr_dt].x = leftoffs + Machine->uifontwidth * (maxlen-2 - sublen) - Machine->uifontwidth/2 - 1;
/*TODO*///			dt[curr_dt].y = topoffs + (3*i+1)*Machine->uifontheight/2;
/*TODO*///			curr_dt++;
/*TODO*///		}
/*TODO*///		if (arrowize_subitem & 2)
/*TODO*///		{
/*TODO*///			dt[curr_dt].text = rightarrow;
/*TODO*///			dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///			dt[curr_dt].x = leftoffs + Machine->uifontwidth * (maxlen-1) - Machine->uifontwidth/2;
/*TODO*///			dt[curr_dt].y = topoffs + (3*i+1)*Machine->uifontheight/2;
/*TODO*///			curr_dt++;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		dt[curr_dt].text = righthilight;
/*TODO*///		dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///		dt[curr_dt].x = leftoffs + Machine->uifontwidth * (maxlen-1) - Machine->uifontwidth/2;
/*TODO*///		dt[curr_dt].y = topoffs + (3*i+1)*Machine->uifontheight/2;
/*TODO*///		curr_dt++;
/*TODO*///	}
/*TODO*///	dt[curr_dt].text = lefthilight;
/*TODO*///	dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///	dt[curr_dt].x = leftoffs + Machine->uifontwidth/2;
/*TODO*///	dt[curr_dt].y = topoffs + (3*i+1)*Machine->uifontheight/2;
/*TODO*///	curr_dt++;
/*TODO*///
/*TODO*///	dt[curr_dt].text = 0;	/* terminate array */
/*TODO*///
/*TODO*///	displaytext(bitmap,dt);
/*TODO*///
/*TODO*///	if (selected_long)
/*TODO*///	{
/*TODO*///		int long_dx;
/*TODO*///		int long_dy;
/*TODO*///		int long_x;
/*TODO*///		int long_y;
/*TODO*///		unsigned long_max;
/*TODO*///
/*TODO*///		long_max = (Machine->uiwidth / Machine->uifontwidth) - 2;
/*TODO*///		multilinebox_size(&long_dx,&long_dy,subitems[selected],subitems[selected] + strlen(subitems[selected]), long_max);
/*TODO*///
/*TODO*///		long_x = Machine->uiwidth - long_dx;
/*TODO*///		long_y = topoffs + (i+1) * 3*Machine->uifontheight/2;
/*TODO*///
/*TODO*///		/* if too low display up */
/*TODO*///		if (long_y + long_dy > Machine->uiheight)
/*TODO*///			long_y = topoffs + i * 3*Machine->uifontheight/2 - long_dy;
/*TODO*///
/*TODO*///		ui_multitextbox_ex(bitmap,subitems[selected],subitems[selected] + strlen(subitems[selected]), long_max, long_x,long_y,long_dx,long_dy, UI_COLOR_NORMAL);
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///void ui_displaymessagewindow(struct mame_bitmap *bitmap,const char *text)
/*TODO*///{
/*TODO*///	struct DisplayText dt[256];
/*TODO*///	int curr_dt;
/*TODO*///	char *c,*c2;
/*TODO*///	int i,len,maxlen,lines;
/*TODO*///	char textcopy[2048];
/*TODO*///	int leftoffs,topoffs;
/*TODO*///	int maxcols,maxrows;
/*TODO*///
/*TODO*///	maxcols = (Machine->uiwidth / Machine->uifontwidth) - 1;
/*TODO*///	maxrows = (2 * Machine->uiheight - Machine->uifontheight) / (3 * Machine->uifontheight);
/*TODO*///
/*TODO*///	/* copy text, calculate max len, count lines, wrap long lines and crop height to fit */
/*TODO*///	maxlen = 0;
/*TODO*///	lines = 0;
/*TODO*///	c = (char *)text;
/*TODO*///	c2 = textcopy;
/*TODO*///	while (*c)
/*TODO*///	{
/*TODO*///		len = 0;
/*TODO*///		while (*c && *c != '\n')
/*TODO*///		{
/*TODO*///			*c2++ = *c++;
/*TODO*///			len++;
/*TODO*///			if (len == maxcols && *c != '\n')
/*TODO*///			{
/*TODO*///				/* attempt word wrap */
/*TODO*///				char *csave = c, *c2save = c2;
/*TODO*///				int lensave = len;
/*TODO*///
/*TODO*///				/* back up to last space or beginning of line */
/*TODO*///				while (*c != ' ' && *c != '\n' && c > text)
/*TODO*///					--c, --c2, --len;
/*TODO*///
/*TODO*///				/* if no space was found, hard wrap instead */
/*TODO*///				if (*c != ' ')
/*TODO*///					c = csave, c2 = c2save, len = lensave;
/*TODO*///				else
/*TODO*///					c++;
/*TODO*///
/*TODO*///				*c2++ = '\n'; /* insert wrap */
/*TODO*///				break;
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		if (*c == '\n')
/*TODO*///			*c2++ = *c++;
/*TODO*///
/*TODO*///		if (len > maxlen) maxlen = len;
/*TODO*///
/*TODO*///		lines++;
/*TODO*///		if (lines == maxrows)
/*TODO*///			break;
/*TODO*///	}
/*TODO*///	*c2 = '\0';
/*TODO*///
/*TODO*///	maxlen += 1;
/*TODO*///
/*TODO*///	leftoffs = (Machine->uiwidth - Machine->uifontwidth * maxlen) / 2;
/*TODO*///	if (leftoffs < 0) leftoffs = 0;
/*TODO*///	topoffs = (Machine->uiheight - (3 * lines + 1) * Machine->uifontheight / 2) / 2;
/*TODO*///
/*TODO*///	/* black background */
/*TODO*///	ui_drawbox(bitmap,leftoffs,topoffs,maxlen * Machine->uifontwidth,(3 * lines + 1) * Machine->uifontheight / 2);
/*TODO*///
/*TODO*///	curr_dt = 0;
/*TODO*///	c = textcopy;
/*TODO*///	i = 0;
/*TODO*///	while (*c)
/*TODO*///	{
/*TODO*///		c2 = c;
/*TODO*///		while (*c && *c != '\n')
/*TODO*///			c++;
/*TODO*///
/*TODO*///		if (*c == '\n')
/*TODO*///		{
/*TODO*///			*c = '\0';
/*TODO*///			c++;
/*TODO*///		}
/*TODO*///
/*TODO*///		if (*c2 == '\t')    /* center text */
/*TODO*///		{
/*TODO*///			c2++;
/*TODO*///			dt[curr_dt].x = (Machine->uiwidth - Machine->uifontwidth * (c - c2)) / 2;
/*TODO*///		}
/*TODO*///		else
/*TODO*///			dt[curr_dt].x = leftoffs + Machine->uifontwidth/2;
/*TODO*///
/*TODO*///		dt[curr_dt].text = c2;
/*TODO*///		dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///		dt[curr_dt].y = topoffs + (3*i+1)*Machine->uifontheight/2;
/*TODO*///		curr_dt++;
/*TODO*///
/*TODO*///		i++;
/*TODO*///	}
/*TODO*///
/*TODO*///	dt[curr_dt].text = 0;	/* terminate array */
/*TODO*///
/*TODO*///	displaytext(bitmap,dt);
/*TODO*///}
/*TODO*///
/*TODO*///
    public static void showcharset(mame_bitmap bitmap) {
        int i;
        String buf = "";
        int bank, color, firstdrawn;
        int palpage;
        int changed;
        int total_colors = 0;
        IntArray colortable = null;

        bank = -2;
        color = 0;
        firstdrawn = 0;
        palpage = 0;

        changed = 1;

        do {
            int cpx, cpy, skip_chars;

            if (bank >= 0) {
                cpx = Machine.uiwidth / Machine.gfx[bank].width;
                cpy = (Machine.uiheight - Machine.uifontheight) / Machine.gfx[bank].height;
                skip_chars = cpx * cpy;
            } else {
                cpx = cpy = skip_chars = 0;

                if (bank == -2) /* palette */ {
                    total_colors = Machine.drv.total_colors;
                    colortable = new IntArray(Machine.pens);
                } else if (bank == -1) /* clut */ {
                    total_colors = Machine.drv.color_table_len;
                    colortable = Machine.remapped_colortable;
                }
            }

            if (changed != 0) {
                int lastdrawn = 0;

                erase_screen(bitmap);

                /* validity check after char bank change */
                if (bank >= 0) {
                    if (firstdrawn >= Machine.gfx[bank].total_elements) {
                        firstdrawn = Machine.gfx[bank].total_elements - skip_chars;
                        if (firstdrawn < 0) {
                            firstdrawn = 0;
                        }
                    }
                }

                switch_ui_orientation();

                if (bank >= 0) {
                    int flipx, flipy;

                    flipx = (Machine.orientation ^ trueorientation) & ORIENTATION_FLIP_X;
                    flipy = (Machine.orientation ^ trueorientation) & ORIENTATION_FLIP_Y;

                    if ((Machine.orientation & ORIENTATION_SWAP_XY) != 0) {
                        int t;
                        t = flipx;
                        flipx = flipy;
                        flipy = t;
                    }

                    for (i = 0; i + firstdrawn < Machine.gfx[bank].total_elements && i < cpx * cpy; i++) {
                        drawgfx(bitmap, Machine.gfx[bank],
                                i + firstdrawn, color, /*sprite num, color*/
                                flipx, flipy,
                                (i % cpx) * Machine.gfx[bank].width + Machine.uixmin,
                                Machine.uifontheight + (i / cpx) * Machine.gfx[bank].height + Machine.uiymin,
                                null, Machine.gfx[bank].colortable != null ? TRANSPARENCY_NONE : TRANSPARENCY_NONE_RAW, 0);

                        lastdrawn = i + firstdrawn;
                    }
                } else {
                    if (total_colors != 0) {
                        int sx, sy, colors;

                        colors = total_colors - 256 * palpage;
                        if (colors > 256) {
                            colors = 256;
                        }

                        for (i = 0; i < 16; i++) {
                            String bf = "";

                            sx = 3 * Machine.uifontwidth + (Machine.uifontwidth * 4 / 3) * (i % 16);
                            bf = sprintf("%X", i);
                            ui_text(bitmap, bf, sx, 2 * Machine.uifontheight);
                            if (16 * i < colors) {
                                sy = 3 * Machine.uifontheight + (Machine.uifontheight) * (i % 16);
                                bf = sprintf("%3X", i + 16 * palpage);
                                ui_text(bitmap, bf, 0, sy);
                            }
                        }

                        for (i = 0; i < colors; i++) {
                            sx = Machine.uixmin + 3 * Machine.uifontwidth + (Machine.uifontwidth * 4 / 3) * (i % 16);
                            sy = Machine.uiymin + 2 * Machine.uifontheight + (Machine.uifontheight) * (i / 16) + Machine.uifontheight;
                            plot_box.handler(bitmap, sx, sy, Machine.uifontwidth * 4 / 3, Machine.uifontheight, colortable.read(i + 256 * palpage));
                        }
                    } else {
                        ui_text(bitmap, "N/A", 3 * Machine.uifontwidth, 2 * Machine.uifontheight);
                    }
                }

                switch_true_orientation();

                if (bank >= 0) {
                    sprintf(buf, "GFXSET %d COLOR %2X CODE %X-%X", bank, color, firstdrawn, lastdrawn);
                } else if (bank == -2) {
                    buf = "PALETTE";
                } else if (bank == -1) {
                    buf = "CLUT";
                }
                ui_text(bitmap, buf, 0, 0);

                changed = 0;
            }

            update_video_and_audio();

            if (code_pressed(KEYCODE_LCONTROL) != 0 || code_pressed(KEYCODE_RCONTROL) != 0) {
                skip_chars = cpx;
            }
            if (code_pressed(KEYCODE_LSHIFT) != 0 || code_pressed(KEYCODE_RSHIFT) != 0) {
                skip_chars = 1;
            }

            if (input_ui_pressed_repeat(IPT_UI_RIGHT, 8) != 0) {
                int next;

                next = bank + 1;
                if (next == -1 && Machine.drv.color_table_len == 0) {
                    next++;
                }

                if (next < 0 || (next < MAX_GFX_ELEMENTS && Machine.gfx[next] != null)) {
                    bank = next;
//				firstdrawn = 0;
                    changed = 1;
                }
            }

            if (input_ui_pressed_repeat(IPT_UI_LEFT, 8) != 0) {
                if (bank > -2) {
                    bank--;
                    if (bank == -1 && Machine.drv.color_table_len == 0) {
                        bank--;
                    }
//				firstdrawn = 0;
                    changed = 1;
                }
            }

            if (code_pressed_memory_repeat(KEYCODE_PGDN, 4) != 0) {
                if (bank >= 0) {
                    if (firstdrawn + skip_chars < Machine.gfx[bank].total_elements) {
                        firstdrawn += skip_chars;
                        changed = 1;
                    }
                } else {
                    if (256 * (palpage + 1) < total_colors) {
                        palpage++;
                        changed = 1;
                    }
                }
            }

            if (code_pressed_memory_repeat(KEYCODE_PGUP, 4) != 0) {
                if (bank >= 0) {
                    firstdrawn -= skip_chars;
                    if (firstdrawn < 0) {
                        firstdrawn = 0;
                    }
                    changed = 1;
                } else {
                    if (palpage > 0) {
                        palpage--;
                        changed = 1;
                    }
                }
            }

            if (input_ui_pressed_repeat(IPT_UI_UP, 6) != 0) {
                if (bank >= 0) {
                    if (color < Machine.gfx[bank].total_colors - 1) {
                        color++;
                        changed = 1;
                    }
                }
            }

            if (input_ui_pressed_repeat(IPT_UI_DOWN, 6) != 0) {
                if (bank >= 0) {
                    if (color > 0) {
                        color--;
                        changed = 1;
                    }
                }
            }

            if (input_ui_pressed(IPT_UI_SNAPSHOT) != 0) {
                osd_save_snapshot(bitmap);
            }
        } while (input_ui_pressed(IPT_UI_SHOW_GFX) == 0
                && input_ui_pressed(IPT_UI_CANCEL) == 0);

        schedule_full_refresh();
    }

    /*TODO*///
/*TODO*///
/*TODO*///static int setdipswitches(struct mame_bitmap *bitmap,int selected)
/*TODO*///{
/*TODO*///	const char *menu_item[128];
/*TODO*///	const char *menu_subitem[128];
/*TODO*///	struct InputPort *entry[128];
/*TODO*///	char flag[40];
/*TODO*///	int i,sel;
/*TODO*///	struct InputPort *in;
/*TODO*///	int total;
/*TODO*///	int arrowize;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///
/*TODO*///	in = Machine->input_ports;
/*TODO*///
/*TODO*///	total = 0;
/*TODO*///	while (in->type != IPT_END)
/*TODO*///	{
/*TODO*///		if ((in->type & ~IPF_MASK) == IPT_DIPSWITCH_NAME && input_port_name(in) != 0 &&
/*TODO*///				(in->type & IPF_UNUSED) == 0 &&
/*TODO*///				!(!options.cheat && (in->type & IPF_CHEAT)))
/*TODO*///		{
/*TODO*///			entry[total] = in;
/*TODO*///			menu_item[total] = input_port_name(in);
/*TODO*///
/*TODO*///			total++;
/*TODO*///		}
/*TODO*///
/*TODO*///		in++;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (total == 0) return 0;
/*TODO*///
/*TODO*///	menu_item[total] = ui_getstring (UI_returntomain);
/*TODO*///	menu_item[total + 1] = 0;	/* terminate array */
/*TODO*///	total++;
/*TODO*///
/*TODO*///
/*TODO*///	for (i = 0;i < total;i++)
/*TODO*///	{
/*TODO*///		flag[i] = 0; /* TODO: flag the dip if it's not the real default */
/*TODO*///		if (i < total - 1)
/*TODO*///		{
/*TODO*///			in = entry[i] + 1;
/*TODO*///			while ((in->type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING &&
/*TODO*///					in->default_value != entry[i]->default_value)
/*TODO*///				in++;
/*TODO*///
/*TODO*///			if ((in->type & ~IPF_MASK) != IPT_DIPSWITCH_SETTING)
/*TODO*///				menu_subitem[i] = ui_getstring (UI_INVALID);
/*TODO*///			else menu_subitem[i] = input_port_name(in);
/*TODO*///		}
/*TODO*///		else menu_subitem[i] = 0;	/* no subitem */
/*TODO*///	}
/*TODO*///
/*TODO*///	arrowize = 0;
/*TODO*///	if (sel < total - 1)
/*TODO*///	{
/*TODO*///		in = entry[sel] + 1;
/*TODO*///		while ((in->type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING &&
/*TODO*///				in->default_value != entry[sel]->default_value)
/*TODO*///			in++;
/*TODO*///
/*TODO*///		if ((in->type & ~IPF_MASK) != IPT_DIPSWITCH_SETTING)
/*TODO*///			/* invalid setting: revert to a valid one */
/*TODO*///			arrowize |= 1;
/*TODO*///		else
/*TODO*///		{
/*TODO*///			if (((in-1)->type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING &&
/*TODO*///					!(!options.cheat && ((in-1)->type & IPF_CHEAT)))
/*TODO*///				arrowize |= 1;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	if (sel < total - 1)
/*TODO*///	{
/*TODO*///		in = entry[sel] + 1;
/*TODO*///		while ((in->type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING &&
/*TODO*///				in->default_value != entry[sel]->default_value)
/*TODO*///			in++;
/*TODO*///
/*TODO*///		if ((in->type & ~IPF_MASK) != IPT_DIPSWITCH_SETTING)
/*TODO*///			/* invalid setting: revert to a valid one */
/*TODO*///			arrowize |= 2;
/*TODO*///		else
/*TODO*///		{
/*TODO*///			if (((in+1)->type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING &&
/*TODO*///					!(!options.cheat && ((in+1)->type & IPF_CHEAT)))
/*TODO*///				arrowize |= 2;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,menu_subitem,flag,sel,arrowize);
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///		sel = (sel + 1) % total;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///		sel = (sel + total - 1) % total;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_RIGHT,8))
/*TODO*///	{
/*TODO*///		if (sel < total - 1)
/*TODO*///		{
/*TODO*///			in = entry[sel] + 1;
/*TODO*///			while ((in->type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING &&
/*TODO*///					in->default_value != entry[sel]->default_value)
/*TODO*///				in++;
/*TODO*///
/*TODO*///			if ((in->type & ~IPF_MASK) != IPT_DIPSWITCH_SETTING)
/*TODO*///				/* invalid setting: revert to a valid one */
/*TODO*///				entry[sel]->default_value = (entry[sel]+1)->default_value & entry[sel]->mask;
/*TODO*///			else
/*TODO*///			{
/*TODO*///				if (((in+1)->type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING &&
/*TODO*///						!(!options.cheat && ((in+1)->type & IPF_CHEAT)))
/*TODO*///					entry[sel]->default_value = (in+1)->default_value & entry[sel]->mask;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* tell updatescreen() to clean after us (in case the window changes size) */
/*TODO*///			schedule_full_refresh();
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_LEFT,8))
/*TODO*///	{
/*TODO*///		if (sel < total - 1)
/*TODO*///		{
/*TODO*///			in = entry[sel] + 1;
/*TODO*///			while ((in->type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING &&
/*TODO*///					in->default_value != entry[sel]->default_value)
/*TODO*///				in++;
/*TODO*///
/*TODO*///			if ((in->type & ~IPF_MASK) != IPT_DIPSWITCH_SETTING)
/*TODO*///				/* invalid setting: revert to a valid one */
/*TODO*///				entry[sel]->default_value = (entry[sel]+1)->default_value & entry[sel]->mask;
/*TODO*///			else
/*TODO*///			{
/*TODO*///				if (((in-1)->type & ~IPF_MASK) == IPT_DIPSWITCH_SETTING &&
/*TODO*///						!(!options.cheat && ((in-1)->type & IPF_CHEAT)))
/*TODO*///					entry[sel]->default_value = (in-1)->default_value & entry[sel]->mask;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* tell updatescreen() to clean after us (in case the window changes size) */
/*TODO*///			schedule_full_refresh();
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		if (sel == total - 1) sel = -1;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		schedule_full_refresh();
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*////* This flag is used for record OR sequence of key/joy */
/*TODO*////* when is !=0 the first sequence is record, otherwise the first free */
/*TODO*////* it's used byt setdefkeysettings, setdefjoysettings, setkeysettings, setjoysettings */
/*TODO*///static int record_first_insert = 1;
/*TODO*///
/*TODO*///static char menu_subitem_buffer[400][96];
/*TODO*///
/*TODO*///static int setdefcodesettings(struct mame_bitmap *bitmap,int selected)
/*TODO*///{
/*TODO*///	const char *menu_item[400];
/*TODO*///	const char *menu_subitem[400];
/*TODO*///	struct ipd *entry[400];
/*TODO*///	char flag[400];
/*TODO*///	int i,sel;
/*TODO*///	struct ipd *in;
/*TODO*///	int total;
/*TODO*///	extern struct ipd inputport_defaults[];
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///
/*TODO*///	if (Machine->input_ports == 0)
/*TODO*///		return 0;
/*TODO*///
/*TODO*///	in = inputport_defaults;
/*TODO*///
/*TODO*///	total = 0;
/*TODO*///	while (in->type != IPT_END)
/*TODO*///	{
/*TODO*///		if (in->name != 0  && (in->type & ~IPF_MASK) != IPT_UNKNOWN && (in->type & IPF_UNUSED) == 0
/*TODO*///			&& !(!options.cheat && (in->type & IPF_CHEAT)))
/*TODO*///		{
/*TODO*///			entry[total] = in;
/*TODO*///			menu_item[total] = in->name;
/*TODO*///
/*TODO*///			total++;
/*TODO*///		}
/*TODO*///
/*TODO*///		in++;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (total == 0) return 0;
/*TODO*///
/*TODO*///	menu_item[total] = ui_getstring (UI_returntomain);
/*TODO*///	menu_item[total + 1] = 0;	/* terminate array */
/*TODO*///	total++;
/*TODO*///
/*TODO*///	for (i = 0;i < total;i++)
/*TODO*///	{
/*TODO*///		if (i < total - 1)
/*TODO*///		{
/*TODO*///			seq_name(&entry[i]->seq,menu_subitem_buffer[i],sizeof(menu_subitem_buffer[0]));
/*TODO*///			menu_subitem[i] = menu_subitem_buffer[i];
/*TODO*///		} else
/*TODO*///			menu_subitem[i] = 0;	/* no subitem */
/*TODO*///		flag[i] = 0;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (sel > SEL_MASK)   /* are we waiting for a new key? */
/*TODO*///	{
/*TODO*///		int ret;
/*TODO*///
/*TODO*///		menu_subitem[sel & SEL_MASK] = "    ";
/*TODO*///		ui_displaymenu(bitmap,menu_item,menu_subitem,flag,sel & SEL_MASK,3);
/*TODO*///
/*TODO*///		ret = seq_read_async(&entry[sel & SEL_MASK]->seq,record_first_insert);
/*TODO*///
/*TODO*///		if (ret >= 0)
/*TODO*///		{
/*TODO*///			sel &= 0xff;
/*TODO*///
/*TODO*///			if (ret > 0 || seq_get_1(&entry[sel]->seq) == CODE_NONE)
/*TODO*///			{
/*TODO*///				seq_set_1(&entry[sel]->seq,CODE_NONE);
/*TODO*///				ret = 1;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* tell updatescreen() to clean after us (in case the window changes size) */
/*TODO*///			schedule_full_refresh();
/*TODO*///
/*TODO*///			record_first_insert = ret != 0;
/*TODO*///		}
/*TODO*///
/*TODO*///
/*TODO*///		return sel + 1;
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,menu_subitem,flag,sel,0);
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///	{
/*TODO*///		sel = (sel + 1) % total;
/*TODO*///		record_first_insert = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///	{
/*TODO*///		sel = (sel + total - 1) % total;
/*TODO*///		record_first_insert = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		if (sel == total - 1) sel = -1;
/*TODO*///		else
/*TODO*///		{
/*TODO*///			seq_read_async_start();
/*TODO*///
/*TODO*///			sel |= 1 << SEL_BITS;	/* we'll ask for a key */
/*TODO*///
/*TODO*///			/* tell updatescreen() to clean after us (in case the window changes size) */
/*TODO*///			schedule_full_refresh();
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		/* tell updatescreen() to clean after us */
/*TODO*///		schedule_full_refresh();
/*TODO*///
/*TODO*///		record_first_insert = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///static int setcodesettings(struct mame_bitmap *bitmap,int selected)
/*TODO*///{
/*TODO*///	const char *menu_item[400];
/*TODO*///	const char *menu_subitem[400];
/*TODO*///	struct InputPort *entry[400];
/*TODO*///	char flag[400];
/*TODO*///	int i,sel;
/*TODO*///	struct InputPort *in;
/*TODO*///	int total;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///
/*TODO*///	if (Machine->input_ports == 0)
/*TODO*///		return 0;
/*TODO*///
/*TODO*///	in = Machine->input_ports;
/*TODO*///
/*TODO*///	total = 0;
/*TODO*///	while (in->type != IPT_END)
/*TODO*///	{
/*TODO*///		if (input_port_name(in) != 0 && seq_get_1(&in->seq) != CODE_NONE && (in->type & ~IPF_MASK) != IPT_UNKNOWN)
/*TODO*///		{
/*TODO*///			entry[total] = in;
/*TODO*///			menu_item[total] = input_port_name(in);
/*TODO*///
/*TODO*///			total++;
/*TODO*///		}
/*TODO*///
/*TODO*///		in++;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (total == 0) return 0;
/*TODO*///
/*TODO*///	menu_item[total] = ui_getstring (UI_returntomain);
/*TODO*///	menu_item[total + 1] = 0;	/* terminate array */
/*TODO*///	total++;
/*TODO*///
/*TODO*///	for (i = 0;i < total;i++)
/*TODO*///	{
/*TODO*///		if (i < total - 1)
/*TODO*///		{
/*TODO*///			seq_name(input_port_seq(entry[i]),menu_subitem_buffer[i],sizeof(menu_subitem_buffer[0]));
/*TODO*///			menu_subitem[i] = menu_subitem_buffer[i];
/*TODO*///
/*TODO*///			/* If the key isn't the default, flag it */
/*TODO*///			if (seq_get_1(&entry[i]->seq) != CODE_DEFAULT)
/*TODO*///				flag[i] = 1;
/*TODO*///			else
/*TODO*///				flag[i] = 0;
/*TODO*///
/*TODO*///		} else
/*TODO*///			menu_subitem[i] = 0;	/* no subitem */
/*TODO*///	}
/*TODO*///
/*TODO*///	if (sel > SEL_MASK)   /* are we waiting for a new key? */
/*TODO*///	{
/*TODO*///		int ret;
/*TODO*///
/*TODO*///		menu_subitem[sel & SEL_MASK] = "    ";
/*TODO*///		ui_displaymenu(bitmap,menu_item,menu_subitem,flag,sel & SEL_MASK,3);
/*TODO*///
/*TODO*///		ret = seq_read_async(&entry[sel & SEL_MASK]->seq,record_first_insert);
/*TODO*///
/*TODO*///		if (ret >= 0)
/*TODO*///		{
/*TODO*///			sel &= 0xff;
/*TODO*///
/*TODO*///			if (ret > 0 || seq_get_1(&entry[sel]->seq) == CODE_NONE)
/*TODO*///			{
/*TODO*///				seq_set_1(&entry[sel]->seq, CODE_DEFAULT);
/*TODO*///				ret = 1;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* tell updatescreen() to clean after us (in case the window changes size) */
/*TODO*///			schedule_full_refresh();
/*TODO*///
/*TODO*///			record_first_insert = ret != 0;
/*TODO*///		}
/*TODO*///
/*TODO*///		return sel + 1;
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,menu_subitem,flag,sel,0);
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///	{
/*TODO*///		sel = (sel + 1) % total;
/*TODO*///		record_first_insert = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///	{
/*TODO*///		sel = (sel + total - 1) % total;
/*TODO*///		record_first_insert = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		if (sel == total - 1) sel = -1;
/*TODO*///		else
/*TODO*///		{
/*TODO*///			seq_read_async_start();
/*TODO*///
/*TODO*///			sel |= 1 << SEL_BITS;	/* we'll ask for a key */
/*TODO*///
/*TODO*///			/* tell updatescreen() to clean after us (in case the window changes size) */
/*TODO*///			schedule_full_refresh();
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		schedule_full_refresh();
/*TODO*///
/*TODO*///		record_first_insert = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///static int calibratejoysticks(struct mame_bitmap *bitmap,int selected)
/*TODO*///{
/*TODO*///	const char *msg;
/*TODO*///	static char buf[2048];
/*TODO*///	int sel;
/*TODO*///	static int calibration_started = 0;
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///	if (calibration_started == 0)
/*TODO*///	{
/*TODO*///		osd_joystick_start_calibration();
/*TODO*///		calibration_started = 1;
/*TODO*///		strcpy (buf, "");
/*TODO*///	}
/*TODO*///
/*TODO*///	if (sel > SEL_MASK) /* Waiting for the user to acknowledge joystick movement */
/*TODO*///	{
/*TODO*///		if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		{
/*TODO*///			calibration_started = 0;
/*TODO*///			sel = -1;
/*TODO*///		}
/*TODO*///		else if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///		{
/*TODO*///			osd_joystick_calibrate();
/*TODO*///			sel &= 0xff;
/*TODO*///		}
/*TODO*///
/*TODO*///		ui_displaymessagewindow(bitmap,buf);
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		msg = osd_joystick_calibrate_next();
/*TODO*///		schedule_full_refresh();
/*TODO*///		if (msg == 0)
/*TODO*///		{
/*TODO*///			calibration_started = 0;
/*TODO*///			osd_joystick_end_calibration();
/*TODO*///			sel = -1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			strcpy (buf, msg);
/*TODO*///			ui_displaymessagewindow(bitmap,buf);
/*TODO*///			sel |= 1 << SEL_BITS;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		schedule_full_refresh();
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///static int settraksettings(struct mame_bitmap *bitmap,int selected)
/*TODO*///{
/*TODO*///	const char *menu_item[40];
/*TODO*///	const char *menu_subitem[40];
/*TODO*///	struct InputPort *entry[40];
/*TODO*///	int i,sel;
/*TODO*///	struct InputPort *in;
/*TODO*///	int total,total2;
/*TODO*///	int arrowize;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///
/*TODO*///	if (Machine->input_ports == 0)
/*TODO*///		return 0;
/*TODO*///
/*TODO*///	in = Machine->input_ports;
/*TODO*///
/*TODO*///	/* Count the total number of analog controls */
/*TODO*///	total = 0;
/*TODO*///	while (in->type != IPT_END)
/*TODO*///	{
/*TODO*///		if (((in->type & 0xff) > IPT_ANALOG_START) && ((in->type & 0xff) < IPT_ANALOG_END)
/*TODO*///				&& !(!options.cheat && (in->type & IPF_CHEAT)))
/*TODO*///		{
/*TODO*///			entry[total] = in;
/*TODO*///			total++;
/*TODO*///		}
/*TODO*///		in++;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (total == 0) return 0;
/*TODO*///
/*TODO*///	/* Each analog control has 3 entries - key & joy delta, reverse, sensitivity */
/*TODO*///
/*TODO*///#define ENTRIES 3
/*TODO*///
/*TODO*///	total2 = total * ENTRIES;
/*TODO*///
/*TODO*///	menu_item[total2] = ui_getstring (UI_returntomain);
/*TODO*///	menu_item[total2 + 1] = 0;	/* terminate array */
/*TODO*///	total2++;
/*TODO*///
/*TODO*///	arrowize = 0;
/*TODO*///	for (i = 0;i < total2;i++)
/*TODO*///	{
/*TODO*///		if (i < total2 - 1)
/*TODO*///		{
/*TODO*///			char label[30][40];
/*TODO*///			char setting[30][40];
/*TODO*///			int sensitivity,delta;
/*TODO*///			int reverse;
/*TODO*///
/*TODO*///			strcpy (label[i], input_port_name(entry[i/ENTRIES]));
/*TODO*///			sensitivity = IP_GET_SENSITIVITY(entry[i/ENTRIES]);
/*TODO*///			delta = IP_GET_DELTA(entry[i/ENTRIES]);
/*TODO*///			reverse = (entry[i/ENTRIES]->type & IPF_REVERSE);
/*TODO*///
/*TODO*///			strcat (label[i], " ");
/*TODO*///			switch (i%ENTRIES)
/*TODO*///			{
/*TODO*///				case 0:
/*TODO*///					strcat (label[i], ui_getstring (UI_keyjoyspeed));
/*TODO*///					sprintf(setting[i],"%d",delta);
/*TODO*///					if (i == sel) arrowize = 3;
/*TODO*///					break;
/*TODO*///				case 1:
/*TODO*///					strcat (label[i], ui_getstring (UI_reverse));
/*TODO*///					if (reverse)
/*TODO*///						sprintf(setting[i],ui_getstring (UI_on));
/*TODO*///					else
/*TODO*///						sprintf(setting[i],ui_getstring (UI_off));
/*TODO*///					if (i == sel) arrowize = 3;
/*TODO*///					break;
/*TODO*///				case 2:
/*TODO*///					strcat (label[i], ui_getstring (UI_sensitivity));
/*TODO*///					sprintf(setting[i],"%3d%%",sensitivity);
/*TODO*///					if (i == sel) arrowize = 3;
/*TODO*///					break;
/*TODO*///			}
/*TODO*///
/*TODO*///			menu_item[i] = label[i];
/*TODO*///			menu_subitem[i] = setting[i];
/*TODO*///
/*TODO*///			in++;
/*TODO*///		}
/*TODO*///		else menu_subitem[i] = 0;	/* no subitem */
/*TODO*///	}
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,menu_subitem,0,sel,arrowize);
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///		sel = (sel + 1) % total2;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///		sel = (sel + total2 - 1) % total2;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_LEFT,8))
/*TODO*///	{
/*TODO*///		if ((sel % ENTRIES) == 0)
/*TODO*///		/* keyboard/joystick delta */
/*TODO*///		{
/*TODO*///			int val = IP_GET_DELTA(entry[sel/ENTRIES]);
/*TODO*///
/*TODO*///			val --;
/*TODO*///			if (val < 1) val = 1;
/*TODO*///			IP_SET_DELTA(entry[sel/ENTRIES],val);
/*TODO*///		}
/*TODO*///		else if ((sel % ENTRIES) == 1)
/*TODO*///		/* reverse */
/*TODO*///		{
/*TODO*///			int reverse = entry[sel/ENTRIES]->type & IPF_REVERSE;
/*TODO*///			if (reverse)
/*TODO*///				reverse=0;
/*TODO*///			else
/*TODO*///				reverse=IPF_REVERSE;
/*TODO*///			entry[sel/ENTRIES]->type &= ~IPF_REVERSE;
/*TODO*///			entry[sel/ENTRIES]->type |= reverse;
/*TODO*///		}
/*TODO*///		else if ((sel % ENTRIES) == 2)
/*TODO*///		/* sensitivity */
/*TODO*///		{
/*TODO*///			int val = IP_GET_SENSITIVITY(entry[sel/ENTRIES]);
/*TODO*///
/*TODO*///			val --;
/*TODO*///			if (val < 1) val = 1;
/*TODO*///			IP_SET_SENSITIVITY(entry[sel/ENTRIES],val);
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_RIGHT,8))
/*TODO*///	{
/*TODO*///		if ((sel % ENTRIES) == 0)
/*TODO*///		/* keyboard/joystick delta */
/*TODO*///		{
/*TODO*///			int val = IP_GET_DELTA(entry[sel/ENTRIES]);
/*TODO*///
/*TODO*///			val ++;
/*TODO*///			if (val > 255) val = 255;
/*TODO*///			IP_SET_DELTA(entry[sel/ENTRIES],val);
/*TODO*///		}
/*TODO*///		else if ((sel % ENTRIES) == 1)
/*TODO*///		/* reverse */
/*TODO*///		{
/*TODO*///			int reverse = entry[sel/ENTRIES]->type & IPF_REVERSE;
/*TODO*///			if (reverse)
/*TODO*///				reverse=0;
/*TODO*///			else
/*TODO*///				reverse=IPF_REVERSE;
/*TODO*///			entry[sel/ENTRIES]->type &= ~IPF_REVERSE;
/*TODO*///			entry[sel/ENTRIES]->type |= reverse;
/*TODO*///		}
/*TODO*///		else if ((sel % ENTRIES) == 2)
/*TODO*///		/* sensitivity */
/*TODO*///		{
/*TODO*///			int val = IP_GET_SENSITIVITY(entry[sel/ENTRIES]);
/*TODO*///
/*TODO*///			val ++;
/*TODO*///			if (val > 255) val = 255;
/*TODO*///			IP_SET_SENSITIVITY(entry[sel/ENTRIES],val);
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		if (sel == total2 - 1) sel = -1;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		schedule_full_refresh();
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///#ifndef MESS
/*TODO*///static int mame_stats(struct mame_bitmap *bitmap,int selected)
/*TODO*///{
/*TODO*///	char temp[10];
/*TODO*///	char buf[2048];
/*TODO*///	int sel, i;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///	buf[0] = 0;
/*TODO*///
/*TODO*///	if (dispensed_tickets)
/*TODO*///	{
/*TODO*///		strcat(buf, ui_getstring (UI_tickets));
/*TODO*///		strcat(buf, ": ");
/*TODO*///		sprintf(temp, "%d\n\n", dispensed_tickets);
/*TODO*///		strcat(buf, temp);
/*TODO*///	}
/*TODO*///
/*TODO*///	for (i=0; i<COIN_COUNTERS; i++)
/*TODO*///	{
/*TODO*///		strcat(buf, ui_getstring (UI_coin));
/*TODO*///		sprintf(temp, " %c: ", i+'A');
/*TODO*///		strcat(buf, temp);
/*TODO*///		if (!coins[i])
/*TODO*///			strcat (buf, ui_getstring (UI_NA));
/*TODO*///		else
/*TODO*///		{
/*TODO*///			sprintf (temp, "%d", coins[i]);
/*TODO*///			strcat (buf, temp);
/*TODO*///		}
/*TODO*///		if (coinlockedout[i])
/*TODO*///		{
/*TODO*///			strcat(buf, " ");
/*TODO*///			strcat(buf, ui_getstring (UI_locked));
/*TODO*///			strcat(buf, "\n");
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			strcat(buf, "\n");
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	{
/*TODO*///		/* menu system, use the normal menu keys */
/*TODO*///		strcat(buf,"\n\t");
/*TODO*///		strcat(buf,ui_getstring (UI_lefthilight));
/*TODO*///		strcat(buf," ");
/*TODO*///		strcat(buf,ui_getstring (UI_returntomain));
/*TODO*///		strcat(buf," ");
/*TODO*///		strcat(buf,ui_getstring (UI_righthilight));
/*TODO*///
/*TODO*///		ui_displaymessagewindow(bitmap,buf);
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///			sel = -1;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///			sel = -1;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///			sel = -2;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		schedule_full_refresh();
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///#endif
/*TODO*///
/*TODO*///int showcopyright(struct mame_bitmap *bitmap)
/*TODO*///{
/*TODO*///	int done;
/*TODO*///	char buf[1000];
/*TODO*///	char buf2[256];
/*TODO*///
/*TODO*///	strcpy (buf, ui_getstring(UI_copyright1));
/*TODO*///	strcat (buf, "\n\n");
/*TODO*///	sprintf(buf2, ui_getstring(UI_copyright2), Machine->gamedrv->description);
/*TODO*///	strcat (buf, buf2);
/*TODO*///	strcat (buf, "\n\n");
/*TODO*///	strcat (buf, ui_getstring(UI_copyright3));
/*TODO*///
/*TODO*///	erase_screen(bitmap);
/*TODO*///	ui_displaymessagewindow(bitmap,buf);
/*TODO*///
/*TODO*///	setup_selected = -1;////
/*TODO*///	done = 0;
/*TODO*///	do
/*TODO*///	{
/*TODO*///		update_video_and_audio();
/*TODO*///		if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		{
/*TODO*///			setup_selected = 0;////
/*TODO*///			return 1;
/*TODO*///		}
/*TODO*///		if (keyboard_pressed_memory(KEYCODE_O) ||
/*TODO*///				input_ui_pressed(IPT_UI_LEFT))
/*TODO*///			done = 1;
/*TODO*///		if (done == 1 && (keyboard_pressed_memory(KEYCODE_K) ||
/*TODO*///				input_ui_pressed(IPT_UI_RIGHT)))
/*TODO*///			done = 2;
/*TODO*///	} while (done < 2);
/*TODO*///
/*TODO*///	setup_selected = 0;////
/*TODO*///	erase_screen(bitmap);
/*TODO*///	update_video_and_audio();
/*TODO*///
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///static int displaygameinfo(struct mame_bitmap *bitmap,int selected)
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	char buf[2048];
/*TODO*///	char buf2[32];
/*TODO*///	int sel;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///
/*TODO*///	sprintf(buf,"%s\n%s %s\n\n%s:\n",Machine->gamedrv->description,Machine->gamedrv->year,Machine->gamedrv->manufacturer,
/*TODO*///		ui_getstring (UI_cpu));
/*TODO*///	i = 0;
/*TODO*///	while (i < MAX_CPU && Machine->drv->cpu[i].cpu_type)
/*TODO*///	{
/*TODO*///
/*TODO*///		if (Machine->drv->cpu[i].cpu_clock >= 1000000)
/*TODO*///			sprintf(&buf[strlen(buf)],"%s %d.%06d MHz",
/*TODO*///					cputype_name(Machine->drv->cpu[i].cpu_type),
/*TODO*///					Machine->drv->cpu[i].cpu_clock / 1000000,
/*TODO*///					Machine->drv->cpu[i].cpu_clock % 1000000);
/*TODO*///		else
/*TODO*///			sprintf(&buf[strlen(buf)],"%s %d.%03d kHz",
/*TODO*///					cputype_name(Machine->drv->cpu[i].cpu_type),
/*TODO*///					Machine->drv->cpu[i].cpu_clock / 1000,
/*TODO*///					Machine->drv->cpu[i].cpu_clock % 1000);
/*TODO*///
/*TODO*///		if (Machine->drv->cpu[i].cpu_type & CPU_AUDIO_CPU)
/*TODO*///		{
/*TODO*///			sprintf (buf2, " (%s)", ui_getstring (UI_sound_lc));
/*TODO*///			strcat(buf, buf2);
/*TODO*///		}
/*TODO*///
/*TODO*///		strcat(buf,"\n");
/*TODO*///
/*TODO*///		i++;
/*TODO*///	}
/*TODO*///
/*TODO*///	sprintf (buf2, "\n%s", ui_getstring (UI_sound));
/*TODO*///	strcat (buf, buf2);
/*TODO*///	if (Machine->drv->sound_attributes & SOUND_SUPPORTS_STEREO)
/*TODO*///		sprintf(&buf[strlen(buf)]," (%s)", ui_getstring (UI_stereo));
/*TODO*///	strcat(buf,":\n");
/*TODO*///
/*TODO*///	i = 0;
/*TODO*///	while (i < MAX_SOUND && Machine->drv->sound[i].sound_type)
/*TODO*///	{
/*TODO*///		if (sound_num(&Machine->drv->sound[i]))
/*TODO*///			sprintf(&buf[strlen(buf)],"%dx",sound_num(&Machine->drv->sound[i]));
/*TODO*///
/*TODO*///		sprintf(&buf[strlen(buf)],"%s",sound_name(&Machine->drv->sound[i]));
/*TODO*///
/*TODO*///		if (sound_clock(&Machine->drv->sound[i]))
/*TODO*///		{
/*TODO*///			if (sound_clock(&Machine->drv->sound[i]) >= 1000000)
/*TODO*///				sprintf(&buf[strlen(buf)]," %d.%06d MHz",
/*TODO*///						sound_clock(&Machine->drv->sound[i]) / 1000000,
/*TODO*///						sound_clock(&Machine->drv->sound[i]) % 1000000);
/*TODO*///			else
/*TODO*///				sprintf(&buf[strlen(buf)]," %d.%03d kHz",
/*TODO*///						sound_clock(&Machine->drv->sound[i]) / 1000,
/*TODO*///						sound_clock(&Machine->drv->sound[i]) % 1000);
/*TODO*///		}
/*TODO*///
/*TODO*///		strcat(buf,"\n");
/*TODO*///
/*TODO*///		i++;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (Machine->drv->video_attributes & VIDEO_TYPE_VECTOR)
/*TODO*///		sprintf(&buf[strlen(buf)],"\n%s\n", ui_getstring (UI_vectorgame));
/*TODO*///	else
/*TODO*///	{
/*TODO*///		sprintf(&buf[strlen(buf)],"\n%s:\n", ui_getstring (UI_screenres));
/*TODO*///		sprintf(&buf[strlen(buf)],"%d x %d (%s) %f Hz\n",
/*TODO*///				Machine->visible_area.max_x - Machine->visible_area.min_x + 1,
/*TODO*///				Machine->visible_area.max_y - Machine->visible_area.min_y + 1,
/*TODO*///				(Machine->gamedrv->flags & ORIENTATION_SWAP_XY) ? "V" : "H",
/*TODO*///				Machine->drv->frames_per_second);
/*TODO*///#if 0
/*TODO*///		{
/*TODO*///			int pixelx,pixely,tmax,tmin,rem;
/*TODO*///
/*TODO*///			pixelx = 4 * (Machine->visible_area.max_y - Machine->visible_area.min_y + 1);
/*TODO*///			pixely = 3 * (Machine->visible_area.max_x - Machine->visible_area.min_x + 1);
/*TODO*///
/*TODO*///			/* calculate MCD */
/*TODO*///			if (pixelx >= pixely)
/*TODO*///			{
/*TODO*///				tmax = pixelx;
/*TODO*///				tmin = pixely;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				tmax = pixely;
/*TODO*///				tmin = pixelx;
/*TODO*///			}
/*TODO*///			while ( (rem = tmax % tmin) )
/*TODO*///			{
/*TODO*///				tmax = tmin;
/*TODO*///				tmin = rem;
/*TODO*///			}
/*TODO*///			/* tmin is now the MCD */
/*TODO*///
/*TODO*///			pixelx /= tmin;
/*TODO*///			pixely /= tmin;
/*TODO*///
/*TODO*///			sprintf(&buf[strlen(buf)],"pixel aspect ratio %d:%d\n",
/*TODO*///					pixelx,pixely);
/*TODO*///		}
/*TODO*///		sprintf(&buf[strlen(buf)],"%d colors ",Machine->drv->total_colors);
/*TODO*///#endif
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	if (sel == -1)
/*TODO*///	{
/*TODO*///		/* startup info, print MAME version and ask for any key */
/*TODO*///
/*TODO*///		sprintf (buf2, "\n\t%s ", ui_getstring (UI_mame));	/* \t means that the line will be centered */
/*TODO*///		strcat(buf, buf2);
/*TODO*///
/*TODO*///		strcat(buf,build_version);
/*TODO*///		sprintf (buf2, "\n\t%s", ui_getstring (UI_anykey));
/*TODO*///		strcat(buf,buf2);
/*TODO*///		ui_drawbox(bitmap,0,0,Machine->uiwidth,Machine->uiheight);
/*TODO*///		ui_displaymessagewindow(bitmap,buf);
/*TODO*///
/*TODO*///		sel = 0;
/*TODO*///		if (code_read_async() != CODE_NONE)
/*TODO*///			sel = -1;
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		/* menu system, use the normal menu keys */
/*TODO*///		strcat(buf,"\n\t");
/*TODO*///		strcat(buf,ui_getstring (UI_lefthilight));
/*TODO*///		strcat(buf," ");
/*TODO*///		strcat(buf,ui_getstring (UI_returntomain));
/*TODO*///		strcat(buf," ");
/*TODO*///		strcat(buf,ui_getstring (UI_righthilight));
/*TODO*///
/*TODO*///		ui_displaymessagewindow(bitmap,buf);
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///			sel = -1;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///			sel = -1;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///			sel = -2;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		schedule_full_refresh();
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///int showgamewarnings(struct mame_bitmap *bitmap)
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	char buf[2048];
/*TODO*///
/*TODO*///	if (Machine->gamedrv->flags &
/*TODO*///			(GAME_NOT_WORKING | GAME_UNEMULATED_PROTECTION | GAME_WRONG_COLORS | GAME_IMPERFECT_COLORS |
/*TODO*///			  GAME_NO_SOUND | GAME_IMPERFECT_SOUND | GAME_IMPERFECT_GRAPHICS | GAME_NO_COCKTAIL))
/*TODO*///	{
/*TODO*///		int done;
/*TODO*///
/*TODO*///		strcpy(buf, ui_getstring (UI_knownproblems));
/*TODO*///		strcat(buf, "\n\n");
/*TODO*///
/*TODO*///
/*TODO*///		if (Machine->gamedrv->flags & GAME_IMPERFECT_COLORS)
/*TODO*///		{
/*TODO*///			strcat(buf, ui_getstring (UI_imperfectcolors));
/*TODO*///			strcat(buf, "\n");
/*TODO*///		}
/*TODO*///
/*TODO*///		if (Machine->gamedrv->flags & GAME_WRONG_COLORS)
/*TODO*///		{
/*TODO*///			strcat(buf, ui_getstring (UI_wrongcolors));
/*TODO*///			strcat(buf, "\n");
/*TODO*///		}
/*TODO*///
/*TODO*///		if (Machine->gamedrv->flags & GAME_IMPERFECT_GRAPHICS)
/*TODO*///		{
/*TODO*///			strcat(buf, ui_getstring (UI_imperfectgraphics));
/*TODO*///			strcat(buf, "\n");
/*TODO*///		}
/*TODO*///
/*TODO*///		if (Machine->gamedrv->flags & GAME_IMPERFECT_SOUND)
/*TODO*///		{
/*TODO*///			strcat(buf, ui_getstring (UI_imperfectsound));
/*TODO*///			strcat(buf, "\n");
/*TODO*///		}
/*TODO*///
/*TODO*///		if (Machine->gamedrv->flags & GAME_NO_SOUND)
/*TODO*///		{
/*TODO*///			strcat(buf, ui_getstring (UI_nosound));
/*TODO*///			strcat(buf, "\n");
/*TODO*///		}
/*TODO*///
/*TODO*///		if (Machine->gamedrv->flags & GAME_NO_COCKTAIL)
/*TODO*///		{
/*TODO*///			strcat(buf, ui_getstring (UI_nococktail));
/*TODO*///			strcat(buf, "\n");
/*TODO*///		}
/*TODO*///
/*TODO*///		if (Machine->gamedrv->flags & (GAME_NOT_WORKING | GAME_UNEMULATED_PROTECTION))
/*TODO*///		{
/*TODO*///			const struct GameDriver *maindrv;
/*TODO*///			int foundworking;
/*TODO*///
/*TODO*///			if (Machine->gamedrv->flags & GAME_NOT_WORKING)
/*TODO*///			{
/*TODO*///				strcpy(buf, ui_getstring (UI_brokengame));
/*TODO*///				strcat(buf, "\n");
/*TODO*///			}
/*TODO*///			if (Machine->gamedrv->flags & GAME_UNEMULATED_PROTECTION)
/*TODO*///			{
/*TODO*///				strcat(buf, ui_getstring (UI_brokenprotection));
/*TODO*///				strcat(buf, "\n");
/*TODO*///			}
/*TODO*///
/*TODO*///			if (Machine->gamedrv->clone_of && !(Machine->gamedrv->clone_of->flags & NOT_A_DRIVER))
/*TODO*///				maindrv = Machine->gamedrv->clone_of;
/*TODO*///			else maindrv = Machine->gamedrv;
/*TODO*///
/*TODO*///			foundworking = 0;
/*TODO*///			i = 0;
/*TODO*///			while (drivers[i])
/*TODO*///			{
/*TODO*///				if (drivers[i] == maindrv || drivers[i]->clone_of == maindrv)
/*TODO*///				{
/*TODO*///					if ((drivers[i]->flags & (GAME_NOT_WORKING | GAME_UNEMULATED_PROTECTION)) == 0)
/*TODO*///					{
/*TODO*///						if (foundworking == 0)
/*TODO*///						{
/*TODO*///							strcat(buf,"\n\n");
/*TODO*///							strcat(buf, ui_getstring (UI_workingclones));
/*TODO*///							strcat(buf,"\n\n");
/*TODO*///						}
/*TODO*///						foundworking = 1;
/*TODO*///
/*TODO*///						sprintf(&buf[strlen(buf)],"%s\n",drivers[i]->name);
/*TODO*///					}
/*TODO*///				}
/*TODO*///				i++;
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		strcat(buf,"\n\n");
/*TODO*///		strcat(buf,ui_getstring (UI_typeok));
/*TODO*///
/*TODO*///		erase_screen(bitmap);
/*TODO*///		ui_displaymessagewindow(bitmap,buf);
/*TODO*///
/*TODO*///		done = 0;
/*TODO*///		do
/*TODO*///		{
/*TODO*///			update_video_and_audio();
/*TODO*///			if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///				return 1;
/*TODO*///			if (code_pressed_memory(KEYCODE_O) ||
/*TODO*///					input_ui_pressed(IPT_UI_LEFT))
/*TODO*///				done = 1;
/*TODO*///			if (done == 1 && (code_pressed_memory(KEYCODE_K) ||
/*TODO*///					input_ui_pressed(IPT_UI_RIGHT)))
/*TODO*///				done = 2;
/*TODO*///		} while (done < 2);
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	erase_screen(bitmap);
/*TODO*///
/*TODO*///	/* clear the input memory */
/*TODO*///	while (code_read_async() != CODE_NONE) {};
/*TODO*///
/*TODO*///	while (displaygameinfo(bitmap,0) == 1)
/*TODO*///	{
/*TODO*///		update_video_and_audio();
/*TODO*///	}
/*TODO*///
/*TODO*///	erase_screen(bitmap);
/*TODO*///	/* make sure that the screen is really cleared, in case autoframeskip kicked in */
/*TODO*///	update_video_and_audio();
/*TODO*///	update_video_and_audio();
/*TODO*///	update_video_and_audio();
/*TODO*///	update_video_and_audio();
/*TODO*///
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*////* Word-wraps the text in the specified buffer to fit in maxwidth characters per line.
/*TODO*///   The contents of the buffer are modified.
/*TODO*///   Known limitations: Words longer than maxwidth cause the function to fail. */
/*TODO*///static void wordwrap_text_buffer (char *buffer, int maxwidth)
/*TODO*///{
/*TODO*///	int width = 0;
/*TODO*///
/*TODO*///	while (*buffer)
/*TODO*///	{
/*TODO*///		if (*buffer == '\n')
/*TODO*///		{
/*TODO*///			buffer++;
/*TODO*///			width = 0;
/*TODO*///			continue;
/*TODO*///		}
/*TODO*///
/*TODO*///		width++;
/*TODO*///
/*TODO*///		if (width > maxwidth)
/*TODO*///		{
/*TODO*///			/* backtrack until a space is found */
/*TODO*///			while (*buffer != ' ')
/*TODO*///			{
/*TODO*///				buffer--;
/*TODO*///				width--;
/*TODO*///			}
/*TODO*///			if (width < 1) return;	/* word too long */
/*TODO*///
/*TODO*///			/* replace space with a newline */
/*TODO*///			*buffer = '\n';
/*TODO*///		}
/*TODO*///		else
/*TODO*///			buffer++;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///static int count_lines_in_buffer (char *buffer)
/*TODO*///{
/*TODO*///	int lines = 0;
/*TODO*///	char c;
/*TODO*///
/*TODO*///	while ( (c = *buffer++) )
/*TODO*///		if (c == '\n') lines++;
/*TODO*///
/*TODO*///	return lines;
/*TODO*///}
/*TODO*///
/*TODO*////* Display lines from buffer, starting with line 'scroll', in a width x height text window */
/*TODO*///static void display_scroll_message (struct mame_bitmap *bitmap, int *scroll, int width, int height, char *buf)
/*TODO*///{
/*TODO*///	struct DisplayText dt[256];
/*TODO*///	int curr_dt = 0;
/*TODO*///	const char *uparrow = ui_getstring (UI_uparrow);
/*TODO*///	const char *downarrow = ui_getstring (UI_downarrow);
/*TODO*///	char textcopy[2048];
/*TODO*///	char *copy;
/*TODO*///	int leftoffs,topoffs;
/*TODO*///	int first = *scroll;
/*TODO*///	int buflines,showlines;
/*TODO*///	int i;
/*TODO*///
/*TODO*///
/*TODO*///	/* draw box */
/*TODO*///	leftoffs = (Machine->uiwidth - Machine->uifontwidth * (width + 1)) / 2;
/*TODO*///	if (leftoffs < 0) leftoffs = 0;
/*TODO*///	topoffs = (Machine->uiheight - (3 * height + 1) * Machine->uifontheight / 2) / 2;
/*TODO*///	ui_drawbox(bitmap,leftoffs,topoffs,(width + 1) * Machine->uifontwidth,(3 * height + 1) * Machine->uifontheight / 2);
/*TODO*///
/*TODO*///	buflines = count_lines_in_buffer (buf);
/*TODO*///	if (first > 0)
/*TODO*///	{
/*TODO*///		if (buflines <= height)
/*TODO*///			first = 0;
/*TODO*///		else
/*TODO*///		{
/*TODO*///			height--;
/*TODO*///			if (first > (buflines - height))
/*TODO*///				first = buflines - height;
/*TODO*///		}
/*TODO*///		*scroll = first;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (first != 0)
/*TODO*///	{
/*TODO*///		/* indicate that scrolling upward is possible */
/*TODO*///		dt[curr_dt].text = uparrow;
/*TODO*///		dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///		dt[curr_dt].x = (Machine->uiwidth - Machine->uifontwidth * strlen(uparrow)) / 2;
/*TODO*///		dt[curr_dt].y = topoffs + (3*curr_dt+1)*Machine->uifontheight/2;
/*TODO*///		curr_dt++;
/*TODO*///	}
/*TODO*///
/*TODO*///	if ((buflines - first) > height)
/*TODO*///		showlines = height - 1;
/*TODO*///	else
/*TODO*///		showlines = height;
/*TODO*///
/*TODO*///	/* skip to first line */
/*TODO*///	while (first > 0)
/*TODO*///	{
/*TODO*///		char c;
/*TODO*///
/*TODO*///		while ( (c = *buf++) )
/*TODO*///		{
/*TODO*///			if (c == '\n')
/*TODO*///			{
/*TODO*///				first--;
/*TODO*///				break;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* copy 'showlines' lines from buffer, starting with line 'first' */
/*TODO*///	copy = textcopy;
/*TODO*///	for (i = 0; i < showlines; i++)
/*TODO*///	{
/*TODO*///		char *copystart = copy;
/*TODO*///
/*TODO*///		while (*buf && *buf != '\n')
/*TODO*///		{
/*TODO*///			*copy = *buf;
/*TODO*///			copy++;
/*TODO*///			buf++;
/*TODO*///		}
/*TODO*///		*copy = '\0';
/*TODO*///		copy++;
/*TODO*///		if (*buf == '\n')
/*TODO*///			buf++;
/*TODO*///
/*TODO*///		if (*copystart == '\t') /* center text */
/*TODO*///		{
/*TODO*///			copystart++;
/*TODO*///			dt[curr_dt].x = (Machine->uiwidth - Machine->uifontwidth * (copy - copystart)) / 2;
/*TODO*///		}
/*TODO*///		else
/*TODO*///			dt[curr_dt].x = leftoffs + Machine->uifontwidth/2;
/*TODO*///
/*TODO*///		dt[curr_dt].text = copystart;
/*TODO*///		dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///		dt[curr_dt].y = topoffs + (3*curr_dt+1)*Machine->uifontheight/2;
/*TODO*///		curr_dt++;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (showlines == (height - 1))
/*TODO*///	{
/*TODO*///		/* indicate that scrolling downward is possible */
/*TODO*///		dt[curr_dt].text = downarrow;
/*TODO*///		dt[curr_dt].color = UI_COLOR_NORMAL;
/*TODO*///		dt[curr_dt].x = (Machine->uiwidth - Machine->uifontwidth * strlen(downarrow)) / 2;
/*TODO*///		dt[curr_dt].y = topoffs + (3*curr_dt+1)*Machine->uifontheight/2;
/*TODO*///		curr_dt++;
/*TODO*///	}
/*TODO*///
/*TODO*///	dt[curr_dt].text = 0;	/* terminate array */
/*TODO*///
/*TODO*///	displaytext(bitmap,dt);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////* Display text entry for current driver from history.dat and mameinfo.dat. */
/*TODO*///static int displayhistory (struct mame_bitmap *bitmap, int selected)
/*TODO*///{
/*TODO*///	static int scroll = 0;
/*TODO*///	static char *buf = 0;
/*TODO*///	int maxcols,maxrows;
/*TODO*///	int sel;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///
/*TODO*///	maxcols = (Machine->uiwidth / Machine->uifontwidth) - 1;
/*TODO*///	maxrows = (2 * Machine->uiheight - Machine->uifontheight) / (3 * Machine->uifontheight);
/*TODO*///	maxcols -= 2;
/*TODO*///	maxrows -= 8;
/*TODO*///
/*TODO*///	if (!buf)
/*TODO*///	{
/*TODO*///		/* allocate a buffer for the text */
/*TODO*///		#ifndef MESS
/*TODO*///		buf = malloc (8192);
/*TODO*///		#else
/*TODO*///		buf = malloc (200*1024);
/*TODO*///		#endif
/*TODO*///		if (buf)
/*TODO*///		{
/*TODO*///			/* try to load entry */
/*TODO*///			#ifndef MESS
/*TODO*///			if (load_driver_history (Machine->gamedrv, buf, 8192) == 0)
/*TODO*///			#else
/*TODO*///			if (load_driver_history (Machine->gamedrv, buf, 200*1024) == 0)
/*TODO*///			#endif
/*TODO*///			{
/*TODO*///				scroll = 0;
/*TODO*///				wordwrap_text_buffer (buf, maxcols);
/*TODO*///				strcat(buf,"\n\t");
/*TODO*///				strcat(buf,ui_getstring (UI_lefthilight));
/*TODO*///				strcat(buf," ");
/*TODO*///				strcat(buf,ui_getstring (UI_returntomain));
/*TODO*///				strcat(buf," ");
/*TODO*///				strcat(buf,ui_getstring (UI_righthilight));
/*TODO*///				strcat(buf,"\n");
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				free (buf);
/*TODO*///				buf = 0;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	{
/*TODO*///		if (buf)
/*TODO*///			display_scroll_message (bitmap, &scroll, maxcols, maxrows, buf);
/*TODO*///		else
/*TODO*///		{
/*TODO*///			char msg[80];
/*TODO*///
/*TODO*///			strcpy(msg,"\t");
/*TODO*///			strcat(msg,ui_getstring(UI_historymissing));
/*TODO*///			strcat(msg,"\n\n\t");
/*TODO*///			strcat(msg,ui_getstring (UI_lefthilight));
/*TODO*///			strcat(msg," ");
/*TODO*///			strcat(msg,ui_getstring (UI_returntomain));
/*TODO*///			strcat(msg," ");
/*TODO*///			strcat(msg,ui_getstring (UI_righthilight));
/*TODO*///			ui_displaymessagewindow(bitmap,msg);
/*TODO*///		}
/*TODO*///
/*TODO*///		if ((scroll > 0) && input_ui_pressed_repeat(IPT_UI_UP,4))
/*TODO*///		{
/*TODO*///			if (scroll == 2) scroll = 0;	/* 1 would be the same as 0, but with arrow on top */
/*TODO*///			else scroll--;
/*TODO*///		}
/*TODO*///
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_DOWN,4))
/*TODO*///		{
/*TODO*///			if (scroll == 0) scroll = 2;	/* 1 would be the same as 0, but with arrow on top */
/*TODO*///			else scroll++;
/*TODO*///		}
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///			sel = -1;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///			sel = -1;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///			sel = -2;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		schedule_full_refresh();
/*TODO*///
/*TODO*///		/* force buffer to be recreated */
/*TODO*///		if (buf)
/*TODO*///		{
/*TODO*///			free (buf);
/*TODO*///			buf = 0;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///#ifndef MESS
/*TODO*///#ifndef TINY_COMPILE
/*TODO*///#ifndef CPSMAME
/*TODO*///int memcard_menu(struct mame_bitmap *bitmap, int selection)
/*TODO*///{
/*TODO*///	int sel;
/*TODO*///	int menutotal = 0;
/*TODO*///	const char *menuitem[10];
/*TODO*///	char buf[256];
/*TODO*///	char buf2[256];
/*TODO*///
/*TODO*///	sel = selection - 1 ;
/*TODO*///
/*TODO*///	sprintf(buf, "%s %03d", ui_getstring (UI_loadcard), mcd_number);
/*TODO*///	menuitem[menutotal++] = buf;
/*TODO*///	menuitem[menutotal++] = ui_getstring (UI_ejectcard);
/*TODO*///	menuitem[menutotal++] = ui_getstring (UI_createcard);
/*TODO*///	menuitem[menutotal++] = ui_getstring (UI_resetcard);
/*TODO*///	menuitem[menutotal++] = ui_getstring (UI_returntomain);
/*TODO*///	menuitem[menutotal] = 0;
/*TODO*///
/*TODO*///	if (mcd_action!=0)
/*TODO*///	{
/*TODO*///		strcpy (buf2, "\n");
/*TODO*///
/*TODO*///		switch(mcd_action)
/*TODO*///		{
/*TODO*///			case 1:
/*TODO*///				strcat (buf2, ui_getstring (UI_loadfailed));
/*TODO*///				break;
/*TODO*///			case 2:
/*TODO*///				strcat (buf2, ui_getstring (UI_loadok));
/*TODO*///				break;
/*TODO*///			case 3:
/*TODO*///				strcat (buf2, ui_getstring (UI_cardejected));
/*TODO*///				break;
/*TODO*///			case 4:
/*TODO*///				strcat (buf2, ui_getstring (UI_cardcreated));
/*TODO*///				break;
/*TODO*///			case 5:
/*TODO*///				strcat (buf2, ui_getstring (UI_cardcreatedfailed));
/*TODO*///				strcat (buf2, "\n");
/*TODO*///				strcat (buf2, ui_getstring (UI_cardcreatedfailed2));
/*TODO*///				break;
/*TODO*///			default:
/*TODO*///				strcat (buf2, ui_getstring (UI_carderror));
/*TODO*///				break;
/*TODO*///		}
/*TODO*///
/*TODO*///		strcat (buf2, "\n\n");
/*TODO*///		ui_displaymessagewindow(bitmap,buf2);
/*TODO*///		if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///			mcd_action = 0;
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		ui_displaymenu(bitmap,menuitem,0,0,sel,0);
/*TODO*///
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_RIGHT,8))
/*TODO*///			mcd_number = (mcd_number + 1) % 1000;
/*TODO*///
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_LEFT,8))
/*TODO*///			mcd_number = (mcd_number + 999) % 1000;
/*TODO*///
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///			sel = (sel + 1) % menutotal;
/*TODO*///
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///			sel = (sel + menutotal - 1) % menutotal;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///		{
/*TODO*///			switch(sel)
/*TODO*///			{
/*TODO*///			case 0:
/*TODO*///				neogeo_memcard_eject();
/*TODO*///				if (neogeo_memcard_load(mcd_number))
/*TODO*///				{
/*TODO*///					memcard_status=1;
/*TODO*///					memcard_number=mcd_number;
/*TODO*///					mcd_action = 2;
/*TODO*///				}
/*TODO*///				else
/*TODO*///					mcd_action = 1;
/*TODO*///				break;
/*TODO*///			case 1:
/*TODO*///				neogeo_memcard_eject();
/*TODO*///				mcd_action = 3;
/*TODO*///				break;
/*TODO*///			case 2:
/*TODO*///				if (neogeo_memcard_create(mcd_number))
/*TODO*///					mcd_action = 4;
/*TODO*///				else
/*TODO*///					mcd_action = 5;
/*TODO*///				break;
/*TODO*///			case 3:
/*TODO*///				memcard_manager=1;
/*TODO*///				sel=-2;
/*TODO*///				machine_reset();
/*TODO*///				break;
/*TODO*///			case 4:
/*TODO*///				sel=-1;
/*TODO*///				break;
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///			sel = -1;
/*TODO*///
/*TODO*///		if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///			sel = -2;
/*TODO*///
/*TODO*///		if (sel == -1 || sel == -2)
/*TODO*///		{
/*TODO*///			schedule_full_refresh();
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///#endif
/*TODO*///#endif
/*TODO*///#endif
/*TODO*///
/*TODO*///
/*TODO*///enum { UI_SWITCH = 0,UI_DEFCODE,UI_CODE,UI_ANALOG,UI_CALIBRATE,
/*TODO*///		UI_STATS,UI_GAMEINFO, UI_HISTORY,
/*TODO*///		UI_CHEAT,UI_RESET,UI_MEMCARD,UI_EXIT };
/*TODO*///
/*TODO*///
/*TODO*///#define MAX_SETUPMENU_ITEMS 20
/*TODO*///static const char *menu_item[MAX_SETUPMENU_ITEMS];
/*TODO*///static int menu_action[MAX_SETUPMENU_ITEMS];
/*TODO*///static int menu_total;
/*TODO*///
/*TODO*///
/*TODO*///static void setup_menu_init(void)
/*TODO*///{
/*TODO*///	menu_total = 0;
/*TODO*///
/*TODO*///	menu_item[menu_total] = ui_getstring (UI_inputgeneral); menu_action[menu_total++] = UI_DEFCODE;
/*TODO*///	menu_item[menu_total] = ui_getstring (UI_inputspecific); menu_action[menu_total++] = UI_CODE;
/*TODO*///	menu_item[menu_total] = ui_getstring (UI_dipswitches); menu_action[menu_total++] = UI_SWITCH;
/*TODO*///
/*TODO*///	/* Determine if there are any analog controls */
/*TODO*///	{
/*TODO*///		struct InputPort *in;
/*TODO*///		int num;
/*TODO*///
/*TODO*///		in = Machine->input_ports;
/*TODO*///
/*TODO*///		num = 0;
/*TODO*///		while (in->type != IPT_END)
/*TODO*///		{
/*TODO*///			if (((in->type & 0xff) > IPT_ANALOG_START) && ((in->type & 0xff) < IPT_ANALOG_END)
/*TODO*///					&& !(!options.cheat && (in->type & IPF_CHEAT)))
/*TODO*///				num++;
/*TODO*///			in++;
/*TODO*///		}
/*TODO*///
/*TODO*///		if (num != 0)
/*TODO*///		{
/*TODO*///			menu_item[menu_total] = ui_getstring (UI_analogcontrols); menu_action[menu_total++] = UI_ANALOG;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* Joystick calibration possible? */
/*TODO*///	if ((osd_joystick_needs_calibration()) != 0)
/*TODO*///	{
/*TODO*///		menu_item[menu_total] = ui_getstring (UI_calibrate); menu_action[menu_total++] = UI_CALIBRATE;
/*TODO*///	}
/*TODO*///
/*TODO*///	menu_item[menu_total] = ui_getstring (UI_bookkeeping); menu_action[menu_total++] = UI_STATS;
/*TODO*///	menu_item[menu_total] = ui_getstring (UI_gameinfo); menu_action[menu_total++] = UI_GAMEINFO;
/*TODO*///	menu_item[menu_total] = ui_getstring (UI_history); menu_action[menu_total++] = UI_HISTORY;
/*TODO*///
/*TODO*///	if (options.cheat)
/*TODO*///	{
/*TODO*///		menu_item[menu_total] = ui_getstring (UI_cheat); menu_action[menu_total++] = UI_CHEAT;
/*TODO*///	}
/*TODO*///	if (Machine->gamedrv->clone_of == &driver_neogeo ||
/*TODO*///			(Machine->gamedrv->clone_of &&
/*TODO*///				Machine->gamedrv->clone_of->clone_of == &driver_neogeo))
/*TODO*///	{
/*TODO*///		menu_item[menu_total] = ui_getstring (UI_memorycard); menu_action[menu_total++] = UI_MEMCARD;
/*TODO*///	}
/*TODO*///	menu_item[menu_total] = ui_getstring (UI_resetgame); menu_action[menu_total++] = UI_RESET;
/*TODO*///	menu_item[menu_total] = ui_getstring (UI_returntogame); menu_action[menu_total++] = UI_EXIT;
/*TODO*///	menu_item[menu_total] = 0; /* terminate array */
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///static int setup_menu(struct mame_bitmap *bitmap, int selected)
/*TODO*///{
/*TODO*///	int sel,res=-1;
/*TODO*///	static int menu_lastselected = 0;
/*TODO*///
/*TODO*///
/*TODO*///	if (selected == -1)
/*TODO*///		sel = menu_lastselected;
/*TODO*///	else sel = selected - 1;
/*TODO*///
/*TODO*///	if (sel > SEL_MASK)
/*TODO*///	{
/*TODO*///		switch (menu_action[sel & SEL_MASK])
/*TODO*///		{
/*TODO*///			case UI_SWITCH:
/*TODO*///				res = setdipswitches(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///			case UI_DEFCODE:
/*TODO*///				res = setdefcodesettings(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///			case UI_CODE:
/*TODO*///				res = setcodesettings(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///			case UI_ANALOG:
/*TODO*///				res = settraksettings(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///			case UI_CALIBRATE:
/*TODO*///				res = calibratejoysticks(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///			case UI_STATS:
/*TODO*///				res = mame_stats(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///			case UI_GAMEINFO:
/*TODO*///				res = displaygameinfo(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///			case UI_HISTORY:
/*TODO*///				res = displayhistory(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///			case UI_CHEAT:
/*TODO*///				res = cheat_menu(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///			case UI_MEMCARD:
/*TODO*///				res = memcard_menu(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///		}
/*TODO*///
/*TODO*///		if (res == -1)
/*TODO*///		{
/*TODO*///			menu_lastselected = sel;
/*TODO*///			sel = -1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///			sel = (sel & SEL_MASK) | (res << SEL_BITS);
/*TODO*///
/*TODO*///		return sel + 1;
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,0,0,sel,0);
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///		sel = (sel + 1) % menu_total;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///		sel = (sel + menu_total - 1) % menu_total;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		switch (menu_action[sel])
/*TODO*///		{
/*TODO*///			case UI_SWITCH:
/*TODO*///			case UI_DEFCODE:
/*TODO*///			case UI_CODE:
/*TODO*///			case UI_ANALOG:
/*TODO*///			case UI_CALIBRATE:
/*TODO*///			case UI_STATS:
/*TODO*///			case UI_GAMEINFO:
/*TODO*///			case UI_HISTORY:
/*TODO*///			case UI_CHEAT:
/*TODO*///			case UI_MEMCARD:
/*TODO*///				sel |= 1 << SEL_BITS;
/*TODO*///				schedule_full_refresh();
/*TODO*///				break;
/*TODO*///
/*TODO*///			case UI_RESET:
/*TODO*///				machine_reset();
/*TODO*///				break;
/*TODO*///
/*TODO*///			case UI_EXIT:
/*TODO*///				menu_lastselected = 0;
/*TODO*///				sel = -1;
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL) ||
/*TODO*///			input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///	{
/*TODO*///		menu_lastselected = sel;
/*TODO*///		sel = -1;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (sel == -1)
/*TODO*///	{
/*TODO*///		schedule_full_refresh();
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*********************************************************************
/*TODO*///
/*TODO*///  start of On Screen Display handling
/*TODO*///
/*TODO*///*********************************************************************/
/*TODO*///
/*TODO*///static void displayosd(struct mame_bitmap *bitmap,const char *text,int percentage,int default_percentage)
/*TODO*///{
/*TODO*///	struct DisplayText dt[2];
/*TODO*///	int avail;
/*TODO*///
/*TODO*///
/*TODO*///	avail = (Machine->uiwidth / Machine->uifontwidth) * 19 / 20;
/*TODO*///
/*TODO*///	ui_drawbox(bitmap,(Machine->uiwidth - Machine->uifontwidth * avail) / 2,
/*TODO*///			(Machine->uiheight - 7*Machine->uifontheight/2),
/*TODO*///			avail * Machine->uifontwidth,
/*TODO*///			3*Machine->uifontheight);
/*TODO*///
/*TODO*///	avail--;
/*TODO*///
/*TODO*///	drawbar(bitmap,(Machine->uiwidth - Machine->uifontwidth * avail) / 2,
/*TODO*///			(Machine->uiheight - 3*Machine->uifontheight),
/*TODO*///			avail * Machine->uifontwidth,
/*TODO*///			Machine->uifontheight,
/*TODO*///			percentage,default_percentage);
/*TODO*///
/*TODO*///	dt[0].text = text;
/*TODO*///	dt[0].color = UI_COLOR_NORMAL;
/*TODO*///	dt[0].x = (Machine->uiwidth - Machine->uifontwidth * strlen(text)) / 2;
/*TODO*///	dt[0].y = (Machine->uiheight - 2*Machine->uifontheight) + 2;
/*TODO*///	dt[1].text = 0; /* terminate array */
/*TODO*///	displaytext(bitmap,dt);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///static void onscrd_volume(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	char buf[20];
/*TODO*///	int attenuation;
/*TODO*///
/*TODO*///	if (increment)
/*TODO*///	{
/*TODO*///		attenuation = osd_get_mastervolume();
/*TODO*///		attenuation += increment;
/*TODO*///		if (attenuation > 0) attenuation = 0;
/*TODO*///		if (attenuation < -32) attenuation = -32;
/*TODO*///		osd_set_mastervolume(attenuation);
/*TODO*///	}
/*TODO*///	attenuation = osd_get_mastervolume();
/*TODO*///
/*TODO*///	sprintf(buf,"%s %3ddB", ui_getstring (UI_volume), attenuation);
/*TODO*///	displayosd(bitmap,buf,100 * (attenuation + 32) / 32,100);
/*TODO*///}
/*TODO*///
/*TODO*///static void onscrd_mixervol(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	static void *driver = 0;
/*TODO*///	char buf[40];
/*TODO*///	int volume,ch;
/*TODO*///	int doallchannels = 0;
/*TODO*///	int proportional = 0;
/*TODO*///
/*TODO*///
/*TODO*///	if (code_pressed(KEYCODE_LSHIFT) || code_pressed(KEYCODE_RSHIFT))
/*TODO*///		doallchannels = 1;
/*TODO*///	if (!code_pressed(KEYCODE_LCONTROL) && !code_pressed(KEYCODE_RCONTROL))
/*TODO*///		increment *= 5;
/*TODO*///	if (code_pressed(KEYCODE_LALT) || code_pressed(KEYCODE_RALT))
/*TODO*///		proportional = 1;
/*TODO*///
/*TODO*///	if (increment)
/*TODO*///	{
/*TODO*///		if (proportional)
/*TODO*///		{
/*TODO*///			static int old_vol[MIXER_MAX_CHANNELS];
/*TODO*///			float ratio = 1.0;
/*TODO*///			int overflow = 0;
/*TODO*///
/*TODO*///			if (driver != Machine->drv)
/*TODO*///			{
/*TODO*///				driver = (void *)Machine->drv;
/*TODO*///				for (ch = 0; ch < MIXER_MAX_CHANNELS; ch++)
/*TODO*///					old_vol[ch] = mixer_get_mixing_level(ch);
/*TODO*///			}
/*TODO*///
/*TODO*///			volume = mixer_get_mixing_level(arg);
/*TODO*///			if (old_vol[arg])
/*TODO*///				ratio = (float)(volume + increment) / (float)old_vol[arg];
/*TODO*///
/*TODO*///			for (ch = 0; ch < MIXER_MAX_CHANNELS; ch++)
/*TODO*///			{
/*TODO*///				if (mixer_get_name(ch) != 0)
/*TODO*///				{
/*TODO*///					volume = ratio * old_vol[ch];
/*TODO*///					if (volume < 0 || volume > 100)
/*TODO*///						overflow = 1;
/*TODO*///				}
/*TODO*///			}
/*TODO*///
/*TODO*///			if (!overflow)
/*TODO*///			{
/*TODO*///				for (ch = 0; ch < MIXER_MAX_CHANNELS; ch++)
/*TODO*///				{
/*TODO*///					volume = ratio * old_vol[ch];
/*TODO*///					mixer_set_mixing_level(ch,volume);
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			driver = 0; /* force reset of saved volumes */
/*TODO*///
/*TODO*///			volume = mixer_get_mixing_level(arg);
/*TODO*///			volume += increment;
/*TODO*///			if (volume > 100) volume = 100;
/*TODO*///			if (volume < 0) volume = 0;
/*TODO*///
/*TODO*///			if (doallchannels)
/*TODO*///			{
/*TODO*///				for (ch = 0;ch < MIXER_MAX_CHANNELS;ch++)
/*TODO*///					mixer_set_mixing_level(ch,volume);
/*TODO*///			}
/*TODO*///			else
/*TODO*///				mixer_set_mixing_level(arg,volume);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	volume = mixer_get_mixing_level(arg);
/*TODO*///
/*TODO*///	if (proportional)
/*TODO*///		sprintf(buf,"%s %s %3d%%", ui_getstring (UI_allchannels), ui_getstring (UI_relative), volume);
/*TODO*///	else if (doallchannels)
/*TODO*///		sprintf(buf,"%s %s %3d%%", ui_getstring (UI_allchannels), ui_getstring (UI_volume), volume);
/*TODO*///	else
/*TODO*///		sprintf(buf,"%s %s %3d%%",mixer_get_name(arg), ui_getstring (UI_volume), volume);
/*TODO*///	displayosd(bitmap,buf,volume,mixer_get_default_mixing_level(arg));
/*TODO*///}
/*TODO*///
/*TODO*///static void onscrd_brightness(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	char buf[20];
/*TODO*///	int brightness;
/*TODO*///
/*TODO*///
/*TODO*///	if (increment)
/*TODO*///	{
/*TODO*///		brightness = osd_get_brightness();
/*TODO*///		brightness += 5 * increment;
/*TODO*///		if (brightness < 0) brightness = 0;
/*TODO*///		if (brightness > 100) brightness = 100;
/*TODO*///		osd_set_brightness(brightness);
/*TODO*///	}
/*TODO*///	brightness = osd_get_brightness();
/*TODO*///
/*TODO*///	sprintf(buf,"%s %3d%%", ui_getstring (UI_brightness), brightness);
/*TODO*///	displayosd(bitmap,buf,brightness,100);
/*TODO*///}
/*TODO*///
/*TODO*///static void onscrd_gamma(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	char buf[20];
/*TODO*///	float gamma_correction;
/*TODO*///
/*TODO*///	if (increment)
/*TODO*///	{
/*TODO*///		gamma_correction = osd_get_gamma();
/*TODO*///
/*TODO*///		gamma_correction += 0.05 * increment;
/*TODO*///		if (gamma_correction < 0.5) gamma_correction = 0.5;
/*TODO*///		if (gamma_correction > 2.0) gamma_correction = 2.0;
/*TODO*///
/*TODO*///		osd_set_gamma(gamma_correction);
/*TODO*///	}
/*TODO*///	gamma_correction = osd_get_gamma();
/*TODO*///
/*TODO*///	sprintf(buf,"%s %1.2f", ui_getstring (UI_gamma), gamma_correction);
/*TODO*///	displayosd(bitmap,buf,100*(gamma_correction-0.5)/(2.0-0.5),100*(1.0-0.5)/(2.0-0.5));
/*TODO*///}
/*TODO*///
/*TODO*///static void onscrd_vector_flicker(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	char buf[1000];
/*TODO*///	float flicker_correction;
/*TODO*///
/*TODO*///	if (!code_pressed(KEYCODE_LCONTROL) && !code_pressed(KEYCODE_RCONTROL))
/*TODO*///		increment *= 5;
/*TODO*///
/*TODO*///	if (increment)
/*TODO*///	{
/*TODO*///		flicker_correction = vector_get_flicker();
/*TODO*///
/*TODO*///		flicker_correction += increment;
/*TODO*///		if (flicker_correction < 0.0) flicker_correction = 0.0;
/*TODO*///		if (flicker_correction > 100.0) flicker_correction = 100.0;
/*TODO*///
/*TODO*///		vector_set_flicker(flicker_correction);
/*TODO*///	}
/*TODO*///	flicker_correction = vector_get_flicker();
/*TODO*///
/*TODO*///	sprintf(buf,"%s %1.2f", ui_getstring (UI_vectorflicker), flicker_correction);
/*TODO*///	displayosd(bitmap,buf,flicker_correction,0);
/*TODO*///}
/*TODO*///
/*TODO*///static void onscrd_vector_intensity(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	char buf[30];
/*TODO*///	float intensity_correction;
/*TODO*///
/*TODO*///	if (increment)
/*TODO*///	{
/*TODO*///		intensity_correction = vector_get_intensity();
/*TODO*///
/*TODO*///		intensity_correction += 0.05 * increment;
/*TODO*///		if (intensity_correction < 0.5) intensity_correction = 0.5;
/*TODO*///		if (intensity_correction > 3.0) intensity_correction = 3.0;
/*TODO*///
/*TODO*///		vector_set_intensity(intensity_correction);
/*TODO*///	}
/*TODO*///	intensity_correction = vector_get_intensity();
/*TODO*///
/*TODO*///	sprintf(buf,"%s %1.2f", ui_getstring (UI_vectorintensity), intensity_correction);
/*TODO*///	displayosd(bitmap,buf,100*(intensity_correction-0.5)/(3.0-0.5),100*(1.5-0.5)/(3.0-0.5));
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///static void onscrd_overclock(struct mame_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	char buf[30];
/*TODO*///	double overclock;
/*TODO*///	int cpu, doallcpus = 0, oc;
/*TODO*///
/*TODO*///	if (code_pressed(KEYCODE_LSHIFT) || code_pressed(KEYCODE_RSHIFT))
/*TODO*///		doallcpus = 1;
/*TODO*///	if (!code_pressed(KEYCODE_LCONTROL) && !code_pressed(KEYCODE_RCONTROL))
/*TODO*///		increment *= 5;
/*TODO*///	if( increment )
/*TODO*///	{
/*TODO*///		overclock = timer_get_overclock(arg);
/*TODO*///		overclock += 0.01 * increment;
/*TODO*///		if (overclock < 0.01) overclock = 0.01;
/*TODO*///		if (overclock > 2.0) overclock = 2.0;
/*TODO*///		if( doallcpus )
/*TODO*///			for( cpu = 0; cpu < cpu_gettotalcpu(); cpu++ )
/*TODO*///				timer_set_overclock(cpu, overclock);
/*TODO*///		else
/*TODO*///			timer_set_overclock(arg, overclock);
/*TODO*///	}
/*TODO*///
/*TODO*///	oc = 100 * timer_get_overclock(arg) + 0.5;
/*TODO*///
/*TODO*///	if( doallcpus )
/*TODO*///		sprintf(buf,"%s %s %3d%%", ui_getstring (UI_allcpus), ui_getstring (UI_overclock), oc);
/*TODO*///	else
/*TODO*///		sprintf(buf,"%s %s%d %3d%%", ui_getstring (UI_overclock), ui_getstring (UI_cpu), arg, oc);
/*TODO*///	displayosd(bitmap,buf,oc/2,100/2);
/*TODO*///}
/*TODO*///
/*TODO*///#define MAX_OSD_ITEMS 30
/*TODO*///static void (*onscrd_fnc[MAX_OSD_ITEMS])(struct mame_bitmap *bitmap,int increment,int arg);
/*TODO*///static int onscrd_arg[MAX_OSD_ITEMS];
/*TODO*///static int onscrd_total_items;
/*TODO*///
/*TODO*///static void onscrd_init(void)
/*TODO*///{
/*TODO*///	int item,ch;
/*TODO*///
/*TODO*///
/*TODO*///	item = 0;
/*TODO*///
/*TODO*///	if (Machine->sample_rate)
/*TODO*///	{
/*TODO*///		onscrd_fnc[item] = onscrd_volume;
/*TODO*///		onscrd_arg[item] = 0;
/*TODO*///		item++;
/*TODO*///
/*TODO*///		for (ch = 0;ch < MIXER_MAX_CHANNELS;ch++)
/*TODO*///		{
/*TODO*///			if (mixer_get_name(ch) != 0)
/*TODO*///			{
/*TODO*///				onscrd_fnc[item] = onscrd_mixervol;
/*TODO*///				onscrd_arg[item] = ch;
/*TODO*///				item++;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (options.cheat)
/*TODO*///	{
/*TODO*///		for (ch = 0;ch < cpu_gettotalcpu();ch++)
/*TODO*///		{
/*TODO*///			onscrd_fnc[item] = onscrd_overclock;
/*TODO*///			onscrd_arg[item] = ch;
/*TODO*///			item++;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	onscrd_fnc[item] = onscrd_brightness;
/*TODO*///	onscrd_arg[item] = 0;
/*TODO*///	item++;
/*TODO*///
/*TODO*///	onscrd_fnc[item] = onscrd_gamma;
/*TODO*///	onscrd_arg[item] = 0;
/*TODO*///	item++;
/*TODO*///
/*TODO*///	if (Machine->drv->video_attributes & VIDEO_TYPE_VECTOR)
/*TODO*///	{
/*TODO*///		onscrd_fnc[item] = onscrd_vector_flicker;
/*TODO*///		onscrd_arg[item] = 0;
/*TODO*///		item++;
/*TODO*///
/*TODO*///		onscrd_fnc[item] = onscrd_vector_intensity;
/*TODO*///		onscrd_arg[item] = 0;
/*TODO*///		item++;
/*TODO*///	}
/*TODO*///
/*TODO*///	onscrd_total_items = item;
/*TODO*///}
/*TODO*///
/*TODO*///static int on_screen_display(struct mame_bitmap *bitmap, int selected)
/*TODO*///{
/*TODO*///	int increment,sel;
/*TODO*///	static int lastselected = 0;
/*TODO*///
/*TODO*///
/*TODO*///	if (selected == -1)
/*TODO*///		sel = lastselected;
/*TODO*///	else sel = selected - 1;
/*TODO*///
/*TODO*///	increment = 0;
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_LEFT,8))
/*TODO*///		increment = -1;
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_RIGHT,8))
/*TODO*///		increment = 1;
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///		sel = (sel + 1) % onscrd_total_items;
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///		sel = (sel + onscrd_total_items - 1) % onscrd_total_items;
/*TODO*///
/*TODO*///	(*onscrd_fnc[sel])(bitmap,increment,onscrd_arg[sel]);
/*TODO*///
/*TODO*///	lastselected = sel;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_ON_SCREEN_DISPLAY))
/*TODO*///	{
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///		schedule_full_refresh();
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*////*********************************************************************
/*TODO*///
/*TODO*///  end of On Screen Display handling
/*TODO*///
/*TODO*///*********************************************************************/
/*TODO*///
/*TODO*///
/*TODO*///static void displaymessage(struct mame_bitmap *bitmap,const char *text)
/*TODO*///{
/*TODO*///	struct DisplayText dt[2];
/*TODO*///	int avail;
/*TODO*///
/*TODO*///
/*TODO*///	if (Machine->uiwidth < Machine->uifontwidth * strlen(text))
/*TODO*///	{
/*TODO*///		ui_displaymessagewindow(bitmap,text);
/*TODO*///		return;
/*TODO*///	}
/*TODO*///
/*TODO*///	avail = strlen(text)+2;
/*TODO*///
/*TODO*///	ui_drawbox(bitmap,(Machine->uiwidth - Machine->uifontwidth * avail) / 2,
/*TODO*///			Machine->uiheight - 3*Machine->uifontheight,
/*TODO*///			avail * Machine->uifontwidth,
/*TODO*///			2*Machine->uifontheight);
/*TODO*///
/*TODO*///	dt[0].text = text;
/*TODO*///	dt[0].color = UI_COLOR_NORMAL;
/*TODO*///	dt[0].x = (Machine->uiwidth - Machine->uifontwidth * strlen(text)) / 2;
/*TODO*///	dt[0].y = Machine->uiheight - 5*Machine->uifontheight/2;
/*TODO*///	dt[1].text = 0; /* terminate array */
/*TODO*///	displaytext(bitmap,dt);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///static char messagetext[80];
/*TODO*///static int messagecounter;
/*TODO*///
/*TODO*///void CLIB_DECL usrintf_showmessage(const char *text,...)
/*TODO*///{
/*TODO*///	va_list arg;
/*TODO*///	va_start(arg,text);
/*TODO*///	vsprintf(messagetext,text,arg);
/*TODO*///	va_end(arg);
/*TODO*///	messagecounter = 2 * Machine->drv->frames_per_second;
/*TODO*///}
/*TODO*///
/*TODO*///void CLIB_DECL usrintf_showmessage_secs(int seconds, const char *text,...)
/*TODO*///{
/*TODO*///	va_list arg;
/*TODO*///	va_start(arg,text);
/*TODO*///	vsprintf(messagetext,text,arg);
/*TODO*///	va_end(arg);
/*TODO*///	messagecounter = seconds * Machine->drv->frames_per_second;
/*TODO*///}
/*TODO*///
/*TODO*///int handle_user_interface(struct mame_bitmap *bitmap)
/*TODO*///{
/*TODO*///	static int show_profiler;
/*TODO*///	int request_loadsave = LOADSAVE_NONE;
/*TODO*///
/*TODO*///
/*TODO*///	/* if the user pressed F12, save the screen to a file */
/*TODO*///	if (input_ui_pressed(IPT_UI_SNAPSHOT))
/*TODO*///		osd_save_snapshot(bitmap);
/*TODO*///
/*TODO*///	/* This call is for the cheat, it must be called once a frame */
/*TODO*///	if (options.cheat) DoCheat(bitmap);
/*TODO*///
/*TODO*///	/* if the user pressed ESC, stop the emulation */
/*TODO*///	/* but don't quit if the setup menu is on screen */
/*TODO*///	if (setup_selected == 0 && input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		return 1;
/*TODO*///
/*TODO*///	if (setup_selected == 0 && input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///	{
/*TODO*///		setup_selected = -1;
/*TODO*///		if (osd_selected != 0)
/*TODO*///		{
/*TODO*///			osd_selected = 0;	/* disable on screen display */
/*TODO*///			schedule_full_refresh();
/*TODO*///		}
/*TODO*///	}
/*TODO*///	if (setup_selected != 0) setup_selected = setup_menu(bitmap, setup_selected);
/*TODO*///
/*TODO*///	if (!mame_debug && osd_selected == 0 && input_ui_pressed(IPT_UI_ON_SCREEN_DISPLAY))
/*TODO*///	{
/*TODO*///		osd_selected = -1;
/*TODO*///		if (setup_selected != 0)
/*TODO*///		{
/*TODO*///			setup_selected = 0; /* disable setup menu */
/*TODO*///			schedule_full_refresh();
/*TODO*///		}
/*TODO*///	}
/*TODO*///	if (osd_selected != 0) osd_selected = on_screen_display(bitmap, osd_selected);
/*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///	if (keyboard_pressed_memory(KEYCODE_BACKSPACE))
/*TODO*///	{
/*TODO*///		if (jukebox_selected != -1)
/*TODO*///		{
/*TODO*///			jukebox_selected = -1;
/*TODO*///			cpu_halt(0,1);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			jukebox_selected = 0;
/*TODO*///			cpu_halt(0,0);
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (jukebox_selected != -1)
/*TODO*///	{
/*TODO*///		char buf[40];
/*TODO*///		watchdog_reset_w(0,0);
/*TODO*///		if (keyboard_pressed_memory(KEYCODE_LCONTROL))
/*TODO*///		{
/*TODO*///#include "cpu/z80/z80.h"
/*TODO*///			soundlatch_w(0,jukebox_selected);
/*TODO*///			cpu_cause_interrupt(1,Z80_NMI_INT);
/*TODO*///		}
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_RIGHT,8))
/*TODO*///		{
/*TODO*///			jukebox_selected = (jukebox_selected + 1) & 0xff;
/*TODO*///		}
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_LEFT,8))
/*TODO*///		{
/*TODO*///			jukebox_selected = (jukebox_selected - 1) & 0xff;
/*TODO*///		}
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///		{
/*TODO*///			jukebox_selected = (jukebox_selected + 16) & 0xff;
/*TODO*///		}
/*TODO*///		if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///		{
/*TODO*///			jukebox_selected = (jukebox_selected - 16) & 0xff;
/*TODO*///		}
/*TODO*///		sprintf(buf,"sound cmd %02x",jukebox_selected);
/*TODO*///		displaymessage(buf);
/*TODO*///	}
/*TODO*///#endif
/*TODO*///
/*TODO*///
/*TODO*///	/* if the user pressed F3, reset the emulation */
/*TODO*///	if (input_ui_pressed(IPT_UI_RESET_MACHINE))
/*TODO*///		machine_reset();
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SAVE_STATE))
/*TODO*///		request_loadsave = LOADSAVE_SAVE;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_LOAD_STATE))
/*TODO*///		request_loadsave = LOADSAVE_LOAD;
/*TODO*///
/*TODO*///	if (request_loadsave != LOADSAVE_NONE)
/*TODO*///	{
/*TODO*///		int file = 0;
/*TODO*///
/*TODO*///		osd_sound_enable(0);
/*TODO*///		osd_pause(1);
/*TODO*///
/*TODO*///		do
/*TODO*///		{
/*TODO*///			InputCode code;
/*TODO*///
/*TODO*///			if (request_loadsave == LOADSAVE_SAVE)
/*TODO*///				displaymessage(bitmap, "Select position to save to");
/*TODO*///			else
/*TODO*///				displaymessage(bitmap, "Select position to load from");
/*TODO*///
/*TODO*///			update_video_and_audio();
/*TODO*///
/*TODO*///			if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///				break;
/*TODO*///
/*TODO*///			code = code_read_async();
/*TODO*///			if (code != CODE_NONE)
/*TODO*///			{
/*TODO*///				if (code >= KEYCODE_A && code <= KEYCODE_Z)
/*TODO*///					file = 'a' + (code - KEYCODE_A);
/*TODO*///				else if (code >= KEYCODE_0 && code <= KEYCODE_9)
/*TODO*///					file = '0' + (code - KEYCODE_0);
/*TODO*///				else if (code >= KEYCODE_0_PAD && code <= KEYCODE_9_PAD)
/*TODO*///					file = '0' + (code - KEYCODE_0);
/*TODO*///			}
/*TODO*///		}
/*TODO*///		while (!file);
/*TODO*///
/*TODO*///		osd_pause(0);
/*TODO*///		osd_sound_enable(1);
/*TODO*///
/*TODO*///		if (file > 0)
/*TODO*///		{
/*TODO*///			if (request_loadsave == LOADSAVE_SAVE)
/*TODO*///				usrintf_showmessage("Save to position %c", file);
/*TODO*///			else
/*TODO*///				usrintf_showmessage("Load from position %c", file);
/*TODO*///			cpu_loadsave_schedule(request_loadsave, file);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			if (request_loadsave == LOADSAVE_SAVE)
/*TODO*///				usrintf_showmessage("Save cancelled");
/*TODO*///			else
/*TODO*///				usrintf_showmessage("Load cancelled");
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (single_step || input_ui_pressed(IPT_UI_PAUSE)) /* pause the game */
/*TODO*///	{
/*TODO*////*		osd_selected = 0;	   disable on screen display, since we are going   */
/*TODO*///							/* to change parameters affected by it */
/*TODO*///
/*TODO*///		if (single_step == 0)
/*TODO*///		{
/*TODO*///			osd_sound_enable(0);
/*TODO*///			osd_pause(1);
/*TODO*///		}
/*TODO*///
/*TODO*///		while (!input_ui_pressed(IPT_UI_PAUSE))
/*TODO*///		{
/*TODO*///#ifdef MAME_NET
/*TODO*///			osd_net_sync();
/*TODO*///#endif /* MAME_NET */
/*TODO*///			profiler_mark(PROFILER_VIDEO);
/*TODO*///			if (osd_skip_this_frame() == 0)
/*TODO*///			{
/*TODO*///				/* keep calling vh_screenrefresh() while paused so we can stuff */
/*TODO*///				/* debug code in there */
/*TODO*///				draw_screen();
/*TODO*///			}
/*TODO*///			profiler_mark(PROFILER_END);
/*TODO*///
/*TODO*///			if (input_ui_pressed(IPT_UI_SNAPSHOT))
/*TODO*///				osd_save_snapshot(bitmap);
/*TODO*///
/*TODO*///			if (setup_selected == 0 && input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///				return 1;
/*TODO*///
/*TODO*///			if (setup_selected == 0 && input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///			{
/*TODO*///				setup_selected = -1;
/*TODO*///				if (osd_selected != 0)
/*TODO*///				{
/*TODO*///					osd_selected = 0;	/* disable on screen display */
/*TODO*///					schedule_full_refresh();
/*TODO*///				}
/*TODO*///			}
/*TODO*///			if (setup_selected != 0) setup_selected = setup_menu(bitmap, setup_selected);
/*TODO*///
/*TODO*///			if (!mame_debug && osd_selected == 0 && input_ui_pressed(IPT_UI_ON_SCREEN_DISPLAY))
/*TODO*///			{
/*TODO*///				osd_selected = -1;
/*TODO*///				if (setup_selected != 0)
/*TODO*///				{
/*TODO*///					setup_selected = 0; /* disable setup menu */
/*TODO*///					schedule_full_refresh();
/*TODO*///				}
/*TODO*///			}
/*TODO*///			if (osd_selected != 0) osd_selected = on_screen_display(bitmap, osd_selected);
/*TODO*///
/*TODO*///			if (options.cheat) DisplayWatches(bitmap);
/*TODO*///
/*TODO*///			/* show popup message if any */
/*TODO*///			if (messagecounter > 0) displaymessage(bitmap, messagetext);
/*TODO*///
/*TODO*///			update_video_and_audio();
/*TODO*///		}
/*TODO*///
/*TODO*///		if (code_pressed(KEYCODE_LSHIFT) || code_pressed(KEYCODE_RSHIFT))
/*TODO*///			single_step = 1;
/*TODO*///		else
/*TODO*///		{
/*TODO*///			single_step = 0;
/*TODO*///			osd_pause(0);
/*TODO*///			osd_sound_enable(1);
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	/* show popup message if any */
/*TODO*///	if (messagecounter > 0)
/*TODO*///	{
/*TODO*///		displaymessage(bitmap, messagetext);
/*TODO*///
/*TODO*///		if (--messagecounter == 0)
/*TODO*///			schedule_full_refresh();
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SHOW_PROFILER))
/*TODO*///	{
/*TODO*///		show_profiler ^= 1;
/*TODO*///		if (show_profiler)
/*TODO*///			profiler_start();
/*TODO*///		else
/*TODO*///		{
/*TODO*///			profiler_stop();
/*TODO*///			schedule_full_refresh();
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (show_profiler) profiler_show(bitmap);
/*TODO*///
/*TODO*///
/*TODO*///	/* if the user pressed F4, show the character set */
/*TODO*///	if (input_ui_pressed(IPT_UI_SHOW_GFX))
/*TODO*///	{
/*TODO*///		osd_sound_enable(0);
/*TODO*///
/*TODO*///		showcharset(bitmap);
/*TODO*///
/*TODO*///		osd_sound_enable(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
    public static void init_user_interface() {
        snapno = 0;/* reset snapshot counter */

        setup_menu_init();
        setup_selected = 0;

        onscrd_init();
        osd_selected = 0;

        jukebox_selected = -1;

        single_step = 0;

        orientation_count = 0;
    }

    public static int onscrd_active() {
        return osd_selected;
    }

    public static int setup_active() {
        return setup_selected;
    }   
}