b = Buffer.readChannel(s, "/home/roger/Documents/jacob/pieces/minDrom/sound/stems/chorus1.wav", channels: [0]).normalize;
b
{TimeStretch.ar(b, 1, 1, 0.2)*0.2}.play;
{PlayBuf.ar(1, b)*0.2}.play



f = WriteFFT.new(b, PathName.tmp++"TESTFFT.scpv");
f.outPath
f.rate = 0.5
f.rate
f.trans = 0.5
f.hop = 0.5
f.frameSize = 2048
f.process
f.winType
f.frameSize
f.hop

c = Buffer.read(s, "/tmp/TESTFFT.scpv");
c.plot

(
SynthDef("pvplay", { arg out=0, recBuf=1;
    var in, chain, bufnum;
    bufnum = LocalBuf.new(2048);
    chain = PV_PlayBuf(bufnum, recBuf, 1, 0, 0);
    Out.ar(out, IFFT(chain, 0).dup);
  }).send(s);
);
Synth("pvplay", [\out, 0, \recBuf, c]);


// quantiles
// potrace
-9.dbamp
