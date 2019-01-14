/*
This file is part of Arcadeflex.

Arcadeflex is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Arcadeflex is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Arcadeflex.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * ported to 0.37b7
 */
package old2.mame;

import arcadeflex.libc.ptr.UBytePtr;
import static old.arcadeflex.libc_old.FILE;
import static mame056.commonH.GameSamples;
import static mame.drawgfxH.GfxElement;
import static mame.drawgfxH.rectangle;
import static mame.driverH.GameDriver;
import static mame.driverH.MachineDriver;
import static old.mame.inptportH.InputPort;
import static mame.osdependH.osd_bitmap;
import static common.subArrays.*;

public class mameH {

    public static final int MAX_GFX_ELEMENTS = 32;
    public static final int MAX_MEMORY_REGIONS = 32;

    public static class RegionInfo {

        public UBytePtr base;
        public int length;
        public int/*UINT32*/ type;
        public int/*UINT32*/ flags;
    }
    public static class RunningMachine {

        public RegionInfo[] memory_region = new RegionInfo[MAX_MEMORY_REGIONS];

        public GfxElement gfx[] = new GfxElement[MAX_GFX_ELEMENTS];
        public osd_bitmap scrbitmap;
        public rectangle visible_area;
        public /*UINT16 * */ char[] pens;
        public /*UINT16 * */ char[] game_colortable;
        public UShortArray remapped_colortable;
        public GameDriver gamedrv;
        public MachineDriver drv;
        public int color_depth;
        public int sample_rate;

        public int obsolete;
        public GameSamples samples;
        public InputPort[] input_ports;
        public InputPort[] input_ports_default;
        public int orientation;
        public GfxElement uifont;
        public int uifontwidth, uifontheight;
        public int uixmin, uiymin;
        public int uiwidth, uiheight;
        public int ui_orientation;
        public rectangle absolute_visible_area;
    }

    public static class GameOptions {

        public FILE record;
        public FILE playback;
        public FILE language_file;
        public int mame_debug;
        public int cheat;
        public int gui_host;
        public int samplerate;
        public int use_samples;
        public int use_emulated_ym3812;
        public int color_depth;
        public int vector_width;
        public int vector_height;
        public int norotate;
        public int ror;
        public int rol;
        public int flipx;
        public int flipy;
        public int beam;
        public int flicker;
        public int translucency;
        public int antialias;
        public int use_artwork;

    }

}
