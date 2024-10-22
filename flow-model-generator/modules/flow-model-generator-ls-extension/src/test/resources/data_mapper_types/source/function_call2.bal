type UserInfo record {|
   readonly string username;
   string password;
   Address address;
|};

type Address record {|
   string city;
   string country;
|};

public function foo(int i, UserInfo userInfo) {

}
