/**
 * Ported to 0.56
 */
package mame056;

import static mame056.cpuintrf.*;

public class cpuintrfH {

    /**
     * ***********************************
     *
     * Enum listing all the CPUs
     *
     ************************************
     */
    public static final int CPU_DUMMY = 0;
    public static final int CPU_Z80 = 1;
    public static final int CPU_8080 = 2;
    public static final int CPU_8085A = 3;
    public static final int CPU_M6502 = 4;
    public static final int CPU_M65C02 = 5;
    public static final int CPU_M65SC02 = 6;
    public static final int CPU_M65CE02 = 7;
    public static final int CPU_M6509 = 8;
    public static final int CPU_M6510 = 9;
    public static final int CPU_M6510T = 10;
    public static final int CPU_M7501 = 11;
    public static final int CPU_M8502 = 12;
    public static final int CPU_N2A03 = 13;
    public static final int CPU_M4510 = 14;
    public static final int CPU_H6280 = 15;
    public static final int CPU_I86 = 16;
    public static final int CPU_I88 = 17;
    public static final int CPU_I186 = 18;
    public static final int CPU_I188 = 19;
    public static final int CPU_I286 = 20;
    public static final int CPU_V20 = 21;
    public static final int CPU_V30 = 22;
    public static final int CPU_V33 = 23;
    public static final int CPU_V60 = 24;
    public static final int CPU_I8035 = 25;
    public static final int CPU_I8039 = 26;
    public static final int CPU_I8048 = 27;
    public static final int CPU_N7751 = 28;
    public static final int CPU_I8X41 = 29;
    public static final int CPU_M6800 = 30;
    public static final int CPU_M6801 = 31;
    public static final int CPU_M6802 = 32;
    public static final int CPU_M6803 = 33;
    public static final int CPU_M6808 = 34;
    public static final int CPU_HD63701 = 35;
    public static final int CPU_NSC8105 = 36;
    public static final int CPU_M6805 = 37;
    public static final int CPU_M68705 = 38;
    public static final int CPU_HD63705 = 39;
    public static final int CPU_HD6309 = 40;
    public static final int CPU_M6809 = 41;
    public static final int CPU_KONAMI = 42;
    public static final int CPU_M68000 = 43;
    public static final int CPU_M68010 = 44;
    public static final int CPU_M68EC020 = 45;
    public static final int CPU_M68020 = 46;
    public static final int CPU_T11 = 47;
    public static final int CPU_S2650 = 48;
    public static final int CPU_TMS34010 = 49;
    public static final int CPU_TMS34020 = 50;
    public static final int CPU_TMS9900 = 51;
    public static final int CPU_TMS9940 = 52;
    public static final int CPU_TMS9980 = 53;
    public static final int CPU_TMS9985 = 54;
    public static final int CPU_TMS9989 = 55;
    public static final int CPU_TMS9995 = 56;
    public static final int CPU_TMS99105A = 57;
    public static final int CPU_TMS99110A = 58;
    public static final int CPU_Z8000 = 59;
    public static final int CPU_TMS320C10 = 60;
    public static final int CPU_CCPU = 61;
    public static final int CPU_ADSP2100 = 62;
    public static final int CPU_ADSP2105 = 63;
    public static final int CPU_PSXCPU = 64;
    public static final int CPU_ASAP = 65;
    public static final int CPU_UPD7810 = 66;

    public static final int CPU_COUNT = 67;

    /**
     * ***********************************
     *
     * Interrupt line constants
     *
     ************************************
     */
    /* line states */
    public static final int CLEAR_LINE = 0;/* clear (a fired, held or pulsed) line */
    public static final int ASSERT_LINE = 1;/* assert an interrupt immediately */
    public static final int HOLD_LINE = 2;/* hold interrupt line until acknowledged */
    public static final int PULSE_LINE = 3;/* pulse interrupt line for one instruction */

 /* internal flags (not for use by drivers!) */
    public static final int INTERNAL_CLEAR_LINE = 100 + CLEAR_LINE;
    public static final int INTERNAL_ASSERT_LINE = 100 + ASSERT_LINE;

    /* interrupt parameters */
    public static final int MAX_IRQ_LINES = 8;/* maximum number of IRQ lines per CPU */
    public static final int IRQ_LINE_NMI = 127;/* IRQ line for NMIs */


    /**
     * ***********************************
     *
     * CPU information constants
     *
     ************************************
     */

    /* get_reg/set_reg constants */
    public static final int MAX_REGS = 128;/* maximum number of register of any CPU */

 /* This value is passed to cpu_get_reg to retrieve the previous
	 * program counter value, ie. before a CPU emulation started
	 * to fetch opcodes and arguments for the current instrution. */
    public static final int REG_PREVIOUSPC = -1;

    /* This value is passed to cpu_get_reg to retrieve the current
	 * program counter value. */
    public static final int REG_PC = -2;

    /* This value is passed to cpu_get_reg to retrieve the current
	 * stack pointer value. */
    public static final int REG_SP = -3;

    /* This value is passed to cpu_get_reg/cpu_set_reg, instead of one of
	 * the names from the enum a CPU core defines for it's registers,
	 * to get or set the contents of the memory pointed to by a stack pointer.
	 * You can specify the n'th element on the stack by (REG_SP_CONTENTS-n),
	 * ie. lower negative values. The actual element size (UINT16 or UINT32)
	 * depends on the CPU core. */
    public static final int REG_SP_CONTENTS = -4;


    /* endianness constants */
    public static final int CPU_IS_LE = 0;/* emulated CPU is little endian */
    public static final int CPU_IS_BE = 1;/* emulated CPU is big endian */

 /* Values passed to the cpu_info function of a core to retrieve information */
    public static final int CPU_INFO_REG = 0;
    public static final int CPU_INFO_FLAGS = MAX_REGS;
    public static final int CPU_INFO_NAME = MAX_REGS + 1;
    public static final int CPU_INFO_FAMILY = MAX_REGS + 2;
    public static final int CPU_INFO_VERSION = MAX_REGS + 3;
    public static final int CPU_INFO_FILE = MAX_REGS + 4;
    public static final int CPU_INFO_CREDITS = MAX_REGS + 5;
    public static final int CPU_INFO_REG_LAYOUT = MAX_REGS + 6;
    public static final int CPU_INFO_WIN_LAYOUT = MAX_REGS + 7;

    /**
     * ***********************************
     *
     * Core CPU interface structure
     *
     ************************************
     */
    public static abstract interface burnPtr {

        public abstract void handler(int cycles);
    }

    public static abstract interface irqcallbacksPtr {

        public abstract int handler(int irqline);
    }

    public static abstract class cpu_interface {

        /* index (used to make sure we mach the enum above */
        public int cpu_num;

        /* table of core functions */
        public abstract void init();

        public abstract void reset(Object param);

        public abstract void exit();

        public abstract int execute(int cycles);

        public burnPtr burn;

        public abstract Object init_context();//not in mame , used specific for arcadeflex

        public abstract Object get_context();//different from mame returns reg object and not size since java doesn't support references

        public abstract void set_context(Object reg);

        public abstract int[] get_cycle_table(int which);

        public abstract void set_cycle_table(int which, int[] new_table);

        public abstract int get_reg(int regnum);

        public abstract void set_reg(int regnum, int val);

        public abstract void set_irq_line(int irqline, int linestate);

        public abstract void set_irq_callback(irqcallbacksPtr callback);

        public abstract String cpu_info(Object context, int regnum);

        public abstract String cpu_dasm(String buffer, int pc);

        /* IRQ and clock information */
        public int/*unsigned*/ num_irqs;
        public int default_vector;
        public int[] icount;
        public double overclock;
        public int irq_int;

        /* memory information */
        public int databus_width;

        public abstract int memory_read(int offset);

        public abstract void memory_write(int offset, int data);

        public abstract int internal_read(int offset);

        public abstract void internal_write(int offset, int data);

        public int pgm_memory_base;

        public abstract void set_op_base(int pc);

        public int address_shift;
        public int/*unsigned*/ address_bits;
        public int/*unsigned*/ endianess;
        public int/*unsigned*/ align_unit;
        public int/*unsigned*/ max_inst_len;
    }

    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	 Core CPU interface functions
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////* reset the internal CPU tracking */
/*TODO*///int cpuintrf_init(void);
/*TODO*///
/*TODO*////* set up the interface for one CPU of a given type */
/*TODO*///int	cpuintrf_init_cpu(int cpunum, int cputype);
/*TODO*///
/*TODO*////* clean up the interface for one CPU */
/*TODO*///void cpuintrf_exit_cpu(int cpunum);
/*TODO*///
/*TODO*////* remember the previous context and set a new one */
/*TODO*///void cpuintrf_push_context(int cpunum);
/*TODO*///
/*TODO*////* restore the previous context */
/*TODO*///void cpuintrf_pop_context(void);
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	 Active CPU acccessors
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////* apply a +/- to the current icount */
/*TODO*///void activecpu_adjust_icount(int delta);
/*TODO*///
/*TODO*////* return the current icount */
/*TODO*///int activecpu_get_icount(void);
/*TODO*///
/*TODO*////* ensure banking is reset properly */
/*TODO*///void activecpu_reset_banking(void);
/*TODO*///
/*TODO*////* set the IRQ line on a CPU -- drivers use cpu_set_irq_line() */
/*TODO*///void activecpu_set_irq_line(int irqline, int state);
/*TODO*///
/*TODO*////* return a pointer to the active cycle count table for the active CPU */
/*TODO*///void *activecpu_get_cycle_table(int which);
/*TODO*///
/*TODO*////* set a pointer to the active cycle count table for the active CPU */
/*TODO*///void activecpu_set_cycle_tbl(int which, void *new_table);
/*TODO*///
/*TODO*////* return the value of a register on the active CPU */
/*TODO*///unsigned activecpu_get_reg(int regnum);
/*TODO*///
/*TODO*////* set the value of a register on the active CPU */
/*TODO*///void activecpu_set_reg(int regnum, unsigned val);
/*TODO*///
/*TODO*////* return the PC, corrected to a byte offset, on the active CPU */
/*TODO*///offs_t activecpu_get_pc_byte(void);
/*TODO*///
/*TODO*////* update the banking on the active CPU */
/*TODO*///void activecpu_set_op_base(unsigned val);
/*TODO*///
/*TODO*////* disassemble a line at a given PC on the active CPU */
/*TODO*///unsigned activecpu_dasm(char *buffer, unsigned pc);
/*TODO*///
/*TODO*////* return a string containing the state of the flags on the active CPU */
/*TODO*///const char *activecpu_flags(void);
/*TODO*///
/*TODO*////* return a string containing the value of a register on the active CPU */
/*TODO*///const char *activecpu_dump_reg(int regnum);
/*TODO*///
/*TODO*////* return a string containing the state of the active CPU */
/*TODO*///const char *activecpu_dump_state(void);
/*TODO*///
/*TODO*////* return the default IRQ line for the active CPU */
/*TODO*///int activecpu_default_irq_line(void);
/*TODO*///
/*TODO*////* return the default IRQ vector for the active CPU */
/*TODO*///int activecpu_default_irq_vector(void);
/*TODO*///
/*TODO*////* return the width of the address bus on the active CPU */
/*TODO*///unsigned activecpu_address_bits(void);
/*TODO*///
/*TODO*////* return the active address mask on the active CPU */
/*TODO*///unsigned activecpu_address_mask(void);
/*TODO*///
/*TODO*////* return the shift value to convert from address to bytes on the active CPU */
/*TODO*///int activecpu_address_shift(void);
/*TODO*///
/*TODO*////* return the endianess of the active CPU */
/*TODO*///unsigned activecpu_endianess(void);
/*TODO*///
/*TODO*////* return the width of the data bus on the active CPU */
/*TODO*///unsigned activecpu_databus_width(void);
/*TODO*///
/*TODO*////* return the required alignment of data accesses on the active CPU */
/*TODO*///unsigned activecpu_align_unit(void);
/*TODO*///
/*TODO*////* return the maximum length of one instruction on the active CPU */
/*TODO*///unsigned activecpu_max_inst_len(void);
/*TODO*///
/*TODO*////* return a string containing the name of the active CPU */
/*TODO*///const char *activecpu_name(void);
/*TODO*///
/*TODO*////* return a string containing the family of the active CPU */
/*TODO*///const char *activecpu_core_family(void);
/*TODO*///
/*TODO*////* return a string containing the version of the active CPU */
/*TODO*///const char *activecpu_core_version(void);
/*TODO*///
/*TODO*////* return a string containing the filename for the emulator of the active CPU */
/*TODO*///const char *activecpu_core_file(void);
/*TODO*///
/*TODO*////* return a string containing the emulation credits for the active CPU */
/*TODO*///const char *activecpu_core_credits(void);
/*TODO*///
/*TODO*////* return a string containing the registers of the active CPU */
/*TODO*///const char *activecpu_reg_layout(void);
/*TODO*///
/*TODO*////* return a string containing the debugger layout of the active CPU */
/*TODO*///const char *activecpu_win_layout(void);
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	 Specific CPU acccessors
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////* execute the requested cycles on a given CPU */
/*TODO*///int cpunum_execute(int cpunum, int cycles);
/*TODO*///
/*TODO*////* signal a reset and set the IRQ ack callback for a given CPU */
/*TODO*///void cpunum_reset(int cpunum, void *param, int (*irqack)(int));
/*TODO*///
/*TODO*////* read a byte from another CPU's memory space */
/*TODO*///data8_t cpunum_read_byte(int cpunum, offs_t address);
/*TODO*///
/*TODO*////* write a byte from another CPU's memory space */
/*TODO*///void cpunum_write_byte(int cpunum, offs_t address, data8_t data);
/*TODO*///
/*TODO*////* return a pointer to the saved context of a given CPU, or NULL if the
/*TODO*///   context is active (and contained within the CPU core */
/*TODO*///void *cpunum_get_context_ptr(int cpunum);
/*TODO*///
/*TODO*////* return a pointer to the active cycle count table for a given CPU */
/*TODO*///void *cpunum_get_cycle_table(int cpunum, int which);
/*TODO*///
/*TODO*////* set a pointer to the active cycle count table for a given CPU */
/*TODO*///void cpunum_set_cycle_tbl(int cpunum, int which, void *new_table);
/*TODO*///
/*TODO*////* return the value of a register on a given CPU */
/*TODO*///unsigned cpunum_get_reg(int cpunum, int regnum);
/*TODO*///
/*TODO*////* set the value of a register on a given CPU */
/*TODO*///void cpunum_set_reg(int cpunum, int regnum, unsigned val);
/*TODO*///
/*TODO*////* return the PC, corrected to a byte offset, on a given CPU */
/*TODO*///offs_t cpunum_get_pc_byte(int cpunum);
/*TODO*///
/*TODO*////* update the banking on a given CPU */
/*TODO*///void cpunum_set_op_base(int cpunum, unsigned val);
/*TODO*///
/*TODO*////* disassemble a line at a given PC on a given CPU */
/*TODO*///unsigned cpunum_dasm(int cpunum, char *buffer, unsigned pc);
/*TODO*///
/*TODO*////* return a string containing the state of the flags on a given CPU */
/*TODO*///const char *cpunum_flags(int cpunum);
/*TODO*///
/*TODO*////* return a string containing the value of a register on a given CPU */
/*TODO*///const char *cpunum_dump_reg(int cpunum, int regnum);
/*TODO*///
/*TODO*////* return a string containing the state of a given CPU */
/*TODO*///const char *cpunum_dump_state(int cpunum);
/*TODO*///
/*TODO*////* return the default IRQ line for a given CPU */
/*TODO*///int cpunum_default_irq_line(int cpunum);
/*TODO*///
/*TODO*////* return the default IRQ vector for a given CPU */
/*TODO*///int cpunum_default_irq_vector(int cpunum);
/*TODO*///
/*TODO*////* return the width of the address bus on a given CPU */
/*TODO*///unsigned cpunum_address_bits(int cpunum);
/*TODO*///
/*TODO*////* return the active address mask on a given CPU */
/*TODO*///unsigned cpunum_address_mask(int cpunum);
/*TODO*///
/*TODO*////* return the shift value to convert from address to bytes on a given CPU */
/*TODO*///int cpunum_address_shift(int cpunum);
/*TODO*///
/*TODO*////* return the endianess of a given CPU */
/*TODO*///unsigned cpunum_endianess(int cpunum);
/*TODO*///
/*TODO*////* return the width of the data bus on a given CPU */
/*TODO*///unsigned cpunum_databus_width(int cpunum);
/*TODO*///
/*TODO*////* return the required alignment of data accesses on a given CPU */
/*TODO*///unsigned cpunum_align_unit(int cpunum);
/*TODO*///
/*TODO*////* return the maximum length of one instruction on a given CPU */
/*TODO*///unsigned cpunum_max_inst_len(int cpunum);
/*TODO*///
/*TODO*////* return a string containing the name of a given CPU */
/*TODO*///const char *cpunum_name(int cpunum);
/*TODO*///
/*TODO*////* return a string containing the family of a given CPU */
/*TODO*///const char *cpunum_core_family(int cpunum);
/*TODO*///
/*TODO*////* return a string containing the version of a given CPU */
/*TODO*///const char *cpunum_core_version(int cpunum);
/*TODO*///
/*TODO*////* return a string containing the filename for the emulator of a given CPU */
/*TODO*///const char *cpunum_core_file(int cpunum);
/*TODO*///
/*TODO*////* return a string containing the emulation credits for a given CPU */
/*TODO*///const char *cpunum_core_credits(int cpunum);
/*TODO*///
/*TODO*////* return a string containing the registers of a given CPU */
/*TODO*///const char *cpunum_reg_layout(int cpunum);
/*TODO*///
/*TODO*////* return a string containing the debugger layout of a given CPU */
/*TODO*///const char *cpunum_win_layout(int cpunum);
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	 CPU type acccessors
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////* return the default IRQ line for a given CPU type */
/*TODO*///int cputype_default_irq_line(int cputype);
/*TODO*///
/*TODO*////* return the default IRQ vector for a given CPU type */
/*TODO*///int cputype_default_irq_vector(int cputype);
/*TODO*///
/*TODO*////* return the width of the address bus on a given CPU type */
/*TODO*///unsigned cputype_address_bits(int cputype);
/*TODO*///
/*TODO*////* return the active address mask on a given CPU type */
/*TODO*///unsigned cputype_address_mask(int cputype);
/*TODO*///
/*TODO*////* return the shift value to convert from address to bytes on a given CPU type */
/*TODO*///int cputype_address_shift(int cputype);
/*TODO*///
/*TODO*////* return the endianess of a given CPU type */
/*TODO*///unsigned cputype_endianess(int cputype);
/*TODO*///
/*TODO*////* return the width of the data bus on a given CPU type */
/*TODO*///unsigned cputype_databus_width(int cputype);
/*TODO*///
/*TODO*////* return the required alignment of data accesses on a given CPU type */
/*TODO*///unsigned cputype_align_unit(int cputype);
/*TODO*///
/*TODO*////* return the maximum length of one instruction on a given CPU type */
/*TODO*///unsigned cputype_max_inst_len(int cputype);
/*TODO*///
/*TODO*////* return a string containing the name of a given CPU type */
/*TODO*///const char *cputype_name(int cputype);
/*TODO*///
/*TODO*////* return a string containing the family of a given CPU type */
/*TODO*///const char *cputype_core_family(int cputype);
/*TODO*///
/*TODO*////* return a string containing the version of a given CPU type */
/*TODO*///const char *cputype_core_version(int cputype);
/*TODO*///
/*TODO*////* return a string containing the filename for the emulator of a given CPU type */
/*TODO*///const char *cputype_core_file(int cputype);
/*TODO*///
/*TODO*////* return a string containing the emulation credits for a given CPU type */
/*TODO*///const char *cputype_core_credits(int cputype);
/*TODO*///
/*TODO*////* return a string containing the registers of a given CPU type */
/*TODO*///const char *cputype_reg_layout(int cputype);
/*TODO*///
/*TODO*////* return a string containing the debugger layout of a given CPU type */
/*TODO*///const char *cputype_win_layout(int cputype);
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	 Miscellaneous functions
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////* dump the states of all CPUs */
/*TODO*///void cpu_dump_states(void);
/*TODO*///
/*TODO*////* set a callback function for reset on the 68k */
/*TODO*///void cpu_set_m68k_reset(int cpunum, void (*resetfn)(void));
/*TODO*///
/*TODO*////* convert IRQ number to IRQ line for old-style interrupts */
/*TODO*///int convert_type_to_irq_line(int cpunum, int num, int *vector);
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	 Macros
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///#define		activecpu_get_previouspc()	activecpu_get_reg(REG_PREVIOUSPC)
    public static int activecpu_get_pc() {
        return activecpu_get_reg(REG_PC);
    }

    /*TODO*///#define		activecpu_get_sp()			activecpu_get_reg(REG_SP)
/*TODO*///#define		activecpu_set_pc(val)		activecpu_set_reg(REG_PC, val)
/*TODO*///#define		activecpu_set_sp(val)		activecpu_set_reg(REG_SP, val)
/*TODO*///
/*TODO*///#define		cpunum_get_previouspc(cpu)	cpunum_get_reg(cpu, REG_PREVIOUSPC)
/*TODO*///#define		cpunum_get_pc(cpu)			cpunum_get_reg(cpu, REG_PC)
/*TODO*///#define		cpunum_get_sp(cpu)			cpunum_get_reg(cpu, REG_SP)
/*TODO*///#define		cpunum_set_pc(cpu, val)		cpunum_set_reg(cpu, REG_PC, val)
/*TODO*///#define		cpunum_set_sp(cpu, val)		cpunum_set_reg(cpu, REG_SP, val)
/*TODO*///
/*TODO*////* this is kind of gross - is it necessary */
/*TODO*///#define 	cpu_geturnpc() 				activecpu_get_reg(REG_SP_CONTENTS)
/*TODO*///
    /* map older cpu_* functions to activecpu_* */
    public static int cpu_get_pc() {
        return activecpu_get_pc();
    }

    /*TODO*///#define		cpu_get_sp					activecpu_get_sp
    public static int cpu_get_reg(int regnum) {
        return activecpu_get_reg(regnum);
    }

    /*TODO*///#define		cpu_set_reg					activecpu_set_reg
    public static int cpu_getpreviouspc() {
        throw new UnsupportedOperationException("Unsupported");
        /*TODO*///    activecpu_get_previouspc
    }

    public static void cpu_set_op_base(int val) {
        activecpu_set_op_base(val);
    }

    public static int cpu_get_pc_byte() {
        return activecpu_get_pc_byte();
    }

    /**
     * ***********************************
     *
     * CPU interface accessors
     *
     ************************************
     */

    /* return a pointer to the interface struct for a given CPU type */
    public static cpu_interface cputype_get_interface(int cputype) {
        return cpuintrf[cputype];
    }

    /* return a the index of the active CPU */
    public static int cpu_getactivecpu() {
        return activecpu;
    }

    /* return a the total number of registered CPUs */
    public static int cpu_gettotalcpu() {
        return totalcpu;
    }
}
