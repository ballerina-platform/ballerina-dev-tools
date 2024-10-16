int i = 0;

type UserInfo record {|
   readonly string username;
   string password;
   Address address;
|};

type Address record {|
   string city;
   string country;
|};

