+ SequenceableCollection {

  // write to file. Array.writeFile gets weird so I wrote this one.
  writeToFile {|path, delimiter = "\n", func|
    var file;
    file = File.open(path, "w"); // open a file
    this.do{|val, i|
      if(func.notNil) {
        file.write(func.(val, i) ++ delimiter); // write it
      } {
        file.write(val.asString ++ delimiter); // write it
      }
    };
    file.close;
  }

}
