TimeStretch {

  // stretch a sound in a buffer in the time domain

  *ar {|buff, rate = 1, trans = 1, winSize = 0.2, timeDisp = 0.2, start = 0, end = 1, mul = 1, add = 0|
    var sig, trig, pos;

    trig = Impulse.kr((winSize.reciprocal*50)); // dynamically change the trigger frequency according to winSize
    pos = Phasor.ar(0,
      rate: ((end-start) * rate * (BufDur.kr(buff)*(end-start)).reciprocal)/SampleRate.ir,
      start: start,
      end: end
    ); // read through the file at the correct rate for the correct distance

    sig = GrainBuf.ar(numChannels: 1,
      trigger: DelayN.kr(trig, winSize, TRand.kr(0,timeDisp,trig)), // randomly change the trigger to remove comb effect
      dur: winSize, sndbuf: buff, rate: trans, pos: pos, // other junk & try to minimize maxGrains
      interp: 4, pan: 0, envbufnum: -1, maxGrains: (winSize.reciprocal*50).min(512), mul: mul, add: add);

    ^sig; // return

  }

}

TimeStretchStereo {

  // stretch a sound in a buffer in the time domain

  *ar {|buffL, buffR, rate = 1, trans = 1, winSize = 0.2, timeDisp = 0.2, start = 0, end = 1, mul = 1, add = 0|
    var sigL, sigR, trig, pos;

    trig = Impulse.kr((winSize.reciprocal*50)); // dynamically change the trigger frequency according to winSize
    pos = Phasor.ar(0,
      rate: ((end-start) * rate * (BufDur.kr(buffL)*(end-start)).reciprocal)/SampleRate.ir,
      start: start,
      end: end
    ); // read through the file at the correct rate for the correct distance

    sigL = GrainBuf.ar(numChannels: 1,
      trigger: DelayN.kr(trig, winSize, TRand.kr(0,timeDisp,trig)), // randomly change the trigger to remove comb effect
      dur: winSize, sndbuf: buffL, rate: trans, pos: pos, // other junk & try to minimize maxGrains
      interp: 4, pan: 0, envbufnum: -1, maxGrains: (winSize.reciprocal*50).min(512), mul: mul, add: add);

    sigR = GrainBuf.ar(numChannels: 1,
      trigger: DelayN.kr(trig, winSize, TRand.kr(0,timeDisp,trig)), // randomly change the trigger to remove comb effect
      dur: winSize, sndbuf: buffR, rate: trans, pos: pos, // other junk & try to minimize maxGrains
      interp: 4, pan: 0, envbufnum: -1, maxGrains: (winSize.reciprocal*50).min(512), mul: mul, add: add);

    ^[sigL, sigR]; // return

  }

}
