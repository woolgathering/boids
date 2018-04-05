RunningRange {

  /*
    ///////////////////////////////////
    // RunningRange
    ///////////////////////////////////

    "Normalize" the range of an incoming signal to between 0 and 1. High and low are resettable independently.

    ISSUES: will sometimes output NaN's. Use NaNFilter to get rid of them (unless clipping range fixes it. Untested...)
  */

  *ar {|in, resetHi = 0, resetLow = 0|
    var min, max, range, out;
    min = RunningMin.ar(in, resetLow); // remember the min
    max = RunningMax.ar(in, resetHi); // remember the max
    range = (max-min).max(0.01); // get the range (max kills NaN's?)
    ^out = (in-min)/range; // normalize
  }

  *kr {|in, resetHi = 0, resetLow = 0|
    var min, max, range, out;
    min = RunningMin.kr(in, resetLow); // remember the min
    max = RunningMax.kr(in, resetHi); // remember the max
    range = (max-min).max(0.01); // get the range
    ^out = (in-min)/range; // normalize
  }

}
