/*
  Flock

  A helper class for using Boids2D as the spatializer. Helps keep all Boids classes abstact
  and apart from a spatialized implementation.

  For each BoidUnit, a single synth and bus routed to the input of that synth is created in a larger
  container group.
*/

Flock {
  var <numBoids, <numChans, <>outBus, <>timestep, <>server;
  var <minGain, <>width, <busses, <task, <>func, group, <boids, <synths;

  *new {|numBoids = 20, numChans = 4, outBus = 0, timestep = 0.1, server|
    ^super.newCopyArgs(numBoids, numChans, outBus, timestep, server).init;
  }

  init {
    boids = Boids2D(numBoids, timestep: timestep); // make a Boids2D
    boids.addTarget([0,0], 0.05); // add a target at the origin (5% attraction)
    minGain = 0.05; // minGain for the scaling function in amplitude
    width = 2; // how many speakers to width a signal between (see PanAz)

    this.prMakeBusses; // make the busses
    this.prMakeDefaultFunc; // set the default function
    this.prMakeTask; // make the task

    server = server ? Server.local; // default to localhost if nothing is passed in
    forkIfNeeded {
      server.loadDirectory(ReynoldsAlgorithms.synthDir++"boids"); // load the synthdefs
      server.sync;
      this.makeSynths; // make the synths
    };
  }

  // this can be called after CmdPeriod to redo everything without calling Flock.new again
  makeSynths {
    group = Group.tail; // put the group at the tail
    group.register; // register this group
    synths = numBoids.collect{|i|
      Synth("boids2D_%chan".format(numChans).asSymbol, [inBus: busses[i], outBus: outBus], target: group);
    };
  }

  prMakeBusses {
    busses = numBoids.collect{Bus.audio(server, 1)}; // collect busses into an array
  }

  prMakeDefaultFunc {
    func = {|thisFlock|
      thisFlock.boidList.do{|boidUnit, i|
        var panVals, amp;
        panVals = boidUnit.getPanVals;
        // "Boid #%::\t%\n".postf(i, boidUnit.pos);
        amp = panVals[1].linexp(1,141.421,1,minGain); // <------------- this should be settable by args
        server.makeBundle(nil, {
          synths[i].set(\pan, panVals[0].raddeg);
          synths[i].set(\amp, amp);
          synths[i].set(\width, width);
        });
      };
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

  free {
    if(group.isRunning) {group.free}; // free the group and all the synths if it's running
    busses.do(_.free); // free the busses
  }

  //////////////////
  // custom setter methods
  //////////////////////////////
  minGain_ {|val|
    minGain = val;
    this.prMakeDefaultFunc;
  }

}
