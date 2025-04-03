type Person record {|
    string name;
    int age;
    boolean isAdult;
|};

type PersonWithChildren record {|
    string name;
    int age;
    boolean isAdult;
    Person[] children;
|};

// Array types

type Persons Person[4];

type Parents (PersonWithChildren|Person)[];

type Names string[2];

type Users (Person|string)[4];

type Employees record {|
    int id;
    string name;
    Names otherNames;
|}[];
