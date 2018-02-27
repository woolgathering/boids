RunningRange {

  *ar {|in, resetHi = 0, resetLow = 0|
    var min, max, range, out;
    min = RunningMin.ar(in, resetLow); // remember the min
    max = RunningMax.ar(in, resetHi); // remember the max
    range = max-min; // get the range
    ^out = (in-min)/range; // normalize
  }

  *kr {|in, resetHi = 0, resetLow = 0|
    var min, max, range, out;
    min = RunningMin.kr(in, resetLow); // remember the min
    max = RunningMax.kr(in, resetHi); // remember the max
    range = max-min; // get the range
    ^out = (in-min)/range; // normalize
  }

}
