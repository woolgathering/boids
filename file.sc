+ File {

  // include a file.
  *include {|path|
    var file, envir;
    if(File.exists(path)) // check if we're talking about a real file
      {
        file = File.open(path,"r"); // read the file
        envir = thisProcess.interpreter.interpret(file.readAllString); // interpret it as a string
        file.close; // close the file
        ^envir; // return our new environment
      } {
        ^"File at % does not exist!".format(path.quote).warn; // if it doesn't exist, throw a fit
      };
    
  }

}