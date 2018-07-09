+ String {

  stripNewlines {
    var str = "";
    this.do{|char|
      if(char==$\n) {str = str++""} {str = str++char};
    };
    ^str;
  }

}
