DelayS {

  *ar {|in, samples = 5, mul = 1, add = 0|
    var delTime;
    delTime = samples/SampleRate.ir;
    in = DelayN.ar(in, delTime, delTime, mul, add);
    ^in;
  }

}