// one pole filter class for the client side
// useful to smooth out values

SinglePole {
  var <>cutoff, <>sr, <output, <a0, <b1;

  *new {|cutoff = 100, sr = 1024|
    ^super.newCopyArgs(cutoff, sr).init;
  }

  init {
    output = 0; // initialize x
    this.setWithCutoffFreq(cutoff);
  }

  compute {|input|
    // output = output + (cutoff*(input-output)); // calculate
    output = (input*a0) + (output*b1);
    ^output; // return the output
  }

}

SinglePoleLP : SinglePole {

  *new {|cutoff = 100, sr = 1024|
    ^super.newCopyArgs(cutoff, sr).init;
  }

  setWithCutoffFreq {|freq = 10|
    // be mindful of the samplerate (sr)
    freq = freq/sr;
    b1 = exp(-2*pi*freq); // set it with frequency
    a0 = 1 - b1.abs;
  }

  setWithTimeConstant {|time = 0.1|
    // be mindful of the samplerate (sr)
    b1 = exp(-1/(sr*time)); // set it with time
    a0 = 1 - b1.abs;
  }

}

SinglePoleHP : SinglePole {

  *new {|cutoff = 100, sr = 1024|
    ^super.newCopyArgs(cutoff, sr).init;
  }

  setWithCutoffFreq {|freq = 10|
    // be mindful of the samplerate (sr)
    freq = freq/sr;
    b1 = -1 * exp(-2*pi*(0.5-freq)); // set it with frequency
    a0 = 1 + b1.abs;
  }

  setWithTimeConstant {|time = 0.1|
    // be mindful of the samplerate (sr)
    b1 = -1 * exp(-1/(sr*time)); // set it with time
    a0 = 1 + b1.abs;
  }

}
