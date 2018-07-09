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
  var >boidList, <maxVelocity, <minSpace, targets, obstacles;

  *new {|numBoids = 10, timestep = 0.5, centerInstinct = 1, innerDistance = 1, matchVelocity = 1|
    ^super.newCopyArgs(numBoids, timestep, centerInstinct, innerDistance, matchVelocity).init;
  }

  init {
    boidList = List.new(0); // an empty list of BoidUnits that we fill below
    maxVelocity = 5; // speed limit in meters per second (need to multiply it by the timestep)
    minSpace = 1; // minmum distance between boids in a flock in meters
    centerOfMass = RealVector.zero(2).asRealVector2D; // init center of mass at the origin
    this.fillBoidList(numBoids); // fill the list with boidse

    targets = List.new(0);
    obstacles = List.new(0);
  }

  // rule 1
  getCenterOfMass {
    var sum = RealVector.zero(2).asRealVector2D; // a zero vector to add to
    boidList.do{|boid, i|
      sum = sum + boid.pos; // sum the values
    };
    centerOfMass = sum/boidList.size; // get the average and set it

    // now set the average within each BoidUnit and compensate for its percieved center by subtracting itself
    boidList.do{|boid, i|
      boid.centerOfMass = centerInstinct * (sum - boid.pos)/(boidList.size - 1); // set it
    };
  }

  // rule 2
  getInnerDistance {
    boidList.do{|boid|
      var vec, dist;
      vec = RealVector.zero(2).asRealVector2D; // a new zero vector
      boidList.do{|thisBoid|
        // don't check for boids that are the exact same object
        if ((boid === thisBoid).not) {
          dist = boid.pos.dist(thisBoid.pos); // get the distance between these boids
          // if the absolute value of the distance is less than the threshold
          if (abs(dist) < minSpace) {
            vec = vec - dist; // calculate the difference vector
          };
        };
      };
    boid.innerDistance = innerDistance * vec; // set the innerDistance vector in each BoidUnit
    };
  }

  // rule 3
  getVelocityMatch {
    var sum = RealVector.zero(2).asRealVector2D; // a new zero vector
    // sum the velocities
    boidList.do{|boid|
      sum = sum + boid.vel;
    };
    boidList.do{|boid|
      var thisSum = sum - boid.vel; // remove this boid from the sum
      boid.matchVelocity = matchVelocity * (thisSum/(boidList.size-1)) * 0.125; // send one eigth of the magnitude to the boid
    };
  }

  fillBoidList {|num|
    // could instead pass an array of Nodes from which the NodeID's could be extracted and passed...
    num.do{
      boidList.add(BoidUnit.rand(centerInstinct, innerDistance, matchVelocity, maxVelocity: maxVelocity*timestep)); // add a new Boid Unit
    };
  }

  addBoid {|nodeID|
    var initPos, boid;
    initPos = RealVector.rand2D(-10,10,-10,10).asRealVector2D; // get a random vector position
    initPos = centerOfMass + initPos.limit(maxVelocity*2); // place it near the center of the flock
    boid = BoidUnit.new(pos: initPos, maxVelocity: maxVelocity*timestep); // make it
    // if(nodeID.isNil) {boid.nodeID = nodeID};
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
    this.getCenterOfMass; // rule 1
    this.getInnerDistance; // rule 2
    this.getVelocityMatch; // rule 3

    // method to set tell all the boids to now calculate and move?
    boidList.do{|boid|
      boid.moveBoid(targets, obstacles); // tell the boid to calculate and move it's position
    };
    func.(this); // evaluate the function while passing this instance
  }

  // calculate the new values but don't send them to the BoidUnits
  calcFlock {|func|
    this.getCenterOfMass; // rule 1
    this.getInnerDistance; // rule 2
    this.getVelocityMatch; // rule 3

    func.(this); // evaluate the function while passing this instance
  }

  getPanVals {
    ^boidList.collect{|boid|
      boid.getPanVals; // get the pan values
    };
  }

  bounds {|xLen, yLen|
    var rect;
    if(xLen.isNil or: yLen.isNil) {"Missing a dimension: x = %, y = %".format(xLen, yLen).error; ^nil};

    // create the bounds of a rectangle with the given dimensions with the origin at the center
    rect = [[-0.5*xLen, 0.5*xLen], [-0.5*yLen, 0.5*yLen]];
    // set the bounds in each BoidUnit
    boidList.do{|boid|
      boid.bounds = rect; // set it in each Boid
    };
  }

  ///////////////////////////////////
  // targeting
  ///////////////////////////////////
  addTarget {|vector, strength|
    if(vector.isNil or: strength.isNil)
      {"Insuffient arguments: %, %: no target was added!".format(vector, strength).warn; ^this};
    targets.add([RealVector2D.newFrom(vector[..1]), strength]);
  }

  clearTargets {
    targets = List[]; // clear the list
  }

  removeTarget {|index|
    if(index.isNil) {
      targets.pop; // remove the last index
    } {
      targets.removeAt(index); // remove at the index
    };
  }

  editTarget {|index, target, strength|
    if(index.isNil) {"Index is nil: no targets were edited!".warn}; // throw a warning if insufficent args were supplied
    if(target.notNil) {targets[index][0] = RealVector2D.newFrom(target[..1])}; // should check here if target is a Vector or not
    if(strength.notNil) {targets[index][1] = strength}; // edit the strength parameter
  }

  /////////////////////////////////////////
  ///// obstacles
  //////////////////////////////////////////
  addObstacle {|vector, strength|
    if(vector.isNil or: strength.isNil)
      {"Insuffient arguments: %, %: no obstacle was added!".format(vector, strength).warn; ^this};
    obstacles.add([RealVector2D.newFrom(vector[..1]), strength]); // add a new obstacle
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

  editObstacle {|index, obstacle, strength|
    if(index.isNil) {"Index is nil: no obstacles were edited!".warn}; // throw a warning if insufficent args were supplied
    if(obstacle.notNil) {obstacles[index][0] = RealVector2D.newFrom(obstacle[..1])}; // should check here if target is a Vector or not
    if(strength.notNil) {obstacles[index][1] = strength}; // edit the strength parameter
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
    boidList.do{|boid|
      boid.maxVelocity = maxVelocity; // set it in each individual boid
    };
  }

  minSpace_ {|val|
    minSpace = val;
    this.getInnerDistance;
  }

  /////////////////////////////
  // custom getter methods
  /////////////////////////////
  boidList {
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
  var <>vel, <>pos, <maxVelocity, <nodeID, <centerOfMass;
  var <>centerInstinct, <>innerDistance, <>matchVelocity, <>bounds;

  // perhaps set an independence value, such that this weight value is the weight of a random vector added to the final
  // position of the boid? In that way, the vector calculated from the rest of the flock can be diminished and the
  // random value can be added so it has more perceived independence

  *new {|vel, pos, maxVelocity = 5|
    ^super.newCopyArgs(vel, pos, maxVelocity).init;
  }

  *rand {|centerOfMass, innerDistance, matchVelocity, maxVelocity = 5|
    ^super.new.init(centerOfMass, innerDistance, matchVelocity, maxVelocity);
  }

  init {|...args|
    vel = vel ? RealVector.rand2D(-15,15,-15,15).asRealVector2D;
    pos = pos ? RealVector.rand2D(-50,50,-50,50).asRealVector2D;
    maxVelocity = args[3] ? 5;

    // if these are not set, set them
    centerOfMass = args[0] ? RealVector.rand2D(-10,10,-10,10).asRealVector2D;
    innerDistance = args[1] ? RealVector.rand2D(-10,10,-10,10).asRealVector2D;
    matchVelocity = args[2] ? RealVector.rand2D(-10,10,-10,10).asRealVector2D;

    centerInstinct = centerOfMass/100; // set this here
    vel = vel.limit(maxVelocity); // limit the size of the velocity vector
    bounds = [[-100,100],[-100,100]]; // [ [xmin, xmax], [ymin, ymax]]
  }

  bound {
    var vec, thisX = 0, thisY = 0;
    // x position
    if (pos.x < bounds[0][0]) {thisX = 2*maxVelocity};
    if (pos.x > bounds[0][1]) {thisX = -2*maxVelocity};
    // y position
    if (pos.y < bounds[1][0]) {thisY = 2*maxVelocity};
    if (pos.y > bounds[1][1]) {thisY = -2*maxVelocity};

    vec = RealVector2D.newFrom([thisX,thisY]);
    pos = pos + vec; // add the vectors
  }

  moveBoid {|targets, obstacles|
    vel = vel + centerInstinct + innerDistance + matchVelocity; // sum the vectors and get a new velocity
    if (targets.isEmpty.not) {vel = vel + this.calcTargets(targets)}; // if there are targets, calculate the vector
    if (obstacles.isEmpty.not) {vel = vel + this.calcTargets(obstacles)}; // if there are obstacles, calculate the vector
    vel = vel.limit(maxVelocity); // speed limit
    // this.bound; // bound the coordinates
    pos = pos + vel; // get the new position
    // pos = pos.clip(bounds[0][0],bounds[0][1]);
    this.bound; // bound the coordinates
  }

  getPanVals {
    var zero = RealVector.zero(2).asRealVector2D;
    ^[pos.theta, pos.dist(zero)]; // return the angle in radians and the distance from the origin
  }

  calcObstacles {|obstacles|
    var vec = RealVector.zero(2).asRealVector2D;
    obstacles.do{|obstacle|
      vec = vec + ((obstacle[0]-pos)*obstacle[1]*-1);
    };
    ^vec; // return the vector
  }

  calcTargets {|targets|
    var vec = RealVector.zero(2).asRealVector2D;
    targets.do{|target|
      vec = vec + ((target[0]-pos)*target[1]);
    };
    ^vec; // return the vector
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
    // this.moveBoid;
  }
}
