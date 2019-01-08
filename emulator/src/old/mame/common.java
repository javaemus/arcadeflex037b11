/*
 *  Ported to 0.37b5
 *
 */
package old.mame;
import static arcadeflex.libc.cstdio.*;
import static arcadeflex.libc.cstring.*;
import static old.arcadeflex.fileio.*;
import static arcadeflex.fucPtr.WriteHandlerPtr;
import static old.arcadeflex.libc_old.*;
import static arcadeflex.libc.ptr.*;
import static old2.arcadeflex.libc_v2.charArrayToInt;
import static old2.arcadeflex.libc_v2.charArrayToLong;
import static old.arcadeflex.osdepend.logerror;
import static arcadeflex.video.*;
import static mame.commonH.*;
import static mame.drawgfxH.rectangle;
import static mame.driverH.*;
import static old2.mame.mame.*;
import static old2.mame.mameH.MAX_MEMORY_REGIONS;
import static mame.osdependH.*;

public class common {
    
    /* These globals are only kept on a machine_old basis - LBO 042898 */

    public static int dispensed_tickets;
    public static int coins[] = new int[COIN_COUNTERS];
    public static int lastcoin[] = new int[COIN_COUNTERS];
    public static int coinlockedout[] = new int[COIN_COUNTERS];
    
    public static void showdisclaimer()   /* MAURY_BEGIN: dichiarazione */ {
        printf("MAME is an emulator: it reproduces, more or less faithfully, the behaviour of\n"
                + "several arcade machines. But hardware is useless without software, so an image\n"
                + "of the ROMs which run on that hardware is required. Such ROMs, like any other\n"
                + "commercial software, are copyrighted material and it is therefore illegal to\n"
                + "use them if you don't own the original arcade machine_old. Needless to say, ROMs\n"
                + "are not distributed together with MAME. Distribution of MAME together with ROM\n"
                + "images is a violation of copyright law and should be promptly reported to the\n"
                + "authors so that appropriate legal action can be taken.\n\n");
    }

    public static void printromlist(RomModule[] romp, String basename)
    {
        if (romp==null) return;

        printf("This is the list of the ROMs required for driver \"%s\".\n"
                +"Name              Size       Checksum\n",basename);
        int rom_ptr=0;
        while (romp[rom_ptr].name!=null || romp[rom_ptr].offset!=0 || romp[rom_ptr].length!=0)
        {
            rom_ptr++;	/* skip memory region definition */

            while (romp[rom_ptr].length!=0)
            {
                String name;
                int length=0,expchecksum=0;


                name = romp[rom_ptr].name;
                expchecksum = romp[rom_ptr].crc;

                length = 0;

                do
                {
                                    /* ROM_RELOAD */
                    if ((romp[rom_ptr].name != null) && (romp[rom_ptr].name.compareTo("-1") == 0))
                        length = 0;	/* restart */

                    length += romp[rom_ptr].length & ~ROMFLAG_MASK;

                    rom_ptr++;
                } while (romp[rom_ptr].length!=0 && (romp[rom_ptr].name == null || romp[rom_ptr].name.compareTo("-1") == 0));

                if (expchecksum!=0)
                    printf("%-12s  %7d bytes  %08x\n",name,length,expchecksum);
                else
                    printf("%-12s  %7d bytes  NO GOOD DUMP KNOWN\n",name,length);
            }
        }
    }

    /* ************************************************************************
     * <p>
     * Read samples into memory.
     * This function is different from readroms() because it doesn't fail if
     * it doesn't find a file: it will load as many samples as it can find.
     * <p>
     * *************************************************************************
     */
    static GameSample read_wav_sample(Object f) {
        long /*unsigned*/ offset = 0;
        long /*UINT32*/ length, rate, filesize, temp32;
        int /*UINT16*/ bits, temp16;
        char[] /*UINT8*/ buf = new char[32];
        GameSample result = null;


	/* read the core header and make sure it's a WAVE file */
        offset += osd_fread(f, buf, 4);
        if (offset < 4)
            return null;
        if (memcmp(buf, 0, "RIFF", 4) != 0)
            return null;

	/* get the total size */
        offset += osd_fread(f, buf, 4);
        if (offset < 8)
            return null;
        filesize = charArrayToLong(buf);

	/* read the RIFF file type and make sure it's a WAVE file */
        offset += osd_fread(f, buf, 4);
        if (offset < 12)
            return null;
        if (memcmp(buf, 0, "WAVE", 4) != 0)
            return null;


	/* seek until we find a format tag */
        while (true) {
            offset += osd_fread(f, buf, 4);
            char[] tmp = new char[buf.length];//temp creation
            System.arraycopy(buf, 0, tmp, 0, buf.length);//temp creation
            offset += osd_fread(f, buf, 4);//offset += osd_fread(f, &length, 4);
            length = charArrayToLong(buf);
            if (memcmp(tmp, 0, "fmt ", 4) == 0)
                break;

		/* seek to the next block */
            osd_fseek(f, (int) length, SEEK_CUR);
            offset += length;
            if (offset >= filesize)
                return null;
        }
    /* read the format -- make sure it is PCM */
        offset += osd_fread_lsbfirst(f, buf, 2);
        temp16 = charArrayToInt(buf);
        if (temp16 != 1)
            return null;

	/* number of channels -- only mono is supported */
        offset += osd_fread_lsbfirst(f, buf, 2);
        temp16 = charArrayToInt(buf);
        if (temp16 != 1)
            return null;

	/* sample rate */
        offset += osd_fread(f, buf, 4);
        rate = charArrayToLong(buf);

	/* bytes/second and block alignment are ignored */
        offset += osd_fread(f, buf, 6);

	/* bits/sample */
        offset += osd_fread_lsbfirst(f, buf, 2);
        bits = charArrayToInt(buf);
        if (bits != 8 && bits != 16)
            return null;


	/* seek past any extra data */
        osd_fseek(f, (int) length - 16, SEEK_CUR);
        offset += length - 16;

	/* seek until we find a data tag */
        while (true) {
            offset += osd_fread(f, buf, 4);
            char[] tmp = new char[buf.length];//temp creation
            System.arraycopy(buf, 0, tmp, 0, buf.length);//temp creation
            offset += osd_fread(f, buf, 4);//offset += osd_fread(f, &length, 4);
            length = charArrayToLong(buf);
            if (memcmp(tmp, 0, "data", 4) == 0)
                break;

		/* seek to the next block */
            osd_fseek(f, (int) length, SEEK_CUR);
            offset += length;
            if (offset >= filesize)
                return null;
        }
    /* allocate the game sample */
        result = new GameSample((int) length);
    /* fill in the sample data */
        result.length = (int) length;
        result.smpfreq = (int) rate;
        result.resolution = bits;

	/* read the data in */
        if (bits == 8) {
            osd_fread(f, result.data, (int) length);

		/* convert 8-bit data to signed samples */
            for (temp32 = 0; temp32 < length; temp32++)
                result.data[(int) temp32] ^= 0x80;
        } else {
        /* 16-bit data is fine as-is */
            osd_fread_lsbfirst(f, result.data, (int) length);
        }

        return result;
    }

    public static GameSamples readsamples(String[] samplenames, String basename)
/* V.V - avoids samples duplication */
/* if first samplename is *dir, looks for samples into "basename" first, then "dir" */ {
        int i;
        GameSamples samples = new GameSamples();
        int skipfirst = 0;

	/* if the user doesn't want to use samples, bail */
        if (options.use_samples == 0) return null;

        if (samplenames == null || samplenames[0] == null) return null;


        if (samplenames[0].charAt(0) == '*')
            skipfirst = 1;

        i = 0;
        while (samplenames[i + skipfirst] != null) i++;

        if (i == 0) return null;

        samples = new GameSamples(i);

        samples.total = i;
        for (i = 0; i < samples.total; i++)
            samples.sample[i] = null;

        for (i = 0; i < samples.total; i++) {
            Object f;

            if (samplenames[i + skipfirst].length() > 0 && samplenames[i + skipfirst].charAt(0) != '\0') {
                if ((f = osd_fopen(basename, samplenames[i + skipfirst], OSD_FILETYPE_SAMPLE, 0)) == null)
                    if (skipfirst != 0)
                        f = osd_fopen(samplenames[0].substring(1, samplenames[0].length())/*samplenames[0] + 1*/, samplenames[i + skipfirst], OSD_FILETYPE_SAMPLE, 0);
                if (f != null) {
                    samples.sample[i] = read_wav_sample(f);
                    osd_fclose(f);
                }
            }
        }

        return samples;
    }


    public static void freesamples(GameSamples samples) {
        int i;

        if (samples == null) return;

        for (i = 0; i < samples.total; i++)
            samples.sample[i] = null;

        samples = null;
    }
    /* LBO 042898 - added coin counters */
    public static WriteHandlerPtr coin_counter_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (offset >= COIN_COUNTERS) return;
            /* Count it only if the data has changed from 0 to non-zero */
            if (data != 0 && (lastcoin[offset] == 0)) {
                coins[offset]++;
            }
            lastcoin[offset] = data;
        }
    };
    public static WriteHandlerPtr coin_lockout_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (offset >= COIN_COUNTERS) return;

            coinlockedout[offset] = data;
        }
    };

    /* Locks out all the coin inputs */
    public static WriteHandlerPtr coin_lockout_global_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int i;

            for (i = 0; i < COIN_COUNTERS; i++) {
                coin_lockout_w.handler(i, data);
            }
        }
    };
}
