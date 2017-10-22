+ Date {

  yesterday {
    var today;
    today = this.format("%a");
    switch (today,
      "Sun", {^"Sat"},
      "Mon", {^"Sun"},
      "Tue", {^"Mon"},
      "Eed", {^"Tue"},
      "Thu", {^"Wed"},
      "Fri", {^"Thu"},
      "Sat", {^"Fri"},
    );
  }

  tomorrow {
    var today;
    today = this.format("%a");
    switch (today,
      "Sun", {^"Mon"},
      "Mon", {^"Tue"},
      "Tue", {^"Wed"},
      "Eed", {^"Thu"},
      "Thu", {^"Fri"},
      "Fri", {^"Sat"},
      "Sat", {^"Sun"},
    );
  }

}
