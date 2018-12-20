Automator {
  var <nodes, <curve, <>floppedNodes, <>normalizedNodes, <totalTime, <maxVal, actualMax, <minVal, <>name;
  var window, envView, key, playRoutine, env;
  classvar allAutomators;

  *initClass {
    allAutomators = List.new(0); // a List to have all playRoutines (for *stopAll)
  }

  *new {|nodes, curve = \lin|
    ^super.newCopyArgs(nodes.asFloat, curve).init;
  }

  *load {|path|
    var tmp, dict;
    if(path.notNil) {
      tmp = ZArchive.read(path); // read the ZArchive
      dict = tmp.readItem; // read the first and only item in this
      ^super.newCopyArgs(dict.nodes, dict.curve).init; // make a new Automator
    } {
      ^"Path must not be nil!".error;
    };
  }

  // stop all automator routines
  *stopAll {
    allAutomators.do(_.stop);
  }

  // make an Automator from an Envelope
  // can be used to easily save and retrieve automation curves
  *newFromEnv {|env|
    var levels, times, nodes;
    levels = env.levels; // get the levels
    times = env.times.addFirst(0).integrate; // get the times and their incremental sums
    nodes = [times,levels].flop; // make  the nodes
    ^super.newCopyArgs(nodes, env.curves).init; // make a new instance
  }

  init {
    curve = curve ? \lin;
    floppedNodes = nodes.flop;
    normalizedNodes = this.normalize(floppedNodes);
    totalTime = floppedNodes[0].maxItem;
    maxVal = floppedNodes[1].maxItem;
    minVal = floppedNodes[1].minItem;
    actualMax = maxVal;
    env = this.getEnv;
    this.makeEnvView;
  }

  // play the BPF on the client
  play {|timeInc = 0.1, func|
    var time, steps;
    steps = (env.duration/timeInc)+1; // get the number of steps we need
    time = 0; // set the initial time
    playRoutine = forkIfNeeded {
      steps.do{
        func.(env.at(time), time); // evaluate the function. pass in the value and the time.
        time = time+timeInc; // increment
        timeInc.wait;
      };
    };
    allAutomators.add(playRoutine); // add it to the master list
  }

  // stop the routine
  stop {
    if(playRoutine.notNil) {
      playRoutine.stop; // stop the routine
    };
  }

  // make an array of the values at the time incremenet
  makeArray {|timeInc = 0.1|
    var steps, env;
    steps = totalTime/timeInc; // get the total number of steps at timeInc
    env = this.getEnv;
    ^env.asSignal(steps.floor).as(Array); // make a signal and return it
  }

  // make the EnveopeView so that things don't get wonky
  makeEnvView {
    var width, height, step, labels;
    name = name ?? {"Automator"};
    width = Window.screenBounds.width;
    height = Window.screenBounds.height;
    step = 1/(totalTime*2); // calculate the step
    window = Window(name.asString, Rect(width/3 , height/3, width/2, height/3));
    window.view.decorator = FlowLayout(window.view.bounds);
    envView = EnvelopeView(window, Rect(0, 0, (width/2)-5, (height/3)-5))
      .drawLines_(true)
      .selectionColor_(Color.red)
      .drawRects_(true)
      .resize_(5)
      .keepHorizontalOrder_(true)
      .step_(0.001) // this will probably have to be edited according to the total durtation to make each step 500ms
      .grid_(Point(step*2, step*2)) // see comment on step
      .gridOn_(true)
      .thumbSize_(10)
      .curves_(curve) // set the curve
      .action_({|view| this.envViewAction})
      .value_(normalizedNodes);

    // make the labels and update
    this.prUpdateView;

    ////////////////////////
    // add and remove nodes
    ////////////////////////
    envView.mouseDownAction_({|view|
      var mousePos, relX, relY, currentValue, newValue;
      // if "a" is pressed, do this (how to make it ctrl??)
      if (key == 97) {
        mousePos = QtGUI.cursorPosition; // get the cursor position (pass in x and y from args??)
        relX = (mousePos.x - view.absoluteBounds.left) / view.absoluteBounds.width; // get the relative X position
        relY = (mousePos.y - view.absoluteBounds.top) / view.absoluteBounds.height; // get the relative Y position
        relY = 1 - relY; // invert to get the correct spot
        currentValue = view.value.flop; // get the value and flop it
        newValue = currentValue.add([relX, relY]); // add the new point to the value
        newValue = newValue.sort({|point1, point2|
          point1[0] < point2[0]; // sort by the first element in each subarray
        });

        defer {
          view.value_(newValue.flop); // make it in the display
          // get the new values (often shifted slightly due to the step value in the EnvelopeView)
          floppedNodes = envView.value.collect{|array, i|
            if(i==0) {
              array*totalTime;
            } {
              (array*abs(minVal-maxVal)) + minVal;
            };
          };

          // set things again
          nodes = floppedNodes.flop;
          normalizedNodes = envView.value;
          maxVal = floppedNodes[1].maxItem;
          minVal = floppedNodes[1].minItem;
        };

        this.prUpdateView; // update
      };
    });

    /////////////////////////////////
    // delete a node with backspace
    /////////////////////////////////
    envView.keyDownAction_({|view, char, mod, unicode|
      key = unicode; // set the key
      // only delete if we have an index selected and don't allow nodes at the beginning and end to be deleted
      if ((unicode == 8) && (view.index > 0) && (view.index != (view.value[0].size-1))) {
        // remove the values at the index in normalizedNodes
        2.do{|i|
          normalizedNodes[i].removeAt(view.index);
        };
        this.prUpdateView;
      };

      // on esc, despect all (doesn't work???)
      if (unicode == 27) {
        defer {view.selectIndex(-1)};
      };
    });

    envView.keyUpAction_({
      key = -1; // set it so that nothing happens when we release any key
    });
    /////////////////////////////////////////////////////////

    window
      .view.deleteOnClose_(false); // don't destory when we close (but must do so manually)
  }

  // show the GUI and allow editing
  show {|str|
    if (str.notNil) {window.name = str.asString}; // give it a name
    window.front;
  }

  getEnv {
    ^Env(floppedNodes[1], floppedNodes[0].differentiate[1..], curve);
  }

  // a function to evaluate when the envelope view is edited
  envViewAction {
    // set our variables in this instance every time we edit
    normalizedNodes = envView.value;
    floppedNodes = this.unnormalize;
    nodes = floppedNodes.flop;
    this.prUpdateView; // update the GUI

    // if we select the first or last node, don't allow the x position to be adjusted!! <-----------------------------------------------
  }

  // digest the nodes into a form that's passable to an EnvelopeView
  normalize {|nodes, max|
    var normalized, min, diff, range;
    normalized = 2.collect{|i|
      if (i==1) {
        if(max.notNil) {
          min = nodes[i].minItem;
          nodes[i].normalize(0, abs(min-nodes[i].maxItem)/abs(min-max)); // normalize for our range (don't move nodes)
        } {
          nodes[i].normalize; // otherwise normalize and move the nodes
        };
      } {
        nodes[i].sort.normalize; // normalize the time domain
      };
    };
    ^normalized;
  }

  // go the other way (pass in normalized?)
  unnormalize {
    var tmp = Array.newClear(2);
    tmp[0] = envView.value[0]*totalTime;
    tmp[1] = (envView.value[1]*abs(minVal-maxVal)) + minVal;
    ^tmp;
  }

  // if a view exists, change the display to account for the new info
  prUpdateView {
    var labels, step;
    if (envView.notNil) {
      defer {
        envView.value_(normalizedNodes);
        labels = nodes.collect{|coordinate, i|
          "(%, %)".format(coordinate[0].round(0.001), coordinate[1].round(0.001)); // make a string with the correctly formatted label
        };
        envView.strings_(labels); // set it
        envView.curves = curve; // reset the curve, as well
        env = this.getEnv; // get the new Envelope
      };
    };
  }

  free {
    window.close; // close and destory the window containing the EnvelopeView manually
  }

  // save the nodes to a file
  save {|path|
    var dict = (), archive; // an empty dictionary
    if(path.notNil) {
      archive = ZArchive.write(path); // make a new ZArchive
      dict.nodes = nodes; // save the nodes
      dict.curve = curve; // save the curve
      archive.writeItem(dict); // write the entire dictionary
    } {
      ^"Path must not be nil!".error;
    };
  }

  // load nodes from a file
  loadOldArchive {|path|
    var dict;
    if(path.notNil) {
      dict = Object.readArchive(path); // read it
      nodes = dict.nodes; // set the values
      curve = dict.curve; // set the values

      //////////////////////////////////////////////
      // not exactly sure why we need to do this here but otherwise the array is created that starts with a bunch of 0's
      normalizedNodes = envView.value;
      floppedNodes = this.unnormalize;
      nodes = floppedNodes.flop;
      env = this.getEnv;
      //////////////////////////////////////////////
    } {
      ^"Path must not be nil!".error;
    };
  }

  ///////////////////////////////
  // special setter methods
  ///////////////////////////////
  nodes_ {|nodes|
    // set nodes and normalize
    nodes = nodes;
    floppedNodes = nodes.flop;
    normalizedNodes = this.normalize(floppedNodes);
    totalTime = floppedNodes[0].maxItem;
    maxVal = floppedNodes[1].maxItem;
  }

  // change the total time, keeping the proportions
  totalTime_ {|time|
    totalTime = time;
    // floppedNodes = [normalizedNodes[0]*totalTime, normalizedNodes[1]*maxVal]; // adjust the times
    floppedNodes[0] = normalizedNodes[0]*totalTime;
    nodes = floppedNodes.flop; // set the nodes
    this.prUpdateView; // update the node labels
  }

  maxVal_ {|val|
    this.setMaxVal(val, false); // don't adjust nodes in the vertical by default. If you want to, use setMaxVal(val, true)
  }

  // reset the max value and scale everything accordingly
  setMaxVal {|val, adjustNodes = false|
    maxVal = val; // set the maximum value
    // use different methods depending on if we want to adjust nodes
    if(adjustNodes) {
      normalizedNodes = this.normalize(floppedNodes);
    } {
      normalizedNodes = this.normalize(floppedNodes, maxVal);
    };
    this.prUpdateView; // update the node labels
  }

  curve_ {|curveVal|
    curve = curveVal;
    if (envView.notNil) {
      defer {
        envView.curves = curve; // reset it
        this.prUpdateView;
      };
    };
  }

}
