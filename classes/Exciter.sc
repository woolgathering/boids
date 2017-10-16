/*
	An exciter
*/

Exciter {

	*ar {|in, cutoff = 850, gain = 3, mul = 1, add = 0|
		var sig;
		cutoff = cutoff.min(SampleRate.ir*0.5); // limit the cutoff to the Nyquist. 0 will also make it explode.
		sig = HPF.ar(
			(HPF.ar(in, cutoff, mul: gain)).softclip,
			cutoff,
			mul: gain
		);
		^(sig*mul)+add;
	}

}