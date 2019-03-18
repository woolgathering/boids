///////////////////////////////////////////
/// Boids class
///////////////////////////////////////////
///////////////////////////////////////////
/*
  Usage:

    b = Boids(numBoids: 10); // an initial number of boids. Flock size can be arbitrarily increased by using addBoid()
    f = {|thisInstance, boids|
      thisInstace.info; // print info about this flock
      boids.postln; // the list of BoidUnits
    };
    b.moveFlock(f);
*/


Boids2D {
  var <>numBoids, <>timestep, <>centerInstinct, <>innerDistance, <>matchVelocity, <centerOfMass;
  var >boidList, <maxVelocity, workingMaxVelocity, <minSpace, targets, obstacles;
  var <bounds, <innerBoundRatio, <useInnerBounds, <innerBounds;

  *new {|numBoids = 10, timestep = 0.5, centerInstinct = 1, innerDistance = 1, matchVelocity = 1|
    ^super.newCopyArgs(numBoids, timestep, centerInstinct, innerDistance, matchVelocity).init;
  }

  init {
    boidList = List.new(0); // an empty list of BoidUnits that we fill below
    maxVelocity = 5; // speed limit in meters per second (need to multiply it by the timestep)
    workingMaxVelocity = maxVelocity * timestep; // set the workingMaxVelocity (accounting for the timestep)
    minSpace = 1; // minmum distance between boids in a flock in meters
    centerOfMass = RealVector2D.zero; // init center of mass at the origin
    bounds = [[-500,500], [-500,500]]; // set the default bounds
    useInnerBounds = false; // default to not using the inner bounds
    innerBoundRatio = 0.1; // default to 10%
    innerBounds = innerBoundRatio * bounds; // for ease of getting and setting
    this.prFillBoidList(numBoids); // fill the list with boids

    targets = List.new(0);
    obstacles = List.new(0);
  }

  // rule 1
  prGetCenterOfMass {
    var sum = RealVector2D.zero; // a zero vector to add to
    boidList.do{|boid, i|
      sum = sum + boid.pos; // sum the values
    };
    centerOfMass = sum/boidList.size; // get the average and set it

    // now set the average within each BoidUnit and compensate for its percieved center by subtracting itself
    boidList.do{|boid, i|
      boid.centerOfMass = centerInstinct * ((sum - boid.pos)/(boidList.size - 1)); // set it
    };
  }

  // rule 2
  prGetInnerDistance {
    boidList.do{|boid|
      var vec, dist, count;
      vec = RealVector2D.newFrom([0,0]); // a new zero vector
      count = 1;
      boidList.do{|thisBoid|
        // var tmpVec = RealVector2D.zero;
        // don't check for boids that are the exact same object
        if ((boid === thisBoid).not) {
          dist = boid.pos.dist(thisBoid.pos); // get the distance between these boids
          // if the absolute value of the distance is less than the threshold
          if (abs(dist) < minSpace) {
            ///// original ///////
            // vec = vec - ((boid.pos-thisBoid.pos)/abs(dist)); // calculate the difference vector
            vec = vec + ((boid.pos-thisBoid.pos)*(minSpace/(dist**2))); // calculate the difference vector
            /////////////////////
            count = count+1; // keep counting the boids in the vicinity
          };
        };
      };
      vec = vec/count; // average
      boid.innerDistance = innerDistance * vec; // set the innerDistance vector in each BoidUnit
    };
  }

  // rule 3
  prGetVelocityMatch {
    var sum = RealVector2D.zero; // a new zero vector
    // sum the velocities
    boidList.do{|boid|
      sum = sum + boid.vel;
    };
    boidList.do{|boid|
      var thisSum = sum - boid.vel; // remove this boid from the sum
      boid.matchVelocity = matchVelocity * ((thisSum/(boidList.size-1)) * 0.125); // send one eigth of the magnitude to the boid
    };
  }

  prFillBoidList {|num|
    // could instead pass an array of Nodes from which the NodeID's could be extracted and passed...
    num.do{
      var boid;
      boid = BoidUnit2D.rand(bounds, centerInstinct, innerDistance, matchVelocity, workingMaxVelocity)
        .useInnerBounds_(useInnerBounds)
        .innerBounds_(innerBounds);
      boidList.add(boid); // add it to the list
    };
  }

  addBoid {|initPos|
    var boid, initVel;
    initPos = initPos ? centerOfMass; // place it near the center of the flock
    initVel = RealVector2D.newFrom(Array.fill(2, {rrand(0.0,3.0)})); // random velocity
    boid = BoidUnit2D.new(initVel, initPos, bounds, centerOfMass, workingMaxVelocity)
      .useInnerBounds_(useInnerBounds)
      .innerBounds_(innerBounds);
    boidList.add(boid); // add it
  }

  removeBoid {|index|
    if (index.isNil) {
      boidList.pop; // if no arg, remove the last BoidUnit
    } {
      boidList.removeAt(index); // else, remove at the index
    };
  }

  sizeOfFlock {
    ^boidList.size; // return the size of the flock
  }

  moveFlock {|func|
    this.prGetCenterOfMass; // rule 1
    this.prGetInnerDistance; // rule 2
    this.prGetVelocityMatch; // rule 3

    // method to set tell all the boids to now calculate and move?
    boidList.do{|boid|
      boid.moveBoid(targets, obstacles); // tell the boid to calculate and move it's position
    };
    func.(this); // evaluate the function while passing this instance
  }

  // calculate the new values but don't send them to the BoidUnits
  calcFlock {|func|
    this.prGetCenterOfMass; // rule 1
    this.prGetInnerDistance; // rule 2
    this.prGetVelocityMatch; // rule 3

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
        // we're good
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
    innerBounds = bounds * innerBoundRatio; // recompute the innerBounds
    // set the bounds in each BoidUnit
    boidList.do{|boid|
      boid.bounds = bounds; // set it in each Boid
      boid.innerBounds = innerBounds;
    };
  }

  ///////////////////////////////////
  // targeting
  ///////////////////////////////////
  addTarget {|vector, gravity|
    if(vector.isNil or: gravity.isNil)
      {"Insuffient arguments: %, %: no target was added!".format(vector, gravity).warn; ^this};
    targets.add([RealVector2D.newFrom(vector[..1]), gravity]);
  }

  clearTargets {
    targets = targets.clear; // clear the list
  }

  removeTarget {|index|
    if(index.isNil) {
      targets.pop; // remove the last index
    } {
      targets.removeAt(index); // remove at the index
    };
  }

  editTarget {|index, target, gravity|
    if(index.isNil) {"Index is nil: no targets were edited!".warn}; // throw a warning if insufficent args were supplied
    if(target.notNil) {targets[index][0] = RealVector2D.newFrom(target[..1])}; // should check here if target is a Vector or not
    if(gravity.notNil) {targets[index][1] = gravity}; // edit the gravity parameter
  }

  /////////////////////////////////////////
  ///// obstacles
  //////////////////////////////////////////
  addObstacle {|vector, repulsion|
    if(vector.isNil or: repulsion.isNil)
      {"Insuffient arguments: %, %: no obstacle was added!".format(vector, repulsion).warn; ^this};
    obstacles.add([RealVector2D.newFrom(vector[..1]), repulsion]); // add a new obstacle
  }

  clearObstacles {
    obstacles = List[]; // clear the list
  }

  removeObstacle {|index|
    if(index.isNil) {
      obstacles.pop; // remove the last index
    } {
      obstacles.removeAt(index); // remove at the index
    };
  }

  editObstacle {|index, obstacle, repulsion|
    if(index.isNil) {"Index is nil: no obstacles were edited!".warn}; // throw a warning if insufficent args were supplied
    if(obstacle.notNil) {obstacles[index][0] = RealVector2D.newFrom(obstacle[..1])}; // should check here if target is a Vector or not
    if(repulsion.notNil) {obstacles[index][1] = repulsion}; // edit the repulsion parameter
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
      boid.maxVelocity = workingMaxVelocity; // set it in each individual boid
    };
  }

  minSpace_ {|val|
    minSpace = val;
    this.prGetInnerDistance;
  }

  innerBoundRatio_ {|val|
    if (val>0.95) {"Clipped innerBoundRatio to 0.95".warn}; // tell if we're doing something dumb
    innerBoundRatio = val.clip(0,0.95); // clip it
    innerBounds = bounds * innerBoundRatio; // for easer getting
    boidList.do(_.innerBounds_(innerBounds)); // set it in the BoidUnits internally
  }

  useInnerBounds_ {|boolean|
    useInnerBounds = boolean; // set it
    boidList.do(_.useInnerBounds_(useInnerBounds)); // set it for each BoidUnit
  }

  // don't let us set the inner bounds manually (for now?)
  innerBounds_ {
    "set innerBounds with innerBoundRatio!".warn;
    ^this;
  }

  // visualizer
  visualizer {|showLabels = false, returnWindow = false|
    var window, loop, availableBounds, size;
    availableBounds = Window.availableBounds;
    size = availableBounds.width/3;
    window = Window("Flock Visualizer", Rect(availableBounds.width-size,availableBounds.height-size,size,size)).front;
    window.view.background_(Color.white);

    // draw the boids (as squares for now)
    window.drawFunc = {
      ////////
      // plot the boids as black squares ////////
      ////////
      boidList.do{|boid, i|
        var normalizedPos;
        Pen.color = Color.black;
        // normalize the position for the window
        normalizedPos = [
          (boid.pos.x+bounds[0][0].abs)/(bounds[0][0].abs*2),
          1 - ((boid.pos.y+bounds[1][0].abs)/(bounds[1][0].abs*2))
        ];
        // Pen.addOval(Rect(window.bounds.width*normalizedPos[0], window.bounds.height*normalizedPos[1], 5, 5));
        Pen.addWedge(
          Point(window.bounds.width*normalizedPos[0], window.bounds.height*normalizedPos[1]), // point
          7.5, // radius (pixels)
          (-1*boid.vel.theta) - 3.5342917352885, // start angle (angle - pi/8 - pi) for visualizer corrections
          // (-1*boid.vel.theta) - (pi/8) - pi, // start angle (angle - pi/8 - pi) for visualizer corrections
          0.78539816339745 // size of angle (pi/4)
        );
        // show labels on the boids
        if(showLabels) {
          Pen.stringAtPoint(i.asString, Point(window.bounds.width*normalizedPos[0] + 3, window.bounds.height*normalizedPos[1] + 3), color: Color.black);
        };
        Pen.perform(\fill);
      };

      ////////
      // plot the targets as blue circles
      ////////
      targets.do{|target, i|
        var normalizedPos, color;
        color = Color.fromHexString("4989FF");
        Pen.color = color;
        normalizedPos = [
          (target[0].x+bounds[0][0].abs)/(bounds[0][0].abs*2),
          (target[0].y+bounds[1][0].abs)/(bounds[1][0].abs*2)
        ];
        normalizedPos = [normalizedPos[0], 1 - normalizedPos[1]];
        // normalizedPos.postln;
        Pen.addOval(
          Rect(window.bounds.width*normalizedPos[0], window.bounds.height*normalizedPos[1], 5, 5);
        );
        if(showLabels) {
          Pen.stringAtPoint(i.asString, Point(window.bounds.width*normalizedPos[0] + 3, window.bounds.height*normalizedPos[1] + 3), color: color);
        };
        Pen.perform(\fill);
      };

      ////////
      // plot the obstacles as red circles
      ////////
      obstacles.do{|obstacle, i|
        var normalizedPos, color;
        color = Color.fromHexString("FF4949");
        Pen.color = color;
        normalizedPos = [
          (obstacle[0].x+bounds[0][0].abs)/(bounds[0][0].abs*2),
          (obstacle[0].y+bounds[1][0].abs)/(bounds[1][0].abs*2)
        ];
        normalizedPos = [normalizedPos[0], 1 - normalizedPos[1]];

        Pen.addOval(
          Rect(window.bounds.width*normalizedPos[0], window.bounds.height*normalizedPos[1], 5, 5);
        );
        if(showLabels) {
          Pen.stringAtPoint(i.asString, Point(window.bounds.width*normalizedPos[0] + 3, window.bounds.height*normalizedPos[1] + 3), color: color);
        };
        Pen.perform(\fill);
      };

      ////////
      // plot the inner bounds as an unfilled square
      ////////
      // {
      //   var verticies, size;
      //   size = innerBounds[0][1];
      //   size = size/(bounds[1][1]*2); // normalize it
      //   verticies = [
      //     Point((-1*size)/2, (-1*size)/2),
      //     Point(size/2, size/2),
      //     Point(size/2, (-1*size)/2),
      //     Point((-1*size)/2, size/2)
      //   ];
      //   Pen.color = Color.grey;
      //   Pen.addRect(
      //     Rect(verticies[0].x, verticies[0].y, size, size);
      //   );
      //   Pen.perform(\stroke);
      // }.value;
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
BoidUnit2D {
  var <>vel, <>pos, <bounds, <centerOfMass, <maxVelocity;
  var <>centerInstinct, <>innerDistance, <>matchVelocity, <>useInnerBounds, <>innerBounds;

  *new {|vel, pos, bounds, centerOfMass, maxVelocity = 5|
    ^super.newCopyArgs(vel, pos, bounds, centerOfMass, maxVelocity).init;
  }

  *rand {|bounds, centerOfMass, innerDistance, matchVelocity, maxVelocity = 5|
    ^super.new.init(bounds, centerOfMass, innerDistance, matchVelocity, maxVelocity);
  }

  init {|...args|
    bounds = bounds ? args[0] ? [[-500,500],[-500,500]]; // [ [xmin, xmax], [ymin, ymax]]
    vel = vel ? RealVector2D.newFrom(Array.fill(2, {rrand(0.0,3.0)}));
    pos = pos ? RealVector.rand2D(bounds[0][0],bounds[0][1],bounds[1][0],bounds[1][1]).asRealVector2D;
    maxVelocity = maxVelocity ? args[4] ? 5; // max velocity

    // if these are not set, set them
    centerOfMass = args[1] ? RealVector.rand2D(-10,10,-10,10).asRealVector2D;
    innerDistance = args[2] ? RealVector.rand2D(-10,10,-10,10).asRealVector2D;
    matchVelocity = args[3] ? RealVector.rand2D(-10,10,-10,10).asRealVector2D;

    centerInstinct = centerOfMass/100; // set this here
    vel = vel.limit(maxVelocity); // limit the size of the velocity vector
    useInnerBounds = false; // default to not using an inner bound method
    innerBounds = bounds * 0.1; // calculate the size as default
  }

  bound {
    2.collect{|i|
      var amount = 0;
    var vec = RealVector2D.zero; // a zero vector
      if(pos[i] < bounds[i][0]) {
          amount = bounds[i][0] + pos[i].abs; // how far off are we
          amount = maxVelocity * (amount/maxVelocity).min(1); // scale it according to how far off we are
        }
        {
          if(pos[i] > bounds[i][1]) {
            amount = bounds[i][1] - pos[i]; // how far off are we
            amount = maxVelocity * (amount/maxVelocity).min(1); // scale it according to how far off we are
          };
        };
      vec.add(amount); // add it to the list
    };

    vec = RealVector2D.newFrom(vec.asArray);
    vel = vel + vec; // add the vectors in velocity-space
  }

  cirlceBound {
    var vec, radius, dist, diff, zero, gravity;
    zero = RealVector2D.zero;
    radius = bounds[0][1]; // get a radius from the origin to a side
    dist = pos.dist(zero); // get the distance between this and the origin

    // if the distance is greater than the radius, then add another vector that points to the origin
    if(dist>radius) {
      diff = dist-radius; // get the difference
      // vec = RealVector2D.zero + ((zero-pos)*diff.lincurve(1, 20.0, 0.01, 1.0, 0.5, \min)); // make a new vector and scale it
      // vec = RealVector2D.zero + ((zero-pos)*diff.lincurve(1, 20.0, 0.01, 5.0, 0.5)*maxVelocity); // make a new vector and scale it
      // vec = RealVector2D.zero + (zero-pos); // make a new vector and scale it
      vec = (RealVector2D.zero + (zero-pos)) * (diff/maxVelocity).min(1); // make a new vector and scale it
      vec = vec.limit(maxVelocity);
      vel = vel + vec; // add it
    };
  }

  // an inner bound. Useful when using as a spatializer
  innerBound {
    var vec, thisX = 0, thisY = 0;
    // along the x-axis
    if ((pos.x > innerBounds[0][0]) and: (pos.x < innerBounds[0][1])) {
      /*
      // get the scalar for the x-axis
      dist = dist(pos,zero);
      ratio = 1 - (dist/innerBounds[0][1]); // use the positive number since they're both the same
      scalar = innerBoundScalar * ratio; // multiply by the "anti-gravity"

      .... then

      thisX = scalar * maxVelocity;
      thisX = -1 * scalar * maxVelocity;
      */
      if (pos.x >= 0) {
        thisX = maxVelocity; // move right
      } {
        thisX = -1*maxVelocity; // move left
      };
    };
    // along the y-axis
    if ((pos.y > innerBounds[1][0]) and: (pos.y < innerBounds[1][1])) {
      if (pos.y >= 0) {
        thisY = maxVelocity; // move up
      } {
        thisY = -1*maxVelocity; // move down
      };
    };

    // "Calculating inner bound...".postln;
    vec = RealVector2D.newFrom([thisX,thisY]);
    pos = pos + vec; // add the vectors
  }

  moveBoid {|targets, obstacles|
    vel = vel + centerInstinct + innerDistance + matchVelocity; // sum the vectors and get a new velocity
    // if (targets.isEmpty.not) {vel = vel + this.calcTargets(targets)}; // if there are targets, calculate the vector
    if (targets.isEmpty.not) {vel = vel + this.calcTargetsWithField(targets)}; // if there are targets, calculate the vector
    if (obstacles.isEmpty.not) {vel = vel + this.calcObstaclesWithField(obstacles)}; // if there are obstacles, calculate the vector
    this.bound; // bound the coordinates
    if (useInnerBounds) {this.innerBound}; // only do the inner bounds when we want
    vel = vel.limit(maxVelocity); // speed limit
    pos = pos + vel; // get the new position
  }

  getPanVals {
    var zero = RealVector2D.zero;
    ^[pos.theta, pos.dist(zero)]; // return the angle in radians and the distance from the origin
  }

  calcObstacles {|obstacles|
    var vec = RealVector2D.zero;
    obstacles.do{|obstacle|
      vec = vec + ((obstacle[0]+pos)*obstacle[1]);
    };
    ^vec; // return the vector
  }

  calcObstaclesWithField {|obstacles|
    var vec = RealVector2D.zero, distFromTarget, gravity;
    obstacles.do{|obstacale|
      distFromTarget = pos.dist(obstacale[0]).max(1); // get the distance from this boid to the obstacale
      gravity = this.inverseSquare(distFromTarget, obstacale[1]).clip(0,1);
      vec = vec + ((obstacale[0]+pos)*gravity);
    };
    ^vec; // return the vector
  }

  calcTargets {|targets|
    var vec = RealVector2D.zero;
    targets.do{|target|
      vec = vec + ((target[0]-pos)*target[1]);
    };
    ^vec; // return the vector
  }

  calcTargetsWithField {|targets|
    var vec = RealVector2D.zero, distFromTarget, gravity;
    targets.do{|target|
      distFromTarget = pos.dist(target[0]).max(1); // get the distance from this boid to the target
      gravity = this.inverseSquare(distFromTarget, target[1]*100).clip(0,1); // must multiply by 100??
      vec = vec + ((target[0]-pos)*gravity);
    };
    ^vec; // return the vector
  }

  inverseSquare {|dist = 1, gravity = 1|
    ^(1*gravity)/(dist**2);
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
    // innerBounds = bounds * innerBoundRatio;
  }
}
