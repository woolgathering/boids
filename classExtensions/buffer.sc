+ Buffer {

  *makeKernel {|server, path, start = 0, size = 1024, maxAmp = 1.0, winType = 'hann'|
    var win, real, imag, array, cosTable, scale, fft, buff, source;
    buff = Buffer.alloc(server, size); // allocate a buffer that will be returned
    // make a window
    switch (winType)
      {'hann'} {win = Signal.hanningWindow(size).as(Array)}
      {'hamm'} {win = Signal.hammingWindow(size).as(Array)}
      {'welch'} {win = Signal.welchWindow(size).as(Array)}
      {'rect'} {win = Signal.rectWindow(size).as(Array)};

    fork {
      source = Buffer.readChannel(server, path, channels: [0]).normalize;
      server.sync;
      // load what we want to a FloatArray, get the max magnitude, nomalize, then return a new buffer with our windowed and normalized result.
      source.loadToFloatArray(start, size, action: {|kernel|
        kernel = kernel - (kernel.sum/kernel.size); // remove DC
        kernel = kernel * win; // window it
        real = kernel.as(Signal); // for the real signal
        imag = Signal.newClear(kernel.size); // for imaginary numbers
        cosTable = Signal.fftCosTable(kernel.size); // a cosTable
        fft = fft(real, imag, cosTable); // do the FFT
        scale = fft.magnitude.maxItem.reciprocal * maxAmp; // calculate the scaling factor
        kernel = (kernel*scale).as(Array); // scale it
        buff.loadCollection(kernel); // load it into the buffer
        source.free; // free that buffer
        buff.updateInfo; // update things just in case
      });
    };
    ^buff; // return the allocated buffer
  }

  makeKernel {|start = 0, size = 1024, maxAmp = 1.0, winType = 'hann'|
    var win, real, imag, array, cosTable, scale, fft, buff;
    buff = Buffer.alloc(this.server, size); // allocate a buffer that will be returned
    // make a window
    switch (winType)
      {'hann'} {win = Signal.hanningWindow(size).as(Array)}
      {'hamm'} {win = Signal.hammingWindow(size).as(Array)}
      {'welch'} {win = Signal.welchWindow(size).as(Array)}
      {'rect'} {win = Signal.rectWindow(size).as(Array)};

    // load what we want to a FloatArray, get the max magnitude, nomalize, then return a new buffer with our windowed and normalized result.
    this.loadToFloatArray(start, size, action: {|kernel|
      kernel = kernel - (kernel.sum/kernel.size); // remove DC
      kernel = kernel * win; // window it
      real = kernel.as(Signal); // for the real signal
      imag = Signal.newClear(kernel.size); // for imaginary numbers
      cosTable = Signal.fftCosTable(kernel.size); // a cosTable
      fft = fft(real, imag, cosTable); // do the FFT
      scale = fft.magnitude.maxItem.reciprocal * maxAmp; // calculate the scaling factor
      kernel = (kernel*scale).as(Array); // scale it
      buff.loadCollection(kernel); // load it into the buffer
      buff.updateInfo; // update things just in case
    });
    ^buff; // return the allocated buffer
  }

}
