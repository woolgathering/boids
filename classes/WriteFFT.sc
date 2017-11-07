// make a file of spectral data to be read offline. Saved as a 32-bit floating point WAV file.

WriteFFT {

  var <>buff, <>outPath, <>frameSize, <>hop, <>winType, <>winSize, <>rate, <>trans, sndBuff, <fftBuff, score, soundInfo, <server, freeables;

  *new {|buff, outPath, frameSize, hop, winType|
    ^super.newCopyArgs(buff, outPath, frameSize, hop, winType).init;
  }

  init {
    if(buff.isNil) {^"Need an input sound file!".error}; // return an error if our buff is nil
    server = Server.default;
    soundInfo = SoundFile.openRead(buff.path);
    outPath = outPath ? PathName.tmp++"fftData.scpv"; // output path
    frameSize = frameSize ? 1024; // fft frame size
    hop = hop ? 0.5; // hop size
    winType = winType ? 0; // -1: rectangular, 0: sine, 1: Hann
    winSize = 0.2;
    score = Score.new;
    freeables = List.new(0); // an empty list

    rate = rate ? 1;
    trans = trans ? 1;
  }

  process {
    this.makeScore;
    score.recordNRT(PathName.tmp++"tmp.osc", "/dev/null", inputFilePath: sndBuff.path, sampleRate: soundInfo.sampleRate,
      options: ServerOptions.new
      	.memSize_(32768)
      	.verbosity_(-1)
        .sampleRate_(soundInfo.sampleRate),
      action: {
        freeables.do(_.free); // free the freeables
      }
    );
  }

  makeBuffBundle {
    var bundle;
    bundle = server.makeBundle(false, {
      fftBuff = Buffer.alloc(server, soundInfo.duration.calcPVRecSize(frameSize, hop)); // buffer to store analysis
      sndBuff = Buffer.readChannel(server, buff.path, channels: [0]); // buffer containing the original sound.
      freeables.add(fftBuff, sndBuff);
    });
    ^bundle; // return the bundle
  }

  addSynthDef {
    var bundle;
    bundle = server.makeBundle(false, {
      SynthDef(\fftRec, {|sndBuff, recBuff, rate = 1, trans = 1, winSize = 0.2, frameSize = 1024, hop = 0.5, winType = 0|
        var sig, chain, localBuff;
        localBuff = LocalBuf(frameSize, 1); // a single channel
        Line.kr(1, 1, BufDur.kr(sndBuff) / rate, doneAction: 2); // a line to control the doneAction
        sig = TimeStretch.ar(sndBuff, rate, trans, winSize, timeDisp: winSize); // stretch it out (if we need to)
        chain = FFT(localBuff, in: sig, hop: hop, wintype: winType, active: 1); // analyze
        chain = PV_RecordBuf(chain, recBuff, offset: 0, run: 1, loop: 0, hop: hop, wintype: winType); // record it
      }).send(server);
    });
    ^bundle; // return the bundle
  }

  addSynth {
    var bundle;
    bundle = server.makeBundle(false, {
      Synth(\fftRec,
        [
          sndBuff: sndBuff, recBuff: fftBuff, rate: rate, trans: trans,
          winSize: winSize, frameSize: frameSize, hop: hop, winType: winType
        ]
      );
    });
    ^bundle;
  }

  writeFile {
    var bundle;
    bundle = server.makeBundle(false, {
      fftBuff.write(outPath, "wav", "float32"); // write it from inside the score
    });
    ^bundle; // return the bundle
  }

  // make the score. Maybe enable passing of arguments (editing of arguments) here in the future?
  makeScore {
    score.add(this.makeBuffBundle.addFirst(0.0)); // add the buffers
    score.add(this.addSynthDef.addFirst(0.0)); // add the SynthDef
    score.add(this.addSynth.addFirst(0.1)); // play the synth
    score.add(this.writeFile.addFirst((soundInfo.duration/rate)+0.5)); // write the file to disk
    score.add([[\c_set, 0, 0]].addFirst((soundInfo.duration/rate)+1)); // a dummy event to end the NRT processing
    score.sort; // just to be sure
  }

  free {
    soundInfo.close;
    // freeables.do(_.free);
  }

}
