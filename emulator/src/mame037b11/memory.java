/**
 * ported to 037b11
 */
package mame037b11;

import static arcadeflex.fucPtr.*;
import arcadeflex.libc.ptr.UBytePtr;
import static common.libc.cstring.memset;
import static mame.commonH.REGION_CPU1;
import static mame.driverH.MAX_CPU;
import static mame037b11.memoryH.*;
import static mame037b11.cpuintrf.*;
import static old2.mame.mame.*;

public class memory {

    /*TODO*////***************************************************************************
/*TODO*///
/*TODO*///  memory.c
/*TODO*///
/*TODO*///  Functions which handle the CPU memory and I/O port access.
/*TODO*///
/*TODO*///  Caveats:
/*TODO*///
/*TODO*///  * The install_mem/port_*_handler functions are only intended to be
/*TODO*///	called at driver init time. Do not call them after this time.
/*TODO*///
/*TODO*///  * If your driver executes an opcode which crosses a bank-switched
/*TODO*///	boundary, it will pull the wrong data out of memory. Although not
/*TODO*///	a common case, you may need to revert to memcpy to work around this.
/*TODO*///	See machine/tnzs.c for an example.
/*TODO*///
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*///#include "driver.h"
/*TODO*///#include "osd_cpu.h"
/*TODO*///
/*TODO*///#include <stdarg.h>
/*TODO*///
/*TODO*///
/*TODO*/////#define MEM_DUMP
/*TODO*/////#define CHECK_MASKS
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////***************************************************************************
/*TODO*///
/*TODO*///	Basic theory of memory handling:
/*TODO*///
/*TODO*///	An address with up to 32 bits is passed to a memory handler. First,
/*TODO*///	the non-significant bits are removed from the bottom; for example,
/*TODO*///	a 16-bit memory handler doesn't care about the low bit, so that is
/*TODO*///	removed.
/*TODO*///
/*TODO*///	Next, the address is broken into two halves, an upper half and a
/*TODO*///	lower half. The number of bits in each half varies based on the
/*TODO*///	total number of address bits. The upper half is then used as an
/*TODO*///	index into the base_lookup table.
/*TODO*///
/*TODO*///	If the value pulled from the table is within the range 192-255, then
/*TODO*///	the lower half of the address is needed to resolve the final handler.
/*TODO*///	The value from the table (192-255) is combined with the lower address
/*TODO*///	bits to form an index into a subtable.
/*TODO*///
/*TODO*///	Table values in the range 0-31 are reserved for internal handling
/*TODO*///	(such as RAM, ROM, NOP, and banking). Table values between 32 and 192
/*TODO*///	are assigned dynamically at startup.
/*TODO*///
/*TODO*///***************************************************************************/
/*TODO*///
/*TODO*////* macros for the profiler */
/*TODO*///#define MEMREADSTART			profiler_mark(PROFILER_MEMREAD);
/*TODO*///#define MEMREADEND(ret)			{ profiler_mark(PROFILER_END); return ret; }
/*TODO*///#define MEMWRITESTART			profiler_mark(PROFILER_MEMWRITE);
/*TODO*///#define MEMWRITEEND(ret)		{ (ret); profiler_mark(PROFILER_END); return; }
/*TODO*///
    public static int DATABITS_TO_SHIFT(int d) {
        return (((d) == 32) ? 2 : ((d) == 16) ? 1 : 0);
    }

    /* helper macros */
    public static boolean HANDLER_IS_RAM(int h) {
        return ((h) == STATIC_RAM);
    }

    public static boolean HANDLER_IS_ROM(int h) {
        return ((h) == STATIC_ROM);
    }

    public static boolean HANDLER_IS_RAMROM(int h) {
        return ((h) == STATIC_RAMROM);
    }

    /*TODO*///#define HANDLER_IS_NOP(h)		((FPTR)(h) == STATIC_NOP)
    public static boolean HANDLER_IS_BANK(int h) {
        return ((h) >= STATIC_BANK1 && (h) <= STATIC_BANKMAX);
    }

    public static boolean HANDLER_IS_STATIC(int h) {
        return (h < STATIC_COUNT && h != -15000);//special handle for arcadeflex
    }

    /*TODO*///#define HANDLER_TO_BANK(h)		((FPTR)(h))
/*TODO*///#define BANK_TO_HANDLER(b)		((void *)(b))
/*TODO*///

    /*-------------------------------------------------
            TYPE DEFINITIONS
    -------------------------------------------------*/
    public static class bank_data {
        /*TODO*///	UINT8 				used;				/* is this bank used? */
/*TODO*///	UINT8 				cpunum;				/* the CPU it is used for */
/*TODO*///	offs_t 				base;				/* the base offset */
/*TODO*///	offs_t				readoffset;			/* original base offset for reads */
/*TODO*///	offs_t				writeoffset;		/* original base offset for writes */
    }

    public static class handler_data {

        public Object handler;/* function pointer for handler */
        public int offset;/* base offset for handler */

        public static handler_data[] create(int n) {
            handler_data[] a = new handler_data[n];
            for (int k = 0; k < n; k++) {
                a[k] = new handler_data();
            }
            return a;
        }
    }

    public static class table_data {

        UBytePtr table;/* pointer to base of table */
        int /*UINT8*/ subtable_count;/* number of subtables used */
        int /*UINT8*/ subtable_alloc;/* number of subtables allocated */
        handler_data[] handlers;/* pointer to which set of handlers */
    }

    public static class memport_data {

        int cpunum;/* CPU index */
        int abits;/* address bits */
        int dbits;/* data bits */
        int ebits;/* effective address bits */
        int /*offs_t*/ mask;/* address mask */
        table_data read = new table_data();/* memory read lookup table */
        table_data write = new table_data();/* memory write lookup table */
    }

    public static class cpu_data {

        UBytePtr rombase;/* ROM base pointer */
        UBytePtr rambase;/* RAM base pointer */
 /*TODO*///	opbase_handler 		opbase;				/* opcode base handler */
        memport_data mem = new memport_data();/* memory tables */
        memport_data port = new memport_data();/* port tables */
    }

    /*-------------------------------------------------
	GLOBAL VARIABLES
-------------------------------------------------*/
    public static UBytePtr OP_ROM = new UBytePtr();/* opcode ROM base */
    public static UBytePtr OP_RAM = new UBytePtr();/* opcode RAM base */
 /*TODO*///UINT8		 				opcode_entry;					/* opcode readmem entry */
/*TODO*///
/*TODO*///UINT8 *						readmem_lookup;					/* memory read lookup table */
/*TODO*///static UINT8 *				writemem_lookup;				/* memory write lookup table */
/*TODO*///static UINT8 *				readport_lookup;				/* port read lookup table */
/*TODO*///static UINT8 *				writeport_lookup;				/* port write lookup table */
/*TODO*///
/*TODO*///offs_t						memory_amask;					/* memory address mask */
/*TODO*///static offs_t				port_amask;						/* port address mask */
/*TODO*///
    public static UBytePtr[] cpu_bankbase = new UBytePtr[STATIC_COUNT];/* array of bank bases */
    public static ExtMemory[] ext_memory = ExtMemory.create(MAX_EXT_MEMORY);/* externally-allocated memory */
 /*TODO*///
/*TODO*///static opbase_handler		opbasefunc;						/* opcode base override */
/*TODO*///
    public static handler_data[] rmemhandler8 = handler_data.create(ENTRY_COUNT);/* 8-bit memory read handlers */
    public static handler_data[] rmemhandler16 = handler_data.create(ENTRY_COUNT);/* 16-bit memory read handlers */
    public static handler_data[] rmemhandler32 = handler_data.create(ENTRY_COUNT);/* 32-bit memory read handlers */
    public static handler_data[] wmemhandler8 = handler_data.create(ENTRY_COUNT);/* 8-bit memory write handlers */
    public static handler_data[] wmemhandler16 = handler_data.create(ENTRY_COUNT);/* 16-bit memory write handlers */
    public static handler_data[] wmemhandler32 = handler_data.create(ENTRY_COUNT);/* 32-bit memory write handlers */

    public static handler_data[] rporthandler8 = handler_data.create(ENTRY_COUNT);/* 8-bit port read handlers */
    public static handler_data[] rporthandler16 = handler_data.create(ENTRY_COUNT);/* 16-bit port read handlers */
    public static handler_data[] rporthandler32 = handler_data.create(ENTRY_COUNT);/* 32-bit port read handlers */
    public static handler_data[] wporthandler8 = handler_data.create(ENTRY_COUNT);/* 8-bit port write handlers */
    public static handler_data[] wporthandler16 = handler_data.create(ENTRY_COUNT);/* 16-bit port write handlers */
    public static handler_data[] wporthandler32 = handler_data.create(ENTRY_COUNT);/* 32-bit port write handlers */
 /*TODO*///
/*TODO*///static read8_handler 		rmemhandler8s[STATIC_COUNT];	/* copy of 8-bit static read memory handlers */
/*TODO*///static write8_handler 		wmemhandler8s[STATIC_COUNT];	/* copy of 8-bit static write memory handlers */
/*TODO*///
    public static cpu_data[] cpudata = new cpu_data[MAX_CPU];/* data gathered for each CPU */
    public static bank_data[] bankdata = new bank_data[MAX_BANKS];/* data gathered for each bank */
 /*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	PROTOTYPES
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///static int fatalerror(const char *string, ...);
/*TODO*///static UINT8 get_handler_index(struct handler_data *table, void *handler, offs_t start);
/*TODO*///static UINT8 alloc_new_subtable(const struct memport_data *memport, struct table_data *tabledata, UINT8 previous_value);
/*TODO*///static void populate_table(struct memport_data *memport, int iswrite, offs_t start, offs_t stop, UINT8 handler);
/*TODO*///static void *assign_dynamic_bank(int cpu, offs_t start);
/*TODO*///static void install_mem_handler(struct memport_data *memport, int iswrite, offs_t start, offs_t end, void *handler);
/*TODO*///static void install_port_handler(struct memport_data *memport, int iswrite, offs_t start, offs_t end, void *handler);
/*TODO*///static void set_static_handler(int idx,
/*TODO*///		read8_handler r8handler, read16_handler r16handler, read32_handler r32handler,
/*TODO*///		write8_handler w8handler, write16_handler w16handler, write32_handler w32handler);
/*TODO*///static int init_cpudata(void);
/*TODO*///static int init_memport(int cpu, struct memport_data *data, int abits, int dbits, int ismemory);
/*TODO*///static int verify_memory(void);
/*TODO*///static int verify_ports(void);
/*TODO*///static int allocate_memory(void);
/*TODO*///static int populate_memory(void);
/*TODO*///static int populate_ports(void);
/*TODO*///static int address_bits_of_cpu(int cpu);
/*TODO*///static int init_static(void);
/*TODO*///
/*TODO*///#ifdef MEM_DUMP
/*TODO*///static void mem_dump(void);
/*TODO*///#endif
/*TODO*///#ifdef CHECK_MASKS
/*TODO*///static void verify_masks(void);
/*TODO*///#endif
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	memory_init - initialize the memory system
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///int memory_init(void)
/*TODO*///{
/*TODO*///#ifdef CHECK_MASKS
/*TODO*///	verify_masks();
/*TODO*///#endif
/*TODO*///
/*TODO*///	/* init the static handlers */
/*TODO*///	if (!init_static())
/*TODO*///		return 0;
/*TODO*///
/*TODO*///	/* init the CPUs */
/*TODO*///	if (!init_cpudata())
/*TODO*///		return 0;
/*TODO*///
/*TODO*///	/* verify the memory handlers and check banks */
/*TODO*///	if (!verify_memory())
/*TODO*///		return 0;
/*TODO*///	if (!verify_ports())
/*TODO*///		return 0;
/*TODO*///
/*TODO*///	/* allocate memory for sparse address spaces */
/*TODO*///	if (!allocate_memory())
/*TODO*///		return 0;
/*TODO*///
/*TODO*///	/* then fill in the tables */
/*TODO*///	if (!populate_memory())
/*TODO*///		return 0;
/*TODO*///	if (!populate_ports())
/*TODO*///		return 0;
/*TODO*///
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the final memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///
/*TODO*///	return 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	memory_shutdown - free memory
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void memory_shutdown(void)
/*TODO*///{
/*TODO*///	struct ExtMemory *ext;
/*TODO*///	int cpu;
/*TODO*///
/*TODO*///	/* free all the tables */
/*TODO*///	for (cpu = 0; cpu < MAX_CPU; cpu++ )
/*TODO*///	{
/*TODO*///		if (cpudata[cpu].mem.read.table)
/*TODO*///			free(cpudata[cpu].mem.read.table);
/*TODO*///		if (cpudata[cpu].mem.write.table)
/*TODO*///			free(cpudata[cpu].mem.write.table);
/*TODO*///		if (cpudata[cpu].port.read.table)
/*TODO*///			free(cpudata[cpu].port.read.table);
/*TODO*///		if (cpudata[cpu].port.write.table)
/*TODO*///			free(cpudata[cpu].port.write.table);
/*TODO*///	}
/*TODO*///	memset(&cpudata, 0, sizeof(cpudata));
/*TODO*///
/*TODO*///	/* free all the external memory */
/*TODO*///	for (ext = ext_memory; ext->data; ext++)
/*TODO*///		free(ext->data);
/*TODO*///	memset(ext_memory, 0, sizeof(ext_memory));
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	memory_set_opcode_base - set the base of
/*TODO*///	ROM
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void memory_set_opcode_base(int cpu, void *base)
/*TODO*///{
/*TODO*///	cpudata[cpu].rombase = base;
/*TODO*///}
/*TODO*///

    /*-------------------------------------------------
        memory_set_context - set the memory context
    -------------------------------------------------*/

    public static void memory_set_context(int activecpu) {
        throw new UnsupportedOperationException("unsupported");
        /*TODO*///	OP_RAM = cpu_bankbase[STATIC_RAM] = cpudata[activecpu].rambase;
/*TODO*///	OP_ROM = cpudata[activecpu].rombase;
/*TODO*///	opcode_entry = STATIC_ROM;
/*TODO*///
/*TODO*///	readmem_lookup = cpudata[activecpu].mem.read.table;
/*TODO*///	writemem_lookup = cpudata[activecpu].mem.write.table;
/*TODO*///	readport_lookup = cpudata[activecpu].port.read.table;
/*TODO*///	writeport_lookup = cpudata[activecpu].port.write.table;
/*TODO*///
/*TODO*///	memory_amask = cpudata[activecpu].mem.mask;
/*TODO*///	port_amask = cpudata[activecpu].port.mask;
/*TODO*///
/*TODO*///	opbasefunc = cpudata[activecpu].opbase;
    }

    /*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	memory_set_bankhandler_r - set readmemory
/*TODO*///	handler for bank memory (8-bit only!)
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void memory_set_bankhandler_r(int bank, offs_t offset, mem_read_handler handler)
/*TODO*///{
/*TODO*///	/* determine the new offset */
/*TODO*///	if (HANDLER_IS_RAM(handler) || HANDLER_IS_ROM(handler))
/*TODO*///		rmemhandler8[bank].offset = 0 - offset, handler = (mem_read_handler)STATIC_RAM;
/*TODO*///	else if (HANDLER_IS_BANK(handler))
/*TODO*///		rmemhandler8[bank].offset = bankdata[HANDLER_TO_BANK(handler)].readoffset - offset;
/*TODO*///	else
/*TODO*///		rmemhandler8[bank].offset = bankdata[bank].readoffset - offset;
/*TODO*///
/*TODO*///	/* set the new handler */
/*TODO*///	if (HANDLER_IS_STATIC(handler))
/*TODO*///		handler = rmemhandler8s[(FPTR)handler];
/*TODO*///	rmemhandler8[bank].handler = (void *)handler;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	memory_set_bankhandler_w - set writememory
/*TODO*///	handler for bank memory (8-bit only!)
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void memory_set_bankhandler_w(int bank, offs_t offset, mem_write_handler handler)
/*TODO*///{
/*TODO*///	/* determine the new offset */
/*TODO*///	if (HANDLER_IS_RAM(handler) || HANDLER_IS_ROM(handler) || HANDLER_IS_RAMROM(handler))
/*TODO*///		wmemhandler8[bank].offset = 0 - offset;
/*TODO*///	else if (HANDLER_IS_BANK(handler))
/*TODO*///		wmemhandler8[bank].offset = bankdata[HANDLER_TO_BANK(handler)].writeoffset - offset;
/*TODO*///	else
/*TODO*///		wmemhandler8[bank].offset = bankdata[bank].writeoffset - offset;
/*TODO*///
/*TODO*///	/* set the new handler */
/*TODO*///	if (HANDLER_IS_STATIC(handler))
/*TODO*///		handler = wmemhandler8s[(FPTR)handler];
/*TODO*///	wmemhandler8[bank].handler = (void *)handler;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	memory_set_opbase_handler - change op-code
/*TODO*///	memory base
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///opbase_handler memory_set_opbase_handler(int cpu, opbase_handler function)
/*TODO*///{
/*TODO*///	opbase_handler old = cpudata[cpu].opbase;
/*TODO*///	cpudata[cpu].opbase = function;
/*TODO*///	if (cpu == cpu_getactivecpu())
/*TODO*///		opbasefunc = function;
/*TODO*///	return old;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_mem_read_handler - install dynamic
/*TODO*///	read handler for 8-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///data8_t *install_mem_read_handler(int cpu, offs_t start, offs_t end, mem_read_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].mem.dbits != 8)
/*TODO*///	{
/*TODO*///		printf("fatal: install_mem_read_handler called on %d-bit cpu\n",cpudata[cpu].mem.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_mem_handler(&cpudata[cpu].mem, 0, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///	return memory_find_base(cpu, start);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_mem_read16_handler - install dynamic
/*TODO*///	read handler for 16-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///data16_t *install_mem_read16_handler(int cpu, offs_t start, offs_t end, mem_read16_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].mem.dbits != 16)
/*TODO*///	{
/*TODO*///		printf("fatal: install_mem_read16_handler called on %d-bit cpu\n",cpudata[cpu].mem.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_mem_handler(&cpudata[cpu].mem, 0, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///	return memory_find_base(cpu, start);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_mem_read32_handler - install dynamic
/*TODO*///	read handler for 32-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///data32_t *install_mem_read32_handler(int cpu, offs_t start, offs_t end, mem_read32_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].mem.dbits != 32)
/*TODO*///	{
/*TODO*///		printf("fatal: install_mem_read32_handler called on %d-bit cpu\n",cpudata[cpu].mem.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_mem_handler(&cpudata[cpu].mem, 0, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///	return memory_find_base(cpu, start);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_mem_write_handler - install dynamic
/*TODO*///	read handler for 8-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///data8_t *install_mem_write_handler(int cpu, offs_t start, offs_t end, mem_write_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].mem.dbits != 8)
/*TODO*///	{
/*TODO*///		printf("fatal: install_mem_write_handler called on %d-bit cpu\n",cpudata[cpu].mem.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_mem_handler(&cpudata[cpu].mem, 1, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///	return memory_find_base(cpu, start);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_mem_write16_handler - install dynamic
/*TODO*///	read handler for 16-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///data16_t *install_mem_write16_handler(int cpu, offs_t start, offs_t end, mem_write16_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].mem.dbits != 16)
/*TODO*///	{
/*TODO*///		printf("fatal: install_mem_write16_handler called on %d-bit cpu\n",cpudata[cpu].mem.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_mem_handler(&cpudata[cpu].mem, 1, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///	return memory_find_base(cpu, start);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_mem_write32_handler - install dynamic
/*TODO*///	read handler for 32-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///data32_t *install_mem_write32_handler(int cpu, offs_t start, offs_t end, mem_write32_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].mem.dbits != 32)
/*TODO*///	{
/*TODO*///		printf("fatal: install_mem_write32_handler called on %d-bit cpu\n",cpudata[cpu].mem.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_mem_handler(&cpudata[cpu].mem, 1, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///	return memory_find_base(cpu, start);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_port_read_handler - install dynamic
/*TODO*///	read handler for 8-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void install_port_read_handler(int cpu, offs_t start, offs_t end, port_read_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].port.dbits != 8)
/*TODO*///	{
/*TODO*///		printf("fatal: install_port_read_handler called on %d-bit cpu\n",cpudata[cpu].port.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_port_handler(&cpudata[cpu].port, 0, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_port_read16_handler - install dynamic
/*TODO*///	read handler for 16-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void install_port_read16_handler(int cpu, offs_t start, offs_t end, port_read16_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].port.dbits != 16)
/*TODO*///	{
/*TODO*///		printf("fatal: install_port_read16_handler called on %d-bit cpu\n",cpudata[cpu].port.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_port_handler(&cpudata[cpu].port, 0, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_port_read32_handler - install dynamic
/*TODO*///	read handler for 32-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void install_port_read32_handler(int cpu, offs_t start, offs_t end, port_read32_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].port.dbits != 32)
/*TODO*///	{
/*TODO*///		printf("fatal: install_port_read32_handler called on %d-bit cpu\n",cpudata[cpu].port.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_port_handler(&cpudata[cpu].port, 0, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_port_write_handler - install dynamic
/*TODO*///	read handler for 8-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void install_port_write_handler(int cpu, offs_t start, offs_t end, port_write_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].port.dbits != 8)
/*TODO*///	{
/*TODO*///		printf("fatal: install_port_write_handler called on %d-bit cpu\n",cpudata[cpu].port.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_port_handler(&cpudata[cpu].port, 1, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_port_write16_handler - install dynamic
/*TODO*///	read handler for 16-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void install_port_write16_handler(int cpu, offs_t start, offs_t end, port_write16_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].port.dbits != 16)
/*TODO*///	{
/*TODO*///		printf("fatal: install_port_write16_handler called on %d-bit cpu\n",cpudata[cpu].port.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_port_handler(&cpudata[cpu].port, 1, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	install_port_write32_handler - install dynamic
/*TODO*///	read handler for 32-bit case
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void install_port_write32_handler(int cpu, offs_t start, offs_t end, port_write32_handler handler)
/*TODO*///{
/*TODO*///	/* sanity check */
/*TODO*///	if (cpudata[cpu].port.dbits != 32)
/*TODO*///	{
/*TODO*///		printf("fatal: install_port_write32_handler called on %d-bit cpu\n",cpudata[cpu].port.dbits);
/*TODO*///		exit(1);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* install the handler */
/*TODO*///	install_port_handler(&cpudata[cpu].port, 1, start, end, (void *)handler);
/*TODO*///#ifdef MEM_DUMP
/*TODO*///	/* dump the new memory configuration */
/*TODO*///	mem_dump();
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///
/*-------------------------------------------------
            fatalerror - display an error message and
            exit immediately
    -------------------------------------------------*/
    public static int fatalerror(String str, Object... arguments) {
        System.out.println(String.format(str, arguments));
        return 0;
    }

    /*-------------------------------------------------
            memory_find_base - return a pointer to the
            base of RAM associated with the given CPU
            and offset
    -------------------------------------------------*/
    public static UBytePtr memory_find_base(int cpunum, int offset) {

        int region = REGION_CPU1 + cpunum;

        /* look in external memory first */
        for (ExtMemory ext : ext_memory) {
            if (ext.data == null) {
                break;
            }
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///		if (ext->region == region && ext->start <= offset && ext->end >= offset)
/*TODO*///			return (void *)((UINT8 *)ext->data + (offset - ext->start));
        }
        return new UBytePtr(cpudata[cpunum].rambase, offset);
    }

    /*-------------------------------------------------
            get_handler_index - finds the index of a
            handler, or allocates a new one as necessary
    -------------------------------------------------*/
    public static int get_handler_index(handler_data[] table, int handler, Object _handler, int start) {
        int i;

        /* all static handlers are hardcoded */
        if (HANDLER_IS_STATIC(handler)) {
            return handler;
        }

        /* otherwise, we have to search */
        for (i = STATIC_COUNT; i < SUBTABLE_BASE; i++) {
            if (table[i].handler == null) {
                table[i].handler = _handler;
                table[i].offset = start;
            }
            if (table[i].handler == _handler && table[i].offset == start) {
                return i;
            }
        }
        return 0;
    }

    /*-------------------------------------------------
	alloc_new_subtable - allocates more space
	for a new subtable
    -------------------------------------------------*/
    static int/*UINT8*/ alloc_new_subtable(memport_data memport, table_data tabledata, int/*UINT8*/ previous_value) {
        int l1bits = LEVEL1_BITS(memport.ebits);
        int l2bits = LEVEL2_BITS(memport.ebits);

        /* make sure we don't run out */
        if (tabledata.subtable_count + 1 == SUBTABLE_COUNT) {
            fatalerror("error: ran out of memory subtables\n");
        }

        /* allocate more memory if we need to */
        if (tabledata.subtable_count <= tabledata.subtable_alloc) {
            tabledata.subtable_alloc = (tabledata.subtable_alloc + SUBTABLE_ALLOC) & 0xFF;
            //realloc (to be checked)
            UBytePtr temp = new UBytePtr((1 << l1bits) + (tabledata.subtable_alloc << l2bits));//tabledata->table = realloc(tabledata->table, (1 << l1bits) + (tabledata->subtable_alloc << l2bits));
            System.arraycopy(tabledata.table.memory, 0, temp.memory, 0, tabledata.table.memory.length);
            tabledata.table.memory = temp.memory;
            tabledata.table.offset = 0;
        }

        /* initialize the table entries */
        memset(tabledata.table, (1 << l1bits) + (tabledata.subtable_count << l2bits), previous_value & 0xFF, 1 << l2bits);

        /* return the new index */
        return ((SUBTABLE_BASE + (tabledata.subtable_count++)) & 0xFF);
    }


    /*-------------------------------------------------
	populate_table - assign a memory handler to
	a range of addresses
    -------------------------------------------------*/
    public static void populate_table(memport_data memport, int iswrite, int start, int stop, int handler) {
        table_data tabledata = iswrite != 0 ? memport.write : memport.read;
        int minbits = DATABITS_TO_SHIFT(memport.dbits);
        int l1bits = LEVEL1_BITS(memport.ebits);
        int l2bits = LEVEL2_BITS(memport.ebits);
        int l2mask = LEVEL2_MASK(memport.ebits);
        int l1start = start >> (l2bits + minbits);
        int l2start = (start >> minbits) & l2mask;
        int l1stop = stop >> (l2bits + minbits);
        int l2stop = (stop >> minbits) & l2mask;
        int/*UINT8*/ subindex;

        /* sanity check */
        if (start > stop) {
            return;
        }

        /* set the base for non RAM/ROM cases */
        if (handler != STATIC_RAM && handler != STATIC_ROM && handler != STATIC_RAMROM) {
            tabledata.handlers[handler].offset = start;
        }
        /*TODO*///
/*TODO*///	/* remember the base for banks */
/*TODO*///	if (handler >= STATIC_BANK1 && handler <= STATIC_BANKMAX)
/*TODO*///	{
/*TODO*///		if (iswrite)
/*TODO*///			bankdata[handler].writeoffset = start;
/*TODO*///		else
/*TODO*///			bankdata[handler].readoffset = start;
/*TODO*///	}
/*TODO*///
        /* handle the starting edge if it's not on a block boundary */
        if (l2start != 0) {
            /* get the subtable index */
            subindex = tabledata.table.read(l1start);
            if (subindex < SUBTABLE_BASE) {
                tabledata.table.write(l1start, alloc_new_subtable(memport, tabledata, subindex));
                subindex = tabledata.table.read(l1start);
            }
            subindex &= SUBTABLE_MASK;

            /* if the start and stop end within the same block, handle that */
            if (l1start == l1stop) {
                memset(tabledata.table, (1 << l1bits) + (subindex << l2bits) + l2start, handler, l2stop - l2start + 1);
                return;
            }

            /* otherwise, fill until the end */
            memset(tabledata.table, (1 << l1bits) + (subindex << l2bits) + l2start, handler, (1 << l2bits) - l2start);
            if (l1start != Integer.MAX_VALUE) {
                l1start++;
            }
        }

        /* handle the trailing edge if it's not on a block boundary */
        if (l2stop != l2mask) {
            /* get the subtable index */
            subindex = tabledata.table.read(l1stop);
            if (subindex < SUBTABLE_BASE) {
                throw new UnsupportedOperationException("Unsupported");
                /*TODO*///			subindex = tabledata->table[l1stop] = alloc_new_subtable(memport, tabledata, subindex);
            }
            subindex &= SUBTABLE_MASK;

            /* fill from the beginning */
            memset(tabledata.table, (1 << l1bits) + (subindex << l2bits), handler, l2stop + 1);

            /* if the start and stop end within the same block, handle that */
            if (l1start == l1stop) {
                return;
            }
            if (l1stop != 0) {
                l1stop--;
            }
        }

        /* now fill in the middle tables */
        if (l1start <= l1stop) {
            memset(tabledata.table, l1start, handler, l1stop - l1start + 1);
        }
    }

    /*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	assign_dynamic_bank - finds a free or exact
/*TODO*///	matching bank
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///void *assign_dynamic_bank(int cpu, offs_t start)
/*TODO*///{
/*TODO*///	int bank;
/*TODO*///
/*TODO*///	/* special case: never assign a dynamic bank to an offset that */
/*TODO*///	/* intersects the CPU's region; always use RAM for that */
/*TODO*///	if (start < memory_region_length(REGION_CPU1 + cpu))
/*TODO*///		return (void *)STATIC_RAM;
/*TODO*///
/*TODO*///	/* loop over banks, searching for an exact match or an empty */
/*TODO*///	for (bank = 1; bank <= MAX_BANKS; bank++)
/*TODO*///		if (!bankdata[bank].used || (bankdata[bank].cpu == cpu && bankdata[bank].base == start))
/*TODO*///		{
/*TODO*///			bankdata[bank].used = 1;
/*TODO*///			bankdata[bank].cpu = cpu;
/*TODO*///			bankdata[bank].base = start;
/*TODO*///			return BANK_TO_HANDLER(bank);
/*TODO*///		}
/*TODO*///
/*TODO*///	/* if we got here, we failed */
/*TODO*///	fatalerror("cpu #%d: ran out of banks for sparse memory regions!\n", cpu);
/*TODO*///	return NULL;
/*TODO*///}
/*TODO*///
/*TODO*///
    /*-------------------------------------------------
            install_mem_handler - installs a handler for
            memory operatinos
    -------------------------------------------------*/
    public static void install_mem_handler(memport_data memport, int iswrite, int start, int end, int handler, Object _handler) {
        table_data tabledata = iswrite != 0 ? memport.write : memport.read;
        int /*UINT8*/ idx;

        /* translate ROM and RAMROM to RAM here for read cases */
        if (iswrite == 0) {
            if (HANDLER_IS_ROM(handler) || HANDLER_IS_RAMROM(handler)) {
                handler = MRA_RAM;
            }
        }

        /* assign banks for sparse memory spaces */
        if (IS_SPARSE(memport.abits) && HANDLER_IS_RAM(handler)) {
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///		handler = (void *)assign_dynamic_bank(memport->cpunum, start);
        }
        /* set the handler */
        idx = get_handler_index(tabledata.handlers, handler, _handler, start);
        populate_table(memport, iswrite, start, end, idx);
        /*TODO*///
/*TODO*///	/* if this is a bank, set the bankbase as well */
/*TODO*///	if (HANDLER_IS_BANK(handler))
/*TODO*///		cpu_bankbase[HANDLER_TO_BANK(handler)] = memory_find_base(memport->cpunum, start);
    }

    /*-------------------------------------------------
	install_port_handler - installs a handler for
	port operatinos
    -------------------------------------------------*/
    public static void install_port_handler(memport_data memport, int iswrite, int start, int end, int handler, Object _handler) {
        table_data tabledata = iswrite != 0 ? memport.write : memport.read;
        int /*UINT8*/ idx = get_handler_index(tabledata.handlers, handler, _handler, start);
        populate_table(memport, iswrite, start, end, idx);
    }
    /*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	set_static_handler - handy shortcut for
/*TODO*///	setting all 6 handlers for a given index
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///static void set_static_handler(int idx,
/*TODO*///		read8_handler r8handler, read16_handler r16handler, read32_handler r32handler,
/*TODO*///		write8_handler w8handler, write16_handler w16handler, write32_handler w32handler)
/*TODO*///{
/*TODO*///	rmemhandler8s[idx] = r8handler;
/*TODO*///	wmemhandler8s[idx] = w8handler;
/*TODO*///
/*TODO*///	rmemhandler8[idx].handler = (void *)r8handler;
/*TODO*///	rmemhandler16[idx].handler = (void *)r16handler;
/*TODO*///	rmemhandler32[idx].handler = (void *)r32handler;
/*TODO*///	wmemhandler8[idx].handler = (void *)w8handler;
/*TODO*///	wmemhandler16[idx].handler = (void *)w16handler;
/*TODO*///	wmemhandler32[idx].handler = (void *)w32handler;
/*TODO*///
/*TODO*///	rporthandler8[idx].handler = (void *)r8handler;
/*TODO*///	rporthandler16[idx].handler = (void *)r16handler;
/*TODO*///	rporthandler32[idx].handler = (void *)r32handler;
/*TODO*///	wporthandler8[idx].handler = (void *)w8handler;
/*TODO*///	wporthandler16[idx].handler = (void *)w16handler;
/*TODO*///	wporthandler32[idx].handler = (void *)w32handler;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	init_cpudata - initialize the cpudata
/*TODO*///	structure for each CPU
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///static int init_cpudata(void)
/*TODO*///{
/*TODO*///	int cpu;
/*TODO*///
/*TODO*///	/* zap the cpudata structure */
/*TODO*///	memset(&cpudata, 0, sizeof(cpudata));
/*TODO*///
/*TODO*///	/* loop over CPUs */
/*TODO*///	for (cpu = 0; cpu < cpu_gettotalcpu(); cpu++)
/*TODO*///	{
/*TODO*///		/* set the RAM/ROM base */
/*TODO*///		cpudata[cpu].rambase = cpudata[cpu].rombase = memory_region(REGION_CPU1 + cpu);
/*TODO*///		cpudata[cpu].opbase = NULL;
/*TODO*///
/*TODO*///		/* initialize the readmem and writemem tables */
/*TODO*///		if (!init_memport(cpu, &cpudata[cpu].mem, address_bits_of_cpu(cpu), cpunum_databus_width(cpu), 1))
/*TODO*///			return 0;
/*TODO*///
/*TODO*///		/* initialize the readport and writeport tables */
/*TODO*///		if (!init_memport(cpu, &cpudata[cpu].port, PORT_BITS, cpunum_databus_width(cpu), 0))
/*TODO*///			return 0;
/*TODO*///
/*TODO*///#if HAS_Z80
/*TODO*///		/* Z80 port mask kludge */
/*TODO*///		if ((Machine->drv->cpu[cpu].cpu_type & ~CPU_FLAGS_MASK) == CPU_Z80)
/*TODO*///			if (!(Machine->drv->cpu[cpu].cpu_type & CPU_16BIT_PORT))
/*TODO*///				cpudata[cpu].port.mask = 0xff;
/*TODO*///#endif
/*TODO*///	}
/*TODO*///	return 1;
/*TODO*///}
/*TODO*///
/*TODO*///
    /*-------------------------------------------------
	init_memport - initialize the mem/port data
	structure
    -------------------------------------------------*/
    static int init_memport(int cpunum, memport_data data, int abits, int dbits, int ismemory) {
        /* determine the address and data bits */
        data.cpunum = cpunum;
        data.abits = abits;
        data.dbits = dbits;
        data.ebits = abits - DATABITS_TO_SHIFT(dbits);
        data.mask = 0xffffffff >>> (32 - abits);

        /* allocate memory */
        data.read.table = new UBytePtr(1 << LEVEL1_BITS(data.ebits));
        data.write.table = new UBytePtr(1 << LEVEL1_BITS(data.ebits));

        /* initialize everything to unmapped */
        memset(data.read.table, STATIC_UNMAP, 1 << LEVEL1_BITS(data.ebits));
        memset(data.write.table, STATIC_UNMAP, 1 << LEVEL1_BITS(data.ebits));

        /* initialize the pointers to the handlers */
        if (ismemory != 0) {
            data.read.handlers = (dbits == 32) ? rmemhandler32 : (dbits == 16) ? rmemhandler16 : rmemhandler8;
            data.write.handlers = (dbits == 32) ? wmemhandler32 : (dbits == 16) ? wmemhandler16 : wmemhandler8;
        } else {
            data.read.handlers = (dbits == 32) ? rporthandler32 : (dbits == 16) ? rporthandler16 : rporthandler8;
            data.write.handlers = (dbits == 32) ? wporthandler32 : (dbits == 16) ? wporthandler16 : wporthandler8;
        }
        return 1;
    }
    /*-------------------------------------------------
            verify_memory - verify the memory structs
            and track which banks are referenced
    -------------------------------------------------*/
    public static int verify_memory() {
        int cpunum;

        /* zap the bank data */
        for (int i = 0; i < MAX_BANKS; i++) {
            bankdata[i] = new bank_data();//memset(&bankdata, 0, sizeof(bankdata));
        }

        /* loop over CPUs */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {

            int width;
            int bank;

            /* determine the desired width */
            switch (cpunum_databus_width(cpunum)) {
                case 8:
                    width = MEMPORT_WIDTH_8;
                    break;
                case 16:
                    width = MEMPORT_WIDTH_16;
                    break;
                case 32:
                    width = MEMPORT_WIDTH_32;
                    break;
                default:
                    return fatalerror("cpu #%d has invalid memory width!\n", cpunum);
            }
            Object mra_obj = Machine.drv.cpu[cpunum].memory_read;
            Object mwa_obj = Machine.drv.cpu[cpunum].memory_write;
            if (mra_obj instanceof Memory_ReadAddress[]) {
                Memory_ReadAddress[] mra = (Memory_ReadAddress[]) mra_obj;
                int mra_ptr = 0;
                /* verify the read handlers */
                if (mra != null) {
                    /* verify the MEMPORT_READ_START header */
                    if (mra[mra_ptr].start == MEMPORT_MARKER && mra[mra_ptr].end != 0) {
                        if ((mra[mra_ptr].end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_MEM) {
                            return fatalerror("cpu #%d has port handlers in place of memory read handlers!\n", cpunum);
                        }
                        if ((mra[mra_ptr].end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_READ) {
                            return fatalerror("cpu #%d has memory write handlers in place of memory read handlers!\n", cpunum);
                        }
                        if ((mra[mra_ptr].end & MEMPORT_WIDTH_MASK) != width) {
                            return fatalerror("cpu #%d uses wrong data width memory handlers! (width = %d, memory = %08x)\n", cpunum, cpunum_databus_width(cpunum), mra[mra_ptr].end);
                        }
                        mra_ptr++;
                    }
                    /*TODO*///
/*TODO*///			/* track banks used */
/*TODO*///			for ( ; !IS_MEMPORT_END(mra); mra++)
/*TODO*///				if (!IS_MEMPORT_MARKER(mra) && HANDLER_IS_BANK(mra->handler))
/*TODO*///				{
/*TODO*///					bank = HANDLER_TO_BANK(mra->handler);
/*TODO*///					bankdata[bank].used = 1;
/*TODO*///					bankdata[bank].cpunum = -1;
/*TODO*///				}
                }
                /* verify the write handlers */
                Memory_WriteAddress[] mwa = (Memory_WriteAddress[]) mwa_obj;
                int mwa_ptr = 0;
                if (mwa != null) {
                    /* verify the MEMPORT_WRITE_START header */
                    if (mwa[mwa_ptr].start == MEMPORT_MARKER && mwa[mwa_ptr].end != 0) {
                        if ((mwa[mwa_ptr].end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_MEM) {
                            return fatalerror("cpu #%d has port handlers in place of memory write handlers!\n", cpunum);
                        }
                        if ((mwa[mwa_ptr].end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_WRITE) {
                            return fatalerror("cpu #%d has memory read handlers in place of memory write handlers!\n", cpunum);
                        }
                        if ((mwa[mwa_ptr].end & MEMPORT_WIDTH_MASK) != width) {
                            return fatalerror("cpu #%d uses wrong data width memory handlers! (width = %d, memory = %08x)\n", cpunum, cpunum_databus_width(cpunum), mwa[mwa_ptr].end);
                        }
                        mwa_ptr++;
                    }
                    /*TODO*///
/*TODO*///			/* track banks used */
/*TODO*///			for (; !IS_MEMPORT_END(mwa); mwa++)
/*TODO*///				if (!IS_MEMPORT_MARKER(mwa) && HANDLER_IS_BANK(mwa->handler))
/*TODO*///				{
/*TODO*///					bank = HANDLER_TO_BANK(mwa->handler);
/*TODO*///					bankdata[bank].used = 1;
/*TODO*///					bankdata[bank].cpunum = -1;
/*TODO*///				}
/*TODO*///				mwa++;
                }
            } else {
                //do the same for 16,32bit handlers
                throw new UnsupportedOperationException("Unsupported");
            }
            /*TODO*///		const struct Memory_ReadAddress *mra = Machine->drv->cpu[cpunum].memory_read;
/*TODO*///		const struct Memory_WriteAddress *mwa = Machine->drv->cpu[cpunum].memory_write;
/*TODO*///
/*TODO*///		/* verify the read handlers */
/*TODO*///		if (mra)
/*TODO*///		{
/*TODO*///			/* verify the MEMPORT_READ_START header */
/*TODO*///			if (mra->start == MEMPORT_MARKER && mra->end != 0)
/*TODO*///			{
/*TODO*///				if ((mra->end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_MEM)
/*TODO*///					return fatalerror("cpu #%d has port handlers in place of memory read handlers!\n", cpunum);
/*TODO*///				if ((mra->end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_READ)
/*TODO*///					return fatalerror("cpu #%d has memory write handlers in place of memory read handlers!\n", cpunum);
/*TODO*///				if ((mra->end & MEMPORT_WIDTH_MASK) != width)
/*TODO*///					return fatalerror("cpu #%d uses wrong data width memory handlers! (width = %d, memory = %08x)\n", cpunum,cpunum_databus_width(cpunum),mra->end);
/*TODO*///				mra++;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* track banks used */
/*TODO*///			for ( ; !IS_MEMPORT_END(mra); mra++)
/*TODO*///				if (!IS_MEMPORT_MARKER(mra) && HANDLER_IS_BANK(mra->handler))
/*TODO*///				{
/*TODO*///					bank = HANDLER_TO_BANK(mra->handler);
/*TODO*///					bankdata[bank].used = 1;
/*TODO*///					bankdata[bank].cpunum = -1;
/*TODO*///				}
/*TODO*///		}
/*TODO*///
/*TODO*///		/* verify the write handlers */
/*TODO*///		if (mwa)
/*TODO*///		{
/*TODO*///			/* verify the MEMPORT_WRITE_START header */
/*TODO*///			if (mwa->start == MEMPORT_MARKER && mwa->end != 0)
/*TODO*///			{
/*TODO*///				if ((mwa->end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_MEM)
/*TODO*///					return fatalerror("cpu #%d has port handlers in place of memory write handlers!\n", cpunum);
/*TODO*///				if ((mwa->end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_WRITE)
/*TODO*///					return fatalerror("cpu #%d has memory read handlers in place of memory write handlers!\n", cpunum);
/*TODO*///				if ((mwa->end & MEMPORT_WIDTH_MASK) != width)
/*TODO*///					return fatalerror("cpu #%d uses wrong data width memory handlers! (width = %d, memory = %08x)\n", cpunum,cpunum_databus_width(cpunum),mwa->end);
/*TODO*///				mwa++;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* track banks used */
/*TODO*///			for (; !IS_MEMPORT_END(mwa); mwa++)
/*TODO*///				if (!IS_MEMPORT_MARKER(mwa) && HANDLER_IS_BANK(mwa->handler))
/*TODO*///				{
/*TODO*///					bank = HANDLER_TO_BANK(mwa->handler);
/*TODO*///					bankdata[bank].used = 1;
/*TODO*///					bankdata[bank].cpunum = -1;
/*TODO*///				}
/*TODO*///				mwa++;
/*TODO*///		}
        }
        return 1;
    }

    /*-------------------------------------------------
    	verify_ports - verify the port structs
    -------------------------------------------------*/
    static int verify_ports() {
        int cpunum;

        /* loop over CPUs */
        for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++) {
            int/*UINT32*/ width;

            /* determine the desired width */
            switch (cpunum_databus_width(cpunum)) {
                case 8:
                    width = MEMPORT_WIDTH_8;
                    break;
                case 16:
                    width = MEMPORT_WIDTH_16;
                    break;
                case 32:
                    width = MEMPORT_WIDTH_32;
                    break;
                default:
                    return fatalerror("cpu #%d has invalid memory width!\n", cpunum);
            }
            Object mra_obj = Machine.drv.cpu[cpunum].port_read;
            Object mwa_obj = Machine.drv.cpu[cpunum].port_write;
            if (mra_obj instanceof IO_ReadPort[]) {
                IO_ReadPort[] mra = (IO_ReadPort[]) mra_obj;
                int mra_ptr = 0;
                /* verify the read handlers */
                if (mra != null) {
                    /* verify the PORT_READ_START header */
                    if (mra[mra_ptr].start == MEMPORT_MARKER && mra[mra_ptr].end != 0) {
                        if ((mra[mra_ptr].end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_IO) {
                            return fatalerror("cpu #%d has memory handlers in place of I/O read handlers!\n", cpunum);
                        }
                        if ((mra[mra_ptr].end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_READ) {
                            return fatalerror("cpu #%d has port write handlers in place of port read handlers!\n", cpunum);
                        }
                        if ((mra[mra_ptr].end & MEMPORT_WIDTH_MASK) != width) {
                            return fatalerror("cpu #%d uses wrong data width port handlers! (width = %d, memory = %08x)\n", cpunum, cpunum_databus_width(cpunum), mra[mra_ptr].end);
                        }
                    }
                }

                /* verify the write handlers */
                IO_WritePort[] mwa = (IO_WritePort[]) mwa_obj;
                int mwa_ptr = 0;
                if (mwa != null) {
                    /* verify the PORT_WRITE_START header */
                    if (mwa[mwa_ptr].start == MEMPORT_MARKER && mwa[mwa_ptr].end != 0) {
                        if ((mwa[mwa_ptr].end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_IO) {
                            return fatalerror("cpu #%d has memory handlers in place of I/O write handlers!\n", cpunum);
                        }
                        if ((mwa[mwa_ptr].end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_WRITE) {
                            return fatalerror("cpu #%d has port read handlers in place of port write handlers!\n", cpunum);
                        }
                        if ((mwa[mwa_ptr].end & MEMPORT_WIDTH_MASK) != width) {
                            return fatalerror("cpu #%d uses wrong data width port handlers! (width = %d, memory = %08x)\n", cpunum, cpunum_databus_width(cpunum), mwa[mwa_ptr].end);
                        }
                    }
                }
            } else {
                //do the same for 16,32bit handlers
                throw new UnsupportedOperationException("Unsupported");
            }
            /*TODO*///		const struct IO_ReadPort *mra = Machine->drv->cpu[cpunum].port_read;
            /*TODO*///		const struct IO_WritePort *mwa = Machine->drv->cpu[cpunum].port_write;
            /*TODO*///
            /*TODO*///		/* verify the read handlers */
            /*TODO*///		if (mra)
            /*TODO*///		{
            /*TODO*///			/* verify the PORT_READ_START header */
            /*TODO*///			if (mra->start == MEMPORT_MARKER && mra->end != 0)
            /*TODO*///			{
            /*TODO*///				if ((mra->end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_IO)
            /*TODO*///					return fatalerror("cpu #%d has memory handlers in place of I/O read handlers!\n", cpunum);
            /*TODO*///				if ((mra->end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_READ)
            /*TODO*///					return fatalerror("cpu #%d has port write handlers in place of port read handlers!\n", cpunum);
            /*TODO*///				if ((mra->end & MEMPORT_WIDTH_MASK) != width)
            /*TODO*///					return fatalerror("cpu #%d uses wrong data width port handlers! (width = %d, memory = %08x)\n", cpunum,cpunum_databus_width(cpunum),mra->end);
            /*TODO*///			}
            /*TODO*///		}
            /*TODO*///
            /*TODO*///		/* verify the write handlers */
            /*TODO*///		if (mwa)
            /*TODO*///		{
            /*TODO*///			/* verify the PORT_WRITE_START header */
            /*TODO*///			if (mwa->start == MEMPORT_MARKER && mwa->end != 0)
            /*TODO*///			{
            /*TODO*///				if ((mwa->end & MEMPORT_TYPE_MASK) != MEMPORT_TYPE_IO)
            /*TODO*///					return fatalerror("cpu #%d has memory handlers in place of I/O write handlers!\n", cpunum);
            /*TODO*///				if ((mwa->end & MEMPORT_DIRECTION_MASK) != MEMPORT_DIRECTION_WRITE)
            /*TODO*///					return fatalerror("cpu #%d has port read handlers in place of port write handlers!\n", cpunum);
            /*TODO*///				if ((mwa->end & MEMPORT_WIDTH_MASK) != width)
            /*TODO*///					return fatalerror("cpu #%d uses wrong data width port handlers! (width = %d, memory = %08x)\n", cpunum,cpunum_databus_width(cpunum),mwa->end);
            /*TODO*///			}
            /*TODO*///		}
        }
        return 1;
    }
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	needs_ram - returns true if a given type
/*TODO*///	of memory needs RAM backing it
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///static int needs_ram(int cpu, void *handler)
/*TODO*///{
/*TODO*///	/* RAM, ROM, and banks always need RAM */
/*TODO*///	if (HANDLER_IS_RAM(handler) || HANDLER_IS_ROM(handler) || HANDLER_IS_RAMROM(handler) || HANDLER_IS_BANK(handler))
/*TODO*///		return 1;
/*TODO*///
/*TODO*///	/* NOPs never need RAM */
/*TODO*///	else if (HANDLER_IS_NOP(handler))
/*TODO*///		return 0;
/*TODO*///
/*TODO*///	/* otherwise, we only need RAM for sparse memory spaces */
/*TODO*///	else
/*TODO*///		return IS_SPARSE(cpudata[cpu].mem.abits);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	allocate_memory - allocate memory for
/*TODO*///	sparse CPU address spaces
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///static int allocate_memory(void)
/*TODO*///{
/*TODO*///	struct ExtMemory *ext = ext_memory;
/*TODO*///	int cpu;
/*TODO*///
/*TODO*///	/* don't do it for drivers that don't have ROM (MESS needs this) */
/*TODO*///	if (Machine->gamedrv->rom == 0)
/*TODO*///		return 1;
/*TODO*///
/*TODO*///	/* loop over all CPUs */
/*TODO*///	for (cpu = 0; cpu < cpu_gettotalcpu(); cpu++)
/*TODO*///	{
/*TODO*///		int region = REGION_CPU1 + cpu;
/*TODO*///		int region_length = memory_region(region) ? memory_region_length(region) : 0;
/*TODO*///		int size = region_length;
/*TODO*///
/*TODO*///		/* keep going until we break out */
/*TODO*///		while (1)
/*TODO*///		{
/*TODO*///			const struct Memory_ReadAddress *mra = Machine->drv->cpu[cpu].memory_read;
/*TODO*///			const struct Memory_WriteAddress *mwa = Machine->drv->cpu[cpu].memory_write;
/*TODO*///			offs_t lowest = ~0, end, lastend;
/*TODO*///
/*TODO*///			/* find the base of the lowest memory region that extends past the end */
/*TODO*///			for (mra = Machine->drv->cpu[cpu].memory_read; !IS_MEMPORT_END(mra); mra++)
/*TODO*///				if (!IS_MEMPORT_MARKER(mra))
/*TODO*///					if (mra->end >= size && mra->start < lowest && needs_ram(cpu, (void *)mra->handler))
/*TODO*///						lowest = mra->start;
/*TODO*///
/*TODO*///			for (mwa = Machine->drv->cpu[cpu].memory_write; !IS_MEMPORT_END(mwa); mwa++)
/*TODO*///				if (!IS_MEMPORT_MARKER(mwa))
/*TODO*///					if (mwa->end >= size && mwa->start < lowest && (mwa->base || needs_ram(cpu, (void *)mwa->handler)))
/*TODO*///						lowest = mwa->start;
/*TODO*///
/*TODO*///			/* done if nothing found */
/*TODO*///			if (lowest == ~0)
/*TODO*///				break;
/*TODO*///
/*TODO*///			/* now loop until we find the end of this contiguous block of memory */
/*TODO*///			lastend = ~0;
/*TODO*///			end = lowest;
/*TODO*///			while (end != lastend)
/*TODO*///			{
/*TODO*///				lastend = end;
/*TODO*///
/*TODO*///				/* find the end of the contiguous block of memory */
/*TODO*///				for (mra = Machine->drv->cpu[cpu].memory_read; !IS_MEMPORT_END(mra); mra++)
/*TODO*///					if (!IS_MEMPORT_MARKER(mra))
/*TODO*///						if (mra->start <= end+1 && mra->end > end && needs_ram(cpu, (void *)mra->handler))
/*TODO*///							end = mra->end;
/*TODO*///
/*TODO*///				for (mwa = Machine->drv->cpu[cpu].memory_write; !IS_MEMPORT_END(mwa); mwa++)
/*TODO*///					if (!IS_MEMPORT_MARKER(mwa))
/*TODO*///						if (mwa->start <= end+1 && mwa->end > end && (mwa->base || needs_ram(cpu, (void *)mwa->handler)))
/*TODO*///							end = mwa->end;
/*TODO*///			}
/*TODO*///
/*TODO*///			/* fill in the data structure */
/*TODO*///			ext->start = lowest;
/*TODO*///			ext->end = end;
/*TODO*///			ext->region = region;
/*TODO*///
/*TODO*///			/* allocate memory */
/*TODO*///			ext->data = malloc(end+1 - lowest);
/*TODO*///			if (!ext->data)
/*TODO*///				fatalerror("malloc(%d) failed (lowest: %x - end: %x)\n", end + 1 - lowest, lowest, end);
/*TODO*///
/*TODO*///			/* reset the memory */
/*TODO*///			memset(ext->data, 0, end+1 - lowest);
/*TODO*///
/*TODO*///			/* prepare for the next loop */
/*TODO*///			size = ext->end + 1;
/*TODO*///			ext++;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	return 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	populate_memory - populate the memory mapping
/*TODO*///	tables with entries
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///static int populate_memory(void)
/*TODO*///{
/*TODO*///	int cpu;
/*TODO*///
/*TODO*///	/* loop over CPUs */
/*TODO*///	for (cpu = 0; cpu < cpu_gettotalcpu(); cpu++)
/*TODO*///	{
/*TODO*///		const struct Memory_ReadAddress *mra, *mra_start = Machine->drv->cpu[cpu].memory_read;
/*TODO*///		const struct Memory_WriteAddress *mwa, *mwa_start = Machine->drv->cpu[cpu].memory_write;
/*TODO*///
/*TODO*///		/* install the read handlers */
/*TODO*///		if (mra_start)
/*TODO*///		{
/*TODO*///			/* first find the end and check for address bits */
/*TODO*///			for (mra = mra_start; !IS_MEMPORT_END(mra); mra++)
/*TODO*///				if (IS_MEMPORT_MARKER(mra) && (mra->end & MEMPORT_ABITS_MASK))
/*TODO*///					cpudata[cpu].mem.mask = 0xffffffffUL >> (32 - (mra->end & MEMPORT_ABITS_VAL_MASK));
/*TODO*///
/*TODO*///			/* then work backwards */
/*TODO*///			for (mra--; mra >= mra_start; mra--)
/*TODO*///				if (!IS_MEMPORT_MARKER(mra))
/*TODO*///					install_mem_handler(&cpudata[cpu].mem, 0, mra->start, mra->end, (void *)mra->handler);
/*TODO*///		}
/*TODO*///
/*TODO*///		/* install the write handlers */
/*TODO*///		if (mwa_start)
/*TODO*///		{
/*TODO*///			/* first find the end and check for address bits */
/*TODO*///			for (mwa = mwa_start; !IS_MEMPORT_END(mwa); mwa++)
/*TODO*///				if (IS_MEMPORT_MARKER(mwa) && (mwa->end & MEMPORT_ABITS_MASK))
/*TODO*///					cpudata[cpu].mem.mask = 0xffffffffUL >> (32 - (mwa->end & MEMPORT_ABITS_VAL_MASK));
/*TODO*///
/*TODO*///			/* then work backwards */
/*TODO*///			for (mwa--; mwa >= mwa_start; mwa--)
/*TODO*///				if (!IS_MEMPORT_MARKER(mwa))
/*TODO*///				{
/*TODO*///					install_mem_handler(&cpudata[cpu].mem, 1, mwa->start, mwa->end, (void *)mwa->handler);
/*TODO*///					if (mwa->base) *mwa->base = memory_find_base(cpu, mwa->start);
/*TODO*///					if (mwa->size) *mwa->size = mwa->end - mwa->start + 1;
/*TODO*///				}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	return 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	populate_ports - populate the port mapping
/*TODO*///	tables with entries
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///static int populate_ports(void)
/*TODO*///{
/*TODO*///	int cpu;
/*TODO*///
/*TODO*///	/* loop over CPUs */
/*TODO*///	for (cpu = 0; cpu < cpu_gettotalcpu(); cpu++)
/*TODO*///	{
/*TODO*///		const struct IO_ReadPort *mra, *mra_start = Machine->drv->cpu[cpu].port_read;
/*TODO*///		const struct IO_WritePort *mwa, *mwa_start = Machine->drv->cpu[cpu].port_write;
/*TODO*///
/*TODO*///		/* install the read handlers */
/*TODO*///		if (mra_start)
/*TODO*///		{
/*TODO*///			/* first find the end and check for address bits */
/*TODO*///			for (mra = mra_start; !IS_MEMPORT_END(mra); mra++)
/*TODO*///				if (IS_MEMPORT_MARKER(mra) && (mra->end & MEMPORT_ABITS_MASK))
/*TODO*///					cpudata[cpu].port.mask = 0xffffffffUL >> (32 - (mra->end & MEMPORT_ABITS_VAL_MASK));
/*TODO*///
/*TODO*///			/* then work backwards */
/*TODO*///			for (mra--; mra != mra_start; mra--)
/*TODO*///				if (!IS_MEMPORT_MARKER(mra))
/*TODO*///					install_port_handler(&cpudata[cpu].port, 0, mra->start, mra->end, (void *)mra->handler);
/*TODO*///		}
/*TODO*///
/*TODO*///		/* install the write handlers */
/*TODO*///		if (mwa_start)
/*TODO*///		{
/*TODO*///			/* first find the end and check for address bits */
/*TODO*///			for (mwa = mwa_start; !IS_MEMPORT_END(mwa); mwa++)
/*TODO*///				if (IS_MEMPORT_MARKER(mwa) && (mwa->end & MEMPORT_ABITS_MASK))
/*TODO*///					cpudata[cpu].port.mask = 0xffffffffUL >> (32 - (mwa->end & MEMPORT_ABITS_VAL_MASK));
/*TODO*///
/*TODO*///			/* then work backwards */
/*TODO*///			for (mwa--; mwa != mwa_start; mwa--)
/*TODO*///				if (!IS_MEMPORT_MARKER(mwa))
/*TODO*///					install_port_handler(&cpudata[cpu].port, 1, mwa->start, mwa->end, (void *)mwa->handler);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	return 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	READBYTE - generic byte-sized read handler
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///#define READBYTE8(name,abits,lookup,handlist,mask)										\
/*TODO*///data8_t name(offs_t address)															\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMREADSTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,0)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,0)];							\
/*TODO*///																						\
/*TODO*///	/* for compatibility with setbankhandler, 8-bit systems */							\
/*TODO*///	/* must call handlers for banks */													\
/*TODO*///	if (entry == STATIC_RAM)															\
/*TODO*///		MEMREADEND(cpu_bankbase[STATIC_RAM][address])									\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		read8_handler handler = (read8_handler)handlist[entry].handler;					\
/*TODO*///		MEMREADEND((*handler)(address - handlist[entry].offset))						\
/*TODO*///	}																					\
/*TODO*///	return 0;																			\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define READBYTE16BE(name,abits,lookup,handlist,mask)									\
/*TODO*///data8_t name(offs_t address)															\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMREADSTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMREADEND(cpu_bankbase[entry][BYTE_XOR_BE(address)])							\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (~address & 1);													\
/*TODO*///		read16_handler handler = (read16_handler)handlist[entry].handler;				\
/*TODO*///		MEMREADEND((*handler)(address >> 1) >> shift)									\
/*TODO*///	}																					\
/*TODO*///	return 0;																			\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define READBYTE16LE(name,abits,lookup,handlist,mask)									\
/*TODO*///data8_t name(offs_t address)															\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMREADSTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMREADEND(cpu_bankbase[entry][BYTE_XOR_LE(address)])							\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (address & 1);													\
/*TODO*///		read16_handler handler = (read16_handler)handlist[entry].handler;				\
/*TODO*///		MEMREADEND((*handler)(address >> 1) >> shift)									\
/*TODO*///	}																					\
/*TODO*///	return 0;																			\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define READBYTE32BE(name,abits,lookup,handlist,mask)									\
/*TODO*///data8_t name(offs_t address)															\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMREADSTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMREADEND(cpu_bankbase[entry][BYTE4_XOR_BE(address)])							\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (~address & 3);													\
/*TODO*///		read32_handler handler = (read32_handler)handlist[entry].handler;				\
/*TODO*///		MEMREADEND((*handler)(address >> 2) >> shift) 									\
/*TODO*///	}																					\
/*TODO*///	return 0;																			\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define READBYTE32LE(name,abits,lookup,handlist,mask)									\
/*TODO*///data8_t name(offs_t address)															\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMREADSTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMREADEND(cpu_bankbase[entry][BYTE4_XOR_LE(address)])							\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (address & 3);													\
/*TODO*///		read32_handler handler = (read32_handler)handlist[entry].handler;				\
/*TODO*///		MEMREADEND((*handler)(address >> 2) >> shift) 									\
/*TODO*///	}																					\
/*TODO*///	return 0;																			\
/*TODO*///}																						\
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	READWORD - generic word-sized read handler
/*TODO*///	(16-bit and 32-bit aligned only!)
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///#define READWORD16(name,abits,lookup,handlist,mask)										\
/*TODO*///data16_t name(offs_t address)															\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMREADSTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMREADEND(*(data16_t *)&cpu_bankbase[entry][address])							\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		read16_handler handler = (read16_handler)handlist[entry].handler;				\
/*TODO*///		MEMREADEND((*handler)(address >> 1))										 	\
/*TODO*///	}																					\
/*TODO*///	return 0;																			\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define READWORD32BE(name,abits,lookup,handlist,mask)									\
/*TODO*///data16_t name(offs_t address)															\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMREADSTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMREADEND(*(data16_t *)&cpu_bankbase[entry][WORD_XOR_BE(address)])				\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (~address & 2);													\
/*TODO*///		read32_handler handler = (read32_handler)handlist[entry].handler;				\
/*TODO*///		MEMREADEND((*handler)(address >> 2) >> shift)									\
/*TODO*///	}																					\
/*TODO*///	return 0;																			\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define READWORD32LE(name,abits,lookup,handlist,mask)									\
/*TODO*///data16_t name(offs_t address)															\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMREADSTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMREADEND(*(data16_t *)&cpu_bankbase[entry][WORD_XOR_LE(address)])				\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (address & 2);													\
/*TODO*///		read32_handler handler = (read32_handler)handlist[entry].handler;				\
/*TODO*///		MEMREADEND((*handler)(address >> 2) >> shift)									\
/*TODO*///	}																					\
/*TODO*///	return 0;																			\
/*TODO*///}																						\
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	READLONG - generic dword-sized read handler
/*TODO*///	(32-bit aligned only!)
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///#define READLONG32(name,abits,lookup,handlist,mask)										\
/*TODO*///data32_t name(offs_t address)															\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMREADSTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMREADEND(*(data32_t *)&cpu_bankbase[entry][address])							\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		read32_handler handler = (read32_handler)handlist[entry].handler;				\
/*TODO*///		MEMREADEND((*handler)(address >> 2))										 	\
/*TODO*///	}																					\
/*TODO*///	return 0;																			\
/*TODO*///}																						\
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	WRITEBYTE - generic byte-sized write handler
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///#define WRITEBYTE8(name,abits,lookup,handlist,mask)										\
/*TODO*///void name(offs_t address, data8_t data)													\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMWRITESTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,0)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,0)];							\
/*TODO*///																						\
/*TODO*///	/* for compatibility with setbankhandler, 8-bit systems */							\
/*TODO*///	/* must call handlers for banks */													\
/*TODO*///	if (entry == (FPTR)MRA_RAM)															\
/*TODO*///		MEMWRITEEND(cpu_bankbase[STATIC_RAM][address] = data)							\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		write8_handler handler = (write8_handler)handlist[entry].handler;				\
/*TODO*///		MEMWRITEEND((*handler)(address - handlist[entry].offset, data))					\
/*TODO*///	}																					\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define WRITEBYTE16BE(name,abits,lookup,handlist,mask)									\
/*TODO*///void name(offs_t address, data8_t data)													\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMWRITESTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMWRITEEND(cpu_bankbase[entry][BYTE_XOR_BE(address)] = data)					\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (~address & 1);													\
/*TODO*///		write16_handler handler = (write16_handler)handlist[entry].handler;				\
/*TODO*///		MEMWRITEEND((*handler)(address >> 1, data << shift, ~(0xff << shift))) 			\
/*TODO*///	}																					\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define WRITEBYTE16LE(name,abits,lookup,handlist,mask)									\
/*TODO*///void name(offs_t address, data8_t data)													\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMWRITESTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMWRITEEND(cpu_bankbase[entry][BYTE_XOR_LE(address)] = data)					\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (address & 1);													\
/*TODO*///		write16_handler handler = (write16_handler)handlist[entry].handler;				\
/*TODO*///		MEMWRITEEND((*handler)(address >> 1, data << shift, ~(0xff << shift)))			\
/*TODO*///	}																					\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define WRITEBYTE32BE(name,abits,lookup,handlist,mask)									\
/*TODO*///void name(offs_t address, data8_t data)													\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMWRITESTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMWRITEEND(cpu_bankbase[entry][BYTE4_XOR_BE(address)] = data)					\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (~address & 3);													\
/*TODO*///		write32_handler handler = (write32_handler)handlist[entry].handler;				\
/*TODO*///		MEMWRITEEND((*handler)(address >> 2, data << shift, ~(0xff << shift))) 			\
/*TODO*///	}																					\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define WRITEBYTE32LE(name,abits,lookup,handlist,mask)									\
/*TODO*///void name(offs_t address, data8_t data)													\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMWRITESTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMWRITEEND(cpu_bankbase[entry][BYTE4_XOR_LE(address)] = data)					\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (address & 3);													\
/*TODO*///		write32_handler handler = (write32_handler)handlist[entry].handler;				\
/*TODO*///		MEMWRITEEND((*handler)(address >> 2, data << shift, ~(0xff << shift))) 			\
/*TODO*///	}																					\
/*TODO*///}																						\
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	WRITEWORD - generic word-sized write handler
/*TODO*///	(16-bit and 32-bit aligned only!)
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///#define WRITEWORD16(name,abits,lookup,handlist,mask)									\
/*TODO*///void name(offs_t address, data16_t data)												\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMWRITESTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,1)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,1)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMWRITEEND(*(data16_t *)&cpu_bankbase[entry][address] = data)					\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		write16_handler handler = (write16_handler)handlist[entry].handler;				\
/*TODO*///		MEMWRITEEND((*handler)(address >> 1, data, 0))								 	\
/*TODO*///	}																					\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define WRITEWORD32BE(name,abits,lookup,handlist,mask)									\
/*TODO*///void name(offs_t address, data16_t data)												\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMWRITESTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMWRITEEND(*(data16_t *)&cpu_bankbase[entry][WORD_XOR_BE(address)] = data)		\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (~address & 2);													\
/*TODO*///		write32_handler handler = (write32_handler)handlist[entry].handler;				\
/*TODO*///		MEMWRITEEND((*handler)(address >> 2, data << shift, ~(0xffff << shift))) 		\
/*TODO*///	}																					\
/*TODO*///}																						\
/*TODO*///
/*TODO*///#define WRITEWORD32LE(name,abits,lookup,handlist,mask)									\
/*TODO*///void name(offs_t address, data16_t data)												\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMWRITESTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMWRITEEND(*(data16_t *)&cpu_bankbase[entry][WORD_XOR_LE(address)] = data)		\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		int shift = 8 * (address & 2);													\
/*TODO*///		write32_handler handler = (write32_handler)handlist[entry].handler;				\
/*TODO*///		MEMWRITEEND((*handler)(address >> 2, data << shift, ~(0xffff << shift))) 		\
/*TODO*///	}																					\
/*TODO*///}																						\
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	WRITELONG - dword-sized write handler
/*TODO*///	(32-bit aligned only!)
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///#define WRITELONG32(name,abits,lookup,handlist,mask)									\
/*TODO*///void name(offs_t address, data32_t data)												\
/*TODO*///{																						\
/*TODO*///	UINT8 entry;																		\
/*TODO*///	MEMWRITESTART																		\
/*TODO*///																						\
/*TODO*///	/* perform lookup */																\
/*TODO*///	address &= mask;																	\
/*TODO*///	entry = lookup[LEVEL1_INDEX(address,abits,2)];										\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = lookup[LEVEL2_INDEX(entry,address,abits,2)];							\
/*TODO*///																						\
/*TODO*///	/* handle banks inline */															\
/*TODO*///	address -= handlist[entry].offset;													\
/*TODO*///	if (entry <= STATIC_RAM)															\
/*TODO*///		MEMWRITEEND(*(data32_t *)&cpu_bankbase[entry][address] = data)					\
/*TODO*///																						\
/*TODO*///	/* fall back to the handler */														\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		write32_handler handler = (write32_handler)handlist[entry].handler;				\
/*TODO*///		MEMWRITEEND((*handler)(address >> 2, data, 0))								 	\
/*TODO*///	}																					\
/*TODO*///}																						\
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	SETOPBASE - generic opcode base changer
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///#define SETOPBASE(name,abits,minbits,table)												\
/*TODO*///void name(offs_t pc)																	\
/*TODO*///{																						\
/*TODO*///	UINT8 *base;																		\
/*TODO*///	UINT8 entry;																		\
/*TODO*///																						\
/*TODO*///	/* allow overrides */																\
/*TODO*///	if (opbasefunc) 																	\
/*TODO*///	{																					\
/*TODO*///		pc = (*opbasefunc)(pc);															\
/*TODO*///		if (pc == ~0)																	\
/*TODO*///			return; 																	\
/*TODO*///	}																					\
/*TODO*///																						\
/*TODO*///	/* perform the lookup */															\
/*TODO*///	pc &= memory_amask;																	\
/*TODO*///	entry = readmem_lookup[LEVEL1_INDEX(pc,abits,minbits)];								\
/*TODO*///	if (entry >= SUBTABLE_BASE)															\
/*TODO*///		entry = readmem_lookup[LEVEL2_INDEX(entry,pc,abits,minbits)];					\
/*TODO*///	opcode_entry = entry;																\
/*TODO*///																						\
/*TODO*///	/* RAM/ROM/RAMROM */																\
/*TODO*///	if (entry >= STATIC_RAM && entry <= STATIC_RAMROM)									\
/*TODO*///		base = cpu_bankbase[STATIC_RAM];												\
/*TODO*///																						\
/*TODO*///	/* banked memory */																	\
/*TODO*///	else if (entry >= STATIC_BANK1 && entry <= STATIC_RAM)								\
/*TODO*///		base = cpu_bankbase[entry];														\
/*TODO*///																						\
/*TODO*///	/* other memory -- could be very slow! */											\
/*TODO*///	else																				\
/*TODO*///	{																					\
/*TODO*///		logerror("cpu #%d (PC=%08X): warning - op-code execute on mapped I/O\n",		\
/*TODO*///					cpu_getactivecpu(),cpu_get_pc());									\
/*TODO*///		/*base = memory_find_base(cpu_getactivecpu(), pc);*/							\
/*TODO*///		return;																			\
/*TODO*///	}																					\
/*TODO*///																						\
/*TODO*///	/* compute the adjusted base */														\
/*TODO*///	OP_ROM = base - table[entry].offset + (OP_ROM - OP_RAM);							\
/*TODO*///	OP_RAM = base - table[entry].offset;												\
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	GENERATE_HANDLERS - macros to spew out all
/*TODO*///	the handlers needed for a given memory type
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///#define GENERATE_HANDLERS_8BIT(abits) \
/*TODO*///	    READBYTE8(cpu_readmem##abits,             abits, readmem_lookup,  rmemhandler8,  memory_amask) \
/*TODO*///	   WRITEBYTE8(cpu_writemem##abits,            abits, writemem_lookup, wmemhandler8,  memory_amask) \
/*TODO*///	    SETOPBASE(cpu_setopbase##abits,           abits, 0, rmemhandler8)
/*TODO*///
/*TODO*///#define GENERATE_HANDLERS_16BIT_BE(abits) \
/*TODO*///	 READBYTE16BE(cpu_readmem##abits##bew,        abits, readmem_lookup,  rmemhandler16, memory_amask) \
/*TODO*///	   READWORD16(cpu_readmem##abits##bew_word,   abits, readmem_lookup,  rmemhandler16, memory_amask) \
/*TODO*///	WRITEBYTE16BE(cpu_writemem##abits##bew,       abits, writemem_lookup, wmemhandler16, memory_amask) \
/*TODO*///	  WRITEWORD16(cpu_writemem##abits##bew_word,  abits, writemem_lookup, wmemhandler16, memory_amask) \
/*TODO*///	    SETOPBASE(cpu_setopbase##abits##bew,      abits, 1, rmemhandler16)
/*TODO*///
/*TODO*///#define GENERATE_HANDLERS_16BIT_LE(abits) \
/*TODO*///	 READBYTE16LE(cpu_readmem##abits##lew,        abits, readmem_lookup,  rmemhandler16, memory_amask) \
/*TODO*///	   READWORD16(cpu_readmem##abits##lew_word,   abits, readmem_lookup,  rmemhandler16, memory_amask) \
/*TODO*///	WRITEBYTE16LE(cpu_writemem##abits##lew,       abits, writemem_lookup, wmemhandler16, memory_amask) \
/*TODO*///	  WRITEWORD16(cpu_writemem##abits##lew_word,  abits, writemem_lookup, wmemhandler16, memory_amask) \
/*TODO*///	    SETOPBASE(cpu_setopbase##abits##lew,      abits, 1, rmemhandler16)
/*TODO*///
/*TODO*///#define GENERATE_HANDLERS_32BIT_BE(abits) \
/*TODO*///	 READBYTE32BE(cpu_readmem##abits##bedw,       abits, readmem_lookup,  rmemhandler32, memory_amask) \
/*TODO*///	 READWORD32BE(cpu_readmem##abits##bedw_word,  abits, readmem_lookup,  rmemhandler32, memory_amask) \
/*TODO*///	   READLONG32(cpu_readmem##abits##bedw_dword, abits, readmem_lookup,  rmemhandler32, memory_amask) \
/*TODO*///	WRITEBYTE32BE(cpu_writemem##abits##bedw,      abits, writemem_lookup, wmemhandler32, memory_amask) \
/*TODO*///	WRITEWORD32BE(cpu_writemem##abits##bedw_word, abits, writemem_lookup, wmemhandler32, memory_amask) \
/*TODO*///	  WRITELONG32(cpu_writemem##abits##bedw_dword,abits, writemem_lookup, wmemhandler32, memory_amask) \
/*TODO*///	    SETOPBASE(cpu_setopbase##abits##bedw,     abits, 2, rmemhandler32)
/*TODO*///
/*TODO*///#define GENERATE_HANDLERS_32BIT_LE(abits) \
/*TODO*///	 READBYTE32LE(cpu_readmem##abits##ledw,       abits, readmem_lookup,  rmemhandler32, memory_amask) \
/*TODO*///	 READWORD32LE(cpu_readmem##abits##ledw_word,  abits, readmem_lookup,  rmemhandler32, memory_amask) \
/*TODO*///	   READLONG32(cpu_readmem##abits##ledw_dword, abits, readmem_lookup,  rmemhandler32, memory_amask) \
/*TODO*///	WRITEBYTE32LE(cpu_writemem##abits##ledw,      abits, writemem_lookup, wmemhandler32, memory_amask) \
/*TODO*///	WRITEWORD32LE(cpu_writemem##abits##ledw_word, abits, writemem_lookup, wmemhandler32, memory_amask) \
/*TODO*///	  WRITELONG32(cpu_writemem##abits##ledw_dword,abits, writemem_lookup, wmemhandler32, memory_amask) \
/*TODO*///	    SETOPBASE(cpu_setopbase##abits##ledw,     abits, 2, rmemhandler32)
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	the memory handlers we need to generate
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///GENERATE_HANDLERS_8BIT(16)
/*TODO*///GENERATE_HANDLERS_8BIT(20)
/*TODO*///GENERATE_HANDLERS_8BIT(21)
/*TODO*///GENERATE_HANDLERS_8BIT(24)
/*TODO*///
/*TODO*///GENERATE_HANDLERS_16BIT_BE(16)
/*TODO*///GENERATE_HANDLERS_16BIT_BE(24)
/*TODO*///GENERATE_HANDLERS_16BIT_BE(32)
/*TODO*///
/*TODO*///GENERATE_HANDLERS_16BIT_LE(16)
/*TODO*///GENERATE_HANDLERS_16BIT_LE(17)
/*TODO*///GENERATE_HANDLERS_16BIT_LE(29)
/*TODO*///GENERATE_HANDLERS_16BIT_LE(32)
/*TODO*///
/*TODO*///GENERATE_HANDLERS_32BIT_BE(24)
/*TODO*///GENERATE_HANDLERS_32BIT_BE(29)
/*TODO*///GENERATE_HANDLERS_32BIT_BE(32)
/*TODO*///
/*TODO*///GENERATE_HANDLERS_32BIT_LE(26)
/*TODO*///GENERATE_HANDLERS_32BIT_LE(29)
/*TODO*///GENERATE_HANDLERS_32BIT_LE(32)
/*TODO*///
/*TODO*///GENERATE_HANDLERS_32BIT_BE(18)	/* HACK -- used for pdp-1 */
/*TODO*///
/*TODO*////* make sure you add an entry to this list whenever you add a set of handlers */
/*TODO*///static const struct memory_address_table readmem_to_bits[] =
/*TODO*///{
/*TODO*///	{ 16, cpu_readmem16 },
/*TODO*///	{ 20, cpu_readmem20 },
/*TODO*///	{ 21, cpu_readmem21 },
/*TODO*///	{ 24, cpu_readmem24 },
/*TODO*///
/*TODO*///	{ 16, cpu_readmem16bew },
/*TODO*///	{ 24, cpu_readmem24bew },
/*TODO*///	{ 32, cpu_readmem32bew },
/*TODO*///
/*TODO*///	{ 16, cpu_readmem16lew },
/*TODO*///	{ 17, cpu_readmem17lew },
/*TODO*///	{ 29, cpu_readmem29lew },
/*TODO*///	{ 32, cpu_readmem32lew },
/*TODO*///
/*TODO*///	{ 24, cpu_readmem24bedw },
/*TODO*///	{ 29, cpu_readmem29bedw },
/*TODO*///	{ 32, cpu_readmem32bedw },
/*TODO*///
/*TODO*///	{ 26, cpu_readmem26ledw },
/*TODO*///	{ 29, cpu_readmem29ledw },
/*TODO*///	{ 32, cpu_readmem32ledw },
/*TODO*///
/*TODO*///	{ 18, cpu_readmem18bedw }
/*TODO*///};
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	the port handlers we need to generate
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///READBYTE8    (cpu_readport16,            16, readport_lookup,  rporthandler8,  port_amask)
/*TODO*///WRITEBYTE8   (cpu_writeport16,           16, writeport_lookup, wporthandler8,  port_amask)
/*TODO*///
/*TODO*///READBYTE16BE (cpu_readport16bew,         16, readport_lookup,  rporthandler16, port_amask)
/*TODO*///READWORD16   (cpu_readport16bew_word,    16, readport_lookup,  rporthandler16, port_amask)
/*TODO*///WRITEBYTE16BE(cpu_writeport16bew,        16, writeport_lookup, wporthandler16, port_amask)
/*TODO*///WRITEWORD16  (cpu_writeport16bew_word,   16, writeport_lookup, wporthandler16, port_amask)
/*TODO*///
/*TODO*///READBYTE16LE (cpu_readport16lew,         16, readport_lookup,  rporthandler16, port_amask)
/*TODO*///READWORD16   (cpu_readport16lew_word,    16, readport_lookup,  rporthandler16, port_amask)
/*TODO*///WRITEBYTE16LE(cpu_writeport16lew,        16, writeport_lookup, wporthandler16, port_amask)
/*TODO*///WRITEWORD16  (cpu_writeport16lew_word,   16, writeport_lookup, wporthandler16, port_amask)
/*TODO*///
/*TODO*///READBYTE32BE (cpu_readport16bedw,        16, readport_lookup,  rporthandler32, port_amask)
/*TODO*///READWORD32BE (cpu_readport16bedw_word,   16, readport_lookup,  rporthandler32, port_amask)
/*TODO*///READLONG32   (cpu_readport16bedw_dword,  16, readport_lookup,  rporthandler32, port_amask)
/*TODO*///WRITEBYTE32BE(cpu_writeport16bedw,       16, writeport_lookup, wporthandler32, port_amask)
/*TODO*///WRITEWORD32BE(cpu_writeport16bedw_word,  16, writeport_lookup, wporthandler32, port_amask)
/*TODO*///WRITELONG32  (cpu_writeport16bedw_dword, 16, writeport_lookup, wporthandler32, port_amask)
/*TODO*///
/*TODO*///READBYTE32LE (cpu_readport16ledw,        16, readport_lookup,  rporthandler32, port_amask)
/*TODO*///READWORD32LE (cpu_readport16ledw_word,   16, readport_lookup,  rporthandler32, port_amask)
/*TODO*///READLONG32   (cpu_readport16ledw_dword,  16, readport_lookup,  rporthandler32, port_amask)
/*TODO*///WRITEBYTE32LE(cpu_writeport16ledw,       16, writeport_lookup, wporthandler32, port_amask)
/*TODO*///WRITEWORD32LE(cpu_writeport16ledw_word,  16, writeport_lookup, wporthandler32, port_amask)
/*TODO*///WRITELONG32  (cpu_writeport16ledw_dword, 16, writeport_lookup, wporthandler32, port_amask)
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	get address bits from a read handler
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///int address_bits_of_cpu(int cpu)
/*TODO*///{
/*TODO*///	read8_handler handler = cpuintf[Machine->drv->cpu[cpu].cpu_type & ~CPU_FLAGS_MASK].memory_read;
/*TODO*///	int	idx;
/*TODO*///
/*TODO*///	/* scan the table */
/*TODO*///	for (idx = 0; idx < sizeof(readmem_to_bits) / sizeof(readmem_to_bits[0]); idx++)
/*TODO*///		if (readmem_to_bits[idx].handler == handler)
/*TODO*///			return readmem_to_bits[idx].bits;
/*TODO*///
/*TODO*///	/* this is a fatal error */
/*TODO*///	fatalerror("CPU #%d memory handlers don't have a table entry in readmem_to_bits!\n");
/*TODO*///	exit(1);
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	basic static handlers
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///static READ_HANDLER( mrh8_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped memory byte read from %08X\n", cpu_getactivecpu(), cpu_get_pc(), offset);
/*TODO*///	if (cpu_address_bits() <= SPARSE_THRESH) return cpu_bankbase[STATIC_RAM][offset];
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///static READ16_HANDLER( mrh16_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped memory word read from %08X\n", cpu_getactivecpu(), cpu_get_pc(), offset*2);
/*TODO*///	if (cpu_address_bits() <= SPARSE_THRESH) return ((data16_t *)cpu_bankbase[STATIC_RAM])[offset];
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///static READ32_HANDLER( mrh32_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped memory dword read from %08X\n", cpu_getactivecpu(), cpu_get_pc(), offset*4);
/*TODO*///	if (cpu_address_bits() <= SPARSE_THRESH) return ((data32_t *)cpu_bankbase[STATIC_RAM])[offset];
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///static WRITE_HANDLER( mwh8_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped memory byte write to %08X = %02X\n", cpu_getactivecpu(), cpu_get_pc(), offset, data);
/*TODO*///	if (cpu_address_bits() <= SPARSE_THRESH) cpu_bankbase[STATIC_RAM][offset] = data;
/*TODO*///}
/*TODO*///static WRITE16_HANDLER( mwh16_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped memory word write to %08X = %04X & %04X\n", cpu_getactivecpu(), cpu_get_pc(), offset*2, data, mem_mask ^ 0xffff);
/*TODO*///	if (cpu_address_bits() <= SPARSE_THRESH) COMBINE_DATA(&((data16_t *)cpu_bankbase[STATIC_RAM])[offset]);
/*TODO*///}
/*TODO*///static WRITE32_HANDLER( mwh32_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped memory dword write to %08X = %08X & %08X\n", cpu_getactivecpu(), cpu_get_pc(), offset*4, data, mem_mask ^ 0xffffffff);
/*TODO*///	if (cpu_address_bits() <= SPARSE_THRESH) COMBINE_DATA(&((data32_t *)cpu_bankbase[STATIC_RAM])[offset]);
/*TODO*///}
/*TODO*///
/*TODO*///static READ_HANDLER( prh8_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped port byte read from %08X\n", cpu_getactivecpu(), cpu_get_pc(), offset);
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///static READ16_HANDLER( prh16_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped port word read from %08X\n", cpu_getactivecpu(), cpu_get_pc(), offset*2);
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///static READ32_HANDLER( prh32_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped port dword read from %08X\n", cpu_getactivecpu(), cpu_get_pc(), offset*4);
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///static WRITE_HANDLER( pwh8_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped port byte write to %08X = %02X\n", cpu_getactivecpu(), cpu_get_pc(), offset, data);
/*TODO*///}
/*TODO*///static WRITE16_HANDLER( pwh16_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped port word write to %08X = %04X & %04X\n", cpu_getactivecpu(), cpu_get_pc(), offset*2, data, mem_mask ^ 0xffff);
/*TODO*///}
/*TODO*///static WRITE32_HANDLER( pwh32_bad )
/*TODO*///{
/*TODO*///	logerror("cpu #%d (PC=%08X): unmapped port dword write to %08X = %08X & %08X\n", cpu_getactivecpu(), cpu_get_pc(), offset*4, data, mem_mask ^ 0xffffffff);
/*TODO*///}
/*TODO*///
/*TODO*///static WRITE_HANDLER( mwh8_rom )       { logerror("cpu #%d (PC=%08X): byte write to ROM %08X = %02X\n", cpu_getactivecpu(), cpu_get_pc(), offset, data); }
/*TODO*///static WRITE16_HANDLER( mwh16_rom )    { logerror("cpu #%d (PC=%08X): word write to %08X = %04X & %04X\n", cpu_getactivecpu(), cpu_get_pc(), offset*2, data, mem_mask ^ 0xffff); }
/*TODO*///static WRITE32_HANDLER( mwh32_rom )    { logerror("cpu #%d (PC=%08X): dword write to %08X = %08X & %08X\n", cpu_getactivecpu(), cpu_get_pc(), offset*4, data, mem_mask ^ 0xffffffff); }
/*TODO*///
/*TODO*///static READ_HANDLER( mrh8_nop )        { return 0; }
/*TODO*///static READ16_HANDLER( mrh16_nop )     { return 0; }
/*TODO*///static READ32_HANDLER( mrh32_nop )     { return 0; }
/*TODO*///
/*TODO*///static WRITE_HANDLER( mwh8_nop )       {  }
/*TODO*///static WRITE16_HANDLER( mwh16_nop )    {  }
/*TODO*///static WRITE32_HANDLER( mwh32_nop )    {  }
/*TODO*///
/*TODO*///static READ_HANDLER( mrh8_ram )        { return cpu_bankbase[STATIC_RAM][offset]; }
/*TODO*///static WRITE_HANDLER( mwh8_ram )       { cpu_bankbase[STATIC_RAM][offset] = data; }
/*TODO*///
/*TODO*///static WRITE_HANDLER( mwh8_ramrom )    { cpu_bankbase[STATIC_RAM][offset] = cpu_bankbase[STATIC_RAM][offset + (OP_ROM - OP_RAM)] = data; }
/*TODO*///static WRITE16_HANDLER( mwh16_ramrom ) { COMBINE_DATA(&cpu_bankbase[STATIC_RAM][offset*2]); COMBINE_DATA(&cpu_bankbase[0][offset*2 + (OP_ROM - OP_RAM)]); }
/*TODO*///static WRITE32_HANDLER( mwh32_ramrom ) { COMBINE_DATA(&cpu_bankbase[STATIC_RAM][offset*4]); COMBINE_DATA(&cpu_bankbase[0][offset*4 + (OP_ROM - OP_RAM)]); }
/*TODO*///
    public static ReadHandlerPtr mrh8_bank1 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[1].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank2 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[2].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank3 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[3].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank4 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[4].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank5 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[5].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank6 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[6].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank7 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[7].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank8 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[8].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank9 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[9].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank10 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[10].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank11 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[11].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank12 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[12].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank13 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[13].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank14 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[14].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank15 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[15].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank16 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[16].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank17 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[17].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank18 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[18].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank19 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[19].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank20 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[20].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank21 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[21].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank22 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[22].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank23 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[23].read(offset);
        }
    };
    public static ReadHandlerPtr mrh8_bank24 = new ReadHandlerPtr() {
        public int handler(int offset) {
            return cpu_bankbase[24].read(offset);
        }
    };

    public static WriteHandlerPtr mwh8_bank1 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[1].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank2 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[2].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank3 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[3].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank4 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[4].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank5 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[5].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank6 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[6].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank7 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[7].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank8 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[8].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank9 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[9].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank10 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[10].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank11 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[11].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank12 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[12].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank13 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[13].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank14 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[14].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank15 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[15].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank16 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[16].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank17 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[17].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank18 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[18].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank19 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[19].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank20 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[20].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank21 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[21].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank22 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[22].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank23 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[23].write(offset, data);
        }
    };
    public static WriteHandlerPtr mwh8_bank24 = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_bankbase[24].write(offset, data);
        }
    };

    /*TODO*////*-------------------------------------------------
/*TODO*///	init_static - sets up the static memory
/*TODO*///	handlers
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///static int init_static(void)
/*TODO*///{
/*TODO*///	memset(rmemhandler8,  0, sizeof(rmemhandler8));
/*TODO*///	memset(rmemhandler8s, 0, sizeof(rmemhandler8s));
/*TODO*///	memset(rmemhandler16, 0, sizeof(rmemhandler16));
/*TODO*///	memset(rmemhandler32, 0, sizeof(rmemhandler32));
/*TODO*///	memset(wmemhandler8,  0, sizeof(wmemhandler8));
/*TODO*///	memset(wmemhandler8s, 0, sizeof(wmemhandler8s));
/*TODO*///	memset(wmemhandler16, 0, sizeof(wmemhandler16));
/*TODO*///	memset(wmemhandler32, 0, sizeof(wmemhandler32));
/*TODO*///
/*TODO*///	memset(rporthandler8,  0, sizeof(rporthandler8));
/*TODO*///	memset(rporthandler16, 0, sizeof(rporthandler16));
/*TODO*///	memset(rporthandler32, 0, sizeof(rporthandler32));
/*TODO*///	memset(wporthandler8,  0, sizeof(wporthandler8));
/*TODO*///	memset(wporthandler16, 0, sizeof(wporthandler16));
/*TODO*///	memset(wporthandler32, 0, sizeof(wporthandler32));
/*TODO*///
/*TODO*///	set_static_handler(STATIC_BANK1,  mrh8_bank1,  NULL,         NULL,         mwh8_bank1,  NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK2,  mrh8_bank2,  NULL,         NULL,         mwh8_bank2,  NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK3,  mrh8_bank3,  NULL,         NULL,         mwh8_bank3,  NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK4,  mrh8_bank4,  NULL,         NULL,         mwh8_bank4,  NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK5,  mrh8_bank5,  NULL,         NULL,         mwh8_bank5,  NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK6,  mrh8_bank6,  NULL,         NULL,         mwh8_bank6,  NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK7,  mrh8_bank7,  NULL,         NULL,         mwh8_bank7,  NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK8,  mrh8_bank8,  NULL,         NULL,         mwh8_bank8,  NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK9,  mrh8_bank9,  NULL,         NULL,         mwh8_bank9,  NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK10, mrh8_bank10, NULL,         NULL,         mwh8_bank10, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK11, mrh8_bank11, NULL,         NULL,         mwh8_bank11, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK12, mrh8_bank12, NULL,         NULL,         mwh8_bank12, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK13, mrh8_bank13, NULL,         NULL,         mwh8_bank13, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK14, mrh8_bank14, NULL,         NULL,         mwh8_bank14, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK15, mrh8_bank15, NULL,         NULL,         mwh8_bank15, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK16, mrh8_bank16, NULL,         NULL,         mwh8_bank16, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK17, mrh8_bank17, NULL,         NULL,         mwh8_bank17, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK18, mrh8_bank18, NULL,         NULL,         mwh8_bank18, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK19, mrh8_bank19, NULL,         NULL,         mwh8_bank19, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK20, mrh8_bank20, NULL,         NULL,         mwh8_bank20, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK21, mrh8_bank21, NULL,         NULL,         mwh8_bank21, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK22, mrh8_bank22, NULL,         NULL,         mwh8_bank22, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK23, mrh8_bank23, NULL,         NULL,         mwh8_bank23, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_BANK24, mrh8_bank24, NULL,         NULL,         mwh8_bank24, NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_UNMAP,  mrh8_bad,    mrh16_bad,    mrh32_bad,    mwh8_bad,    mwh16_bad,    mwh32_bad);
/*TODO*///	set_static_handler(STATIC_NOP,    mrh8_nop,    mrh16_nop,    mrh32_nop,    mwh8_nop,    mwh16_nop,    mwh32_nop);
/*TODO*///	set_static_handler(STATIC_RAM,    mrh8_ram,    NULL,         NULL,         mwh8_ram,    NULL,         NULL);
/*TODO*///	set_static_handler(STATIC_ROM,    NULL,        NULL,         NULL,         mwh8_rom,    mwh16_rom,    mwh32_rom);
/*TODO*///	set_static_handler(STATIC_RAMROM, NULL,        NULL,         NULL,         mwh8_ramrom, mwh16_ramrom, mwh32_ramrom);
/*TODO*///
/*TODO*///	/* override port unmapped handlers */
/*TODO*///	rporthandler8 [STATIC_UNMAP].handler = (void *)prh8_bad;
/*TODO*///	rporthandler16[STATIC_UNMAP].handler = (void *)prh16_bad;
/*TODO*///	rporthandler32[STATIC_UNMAP].handler = (void *)prh32_bad;
/*TODO*///	wporthandler8 [STATIC_UNMAP].handler = (void *)pwh8_bad;
/*TODO*///	wporthandler16[STATIC_UNMAP].handler = (void *)pwh16_bad;
/*TODO*///	wporthandler32[STATIC_UNMAP].handler = (void *)pwh32_bad;
/*TODO*///
/*TODO*///	return 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*-------------------------------------------------
/*TODO*///	debugging
/*TODO*///-------------------------------------------------*/
/*TODO*///
/*TODO*///#ifdef MEM_DUMP
/*TODO*///
/*TODO*///static void dump_map(FILE *file, const struct memport_data *memport, const struct table_data *table)
/*TODO*///{
/*TODO*///	static const char *strings[] =
/*TODO*///	{
/*TODO*///		"invalid",		"bank 1",		"bank 2",		"bank 3",
/*TODO*///		"bank 4",		"bank 5",		"bank 6",		"bank 7",
/*TODO*///		"bank 8",		"bank 9",		"bank 10",		"bank 11",
/*TODO*///		"bank 12",		"bank 13",		"bank 14",		"bank 15",
/*TODO*///		"bank 16",		"bank 17",		"bank 18",		"bank 19",
/*TODO*///		"bank 20",		"bank 21",		"bank 22",		"bank 23",
/*TODO*///		"bank 24",		"RAM",			"ROM",			"RAMROM",
/*TODO*///		"nop",			"unused 1",		"unused 2",		"unmapped"
/*TODO*///	};
/*TODO*///
/*TODO*///	int minbits = DATABITS_TO_SHIFT(memport->dbits);
/*TODO*///	int l1bits = LEVEL1_BITS(memport->ebits);
/*TODO*///	int l2bits = LEVEL2_BITS(memport->ebits);
/*TODO*///	int l1count = 1 << l1bits;
/*TODO*///	int l2count = 1 << l2bits;
/*TODO*///	int i, j;
/*TODO*///
/*TODO*///	fprintf(file, "  Address bits = %d\n", memport->abits);
/*TODO*///	fprintf(file, "     Data bits = %d\n", memport->dbits);
/*TODO*///	fprintf(file, "Effective bits = %d\n", memport->ebits);
/*TODO*///	fprintf(file, "       L1 bits = %d\n", l1bits);
/*TODO*///	fprintf(file, "       L2 bits = %d\n", l2bits);
/*TODO*///	fprintf(file, "  Address mask = %X\n", memport->mask);
/*TODO*///	fprintf(file, "\n");
/*TODO*///
/*TODO*///	for (i = 0; i < l1count; i++)
/*TODO*///	{
/*TODO*///		UINT8 entry = table->table[i];
/*TODO*///		if (entry != STATIC_UNMAP)
/*TODO*///		{
/*TODO*///			fprintf(file, "%05X  %08X-%08X    = %02X: ", i,
/*TODO*///					i << (l2bits + minbits),
/*TODO*///					((i+1) << (l2bits + minbits)) - 1, entry);
/*TODO*///			if (entry < STATIC_COUNT)
/*TODO*///				fprintf(file, "%s [offset=%08X]\n", strings[entry], table->handlers[entry].offset);
/*TODO*///			else if (entry < SUBTABLE_BASE)
/*TODO*///				fprintf(file, "handler(%08X) [offset=%08X]\n", (UINT32)table->handlers[entry].handler, table->handlers[entry].offset);
/*TODO*///			else
/*TODO*///			{
/*TODO*///				fprintf(file, "subtable %d\n", entry & SUBTABLE_MASK);
/*TODO*///				entry &= SUBTABLE_MASK;
/*TODO*///
/*TODO*///				for (j = 0; j < l2count; j++)
/*TODO*///				{
/*TODO*///					UINT8 entry2 = table->table[(1 << l1bits) + (entry << l2bits) + j];
/*TODO*///					if (entry2 != STATIC_UNMAP)
/*TODO*///					{
/*TODO*///						fprintf(file, "   %05X  %08X-%08X = %02X: ", j,
/*TODO*///								(i << (l2bits + minbits)) | (j << minbits),
/*TODO*///								((i << (l2bits + minbits)) | ((j+1) << minbits)) - 1, entry2);
/*TODO*///						if (entry2 < STATIC_COUNT)
/*TODO*///							fprintf(file, "%s [offset=%08X]\n", strings[entry2], table->handlers[entry2].offset);
/*TODO*///						else if (entry2 < SUBTABLE_BASE)
/*TODO*///							fprintf(file, "handler(%08X) [offset=%08X]\n", (UINT32)table->handlers[entry2].handler, table->handlers[entry2].offset);
/*TODO*///						else
/*TODO*///							fprintf(file, "subtable %d???????????\n", entry2 & SUBTABLE_MASK);
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///static void mem_dump(void)
/*TODO*///{
/*TODO*///	FILE *file = fopen("memdump.log", "w");
/*TODO*///	int cpu;
/*TODO*///
/*TODO*///	/* skip if we can't open the file */
/*TODO*///	if (!file)
/*TODO*///		return;
/*TODO*///
/*TODO*///	/* loop over CPUs */
/*TODO*///	for (cpu = 0; cpu < cpu_gettotalcpu(); cpu++)
/*TODO*///	{
/*TODO*///		/* memory handlers */
/*TODO*///		if (cpudata[cpu].mem.abits)
/*TODO*///		{
/*TODO*///			fprintf(file, "\n\n"
/*TODO*///			              "===============================\n"
/*TODO*///			              "CPU %d read memory handler dump\n"
/*TODO*///			              "===============================\n", cpu);
/*TODO*///			dump_map(file, &cpudata[cpu].mem, &cpudata[cpu].mem.read);
/*TODO*///
/*TODO*///			fprintf(file, "\n\n"
/*TODO*///			              "================================\n"
/*TODO*///			              "CPU %d write memory handler dump\n"
/*TODO*///			              "================================\n", cpu);
/*TODO*///			dump_map(file, &cpudata[cpu].mem, &cpudata[cpu].mem.write);
/*TODO*///		}
/*TODO*///
/*TODO*///		/* port handlers */
/*TODO*///		if (cpudata[cpu].port.abits)
/*TODO*///		{
/*TODO*///			fprintf(file, "\n\n"
/*TODO*///			              "=============================\n"
/*TODO*///			              "CPU %d read port handler dump\n"
/*TODO*///			              "=============================\n", cpu);
/*TODO*///			dump_map(file, &cpudata[cpu].port, &cpudata[cpu].port.read);
/*TODO*///
/*TODO*///			fprintf(file, "\n\n"
/*TODO*///			              "==============================\n"
/*TODO*///			              "CPU %d write port handler dump\n"
/*TODO*///			              "==============================\n", cpu);
/*TODO*///			dump_map(file, &cpudata[cpu].port, &cpudata[cpu].port.write);
/*TODO*///		}
/*TODO*///	}
/*TODO*///	fclose(file);
/*TODO*///}
/*TODO*///#endif
/*TODO*///
/*TODO*///#ifdef CHECK_MASKS
/*TODO*///static void *track_buffer[65536];
/*TODO*///static int track_count;
/*TODO*///static int static_count;
/*TODO*///static int track_entry(void *entry, void *handler)
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	for (i = 0; i < track_count; i++)
/*TODO*///		if (track_buffer[i] == entry)
/*TODO*///			return 1;
/*TODO*///	track_buffer[track_count++] = entry;
/*TODO*///	if (HANDLER_IS_STATIC(handler))
/*TODO*///		static_count++;
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///static void verify_masks(void)
/*TODO*///{
/*TODO*///	int i, cpu;
/*TODO*///
/*TODO*///	for (i = 0;drivers[i];i++)
/*TODO*///	{
/*TODO*///		const struct RomModule *romp = drivers[i]->rom;
/*TODO*///		if (romp)
/*TODO*///		{
/*TODO*///			for (cpu = 0;cpu < MAX_CPU;cpu++)
/*TODO*///			{
/*TODO*///				if (drivers[i]->drv->cpu[cpu].cpu_type)
/*TODO*///				{
/*TODO*///					const struct Memory_ReadAddress *mra = drivers[i]->drv->cpu[cpu].memory_read;
/*TODO*///					const struct Memory_WriteAddress *mwa = drivers[i]->drv->cpu[cpu].memory_write;
/*TODO*///					const struct IO_ReadPort *iora = drivers[i]->drv->cpu[cpu].port_read;
/*TODO*///					const struct IO_WritePort *iowa = drivers[i]->drv->cpu[cpu].port_write;
/*TODO*///
/*TODO*///					if (mra)
/*TODO*///						for ( ; !IS_MEMPORT_END(mra); mra++)
/*TODO*///							if (!IS_MEMPORT_MARKER(mra))
/*TODO*///							{
/*TODO*///								size_t size = mra->end - mra->start + 1;
/*TODO*///								if (size)
/*TODO*///								{
/*TODO*///									while (!(size & 1)) size >>= 1;
/*TODO*///									if (size != 1)
/*TODO*///									{
/*TODO*///										if (!track_entry((void *)mra, (void *)mra->handler))
/*TODO*///											printf("%s: %s cpu %d readmem inval size  { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, mra->start, mra->end);
/*TODO*///									}
/*TODO*///								}
/*TODO*///								size--;
/*TODO*///								if ((mra->start & size) != 0)
/*TODO*///								{
/*TODO*///									if (!track_entry((void *)mra, (void *)mra->handler))
/*TODO*///										printf("%s: %s cpu %d readmem inval start { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, mra->start, mra->end);
/*TODO*///								}
/*TODO*///								if ((mra->end & size) != size)
/*TODO*///								{
/*TODO*///									if (!track_entry((void *)mra, (void *)mra->handler))
/*TODO*///										printf("%s: %s cpu %d readmem inval end  { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, mra->start, mra->end);
/*TODO*///								}
/*TODO*///							}
/*TODO*///
/*TODO*///					if (mwa)
/*TODO*///						for ( ; !IS_MEMPORT_END(mwa); mwa++)
/*TODO*///							if (!IS_MEMPORT_MARKER(mwa))
/*TODO*///							{
/*TODO*///								size_t size = mwa->end - mwa->start + 1;
/*TODO*///								if (size)
/*TODO*///								{
/*TODO*///									while (!(size & 1)) size >>= 1;
/*TODO*///									if (size != 1)
/*TODO*///									{
/*TODO*///										if (!track_entry((void *)mwa, (void *)mwa->handler))
/*TODO*///											printf("%s: %s cpu %d writemem inval size  { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, mwa->start, mwa->end);
/*TODO*///									}
/*TODO*///								}
/*TODO*///								size--;
/*TODO*///								if ((mwa->start & size) != 0)
/*TODO*///								{
/*TODO*///									if (!track_entry((void *)mwa, (void *)mwa->handler))
/*TODO*///										printf("%s: %s cpu %d writemem inval start { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, mwa->start, mwa->end);
/*TODO*///								}
/*TODO*///								if ((mwa->end & size) != size)
/*TODO*///								{
/*TODO*///									if (!track_entry((void *)mwa, (void *)mwa->handler))
/*TODO*///										printf("%s: %s cpu %d writemem inval end  { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, mwa->start, mwa->end);
/*TODO*///								}
/*TODO*///							}
/*TODO*///
/*TODO*///					if (iora)
/*TODO*///						for ( ; !IS_MEMPORT_END(iora); iora++)
/*TODO*///							if (!IS_MEMPORT_MARKER(iora))
/*TODO*///							{
/*TODO*///								size_t size = iora->end - iora->start + 1;
/*TODO*///								if (size)
/*TODO*///								{
/*TODO*///									while (!(size & 1)) size >>= 1;
/*TODO*///									if (size != 1)
/*TODO*///									{
/*TODO*///										if (!track_entry((void *)iora, (void *)iora->handler))
/*TODO*///											printf("%s: %s cpu %d readmem inval size  { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, iora->start, iora->end);
/*TODO*///									}
/*TODO*///								}
/*TODO*///								size--;
/*TODO*///								if ((iora->start & size) != 0)
/*TODO*///								{
/*TODO*///									if (!track_entry((void *)iora, (void *)iora->handler))
/*TODO*///										printf("%s: %s cpu %d readmem inval start { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, iora->start, iora->end);
/*TODO*///								}
/*TODO*///								if ((iora->end & size) != size)
/*TODO*///								{
/*TODO*///									if (!track_entry((void *)iora, (void *)iora->handler))
/*TODO*///										printf("%s: %s cpu %d readmem inval end  { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, iora->start, iora->end);
/*TODO*///								}
/*TODO*///							}
/*TODO*///
/*TODO*///					if (iowa)
/*TODO*///						for ( ; !IS_MEMPORT_END(iowa); iowa++)
/*TODO*///							if (!IS_MEMPORT_MARKER(iowa))
/*TODO*///							{
/*TODO*///								size_t size = iowa->end - iowa->start + 1;
/*TODO*///								if (size)
/*TODO*///								{
/*TODO*///									while (!(size & 1)) size >>= 1;
/*TODO*///									if (size != 1)
/*TODO*///									{
/*TODO*///										if (!track_entry((void *)iowa, (void *)iowa->handler))
/*TODO*///											printf("%s: %s cpu %d writemem inval size  { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, iowa->start, iowa->end);
/*TODO*///									}
/*TODO*///								}
/*TODO*///								size--;
/*TODO*///								if ((iowa->start & size) != 0)
/*TODO*///								{
/*TODO*///									if (!track_entry((void *)iowa, (void *)iowa->handler))
/*TODO*///										printf("%s: %s cpu %d writemem inval start { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, iowa->start, iowa->end);
/*TODO*///								}
/*TODO*///								if ((iowa->end & size) != size)
/*TODO*///								{
/*TODO*///									if (!track_entry((void *)iowa, (void *)iowa->handler))
/*TODO*///										printf("%s: %s cpu %d writemem inval end  { %08X, %08X }\n", drivers[i]->source_file, drivers[i]->name, cpu, iowa->start, iowa->end);
/*TODO*///								}
/*TODO*///							}
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	printf("Total busted entries = %d\n", track_count);
/*TODO*///	printf("Busted entries that are static = %d\n", static_count);
/*TODO*///}
/*TODO*///#endif
/*TODO*///
}
