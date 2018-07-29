+ File {

  // include a file.
  *include {|path, envir|
    var file, return;
    path = path.stripNewlines; // strip newlines (only in Atom...)
    if(File.exists(path)) // check if we're talking about a real file
      {
        file = File.open(path,"r"); // read the file
        if(envir.notNil) {
          return = envir.use(thisProcess.interpreter.interpret(file.readAllString)); // interpret it in an environment
        } {
          return = thisProcess.interpreter.interpret(file.readAllString); // interpret it as a string
        };
        file.close; // close the file
        ^return; // return whatever our file returns
      } {
        ^"File at % does not exist!".format(path.quote).warn; // if it doesn't exist, throw a fit
      };

  }

}
