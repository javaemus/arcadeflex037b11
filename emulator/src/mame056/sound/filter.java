/**
 * Ported to v0.56
 */
package mame056.sound;

import static mame056.sound.filterH.*;

public class filter {

    /*TODO*///static filter* filter_alloc(void) {
/*TODO*///	filter* f = malloc(sizeof(filter));
/*TODO*///	return f;
/*TODO*///}
/*TODO*///
/*TODO*///void filter_free(filter* f) {
/*TODO*///	free(f);
/*TODO*///}
/*TODO*///
/*TODO*///void filter_state_reset(filter* f, filter_state* s) {
/*TODO*///	int i;
/*TODO*///	s->prev_mac = 0;
/*TODO*///	for(i=0;i<f->order;++i) {
/*TODO*///		s->xprev[i] = 0;
/*TODO*///	}
/*TODO*///}
/*TODO*///
    public static filter_state filter_state_alloc() {
        int i;
        filter_state s = new filter_state();
        s.prev_mac = 0;
        for (i = 0; i < FILTER_ORDER_MAX; ++i) {
            s.xprev[i] = 0;
        }
        return s;
    }
    /*TODO*///
/*TODO*///void filter_state_free(filter_state* s) {
/*TODO*///	free(s);
/*TODO*///}
/*TODO*///
/*TODO*////****************************************************************************/
/*TODO*////* FIR */
/*TODO*///
/*TODO*///filter_real filter_compute(filter* f, filter_state* s) {
/*TODO*///	unsigned order = f->order;
/*TODO*///	unsigned midorder = f->order / 2;
/*TODO*///	filter_real y = 0;
/*TODO*///	unsigned i,j,k;
/*TODO*///
/*TODO*///	/* i == [0] */
/*TODO*///	/* j == [-2*midorder] */
/*TODO*///	i = s->prev_mac;
/*TODO*///	j = i + 1;
/*TODO*///	if (j == order)
/*TODO*///		j = 0;
/*TODO*///
/*TODO*///	/* x */
/*TODO*///	for(k=0;k<midorder;++k) {
/*TODO*///		y += f->xcoeffs[midorder-k] * (s->xprev[i] + s->xprev[j]);
/*TODO*///		++j;
/*TODO*///		if (j == order)
/*TODO*///			j = 0;
/*TODO*///		if (i == 0)
/*TODO*///			i = order - 1;
/*TODO*///		else
/*TODO*///			--i;
/*TODO*///	}
/*TODO*///	y += f->xcoeffs[0] * s->xprev[i];
/*TODO*///
/*TODO*///#ifdef FILTER_USE_INT
/*TODO*///	return y >> FILTER_INT_FRACT;
/*TODO*///#else
/*TODO*///	return y;
/*TODO*///#endif
/*TODO*///}
/*TODO*///
/*TODO*///filter* filter_lp_fir_alloc(double freq, int order) {
/*TODO*///	filter* f = filter_alloc();
/*TODO*///	unsigned midorder = (order - 1) / 2;
/*TODO*///	unsigned i;
/*TODO*///	double gain;
/*TODO*///
/*TODO*///	assert( order <= FILTER_ORDER_MAX );
/*TODO*///	assert( order % 2 == 1 );
/*TODO*///	assert( 0 < freq && freq <= 0.5 );
/*TODO*///
/*TODO*///	/* Compute the antitrasform of the perfect low pass filter */
/*TODO*///	gain = 2*freq;
/*TODO*///#ifdef FILTER_USE_INT
/*TODO*///	f->xcoeffs[0] = gain * (1 << FILTER_INT_FRACT);
/*TODO*///#else
/*TODO*///	f->xcoeffs[0] = gain;
/*TODO*///#endif
/*TODO*///	for(i=1;i<=midorder;++i) {
/*TODO*///		/* number of the sample starting from 0 to (order-1) included */
/*TODO*///		unsigned n = i + midorder;
/*TODO*///
/*TODO*///		/* sample value */
/*TODO*///		double c = sin(2*M_PI*freq*i) / (M_PI*i);
/*TODO*///
/*TODO*///		/* apply only one window or none */
/*TODO*///		/* double w = 2 - 2*n/(order-1); */ /* Bartlett (triangular) */
/*TODO*///		/* double w = 0.5 * (1 - cos(2*M_PI*n/(order-1))); */ /* Hanning */
/*TODO*///		double w = 0.54 - 0.46 * cos(2*M_PI*n/(order-1)); /* Hamming */
/*TODO*///		/* double w = 0.42 - 0.5 * cos(2*M_PI*n/(order-1)) + 0.08 * cos(4*M_PI*n/(order-1)); */ /* Blackman */
/*TODO*///
/*TODO*///		/* apply the window */
/*TODO*///		c *= w;
/*TODO*///
/*TODO*///		/* update the gain */
/*TODO*///		gain += 2*c;
/*TODO*///
/*TODO*///		/* insert the coeff */
/*TODO*///#ifdef FILTER_USE_INT
/*TODO*///		f->xcoeffs[i] = c * (1 << FILTER_INT_FRACT);
/*TODO*///#else
/*TODO*///		f->xcoeffs[i] = c;
/*TODO*///#endif
/*TODO*///	}
/*TODO*///
/*TODO*///	/* adjust the gain to be exact 1.0 */
/*TODO*///	for(i=0;i<=midorder;++i) {
/*TODO*///#ifdef FILTER_USE_INT
/*TODO*///		f->xcoeffs[i] /= gain;
/*TODO*///#else
/*TODO*///		f->xcoeffs[i] = f->xcoeffs[i] * (double)(1 << FILTER_INT_FRAC) / gain;
/*TODO*///#endif
/*TODO*///	}
/*TODO*///
/*TODO*///	/* decrease the order if the last coeffs are 0 */
/*TODO*///	i = midorder;
/*TODO*///	while (i > 0 && f->xcoeffs[i] == 0.0)
/*TODO*///		--i;
/*TODO*///
/*TODO*///	f->order = i * 2 + 1;
/*TODO*///
/*TODO*///	return f;
/*TODO*///}    
}
