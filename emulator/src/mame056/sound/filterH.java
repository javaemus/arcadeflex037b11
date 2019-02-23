/**
 * Ported to v0.56
 */
package mame056.sound;

public class filterH {

    /* Max filter order */
    public static final int FILTER_ORDER_MAX = 51;

    /*TODO*///
/*TODO*////* Define to use interger calculation */
/*TODO*///#define FILTER_USE_INT
/*TODO*///
/*TODO*///#ifdef FILTER_USE_INT
/*TODO*///typedef int filter_real;
/*TODO*///#define FILTER_INT_FRACT 15 /* fractional bits */
/*TODO*///#else
/*TODO*///typedef double filter_real;
/*TODO*///#endif
/*TODO*///
/*TODO*///typedef struct filter_struct {
/*TODO*///	filter_real xcoeffs[(FILTER_ORDER_MAX+1)/2];
/*TODO*///	unsigned order;
/*TODO*///} filter;
/*TODO*///
    public static class filter_state {

        int/*unsigned*/ prev_mac;
        int[] xprev = new int[FILTER_ORDER_MAX];
    }
    /*TODO*///
/*TODO*////* Allocate a FIR Low Pass filter */
/*TODO*///filter* filter_lp_fir_alloc(double freq, int order);
/*TODO*///void filter_free(filter* f);
/*TODO*///
/*TODO*////* Allocate a filter state */
/*TODO*///filter_state* filter_state_alloc(void);
/*TODO*///
/*TODO*////* Free the filter state */
/*TODO*///void filter_state_free(filter_state* s);
/*TODO*///
/*TODO*////* Clear the filter state */
/*TODO*///void filter_state_reset(filter* f, filter_state* s);
/*TODO*///
/*TODO*////* Insert a value in the filter state */
/*TODO*///INLINE void filter_insert(filter* f, filter_state* s, filter_real x) {
/*TODO*///	/* next state */
/*TODO*///	++s->prev_mac;
/*TODO*///	if (s->prev_mac >= f->order)
/*TODO*///		s->prev_mac = 0;
/*TODO*///
/*TODO*///	/* set x[0] */
/*TODO*///	s->xprev[s->prev_mac] = x;
/*TODO*///}    
}
