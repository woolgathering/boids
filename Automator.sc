Automator {
  var <nodes, <>floppedNodes, <>normalizedNodes, <totalTime, <>maxVal, <>name;
  var window, envView, key;

  *new {|nodes|
    ^super.newCopyArgs(nodes).init;
  }

  *load {|path|
    var tmp;
    if(path.notNil) {
      tmp = Object.readArchive(path); // read it
      ^super.newCopyArgs(tmp).init; // make a new Automator
    } {
      ^"Path must not be nil!".error;
    };
  }

  init {
    // if (nodes.isInteger) {
    //   nodes = Array.fill(nodes, {|i|
    //     [i*10, 0]
    //   });
    // };
    floppedNodes = nodes.flop;
    normalizedNodes = this.normalize(floppedNodes);
    totalTime = floppedNodes[0].maxItem;
    maxVal = floppedNodes[1].maxItem;
  }

  // play the envelope on the client
  play {|timeInc = 0.1, func|
    var diffTimes, diffTargets;
    diffTimes = normalizedNodes[0].differentiate[1..] * totalTime;
    diffTargets = (normalizedNodes[1]*maxVal).differentiate; // don't get rid of the first normalizedNodes in targets
    forkIfNeeded {
        diffTimes.do{|time, i|
          var inc, steps, val, now;
          i = i+1;
          now = 0;
          steps = time/timeInc; // how many steps we'll take at timeInc
          inc = diffTargets[i]/steps; // get the increment we need to add to get to our target in the amount of time we have
          val = (normalizedNodes[1]*maxVal)[i-1]; // set the intial value we're heading from
          while (
            {now < time},
            {
              func.(val); // evalute the function and pass in the value
              val = val+inc; // increment the value
              now = now+timeInc; // increment now
              timeInc.wait; // wait
            }
          );
        }; // end diffTimes.do

    };
  }

  // show the GUI and allow editing
  show {
    var width, height, step, labels;
    name = name ?? {"Automator"};
    width = Window.screenBounds.width;
    height = Window.screenBounds.height;
    step = 1/(totalTime*2); // calculate the step
    // window = Window(name.asString, Rect(width/2, height/2, 500, 300));
    window = Window(name.asString, Rect(width/3 , height/3, width/2, height/3)).front;
    window.view.decorator = FlowLayout(window.view.bounds);
    envView = EnvelopeView(window, Rect(0, 0, (width/2)-5, (height/3)-5))
      .drawLines_(true)
      .selectionColor_(Color.red)
      .drawRects_(true)
      .resize_(5)
      .keepHorizontalOrder_(true)
      .step_(step) // this will probably have to be edited according to the total durtation to make each step 500ms
      .grid_(Point(step*2,step*2)) // see comment on step
      .gridOn_(true)
      .thumbSize_(10)
      .action_({|view| this.envViewAction})
      .value_(normalizedNodes);

    // make the labels
    labels = envView.value.flop.collect{|coordinate|
      "(%, %)".format((coordinate[0]*totalTime).round(0.1), (coordinate[1]*maxVal).round(0.01)); // make a string with the correctly formatted label
    };
    envView.strings_(labels); // set it

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
          var labels;
          view.value_(newValue.flop); // make it in the display
          // relabel
          labels = envView.value.flop.collect{|coordinate|
            "(%, %)".format((coordinate[0]*totalTime).round(0.1), (coordinate[1]*maxVal).round(0.01)); // make a string with the correctly formatted label
          };
          envView.strings_(labels); // set it
        };

        // set all the variables correctly (a lilttle cumbersome here... can be done better)
        {
          var tmp;
          tmp = newValue.flop; // do it once
          floppedNodes = [tmp[0]*totalTime, tmp[1]*maxVal];
          nodes = floppedNodes.flop;
          normalizedNodes = this.normalize(floppedNodes);
          totalTime = floppedNodes[0].maxItem;
          maxVal = floppedNodes[1].maxItem;
        }.value;

      };
    });

    envView.keyDownAction_({|view, char, mod, unicode|
      // delete a node with backspace
      key = unicode; // set the key

      if ((unicode == 8) and: (view.index != -1)) {
        var value, idx;
        value = view.value;
        value = value.flop;
        value.removeAt(view.index);
        defer {
          view.value_(value.flop); // remove the index on backspace
          // relabel
          labels = envView.value.flop.collect{|coordinate|
            "(%, %)".format((coordinate[0]*totalTime).round(0.1), (coordinate[1]*maxVal).round(0.01)); // make a string with the correctly formatted label
          };
          envView.strings_(labels); // set it
        };
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
      .front // show it
      .view.deleteOnClose_(false); // don't destory when we close (but must do so manually)

  }

  // a function to evaluate when the envelope view is edited
  envViewAction {
    // set our variables in this instance every time we edit
    normalizedNodes = envView.value;
    floppedNodes = this.unnormalize;
    nodes = floppedNodes.flop;
    // do stuff to the GUI
    defer {
      envView.setString(envView.index, "(%, %)".format((envView.x*totalTime).round(0.1), (envView.y*maxVal).round(0.1)));
    };
  }

  // digest the nodes into a form that's passable to an EnvelopeView
  normalize {|nodes|
    var normalized, tmp;
    normalized = 2.collect{|i|
      if (i==1) {
        tmp = nodes[i].addFirst(0); // ensure there's a zero in the y values
        tmp = tmp.normalize; // normalize it
        tmp[1..]; // return all but the first index (0)
      } {
        nodes[i].sort.normalize; // normalize normally
      };
    };
    ^normalized;
  }

  // go the other way (pass in normalized?)
  unnormalize {
    var tmp = Array.newClear(2);
    tmp[0] = envView.value[0]*totalTime;
    tmp[1] = envView.value[1]*maxVal;
    ^tmp;
  }

  free {
    window.close; // close and destory the window containing the EnvelopeView manually
  }

  // save the nodes to a file
  save {|path|
    if(path.notNil) {
      nodes.writeArchive(path); // write the nodes to a file
    } {
      ^"Path must not be nil!".error;
    };
  }

  load {|path|
    if(path.notNil) {
      nodes = Object.readArchive(path); // read it
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
    var labels, step;
    totalTime = time;
    floppedNodes = [normalizedNodes[0]*totalTime, normalizedNodes[1]]; // adjust the times
    nodes = floppedNodes.flop; // set the nodes

    // if a view exists, change the display to account for the new info
    if (envView.notNil) {
      defer {
        labels = envView.value.flop.collect{|coordinate|
          "(%, %)".format((coordinate[0]*totalTime).round(0.1), (coordinate[1]*maxVal).round(0.01)); // make a string with the correctly formatted label
        };
        envView.strings_(labels); // set it

        // redraw the grid and recalculate the step
        step = 1/(totalTime*2); // calculate the step
        envView.step_(step).grid_(Point(step*2,step*2));
      };
    };
    // normalizedNodes remains the same
  }



}
