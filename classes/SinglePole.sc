// one pole filter class for the client side
// useful to smooth out values

SinglePole {
	var <>coeff, <>sr, <output;

	*new {|coeff = 0.999, sr = 1024|
		^super.newCopyArgs(coeff, sr).init;
	}

	init {
		output = 0; // initialize x
	}

	setWithTimeConstant {|time = 0.1|
		// be mindful of the samplerate (sr)
		coeff = 1 - exp(-1/(sr*time)); // set it with time
	}

	setWithCutoffFreq {|freq = 10|
		// be mindful of the samplerate (sr)
		coeff = 1 - exp(-2*pi*freq); // set it with frequency
	}

}

SinglePoleLP : SinglePole {

	*new {|coeff = 0.999, sr = 1024|
		^super.newCopyArgs(coeff, sr).init;
	}

	compute {|input|
		output = output + (coeff*(input-output)); // calculate
		^output; // return the output
	}

}

SinglePoleHP : SinglePole {

	*new {|coeff = 0.999, sr = 1024|
		^super.newCopyArgs(coeff, sr).init;
	}

	compute {|input|
		output = output + (coeff*(input+output)); // calculate
		^output; // return the output
	}

}
