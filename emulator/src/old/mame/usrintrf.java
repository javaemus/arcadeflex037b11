/**
 * ported to 0.37b5
 */
package old.mame;
import static arcadeflex.libc.cstdio.*;
import static old.arcadeflex.libc_old.strlen;
import static common.subArrays.*;
import static arcadeflex.libc.ptr.*;
import static mame.cheat.DoCheat;
import static mame.cheat.cheat_menu;
import static old.arcadeflex.sound.*;
import static arcadeflex.video.*;
import static old2.mame.mame.update_video_and_audio;
import static mame056.common.*;
import static mame056.commonH.COIN_COUNTERS;
import static old.mame.drawgfx.*;
import static old.mame.drawgfx.drawgfx;
import static mame.drawgfxH.*;
import static mame.driver.drivers;
import static mame.driverH.*;
import static old.mame.inptport.*;
import static old.mame.inptportH.*;
import mame056.commonH.mame_bitmap;
import static mame056.input.*;
import static mame056.inputH.*;
import static old2.mame.mame.*;
import static old2.mame.mameH.MAX_GFX_ELEMENTS;
import static mame.sndintrf.*;
import static mame056.ui_text.ui_getstring;
import static mame056.ui_textH.*;
import static mame056.usrintrfH.*;
import static mame056.version.build_version;
import static mame056.cpuexec.machine_reset;
import static mame056.cpuexecH.CPU_AUDIO_CPU;
import static mame056.cpuintrf.cputype_name;
import static mame056.usrintrf.mame_stats;
import static mame056.usrintrf.setcodesettings;
import static mame056.usrintrf.setdefcodesettings;
import static mame056.usrintrf.setdipswitches;
import static mame056.usrintrf.showcharset;
import static mame056.usrintrf.switch_true_orientation;
import static mame056.usrintrf.switch_ui_orientation;


public class usrintrf {
    public static int setup_selected;
    public static int osd_selected;
    public static int single_step;


    /***************************************************************************
     * Display text on the screen. If erase is 0, it superimposes the text on
     * the last frame displayed.
     ***************************************************************************/

    public static void displaytext(mame_bitmap bitmap, DisplayText[] dt, int erase, int update_screen) {
        if (erase != 0)
            osd_clearbitmap(bitmap);


        switch_ui_orientation();

        osd_mark_dirty(0, 0, Machine.uiwidth - 1, Machine.uiheight - 1, 1);	/* ASG 971011 */
        int _ptr = 0;
        while (dt[_ptr].text != null) {
            int x, y;
            int c;

            x = dt[_ptr].x;
            y = dt[_ptr].y;
            c = 0;//dt.text;
            while (c < dt[_ptr].text.length() && dt[_ptr].text.charAt(c) != '\0')//while (*c)
            {
                boolean wrapped = false;

                if (dt[_ptr].text.charAt(c) == '\n') {
                    x = dt[_ptr].x;
                    y += Machine.uifontheight + 1;
                    wrapped = true;
                } else if (dt[_ptr].text.charAt(c) == ' ') {
                            /* don't try to word wrap at the beginning of a line (this would cause */
                            /* an endless loop if a word is longer than a line) */
                    if (x != dt[_ptr].x) {
                        int nextlen = 0;
                        int nc;//const char *nc;

                        nc = c + 1;
                        while (nc < dt[_ptr].text.length() && dt[_ptr].text.charAt(nc) != '\0' && dt[_ptr].text.charAt(nc) != ' ' && dt[_ptr].text.charAt(nc) != '\n')//while (*nc && *nc != ' ' && *nc != '\n')
                        {
                            nextlen += Machine.uifontwidth;
                            nc++;
                        }

                                /* word wrap */
                        if (x + Machine.uifontwidth + nextlen > Machine.uiwidth) {
                            x = dt[_ptr].x;
                            y += Machine.uifontheight + 1;
                            wrapped = true;
                        }
                    }
                }

                if (!wrapped) {
                    drawgfx(bitmap, Machine.uifont, dt[_ptr].text.charAt(c), dt[_ptr].color, 0, 0, x + Machine.uixmin, y + Machine.uiymin, null, TRANSPARENCY_NONE, 0);
                    x += Machine.uifontwidth;
                }

                c++;
            }
            _ptr++;
        }
        switch_true_orientation();

        if (update_screen != 0) update_video_and_audio();
    }

    /* Writes messages on the screen. */
    public static void ui_text_ex(mame_bitmap bitmap, String buf_begin, int buf_end, int x, int y, int color) {
        switch_ui_orientation();

        for (int i = 0; i < buf_end; ++i) {
            drawgfx(bitmap, Machine.uifont, buf_begin.charAt(i), color, 0, 0,
                    x + Machine.uixmin,
                    y + Machine.uiymin, null, TRANSPARENCY_NONE, 0);
            x += Machine.uifontwidth;
        }

        switch_true_orientation();
    }

    /* Writes messages on the screen. */
    public static void ui_text(mame_bitmap bitmap, String buf, int x, int y) {
        ui_text_ex(bitmap, buf, buf.length(), x, y, UI_COLOR_NORMAL);
    }

    public static void ui_drawbox(mame_bitmap bitmap, int leftx, int topy, int width, int height) {
        int black, white;


        switch_ui_orientation();

        if (leftx < 0) leftx = 0;
        if (topy < 0) topy = 0;
        if (width > Machine.uiwidth) width = Machine.uiwidth;
        if (height > Machine.uiheight) height = Machine.uiheight;

        leftx += Machine.uixmin;
        topy += Machine.uiymin;

        black = Machine.uifont.colortable.read(0);
        white = Machine.uifont.colortable.read(1);

        plot_box.handler(bitmap, leftx, topy, width, 1, white);
        plot_box.handler(bitmap, leftx, topy + height - 1, width, 1, white);
        plot_box.handler(bitmap, leftx, topy, 1, height, white);
        plot_box.handler(bitmap, leftx + width - 1, topy, 1, height, white);
        plot_box.handler(bitmap, leftx + 1, topy + 1, width - 2, height - 2, black);

        switch_true_orientation();
    }

    public static void drawbar(mame_bitmap bitmap, int leftx, int topy, int width, int height, int percentage, int default_percentage) {
        int black, white;


        switch_ui_orientation();

        if (leftx < 0) leftx = 0;
        if (topy < 0) topy = 0;
        if (width > Machine.uiwidth) width = Machine.uiwidth;
        if (height > Machine.uiheight) height = Machine.uiheight;

        leftx += Machine.uixmin;
        topy += Machine.uiymin;

        black = Machine.uifont.colortable.read(0);
        white = Machine.uifont.colortable.read(1);

        plot_box.handler(bitmap, leftx + (width - 1) * default_percentage / 100, topy, 1, height / 8, white);

        plot_box.handler(bitmap, leftx, topy + height / 8, width, 1, white);

        plot_box.handler(bitmap, leftx, topy + height / 8, 1 + (width - 1) * percentage / 100, height - 2 * (height / 8), white);

        plot_box.handler(bitmap, leftx, topy + height - height / 8 - 1, width, 1, white);

        plot_box.handler(bitmap, leftx + (width - 1) * default_percentage / 100, topy + height - height / 8, 1, height / 8, white);

        switch_true_orientation();
    }

    public static void ui_displaymenu(mame_bitmap bitmap, String[] items, String[] subitems, char[] flag, int selected, int arrowize_subitem) {
        DisplayText[] dt = DisplayText.create(256);
        int curr_dt;
        String lefthilight = ui_getstring(UI_lefthilight);
        String righthilight = ui_getstring(UI_righthilight);
        String uparrow = ui_getstring(UI_uparrow);
        String downarrow = ui_getstring(UI_downarrow);
        String leftarrow = ui_getstring(UI_leftarrow);
        String rightarrow = ui_getstring(UI_rightarrow);
        int i, count, len, maxlen, highlen;
        int leftoffs, topoffs, visible, topitem;
        int selected_long;


        i = 0;
        maxlen = 0;
        highlen = Machine.uiwidth / Machine.uifontwidth;
        while (items[i] != null) {
            len = 3 + strlen(items[i]);
            if (subitems != null && subitems[i] != null)
                len += 2 + strlen(subitems[i]);
            if (len > maxlen && len <= highlen)
                maxlen = len;
            i++;
        }
        count = i;

        visible = Machine.uiheight / (3 * Machine.uifontheight / 2) - 1;
        topitem = 0;
        if (visible > count) visible = count;
        else {
            topitem = selected - visible / 2;
            if (topitem < 0) topitem = 0;
            if (topitem > count - visible) topitem = count - visible;
        }

        leftoffs = (Machine.uiwidth - maxlen * Machine.uifontwidth) / 2;
        topoffs = (Machine.uiheight - (3 * visible + 1) * Machine.uifontheight / 2) / 2;

	/* black background */
        ui_drawbox(bitmap, leftoffs, topoffs, maxlen * Machine.uifontwidth, (3 * visible + 1) * Machine.uifontheight / 2);

        selected_long = 0;
        curr_dt = 0;
        for (i = 0; i < visible; i++) {
            int item = i + topitem;

            if (i == 0 && item > 0) {
                dt[curr_dt].text = uparrow;
                dt[curr_dt].color = UI_COLOR_NORMAL;
                dt[curr_dt].x = (Machine.uiwidth - Machine.uifontwidth * strlen(uparrow)) / 2;
                dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                curr_dt++;
            } else if (i == visible - 1 && item < count - 1) {
                dt[curr_dt].text = downarrow;
                dt[curr_dt].color = UI_COLOR_NORMAL;
                dt[curr_dt].x = (Machine.uiwidth - Machine.uifontwidth * strlen(downarrow)) / 2;
                dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                curr_dt++;
            } else {
                if (subitems != null && subitems[item] != null) {
                    int sublen;
                    len = strlen(items[item]);
                    dt[curr_dt].text = items[item];
                    dt[curr_dt].color = UI_COLOR_NORMAL;
                    dt[curr_dt].x = leftoffs + 3 * Machine.uifontwidth / 2;
                    dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                    curr_dt++;
                    sublen = strlen(subitems[item]);
                    if (sublen > maxlen - 5 - len) {
                        dt[curr_dt].text = "...";
                        sublen = strlen(dt[curr_dt].text);
                        if (item == selected)
                            selected_long = 1;
                    } else {
                        dt[curr_dt].text = subitems[item];
                    }
                /* If this item is flagged, draw it in inverse print */
                    dt[curr_dt].color = (flag != null && flag[item] != 0) ? UI_COLOR_INVERSE : UI_COLOR_NORMAL;
                    dt[curr_dt].x = leftoffs + Machine.uifontwidth * (maxlen - 1 - sublen) - Machine.uifontwidth / 2;
                    dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                    curr_dt++;
                } else {
                    dt[curr_dt].text = items[item];
                    dt[curr_dt].color = UI_COLOR_NORMAL;
                    dt[curr_dt].x = (Machine.uiwidth - Machine.uifontwidth * strlen(items[item])) / 2;
                    dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                    curr_dt++;
                }
            }
        }

        i = selected - topitem;
        if (subitems != null && subitems[selected] != null && arrowize_subitem != 0) {
            if ((arrowize_subitem & 1) != 0) {
                dt[curr_dt].text = leftarrow;
                dt[curr_dt].color = UI_COLOR_NORMAL;
                dt[curr_dt].x = leftoffs + Machine.uifontwidth * (maxlen - 2 - strlen(subitems[selected])) - Machine.uifontwidth / 2 - 1;
                dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                curr_dt++;
            }
            if ((arrowize_subitem & 2) != 0) {
                dt[curr_dt].text = rightarrow;
                dt[curr_dt].color = UI_COLOR_NORMAL;
                dt[curr_dt].x = leftoffs + Machine.uifontwidth * (maxlen - 1) - Machine.uifontwidth / 2;
                dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
                curr_dt++;
            }
        } else {
            dt[curr_dt].text = righthilight;
            dt[curr_dt].color = UI_COLOR_NORMAL;
            dt[curr_dt].x = leftoffs + Machine.uifontwidth * (maxlen - 1) - Machine.uifontwidth / 2;
            dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
            curr_dt++;
        }
        dt[curr_dt].text = lefthilight;
        dt[curr_dt].color = UI_COLOR_NORMAL;
        dt[curr_dt].x = leftoffs + Machine.uifontwidth / 2;
        dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
        curr_dt++;

        dt[curr_dt].text = null;	/* terminate array */

        displaytext(bitmap, dt, 0, 0);

        if (selected_long != 0) {
            throw new UnsupportedOperationException("unimplemented");
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
        }
    }

    public static void ui_displaymessagewindow(mame_bitmap bitmap, String text) {
        DisplayText[] dt = DisplayText.create(256);
        int curr_dt;
        int c, c2;
        char[] textcopy = new char[2048];
        int i, len, maxlen, lines;
        int leftoffs, topoffs;
        int maxcols, maxrows;

        maxcols = (Machine.uiwidth / Machine.uifontwidth) - 1;
        maxrows = (2 * Machine.uiheight - Machine.uifontheight) / (3 * Machine.uifontheight);

    	/* copy text, calculate max len, count lines, wrap long lines and crop height to fit */
        maxlen = 0;
        lines = 0;
        c = 0;//(char *)text;
        c2 = 0;//textcopy;
        while (c < text.length() && text.charAt(c) != '\0')//while (*c)
        {
            len = 0;
            while (c < text.length() && text.charAt(c) != '\0' && text.charAt(c) != '\n')//while (*c && *c != '\n')
            {
                textcopy[c2++] = text.charAt(c++);
                len++;
                if (len == maxcols && text.charAt(c) != '\n') {
                /* attempt word wrap */
                    int csave = c, c2save = c2;
                    int lensave = len;

                        /* back up to last space or beginning of line */
                    while (text.charAt(c) != ' ' && text.charAt(c) != '\n' && c > 0)//while (*c != ' ' && *c != '\n' && c > text)
                    {
                        --c;
                        --c2;
                        --len;
                    }
                    /* if no space was found, hard wrap instead */
                    if (text.charAt(c) != ' ') {
                        c = csave;
                        c2 = c2save;
                        len = lensave;
                    } else
                        c++;

                    textcopy[c2++] = '\n'; /* insert wrap */
                    break;
                }
            }
            if (c < text.length() && text.charAt(c) == '\n')//if (*c == '\n')
                textcopy[c2++] = text.charAt(c++);

            if (len > maxlen) maxlen = len;

            lines++;
            if (lines == maxrows)
                break;
        }
        textcopy[c2] = '\0';

        maxlen += 1;
        leftoffs = (Machine.uiwidth - Machine.uifontwidth * maxlen) / 2;
        if (leftoffs < 0) leftoffs = 0;
        topoffs = (Machine.uiheight - (3 * lines + 1) * Machine.uifontheight / 2) / 2;

        /* black background */
        ui_drawbox(bitmap, leftoffs, topoffs, maxlen * Machine.uifontwidth, (3 * lines + 1) * Machine.uifontheight / 2);

        curr_dt = 0;
        c = 0;//textcopy;
        i = 0;
        while (c < textcopy.length && textcopy[c] != '\0')//while (*c)
        {
            c2 = c;
            while (c < textcopy.length && textcopy[c] != '\0' && textcopy[c] != '\n')//while (*c && *c != '\n')
                c++;

            if (textcopy[c] == '\n') {
                textcopy[c] = '\0';
                c++;
            }
            if (textcopy[c2] == '\t')    /* center text */ {
                c2++;
                dt[curr_dt].x = (Machine.uiwidth - Machine.uifontwidth * (c - c2)) / 2;
            } else
                dt[curr_dt].x = leftoffs + Machine.uifontwidth / 2;

            dt[curr_dt].text = new String(textcopy).substring(c2);//dt[curr_dt].text = c2;
            dt[curr_dt].color = UI_COLOR_NORMAL;
            dt[curr_dt].y = topoffs + (3 * i + 1) * Machine.uifontheight / 2;
            curr_dt++;

            i++;
        }
        dt[curr_dt].text = null;	/* terminate array */

        displaytext(bitmap, dt, 0, 0);
    }

 
    static void showtotalcolors(mame_bitmap bitmap) {
        char[] used;
        int i, l, x, y, total;
        char[] r = new char[1];
        char[] g = new char[1];
        char[] b = new char[1];
        String buf = "";


        used = new char[64 * 64 * 64];
        if (used == null) return;

        for (i = 0; i < 64 * 64 * 64; i++)
            used[i] = 0;

        for (y = 0; y < bitmap.height; y++) {
            for (x = 0; x < bitmap.width; x++) {
                osd_get_pen(read_pixel.handler(bitmap, x, y), r, g, b);
                r[0] >>= 2;
                g[0] >>= 2;
                b[0] >>= 2;
                used[64 * 64 * r[0] + 64 * g[0] + b[0]] = 1;
            }
        }

        total = 0;
        for (i = 0; i < 64 * 64 * 64; i++)
            if (used[i] != 0) total++;

        switch_ui_orientation();

        buf = sprintf("%5d colors", total);
        l = strlen(buf);
        for (i = 0; i < l; i++)
            drawgfx(bitmap, Machine.uifont, buf.charAt(i), total > 256 ? UI_COLOR_INVERSE : UI_COLOR_NORMAL, 0, 0, Machine.uixmin + i * Machine.uifontwidth, Machine.uiymin, null, TRANSPARENCY_NONE, 0);

        switch_true_orientation();

        used = null;
    }

 




    public static int showcopyright(mame_bitmap bitmap) {
        int done;
        String buf = "";
        String buf2 = "";

        buf = ui_getstring(UI_copyright1);
        buf += "\n\n";
        buf2 = sprintf(ui_getstring(UI_copyright2), Machine.gamedrv.description);
        buf += buf2;
        buf += "\n\n";
        buf += ui_getstring(UI_copyright3);

        ui_displaymessagewindow(bitmap, buf);

        setup_selected = -1;////
        done = 0;
        do {
            update_video_and_audio();
/*TODO*///		osd_poll_joysticks();
            if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
                setup_selected = 0;////
                return 1;
            }
            if (keyboard_pressed_memory(KEYCODE_O) != 0 ||
                    input_ui_pressed(IPT_UI_LEFT) != 0)
                done = 1;
            if (done == 1 && (keyboard_pressed_memory(KEYCODE_K) != 0 ||
                    input_ui_pressed(IPT_UI_RIGHT) != 0))
                done = 2;
        } while (done < 2);

        setup_selected = 0;////
        osd_clearbitmap(bitmap);
        update_video_and_audio();

        return 0;
    }

    public static int displaygameinfo(mame_bitmap bitmap, int selected) {
        int i;
        String buf = "";
        String buf2 = "";
        int sel;


        sel = selected - 1;


        buf = sprintf("%s\n%s %s\n\n%s:\n", Machine.gamedrv.description, Machine.gamedrv.year, Machine.gamedrv.manufacturer,
                ui_getstring(UI_cpu));
        i = 0;
        while (i < MAX_CPU && Machine.drv.cpu[i].cpu_type != 0) {

            if (Machine.drv.cpu[i].cpu_clock >= 1000000)
                buf += sprintf("%s %d.%06d MHz",
                        cputype_name(Machine.drv.cpu[i].cpu_type),
                        Machine.drv.cpu[i].cpu_clock / 1000000,
                        Machine.drv.cpu[i].cpu_clock % 1000000);
            else
                buf += sprintf("%s %d.%03d kHz",
                        cputype_name(Machine.drv.cpu[i].cpu_type),
                        Machine.drv.cpu[i].cpu_clock / 1000,
                        Machine.drv.cpu[i].cpu_clock % 1000);

            if ((Machine.drv.cpu[i].cpu_type & CPU_AUDIO_CPU) != 0) {
                buf2 = sprintf(" (%s)", ui_getstring(UI_sound_lc));
                buf += buf2;
            }

            buf += "\n";

            i++;
        }

        buf2 = sprintf("\n%s", ui_getstring(UI_sound));
        buf += buf2;
        if ((Machine.drv.sound_attributes & SOUND_SUPPORTS_STEREO) != 0)
            buf += sprintf(" (%s)", ui_getstring(UI_stereo));
        buf += ":\n";

        i = 0;
        while (i < MAX_SOUND && Machine.drv.sound[i].sound_type != 0) {
            if (sound_num(Machine.drv.sound[i]) != 0)
                buf += sprintf("%dx", sound_num(Machine.drv.sound[i]));

            buf += sprintf("%s", sound_name(Machine.drv.sound[i]));

            if (sound_clock(Machine.drv.sound[i]) != 0) {
                if (sound_clock(Machine.drv.sound[i]) >= 1000000)
                    buf += sprintf(" %d.%06d MHz",
                            sound_clock(Machine.drv.sound[i]) / 1000000,
                            sound_clock(Machine.drv.sound[i]) % 1000000);
                else
                    buf += sprintf(" %d.%03d kHz",
                            sound_clock(Machine.drv.sound[i]) / 1000,
                            sound_clock(Machine.drv.sound[i]) % 1000);
            }
            buf += "\n";

            i++;
        }

        if ((Machine.drv.video_attributes & VIDEO_TYPE_VECTOR) != 0)
            buf += sprintf("\n%s\n", ui_getstring(UI_vectorgame));
        else {
            int pixelx, pixely, tmax, tmin, rem;

            pixelx = 4 * (Machine.visible_area.max_y - Machine.visible_area.min_y + 1);
            pixely = 3 * (Machine.visible_area.max_x - Machine.visible_area.min_x + 1);

		/* calculate MCD */
            if (pixelx >= pixely) {
                tmax = pixelx;
                tmin = pixely;
            } else {
                tmax = pixely;
                tmin = pixelx;
            }
            while ((rem = tmax % tmin) != 0) {
                tmax = tmin;
                tmin = rem;
            }
        /* tmin is now the MCD */

            pixelx /= tmin;
            pixely /= tmin;

            buf += sprintf("\n%s:\n", ui_getstring(UI_screenres));
            buf += sprintf("%d x %d (%s) %f Hz\n",
                    Machine.visible_area.max_x - Machine.visible_area.min_x + 1,
                    Machine.visible_area.max_y - Machine.visible_area.min_y + 1,
                    (Machine.gamedrv.flags & ORIENTATION_SWAP_XY) != 0 ? "V" : "H",
                    Machine.drv.frames_per_second);
//#if 0
            buf += sprintf("pixel aspect ratio %d:%d\n",
                    pixelx, pixely);
            buf += sprintf("%d colors ", Machine.drv.total_colors);
            if ((Machine.gamedrv.flags & GAME_REQUIRES_16BIT) != 0)
                buf += "(16-bit required)\n";
            else if ((Machine.drv.video_attributes & VIDEO_MODIFIES_PALETTE) != 0)
                buf += "(dynamic)\n";
            else buf += "(static)\n";
//#endif
        }


        if (sel == -1) {
        /* startup info, print MAME version and ask for any key */

            buf2 = sprintf("\n\t%s ", "Arcadeflex"/*ui_getstring (UI_mame)*/);	/* \t means that the line will be centered */
            buf += buf2;

            buf += build_version;
            buf2 = sprintf("\n\t%s", ui_getstring(UI_anykey));
            buf += buf2;
            ui_drawbox(bitmap, 0, 0, Machine.uiwidth, Machine.uiheight);
            ui_displaymessagewindow(bitmap, buf);

            sel = 0;
            if (code_read_async() != CODE_NONE)
                sel = -1;
        } else {
        /* menu system, use the normal menu keys */
            buf += "\n\t";
            buf += ui_getstring(UI_lefthilight);
            buf += " ";
            buf += ui_getstring(UI_returntomain);
            buf += " ";
            buf += ui_getstring(UI_righthilight);

            ui_displaymessagewindow(bitmap, buf);

            if (input_ui_pressed(IPT_UI_SELECT) != 0)
                sel = -1;

            if (input_ui_pressed(IPT_UI_CANCEL) != 0)
                sel = -1;

            if (input_ui_pressed(IPT_UI_CONFIGURE) != 0)
                sel = -2;
        }

        if (sel == -1 || sel == -2) {
			/* tell updatescreen() to clean after us */
            need_to_clear_bitmap = 1;
        }

        return sel + 1;
    }

    public static int showgamewarnings(mame_bitmap bitmap) {
        int i;
        String buf = "";

        if ((Machine.gamedrv.flags &
                (GAME_NOT_WORKING | GAME_UNEMULATED_PROTECTION | GAME_WRONG_COLORS | GAME_IMPERFECT_COLORS |
                        GAME_NO_SOUND | GAME_IMPERFECT_SOUND | GAME_NO_COCKTAIL)) != 0) {
            int done;

            buf = ui_getstring(UI_knownproblems);
            buf += "\n\n";

            if ((Machine.gamedrv.flags & GAME_IMPERFECT_COLORS) != 0) {
                buf += ui_getstring(UI_imperfectcolors);
                buf += "\n";
            }

            if ((Machine.gamedrv.flags & GAME_WRONG_COLORS) != 0) {
                buf += ui_getstring(UI_wrongcolors);
                buf += "\n";
            }

            if ((Machine.gamedrv.flags & GAME_IMPERFECT_SOUND) != 0) {
                buf += ui_getstring(UI_imperfectsound);
                buf += "\n";
            }

            if ((Machine.gamedrv.flags & GAME_NO_SOUND) != 0) {
                buf += ui_getstring(UI_nosound);
                buf += "\n";
            }

            if ((Machine.gamedrv.flags & GAME_NO_COCKTAIL) != 0) {
                buf += ui_getstring(UI_nococktail);
                buf += "\n";
            }

            if ((Machine.gamedrv.flags & (GAME_NOT_WORKING | GAME_UNEMULATED_PROTECTION)) != 0) {
                GameDriver maindrv;
                int foundworking;

                if ((Machine.gamedrv.flags & GAME_NOT_WORKING) != 0) {
                    buf += ui_getstring(UI_brokengame);
                    buf += "\n";
                }
                if ((Machine.gamedrv.flags & GAME_UNEMULATED_PROTECTION) != 0) {
                    buf += ui_getstring(UI_brokenprotection);
                    buf += "\n";
                }
                if (Machine.gamedrv.clone_of != null && (Machine.gamedrv.clone_of.flags & NOT_A_DRIVER) == 0)
                    maindrv = Machine.gamedrv.clone_of;
                else maindrv = Machine.gamedrv;

                foundworking = 0;
                i = 0;
                while (drivers[i] != null) {
                    if (drivers[i] == maindrv || drivers[i].clone_of == maindrv) {
                        if ((drivers[i].flags & (GAME_NOT_WORKING | GAME_UNEMULATED_PROTECTION)) == 0) {
                            if (foundworking == 0) {
                                buf += "\n\n";
                                buf += ui_getstring(UI_workingclones);
                                buf += "\n\n";
                            }
                            foundworking = 1;

                            buf += sprintf("%s\n", drivers[i].name);
                        }
                    }
                    i++;
                }
            }


            buf += "\n\n";
            buf += ui_getstring(UI_typeok);

            ui_displaymessagewindow(bitmap, buf);

            done = 0;
            do {
                update_video_and_audio();
/*TODO*///      osd_poll_joysticks();
                if (input_ui_pressed(IPT_UI_CANCEL) != 0)
                    return 1;
                if (code_pressed_memory(KEYCODE_O) != 0 ||
                        input_ui_pressed(IPT_UI_LEFT) != 0)
                    done = 1;
                if (done == 1 && (code_pressed_memory(KEYCODE_K) != 0 ||
                        input_ui_pressed(IPT_UI_RIGHT) != 0))
                    done = 2;
            } while (done < 2);
        }


        osd_clearbitmap(bitmap);

	/* clear the input memory */
        while (code_read_async() != CODE_NONE) {
        }

        while (displaygameinfo(bitmap, 0) == 1) {
            update_video_and_audio();
/*TODO*///      osd_poll_joysticks();
        }

        osd_clearbitmap(bitmap);
    /* make sure that the screen is really cleared, in case autoframeskip kicked in */
        update_video_and_audio();
        update_video_and_audio();
        update_video_and_audio();
        update_video_and_audio();

        return 0;
    }

    static int hist_scroll = 0;

    /* Display text entry for current driver from history.dat and mameinfo.dat. */
    public static int displayhistory(mame_bitmap bitmap, int selected) {

/*TODO*///	static char *buf = 0;
        int maxcols, maxrows;
        int sel;


        sel = selected - 1;


        maxcols = (Machine.uiwidth / Machine.uifontwidth) - 1;
        maxrows = (2 * Machine.uiheight - Machine.uifontheight) / (3 * Machine.uifontheight);
        maxcols -= 2;
        maxrows -= 8;
/*TODO*///
/*TODO*///	if (!buf)
/*TODO*///	{
/*TODO*///		/* allocate a buffer for the text */
/*TODO*///		buf = malloc (8192);
/*TODO*///		if (buf)
/*TODO*///		{
/*TODO*///			/* try to load entry */
/*TODO*///			if (load_driver_history (Machine->gamedrv, buf, 8192) == 0)
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
        {
/*TODO*///		if (buf)
/*TODO*///			display_scroll_message (bitmap, &scroll, maxcols, maxrows, buf);
/*TODO*///		else
/*TODO*///		{
            String msg = "";

            msg += "\t";
            msg += ui_getstring(UI_historymissing);
            msg += "\n\n\t";
            msg += ui_getstring(UI_lefthilight);
            msg += " ";
            msg += ui_getstring(UI_returntomain);
            msg += " ";
            msg += ui_getstring(UI_righthilight);
            ui_displaymessagewindow(bitmap, msg);
/*TODO*///		}

            if ((hist_scroll > 0) && input_ui_pressed_repeat(IPT_UI_UP, 4) != 0) {
                if (hist_scroll == 2) hist_scroll = 0;	/* 1 would be the same as 0, but with arrow on top */
                else hist_scroll--;
            }

            if (input_ui_pressed_repeat(IPT_UI_DOWN, 4) != 0) {
                if (hist_scroll == 0) hist_scroll = 2;	/* 1 would be the same as 0, but with arrow on top */
                else hist_scroll++;
            }

            if (input_ui_pressed(IPT_UI_SELECT) != 0)
                sel = -1;

            if (input_ui_pressed(IPT_UI_CANCEL) != 0)
                sel = -1;

            if (input_ui_pressed(IPT_UI_CONFIGURE) != 0)
                sel = -2;
        }
        if (sel == -1 || sel == -2) {
			/* tell updatescreen() to clean after us */
            need_to_clear_bitmap = 1;
/*TODO*///
/*TODO*///		/* force buffer to be recreated */
/*TODO*///		if (buf)
/*TODO*///		{
/*TODO*///			free (buf);
/*TODO*///			buf = 0;
/*TODO*///        }
        }

        return sel + 1;
    }

    public static final int UI_SWITCH = 0;
    public static final int UI_DEFCODE = 1;
    public static final int UI_CODE = 2;
    public static final int UI_ANALOG = 3;
    public static final int UI_CALIBRATE = 4;
    public static final int UI_STATS = 5;
    public static final int UI_GAMEINFO = 6;
    public static final int UI_HISTORY = 7;
    public static final int UI_CHEAT = 8;
    public static final int UI_RESET = 9;
    public static final int UI_MEMCARD = 10;
    public static final int UI_EXIT = 11;

    public static final int MAX_SETUPMENU_ITEMS = 20;
    static String[] menu_item = new String[MAX_SETUPMENU_ITEMS];
    static int[] menu_action = new int[MAX_SETUPMENU_ITEMS];
    static int menu_total;


    public static void setup_menu_init() {
        menu_total = 0;

        menu_item[menu_total] = ui_getstring(UI_inputgeneral);
        menu_action[menu_total++] = UI_DEFCODE;
        menu_item[menu_total] = ui_getstring(UI_inputspecific);
        menu_action[menu_total++] = UI_CODE;
        menu_item[menu_total] = ui_getstring(UI_dipswitches);
        menu_action[menu_total++] = UI_SWITCH;

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
/*TODO*///
        menu_item[menu_total] = ui_getstring(UI_bookkeeping);
        menu_action[menu_total++] = UI_STATS;
        menu_item[menu_total] = ui_getstring(UI_gameinfo);
        menu_action[menu_total++] = UI_GAMEINFO;
        menu_item[menu_total] = ui_getstring(UI_history);
        menu_action[menu_total++] = UI_HISTORY;


	if (options.cheat!=0)
	{
		menu_item[menu_total] = ui_getstring (UI_cheat); menu_action[menu_total++] = UI_CHEAT;
	}


/*TODO*///	if (Machine->gamedrv->clone_of == &driver_neogeo ||
/*TODO*///			(Machine->gamedrv->clone_of &&
/*TODO*///				Machine->gamedrv->clone_of->clone_of == &driver_neogeo))
/*TODO*///	{
/*TODO*///		menu_item[menu_total] = ui_getstring (UI_memorycard); menu_action[menu_total++] = UI_MEMCARD;
/*TODO*///	}
/*TODO*///
/*TODO*///
        menu_item[menu_total] = ui_getstring(UI_resetgame);
        menu_action[menu_total++] = UI_RESET;
        menu_item[menu_total] = ui_getstring(UI_returntogame);
        menu_action[menu_total++] = UI_EXIT;
        menu_item[menu_total] = null; /* terminate array */
    }

    static int menu_lastselected = 0;

    public static int setup_menu(mame_bitmap bitmap, int selected) {
        int sel, res = -1;

        if (selected == -1)
            sel = menu_lastselected;
        else sel = selected - 1;

        if (sel > SEL_MASK) {
            switch (menu_action[sel & SEL_MASK]) {
                case UI_SWITCH:
                    res = setdipswitches(bitmap, sel >> SEL_BITS);
                    break;
                case UI_DEFCODE:
                    res = setdefcodesettings(bitmap, sel >> SEL_BITS);
                    break;
                case UI_CODE:
                    res = setcodesettings(bitmap, sel >> SEL_BITS);
                    break;
/*TODO*///			case UI_ANALOG:
/*TODO*///				res = settraksettings(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
/*TODO*///			case UI_CALIBRATE:
/*TODO*///				res = calibratejoysticks(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
                case UI_STATS:
                    res = mame_stats(bitmap, sel >> SEL_BITS);
                    break;
                case UI_GAMEINFO:
                    res = displaygameinfo(bitmap, sel >> SEL_BITS);
                    break;
                case UI_HISTORY:
                    res = displayhistory(bitmap, sel >> SEL_BITS);
                    break;
			case UI_CHEAT:
				res = cheat_menu(bitmap, sel >> SEL_BITS);
				break;
/*TODO*///			case UI_MEMCARD:
/*TODO*///				res = memcard_menu(bitmap, sel >> SEL_BITS);
/*TODO*///				break;
            }

            if (res == -1) {
                menu_lastselected = sel;
                sel = -1;
            } else
                sel = (sel & SEL_MASK) | (res << SEL_BITS);

            return sel + 1;
        }


        ui_displaymenu(bitmap, menu_item, null, null, sel, 0);

        if (input_ui_pressed_repeat(IPT_UI_DOWN, 8) != 0)
            sel = (sel + 1) % menu_total;

        if (input_ui_pressed_repeat(IPT_UI_UP, 8) != 0)
            sel = (sel + menu_total - 1) % menu_total;

        if (input_ui_pressed(IPT_UI_SELECT) != 0) {
            switch (menu_action[sel]) {
                case UI_SWITCH:
                case UI_DEFCODE:
                case UI_CODE:
                case UI_ANALOG:
                case UI_CALIBRATE:
                case UI_STATS:
                case UI_GAMEINFO:
                case UI_HISTORY:
                case UI_CHEAT:
                case UI_MEMCARD:
                    sel |= 1 << SEL_BITS;
					/* tell updatescreen() to clean after us */
                    need_to_clear_bitmap = 1;
                    break;

                case UI_RESET:
                    machine_reset();
                    break;

                case UI_EXIT:
                    menu_lastselected = 0;
                    sel = -1;
                    break;
            }
        }

        if (input_ui_pressed(IPT_UI_CANCEL) != 0 ||
                input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
            menu_lastselected = sel;
            sel = -1;
        }

        if (sel == -1) {
			/* tell updatescreen() to clean after us */
            need_to_clear_bitmap = 1;
        }

        return sel + 1;
    }

    /**
     * ******************************************************************
     * <p>
     * start of On Screen Display handling
     * <p>
     * *******************************************************************
     */

    public static void displayosd(mame_bitmap bitmap, String text, int percentage, int default_percentage) {
        DisplayText[] dt = DisplayText.create(2);
        int avail;


        avail = (Machine.uiwidth / Machine.uifontwidth) * 19 / 20;

        ui_drawbox(bitmap, (Machine.uiwidth - Machine.uifontwidth * avail) / 2,
                (Machine.uiheight - 7 * Machine.uifontheight / 2),
                avail * Machine.uifontwidth,
                3 * Machine.uifontheight);

        avail--;

        drawbar(bitmap, (Machine.uiwidth - Machine.uifontwidth * avail) / 2,
                (Machine.uiheight - 3 * Machine.uifontheight),
                avail * Machine.uifontwidth,
                Machine.uifontheight,
                percentage, default_percentage);

        dt[0].text = text;
        dt[0].color = UI_COLOR_NORMAL;
        dt[0].x = (Machine.uiwidth - Machine.uifontwidth * strlen(text)) / 2;
        dt[0].y = (Machine.uiheight - 2 * Machine.uifontheight) + 2;
        dt[1].text = null; /* terminate array */
        displaytext(bitmap, dt, 0, 0);
    }

    public static onscrd_fncPtr onscrd_volume = new onscrd_fncPtr() {
        public void handler(mame_bitmap bitmap, int increment, int arg) {
            String buf;
            int attenuation;

            if (increment != 0) {
                attenuation = osd_get_mastervolume();
                attenuation += increment;
                if (attenuation > 0) attenuation = 0;
                if (attenuation < -32) attenuation = -32;
                osd_set_mastervolume(attenuation);
            }
            attenuation = osd_get_mastervolume();

            buf = sprintf("%s %3ddB", ui_getstring(UI_volume), attenuation);
            displayosd(bitmap, buf, 100 * (attenuation + 32) / 32, 100);
        }
    };
    /*TODO*///
/*TODO*///static void onscrd_mixervol(struct osd_bitmap *bitmap,int increment,int arg)
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

    public static onscrd_fncPtr onscrd_brightness = new onscrd_fncPtr() {
        public void handler(mame_bitmap bitmap, int increment, int arg) {
            String buf;
            int brightness;


            if (increment != 0) {
                brightness = osd_get_brightness();
                brightness += 5 * increment;
                if (brightness < 0) brightness = 0;
                if (brightness > 100) brightness = 100;
                osd_set_brightness(brightness);
            }
            brightness = osd_get_brightness();
            buf = sprintf("%s %3d%%", ui_getstring(UI_brightness), brightness);
            displayosd(bitmap, buf, brightness, 100);
        }
    };

    /*TODO*///static void onscrd_gamma(struct osd_bitmap *bitmap,int increment,int arg)
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
/*TODO*///static void onscrd_vector_intensity(struct osd_bitmap *bitmap,int increment,int arg)
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
/*TODO*///static void onscrd_overclock(struct osd_bitmap *bitmap,int increment,int arg)
/*TODO*///{
/*TODO*///	char buf[30];
/*TODO*///	double overclock;
/*TODO*///	int cpu_old, doallcpus = 0, oc;
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
/*TODO*///			for( cpu_old = 0; cpu_old < cpu_gettotalcpu(); cpu_old++ )
/*TODO*///				timer_set_overclock(cpu_old, overclock);
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
    public static final int MAX_OSD_ITEMS = 30;

    public static abstract interface onscrd_fncPtr {
        public abstract void handler(mame_bitmap bitmap, int increment, int arg);

    }

    public static onscrd_fncPtr[] onscrd_fnc = new onscrd_fncPtr[MAX_OSD_ITEMS];
    public static int[] onscrd_arg = new int[MAX_OSD_ITEMS];
    static int onscrd_total_items;

    public static void onscrd_init() {
        int item, ch;


        item = 0;

        onscrd_fnc[item] = onscrd_volume;
        onscrd_arg[item] = 0;
        item++;

/*TODO*///	for (ch = 0;ch < MIXER_MAX_CHANNELS;ch++)
/*TODO*///	{
/*TODO*///		if (mixer_get_name(ch) != 0)
/*TODO*///		{
/*TODO*///			onscrd_fnc[item] = onscrd_mixervol;
/*TODO*///			onscrd_arg[item] = ch;
/*TODO*///			item++;
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
        onscrd_fnc[item] = onscrd_brightness;
        onscrd_arg[item] = 0;
        item++;

/*TODO*///	onscrd_fnc[item] = onscrd_gamma;
/*TODO*///	onscrd_arg[item] = 0;
/*TODO*///	item++;
/*TODO*///
/*TODO*///	if (Machine->drv->video_attributes & VIDEO_TYPE_VECTOR)
/*TODO*///	{
/*TODO*///		onscrd_fnc[item] = onscrd_vector_intensity;
/*TODO*///		onscrd_arg[item] = 0;
/*TODO*///		item++;
/*TODO*///	}

        onscrd_total_items = item;
    }

    static int lastselected = 0;

    public static int on_screen_display(mame_bitmap bitmap, int selected) {
        int increment, sel;

        if (selected == -1)
            sel = lastselected;
        else sel = selected - 1;

        increment = 0;
        if (input_ui_pressed_repeat(IPT_UI_LEFT, 8) != 0)
            increment = -1;
        if (input_ui_pressed_repeat(IPT_UI_RIGHT, 8) != 0)
            increment = 1;
        if (input_ui_pressed_repeat(IPT_UI_DOWN, 8) != 0)
            sel = (sel + 1) % onscrd_total_items;
        if (input_ui_pressed_repeat(IPT_UI_UP, 8) != 0)
            sel = (sel + onscrd_total_items - 1) % onscrd_total_items;

        onscrd_fnc[sel].handler(bitmap, increment, onscrd_arg[sel]);

        lastselected = sel;

        if (input_ui_pressed(IPT_UI_ON_SCREEN_DISPLAY) != 0) {
            sel = -1;
			/* tell updatescreen() to clean after us */
            need_to_clear_bitmap = 1;
        }

        return sel + 1;
    }

    /**
     * ******************************************************************
     * <p>
     * end of On Screen Display handling
     * <p>
     * *******************************************************************
     */
    public static void displaymessage(mame_bitmap bitmap, String text) {
        DisplayText[] dt = DisplayText.create(2);
        int avail;


        if (Machine.uiwidth < Machine.uifontwidth * strlen(text)) {
            ui_displaymessagewindow(bitmap, text);
            return;
        }

        avail = strlen(text) + 2;

        ui_drawbox(bitmap, (Machine.uiwidth - Machine.uifontwidth * avail) / 2,
                Machine.uiheight - 3 * Machine.uifontheight,
                avail * Machine.uifontwidth,
                2 * Machine.uifontheight);

        dt[0].text = text;
        dt[0].color = UI_COLOR_NORMAL;
        dt[0].x = (Machine.uiwidth - Machine.uifontwidth * strlen(text)) / 2;
        dt[0].y = Machine.uiheight - 5 * Machine.uifontheight / 2;
        dt[1].text = null; /* terminate array */
        displaytext(bitmap, dt, 0, 0);
    }

    public static String messagetext;
    public static int messagecounter;

    public static void usrintf_showmessage(String text, Object... arg) {
        messagetext = sprintf(text, arg);
        messagecounter = (int) (2 * Machine.drv.frames_per_second);
    }

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
/*TODO*///
/*TODO*///
    static int show_total_colors;

    public static int handle_user_interface(mame_bitmap bitmap) {
/*TODO*///	/* if the user pressed F12, save the screen to a file */
/*TODO*///	if (input_ui_pressed(IPT_UI_SNAPSHOT))
/*TODO*///		osd_save_snapshot(bitmap);
/*TODO*///
/*TODO*///	/* This call is for the cheat, it must be called once a frame */
	if (options.cheat!=0) DoCheat(bitmap);
/*TODO*///
    /* if the user pressed ESC, stop the emulation */
    /* but don't quit if the setup menu is on screen */
        if (setup_selected == 0 && input_ui_pressed(IPT_UI_CANCEL) != 0)
            return 1;

        if (setup_selected == 0 && input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
            setup_selected = -1;
            if (osd_selected != 0) {
                osd_selected = 0;	/* disable on screen display */
				/* tell updatescreen() to clean after us */
                need_to_clear_bitmap = 1;
            }
        }
        if (setup_selected != 0) setup_selected = setup_menu(bitmap, setup_selected);

        if (osd_selected == 0 && input_ui_pressed(IPT_UI_ON_SCREEN_DISPLAY) != 0) {
            osd_selected = -1;
            if (setup_selected != 0) {
                setup_selected = 0; /* disable setup menu */
				/* tell updatescreen() to clean after us */
                need_to_clear_bitmap = 1;
            }
        }
        if (osd_selected != 0) osd_selected = on_screen_display(bitmap, osd_selected);

	/* if the user pressed F3, reset the emulation */
        if (input_ui_pressed(IPT_UI_RESET_MACHINE) != 0)
            machine_reset();


        if (single_step != 0 || input_ui_pressed(IPT_UI_PAUSE) != 0) /* pause the game */ {
/*		osd_selected = 0;	   disable on screen display, since we are going   */
                            /* to change parameters affected by it */

            if (single_step == 0) {
                osd_sound_enable(0);
                osd_pause(1);
            }

            while (input_ui_pressed(IPT_UI_PAUSE) == 0) {
                if (osd_skip_this_frame() == 0) {
                    if (need_to_clear_bitmap != 0 || bitmap_dirty != 0) {
                        osd_clearbitmap(bitmap);
                        need_to_clear_bitmap = 0;
                        draw_screen(bitmap_dirty);
                        bitmap_dirty = 0;
                    }
                }

/*TODO*///			if (input_ui_pressed(IPT_UI_SNAPSHOT))
/*TODO*///				osd_save_snapshot(bitmap);

                if (setup_selected == 0 && input_ui_pressed(IPT_UI_CANCEL) != 0)
                    return 1;

                if (setup_selected == 0 && input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
                    setup_selected = -1;
                    if (osd_selected != 0) {
                        osd_selected = 0;	/* disable on screen display */
					/* tell updatescreen() to clean after us */
                        need_to_clear_bitmap = 1;
                    }
                }
                if (setup_selected != 0) setup_selected = setup_menu(bitmap, setup_selected);

                if (osd_selected == 0 && input_ui_pressed(IPT_UI_ON_SCREEN_DISPLAY) != 0) {
                    osd_selected = -1;
                    if (setup_selected != 0) {
                        setup_selected = 0; /* disable setup menu */
					/* tell updatescreen() to clean after us */
                        need_to_clear_bitmap = 1;
                    }
                }
                if (osd_selected != 0) osd_selected = on_screen_display(bitmap, osd_selected);

			/* show popup message if any */
                if (messagecounter > 0) displaymessage(bitmap, messagetext);

                update_video_and_audio();
/*TODO*///			osd_poll_joysticks();
            }

            if (code_pressed(KEYCODE_LSHIFT) != 0 || code_pressed(KEYCODE_RSHIFT) != 0)
                single_step = 1;
            else {
                single_step = 0;
                osd_pause(0);
                osd_sound_enable(1);
            }
        }


	/* show popup message if any */
        if (messagecounter > 0) {
            displaymessage(bitmap, messagetext);

            if (--messagecounter == 0)
			/* tell updatescreen() to clean after us */
                need_to_clear_bitmap = 1;
        }

        if (input_ui_pressed(IPT_UI_SHOW_COLORS) != 0) {
            show_total_colors ^= 1;
            if (show_total_colors == 0)
			/* tell updatescreen() to clean after us */
                need_to_clear_bitmap = 1;
        }
        if (show_total_colors != 0) showtotalcolors(bitmap);


	/* if the user pressed F4, show the character set */
        if (input_ui_pressed(IPT_UI_SHOW_GFX) != 0) {
            osd_sound_enable(0);

            showcharset(bitmap);

            osd_sound_enable(1);
        }

        return 0;
    }
}
