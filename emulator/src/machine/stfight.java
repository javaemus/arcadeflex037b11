/**
 * ported to 0.37b7
 * ported to 0.36
 */
package machine;

import static arcadeflex.fucPtr.*;
import static old2.mame.memoryH.*;
import static mame.commonH.*;
import static old.mame.cpuintrf.*;
import static mame056.common.*;
import static arcadeflex.libc.ptr.*;
import static old2.mame.memory.*;
import static old.mame.inptport.*;
import static sound.MSM5205.*;
import static sound.MSM5205H.*;

public class stfight {

    /*
	
	Encryption PAL 16R4 on CPU board
	
	          +---U---+
	     CP --|       |-- VCC
	 ROM D1 --|       |-- ROM D0          M1 = 0                M1 = 1
	 ROM D3 --|       |-- (NC)
	 ROM D4 --|       |-- D6         D6 = D1 ^^ D3          D6 = / ( D1 ^^ D0 )
	 ROM D6 --|       |-- D4         D4 = / ( D6 ^^ A7 )    D4 = D3 ^^ A0
	     A0 --|       |-- D3         D3 = / ( D0 ^^ A1 )    D3 = D4 ^^ A4
	     A1 --|       |-- D0         D0 = D1 ^^ D4          D0 = / ( D6 ^^ A0 )
	     A4 --|       |-- (NC)
	     A7 --|       |-- /M1
	    GND --|       |-- /OE
	          +-------+
	
     */
    public static InitDriverPtr init_empcity = new InitDriverPtr() {
        public void handler() {
            UBytePtr rom = memory_region(REGION_CPU1);
            int diff = memory_region_length(REGION_CPU1) / 2;
            int A;

            memory_set_opcode_base(0, new UBytePtr(rom, diff));

            for (A = 0; A < 0x8000; A++) {
                /*unsigned*/ char src = rom.read(A);

                // decode opcode
                rom.write(A + diff,
                        (src & 0xA6)
                        | ((((src << 2) ^ src) << 3) & 0x40)
                        | (~((src ^ (A >> 1)) >> 2) & 0x10)
                        | (~(((src << 1) ^ A) << 2) & 0x08)
                        | (((src ^ (src >> 3)) >> 1) & 0x01));

                // decode operand
                rom.write(A,
                        (src & 0xA6)
                        | (~((src ^ (src << 1)) << 5) & 0x40)
                        | (((src ^ (A << 3)) << 1) & 0x10)
                        | (((src ^ A) >> 1) & 0x08)
                        | (~((src >> 6) ^ A) & 0x01));
            }
        }
    };

    public static InitDriverPtr init_stfight = new InitDriverPtr() {
        public void handler() {
            UBytePtr rom = memory_region(REGION_CPU1);
            int diff = memory_region_length(REGION_CPU1) / 2;

            init_empcity.handler();

            /* patch out a tight loop during startup - is the code waiting */
 /* for NMI to wake it up? */
            rom.write(0xb1 + diff, 0x00);
            rom.write(0xb2 + diff, 0x00);
            rom.write(0xb3 + diff, 0x00);
            rom.write(0xb4 + diff, 0x00);
            rom.write(0xb5 + diff, 0x00);
        }
    };
    public static InitMachinePtr stfight_init_machine = new InitMachinePtr() {
        public void handler() {
            // initialise rom bank
            stfight_bank_w.handler(0, 0);
        }
    };

    // It's entirely possible that this bank is never switched out
    // - in fact I don't even know how/where it's switched in!
    public static WriteHandlerPtr stfight_bank_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            UBytePtr ROM2 = new UBytePtr(memory_region(REGION_CPU1), 0x10000);

            cpu_setbank(1, new UBytePtr(ROM2, data << 14));
        }
    };
    public static InterruptPtr stfight_vb_interrupt = new InterruptPtr() {
        public int handler() {
            // Do a RST10
            interrupt_vector_w.handler(0, 0xd7);

            return (interrupt.handler());
        }
    };

    /*
	 *      CPU 1 timed interrupt - 30Hz???
     */
    public static InterruptPtr stfight_interrupt_1 = new InterruptPtr() {
        public int handler() {
            // Do a RST08
            interrupt_vector_w.handler(0, 0xcf);

            return (interrupt.handler());
        }
    };

    /*
	 *      CPU 2 timed interrupt - 120Hz
     */
    public static InterruptPtr stfight_interrupt_2 = new InterruptPtr() {
        public int handler() {
            return (interrupt.handler());
        }
    };

    /*
	 *      Hardware handlers
     */
    // Perhaps define dipswitches as active low?
    public static ReadHandlerPtr stfight_dsw_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            return (~readinputport(3 + offset));
        }
    };

    static int stfight_coin_mech_query_active = 0;
    static int stfight_coin_mech_query;
    static int coin_mech_latch[] = {0x02, 0x01};
    public static ReadHandlerPtr stfight_coin_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            int coin_mech_data;
            int i;

            // Was the coin mech queried by software?
            if (stfight_coin_mech_query_active != 0) {
                stfight_coin_mech_query_active = 0;
                return ((~stfight_coin_mech_query) & 0x03);
            }

            /*
	     *      Is this really necessary?
	     *      - we can control impulse length so that the port is
	     *        never strobed twice within the impulse period
	     *        since it's read by the 30Hz interrupt ISR
             */
            coin_mech_data = readinputport(5);

            for (i = 0; i < 2; i++) {
                /* Only valid on signal edge */
                if ((coin_mech_data & (1 << i)) != coin_mech_latch[i]) {
                    coin_mech_latch[i] = coin_mech_data & (1 << i);
                } else {
                    coin_mech_data |= coin_mech_data & (1 << i);
                }
            }

            return (coin_mech_data);
        }
    };

    public static WriteHandlerPtr stfight_coin_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            // interrogate coin mech
            stfight_coin_mech_query_active = 1;
            stfight_coin_mech_query = data;
        }
    };

    /*
	 *      Machine hardware for MSM5205 ADPCM sound control
     */
    static int sampleLimits[]
            = {
                0x0000, // machine gun fire?
                0x1000, // player getting shot
                0x2C00, // player shooting
                0x3C00, // girl screaming
                0x5400, // girl getting shot
                0x7200 // (end of samples)
            };
    static int adpcm_data_offs;
    static int adpcm_data_end;
    static int toggle;
    public static vclk_interruptPtr stfight_adpcm_int = new vclk_interruptPtr() {
        public void handler(int data) {

            UBytePtr SAMPLES = memory_region(REGION_SOUND1);
            int adpcm_data = SAMPLES.read(adpcm_data_offs & 0x7fff);

            // finished playing sample?
            if (adpcm_data_offs == adpcm_data_end) {
                MSM5205_reset_w.handler(0, 1);
                return;
            }

            if (toggle == 0) {
                MSM5205_data_w.handler(0, (adpcm_data >> 4) & 0x0f);
            } else {
                MSM5205_data_w.handler(0, adpcm_data & 0x0f);
                adpcm_data_offs++;
            }

            toggle ^= 1;
        }
    };

    public static WriteHandlerPtr stfight_adpcm_control_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            if (data < 0x08) {
                adpcm_data_offs = sampleLimits[data];
                adpcm_data_end = sampleLimits[data + 1];
            }

            MSM5205_reset_w.handler(0, (data & 0x08) != 0 ? 1 : 0);
        }
    };

    public static WriteHandlerPtr stfight_e800_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
        }
    };

    /*
	 *      Machine hardware for YM2303 fm sound control
     */
    static /*unsigned*/ char fm_data;

    public static WriteHandlerPtr stfight_fm_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            // the sound cpu ignores any fm data without bit 7 set
            fm_data = (char) ((0x80 | data) & 0xFF);
        }
    };

    public static ReadHandlerPtr stfight_fm_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            int data = fm_data & 0xFF;

            // clear the latch?!?
            fm_data &= 0x7f;

            return (data);
        }
    };
}
