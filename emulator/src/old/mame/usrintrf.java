/**
 * ported to 0.37b5
 */
package old.mame;
import static arcadeflex.libc.cstdio.*;
import static old.arcadeflex.libc_old.strlen;
import static mame.cheat.DoCheat;
import static mame.cheat.cheat_menu;
import static old.arcadeflex.sound.*;
import static arcadeflex.video.*;
import static old2.mame.mame.update_video_and_audio;
import static old.mame.drawgfx.*;
import static old.mame.drawgfx.drawgfx;
import static mame.drawgfxH.*;
import static mame.driver.drivers;
import static mame056.driverH.*;
import static mame056.inptportH.*;
import mame056.commonH.mame_bitmap;
import static mame056.input.*;
import static mame056.inputH.*;
import static old2.mame.mame.*;
import static mame.sndintrf.*;
import static mame056.ui_text.ui_getstring;
import static mame056.ui_textH.*;
import static mame056.usrintrfH.*;
import static mame056.version.build_version;
import static mame056.cpuexec.machine_reset;
import static mame056.cpuexecH.CPU_AUDIO_CPU;
import static mame056.cpuintrf.cputype_name;
import static mame056.usrintrf.displaygameinfo;
import static mame056.usrintrf.drawbar;
import static mame056.usrintrf.mame_stats;
import static mame056.usrintrf.messagecounter;
import static mame056.usrintrf.messagetext;
import static mame056.usrintrf.on_screen_display;
import static mame056.usrintrf.onscrd_brightness;
import static mame056.usrintrf.onscrd_volume;
import static mame056.usrintrf.setcodesettings;
import static mame056.usrintrf.setdefcodesettings;
import static mame056.usrintrf.setdipswitches;
import static mame056.usrintrf.setup_menu;
import static mame056.usrintrf.showcharset;
import static mame056.usrintrf.switch_true_orientation;
import static mame056.usrintrf.switch_ui_orientation;
import static mame056.usrintrf.ui_drawbox;


public class usrintrf {
 
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
}
