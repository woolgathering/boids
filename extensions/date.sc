+ Date {

  // get exactly 24 hours ago
  yesterday {
    var year, month, day, dayOfWeek, newYears, leapYear;

    newYears = (this.month==1) && (this.day==1); // answer true if it's new years day

    year = if(newYears)
      {
        this.year - 1; // today is January 1, so yesterday was last year
      } {
        this.year; // otherwise it's the same year
      };

    // is it leap year?
    leapYear = if((year/4).isInteger)
      {
        if((year/100).isInteger)
          {
            if((year/400).isInteger)
              {
                true;
              } {
                false;
              };
          } {
            false;
          };
      } {
        false;
      };

    month = if(this.day==1)
      {
        if(this.month==1)
          {12} // it's January 1, so last month was December
          {(this.month - 1) % 13}; // it's the first so yesterday was last month
      } {
        this.month; // otherwise it's this month
      };

    day = if(this.day==1)
      {
        switch(month,
          1, {31}, // yesterday was the last day of January
          2, {
            if(leapYear) {29} {28};
            }, // yesterday was the last day of February
          3, {31}, // yesterday was the last day of March
          4, {30}, // yesterday was the last day of April
          5, {31}, // yesterday was the last day of May
          6, {30}, // yesterday was the last day of June
          7, {31}, // yesterday was the last day of July
          8, {31}, // yesterday was the last day of August
          9, {30}, // yesterday was the last day of September
          10, {31}, // yesterday was the last day of October
          11, {30}, // yesterday was the last day of November
          12, {31} // yesterday was the last day of December
        );
      } {
        this.day - 1; // it's not the first so it's just -1
      };

    dayOfWeek = (this.dayOfWeek-1)%7; // make sure we've got it right

    ^Date(year, month, day, this.hour, this.minute, this.second, dayOfWeek, this.rawSeconds); // return exactly 24 hours ago
  }

  // get exactly 24 hours from now
  tomorrow {
    var year, month, day, dayOfWeek, newYearsEve, leapYear;

    newYearsEve = (this.month==11) && (this.day==31); // answer true if it's new years eve

    year = if(newYearsEve)
      {
        this.year + 1; // today is December 31, so tomorrow is next year
      } {
        this.year; // otherwise it's the same year
      };

    // is it leap year?
    leapYear = if((year/4).isInteger)
      {
        if((year/100).isInteger)
          {
            if((year/400).isInteger)
              {
                true;
              } {
                false;
              };
          } {
            false;
          };
      } {
        false;
      };

    month = if(this.day==31)
      {
        if(this.month==12)
          {1} // it's December 31, so tomorrow is January
          {this.month+1}; // it's the 31st so we know it's the end of some month, so get next month
      } {
        if((this.day==30) && ([8,3,5,10].includes(this.month)))
          {
            this.month+1; // it's the 30th of a month that only has 30 days, so get next month
          } {
            this.month; // otherwise it's the same month
          };
      };

    day = if(this.month==month)
      {
        this.day + 1; // today is the same month as tomorrow, so just add a day
      } {
        1; // months are different so tomorrow is the first of next month
      };

    dayOfWeek = (this.dayOfWeek+1)%7; // make sure we've got it right

    ^Date(year, month, day, this.hour, this.minute, this.second, dayOfWeek, this.rawSeconds); // return exactly 24 hours ago
  }


}
