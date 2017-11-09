+ ArrayedCollection {

  /*
    interpolate between arrays. Return an array of arrays of size steps+1 of the interpolated arrays from our
    starting arrays (receiver) to our target array (array).
  */

  *interpolate {|thisArray, thatArray, steps = 5|
    if(thisArray.size == thatArray.size)
      {
        var diffs, stepSize, interpArrays;
        diffs = (thisArray.size).collect{|i|
          thatArray[i]-thisArray[i]; // get the difference between every index
        };
        stepSize = diffs/steps; // get the amount to change per sample
        interpArrays = (steps+1).collect{|i| // interpolate and collect into an array of arrays
          if(i==0)
            {thisArray} // first array to return is thisArray
            {
              thisArray+(stepSize*i);
            };
        };
        ^interpArrays; // return
      }
      {
        "Arrays are not equal size!".warn;
        ^nil;
      }
  }

  interpolate {|array, steps = 5|
    if(this.size == array.size)
      {
        var diffs, stepSize, interpArrays;
        diffs = (this.size).collect{|i|
          array[i]-this[i]; // get the difference between every index
        };
        stepSize = diffs/steps; // get the amount to change per sample
        interpArrays = (steps+1).collect{|i| // interpolate and collect into an array of arrays
          if(i==0)
            {this} // first array to return is this
            {
              this+(stepSize*i);
            };
        };
        ^interpArrays; // return
      }
      {
        "Arrays are not equal size!".warn;
        ^nil;
      }
  }

}
