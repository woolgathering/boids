Distance {

  *ar {|in, radius = 0, density = 0|
    var hiCut, loCut, amp;
    amp = (1/radius.squared).clip(0, 1); 
    // in = HPF.ar(LPF.ar(in, loCut), hiCut);
    [amp, (10000/radius.clip(0.01, inf)).clip(20, 20000)].poll;
    in = amp * LPF.ar(in, (100000/radius.clip(0.01, inf)).clip(20, 20000)); 
    // in = amp * LPF.ar(in, (200000/radius.clip(0.01, inf)).clip(20, 23000));
    ^in;
  }

}
