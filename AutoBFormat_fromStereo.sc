AutoBFormat_fromStereo {

  // *ar {|in, rotate, press|
  //   var sig;
  //   sig = in;
  //   // sig = BiPanB2.ar(in[0], in[1], azimuth: 0);
  //   // sig = FoaPressX.ar(sig, press.range(-pi/6, pi/6));
  //   // sig = FoaRotate.ar(sig, rotate.range(-pi,pi));
  //   ^sig;
  // }

  *ar {|in, rotate = 0, push = 0|
    var sig, left, right;
    left = FoaEncode.ar(in[0], FoaEncoderMatrix.newOmni);
    right = FoaEncode.ar(in[1], FoaEncoderMatrix.newOmni);

    left = FoaPushX.ar(left, push.range(-pi/6, pi/6) + Rand(-pi/2,pi/2)); // random offsets
    left = FoaRotate.ar(left, rotate.range(-pi,pi) + Rand(-pi,pi));

    right = FoaPushX.ar(right, push.range(-pi/6, pi/6) + Rand(-pi/2,pi/2));
    right = FoaRotate.ar(right, rotate.range(-pi,pi) + Rand(-pi,pi));

    sig = Mix.new([left, right]);
    ^sig;
  }

}