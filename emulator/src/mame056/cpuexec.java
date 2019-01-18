/**
 * Ported to 0.56
 */
package mame056;

import static arcadeflex.fucPtr.*;

import static mame056.cpuexecH.*;
import static mame056.cpuintrf.*;
import static mame056.cpuintrfH.*;
import static mame.driverH.*;
import static old2.mame.mame.*;
import static old2.mame.timer.*;
import static old2.mame.timerH.*;
import static old.arcadeflex.osdepend.*;

public class cpuexec {

    /*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Debug logging
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///#define VERBOSE 0
/*TODO*///
/*TODO*///#if VERBOSE
/*TODO*///#define LOG(x)	logerror x
/*TODO*///#else
/*TODO*///#define LOG(x)
/*TODO*///#endif
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Macros to help verify active CPU
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///#define VERIFY_ACTIVECPU(retval, name)						\
/*TODO*///	int activecpu = cpu_getactivecpu();						\
/*TODO*///	if (activecpu < 0)										\
/*TODO*///	{														\
/*TODO*///		logerror(#name "() called with no active cpu!\n");	\
/*TODO*///		return retval;										\
/*TODO*///	}
/*TODO*///
/*TODO*///#define VERIFY_ACTIVECPU_VOID(name)							\
/*TODO*///	int activecpu = cpu_getactivecpu();						\
/*TODO*///	if (activecpu < 0)										\
/*TODO*///	{														\
/*TODO*///		logerror(#name "() called with no active cpu!\n");	\
/*TODO*///		return;												\
/*TODO*///	}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Triggers for the timer system
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///enum
/*TODO*///{
/*TODO*///	TRIGGER_TIMESLICE 	= -1000,
/*TODO*///	TRIGGER_INT 		= -2000,
/*TODO*///	TRIGGER_YIELDTIME 	= -3000,
/*TODO*///	TRIGGER_SUSPENDTIME = -4000
/*TODO*///};
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Internal CPU info structure
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///struct cpuinfo
/*TODO*///{
/*TODO*///	int 	iloops; 				/* number of interrupts remaining this frame */
/*TODO*///	int 	totalcycles;			/* total CPU cycles executed */
/*TODO*///	int 	vblankint_countdown;	/* number of vblank callbacks left until we interrupt */
/*TODO*///	int 	vblankint_multiplier;	/* number of vblank callbacks per interrupt */
/*TODO*///	void *	vblankint_timer;		/* reference to elapsed time counter */
/*TODO*///	double	vblankint_period;		/* timing period of the VBLANK interrupt */
/*TODO*///	void *	timedint_timer;			/* reference to this CPU's timer */
/*TODO*///	double	timedint_period; 		/* timing period of the timed interrupt */
/*TODO*///};
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	General CPU variables
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static struct cpuinfo cpu[MAX_CPU];
    static int time_to_reset;
    static int time_to_quit;

    static int vblank;
    static int current_frame;
    static int watchdog_counter;

    static int cycles_running;
    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	CPU interrupt variables
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static UINT8 interrupt_enable[MAX_CPU];
/*TODO*///static INT32 interrupt_vector[MAX_CPU];
/*TODO*///
    static int[][]/*UINT8*/ irq_line_state = new int[MAX_CPU][MAX_IRQ_LINES];
    static int[][] irq_line_vector = new int[MAX_CPU][MAX_IRQ_LINES];

    /**
     * ***********************************
     *
     * Timer variables
     *
     ************************************
     */
    static Object vblank_timer;
    static int vblank_countdown;
    static int vblank_multiplier;
    static double vblank_period;

    static Object refresh_timer;
    static double refresh_period;
    static double refresh_period_inv;

    static Object timeslice_timer;
    static double timeslice_period;

    static double scanline_period;
    static double scanline_period_inv;

    /*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Save/load variables
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static int loadsave_schedule;
/*TODO*///static char loadsave_schedule_id;
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///static int (*cpu_irq_callbacks[MAX_CPU])(int) =
/*TODO*///{
/*TODO*///	cpu_0_irq_callback,
/*TODO*///	cpu_1_irq_callback,
/*TODO*///	cpu_2_irq_callback,
/*TODO*///	cpu_3_irq_callback,
/*TODO*///	cpu_4_irq_callback,
/*TODO*///	cpu_5_irq_callback,
/*TODO*///	cpu_6_irq_callback,
/*TODO*///	cpu_7_irq_callback
/*TODO*///};
/*TODO*///
/*TODO*///static int (*drv_irq_callbacks[MAX_CPU])(int);
/*TODO*///
    /**
     * ***********************************
     *
     * Initialize all the CPUs
     *
     ************************************
     */
    public static int cpu_init() {
        int cpunum;

        /* initialize the interfaces first */
        if (cpuintrf_init() != 0) {
            return 1;
        }

        /* count how many CPUs we have to emulate */
        for (cpunum = 0; cpunum < MAX_CPU; cpunum++) {
            int cputype = Machine.drv.cpu[cpunum].cpu_type & ~CPU_FLAGS_MASK;
            int irqline;

            /* stop when we hit a dummy */
            if (cputype == CPU_DUMMY) {
                break;
            }

            /* set the save state tag */
 /*TODO*///		state_save_set_current_tag(cpunum + 1);

            /* initialize this CPU */
            if (cpuintrf_init_cpu(cpunum, cputype) != 0) {
                return 1;
            }

            /* reset the IRQ lines */
            for (irqline = 0; irqline < MAX_IRQ_LINES; irqline++) {
                irq_line_state[cpunum][irqline] = CLEAR_LINE;
                irq_line_vector[cpunum][irqline] = cpunum_default_irq_vector(cpunum);
            }
        }
        /*TODO*///
/*TODO*///	/* save some stuff in tag 0 */
/*TODO*///	state_save_set_current_tag(0);
/*TODO*///	state_save_register_UINT8("cpu", 0, "irq enable",     interrupt_enable,  cpunum);
/*TODO*///	state_save_register_INT32("cpu", 0, "irq vector",     interrupt_vector,  cpunum);
/*TODO*///	state_save_register_UINT8("cpu", 0, "irqline state",  &irq_line_state[0][0],  cpunum * MAX_IRQ_LINES);
/*TODO*///	state_save_register_INT32("cpu", 0, "irqline vector", &irq_line_vector[0][0], cpunum * MAX_IRQ_LINES);
/*TODO*///	state_save_register_INT32("cpu", 0, "watchdog count", &watchdog_counter, 1);

        /* init the timer system */
        timer_init();
        timeslice_timer = refresh_timer = vblank_timer = null;

        return 0;
    }

    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Prepare the system for execution
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void cpu_pre_run(void)
/*TODO*///{
/*TODO*///	int cpunum;
/*TODO*///
/*TODO*///	logerror("Machine reset\n");
/*TODO*///
/*TODO*///	auto_malloc_start();
/*TODO*///
/*TODO*///	/* read hi scores information from hiscore.dat */
/*TODO*///	hs_open(Machine->gamedrv->name);
/*TODO*///	hs_init();
/*TODO*///
/*TODO*///	/* initialize the various timers (suspends all CPUs at startup) */
/*TODO*///	cpu_inittimers();
/*TODO*///	watchdog_counter = -1;
/*TODO*///
/*TODO*///	/* reset sound chips */
/*TODO*///	sound_reset();
/*TODO*///
/*TODO*///	/* first pass over CPUs */
/*TODO*///	for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///	{
/*TODO*///		/* enable all CPUs (except for audio CPUs if the sound is off) */
/*TODO*///		if (!(Machine->drv->cpu[cpunum].cpu_type & CPU_AUDIO_CPU) || Machine->sample_rate != 0)
/*TODO*///			timer_suspendcpu(cpunum, 0, SUSPEND_ANY_REASON);
/*TODO*///		else
/*TODO*///			timer_suspendcpu(cpunum, 1, SUSPEND_REASON_DISABLE);
/*TODO*///
/*TODO*///		/* start with interrupts enabled, so the generic routine will work even if */
/*TODO*///		/* the machine doesn't have an interrupt enable port */
/*TODO*///		interrupt_enable[cpunum] = 1;
/*TODO*///		interrupt_vector[cpunum] = 0xff;
/*TODO*///
/*TODO*///		/* reset any driver hooks into the IRQ acknowledge callbacks */
/*TODO*///		drv_irq_callbacks[cpunum] = NULL;
/*TODO*///
/*TODO*///		/* reset the total number of cycles */
/*TODO*///		cpu[cpunum].totalcycles = 0;
/*TODO*///	}
/*TODO*///
/*TODO*///	vblank = 0;
/*TODO*///
/*TODO*///	/* do this AFTER the above so init_machine() can use cpu_halt() to hold the */
/*TODO*///	/* execution of some CPUs, or disable interrupts */
/*TODO*///	if (Machine->drv->init_machine)
/*TODO*///		(*Machine->drv->init_machine)();
/*TODO*///
/*TODO*///	/* now reset each CPU */
/*TODO*///	for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///		cpunum_reset(cpunum, Machine->drv->cpu[cpunum].reset_param, cpu_irq_callbacks[cpunum]);
/*TODO*///
/*TODO*///	/* reset the globals */
/*TODO*///	cpu_vblankreset();
/*TODO*///	current_frame = 0;
/*TODO*///	state_save_dump_registry();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Finish up execution
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void cpu_post_run(void)
/*TODO*///{
/*TODO*///	/* write hi scores to disk - No scores saving if cheat */
/*TODO*///	hs_close();
/*TODO*///
/*TODO*///#ifdef MESS
/*TODO*///	/* stop the machine */
/*TODO*///	if (Machine->drv->stop_machine)
/*TODO*///		(*Machine->drv->stop_machine)();
/*TODO*///#endif
/*TODO*///
/*TODO*///	auto_malloc_stop();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Execute until done
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_run(void)
/*TODO*///{
/*TODO*///	int cpunum;
/*TODO*///
/*TODO*///#ifdef MAME_DEBUG
/*TODO*///	/* initialize the debugger */
/*TODO*///	if (mame_debug)
/*TODO*///		mame_debug_init();
/*TODO*///#endif
/*TODO*///
/*TODO*///	/* loop over multiple resets, until the user quits */
/*TODO*///	time_to_quit = 0;
/*TODO*///	while (!time_to_quit)
/*TODO*///	{
/*TODO*///		/* prepare everything to run */
/*TODO*///		cpu_pre_run();
/*TODO*///
/*TODO*///		/* loop until the user quits or resets */
/*TODO*///		time_to_reset = 0;
/*TODO*///		while (!time_to_quit && !time_to_reset)
/*TODO*///		{
/*TODO*///			profiler_mark(PROFILER_EXTRA);
/*TODO*///
/*TODO*///			/* if we have a load/save scheduled, handle it */
/*TODO*///			if (loadsave_schedule != LOADSAVE_NONE)
/*TODO*///				handle_loadsave();
/*TODO*///
/*TODO*///			/* ask the timer system to schedule */
/*TODO*///			if (timer_schedule_cpu(&cpunum, &cycles_running))
/*TODO*///			{
/*TODO*///				int ran;
/*TODO*///
/*TODO*///				/* run for the requested number of cycles */
/*TODO*///				profiler_mark(PROFILER_CPU1 + cpunum);
/*TODO*///				ran = cpunum_execute(cpunum, cycles_running);
/*TODO*///				profiler_mark(PROFILER_END);
/*TODO*///
/*TODO*///				/* update based on how many cycles we really ran */
/*TODO*///				cpu[cpunum].totalcycles += ran;
/*TODO*///
/*TODO*///				/* update the timer with how long we actually ran */
/*TODO*///				timer_update_cpu(cpunum, ran);
/*TODO*///			}
/*TODO*///
/*TODO*///			profiler_mark(PROFILER_END);
/*TODO*///		}
/*TODO*///
/*TODO*///		/* finish up this iteration */
/*TODO*///		cpu_post_run();
/*TODO*///	}
/*TODO*///
/*TODO*///#ifdef MAME_DEBUG
/*TODO*///	/* shut down the debugger */
/*TODO*///	if (mame_debug)
/*TODO*///		mame_debug_exit();
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Deinitialize all the CPUs
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_exit(void)
/*TODO*///{
/*TODO*///	int cpunum;
/*TODO*///
/*TODO*///	/* shut down the CPU cores */
/*TODO*///	for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///		cpuintrf_exit_cpu(cpunum);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Force a reset at the end of this
/*TODO*/// *	timeslice
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void machine_reset(void)
/*TODO*///{
/*TODO*///	time_to_reset = 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///#pragma mark -
/*TODO*///#pragma mark SAVE/RESTORE
/*TODO*///#endif
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Handle saves at runtime
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void handle_save(void)
/*TODO*///{
/*TODO*///	char name[2] = { 0 };
/*TODO*///	void *file;
/*TODO*///	int cpunum;
/*TODO*///
/*TODO*///	/* open the file */
/*TODO*///	name[0] = loadsave_schedule_id;
/*TODO*///	file = osd_fopen(Machine->gamedrv->name, name, OSD_FILETYPE_STATE, 1);
/*TODO*///
/*TODO*///	/* write the save state */
/*TODO*///	state_save_save_begin(file);
/*TODO*///
/*TODO*///	/* write tag 0 */
/*TODO*///	state_save_set_current_tag(0);
/*TODO*///	state_save_save_continue();
/*TODO*///
/*TODO*///	/* loop over CPUs */
/*TODO*///	for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///	{
/*TODO*///		cpuintrf_push_context(cpunum);
/*TODO*///
/*TODO*///		/* make sure banking is set */
/*TODO*///		activecpu_reset_banking();
/*TODO*///
/*TODO*///		/* save the CPU data */
/*TODO*///		state_save_set_current_tag(cpunum + 1);
/*TODO*///		state_save_save_continue();
/*TODO*///
/*TODO*///		cpuintrf_pop_context();
/*TODO*///	}
/*TODO*///
/*TODO*///	/* finish and close */
/*TODO*///	state_save_save_finish();
/*TODO*///	osd_fclose(file);
/*TODO*///
/*TODO*///	/* unschedule the save */
/*TODO*///	loadsave_schedule = LOADSAVE_NONE;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Handle loads at runtime
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void handle_load(void)
/*TODO*///{
/*TODO*///	char name[2] = { 0 };
/*TODO*///	void *file;
/*TODO*///	int cpunum;
/*TODO*///
/*TODO*///	/* open the file */
/*TODO*///	name[0] = loadsave_schedule_id;
/*TODO*///	file = osd_fopen(Machine->gamedrv->name, name, OSD_FILETYPE_STATE, 0);
/*TODO*///
/*TODO*///	/* if successful, load it */
/*TODO*///	if (file)
/*TODO*///	{
/*TODO*///		/* start loading */
/*TODO*///		if (!state_save_load_begin(file))
/*TODO*///		{
/*TODO*///			/* read tag 0 */
/*TODO*///			state_save_set_current_tag(0);
/*TODO*///			state_save_load_continue();
/*TODO*///
/*TODO*///			/* loop over CPUs */
/*TODO*///			for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///			{
/*TODO*///				cpuintrf_push_context(cpunum);
/*TODO*///
/*TODO*///				/* make sure banking is set */
/*TODO*///				activecpu_reset_banking();
/*TODO*///
/*TODO*///				/* load the CPU data */
/*TODO*///				state_save_set_current_tag(cpunum + 1);
/*TODO*///				state_save_load_continue();
/*TODO*///
/*TODO*///				cpuintrf_pop_context();
/*TODO*///			}
/*TODO*///
/*TODO*///			/* finish and close */
/*TODO*///			state_save_load_finish();
/*TODO*///		}
/*TODO*///		osd_fclose(file);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* unschedule the load */
/*TODO*///	loadsave_schedule = LOADSAVE_NONE;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Handle saves & loads at runtime
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void handle_loadsave(void)
/*TODO*///{
/*TODO*///	/* it's one or the other */
/*TODO*///	if (loadsave_schedule == LOADSAVE_SAVE)
/*TODO*///		handle_save();
/*TODO*///	else if (loadsave_schedule == LOADSAVE_LOAD)
/*TODO*///		handle_load();
/*TODO*///
/*TODO*///	/* reset the schedule */
/*TODO*///	loadsave_schedule = LOADSAVE_NONE;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Schedules a save/load for later
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_loadsave_schedule(int type, char id)
/*TODO*///{
/*TODO*///	loadsave_schedule = type;
/*TODO*///	loadsave_schedule_id = id;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Unschedules any saves or loads
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_loadsave_reset(void)
/*TODO*///{
/*TODO*///	loadsave_schedule = LOADSAVE_NONE;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///#pragma mark -
/*TODO*///#pragma mark WATCHDOG
/*TODO*///#endif
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Watchdog routines
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////*--------------------------------------------------------------
/*TODO*///
/*TODO*///	Use these functions to initialize, and later maintain, the
/*TODO*///	watchdog. For convenience, when the machine is reset, the
/*TODO*///	watchdog is disabled. If you call this function, the
/*TODO*///	watchdog is initialized, and from that point onwards, if you
/*TODO*///	don't call it at least once every 3 seconds, the machine
/*TODO*///	will be reset.
/*TODO*///
/*TODO*///	The 3 seconds delay is targeted at qzshowby, which otherwise
/*TODO*///	would reset at the start of a game.
/*TODO*///
/*TODO*///--------------------------------------------------------------*/
    static void watchdog_reset() {
        if (watchdog_counter == -1) {
            logerror("watchdog armed\n");
        }
        watchdog_counter = (int) (3 * Machine.drv.frames_per_second);
    }

    public static WriteHandlerPtr watchdog_reset_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            watchdog_reset();
        }
    };

    public static ReadHandlerPtr watchdog_reset_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            watchdog_reset();
            return 0xff;
        }
    };

    /*TODO*///
/*TODO*///
/*TODO*///WRITE16_HANDLER( watchdog_reset16_w )
/*TODO*///{
/*TODO*///	watchdog_reset();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///READ16_HANDLER( watchdog_reset16_r )
/*TODO*///{
/*TODO*///	watchdog_reset();
/*TODO*///	return 0xffff;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///WRITE32_HANDLER( watchdog_reset32_w )
/*TODO*///{
/*TODO*///	watchdog_reset();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///READ32_HANDLER( watchdog_reset32_r )
/*TODO*///{
/*TODO*///	watchdog_reset();
/*TODO*///	return 0xffffffff;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///#pragma mark -
/*TODO*///#pragma mark HALT/RESET
/*TODO*///#endif
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Handle reset line changes
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void reset_callback(int param)
/*TODO*///{
/*TODO*///	int cpunum = param & 0xff;
/*TODO*///	int state = param >> 8;
/*TODO*///
/*TODO*///	/* if we're asserting the line, just halt the CPU */
/*TODO*///	if (state == ASSERT_LINE)
/*TODO*///	{
/*TODO*///		timer_suspendcpu(cpunum, 1, SUSPEND_REASON_RESET);
/*TODO*///		return;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* if we're clearing the line that was previously asserted, or if we're just */
/*TODO*///	/* pulsing the line, reset the CPU */
/*TODO*///	if ((state == CLEAR_LINE && timer_iscpususpended(cpunum, SUSPEND_REASON_RESET)) || state == PULSE_LINE)
/*TODO*///		cpunum_reset(cpunum, Machine->drv->cpu[cpunum].reset_param, cpu_irq_callbacks[cpunum]);
/*TODO*///
/*TODO*///	/* if we're clearing the line, make sure the CPU is not halted */
/*TODO*///	timer_suspendcpu(cpunum, 0, SUSPEND_REASON_RESET);
/*TODO*///}
/*TODO*///
/*TODO*///
    public static void cpu_set_reset_line(int cpunum, int state) {
        throw new UnsupportedOperationException("Unsupported");
        /*TODO*///	timer_set(TIME_NOW, (cpunum & 0xff) | (state << 8), reset_callback);
    }

    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Handle halt line changes
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void halt_callback(int param)
/*TODO*///{
/*TODO*///	int cpunum = param & 0xff;
/*TODO*///	int state = param >> 8;
/*TODO*///
/*TODO*///	/* if asserting, halt the CPU */
/*TODO*///	if (state == ASSERT_LINE)
/*TODO*///		timer_suspendcpu(cpunum, 1, SUSPEND_REASON_HALT);
/*TODO*///
/*TODO*///	/* if clearing, unhalt the CPU */
/*TODO*///	else if (state == CLEAR_LINE)
/*TODO*///		timer_suspendcpu(cpunum, 0, SUSPEND_REASON_HALT);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///void cpu_set_halt_line(int cpunum, int state)
/*TODO*///{
/*TODO*///	timer_set(TIME_NOW, (cpunum & 0xff) | (state << 8), halt_callback);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Return suspended status of CPU
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///int cpu_getstatus(int cpunum)
/*TODO*///{
/*TODO*///	if (cpunum < cpu_gettotalcpu())
/*TODO*///		return !timer_iscpususpended(cpunum, SUSPEND_REASON_HALT | SUSPEND_REASON_RESET | SUSPEND_REASON_DISABLE);
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///#pragma mark -
/*TODO*///#pragma mark TIMING HELPERS
/*TODO*///#endif
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Return cycles ran this iteration
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///int cycles_currently_ran(void)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU(0, cycles_currently_ran);
/*TODO*///	return cycles_running - activecpu_get_icount();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Return cycles remaining in this
/*TODO*/// *	iteration
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///int cycles_left_to_run(void)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU(0, cycles_left_to_run);
/*TODO*///	return activecpu_get_icount();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Return total number of CPU cycles
/*TODO*/// *	for the active CPU.
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////*--------------------------------------------------------------
/*TODO*///
/*TODO*///	IMPORTANT: this value wraps around in a relatively short
/*TODO*///	time. For example, for a 6MHz CPU, it will wrap around in
/*TODO*///	2^32/6000000 = 716 seconds = 12 minutes.
/*TODO*///	Make sure you don't do comparisons between values returned
/*TODO*///	by this function, but only use the difference (which will
/*TODO*///	be correct regardless of wraparound).
/*TODO*///
/*TODO*///--------------------------------------------------------------*/
/*TODO*///
/*TODO*///int cpu_gettotalcycles(void)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU(0, cpu_gettotalcycles);
/*TODO*///	return cpu[activecpu].totalcycles + cycles_currently_ran();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Return cycles until next interrupt
/*TODO*/// *	handler call
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///int cpu_geticount(void)
/*TODO*///{
/*TODO*///	int result;
/*TODO*///
/*TODO*////* remove me - only used by mamedbg, m92 */
/*TODO*///	VERIFY_ACTIVECPU(0, cpu_geticount);
/*TODO*///	result = TIME_TO_CYCLES(activecpu, cpu[activecpu].vblankint_period - timer_timeelapsed(cpu[activecpu].vblankint_timer));
/*TODO*///	return (result < 0) ? 0 : result;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Scales a given value by the fraction
/*TODO*/// *	of time elapsed between refreshes
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///int cpu_scalebyfcount(int value)
/*TODO*///{
/*TODO*///	int result = (int)((double)value * timer_timeelapsed(refresh_timer) * refresh_period_inv);
/*TODO*///	if (value >= 0)
/*TODO*///		return (result < value) ? result : value;
/*TODO*///	else
/*TODO*///		return (result > value) ? result : value;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///#pragma mark -
/*TODO*///#pragma mark VIDEO TIMING
/*TODO*///#endif
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Returns the current scanline
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////*--------------------------------------------------------------
/*TODO*///
/*TODO*///	Note: cpu_getscanline() counts from 0, 0 being the first
/*TODO*///	visible line. You might have to adjust this value to match
/*TODO*///	the hardware, since in many cases the first visible line
/*TODO*///	is >0.
/*TODO*///
/*TODO*///--------------------------------------------------------------*/
/*TODO*///
/*TODO*///int cpu_getscanline(void)
/*TODO*///{
/*TODO*///	return (int)(timer_timeelapsed(refresh_timer) * scanline_period_inv);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Returns time until given scanline
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///double cpu_getscanlinetime(int scanline)
/*TODO*///{
/*TODO*///	double scantime = timer_starttime(refresh_timer) + (double)scanline * scanline_period;
/*TODO*///	double abstime = timer_get_time();
/*TODO*///	double result;
/*TODO*///
/*TODO*///	/* if we're already past the computed time, count it for the next frame */
/*TODO*///	if (abstime >= scantime)
/*TODO*///		scantime += TIME_IN_HZ(Machine->drv->frames_per_second);
/*TODO*///
/*TODO*///	/* compute how long from now until that time */
/*TODO*///	result = scantime - abstime;
/*TODO*///
/*TODO*///	/* if it's small, just count a whole frame */
/*TODO*///	if (result < TIME_IN_NSEC(1))
/*TODO*///		result = TIME_IN_HZ(Machine->drv->frames_per_second);
/*TODO*///	return result;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Returns time for one scanline
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///double cpu_getscanlineperiod(void)
/*TODO*///{
/*TODO*///	return scanline_period;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Returns a crude approximation
/*TODO*/// *	of the horizontal position of the
/*TODO*/// *	bream
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///int cpu_gethorzbeampos(void)
/*TODO*///{
/*TODO*///	double elapsed_time = timer_timeelapsed(refresh_timer);
/*TODO*///	int scanline = (int)(elapsed_time * scanline_period_inv);
/*TODO*///	double time_since_scanline = elapsed_time - (double)scanline * scanline_period;
/*TODO*///	return (int)(time_since_scanline * scanline_period_inv * (double)Machine->drv->screen_width);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Returns the VBLANK state
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///int cpu_getvblank(void)
/*TODO*///{
/*TODO*///	return vblank;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Returns the current frame count
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///int cpu_getcurrentframe(void)
/*TODO*///{
/*TODO*///	return current_frame;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///#pragma mark -
/*TODO*///#pragma mark INTERRUPT HANDLING
/*TODO*///#endif
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Set IRQ callback for drivers
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_set_irq_callback(int cpunum, int (*callback)(int))
/*TODO*///{
/*TODO*///	drv_irq_callbacks[cpunum] = callback;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Internal IRQ callbacks
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///INLINE int cpu_irq_callback(int cpunum, int irqline)
/*TODO*///{
/*TODO*///	int vector = irq_line_vector[cpunum][irqline];
/*TODO*///
/*TODO*///	LOG(("cpu_%d_irq_callback(%d) $%04xn", cpunum, irqline, vector));
/*TODO*///
/*TODO*///	/* if the IRQ state is HOLD_LINE, clear it */
/*TODO*///	if (irq_line_state[cpunum][irqline] == HOLD_LINE)
/*TODO*///	{
/*TODO*///		LOG(("->set_irq_line(%d,%d,%d)\n", cpunum, irqline, CLEAR_LINE));
/*TODO*///		activecpu_set_irq_line(irqline, INTERNAL_CLEAR_LINE);
/*TODO*///		irq_line_state[cpunum][irqline] = CLEAR_LINE;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* if there's a driver callback, run it */
/*TODO*///	if (drv_irq_callbacks[cpunum])
/*TODO*///		vector = (*drv_irq_callbacks[cpunum])(irqline);
/*TODO*///
/*TODO*///	/* otherwise, just return the current vector */
/*TODO*///	return vector;
/*TODO*///}
/*TODO*///
/*TODO*///static int cpu_0_irq_callback(int irqline) { return cpu_irq_callback(0, irqline); }
/*TODO*///static int cpu_1_irq_callback(int irqline) { return cpu_irq_callback(1, irqline); }
/*TODO*///static int cpu_2_irq_callback(int irqline) { return cpu_irq_callback(2, irqline); }
/*TODO*///static int cpu_3_irq_callback(int irqline) { return cpu_irq_callback(3, irqline); }
/*TODO*///static int cpu_4_irq_callback(int irqline) { return cpu_irq_callback(4, irqline); }
/*TODO*///static int cpu_5_irq_callback(int irqline) { return cpu_irq_callback(5, irqline); }
/*TODO*///static int cpu_6_irq_callback(int irqline) { return cpu_irq_callback(6, irqline); }
/*TODO*///static int cpu_7_irq_callback(int irqline) { return cpu_irq_callback(7, irqline); }
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Set the IRQ vector for a given
/*TODO*/// *	IRQ line on a CPU
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_irq_line_vector_w(int cpunum, int irqline, int vector)
/*TODO*///{
/*TODO*///	if (cpunum < cpu_gettotalcpu() && irqline >= 0 && irqline < MAX_IRQ_LINES)
/*TODO*///	{
/*TODO*///		LOG(("cpu_irq_line_vector_w(%d,%d,$%04x)\n",cpunum,irqline,vector));
/*TODO*///		irq_line_vector[cpunum][irqline] = vector;
/*TODO*///		return;
/*TODO*///	}
/*TODO*///	LOG(("cpu_irq_line_vector_w CPU#%d irqline %d > max irq lines\n", cpunum, irqline));
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Generate a IRQ interrupt
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void cpu_manualirqcallback(int param)
/*TODO*///{
/*TODO*///	int cpunum = param & 0x0f;
/*TODO*///	int state = (param >> 4) & 0x0f;
/*TODO*///	int irqline = (param >> 8) & 0x7f;
/*TODO*///	int set_vector = (param >> 15) & 0x01;
/*TODO*///	int vector = param >> 16;
/*TODO*///
/*TODO*///	LOG(("cpu_manualirqcallback %d,%d,%d\n",cpunum,irqline,state));
/*TODO*///
/*TODO*///	/* swap to the CPU's context */
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///
/*TODO*///	/* set the IRQ line state and vector */
/*TODO*///	if (irqline >= 0 && irqline < MAX_IRQ_LINES)
/*TODO*///	{
/*TODO*///		irq_line_state[cpunum][irqline] = state;
/*TODO*///		if (set_vector)
/*TODO*///			irq_line_vector[cpunum][irqline] = vector;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* switch off the requested state */
/*TODO*///	switch (state)
/*TODO*///	{
/*TODO*///		case PULSE_LINE:
/*TODO*///			activecpu_set_irq_line(irqline, INTERNAL_ASSERT_LINE);
/*TODO*///			activecpu_set_irq_line(irqline, INTERNAL_CLEAR_LINE);
/*TODO*///			break;
/*TODO*///
/*TODO*///		case HOLD_LINE:
/*TODO*///		case ASSERT_LINE:
/*TODO*///			activecpu_set_irq_line(irqline, INTERNAL_ASSERT_LINE);
/*TODO*///			break;
/*TODO*///
/*TODO*///		case CLEAR_LINE:
/*TODO*///			activecpu_set_irq_line(irqline, INTERNAL_CLEAR_LINE);
/*TODO*///			break;
/*TODO*///
/*TODO*///		default:
/*TODO*///			logerror("cpu_manualirqcallback cpu #%d, line %d, unknown state %d\n", cpunum, irqline, state);
/*TODO*///	}
/*TODO*///	cpuintrf_pop_context();
/*TODO*///
/*TODO*///	/* generate a trigger to unsuspend any CPUs waiting on the interrupt */
/*TODO*///	if (state != CLEAR_LINE)
/*TODO*///		cpu_triggerint(cpunum);
/*TODO*///}
/*TODO*///
/*TODO*///
    public static void cpu_set_irq_line(int cpunum, int irqline, int state) {
        throw new UnsupportedOperationException("Unsupported");
        /*TODO*///	int vector = 0xff;
/*TODO*///
/*TODO*///	/* don't trigger interrupts on suspended CPUs */
/*TODO*///	if (cpu_getstatus(cpunum) == 0)
/*TODO*///		return;
/*TODO*///
/*TODO*///	/* determine the current vector */
/*TODO*///	if (irqline >= 0 && irqline < MAX_IRQ_LINES)
/*TODO*///		vector = irq_line_vector[cpunum][irqline];
/*TODO*///
/*TODO*///	LOG(("cpu_set_irq_line(%d,%d,%d,%02x)\n", cpunum, irqline, state, vector));
/*TODO*///
/*TODO*///	/* set a timer to go off */
/*TODO*///	timer_set(TIME_NOW, (cpunum & 0x0f) | ((state & 0x0f) << 4) | ((irqline & 0x7f) << 8), cpu_manualirqcallback);
    }

    /*TODO*///
/*TODO*///void cpu_set_irq_line_and_vector(int cpunum, int irqline, int state, int vector)
/*TODO*///{
/*TODO*///	/* don't trigger interrupts on suspended CPUs */
/*TODO*///	if (cpu_getstatus(cpunum) == 0)
/*TODO*///		return;
/*TODO*///
/*TODO*///	LOG(("cpu_set_irq_line(%d,%d,%d,%02x)\n", cpunum, irqline, state, vector));
/*TODO*///
/*TODO*///	/* set a timer to go off */
/*TODO*///	timer_set(TIME_NOW, (cpunum & 0x0f) | ((state & 0x0f) << 4) | ((irqline & 0x7f) << 8) | (1 << 15) | (vector << 16), cpu_manualirqcallback);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///#pragma mark -
/*TODO*///#pragma mark OBSOLETE INTERRUPT HANDLING
/*TODO*///#endif
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Old-style interrupt generation
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
    public static void cpu_cause_interrupt(int cpunum, int type) {
        throw new UnsupportedOperationException("Unsupported");
        /*TODO*///	/* special case for none */
/*TODO*///	if (type == INTERRUPT_NONE)
/*TODO*///		return;
/*TODO*///
/*TODO*///	/* special case for NMI type */
/*TODO*///	else if (type == INTERRUPT_NMI)
/*TODO*///		cpu_set_irq_line(cpunum, IRQ_LINE_NMI, PULSE_LINE);
/*TODO*///
/*TODO*///	/* otherwise, convert to an IRQ */
/*TODO*///	else
/*TODO*///	{
/*TODO*///		int vector, irqline;
/*TODO*///		irqline = convert_type_to_irq_line(cpunum, type, &vector);
/*TODO*///		cpu_set_irq_line_and_vector(cpunum, irqline, HOLD_LINE, vector);
/*TODO*///	}
    }
    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Interrupt enabling
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void cpu_clearintcallback(int cpunum)
/*TODO*///{
/*TODO*///	int irqcount = cputype_get_interface(Machine->drv->cpu[cpunum].cpu_type & ~CPU_FLAGS_MASK)->num_irqs;
/*TODO*///	int irqline;
/*TODO*///
/*TODO*///	cpuintrf_push_context(cpunum);
/*TODO*///
/*TODO*///	/* clear NMI and all IRQs */
/*TODO*///	activecpu_set_irq_line(IRQ_LINE_NMI, INTERNAL_CLEAR_LINE);
/*TODO*///	for (irqline = 0; irqline < irqcount; irqline++)
/*TODO*///		activecpu_set_irq_line(irqline, INTERNAL_CLEAR_LINE);
/*TODO*///
/*TODO*///	cpuintrf_pop_context();
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///void cpu_interrupt_enable(int cpunum,int enabled)
/*TODO*///{
/*TODO*///	interrupt_enable[cpunum] = enabled;
/*TODO*///
/*TODO*///LOG(("CPU#%d interrupt_enable=%d\n", cpunum, enabled));
/*TODO*///
/*TODO*///	/* make sure there are no queued interrupts */
/*TODO*///	if (enabled == 0)
/*TODO*///		timer_set(TIME_NOW, cpunum, cpu_clearintcallback);
/*TODO*///}
/*TODO*///
/*TODO*///
    public static WriteHandlerPtr interrupt_enable_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///	VERIFY_ACTIVECPU_VOID(interrupt_enable_w);
/*TODO*///	cpu_interrupt_enable(activecpu, data);
        }
    };
    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///WRITE_HANDLER( interrupt_vector_w )
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU_VOID(interrupt_vector_w);
/*TODO*///	if (interrupt_vector[activecpu] != data)
/*TODO*///	{
/*TODO*///		LOG(("CPU#%d interrupt_vector_w $%02x\n", activecpu, data));
/*TODO*///		interrupt_vector[activecpu] = data;
/*TODO*///
/*TODO*///		/* make sure there are no queued interrupts */
/*TODO*///		timer_set(TIME_NOW, activecpu, cpu_clearintcallback);
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Interrupt generation callbacks
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
    public static InterruptPtr interrupt = new InterruptPtr() {
        public int handler() {
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///	int val = 0;
/*TODO*///
/*TODO*///	VERIFY_ACTIVECPU(INTERRUPT_NONE, interrupt);
/*TODO*///
/*TODO*///	if (interrupt_enable[activecpu] == 0)
/*TODO*///		return INTERRUPT_NONE;
/*TODO*///
/*TODO*///	val = activecpu_default_irq_line();
/*TODO*///	return (val == -1000) ? interrupt_vector[activecpu] : val;
        }
    };
    public static InterruptPtr nmi_interrupt = new InterruptPtr() {
        public int handler() {
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///	VERIFY_ACTIVECPU(INTERRUPT_NONE, nmi_interrupt);
/*TODO*///
/*TODO*///LOG(("nmi_interrupt: interrupt_enable[%d]=%d\n", activecpu, interrupt_enable[activecpu]));
/*TODO*///	if (interrupt_enable[activecpu])
/*TODO*///		cpu_set_nmi_line(activecpu, PULSE_LINE);
/*TODO*///	return INTERRUPT_NONE;
        }
    };
    public static InterruptPtr ignore_interrupt = new InterruptPtr() {
        public int handler() {
            throw new UnsupportedOperationException("Unsupported");
            /*TODO*///	VERIFY_ACTIVECPU(INTERRUPT_NONE, ignore_interrupt);
/*TODO*///	return INTERRUPT_NONE;
        }
    };

    /*TODO*///
/*TODO*///
/*TODO*///#if (HAS_M68000 || HAS_M68010 || HAS_M68020 || HAS_M68EC020)
/*TODO*///
/*TODO*///INLINE int m68_irq(int level)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU(INTERRUPT_NONE, m68_irq);
/*TODO*///	if (interrupt_enable[activecpu])
/*TODO*///	{
/*TODO*///		cpu_irq_line_vector_w(activecpu, level, MC68000_INT_ACK_AUTOVECTOR);
/*TODO*///		cpu_set_irq_line(activecpu, level, HOLD_LINE);
/*TODO*///	}
/*TODO*///	return INTERRUPT_NONE;
/*TODO*///}
/*TODO*///
/*TODO*///int m68_level1_irq(void) { return m68_irq(1); }
/*TODO*///int m68_level2_irq(void) { return m68_irq(2); }
/*TODO*///int m68_level3_irq(void) { return m68_irq(3); }
/*TODO*///int m68_level4_irq(void) { return m68_irq(4); }
/*TODO*///int m68_level5_irq(void) { return m68_irq(5); }
/*TODO*///int m68_level6_irq(void) { return m68_irq(6); }
/*TODO*///int m68_level7_irq(void) { return m68_irq(7); }
/*TODO*///
/*TODO*///#endif
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///#pragma mark -
/*TODO*///#pragma mark SYNCHRONIZATION
/*TODO*///#endif
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Generate a specific trigger
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_trigger(int trigger)
/*TODO*///{
/*TODO*///	timer_trigger(trigger);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Generate a trigger in the future
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_triggertime(double duration, int trigger)
/*TODO*///{
/*TODO*///	timer_set(duration, trigger, cpu_trigger);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Generate a trigger for an int
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_triggerint(int cpunum)
/*TODO*///{
/*TODO*///	timer_trigger(TRIGGER_INT + cpunum);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Burn/yield CPU cycles until a trigger
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_spinuntil_trigger(int trigger)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU_VOID(cpu_spinuntil_trigger);
/*TODO*///	timer_suspendcpu_trigger(activecpu, trigger);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///void cpu_yielduntil_trigger(int trigger)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU_VOID(cpu_yielduntil_trigger);
/*TODO*///	timer_holdcpu_trigger(activecpu, trigger);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Burn/yield CPU cycles until an
/*TODO*/// *	interrupt
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_spinuntil_int(void)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU_VOID(cpu_spinuntil_int);
/*TODO*///	cpu_spinuntil_trigger(TRIGGER_INT + activecpu);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///void cpu_yielduntil_int(void)
/*TODO*///{
/*TODO*///	VERIFY_ACTIVECPU_VOID(cpu_yielduntil_int);
/*TODO*///	cpu_yielduntil_trigger(TRIGGER_INT + activecpu);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Burn/yield CPU cycles until the
/*TODO*/// *	end of the current timeslice
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_spin(void)
/*TODO*///{
/*TODO*///	cpu_spinuntil_trigger(TRIGGER_TIMESLICE);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///void cpu_yield(void)
/*TODO*///{
/*TODO*///	cpu_yielduntil_trigger(TRIGGER_TIMESLICE);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Burn/yield CPU cycles for a
/*TODO*/// *	specific period of time
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///void cpu_spinuntil_time(double duration)
/*TODO*///{
/*TODO*///	static int timetrig = 0;
/*TODO*///
/*TODO*///	cpu_spinuntil_trigger(TRIGGER_SUSPENDTIME + timetrig);
/*TODO*///	cpu_triggertime(duration, TRIGGER_SUSPENDTIME + timetrig);
/*TODO*///	timetrig = (timetrig + 1) & 255;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///void cpu_yielduntil_time(double duration)
/*TODO*///{
/*TODO*///	static int timetrig = 0;
/*TODO*///
/*TODO*///	cpu_yielduntil_trigger(TRIGGER_YIELDTIME + timetrig);
/*TODO*///	cpu_triggertime(duration, TRIGGER_YIELDTIME + timetrig);
/*TODO*///	timetrig = (timetrig + 1) & 255;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///#if 0
/*TODO*///#pragma mark -
/*TODO*///#pragma mark CORE TIMING
/*TODO*///#endif
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Returns the number of times the
/*TODO*/// *	interrupt handler will be called
/*TODO*/// *	before the end of the current
/*TODO*/// *	video frame.
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////*--------------------------------------------------------------
/*TODO*///
/*TODO*///	This can be useful to interrupt handlers to synchronize
/*TODO*///	their operation. If you call this from outside an interrupt
/*TODO*///	handler, add 1 to the result, i.e. if it returns 0, it means
/*TODO*///	that the interrupt handler will be called once.
/*TODO*///
/*TODO*///--------------------------------------------------------------*/
/*TODO*///
    public static int cpu_getiloops() {
        throw new UnsupportedOperationException("Unsupported");
        /*TODO*///	VERIFY_ACTIVECPU(0, cpu_getiloops);
/*TODO*///	return cpu[activecpu].iloops;
    }
    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Hook for updating things on the
/*TODO*/// *	real VBLANK (once per frame)
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void cpu_vblankreset(void)
/*TODO*///{
/*TODO*///	int cpunum;
/*TODO*///
/*TODO*///	/* read hi scores from disk */
/*TODO*///	hs_update();
/*TODO*///
/*TODO*///	/* read keyboard & update the status of the input ports */
/*TODO*///	update_input_ports();
/*TODO*///
/*TODO*///	/* reset the cycle counters */
/*TODO*///	for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///	{
/*TODO*///		if (!timer_iscpususpended(cpunum, SUSPEND_REASON_DISABLE))
/*TODO*///			cpu[cpunum].iloops = Machine->drv->cpu[cpunum].vblank_interrupts_per_frame - 1;
/*TODO*///		else
/*TODO*///			cpu[cpunum].iloops = -1;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	First-run callback for VBLANKs
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void cpu_firstvblankcallback(int param)
/*TODO*///{
/*TODO*///	/* now that we're synced up, pulse from here on out */
/*TODO*///	vblank_timer = timer_pulse(vblank_period, param, cpu_vblankcallback);
/*TODO*///
/*TODO*///	/* but we need to call the standard routine as well */
/*TODO*///	cpu_vblankcallback(param);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	VBLANK core handler
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void cpu_vblankcallback(int param)
/*TODO*///{
/*TODO*///	int cpunum;
/*TODO*///
/*TODO*///	/* loop over CPUs */
/*TODO*///	for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///	{
/*TODO*///		/* if the interrupt multiplier is valid */
/*TODO*///		if (cpu[cpunum].vblankint_multiplier != -1)
/*TODO*///		{
/*TODO*///			/* decrement; if we hit zero, generate the interrupt and reset the countdown */
/*TODO*///			if (!--cpu[cpunum].vblankint_countdown)
/*TODO*///			{
/*TODO*///				/* a param of -1 means don't call any callbacks */
/*TODO*///				if (param != -1)
/*TODO*///				{
/*TODO*///					/* if the CPU has a VBLANK handler, call it */
/*TODO*///					if (Machine->drv->cpu[cpunum].vblank_interrupt && cpu_getstatus(cpunum))
/*TODO*///					{
/*TODO*///						cpuintrf_push_context(cpunum);
/*TODO*///						cpu_cause_interrupt(cpunum, (*Machine->drv->cpu[cpunum].vblank_interrupt)());
/*TODO*///						cpuintrf_pop_context();
/*TODO*///					}
/*TODO*///
/*TODO*///					/* update the counters */
/*TODO*///					cpu[cpunum].iloops--;
/*TODO*///				}
/*TODO*///
/*TODO*///				/* reset the countdown and timer */
/*TODO*///				cpu[cpunum].vblankint_countdown = cpu[cpunum].vblankint_multiplier;
/*TODO*///				timer_reset(cpu[cpunum].vblankint_timer, TIME_NEVER);
/*TODO*///			}
/*TODO*///		}
/*TODO*///
/*TODO*///		/* else reset the VBLANK timer if this is going to be a real VBLANK */
/*TODO*///		else if (vblank_countdown == 1)
/*TODO*///			timer_reset(cpu[cpunum].vblankint_timer, TIME_NEVER);
/*TODO*///	}
/*TODO*///
/*TODO*///	/* is it a real VBLANK? */
/*TODO*///	if (!--vblank_countdown)
/*TODO*///	{
/*TODO*///		/* do we update the screen now? */
/*TODO*///		if (!(Machine->drv->video_attributes & VIDEO_UPDATE_AFTER_VBLANK))
/*TODO*///			time_to_quit = updatescreen();
/*TODO*///
/*TODO*///		/* Set the timer to update the screen */
/*TODO*///		timer_set(TIME_IN_USEC(Machine->drv->vblank_duration), 0, cpu_updatecallback);
/*TODO*///		vblank = 1;
/*TODO*///
/*TODO*///		/* reset the globals */
/*TODO*///		cpu_vblankreset();
/*TODO*///
/*TODO*///		/* reset the counter */
/*TODO*///		vblank_countdown = vblank_multiplier;
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	End-of-VBLANK callback
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void cpu_updatecallback(int param)
/*TODO*///{
/*TODO*///	/* update the screen if we didn't before */
/*TODO*///	if (Machine->drv->video_attributes & VIDEO_UPDATE_AFTER_VBLANK)
/*TODO*///		time_to_quit = updatescreen();
/*TODO*///	vblank = 0;
/*TODO*///
/*TODO*///	/* update IPT_VBLANK input ports */
/*TODO*///	inputport_vblank_end();
/*TODO*///
/*TODO*///	/* check the watchdog */
/*TODO*///	if (watchdog_counter > 0)
/*TODO*///		if (--watchdog_counter == 0)
/*TODO*///		{
/*TODO*///			logerror("reset caused by the watchdog\n");
/*TODO*///			machine_reset();
/*TODO*///		}
/*TODO*///
/*TODO*///	/* track total frames */
/*TODO*///	current_frame++;
/*TODO*///
/*TODO*///	/* reset the refresh timer */
/*TODO*///	timer_reset(refresh_timer, TIME_NEVER);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Callback for timed interrupts
/*TODO*/// *	(not tied to a VBLANK)
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void cpu_timedintcallback(int param)
/*TODO*///{
/*TODO*///	/* bail if there is no routine */
/*TODO*///	if (Machine->drv->cpu[param].timed_interrupt && cpu_getstatus(param))
/*TODO*///	{
/*TODO*///		cpuintrf_push_context(param);
/*TODO*///		cpu_cause_interrupt(param, (*Machine->drv->cpu[param].timed_interrupt)());
/*TODO*///		cpuintrf_pop_context();
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Converts an integral timing rate
/*TODO*/// *	into a period
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*////*--------------------------------------------------------------
/*TODO*///
/*TODO*///	Rates can be specified as follows:
/*TODO*///
/*TODO*///		rate <= 0		-> 0
/*TODO*///		rate < 50000	-> 'rate' cycles per frame
/*TODO*///		rate >= 50000	-> 'rate' nanoseconds
/*TODO*///
/*TODO*///--------------------------------------------------------------*/
/*TODO*///
/*TODO*///static double cpu_computerate(int value)
/*TODO*///{
/*TODO*///	/* values equal to zero are zero */
/*TODO*///	if (value <= 0)
/*TODO*///		return 0.0;
/*TODO*///
/*TODO*///	/* values above between 0 and 50000 are in Hz */
/*TODO*///	if (value < 50000)
/*TODO*///		return TIME_IN_HZ(value);
/*TODO*///
/*TODO*///	/* values greater than 50000 are in nanoseconds */
/*TODO*///	else
/*TODO*///		return TIME_IN_NSEC(value);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Callback to force a timeslice
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void cpu_timeslicecallback(int param)
/*TODO*///{
/*TODO*///	timer_trigger(TRIGGER_TIMESLICE);
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////*************************************
/*TODO*/// *
/*TODO*/// *	Setup all the core timers
/*TODO*/// *
/*TODO*/// *************************************/
/*TODO*///
/*TODO*///static void cpu_inittimers(void)
/*TODO*///{
/*TODO*///	double first_time;
/*TODO*///	int cpunum, max, ipf;
/*TODO*///
/*TODO*///	/* remove old timers */
/*TODO*///	if (timeslice_timer)
/*TODO*///		timer_remove(timeslice_timer);
/*TODO*///	if (refresh_timer)
/*TODO*///		timer_remove(refresh_timer);
/*TODO*///	if (vblank_timer)
/*TODO*///		timer_remove(vblank_timer);
/*TODO*///
/*TODO*///	/* allocate a dummy timer at the minimum frequency to break things up */
/*TODO*///	ipf = Machine->drv->cpu_slices_per_frame;
/*TODO*///	if (ipf <= 0)
/*TODO*///		ipf = 1;
/*TODO*///	timeslice_period = TIME_IN_HZ(Machine->drv->frames_per_second * ipf);
/*TODO*///	timeslice_timer = timer_pulse(timeslice_period, 0, cpu_timeslicecallback);
/*TODO*///
/*TODO*///	/* allocate an infinite timer to track elapsed time since the last refresh */
/*TODO*///	refresh_period = TIME_IN_HZ(Machine->drv->frames_per_second);
/*TODO*///	refresh_period_inv = 1.0 / refresh_period;
/*TODO*///	refresh_timer = timer_set(TIME_NEVER, 0, NULL);
/*TODO*///
/*TODO*///	/* while we're at it, compute the scanline times */
/*TODO*///	if (Machine->drv->vblank_duration)
/*TODO*///		scanline_period = (refresh_period - TIME_IN_USEC(Machine->drv->vblank_duration)) /
/*TODO*///				(double)(Machine->visible_area.max_y - Machine->visible_area.min_y + 1);
/*TODO*///	else
/*TODO*///		scanline_period = refresh_period / (double)Machine->drv->screen_height;
/*TODO*///	scanline_period_inv = 1.0 / scanline_period;
/*TODO*///
/*TODO*///	/*
/*TODO*///	 *	The following code finds all the CPUs that are interrupting in sync with the VBLANK
/*TODO*///	 *	and sets up the VBLANK timer to run at the minimum number of cycles per frame in
/*TODO*///	 *	order to service all the synced interrupts
/*TODO*///	 */
/*TODO*///
/*TODO*///	/* find the CPU with the maximum interrupts per frame */
/*TODO*///	max = 1;
/*TODO*///	for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///	{
/*TODO*///		ipf = Machine->drv->cpu[cpunum].vblank_interrupts_per_frame;
/*TODO*///		if (ipf > max)
/*TODO*///			max = ipf;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* now find the LCD with the rest of the CPUs (brute force - these numbers aren't huge) */
/*TODO*///	vblank_multiplier = max;
/*TODO*///	while (1)
/*TODO*///	{
/*TODO*///		for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///		{
/*TODO*///			ipf = Machine->drv->cpu[cpunum].vblank_interrupts_per_frame;
/*TODO*///			if (ipf > 0 && (vblank_multiplier % ipf) != 0)
/*TODO*///				break;
/*TODO*///		}
/*TODO*///		if (cpunum == cpu_gettotalcpu())
/*TODO*///			break;
/*TODO*///		vblank_multiplier += max;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* initialize the countdown timers and intervals */
/*TODO*///	for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///	{
/*TODO*///		ipf = Machine->drv->cpu[cpunum].vblank_interrupts_per_frame;
/*TODO*///		if (ipf > 0)
/*TODO*///			cpu[cpunum].vblankint_countdown = cpu[cpunum].vblankint_multiplier = vblank_multiplier / ipf;
/*TODO*///		else
/*TODO*///			cpu[cpunum].vblankint_countdown = cpu[cpunum].vblankint_multiplier = -1;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* allocate a vblank timer at the frame rate * the LCD number of interrupts per frame */
/*TODO*///	vblank_period = TIME_IN_HZ(Machine->drv->frames_per_second * vblank_multiplier);
/*TODO*///	vblank_timer = timer_pulse(vblank_period, 0, cpu_vblankcallback);
/*TODO*///	vblank_countdown = vblank_multiplier;
/*TODO*///
/*TODO*///	/*
/*TODO*///	 *		The following code creates individual timers for each CPU whose interrupts are not
/*TODO*///	 *		synced to the VBLANK, and computes the typical number of cycles per interrupt
/*TODO*///	 */
/*TODO*///
/*TODO*///	/* start the CPU interrupt timers */
/*TODO*///	for (cpunum = 0; cpunum < cpu_gettotalcpu(); cpunum++)
/*TODO*///	{
/*TODO*///		ipf = Machine->drv->cpu[cpunum].vblank_interrupts_per_frame;
/*TODO*///
/*TODO*///		/* remove old timers */
/*TODO*///		if (cpu[cpunum].vblankint_timer)
/*TODO*///			timer_remove(cpu[cpunum].vblankint_timer);
/*TODO*///		if (cpu[cpunum].timedint_timer)
/*TODO*///			timer_remove(cpu[cpunum].timedint_timer);
/*TODO*///
/*TODO*///		/* compute the average number of cycles per interrupt */
/*TODO*///		if (ipf <= 0)
/*TODO*///			ipf = 1;
/*TODO*///		cpu[cpunum].vblankint_period = TIME_IN_HZ(Machine->drv->frames_per_second * ipf);
/*TODO*///		cpu[cpunum].vblankint_timer = timer_set(TIME_NEVER, 0, NULL);
/*TODO*///
/*TODO*///		/* see if we need to allocate a CPU timer */
/*TODO*///		ipf = Machine->drv->cpu[cpunum].timed_interrupts_per_second;
/*TODO*///		if (ipf)
/*TODO*///		{
/*TODO*///			cpu[cpunum].timedint_period = cpu_computerate(ipf);
/*TODO*///			cpu[cpunum].timedint_timer = timer_pulse(cpu[cpunum].timedint_period, cpunum, cpu_timedintcallback);
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* note that since we start the first frame on the refresh, we can't pulse starting
/*TODO*///	   immediately; instead, we back up one VBLANK period, and inch forward until we hit
/*TODO*///	   positive time. That time will be the time of the first VBLANK timer callback */
/*TODO*///	timer_remove(vblank_timer);
/*TODO*///
/*TODO*///	first_time = -TIME_IN_USEC(Machine->drv->vblank_duration) + vblank_period;
/*TODO*///	while (first_time < 0)
/*TODO*///	{
/*TODO*///		cpu_vblankcallback(-1);
/*TODO*///		first_time += vblank_period;
/*TODO*///	}
/*TODO*///	vblank_timer = timer_set(first_time, 0, cpu_firstvblankcallback);
/*TODO*///}
/*TODO*///    
}
