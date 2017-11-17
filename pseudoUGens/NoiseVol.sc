NoiseVol {

  *ar {|in, level|
    var noise;
    noise = WhiteNoise.ar(1); // try PinkNoise?
    noise = LPF.ar(LPF.ar(noise, level*400)); // low pass the noise
    noise = LPF.ar(noise*noise*500, 30).clip(0,1); // square, multiply by 500, lowpass again, clip
    ^in * noise.pow(0.5); // multiply
  }

}
