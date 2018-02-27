NaNFilter {

  *ar {|in, nan = 0, infinity = 0, denormal = 0|
    var idx, out;
    idx = CheckBadValues.ar(in, post: 0); // check if it's bad. If it is, give us an index
    ^out = Select.ar(idx, [in, nan, infinity, denormal]); // select the output we want
  }

  *kr {|in, nan = 0, infinity = 0, denormal = 0|
    var idx, out;
    idx = CheckBadValues.kr(in, post: 0); // check if it's bad. If it is, give us an index
    ^out = Select.kr(idx, [in, nan, infinity, denormal]); // select the output we want
  }

}
