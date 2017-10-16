SimpleCPU {

  classvar <>interval = 1, server;

  *new {|serverArg, intervalArg = 1|
    ^super.new.init(serverArg, intervalArg);
  }

  init {|serverArg, intervalArg|
    server = serverArg ?? {Server.default};
    interval = intervalArg;

    if (server.serverRunning) {
      ^this.make(server, interval);
    } {
      ^("ERROR: % not running!".format(server));
    }

  }

  make {
    var window, layout, avgCPU = 0, peakCPU = 0, avgCPUText, peakCPUText, routine;

    window = Window.new(resizable: false);
    layout = VLayout.new;
    avgCPUText = StaticText(window).string_((server.avgCPU.round(0.001) * 10).asString);
    peakCPUText = StaticText(window).string_((server.peakCPU.round(0.001) * 10).asString);
    
    layout.add(
      VLayout(
        HLayout([StaticText(window).string_("avgCPU: "), align: \left], [avgCPUText, align: \right]),
        HLayout([StaticText(window).string_("peakCPU: "), align: \left], [peakCPUText, align: \right]),
      )
    );
    window
    .layout_(layout)
    .bounds_(Rect(100,100,window.sizeHint.width+5, window.sizeHint.height+5))
    .onClose_({routine.stop; SkipJack.stopAll})
    .front;

    SkipJack.new({
        routine = Routine.run({
          loop {
            {
              avgCPUText.string_((server.avgCPU.round(0.001) * 10).asString);
              peakCPUText.string_((server.peakCPU.round(0.001) * 10).asString);
            }.defer;
            interval.wait;
          };
        });
      },
      interval,
      {window.isClosed}
    );



  }

}