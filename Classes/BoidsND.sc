///////////////////////////////////////////
/// Boids class
///////////////////////////////////////////
///////////////////////////////////////////
/*
  Usage:

    b = BoidsND(4, numBoids: 10); // an initial number of boids. Flock size can be arbitrarily increased by using addBoid()
    f = {|thisInstance|
      thisInstace.info; // print info about this flock
      boids.postln; // the list of BoidUnits
    };
    b.moveFlock(f);
*/


BoidsND {
  var <dim, <numBoids, <>timestep, <centerInstinct, <innerDistance, <matchVelocity, <centerOfMass, <centerOfVel;
  var >boidList, <maxVelocity, workingMaxVelocity, <minSpace, targets, obstacles;
  var <bounds, <wrap;

  *new {|dim = 4, numBoids = 10, timestep = 0.5, centerInstinct = 1, innerDistance = 1, matchVelocity = 1|
    ^super.newCopyArgs(dim, numBoids, timestep, centerInstinct, innerDistance, matchVelocity).init;
  }

  init {
    switch (dim)
      {2} {"dim = 2; use Boids2D for better performance!".warn}
      {3} {"dim = 3; use Boids3D for better performance!".warn};
    boidList = List.new(0); // an empty list of BoidUnits that we fill below
    maxVelocity = 100; // speed limit in meters per second (need to multiply it by the timestep)
    workingMaxVelocity = maxVelocity * timestep; // set the workingMaxVelocity (accounting for the timestep)
    minSpace = 10; // minmum distance between boids in a flock in meters
    centerOfMass = RealVector.zero(dim); // init center of mass at the origin
    bounds = Array.fill(dim, {[-500,500]}); // make bounds
    this.prFillBoidList(numBoids); // fill the list with boids
    targets = List.new(0);
    obstacles = List.new(0);
    wrap = false; // default to no wrapping
  }

  // optimized version of the rules
  prDoRules {
    var posSum, velSum, sizeOfNeighbors;
    posSum = RealVector.zero(dim); // empty vectors
    velSum = RealVector.zero(dim);
    sizeOfNeighbors = clip(numBoids-1, 1, 6); // get at most 6. If less, get all (but not itself)

    // for each boidUnit...
    boidList.do{|thisBoid, i|
      var neighbors, vec, count, nearestNeighbors, velAvg, posAvg, nIdx;
      neighbors = Array.newClear(numBoids-1);
      nIdx = 0; // index for neighbors
      vec = RealVector.zero(dim); // for rule 2
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
        // get the 6 nearest neighbors, regardless of distance. If less, use all
        [neighbors[j].at(\pos), neighbors[j].at(\vel)];
      };
      nearestNeighbors = nearestNeighbors.flop; // [positions, velocities]

      //////// RULE 1 ////////////////////////////////////
      // for the six nearest, get their average positions and velocities
      posAvg = nearestNeighbors[0].sum/sizeOfNeighbors; // sum and divide
      thisBoid.centerOfMass = thisBoid.instincts.at(\centerInstinct) * posAvg * 0.01; // set it for rule 1

      //////// RULE 2 ////////////////////////////////////
      vec = vec/count; // average the vector
      thisBoid.innerDistance = thisBoid.instincts.at(\innerDistance) * vec; // set the innerDistance vector in each BoidUnit

      //////// RULE 3 ////////////////////////////////////
      velAvg = nearestNeighbors[1].sum/sizeOfNeighbors;
      thisBoid.matchVelocity = thisBoid.instincts.at(\matchVelocity) * velAvg; // send one eigth of the magnitude
    };

    centerOfMass = posSum/numBoids;
    centerOfVel = velSum/numBoids;
  }

  prFillBoidList {|num|
    num.do{
      var boid;
      boid = BoidUnitND.rand(dim, bounds, centerInstinct, innerDistance, matchVelocity, gauss(workingMaxVelocity, workingMaxVelocity*0.05));
      boidList.add(boid); // add it to the list
    };
  }

  addBoid {|initPos|
    var boid, initVel;
    initPos = initPos ? centerOfMass; // place it near the center of the flock
    initVel = centerOfVel; // the average velocity
    boid = BoidUnitND.new(dim, initVel, initPos, bounds, centerOfMass, gauss(workingMaxVelocity, workingMaxVelocity*0.1));
    boidList.add(boid); // add it
    numBoids = numBoids + 1; // increment numBoids

    // set the instinct attributes
    boid.centerInstinct = gauss(centerInstinct, centerInstinct*0.05);
    boid.innerDistance = gauss(innerDistance, innerDistance*0.05);
    boid.matchVelocity = gauss(matchVelocity, matchVelocity*0.05);
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

  // set with a function instead of at once. Easier.
  setBoundLength {|dim, size|
    if(dim.isNil || size.isNil) {"Not enough args".error; ^nil}; // cry if something is wrong
    bounds[dim] = [size * -0.5, size * 0.5];
  }

  // set them all at once
  setAllBoundLengths {|size|
    bounds = dim.collect{[size * -0.5, size * 0.5]};
  }

  ///////////////////////////////////
  // targeting
  ///////////////////////////////////
  addTarget {|pos, gravity|
    if(pos.isNil or: gravity.isNil)
      {"Insuffient arguments: %, %: no target was added!".format(pos, gravity).warn; ^this};
    targets.add(Dictionary.with(*[\pos->RealVector.newFrom(pos), \strength->gravity]));
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
    if(pos.notNil) {targets[index].add(\pos->RealVector.newFrom(pos))}; // should check here if target is a Vector or not
    if(gravity.notNil) {targets[index].add(\strength->gravity)}; // edit the gravity parameter
  }

  /////////////////////////////////////////
  ///// obstacles
  //////////////////////////////////////////
  addObstacle {|pos, repulsion|
    if(pos.isNil or: repulsion.isNil)
      {"Insuffient arguments: %, %: no obstacle was added!".format(pos, repulsion).warn; ^this};
    obstacles.add(Dictionary.with(*[\pos->RealVector.newFrom(pos), \strength->repulsion]));
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
    if(pos.notNil) {obstacles[index].add(\pos->RealVector.newFrom(pos))}; // should check here if target is a Vector or not
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

  /////////////////////////////////
  // custom setter methods
  /////////////////////////////////
  maxVelocity_ {|val|
    maxVelocity = val; // maxVelocity is the maximum length of the velocity vector
    workingMaxVelocity = maxVelocity * timestep;
    boidList.do{|boid|
      boid.maxVelocity = gauss(workingMaxVelocity, workingMaxVelocity*0.05); // set it in each individual boid
    };
  }

  minSpace_ {|val|
    minSpace = val;
    boidList.do{|boid| boid.instincts.add(\minSpace->gauss(minSpace, minSpace*0.05))};
  }

  wrap_ {|boolean|
    wrap = boolean;
    boidList.do(_.wrap_(boolean));
  }

  centerInstinct_ {|val|
    centerInstinct = val; // set it here (the average)
    boidList.do{|boid|
      boid.instincts.add(\centerInstinct->gauss(val, val*0.05));
    };
  }

  innerDistance_ {|val|
    innerDistance = val; // set it here (the average)
    boidList.do{|boid|
      boid.instincts.add(\innerDistance->gauss(val, val*0.05));
    };
  }

  matchVelocity_ {|val|
    matchVelocity = val; // set it here (the average)
    boidList.do{|boid|
      boid.instincts.add(\matchVelocity->gauss(val, val*0.05));
    };
  }

  // visualizer
  visualizer {|whichDimensions = #[0,1], showLabels = false, returnWindow = false|
    var window, loop, availableBounds, size, getNormalizedPos, makeCircle, makeLabel, plotX, plotY;
    availableBounds = Window.availableBounds;
    size = availableBounds.width/3;
    window = Window("Dimensions: % : %".format(whichDimensions[0], whichDimensions[1]), Rect(availableBounds.width-size,availableBounds.height-size,size,size)).front;
    window.view.background_(Color.white);

    // get the dimensions
    plotX = whichDimensions[0];
    plotY = whichDimensions[1];

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
        normalizedPos = getNormalizedPos.(boid.pos[plotX..plotY]);
        Pen.addWedge(
          Point(window.bounds.width*normalizedPos[0], window.bounds.height*normalizedPos[1]), // point
          7.5, // radius (pixels)
          (-1*atan2(boid.vel[plotY], boid.vel[plotX])) - 3.5342917352885, // start angle (angle - pi/8 - pi) for visualizer corrections
          0.78539816339745 // size of angle (pi/4)
        );
        if(showLabels) {
          makeLabel.(i, normalizedPos, color);
        };
        Pen.perform(\fill);
      };

      // plot the targets as blue circles
      targets.do{|target, i|
        var normalizedPos, color;
        color = Color.fromHexString("4989FF");
        Pen.color = color;
        normalizedPos = getNormalizedPos.(target.at(\pos)[plotX..plotY]);
        makeCircle.(normalizedPos); // make the circle
        if(showLabels) {makeLabel.(i, normalizedPos, color)};
        Pen.perform(\fill);
      };

      ////////
      // plot the obstacles as red circles
      ////////
      obstacles.do{|obstacle, i|
        var normalizedPos, color;
        color = Color.fromHexString("FF4949");
        Pen.color = color;
        normalizedPos = getNormalizedPos.(obstacle.at(\pos)[plotX..plotY]);
        makeCircle.(normalizedPos); // make the cirlce
        if(showLabels) {makeLabel.(i, normalizedPos, color)};
        Pen.perform(\fill);
      };
    };

    loop = {
      loop {window.refresh; timestep.wait};
    }.fork(AppClock);

    window.onClose_({loop.stop});
    if(returnWindow) {^window};
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
BoidUnitND {
  var <dim, <>vel, <>pos, <bounds, <centerOfMass, <maxVelocity;
  var <>centerInstinct, <>innerDistance, <>matchVelocity, <>wrap, <>instincts;

  *new {|dim, vel, pos, bounds, centerOfMass, maxVelocity = 5|
    ^super.newCopyArgs(dim, vel, pos, bounds, centerOfMass, maxVelocity).init;
  }

  *rand {|dim, bounds, centerOfMass, innerDistance, matchVelocity, maxVelocity|
    ^super.new.init(dim, bounds, centerOfMass, innerDistance, matchVelocity, maxVelocity);
  }

  init {|...args|
    dim = args[0];
    bounds = bounds ? args[1] ? Array.fill(dim, {[-500,500]}); // [ [xmin, xmax], [ymin, ymax]]
    vel = vel ? RealVector.newFrom(Array.fill(dim, {rrand(0.0,3.0)}));
    pos = pos ? RealVector.rand(dim,-1*bounds[0][0], bounds[0][0]);
    maxVelocity = maxVelocity ? args[5] ? 100; // max velocity

    // if these are not set, set them
    centerOfMass = RealVector.rand(dim,-10,10);
    innerDistance = RealVector.rand(dim,-10,10);
    matchVelocity = RealVector.rand(dim,-10,10);

    instincts = Dictionary[\centerInstinct->gauss(args[2], args[2]*0.05), \innerDistance->gauss(args[3], args[3]*0.05), \matchVelocity->gauss(args[4], args[4]*0.05), \minSpace->gauss(10,0.5)]; // individualized weights
    centerInstinct = centerOfMass/100; // set this here
    vel = vel.limit(maxVelocity); // limit the size of the velocity vector
    wrap = wrap ? false; // default to no wrapping
  }

  prBound {
    var vec = RealVector.zero(dim); // a zero vector
    dim.do{|i|
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
    dim.do{|i|
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
    vel = vel + centerInstinct + innerDistance + matchVelocity; // sum the vectors and get a new velocity
    if(wrap) {this.prWrap} {this.prBound}; // wrap or bound
    vel = vel.limit(maxVelocity); // speed limit
    pos = pos + vel; // get the new position
  }

  getPanVals {
    var zero = RealVector.zero(dim);
    ^[pos.theta, pos.azi, pos.dist(zero)]; // return the angle in radians and the distance from the origin
  }

  calcObstacles {|obstacles|
    var vec = RealVector.zero(dim), distFromTarget, gravity, diff;
    obstacles.do{|obstacle|
      vec = this.prCalcVec(obstacle, vec, \obstacle);
    };
    ^vec; // return the vector
  }

  calcTargets {|targets|
    var vec = RealVector.zero(dim), distFromTarget, gravity, diff;
    targets.do{|target|
      vec = this.prCalcVec(target, vec, \target);
    };
    ^vec; // return the vector
  }

  prCalcVec {|object, vec, type|
    var distFromObject, diff, gravity, reweighted;
    distFromObject = pos.dist(object.at(\pos)).max(0.001); // get the distance from the object
    switch (type)
      {\target} {
        diff = object.at(\pos)-pos; // get the diff
        if(object.at(\strength).isArray) {
          reweighted = dim.collect{|i| // if gravity is an array, apply gravities in specific dimensions
            ((object.at(\strength)[i]*1000)/distFromObject).max(0); // 1/r
          };
          gravity = RealVector.newFrom(reweighted); // get the vector
        } { // else it's an integer so apply it evenly
          gravity = ((object.at(\strength)*100)/distFromObject).max(0); // 1/r
        };
      }
      {\obstacle} {
        diff = pos-object.at(\pos); // get the diff
        if(object.at(\strength).isArray) {
          reweighted = dim.collect{|i| // if gravity is an array, apply gravities in specific dimensions
            ((object.at(\strength)[i]*100)/distFromObject).max(0); // 1/r
          };
          gravity = RealVector.newFrom(reweighted); // get the vector
        } { // else it's an integer so apply it evenly
          gravity = this.prInverseSquare(distFromObject, object.at(\strength)*1000).max(0); // 1/r^2
          // gravity = dim.collect{|i| this.prInverseSquare(diff[i], object.at(\strength)*1000).max(0)}; // check for distance in particular dimensions
        };
      };
    ^vec + ((diff/diff.norm)*gravity); // return
  }

  // get the distance between two boids
  dist {|boid3D|
    ^pos.dist(boid3D.pos);
  }

  /////////////////////////////////
  // gravity/repulsion scaling functions
  /////////////////////////////////
  prInverseSquare {|dist = 1, gravity = 1|
    ^gravity/(dist**2);
  }

  prInverseCubed {|dist = 1, gravity = 1|
    ^gravity/(dist**3);
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

  prColumbsLaw {|dist, q1, q2|
    var k = 8987551787.3681764;
    ^((q1*q2)/(dist**2))*k;
  }

  /////////////////////////////////
  // custom setter methods
  /////////////////////////////////
  centerOfMass_ {|vec|
    centerOfMass = vec; // get the perceived center of mass for this BoidUnit
    // each time we get a new center of mass, recalculate the first vector offset
    centerInstinct = centerOfMass/100; // get the vector that moves it 1% toward the center of the flock (this can be weighted??)
  }

  maxVelocity_ {|val|
    maxVelocity = val; // set it
    vel = vel.limit(maxVelocity); // limit it if it's bigger
  }

  bounds_ {|val|
    bounds = val;
  }
}
