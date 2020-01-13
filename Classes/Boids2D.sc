///////////////////////////////////////////
/// Boids class
///////////////////////////////////////////
///////////////////////////////////////////
/*
  Usage:

    b = Boids(numBoids: 10); // an initial number of boids. Flock size can be arbitrarily increased by using addBoid()
    f = {|thisInstance|
      thisInstace.info; // print info about this flock
      boids.postln; // the list of BoidUnits
    };
    b.moveFlock(f);
*/


Boids2D {
  var <numBoids, <timestep, <centerInstinct, <innerDistance, <matchVelocity, <centerOfMass, <centerOfVel;
  var >boidList, <maxVelocity, workingMaxVelocity, <minSpace, targets, obstacles;
  var <bounds, <wrap, <sd;

  *new {|numBoids = 10, timestep = 0.5, centerInstinct = 1, innerDistance = 1, matchVelocity = 1|
    ^super.newCopyArgs(numBoids, timestep, centerInstinct, innerDistance, matchVelocity).init;
  }

  init {
    boidList = List.new(0); // an empty list of BoidUnits that we fill below
    maxVelocity = 100; // speed limit in meters per second (need to multiply it by the timestep)
    workingMaxVelocity = maxVelocity * timestep; // set the workingMaxVelocity (accounting for the timestep)
    minSpace = 10; // minmum distance between boids in a flock in meters
    centerOfMass = RealVector2D.zero; // init center of mass at the origin
    bounds = [[-500,500], [-500,500]]; // set the default bounds
    sd = 0.05; // standard deviation in percent of mean
    this.prFillBoidList(numBoids); // fill the list with boids
    targets = List.new(0);
    obstacles = List.new(0);
    wrap = false; // default to no wrapping
  }

  // optimized version of the rules
  prDoRules {
    var posSum = RealVector2D.zero, velSum = RealVector2D.zero, sizeOfNeighbors;
    sizeOfNeighbors = clip(numBoids-1, 1, 6); // get at most 6. If less, get all (but not itself)

    // for each boidUnit...
    boidList.do{|thisBoid, i|
      var neighbors, vec, count, nearestNeighbors, velAvg, posAvg, nIdx;
      neighbors = Array.newClear(numBoids-1);
      nIdx = 0; // index for neighbors
      vec = RealVector2D.zero; // for rule 2
      count = 1; // for rule 2

      // get some averages
      posSum = posSum + thisBoid.pos; // sum the position
      velSum = velSum + thisBoid.vel; // sum the velocities

      // loop again
      boidList.do{|thatBoid, j|
        var dist;
        // don't check for boids that are the exact same object
        if ((thisBoid === thatBoid).not) {
          // get the distances to the other boids
          dist = thisBoid.dist(thatBoid); // get their distance
          neighbors[nIdx] = Dictionary[\pos->thatBoid.pos, \vel->thatBoid.vel, \dist->dist]; // store in a dictionary

          //////// RULE 1 ////////////////////////////////////
          // nothing else to do!

          //////// RULE 2 ////////////////////////////////////
          // if the absolute value of the distance is less than the threshold
          if (dist < thisBoid.instincts.at(\minSpace)) {
            vec = vec + ((thisBoid.pos-thatBoid.pos)*(thisBoid.instincts.at(\minSpace)/(dist**2))); // calculate the difference vector
            count = count+1; // keep counting the boids in the vicinity
          };

          //////// RULE 3 ////////////////////////////////////
          // nothing else to do!

          nIdx = nIdx + 1; // increment the index
        };
      };

      // for rules 1 and 3
      neighbors.sortBy(\dist); // sort neighbors by distance
      nearestNeighbors = sizeOfNeighbors.collect{|j|
        // get the 6 nearest neighbors, regardless of distance (Ballerini, M et al. "Interaction ruling animal collective behavior depends on topological rather than metric distance: Evidence from a field study" (2008))
        [neighbors[j].at(\pos), neighbors[j].at(\vel)];
      };
      nearestNeighbors = nearestNeighbors.flop; // [positions, velocities]

      //////// RULE 1 ////////////////////////////////////
      // for the six nearest, get their average positions and velocities
      posAvg = nearestNeighbors[0].sum/sizeOfNeighbors; // sum and divide
      thisBoid.centerOfMass = thisBoid.instincts.at(\centerInstinct) * posAvg * 0.001; // set it for rule 1

      //////// RULE 2 ////////////////////////////////////
      vec = vec/count; // average the vector
      thisBoid.innerDistance = thisBoid.instincts.at(\innerDistance) * vec; // set the innerDistance vector in each BoidUnit

      //////// RULE 3 ////////////////////////////////////
      velAvg = nearestNeighbors[1].sum/sizeOfNeighbors;
      thisBoid.matchVelocity = thisBoid.instincts.at(\matchVelocity) * velAvg; // send one eigth of the magnitude
    };

    centerOfMass = posSum/numBoids; // get the center of mass
    centerOfVel = velSum/numBoids; // get the avgerage velocity
  }

  prFillBoidList {|num|
    num.do{
      var boid;
      boid = BoidUnit2D.rand(bounds, centerInstinct, innerDistance, matchVelocity, minSpace, workingMaxVelocity, sd);
      boidList.add(boid); // add it to the list
    };
  }

  addBoid {|initPos|
    var boid, initVel;
    initPos = initPos ? centerOfMass; // place it near the center of the flock
    initVel = centerOfVel; // give it the average velocity of the flock
    boid = BoidUnit2D.new(initVel, initPos, bounds, centerInstinct, innerDistance, matchVelocity, minSpace, workingMaxVelocity, sd);
    boidList.add(boid); // add it
    numBoids = numBoids + 1; // increment numBoids
  }

  removeBoid {|index|
    if(numBoids==1) {
      "Cannot have a flock with no agents!".warn;
    } {
      if (index.isNil) {
        boidList.pop; // if no arg, remove the last BoidUnit
        } {
          boidList.removeAt(index); // else, remove at the index
        };
        numBoids = numBoids - 1; // decrement numBoids
    };
  }

  sizeOfFlock {
    ^boidList.size; // return the size of the flock
  }

  moveFlock {|func|
    this.prDoRules; // do them all with two loops

    // move AFTER we've calculated what will happen
    boidList.do{|boid|
      boid.moveBoid(targets, obstacles); // tell the boid to calculate and move it's position
    };
    func.(this); // evaluate the function while passing this instance
  }

  // calculate the new values but don't send them to the BoidUnits
  calcFlock {|func|
    this.prDoRules; // do all rules
    func.(this); // evaluate the function while passing this instance
  }

  getPanVals {
    ^boidList.collect{|boid|
      boid.getPanVals; // get the pan values
    };
  }

  // creates a rectangle of size [dim]
  bounds_ {|dim|
    var rect, xLen, yLen;
    if(dim.isArray) {
      if(dim.size==2) {
        xLen = dim[0];
        yLen = dim[1];
      };
    } {
      "dim must be a two element array".error; // else something is wrong
      ^nil; // return nill
    };

    // create the bounds of a rectangle with the given dimensions with the origin at the center
    rect = [[-0.5*xLen, 0.5*xLen], [-0.5*yLen, 0.5*yLen]];
    bounds = rect; // set the new bounds
    boidList.do{|boid|
      boid.bounds = bounds; // set it in each Boid
    };
  }

  ///////////////////////////////////
  // targeting
  ///////////////////////////////////
  addTarget {|pos, gravity|
    if(pos.isNil or: gravity.isNil)
      {"Insuffient arguments: %, %: no target was added!".format(pos, gravity).warn; ^this};
    targets.add(Dictionary.with(*[\pos->RealVector2D.newFrom(pos[..1]), \strength->gravity]));
  }

  clearTargets {
    targets.clear; // clear the list
  }

  removeTarget {|index|
    if(index.isNil) {
      targets.pop; // remove the last index
    } {
      targets.removeAt(index); // remove at the index
    };
  }

  editTarget {|index, pos, gravity|
    if(index.isNil) {"Index is nil: no targets were edited!".warn}; // throw a warning if insufficent args were supplied
    if(pos.notNil) {targets[index].add(\pos->RealVector2D.newFrom(pos[..1]))}; // should check here if target is a Vector or not
    if(gravity.notNil) {targets[index].add(\strength->gravity)}; // edit the gravity parameter
  }

  /////////////////////////////////////////
  ///// obstacles
  //////////////////////////////////////////
  addObstacle {|pos, repulsion|
    if(pos.isNil or: repulsion.isNil)
      {"Insuffient arguments: %, %: no obstacle was added!".format(pos, repulsion).warn; ^this};
    obstacles.add(Dictionary.with(*[\pos->RealVector2D.newFrom(pos[..1]), \strength->repulsion]));
  }

  clearObstacles {
    obstacles.clear; // clear the list
  }

  removeObstacle {|index|
    if(index.isNil) {
      obstacles.pop; // remove the last index
    } {
      obstacles.removeAt(index); // remove at the index
    };
  }

  editObstacle {|index, pos, repulsion|
    if(index.isNil) {"Index is nil: no obstacles were edited!".warn}; // throw a warning if insufficent args were supplied
    if(pos.notNil) {obstacles[index].add(\pos->RealVector2D.newFrom(pos[..1]))}; // should check here if target is a Vector or not
    if(repulsion.notNil) {obstacles[index].add(\strength->repulsion)}; // edit the repulsion parameter
  }

  // print the variable information for this flock
  info {
    var str = "Boid Info::::\n";
    str = str ++ "\tnumBoids: %\n".format(numBoids);
    str = str ++ "\ttimestep: % s\n".format(timestep);
    str = str ++ "\tcenterInstinct: %\n".format(centerInstinct);
    str = str ++ "\tinnerDistance: %\n".format(innerDistance);
    str = str ++ "\tmatchVelocity: %\n".format(matchVelocity);
    str = str ++ "\tmaxVelocity: % m/s\n".format(maxVelocity);
    str = str ++ "\tminSpace: % m\n".format(minSpace);
    str.postln; // print it
  }

  // visualizer
  visualizer {|showLabels = false, returnWindow = false|
    var window, loop, availableBounds, size, getNormalizedPos, makeCircle, makeLabel;
    availableBounds = Window.availableBounds;
    size = availableBounds.width/3;
    window = Window("Flock Visualizer", Rect(availableBounds.width-size,availableBounds.height-size,size,size)).front;
    window.view.background_(Color.white);

    // functions
    getNormalizedPos = {|pos|
      [(pos.x+bounds[0][0].abs)/(bounds[0][0].abs*2), 1 - ((pos.y+bounds[1][0].abs)/(bounds[1][0].abs*2))];
    };

    makeCircle = {|normalizedPos|
      Pen.addOval(Rect(window.bounds.width*normalizedPos[0], window.bounds.height*normalizedPos[1], 5, 5));
    };

    makeLabel = {|label, normalizedPos, color|
      Pen.stringAtPoint(label.asString, Point(window.bounds.width*normalizedPos[0] + 3, window.bounds.height*normalizedPos[1] + 3), color: color);
    };

    // draw the boids
    window.drawFunc = {
      // plot the boids as black wedges
      boidList.do{|boid, i|
        var normalizedPos, color;
        color = Color.black;
        Pen.color = color;
        normalizedPos = getNormalizedPos.(boid.pos); // normalize the position for the window
        Pen.addWedge(
          Point(window.bounds.width*normalizedPos[0], window.bounds.height*normalizedPos[1]), // point
          7.5, // radius (pixels)
          (-1*boid.vel.theta) - 3.5342917352885, // start angle (angle - sizeOfAngle/2 (pi/8) - pi) for visualizer corrections
          0.78539816339745 // size of angle (pi/4)
        );
        if(showLabels) {
          makeLabel.(i, normalizedPos, color); // make a label
        };
        Pen.perform(\fill);
      };

      // plot the targets as blue circles
      targets.do{|target, i|
        var normalizedPos, color;
        color = Color.fromHexString("4989FF");
        Pen.color = color;
        normalizedPos = getNormalizedPos.(target.at(\pos)); // normalize the position for the window
        makeCircle.(normalizedPos);
        if(showLabels) {makeLabel.(i, normalizedPos, color)}; // make a label
        Pen.perform(\fill);
      };

      // plot the obstacles as red circles
      obstacles.do{|obstacle, i|
        var normalizedPos, color;
        color = Color.fromHexString("FF4949");
        Pen.color = color;
        normalizedPos = getNormalizedPos.(obstacle.at(\pos)); // normalize the position for the window
        makeCircle.(normalizedPos);
        if(showLabels) {makeLabel.(i, normalizedPos, color)}; // make a label
        Pen.perform(\fill);
      };
    };

    loop = {
      loop {window.refresh; timestep.wait};
    }.fork(AppClock);

    window.onClose_({loop.stop});
    if(returnWindow) {^window};
  }

  /////////////////////////////////
  // custom setter methods
  /////////////////////////////////
  maxVelocity_ {|val|
    maxVelocity = val; // maxVelocity is the maximum length of the velocity vector
    workingMaxVelocity = maxVelocity * timestep;
    boidList.do{|boid|
      boid.maxVelocity = gauss(workingMaxVelocity, workingMaxVelocity*sd); // set it in each individual boid
    };
  }

  minSpace_ {|val|
    minSpace = val;
    boidList.do{|boid|
      boid.instincts.add(\minSpace->gauss(minSpace, minSpace*sd));
    };
  }

  wrap_ {|boolean|
    wrap = boolean;
    boidList.do(_.wrap_(boolean));
  }

  centerInstinct_ {|val|
    centerInstinct = val; // set it here (the average)
    boidList.do{|boid|
      boid.instincts.add(\centerInstinct->gauss(val, val*sd));
    };
  }

  innerDistance_ {|val|
    innerDistance = val; // set it here (the average)
    boidList.do{|boid|
      boid.instincts.add(\innerDistance->gauss(val, val*sd));
    };
  }

  matchVelocity_ {|val|
    matchVelocity = val; // set it here (the average)
    boidList.do{|boid|
      boid.instincts.add(\matchVelocity->gauss(val, val*sd));
    };
  }

  // to ensure we return this instance if setting
  timestep_ {|time|
    timestep = time;
  }

  // reset all the values in the boids with the new standard deviation
  sd_ {|value|
    sd = value;
    boidList.do{|boid|
      boid.maxVelocity = gauss(workingMaxVelocity, workingMaxVelocity*sd);
      boid.instincts.add(\minSpace->gauss(minSpace, minSpace*sd));
      boid.instincts.add(\centerInstinct->gauss(centerInstinct, centerInstinct*sd));
      boid.instincts.add(\innerDistance->gauss(innerDistance, innerDistance*sd));
      boid.instincts.add(\matchVelocity->gauss(matchVelocity, matchVelocity*sd));
    };
  }

  /////////////////////////////
  // custom getter methods
  /////////////////////////////
  boidList {
    ^boidList.asArray;
  }

  boids {
    ^boidList.asArray;
  }

  targets {
    ^targets.asArray;
  }

  obstacles {
    ^obstacles.asArray;
  }

}

////////////////////////////////////////////////////////////
// not directly used but rather used by Boids
////////////////////////////////////////////////////////////
BoidUnit2D {
  var <>vel, <>pos, <bounds, <centerOfMass, <maxVelocity;
  var <>centerInstinct, <>innerDistance, <>matchVelocity, <>wrap, <>instincts;

  *new {|vel, pos, bounds, centerInstinct, innerDistance, matchVelocity, minSpace, maxVelocity, sd|
    ^super.newCopyArgs(vel, pos, bounds).init(nil, centerInstinct, innerDistance, matchVelocity, minSpace, maxVelocity, sd);
  }

  *rand {|bounds, centerInstinct, innerDistance, matchVelocity, minSpace, maxVelocity, sd|
    ^super.new.init(bounds, centerInstinct, innerDistance, matchVelocity, minSpace, maxVelocity, sd);
  }

  init {|...args|
    bounds = bounds ? args[0] ? [[-500,500],[-500,500]]; // [ [xmin, xmax], [ymin, ymax]]
    vel = vel ? RealVector2D.newFrom(Array.fill(2, {rrand(0.0,3.0)}));
    pos = pos ? RealVector.rand(2,-1*bounds[0][0], bounds[0][0]).asRealVector2D;
    maxVelocity = maxVelocity ? gauss(args[5], args[5]*args[6]) ? 5; // max velocity

    // if these are not set, set them randomly
    centerOfMass = RealVector.rand(2,-10,10).asRealVector2D;
    innerDistance = RealVector.rand(2,-10,10).asRealVector2D;
    matchVelocity = RealVector.rand(2,-10,10).asRealVector2D;

    instincts = Dictionary[
      \centerInstinct->gauss(args[1], args[1]*args[6]),
      \innerDistance->gauss(args[2], args[2]*args[6]),
      \matchVelocity->gauss(args[3], args[3]*args[6]),
      \minSpace->gauss(args[4], args[4]*args[6])
    ]; // individualized weights
    // centerInstinct = centerOfMass/100; // set this here
    vel = vel.limit(maxVelocity); // limit the size of the velocity vector
    wrap = wrap ? false; // default to no wrapping
  }

  prBound {
    var vec = RealVector2D.zero; // a zero vector
    2.do{|i|
      var amount;
      if(pos[i] < bounds[i][0]) {
        amount = bounds[i][0] + pos[i].abs; // how far off are we
        vec[i] = amount; // change zero for this
      } {
        if(pos[i] > bounds[i][1]) {
          amount = bounds[i][1] - pos[i]; // how far off are we
          vec[i] = amount; // change zero for this
        };
      };
    };
    vel = vel + vec; // add the vectors in velocity-space
  }

  // wrap coordinates
  prWrap {
    2.do{|i|
      if(pos[i] < bounds[i][0]) {
        pos[i] = bounds[i].abs.sum + pos[i];
      } {
        if(pos[i] > bounds[i][1]) {
          pos[i] = pos[i] - bounds[i].abs.sum;
        };
      };
    };
  }

  moveBoid {|targets, obstacles|
    if (targets.isEmpty.not) {vel = vel + this.calcTargets(targets)}; // if there are targets, calculate the vector
    if (obstacles.isEmpty.not) {vel = vel + this.calcObstacles(obstacles)}; // if there are obstacles, calculate the vector
    vel = vel + centerOfMass + innerDistance + matchVelocity; // sum the vectors and get a new velocity
    if(wrap) {this.prWrap} {this.prBound}; // wrap or bound
    vel = vel.limit(maxVelocity); // speed limit
    pos = pos + vel; // get the new position
  }

  getPanVals {
    var zero = RealVector2D.zero;
    ^[pos.theta, pos.dist(zero)]; // return the angle in radians and the distance from the origin
  }

  calcObstacles {|obstacles|
    var vec = RealVector2D.zero, distFromTarget, gravity, diff;
    obstacles.do{|obstacle|
      vec = this.prCalcVec(obstacle, vec, \obstacle);
    };
    ^vec; // return the vector
  }

  calcTargets {|targets|
    var vec = RealVector2D.zero, distFromTarget, gravity, diff;
    targets.do{|target|
      vec = this.prCalcVec(target, vec, \target);
    };
    ^vec; // return the vector
  }

  // prCalcVec {|object, vec, type|
  //   var distFromTarget, diff, gravity;
  //   distFromTarget = pos.dist(object.at(\pos)).max(0.001); // get the distance from the object
  //   switch (type)
  //     {\target} {
  //       diff = object.at(\pos)-pos; // get the diff
  //       gravity = ((object.at(\strength)*100)/distFromTarget).max(0); // 1/r
  //     }
  //     {\obstacle} {
  //       diff = pos-object.at(\pos); // get the diff
  //       gravity = this.prInverseSquare(distFromTarget, object.at(\strength)*1000).max(0); // 1/r^2
  //     };
  //   ^vec + ((diff/diff.norm)*gravity); // return
  // }

  prCalcVec {|object, vec, type|
    var distFromObject, diff, gravity, reweighted;
    distFromObject = pos.dist(object.at(\pos)).max(0.0001); // get the distance from the object
    switch (type)
      {\target} {
        diff = object.at(\pos)-pos; // get the diff
        if(object.at(\strength).isArray) {
          reweighted = 2.collect{|i| // if gravity is an array, apply gravities in specific dimensions
            ((object.at(\strength)[i]*100)/diff[i].abs).max(0); // 1/r
          };
          gravity = RealVector2D.newFrom(reweighted); // get the vector
        } { // else it's an integer so apply it evenly
          gravity = ((object.at(\strength)*100)/distFromObject).max(0); // 1/r
        };
      }
      {\obstacle} {
        diff = pos-object.at(\pos); // get the diff
        if(object.at(\strength).isArray) {
          reweighted = 2.collect{|i| this.prInverseSquare(diff[i].abs, object.at(\strength)[i]*10000).max(0)}; // check for distance in particular dimensions
          gravity = RealVector2D.newFrom(reweighted); // get the vector
        } { // else it's an integer so apply it evenly
          gravity = this.prInverseSquare(distFromObject, object.at(\strength)*1000).max(0); // 1/r^2
        };
      };
    ^vec + ((diff/diff.norm)*gravity); // return
  }

  // get the distance between two boids
  dist {|boid2D|
    ^pos.dist(boid2D.pos);
  }

  /////////////////////////////////
  // gravity/repulsion scaling functions
  /////////////////////////////////
  prInverseSquare {|dist = 1, gravity = 1|
    ^gravity/(dist**2);
  }

  prArcTan {|dist = 1, gravity = 1, scalar = 10|
    gravity = gravity.reciprocal*scalar;
    dist = (dist*gravity)-gravity;
    gravity = atan(-1*dist);
    ^(gravity/3)+0.5;
  }

  prArcTan2 {|dist = 1, gravity = 1, scalar = 5|
    // scalar is where the arctangent function passes through 0 normally
    dist = (dist-(gravity*scalar));
    gravity = atan(-1*dist*gravity.reciprocal);
    ^(gravity/3)+0.5;
  }

  /////////////////////////////////
  // custom setter methods
  /////////////////////////////////
  centerOfMass_ {|vec|
    centerOfMass = vec; // get the perceived center of mass for this BoidUnit
    // each time we get a new center of mass, recalculate the first vector offset
    // centerInstinct = centerOfMass/100; // get the vector that moves it 1% toward the center of the flock (this can be weighted??)
  }

  maxVelocity_ {|val|
    maxVelocity = val; // set it
    vel = vel.limit(maxVelocity); // limit it if it's bigger
  }

  bounds_ {|val|
    bounds = val;
  }
}
