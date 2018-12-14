/*
  Flock

  A helper class for using Boids2D as the spatializer. Helps keep all Boids classes abstact
  and apart from a spatialized implementation.

  For each BoidUnit, a single synth and bus routed to the input of that synth is created in a larger
  container group.
*/

Flock {
  var <numBoids, <numChans, <>outBus, <>timestep, <>server;
  var <minGain, <>width, <busses, <task, <>func, group, <boids, <synths, lastDist, <leader;
  var <window, layout, minIdx, halfCosTable;

  *new {|numBoids = 20, numChans = 4, outBus = 0, timestep = 0.1, server|
    ^super.newCopyArgs(numBoids, numChans, outBus, timestep, server).init;
  }

  init {
    server = server ? Server.local; // default to localhost if nothing is passed in
    if(server.serverRunning.not) {"Server is not running!".error; ^nil}; // yell if the server isn't running
    boids = Boids2D(numBoids, timestep: timestep); // make a Boids2D
    leader = Boids2D(1, timestep: timestep);
    boids.addTarget([0,0], 0.05); // add a target at the origin (5% attraction)
    boids.bounds = [20,20];
    minGain = -66; // minGain for the scaling function in amplitude
    width = 2; // how many speakers to width a signal between (see PanAz)
    lastDist = Array.fill(numBoids, {0}); // an array to hold previous distances

    this.prMakeBusses; // make the busses
    this.prMakeCosTable; // make the half cosine table
    this.prSquashCurve; // recalculate the curve for this minGain
    this.prMakeDefaultFunc; // set the default function
    this.prMakeTask; // make the task

    forkIfNeeded {
      server.loadDirectory(ReynoldsAlgorithms.synthDir++"boids"); // load the synthdefs
      server.sync;
      this.makeSynths; // make the synths
    };
  }

  // this can be called after CmdPeriod to redo everything without calling Flock.new again
  makeSynths {
    group = Group.head; // put the group at the tail
    group.register; // register this group
    synths = numBoids.collect{|i|
      Synth("boids2D_%chan".format(numChans).asSymbol, [inBus: busses[i], outBus: outBus], target: group);
      // Synth("boids2D_%chan".format(numChans).asSymbol, [inBus: busses[i], dryBus: dryBus, wetBus: wetBus, halfCosBuff: halfCosBuff], target: group);
    };
  }

  // moveTargetsWithFunc {|func|
  //   targetFunc = func; // set it
  //   this.prMakeDefaultFunc; // recalculate the function
  // }
  //
  // moveTargetsAsBoids {
  //   targetFlock = Boids2D(boids.targets.size, boids.timestep);
  //   targetFlock.bounds = [];
  //   targetFlock.useInnerBounds = true;
  //   targetFlock.innerBoundRatio = boids.innerBoundRatio;
  //
  //   targetFunc = targetFlock.moveFlock({|thisFlock|
  //       boids.targets.do{|target, i|
  //         boids.editTarget(i, position: thisFlock.boidsList[i].pos); // set the new position of the target
  //         // boids.editTarget(i, weight: rrand(weightMin, weightMax)); // also mess with the weighting
  //       };
  //     }); // move the flock
  // }

  // make a half cosine table
  prMakeCosTable {
    halfCosTable = 2048.collect{|i|
    	i = i*(2048.reciprocal);
    	// (sin((i*pi)-(pi*0.5))*0.5)+0.5; // ascending
      (cos(i*pi)*0.5)+0.5; // descending
    };
  }

  prMakeBusses {
    busses = numBoids.collect{Bus.audio(server, 1)}; // collect busses into an array
  }

  // squash the curve so we don't have to limit indicies or whatever
  prSquashCurve {
    var diff;
    diff = 1 - minGain.dbamp; // get the difference between unity and the minGain
    halfCosTable = halfCosTable.collect{|val, i|
      (val*diff)+minGain.dbamp; // recalculate the values
    };
    halfCosTable = halfCosTable.clip(0,1); // just to be safe
  }

  prMakeDefaultFunc {
    func = {|thisFlock|
      // iterate through the flock
      thisFlock.boidList.do{|boidUnit, i|
        var panVals, amp, vel, thisDist;
        panVals = boidUnit.getPanVals;
        thisDist = boidUnit.pos.dist(RealVector2D[0,0]); // get this distance (only do it once)
        // "Boid #%::\t%\n".postf(i, boidUnit.pos);
        // amp = panVals[1].explin(1,2048,0,minGain); // amp drops by 6dB when the distance doubles

        amp = halfCosTable.blendAt(thisDist.clip(0,2047)); // get the amp of the dry signal
        vel = lastDist[i] - thisDist; // calculate the velocity (for Doppler)
        lastDist[i] = thisDist; // save this distance
        // thisDist = (thisDist/100).clip(0,1); // normalize the distance by 100 meters

        server.makeBundle(nil, {
          synths[i].set(\pan, panVals[0].raddeg);
          synths[i].set(\amp, amp);
          // synths[i].set(\dryAmp, amp);
          // synths[i].set(\wetAmp, (1-amp).max(halfCosTable[minIdx.asInteger]));
          synths[i].set(\width, width);
          synths[i].set(\vel, vel);
          synths[i].set(\dist, thisDist);
          synths[i].set(\lag, timestep);
        });
      };
      // if there's a function to move the targets, do it
      // if(targetFunc.notNil) {
      //   targetFunc.(boids.targets); // passed the targets
      // };
      // if(obstanceFunc.notNil) {
      //   obstacleFunc.(boids.obstacles); // pass the obstacles
      // };
      // "\n".post;
    }
  }

  prMakeTask {
    task = Task({
      loop {
        boids.moveFlock(func); // use func as a variable so we can pass a custom function if we want
        timestep.wait; // wait for the timestep
      };
    });
  }

  /////////////////////////////////////////
  // make a gui for easier control
  // FUNCTIONALITY TO ADD: adding moving targets (another Flock), ability to edit/add/remove targets/obstacles
  ///////////////////////////////////////
  makeGui {|reverbRouter|
    var makerFunc, formatSlider, sliders = (), buttons = (), reverbRouterDict = (), staticText = (), boxes = (), serverMeter;
    window = Window.new("Output Controls");
    layout = HLayout.new;

    // returns an HLayout or VLayout
    formatSlider = {|slider, layout = \horz|
      switch (layout)
        {\horz} {
          HLayout(
            slider.labelView.fixedWidth_(100),
            slider.sliderView,
            slider.numberView.maxWidth_(40),
            slider.unitView.fixedWidth_(50)
          );
        }
        {\vert} {
          VLayout(
            slider.labelView.align_(\center),
            slider.sliderView,
            slider.numberView.maxWidth_(40).align_(\center),
            slider.unitView.fixedWidth_(60).align_(\center)
          );
        }
    };

    makerFunc = {
      // serverMeter = ServerMeterView.new(Server.default, window, 800@10, 0, 8).start; // can't get it aligned properly
      staticText.flockLabel = StaticText(window).string_("Spatialization").font_(Font(size: 20));

      // sliders ////////////////////////////////////////////////
      // minGain
      sliders.minGain = EZSlider(window, label: \minGain, controlSpec: ControlSpec(-66, 0, \db, units: " dB"), initVal: -33, unitWidth: 30)
        .action_({|slider|
          // boids.minGain = slider.value; // set the minGain
          this.minGain_(slider.value);
        });

      // max velocity
      sliders.maxSpeed = EZSlider(window, label: \maxSpeed, controlSpec: ControlSpec(0.001, 20, \exp, units: " m/s"), initVal: 1, initAction: true, unitWidth: 30)
        .action_({|slider|
          boids.maxVelocity = slider.value; // set the minGain
        });

      sliders.innerRatio = EZSlider(window, label: \innerRatio, controlSpec: ControlSpec(0, 0.9, \lin, units: " %"), initVal: 0.1, unitWidth: 30)
        .action_({|slider|
          boids.innerBoundRatio = slider.value; // set the minGain
        });

      sliders.width = EZSlider(window, label: \width, controlSpec: ControlSpec(2, numChans, \lin, units: " spkrs"), initVal: 2.5, initAction: true, unitWidth: 30)
        .action_({|slider|
          width = slider.value; // set the minGain
        });

      // buttons //////////////////////////////////////////////////////
      // start/stop flock flying, use inner bounds
      buttons.flockTask = Button(window)
        .states_([["start flock"],["stop flock", Color.black, Color.green(0.66,0.5)]])
        .action_({|button|
          if(button.value.asBoolean) {
            task.play; // play the task
          } {
            task.stop; // stop the task
          };
        });

      buttons.innerBounds = Button(window)
        .states_([["use inner bounds"], ["use inner bounds", Color.black, Color.green(0.66,0.5)]])
        .action_({|button|
          if(button.value.asBoolean) {
            boids.useInnerBounds = true; // use the inner bounds
          } {
            boids.useInnerBounds = false; // don't use inner bounds
          };
        });

      // number boxes
      // number boxes for x and y values for the bounds
      boxes.unitLabel = StaticText(window).string_("m");
      boxes.xDimLabel = StaticText(window).string_("x:");
      boxes.xDim = NumberBox(window)
        .value_(20)
        .step_(1)
        .align_(\center)
        .clipLo_(1)
        .clipHi_(1000)
        .action_({|box|
          boids.bounds = [box.value, boids.bounds[1].abs.sum]; // just set the x-value
        });

      boxes.yDimLabel = StaticText(window).string_("y:");
      boxes.yDim = NumberBox(window)
        .value_(20)
        .step_(1)
        .align_(\center)
        .clipLo_(1)
        .clipHi_(1000)
        .action_({|box|
          boids.bounds = [boids.bounds[0].abs.sum, box.value]; // just set the x-value
        });
      ///////////////////////////////////////////////////

      if(reverbRouter.notNil) {
        reverbRouterDict.mix = EZSlider(window, 25@100, label: 'mix', controlSpec: ControlSpec(-1, 1), initVal: -1, initAction: true, layout: \horz, unitWidth: 30)
          .action_({arg slider;
            reverbRouter.synth.set(\mix, slider.value);
          });
        reverbRouterDict.hfDamp = EZSlider(window, 25@100, label: 'damping', controlSpec: ControlSpec(100, 20000, \exp, step: 1, units: " Hz"), initVal: 15000, initAction: true, layout: \horz, unitWidth: 30)
          .action_({arg slider;
            reverbRouter.synth.set(\hfDamp, slider.value);
          });
        reverbRouterDict.label = StaticText(window).string_("REVERB").font_(Font(size: 20)); // a label for the section
      };

      // always make the master volume
      reverbRouterDict.masterVol = EZSlider(window, 25@100, label: '', controlSpec: ControlSpec(0.ampdb, 2.ampdb, \db, units: " dB"), initVal: -3, layout: \horz, unitWidth: 30)
        .action_({arg slider;
          reverbRouter.synth.set(\master, slider.value.dbamp);
        });

      // info
      // give basic non-updated info that indicates number of boids, number of channels, output bus, etc..
      staticText.numBoids = StaticText(window).string_("number of boids: %".format(numBoids)).font_(Font(size: 14));
      staticText.numChans = StaticText(window).string_("number of channels: %".format(numChans)).font_(Font(size: 14));
      // staticText.bounds = StaticText(window).string_("bounds (m): %x%".format(boids.bounds[0][1]*2, boids.bounds[1][1]*2)).font_(Font(size: 14));
      staticText.master = StaticText(window).string_("MASTER").font_(Font(size: 20));
      staticText.emptySpace = StaticText(window).string_("  ");

      // add things to the layout
      layout.add(VLayout(
        HLayout(
          // flock
          VLayout(
            staticText.flockLabel.align_(\center),
            HLayout(buttons.flockTask, buttons.innerBounds),
            formatSlider.(sliders.minGain),
            formatSlider.(sliders.maxSpeed),
            formatSlider.(sliders.innerRatio),
            formatSlider.(sliders.width),
            HLayout(
              staticText.numBoids,
              staticText.numChans,
              VLayout(
                HLayout(boxes.xDimLabel.fixedWidth_(15), boxes.xDim.maxWidth_(50), boxes.unitLabel),
                HLayout(boxes.yDimLabel.fixedWidth_(15), boxes.yDim.maxWidth_(50), boxes.unitLabel),
                // boxes.xDim.maxWidth_(50),
                // boxes.yDim.maxWidth_(50),
              ),
            ),
          ),
          // reverb
          if(reverbRouter.notNil) {
            VLayout(
              reverbRouterDict.label.align_(\center),
              HLayout(
                formatSlider.(reverbRouterDict.mix, \vert),
                formatSlider.(reverbRouterDict.hfDamp, \vert),
              ),
            )
          },
          staticText.emptySpace,
          VLayout(
            staticText.master,
            formatSlider.(reverbRouterDict.masterVol, \vert),
          ),
        )
      ));

      window.layout_(layout)
      .front
      .setTopLeftBounds(Rect(200, 200, window.sizeHint.width + 50, window.sizeHint.height)) // auto-resize
    	.onClose_({
    		this.free; // free all the things
    	});

    };

    makerFunc.defer; // make the GUI
  }

  free {
    if(group.isRunning) {group.free}; // free the group and all the synths if it's running
    busses.do(_.free); // free the busses
  }

  save {|path|
    var dict = (), archive;
    if(path.isNil) {
      "path is nil; saving to /tmp/flock.zarchive".warn;
      path = PathName.tmp ++ "flock.zarchive"; // default
    };

    archive = ZArchive.write(path); // open a new archive
    // need to save two items: this Flock and the Boids2D
    // the Flock
    dict.flock = ();
    dict.flock.numBoids = numBoids;
    dict.flock.numChans = numChans;
    dict.flock.timestep = timestep;
    dict.flock.minGain = minGain;
    dict.flock.width = width;
    dict.flock.minIdx = minIdx;

    // the Boids2D
    dict.boids = ();
    dict.boids.maxVelocity = boids.maxVelocity;
    dict.boids.centerInstinct = boids.centerInstinct;
    dict.boids.innerDistance = boids.innerDistance;
    dict.boids.matchVelocity = boids.matchVelocity;
    dict.boids.targets = boids.targets;
    dict.boids.obstacles = boids.obstacles;
    dict.boids.bounds = boids.bounds;
    dict.boids.innerBoundRatio = boids.innerBoundRatio;
    dict.boids.useInnerBounds = boids.useInnerBounds;

    archive.writeItem(dict); // write it
    archive.writeClose; // close it
  }

  //////////////////
  // custom setter methods
  //////////////////////////////
  minGain_ {|val|
    minGain = val;
    this.prSquashCurve;
  }

}
