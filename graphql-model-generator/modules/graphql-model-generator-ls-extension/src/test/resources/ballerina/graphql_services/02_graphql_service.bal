import ballerina/graphql;

type NewProfile record {|
    string name;
    int age;
|};

type OldProfile record {|
    *NewProfile;
    int id;
|};

table<OldProfile> key(id) profiles = table [];

service /graphql on new graphql:Listener(9090) {

    remote function addProfile(NewProfile newProfile) returns OldProfile {
        OldProfile profile = {id: profiles.nextKey(), ...newProfile};
        profiles.add(profile);
        return profile;
    }

    resource function get profiles() returns table<OldProfile> {
        return profiles;
    }
}
